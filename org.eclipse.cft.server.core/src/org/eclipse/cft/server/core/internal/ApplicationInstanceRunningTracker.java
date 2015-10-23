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
package org.eclipse.cft.server.core.internal;

import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.InstanceState;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

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

	public InstanceState track(IProgressMonitor monitor) throws CoreException {

		InstanceState runState = null;

		long currentTime = System.currentTimeMillis();

		long totalTime = currentTime + timeout;

		CloudFoundryApplicationModule appModule = cloudServer.getBehaviour().updateCloudModuleWithInstances(appName,
				monitor);

		ApplicationStats stats = getStats(monitor);

		printlnToConsole(NLS.bind(Messages.ApplicationInstanceStartingTracker_STARTING_TRACKING, appName), appModule);

		while (runState != InstanceState.RUNNING && runState != InstanceState.FLAPPING
				&& runState != InstanceState.CRASHED && currentTime < totalTime) {

			runState = getRunState(stats, appModule.getApplication());
			try {
				Thread.sleep(WAIT_TIME);
			}
			catch (InterruptedException e) {

			}

			stats = getStats(monitor);

			currentTime = System.currentTimeMillis();
		}

		String runningStateMessage = runState == InstanceState.RUNNING
				? NLS.bind(Messages.ApplicationInstanceStartingTracker_APPLICATION_IS_RUNNING, appName)
				: NLS.bind(Messages.ApplicationInstanceStartingTracker_APPLICATION_IS_NOT_RUNNING, appName);
		printlnToConsole(runningStateMessage, appModule);

		return runState;
	}

	protected void printlnToConsole(String message, CloudFoundryApplicationModule appModule) throws CoreException {
		message += '\n';
		CloudFoundryPlugin.getCallback().printToConsole(cloudServer, appModule, message, false, false);
	}

	protected ApplicationStats getStats(IProgressMonitor monitor) throws CoreException {
		return cloudServer.getBehaviour().getApplicationStats(appName, monitor);
	}

	public static InstanceState getRunState(ApplicationStats stats, CloudApplication app) {

		InstanceState runState = InstanceState.UNKNOWN;
		if (stats != null && stats.getRecords() != null && !stats.getRecords().isEmpty()
				&& app.getState() != CloudApplication.AppState.STOPPED) {
			return stats.getRecords().get(0).getState();
		}

		return runState;
	}

}
