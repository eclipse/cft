/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;
import org.eclipse.jdt.internal.launching.JavaSourceLookupDirector;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.util.NLS;

public abstract class CloudFoundryDebugDelegate extends AbstractJavaLaunchConfigurationDelegate {

	public static final String SOURCE_LOCATOR = "org.eclipse.cft.debug.sourcepathcomputer"; //$NON-NLS-1$

	public static final String CLOUD_DEBUG_APP_LAUNCH_ID = "cloudDebugAppLaunchId";//$NON-NLS-1$

	public static final String CLOUD_DEBUG_SERVER = "cloudDebugServer"; //$NON-NLS-1$

	public static final String CLOUD_DEBUG_APP_NAME = "cloudDebugAppName"; //$NON-NLS-1$

	public static final String CLOUD_DEBUG_APP_INSTANCE = "cloudDebugAppInstance"; //$NON-NLS-1$

	public static final String CLOUD_DEBUG_REMOTE_DEBUG_PORT = "cloudDebugRemoteDebugPort"; //$NON-NLS-1$

	public static final String TIME_OUT = "timeout"; //$NON-NLS-1$

	public static final String HOST_NAME = "hostname"; //$NON-NLS-1$

	public static final String PORT = "port"; //$NON-NLS-1$

	public static int DEFAULT_REMOTE_PORT = 45283;

	public abstract String getLaunchConfigurationTypeId();

	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {

		CloudFoundryServer cloudServer = getCloudServer(configuration);
		CloudFoundryApplicationModule appModule = getCloudApplication(configuration);
		int appInstance = getAppInstance(configuration);
		int remoteDebugPort = getRemoteDebugPort(configuration);

		CloudFoundryDebugProvider provider = DebugProviderRegistry.getExistingProvider(appModule, cloudServer);

		DebugConnectionDescriptor connectionDescriptor = getDebugConnectionDescriptor(appModule, cloudServer,
				appInstance, remoteDebugPort, monitor);
		if (connectionDescriptor == null) {
			throw CloudErrorUtil.toCoreException(NLS.bind(
					"Unable to resolve debug connection information for {0}. Please check if debug is supported for the given application in {1} ", //$NON-NLS-1$
					appModule.getDeployedApplicationName(), cloudServer.getServer().getId()));
		}

		// Use default for now
		final IVMConnector connector = JavaRuntime.getDefaultVMConnector();

		// Create the required arguments for the IVMConnector
		final Map<String, String> argMap = new HashMap<String, String>();
		String timeout = configuration.getAttribute(TIME_OUT, (String) null);
		if (timeout != null) {
			String timeoutVal = connectionDescriptor.getTimeout() + "";//$NON-NLS-1$
			argMap.put("timeout", timeoutVal);//$NON-NLS-1$
		}

		argMap.put("hostname", connectionDescriptor.getHost());//$NON-NLS-1$
		argMap.put("port", connectionDescriptor.getPort() + "");//$NON-NLS-1$ //$NON-NLS-2$

		setSourceLocator(launch);
		try {
			connector.connect(argMap, monitor, launch);
			ApplicationDebugLauncher.addDebuggerConnectionListener(
					provider.getApplicationDebugLaunchId(appModule, cloudServer, appInstance), launch);
		}
		catch (CoreException e) {
			fireDebugChanged(configuration, e.getStatus());
			throw e;
		}
	}

	public final void fireDebugChanged(ILaunchConfiguration config, IStatus status) {
		CloudFoundryServer cloudServer = getCloudServer(config);
		CloudFoundryApplicationModule appModule = getCloudApplication(config);
		ApplicationDebugLauncher.fireDebugChanged(cloudServer, appModule, status);
	}

	protected void setSourceLocator(ILaunch launch) throws CoreException {
		ILaunchConfiguration configuration = launch.getLaunchConfiguration();
		if (launch.getSourceLocator() == null) {
			ISourceLookupDirector sourceLocator = new JavaSourceLookupDirector();
			ISourcePathComputer locator = getLaunchManager().getSourcePathComputer(SOURCE_LOCATOR);
			if (locator != null) {
				sourceLocator.setSourcePathComputer(locator); // $NON-NLS-1$
				sourceLocator.initializeDefaults(configuration);
				launch.setSourceLocator(sourceLocator);
			}
		}
	}

	protected abstract DebugConnectionDescriptor getDebugConnectionDescriptor(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, int appInstance, int remoteDebugPort, IProgressMonitor monitor)
					throws CoreException;

	protected void printToConsole(CloudFoundryApplicationModule appModule, CloudFoundryServer server, String message,
			boolean error) {
		if (appModule != null && server != null) {
			CloudFoundryPlugin.getCallback().printToConsole(server, appModule, message + '\n', false, error);
		}
	}

	/*
	 *
	 * 
	 * Static helper methods
	 * 
	 * 
	 * 
	 */

	public static CloudFoundryServer getCloudServer(ILaunchConfiguration config) {
		if (config != null) {
			try {
				String serverId = config.getAttribute(CLOUD_DEBUG_SERVER, (String) null);
				return CloudServerUtil.getCloudServer(serverId);
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
		}
		return null;
	}

	public static int getAppInstance(ILaunchConfiguration config) {
		if (config != null) {
			try {
				return config.getAttribute(CLOUD_DEBUG_APP_INSTANCE, 0);
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
		}
		return 0;
	}

	public static int getRemoteDebugPort(ILaunchConfiguration config) {
		if (config != null) {
			try {
				return config.getAttribute(CLOUD_DEBUG_REMOTE_DEBUG_PORT, DEFAULT_REMOTE_PORT);
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
		}
		return DEFAULT_REMOTE_PORT;
	}

	public static CloudFoundryApplicationModule getCloudApplication(ILaunchConfiguration config) {
		if (config != null) {
			try {
				CloudFoundryServer cloudServer = getCloudServer(config);
				if (cloudServer != null) {
					String appName = config.getAttribute(CLOUD_DEBUG_APP_NAME, (String) null);
					if (appName != null) {
						return cloudServer.getExistingCloudModule(appName);
					}
				}

			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
		}
		return null;
	}

}
