/*******************************************************************************
 * Copyright (c) 2015, 2017 Pivotal Software, Inc. and others
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

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;
import org.eclipse.cft.server.core.CFApplicationArchive;
import org.eclipse.cft.server.core.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * Operation publish an application. If the application is already deployed and
 * synchronised, it will only update the mapping between the module and the
 * {@link CloudApplication}.
 * 
 * <p/>
 * 1. Prompts for deployment information.
 * <p/>
 * 2. Creates the application if the application does not currently exist in the
 * server
 * <p/>
 * 3. Starts the application if specified in the deployment configuration for
 * the application.
 * <p/>
 * If the application is already published (it exists in the server), it will
 * ONLY update the published cloud application mapping in the
 * {@link CloudFoundryApplicationModule}. It will NOT re-create, re-publish, or
 * restart the application.
 * <p/>
 *
 */
public class PushApplicationOperation extends StartOperation {

	/**
	 * 
	 */

	public PushApplicationOperation(CloudFoundryServerBehaviour behaviour, IModule[] modules, boolean clearConsole) {
		super(behaviour, false, modules, clearConsole);
	}

	@Override
	protected DeploymentConfiguration prepareForDeployment(CloudFoundryApplicationModule appModule,
			IProgressMonitor monitor) throws CoreException {
		// If the app is already published, just refresh the application
		// mapping.
		int moduleState = getBehaviour().getServer()
				.getModulePublishState(new IModule[] { appModule.getLocalModule() });
		if (appModule.isDeployed() && moduleState == IServer.PUBLISH_STATE_NONE) {

			getBehaviour().printlnToConsole(appModule, Messages.CONSOLE_APP_FOUND);

			getBehaviour().printlnToConsole(appModule,
					NLS.bind(Messages.CONSOLE_APP_MAPPING_STARTED, appModule.getDeployedApplicationName()));
			try {
				getBehaviour().updateModuleWithBasicCloudInfo(appModule.getDeployedApplicationName(), monitor);
				getBehaviour().printlnToConsole(appModule,
						NLS.bind(Messages.CONSOLE_APP_MAPPING_COMPLETED, appModule.getDeployedApplicationName()));

			}
			catch (CoreException e) {
				// For diagnostic purposes, log as info only.
				CloudFoundryPlugin.log(new Status(Status.INFO, CloudFoundryPlugin.PLUGIN_ID,
						"CoreException thrown during deployment.", e)); // $NON-NLS-1$

				// Do not throw the error. The application may not exist
				// anymore. If it is a network error, it will become evident
				// in further steps
			}

		}
		else {
			CloudFoundryServer cloudServer = getBehaviour().getCloudFoundryServer();

			// prompt user for missing details
			return CloudFoundryPlugin.getCallback().prepareForDeployment(cloudServer, appModule, monitor);
		}
		return null;
	}
	
	

	@Override
	protected void onOperationCanceled(OperationCanceledException e, IProgressMonitor monitor) throws CoreException {

		// Bug 501186: When publish operation is canceled, ensure that the
		// unpublished module is properly handled at the server level. The
		// server always needs to be synchronized with Cloud Foundry, therefore
		// an update on the module will remove it from the server if the
		// associated CF app was never created
		getBehaviour().updateModuleWithAllCloudInfo(getFirstModule(), monitor);

		// Change in behavior:
		// NOTE - Set monitor canceled AFTER updating the module above, as to not cancel the update module operation:
		// If the CF wizard  (callback prepareForDeployment) is canceled by the user, then it should be effectively canceling the monitor
		// for the original publish operation that was initiated in the first place. That way, any adopter code with access to this monitor can
		// check this flag (monitor.isCanceled()) so they can react to the cancel.  One impact on this change is that it will prevent
		// other modules that are in Republish state from being published.  
		// Set monitor to canceled here and not in AbstractPublishApplicationOperation since this operation invokes the wizard.
		monitor.setCanceled(true);

		CloudFoundryPlugin.log(new Status(Status.INFO, CloudFoundryPlugin.PLUGIN_ID,
				"Operation cancelled during prepareForDeployment.", e)); //$NON-NLS-1$
	}

