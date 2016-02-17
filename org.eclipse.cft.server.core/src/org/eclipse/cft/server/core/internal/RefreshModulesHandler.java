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

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.core.internal.client.BehaviourOperation;
import org.eclipse.cft.server.core.internal.client.CloudBehaviourOperations;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;

/**
 * Handles refresh of modules in a target Cloud Space.
 * <p/>
 * As refreshing modules may include fetch a list of {@link CloudApplication}
 * from the target Cloud space associated with the given
 * {@link CloudFoundryServer} which may be a long-running task, module refreshes
 * is performed asynchronously as a job, and only one job is scheduled per
 * behaviour regardless of the number of refresh requests received
 * 
 */
public class RefreshModulesHandler {

	private BehaviourRefreshJob refreshJob;

	private final CloudFoundryServer cloudServer;

	private BehaviourOperation opToRun;

	private static final String NO_SERVER_ERROR = "Null server in refresh module handler. Unable to schedule module refresh."; //$NON-NLS-1$

	/**
	 * 
	 * @param cloudServer may be null if not resolved.
	 */
	public RefreshModulesHandler(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		String serverName = cloudServer != null ? cloudServer.getServer().getId() : "Unknown server"; //$NON-NLS-1$

		String refreshJobLabel = NLS.bind(Messages.RefreshModulesHandler_REFRESH_JOB, serverName);

		this.refreshJob = new BehaviourRefreshJob(refreshJobLabel);
	}

	/**
	 * Updates all modules in the server, as well as services
	 */
	public synchronized void updateAll() {
		if (cloudServer == null) {
			CloudFoundryPlugin.logError(NO_SERVER_ERROR);
		}
		else if (this.opToRun == null) {
			scheduleRefresh(cloudServer.getBehaviour().operations().updateAll());
		}
	}

	public synchronized boolean isScheduled() {
		return this.opToRun != null;
	}

	/**
	 * Schedule an update on a deployed module. If module is not deployed, no
	 * refresh will occur. 
	 * @see CloudBehaviourOperations#updateDeployedModule(IModule)
	 * @param module to refresh
	 */
	public synchronized void updateDeployedModule(IModule module) {
		if (cloudServer == null) {
			CloudFoundryPlugin.logError(NO_SERVER_ERROR);
		}
		else if (this.opToRun == null) {
			scheduleRefresh(cloudServer.getBehaviour().operations().updateDeployedModule(module));
		}
	}
	
	/**
	 * Schedule an update on a module regardless if it is deployed or no.
	 * @see CloudBehaviourOperations#updateModule(IModule)
	 */
	public synchronized void updateModule(IModule module) {
		if (cloudServer == null) {
			CloudFoundryPlugin.logError(NO_SERVER_ERROR);
		}
		else if (this.opToRun == null) {
			scheduleRefresh(cloudServer.getBehaviour().operations().updateModule(module));
		}
	}

	/**
	 * * Updates module and notifies that module has been updated after publish.
	 * This generates a different event than
	 * {@link #updateDeployedModule(IModule)} specific to publishing
	 * @param module
	 */
	public synchronized void updateOnPublish(IModule module) {
		if (cloudServer == null) {
			CloudFoundryPlugin.logError(NO_SERVER_ERROR);
		}
		else if (this.opToRun == null) {
			scheduleRefresh(cloudServer.getBehaviour().operations().updateOnPublish(module));
		}
	}

	private synchronized void scheduleRefresh(BehaviourOperation opToRun) {
		if (this.opToRun == null) {
			this.opToRun = opToRun;
			schedule();
		}
	}

	private void schedule() {
		refreshJob.setSystem(false);
		refreshJob.schedule();
	}

	private class BehaviourRefreshJob extends Job {

		public BehaviourRefreshJob(String label) {
			super(label);
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {
			try {
				CloudFoundryServer cloudServer = null;
				IModule module = opToRun.getModule();

				try {
					cloudServer = opToRun.getBehaviour() != null ? opToRun.getBehaviour().getCloudFoundryServer()
							: null;
				}
				catch (CoreException ce) {
					CloudFoundryPlugin.logError(ce);
				}

				try {
					opToRun.run(monitor);
				}
				catch (Throwable t) {
					// Cloud server must not be null as it's the source of
					// the event
					if (cloudServer == null) {
						CloudFoundryPlugin.logError(
								NLS.bind(Messages.RefreshModulesHandler_EVENT_CLOUD_SERVER_NULL, opToRun.getClass()));
					}
					else {
						ServerEventHandler.getDefault().fireError(cloudServer, module,
								CloudFoundryPlugin.getErrorStatus(Messages.RefreshModulesHandler_REFRESH_FAILURE, t));

					}
				}
			}
			finally {
				opToRun = null;
			}

			return Status.OK_STATUS;
		}
	}

}