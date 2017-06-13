/*******************************************************************************
 * Copyright (c) 2012, 2017 Pivotal Software, Inc. and others
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution. 
 * 
 * The Eclipse Public License is available at 
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * and the Apache License v2.0 is available at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * You may elect to redistribute this code under either of these licenses.
 *  
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.debug;

import org.eclipse.cft.server.core.AbstractDebugProvider;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.cft.server.core.internal.application.ModuleChangeEvent;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

public abstract class ApplicationDebugLauncher {

	public static final ApplicationDebugLauncher NO_DEBUG = new ApplicationDebugLauncher() {

		@Override
		public void launch(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer, int appInstance,
				int remoteDebugPort) throws CoreException {
			throw CloudErrorUtil.toCoreException("Debug not supported for " + cloudServer.getServer().getId()); //$NON-NLS-1$
		}
	};

	abstract public void launch(final CloudFoundryApplicationModule appModule, final CloudFoundryServer cloudServer,
			final int appInstance, final int remoteDebugPort) throws CoreException;

	public static void terminateLaunch(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			int appInstance) throws CoreException {
		AbstractDebugProvider provider = DebugProviderRegistry.getProvider(appModule, cloudServer);
		if (provider != null) {
			String appLaunchId = provider.getApplicationDebugLaunchId(appModule.getLocalModule(), cloudServer.getServer(), appInstance);
			terminateLaunch(appLaunchId);
		}
	}

	public boolean supportsDebug(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		AbstractDebugProvider provider = DebugProviderRegistry.getProvider(appModule, cloudServer);
		return provider != null && provider.isDebugSupported(appModule.getLocalModule(), cloudServer.getServer());
	}

	/**
	 * Determines if the application associated with the given module and server
	 * is connected to a debugger. Note that this does not check if the
	 * application is currently running in the server. It only checks if the
	 * application is connected to a debugger, although implicitly any
	 * application connected to a debugger is also running, but not vice-versa.
	 * 
	 * @return true if associated application is connected to a debugger. False
	 * otherwise
	 */
	public boolean isConnectedToDebugger(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			int appInstance) {
		AbstractDebugProvider provider = DebugProviderRegistry.getProvider(appModule, cloudServer);
		if (provider != null) {
			try {
				String id = provider.getApplicationDebugLaunchId(appModule.getLocalModule(), cloudServer.getServer(), appInstance);
				return ApplicationDebugLauncher.getActiveLaunch(id) != null;
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logWarning(e.getMessage());
			}
		}
		return false;
	}

	public static void terminateLaunch(String launchId) {
		ILaunch launch = getLaunch(launchId);
		CloudFoundryServer cloudServer = null;
		CloudFoundryApplicationModule appModule = null;
		CoreException error = null;

		appModule = null;
		if (launch != null) {

			ILaunchConfiguration config = launch.getLaunchConfiguration();
			if (config != null) {
				try {
					String serverId = config.getAttribute(CloudFoundryDebugDelegate.CLOUD_DEBUG_SERVER, (String) null);
					cloudServer = CloudServerUtil.getCloudServer(serverId);
					if (cloudServer != null) {
						String appName = config.getAttribute(CloudFoundryDebugDelegate.CLOUD_DEBUG_APP_NAME,
								(String) null);
						if (appName != null) {
							appModule = cloudServer.getExistingCloudModule(appName);
						}
					}
				}
				catch (CoreException e) {
					error = e;
				}
			}

			if (!launch.isTerminated()) {
				try {
					launch.terminate();
				}
				catch (DebugException e) {
					CloudFoundryPlugin.logError("Failed to terminate debug connection for : " + launchId, e); //$NON-NLS-1$
				}
			}

			DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);

			if (cloudServer == null || appModule == null) {
				String errorMessage = "Unable to resolve cloud server or application when notifying of debug termination - " //$NON-NLS-1$
						+ launchId;
				CloudFoundryPlugin.logError(errorMessage, error);
			}
			else {
				fireDebugChanged(cloudServer, appModule, Status.OK_STATUS);
			}
		}
	}

	public static void addDebuggerConnectionListener(String connectionId, ILaunch launch) {
		Object source = launch.getDebugTarget();
		ConnectToDebuggerListener debugListener = new ConnectToDebuggerListener(connectionId, source);
		DebugPlugin.getDefault().addDebugEventListener(debugListener);
	}

	public static ILaunch getActiveLaunch(String launchId) {
		ILaunch launch = getLaunch(launchId);
		return launch != null && !launch.isTerminated() ? launch : null;
	}

	public static ILaunch getLaunch(String launchId) {
		if (launchId == null) {
			return null;
		}
		ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
		for (ILaunch launch : launches) {
			ILaunchConfiguration config = launch.getLaunchConfiguration();
			try {
				// Fix for NPE: Bug 518204
				if (config != null && launchId.equals(
						config.getAttribute(CloudFoundryDebugDelegate.CLOUD_DEBUG_APP_LAUNCH_ID, (String) null))) {
					return launch;
				}
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logWarning(e.getMessage());
			}
		}
		return null;
	}

	public static final void fireDebugChanged(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule,
			IStatus status) {
		ServerEventHandler.getDefault().fireServerEvent(new ModuleChangeEvent(cloudServer,
				CloudServerEvent.EVENT_APP_DEBUG, appModule.getLocalModule(), status));
	}

}
