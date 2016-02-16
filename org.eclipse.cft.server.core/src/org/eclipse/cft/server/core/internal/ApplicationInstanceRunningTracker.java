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
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;

/**
 * Tracks the running state of an application in Cloud Foundry by checking if
 * the application instances are running (i.e. started). The tracking will stop
 * if it detects that the application is started, stopped, or tracking times
 * out.
 * <p/>
 * This does NOT update the modules in the {@link IServer}. The purpose of the
 * tracker is to resolve up-to-date running state of the application in Cloud
 * Foundry by direct tracking of the application, but not perform any updates on
 * the {@link IServer}
 *
 */
public class ApplicationInstanceRunningTracker {
	public static final long TIMEOUT = 1000 * 60 * 5;

	public static final long WAIT_TIME = 1000;

	private final CloudFoundryServer cloudServer;

	private final CloudFoundryApplicationModule appModule;

	private final long timeout;

	public ApplicationInstanceRunningTracker(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		this.appModule = appModule;
		this.timeout = TIMEOUT;
	}

	/**
	 * 
	 * @param monitor
	 * @return One of the following application running states:
	 * {@link IServer#STATE_STARTED}, {@link IServer#STATE_STOPPED},
	 * {@link IServer#STATE_UNKNOWN}
	 * @throws CoreException if failure occurred during tracking
	 * @throws OperationCanceledException if tracking was cancelled.
	 */
	public int track(IProgressMonitor monitor) throws CoreException, OperationCanceledException {

		long currentTime = System.currentTimeMillis();

		long totalTime = currentTime + timeout;

		CloudFoundryServerBehaviour behaviour = cloudServer.getBehaviour();
		String appName = appModule.getDeployedApplicationName();

		printlnToConsole(NLS.bind(Messages.ApplicationInstanceStartingTracker_STARTING_TRACKING, appName), appModule);

		int state = IServer.STATE_UNKNOWN;

		while (state != IServer.STATE_STARTED && state != IServer.STATE_STOPPED && currentTime < totalTime) {

			// NOTE: app state is NOT the same as the INSTANCE state. Instance
			// state indicates if app is actually running or not.
			// App state indicates the desired state of the app. So an app in
			// STOPPED state will not have instances running. If
			// app is STARTED, instances may still not be running if the app
			// instances are still starting, are flapping, or have crashed.

			if (monitor != null && monitor.isCanceled()) {
				String error = NLS.bind(Messages.ApplicationInstanceStartingTracker_APPLICATION_CHECK_CANCELED,
						appName);
				printlnToConsole(error, appModule);

				throw new OperationCanceledException(error);
			}

			CloudApplication cloudApp = behaviour.getCloudApplication(appName, monitor);
			ApplicationStats applicationStats = behaviour.getApplicationStats(appName, monitor);

			if (cloudApp == null) {
				// app may no longer exist
				String error = NLS.bind(Messages.ApplicationInstanceStartingTracker_APPLICATION_NOT_EXISTS, appName);
				printlnToConsole(error, appModule);
				throw CloudErrorUtil.toCoreException(error);
			}
			else {
				state = CloudFoundryApplicationModule.getCloudState(cloudApp, applicationStats);
				try {
					Thread.sleep(WAIT_TIME);
				}
				catch (InterruptedException e) {

				}

				currentTime = System.currentTimeMillis();
			}
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

}
