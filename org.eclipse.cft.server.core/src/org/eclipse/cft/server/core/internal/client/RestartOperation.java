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

import org.cloudfoundry.client.lib.CloudFoundryOperations;
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
import org.eclipse.osgi.util.NLS;
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

		try {
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
			appModule = getBehaviour().updateModuleWithAllCloudInfo(appModule.getDeployedApplicationName(), subMonitor.newChild(20));

			final CloudFoundryApplicationModule cloudModule = appModule;

			final ApplicationAction deploymentMode = getDeploymentConfiguration().getApplicationStartMode();
			if (deploymentMode != ApplicationAction.STOP) {

				// Start the application. Use a regular request rather than
				// a staging-aware request, as any staging errors should not
				// result in a reattempt, unlike other cases (e.g. get the
				// staging
				// logs or refreshing app instance stats after an app has
				// started).

				String startLabel = Messages.RestartOperation_STARTING_APP + " - " + deploymentName; //$NON-NLS-1$
				getBehaviour().printlnToConsole(cloudModule, startLabel);

				CloudFoundryPlugin.getCallback().startApplicationConsole(getBehaviour().getCloudFoundryServer(),
						cloudModule, 0, subMonitor.newChild(20));

				new BehaviourRequest<Void>(startLabel, getBehaviour()) {
					@Override
					protected Void doRun(final CloudFoundryOperations client, SubMonitor progress)
							throws CoreException, OperationCanceledException {
						CloudFoundryPlugin.trace("Application " + deploymentName + " starting"); //$NON-NLS-1$ //$NON-NLS-2$

						if (progress.isCanceled()) {
							throw new OperationCanceledException(
									Messages.bind(Messages.OPERATION_CANCELED, getRequestLabel()));
						}
						
						
						// Set the state of the module to Starting
						server.setModuleState(getModules(), IServer.STATE_STARTING);

						StartingInfo info = client.restartApplication(deploymentName);

						// Similarly, check for cancel at this point
						if (progress.isCanceled()) {
							throw new OperationCanceledException(
									Messages.bind(Messages.OPERATION_CANCELED, getRequestLabel()));
						}
						if (info != null) {

							cloudModule.setStartingInfo(info);

							// Inform through callback that application
							// has started
							CloudFoundryPlugin.getCallback().applicationStarting(
									RestartOperation.this.getBehaviour().getCloudFoundryServer(), cloudModule);
						}
						return null;
					}
				}.run(subMonitor.newChild(20));

				// This should be staging aware, in order to reattempt on
				// staging related issues when checking if an app has
				// started or not
				new StagingAwareRequest<Void>(
						NLS.bind(Messages.CloudFoundryServerBehaviour_WAITING_APP_START, deploymentName),
						getBehaviour()) {
					@Override
					protected Void doRun(final CloudFoundryOperations client, SubMonitor progress)
							throws CoreException {

						// TODO: integrate with Application tracker used below.
						// Get the running state of the application based on the instance state
						RestartOperation.this.getBehaviour().getApplicationInstanceRunningTracker(cloudModule)
								.track(progress);
						
						// Check if the app still exists as instance checks can be long running
						// If app is stopped , it may have been stopped
						// externally therefore cancel the restart operation.
						CloudFoundryApplicationModule updatedModule = getBehaviour()
								.updateModuleWithAllCloudInfo(deploymentName, progress);
						if (updatedModule == null || updatedModule.getApplication() == null) {
							server.setModuleState(getModules(), IServer.STATE_STOPPED);

							throw new OperationCanceledException(
									NLS.bind(Messages.RestartOperation_TERMINATING_APP_STOPPED_OR_NOT_EXISTS,
											deploymentName));
						}
				

						AbstractAppStateTracker curTracker = CloudFoundryPlugin.getAppStateTracker(
								RestartOperation.this.getBehaviour().getServer().getServerType().getId(), cloudModule);
						// Check for cancel
						if (progress.isCanceled()) {
							throw new OperationCanceledException(
									Messages.bind(Messages.OPERATION_CANCELED, getRequestLabel()));
						}
						if (curTracker != null) {
							curTracker.setServer(RestartOperation.this.getBehaviour().getServer());
							curTracker.startTracking(cloudModule, progress);
						}

						CloudFoundryPlugin.trace("Application " + deploymentName + " started"); //$NON-NLS-1$ //$NON-NLS-2$

						CloudFoundryPlugin.getCallback().applicationStarted(
								RestartOperation.this.getBehaviour().getCloudFoundryServer(), cloudModule);

						if (curTracker != null) {
							// Framework-based run state tracker. If tracker indicates that the app is no longer starting, it is considered started
							// Wait for application to be ready or getting
							// out of the starting state.
							boolean isAppStarting = true;
							while (isAppStarting && !progress.isCanceled()) {
								if (curTracker.getApplicationState(cloudModule) == IServer.STATE_STARTING) {
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
							server.setModuleState(getModules(), IServer.STATE_STARTED);

						} else {
							server.setModuleState(getModules(), cloudModule.getState());
						}


						return null;
					}
				}.run(subMonitor.newChild(40));
			}
			else {
				// User has selected to deploy the app in STOP mode
				server.setModuleState(getModules(), IServer.STATE_STOPPED);
				subMonitor.worked(80);
			}
		}
		catch (CoreException e) {
			appModule.setError(e);
			server.setModulePublishState(getModules(), IServer.PUBLISH_STATE_UNKNOWN);
			throw e;
		}
	}

	@Override
	protected DeploymentConfiguration getDefaultDeploymentConfiguration() {
		return new DeploymentConfiguration(ApplicationAction.RESTART);
	}
}