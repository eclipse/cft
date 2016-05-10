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

import org.eclipse.cft.server.core.ICloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;

/**
 * Operation that focuses on publish operations for a given application. Among
 * the common steps performed by this operation is creating an
 * {@link ICloudFoundryApplicationModule} for the given app if it doesn't
 * already exist.
 */
public abstract class AbstractPublishApplicationOperation extends BehaviourOperation {

	public static String INTERNAL_ERROR_NO_MAPPED_CLOUD_MODULE = "Internal Error: No cloud application module found for: {0} - Unable to deploy or start application"; //$NON-NLS-1$

	private final IModule[] modules;

	protected AbstractPublishApplicationOperation(CloudFoundryServerBehaviour behaviour, IModule[] modules) {
		super(behaviour, modules[0]);
		this.modules = modules;
	}

	protected IModule[] getModules() {
		return modules;
	}

	public abstract String getOperationName();

	/**
	 * Returns non-null Cloud application module mapped to the first module in
	 * the list of modules. If the cloud module module does not exist for the
	 * given module, it will attempt to create it. To avoid re-creating a cloud
	 * application module that may have been deleted, restrict invoking this
	 * method to only operations that start, restart, or update an application.
	 * Should not be called when deleting an application.
	 * @param local WST modules representing app to be deployed.
	 * @return non-null Cloud Application module mapped to the given WST module.
	 * @throws CoreException if no modules specified or mapped cloud application
	 * module cannot be resolved.
	 */
	protected CloudFoundryApplicationModule getOrCreateCloudApplicationModule(IModule[] modules) throws CoreException {

		IModule module = modules[0];

		CloudFoundryServer cloudServer = getBehaviour().getCloudFoundryServer();

		CloudFoundryApplicationModule appModule = cloudServer.getCloudModule(module);

		if (appModule == null) {
			throw CloudErrorUtil.toCoreException(NLS.bind(INTERNAL_ERROR_NO_MAPPED_CLOUD_MODULE, modules[0].getId()));
		}

		return appModule;
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {

		try {
			doApplicationOperation(monitor);
			getBehaviour().getOperationsScheduler().updateOnPublish(getModule());
		}
		catch (OperationCanceledException e) {
			// [492609] - Modules are not correctly marked as "completed" when
			// cancellation occurs.
			// This results in the internal server module cache going out of
			// synch with Cloud Foundry. By telling the cache that module addition has been completed, the cache
			// can be correctly removed
			getBehaviour().getCloudFoundryServer().moduleAdditionCompleted(getModule());
			
			// ignore so webtools does not show an exception
			((Server) getBehaviour().getServer()).setModuleState(getModules(), IServer.STATE_UNKNOWN);

			// If application operations, like Restart, Start, or
			// PushApplication are canceled, then the publish state is
			// 'indeterminate'
			// TODO: Don't reference internal Server class. We need to revisit
			// this change and revert back to the original state.
			((Server) getBehaviour().getServer()).setServerPublishState(IServer.PUBLISH_STATE_INCREMENTAL);
			((Server) getBehaviour().getServer()).setModulePublishState(modules, IServer.PUBLISH_STATE_INCREMENTAL);

			// Record the canceled operation 'description' to the log file.
			CloudFoundryPlugin.logWarning(e.getMessage());

			CloudFoundryServer cloudServer = getBehaviour().getCloudFoundryServer();

			CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(getModule());
			if (appModule != null && e.getMessage() != null) {
				CloudFoundryPlugin.getCallback().printToConsole(cloudServer, appModule,
						NLS.bind(Messages.AbstractPublishApplicationOperation_OPERATION_CANCELED, e.getMessage())
								+ '\n',
						false, false);
			}
		}
		catch (Throwable e) {

			// [492609] - Modules are not correctly marked as "completed" when
			// error occurs.
			// This results in the internal server module cache going out of
			// synch with Cloud Foundry
			// when errors occur during application deployment, making deletion
			// of the application fail
			// as the internal cache does not get correctly updated below since
			// the cache retains obsolete module information when error occurs and still assumes
			// the module is still being added and will prevent it from being
			// deleted. By telling the cache that module addition has been completed, the cache
			// can be correctly updated
			getBehaviour().getCloudFoundryServer().moduleAdditionCompleted(getModule());

			// [486691] - On error, update the module to ensure that the it is
			// in a consistent state with the IServer.
			// If this step is not done, an error that would cause the module
			// NOT to be deployed
			// but still exist in the Cloud can result in unexpected behaviour
			// when deleting the module and attempting the publish operation
			// again (for example, archiving failing or failure to properly
			// prompt for deployment details)
			getBehaviour().operations().updateModule(getModule()).run(monitor);

			CloudFoundryApplicationModule appModule = getBehaviour().getCloudFoundryServer()
					.getExistingCloudModule(getModule());
			if (appModule != null && e instanceof CoreException) {
				appModule.setError((CoreException)e);
			}
			throw e;
		}

	}

	protected abstract void doApplicationOperation(IProgressMonitor monitor) throws CoreException;
}
