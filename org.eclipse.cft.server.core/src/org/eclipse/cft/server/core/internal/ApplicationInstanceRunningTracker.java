/*******************************************************************************
 * Copyright (c) 2015, 2016 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal;

import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;

/**
 * Tracks the running state of an application in Cloud Foundry by checking if
 * the application instances are running.
 *
 */
public class ApplicationInstanceRunningTracker {
	public static final long TIMEOUT = 1000 * 60 * 5;

	public static final long WAIT_TIME = 1000;

	private final CloudFoundryServer cloudServer;

	private final String appName;

	private final long timeout;

	public ApplicationInstanceRunningTracker(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		this.appName = appModule.getDeployedApplicationName();
		this.timeout = TIMEOUT;
	}

	/**
	 * 
	 * @param monitor
	 * @return One of the following application running states: {@link IServer#STATE_STARTED}, {@link IServer#STATE_STARTING},
	 * {@link IServer#STATE_STOPPED}, {@link IServer#STATE_STOPPING},
	 * {@link IServer#STATE_UNKNOWN}
	 * @throws CoreException
	 */
	public int track(IProgressMonitor monitor) throws CoreException {

		long currentTime = System.currentTimeMillis();

		long totalTime = currentTime + timeout;

		CloudFoundryApplicationModule appModule = cloudServer.getBehaviour().updateModuleWithAllCloudInfo(appName,
				monitor);

		printlnToConsole(NLS.bind(Messages.ApplicationInstanceStartingTracker_STARTING_TRACKING, appName), appModule);

		int state = appModule.getState();
		while (state != IServer.STATE_STARTED && state != IServer.STATE_STOPPED && currentTime < totalTime) {

			// NOTE: app state is NOT the same as the INSTANCE state. Instance
			// state indicates if app is actually running or not.
			// App state indicates the desired state of the app. So an app in
			// STOPPED state will not have instances running. If
			// app is STARTED, instances may still not be running if the app
			// instances are still starting, are flapping, or have crashed.
			appModule = cloudServer.getBehaviour().updateModuleWithAllCloudInfo(appName, monitor);
			if (appModule == null || appModule.getApplication() == null) {
				// app may no longer exist
				printlnToConsole(NLS.bind(Messages.ApplicationInstanceStartingTracker_APPLICATION_NOT_EXISTS, appName),
						appModule);
				return IServer.STATE_UNKNOWN;
			}

			if (monitor != null && monitor.isCanceled()) {
				printlnToConsole(
						NLS.bind(Messages.ApplicationInstanceStartingTracker_APPLICATION_CHECK_CANCELED, appName),
						appModule);

				return IServer.STATE_UNKNOWN;
			}

			state = appModule.getState();
			try {
				Thread.sleep(WAIT_TIME);
			}
			catch (InterruptedException e) {

			}

			currentTime = System.currentTimeMillis();
		}

		String runningStateMessage = state == IServer.STATE_STARTED
				? NLS.bind(Messages.ApplicationInstanceStartingTracker_APPLICATION_IS_RUNNING, appName)
				: NLS.bind(Messages.ApplicationInstanceStartingTracker_APPLICATION_IS_NOT_RUNNING, appName);
		printlnToConsole(runningStateMessage, appModule);

		return state;
	}

	protected void printlnToConsole(String message, CloudFoundryApplicationModule appModule) throws CoreException {
		message += '\n';
		CloudFoundryPlugin.getCallback().printToConsole(cloudServer, appModule, message, false, false);
	}

	protected ApplicationStats getStats(IProgressMonitor monitor) throws CoreException {
		return cloudServer.getBehaviour().getApplicationStats(appName, monitor);
	}
}
