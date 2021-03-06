/*******************************************************************************
 * Copyright (c) 2014, 2017 Pivotal Software, Inc. and others
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

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.cft.server.core.internal.application.ModuleChangeEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;

/**
 * Deletes a given set of application modules. The modules need not have
 * associated deployed applications.
 */
public class DeleteModulesOperation extends ModulesOperation {

	/**
	 * 
	 */

	private final boolean deleteServices;

	private final IModule[] modules;

	public DeleteModulesOperation(CloudFoundryServerBehaviour cloudFoundryServerBehaviour, IModule[] modules,
			boolean deleteServices) {
		super(cloudFoundryServerBehaviour, modules.length > 0 ? modules[0] : null);
		this.modules = modules;
		this.deleteServices = deleteServices;
	}

	@Override
	public void runOnVerifiedModule(IProgressMonitor monitor) throws CoreException {
		doDelete(monitor);
		getBehaviour().asyncUpdateModuleAfterPublish(getFirstModule());
	}

	protected void doDelete(IProgressMonitor monitor) throws CoreException {
		final CloudFoundryServer cloudServer = getBehaviour().getCloudFoundryServer();

		List<String> failedToDeleteApps = new ArrayList<String>();
		Throwable failedDeleteError = null;
		for (IModule module : modules) {
			final CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);

			// Note: the isDeployed check is for:
			// [485228] Attempting to publish (then cancelling) a Web project with the
			// same name as a running Bluemix app. Take care NOT to modify this without thorough testing
			// Skip applications which are not deployed.
			if (appModule == null || !appModule.isDeployed()) {
				continue;
			}

			// Fetch an updated application. Do not fetch all applications as it
			// may slow down the
			// deletion process, only fetch the app being deleted
			CloudApplication application = null;
			try {
				application = getBehaviour().getCloudApplication(appModule.getDeployedApplicationName(), monitor);
			}
			catch (Throwable t) {
				// Ignore if not found. The app does not exist
				// If it is any other error, like a 500 error or network
				// connection, still proceed with the deletion as to not
				// have the module in the server instance that cannot be
				// deleted, but log the error
				if (!CloudErrorUtil.isNotFoundException(t)) {
					failedDeleteError = t;
					failedToDeleteApps.add(appModule.getDeployedApplicationName());
				}
			}

			// NOTE that modules do NOT necessarily have to have deployed
			// applications, so it's incorrect to assume
			// that any module being deleted will also have a corresponding
			// CloudApplication.
			// A case for this is if a user cancels an application deployment.
			// The
			// IModule would have already been created
			// but there would be no corresponding CloudApplication.

			List<String> servicesToDelete = new ArrayList<String>();

			// ONLY delete a remote application if an application is found.
			if (application != null) {
				List<String> actualServices = application.getServices();
				if (actualServices != null) {
					// This has to be used instead of addAll(..), as
					// there is a chance the list is non-empty but
					// contains null entries
					for (String serviceName : actualServices) {
						if (serviceName != null) {
							servicesToDelete.add(serviceName);
						}
					}
				}

				getBehaviour().deleteApplication(application.getName(), monitor);

			}

			CloudFoundryPlugin.getCallback().stopApplicationConsole(appModule, cloudServer);

			// Delete the module locally
			cloudServer.removeApplication(appModule);

			ServerEventHandler.getDefault()
					.fireServerEvent(new ModuleChangeEvent(getBehaviour().getCloudFoundryServer(),
							CloudServerEvent.EVENT_APP_DELETED, appModule.getLocalModule(), Status.OK_STATUS));

			// Be sure the cloud application mapping is removed
			// in case other components still have a reference to
			// the
			// module
			appModule.setCloudApplication(null);

			// Prompt the user to delete services as well
			if (deleteServices && !servicesToDelete.isEmpty()) {
				CloudFoundryPlugin.getCallback().deleteServices(servicesToDelete, cloudServer);
			}

		}
		if (!failedToDeleteApps.isEmpty() && failedDeleteError != null) {
			String errorMessage = NLS.bind(Messages.DeleteModulesOperation_ERROR_DELETE_APP_MESSAGE, failedToDeleteApps,
					failedDeleteError.getMessage());
			IStatus status = CloudFoundryPlugin.getErrorStatus(errorMessage, failedDeleteError);
			CloudFoundryPlugin.log(status);
		}
	}

	@Override
	public String getOperationName() {
		return Messages.DeleteModulesOperation_OPERATION_MESSAGE;
	}
}