	@Override
	protected void pushApplication(CloudFoundryOperations client, final CloudFoundryApplicationModule appModule,
			CFApplicationArchive applicationArchive, final IProgressMonitor monitor) throws CoreException {
		String appName = appModule.getDeploymentInfo().getDeploymentName();

		CloudApplication existingApp = null;

		try {
			existingApp = getBehaviour().getCloudApplication(appName, monitor);
		}
		catch (CoreException ce) {
			if (!CloudErrorUtil.isNotFoundException(ce)) {
				throw ce;
			}
		}

		// Create the application if it doesn't already exist
		if (existingApp == null) {
			String creatingAppLabel = NLS.bind(Messages.CONSOLE_APP_CREATION, appName);
			getBehaviour().printlnToConsole(appModule, creatingAppLabel);

			// BUG - [87862532]: Fetch all the information BEFORE
			// creating the application. The reason for this
			// is to prevent any other operation that updates the module from
			// clearing the deploymentinfo after the application is created
			// but before other properties are updated like environment
			// variables
			// and instances
			String buildpack = appModule.getDeploymentInfo().getBuildpack();
			List<String> uris = appModule.getDeploymentInfo().getUris() != null
					? appModule.getDeploymentInfo().getUris() : new ArrayList<String>(0);
			List<String> services = appModule.getDeploymentInfo().asServiceBindingList();
			List<EnvironmentVariable> variables = appModule.getDeploymentInfo().getEnvVariables();
			int instances = appModule.getDeploymentInfo().getInstances();		
			String stack = appModule.getDeploymentInfo().getStack();
			Integer timeout = appModule.getDeploymentInfo().getTimeout();
			String command = appModule.getDeploymentInfo().getCommand();

			Staging staging = new Staging(command, buildpack, stack, timeout);
			
			CoreException cloudAppCreationClientError = null;

			// Guard against host taken errors and other errors that may
			// create the app but
			// prevent further deployment. If the app was still created
			// attempt to set env vars and instaces
			SubMonitor subMonitor = SubMonitor.convert(monitor, 50);
			subMonitor.subTask(creatingAppLabel);
			try {
				client.createApplication(appName, staging, appModule.getDeploymentInfo().getDiskQuota(), appModule.getDeploymentInfo().getMemory(), uris, services);
			}
			catch (Exception e) {
				String hostTaken = CloudErrorUtil.getHostTakenError(e);
				if (hostTaken != null) {
					cloudAppCreationClientError = CloudErrorUtil.toCoreException(hostTaken);
				}
				else {
					cloudAppCreationClientError = CloudErrorUtil.toCoreException(e);
				}
			}

			subMonitor.worked(30);

			// [87881946] - Try setting the env vars and instances even if an
			// error was thrown while creating the application
			// as the application may still have been created in the Cloud space
			// in spite of the error
			try {
				CloudApplication actualApp = getBehaviour().getCloudApplication(appName, subMonitor.newChild(20));

				if (actualApp != null) {
					SubMonitor updateMonitor = SubMonitor.convert(subMonitor, 100);
					getBehaviour().getRequestFactory().getUpdateEnvVarRequest(appName, variables).run(updateMonitor.newChild(50));

					// Update instances if it is more than 1. By default, app
					// starts
					// with 1 instance.

					if (instances > 1) {
						getBehaviour().updateApplicationInstances(appName, instances, updateMonitor.newChild(50));
					}
					else {
						updateMonitor.worked(50);
					}
				}
			}
			catch (CoreException ce) {
				if (cloudAppCreationClientError == null) {
					throw ce;
				}
			}

			// Even if application was created in the Cloud space, and env vars
			// and instances set, if an exception
			// was thrown while creating the client, throw it
			if (cloudAppCreationClientError != null) {
				throw cloudAppCreationClientError;
			}

		}
		super.pushApplication(client, appModule, applicationArchive, monitor);
	}

	@Override
	protected void performDeployment(CloudFoundryApplicationModule appModule, IProgressMonitor monitor)
			throws CoreException {
		if (!appModule.isDeployed()) {
			super.performDeployment(appModule, monitor);
		}
	}

}