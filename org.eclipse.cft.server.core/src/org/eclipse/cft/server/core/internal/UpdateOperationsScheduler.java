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
 * Schedules update operations on the cloud Server asynchronously. For example,
 * updating a single modules, or all modules in the associated Cloud server.
 * 
 * <p/>
 * Only ONE refresh job per server instance is run.
 * 
 * 
 */
public class UpdateOperationsScheduler implements OperationScheduler {

	private BehaviourRefreshJob refreshJob;

	private final CloudFoundryServer cloudServer;

	private BehaviourOperation opToRun;

	/**
	 * 
	 * @param cloudServer may be null if not resolved.
	 */
	public UpdateOperationsScheduler(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
		String serverName = cloudServer != null ? cloudServer.getServer().getId() : "Unknown server"; //$NON-NLS-1$

		String refreshJobLabel = NLS.bind(Messages.RefreshModulesHandler_REFRESH_JOB, serverName);

		this.refreshJob = new BehaviourRefreshJob(refreshJobLabel);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cft.server.core.internal.OperationScheduler#
	 * getCurrentOperation()
	 */
	@Override
	public synchronized BehaviourOperation getCurrentOperation() {
		return this.opToRun;
	}

	/**
	 * Updates all modules in the server, as well as services
	 */
	public synchronized void updateAll() {
		scheduleRefresh(cloudServer.getBehaviour().operations().updateAll());
	}

	/**
	 * Schedule an update on a deployed module. If module is not deployed, no
	 * refresh will occur.
	 * @see CloudBehaviourOperations#updateDeployedModule(IModule)
	 * @param module to refresh
	 */
	public synchronized void updateDeployedModule(IModule module) {
		scheduleRefresh(cloudServer.getBehaviour().operations().updateDeployedModule(module));
	}

	/**
	 * Schedule an update on a module regardless if it is deployed or no.
	 * @see CloudBehaviourOperations#updateModule(IModule)
	 */
	public synchronized void updateModule(IModule module) {
		scheduleRefresh(cloudServer.getBehaviour().operations().updateModule(module));
	}

	/**
	 * * Updates module and notifies that module has been updated after publish.
	 * This generates a different event than
	 * {@link #updateDeployedModule(IModule)} specific to publishing
	 * @param module
	 */
	public synchronized void updateModuleAfterPublish(IModule module) {
		scheduleRefresh(cloudServer.getBehaviour().operations().updateOnPublish(module));
	}

	private synchronized void scheduleRefresh(BehaviourOperation opToRun) {
		if (this.opToRun == null) {
			this.opToRun = opToRun;
			schedule();
		}
	}

	private void schedule() {
		// Must be visible in progress bar as it can be long running op
		refreshJob.setSystem(false);

		refreshJob.schedule();
	}

	private class BehaviourRefreshJob extends Job {

		public BehaviourRefreshJob(String label) {
			super(label);
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {

			IModule module = null;
			try {
				CloudFoundryServer cloudServer = null;
				module = opToRun.getModule();

				try {
					cloudServer = opToRun.getBehaviour() != null ? opToRun.getBehaviour().getCloudFoundryServer()
							: null;
				}
				catch (CoreException ce) {
					CloudFoundryPlugin.logError(ce);
					return ce.getStatus();
				}

				// Cloud server must not be null as it's the source of
				// the event
				if (cloudServer == null) {
					IStatus error = CloudFoundryPlugin.getErrorStatus(
							NLS.bind(Messages.RefreshModulesHandler_EVENT_CLOUD_SERVER_NULL, opToRun.getClass()));
					CloudFoundryPlugin.log(error);
					return error;
				}

				// At this stage, cloud server is NOT null
				runOperation(module, cloudServer, monitor);
			}
			finally {
				opToRun = null;
			}

			return Status.OK_STATUS;
		}

		/**
		 * 
		 * @param monitor
		 * @param module optionally null if operation is not being performed on
		 * a single module
		 * @param cloudServer must NOT be null
		 */
		protected void runOperation(IModule module, CloudFoundryServer cloudServer, IProgressMonitor monitor) {
			IStatus errorStatus = null;
			try {
				ServerEventHandler.getDefault().fireUpdateStarting(cloudServer);
				opToRun.run(monitor);
			}
			catch (Throwable t) {
				cloudServer.setAndSaveToken(null);
				errorStatus = CloudFoundryPlugin.getErrorStatus(Messages.RefreshModulesHandler_REFRESH_FAILURE, t);
			}
			finally {
				if (errorStatus != null) {
					ServerEventHandler.getDefault().fireError(cloudServer, module, errorStatus);
				}
				else {
					ServerEventHandler.getDefault().fireUpdateCompleted(cloudServer);
				}
			}
		}
	}

}