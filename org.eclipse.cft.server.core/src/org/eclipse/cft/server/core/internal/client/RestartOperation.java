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
package org.eclipse.cft.server.core.internal.client;

import org.cloudfoundry.client.lib.StartingInfo;
import org.eclipse.cft.server.core.AbstractAppStateTracker;
import org.eclipse.cft.server.core.internal.ApplicationAction;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;

/**
 * 
 * Attempts to start an application. It does not create an application, or
 * incrementally or fully push the application's resources. It simply starts the
 * application in the server with the application's currently published
 * resources, regardless of local changes have occurred or not.
 * 
 */
@SuppressWarnings("restriction")
public class RestartOperation extends ApplicationOperation {

	/**
	 * 
	 */

	public RestartOperation(CloudFoundryServerBehaviour behaviour, IModule[] modules, boolean clearConsole) {
		super(behaviour, modules, clearConsole);
	}

	@Override
	public String getOperationName() {
		return Messages.RestartOperation_STARTING_APP;
	}

	@Override
	protected void performDeployment(CloudFoundryApplicationModule appModule, IProgressMonitor monitor)
			throws CoreException {
		final Server server = (Server) getBehaviour().getServer();
		appModule.setStatus(null);

		final String deploymentName = appModule.getDeploymentInfo().getDeploymentName();

		if (deploymentName == null) {
			server.setModuleState(getModules(), IServer.STATE_UNKNOWN);

			throw CloudErrorUtil.toCoreException(
					"Unable to start application. Missing application deployment name in application deployment information."); //$NON-NLS-1$
		}

		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

		// Update the module with the latest CloudApplication from the
		// client before starting the application
		appModule = getBehaviour().updateModuleWithAllCloudInfo(appModule.getDeployedApplicationName(),
				subMonitor.newChild(20));

		final ApplicationAction deploymentMode = getDeploymentConfiguration().getApplicationStartMode();
		if (deploymentMode != ApplicationAction.STOP) {
			startAndTrackApplication(appModule, subMonitor);
		}
		else {
			// User has selected to deploy the app in STOP mode
			server.setModuleState(getModules(), IServer.STATE_STOPPED);
			subMonitor.worked(80);
		}
	}

	protected void startAndTrackApplication(CloudFoundryApplicationModule appModule, SubMonitor monitor)
			throws CoreException {

		final String deploymentName = appModule.getDeployedApplicationName();

		final String startLabel = Messages.RestartOperation_STARTING_APP + " - " + deploymentName; //$NON-NLS-1$
		Server server = (Server) getBehaviour().getServer();

		getBehaviour().printlnToConsole(appModule, startLabel);

		CloudFoundryPlugin.getCallback().startApplicationConsole(getBehaviour().getCloudFoundryServer(), appModule, 0,
				monitor.newChild(20));

		CloudFoundryPlugin.trace("Application " + deploymentName + " starting"); //$NON-NLS-1$ //$NON-NLS-2$

		if (monitor.isCanceled()) {
			throw new OperationCanceledException(Messages.bind(Messages.OPERATION_CANCELED, startLabel));
		}

		// Get the old state first in case it needs to be restored due to
		// tracking or restart failure
		int updatedState = appModule.getState();

		// IMPORTANT: this sets the module state in the server to STARTING. Be
		// sure to update the module state to another state
		// after the restart and tracking is completed, even on exception, to
		// avoid the state to be stuck in STARTING even if restarting
		// or tracking failed, or operation was canceled.
		try {

			// Set the state of the module to Starting
			server.setModuleState(getModules(), IServer.STATE_STARTING);

			// Perform the actual restarting in the client
			StartingInfo info = getBehaviour().getRequestFactory().restartApplication(deploymentName, startLabel)
					.run(monitor.newChild(20));

			appModule.setStartingInfo(info);

			updatedState = trackApplicationRunningState(appModule, startLabel, monitor);
		}
		catch (OperationCanceledException oce) {
			updatedState = IServer.STATE_UNKNOWN;
			throw oce;
		}
		catch (CoreException ce) {
			updatedState = IServer.STATE_UNKNOWN;
			throw ce;
		}
		finally {
			// Always update the module state in the server
			server.setModuleState(getModules(), updatedState);

			// This may also update the module state in the server indirectly,
			// but in case an error occurs during the module update, the module
			// state is still explicitly set above to avoid apps to be stuck in
			// "Starting"
			getBehaviour().updateModuleWithAllCloudInfo(deploymentName, monitor);
		}
	}

	protected int trackApplicationRunningState(CloudFoundryApplicationModule cloudModule, String startLabel,
			IProgressMonitor progress) throws CoreException {

		Server server = (Server) getBehaviour().getServer();
		String deploymentName = cloudModule.getDeployedApplicationName();

		// TODO: integrate with Application tracker used below.
		// Get the running state of the application based on the instance state
		// using the default tracker
		int updatedState = RestartOperation.this.getBehaviour().getApplicationInstanceRunningTracker(cloudModule)
				.track(progress);

		CloudFoundryPlugin.trace("Default tracker: application " + deploymentName + " tracking completed"); //$NON-NLS-1$ //$NON-NLS-2$

		// Perform additional tracking checks through the tracker framework
		AbstractAppStateTracker curTracker = CloudFoundryPlugin.getAppStateTracker(
				RestartOperation.this.getBehaviour().getServer().getServerType().getId(), cloudModule);
		
		// Check for cancel
		if (progress.isCanceled()) {
			throw new OperationCanceledException(Messages.bind(Messages.OPERATION_CANCELED, startLabel));
		}

		if (curTracker != null) {
			
			curTracker.setServer(RestartOperation.this.getBehaviour().getServer());
			curTracker.startTracking(cloudModule, progress);

			// Framework-based run state tracker. If tracker indicates that
			// the
			// app is no longer starting, it is considered started
			// Wait for application to be ready or getting
			// out of the starting state.
			boolean isAppStarting = true;
			while (isAppStarting && !progress.isCanceled()) {
				// For framework trackers, keep tracking as long as tracker
				// indicates it is in STARTING state
				updatedState = curTracker.getApplicationState(cloudModule);
				if (updatedState == IServer.STATE_STARTING) {
					try {
						Thread.sleep(200);
					}
					catch (InterruptedException e) {
						// Do nothing
					}
				}
				else {
					isAppStarting = false;
				}
			}
			curTracker.stopTracking(cloudModule, progress);
		}

		if (updatedState == IServer.STATE_STARTED) {
			CloudFoundryPlugin.getCallback()
					.applicationStarted(RestartOperation.this.getBehaviour().getCloudFoundryServer(), cloudModule);
		}
		return updatedState;
	}

	@Override
	protected DeploymentConfiguration getDefaultDeploymentConfiguration() {
		return new DeploymentConfiguration(ApplicationAction.RESTART);
	}
}