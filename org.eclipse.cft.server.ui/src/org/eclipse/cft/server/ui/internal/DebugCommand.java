/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.ui.internal;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.debug.DebugLaunch;
import org.eclipse.cft.server.core.internal.debug.DebugOperations;
import org.eclipse.cft.server.core.internal.debug.DebugProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * Launches an application in debug mode. Also allows a debugger connection to
 * be terminated.
 */
public class DebugCommand {

	private final DebugLaunch launch;

	public DebugCommand(DebugLaunch launch) {
		this.launch = launch;
	}

	/**
	 * Launch an application specified by the {@link DebugLaunch} in debug mode.
	 */
	public void debug(IProgressMonitor monitor) {

		try {

			if (!launch.isConnectedToDebugger() && launch.configure(monitor)) {

				ILaunchConfiguration launchConfiguration = launch.resolveLaunchConfiguration(monitor);

				DebugUITools.launch(launchConfiguration, ILaunchManager.DEBUG_MODE);
				DebugUITools.setLaunchPerspective(launchConfiguration.getType(), ILaunchManager.DEBUG_MODE,
						IDebugUIConstants.ID_DEBUG_PERSPECTIVE);
				DebugOperations.fireDebugChanged(launch.getCloudFoundryServer(), launch.getApplicationModule(),
						Status.OK_STATUS);
			}

		}
		catch (OperationCanceledException e) {
			// do nothing, debug should be cancelled without error
			return;
		}
		catch (CoreException ce) {
			CloudFoundryPlugin.getCallback().handleError(ce.getStatus());
		}
	}

	public void terminate() {
		if (launch.isConnectedToDebugger()) {
			DebugOperations.terminateLaunch(launch.getDebuggerConnectionIdentifier());
		}
	}

	public DebugLaunch getLaunch() {
		return launch;
	}

	/**
	 * Helper method that launches the given application running on the target
	 * Cloud server in debug mode asynchronously.
	 */
	public static void debug(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule) {

		final DebugCommand command = getCommand(cloudServer, appModule);

		if (command != null) {
			Job job = new Job("Launching debug - " + appModule.getDeployedApplicationName()) { //$NON-NLS-1$

				protected IStatus run(IProgressMonitor monitor) {

					command.debug(monitor);

					return Status.OK_STATUS;
				}
			};

			job.setSystem(true);
			job.setPriority(Job.INTERACTIVE);
			job.schedule();
		}
	}

	public static DebugCommand getCommand(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule) {
		DebugProvider provider = DebugProvider.getCurrent(appModule, cloudServer);

		if (provider != null) {
			final DebugLaunch launch = DebugOperations.getDebugLaunch(cloudServer, appModule, new DebugUIProvider(
					provider));
			if (launch != null) {
				return new DebugCommand(launch);
			}
		}
		return null;
	}
}
