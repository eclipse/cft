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
package org.eclipse.cft.server.core.internal.jrebel;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.CloudServerListener;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.application.ModuleChangeEvent;
import org.eclipse.cft.server.core.internal.client.AppUrlChangeEvent;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;

/**
 * Note: This class is in core plugin due to dependencies on springframework
 * http libraries which cannot be exported outside of the core plugin. Therefore
 * this is the reason that this server integration is "split" between Core and
 * UI components
 *
 */
public abstract class CFRebelServerIntegration implements CloudServerListener {

	abstract public void register();

	// Order in registry determines invocation priority.
	private final CFRebelServerUrlHandler[] HANDLER_REGISTRY = new CFRebelServerUrlHandler[] {
			new CloudRebelServerUrlHandler()};

	@Override
	public void serverChanged(CloudServerEvent event) {

		if (event.getServer() != null && (event.getType() == CloudServerEvent.EVENT_JREBEL_REMOTING_UPDATE
				|| JRebelIntegrationUtility.shouldReplaceRemotingUrl(event.getType()))) {

			List<IModule> modules = new ArrayList<IModule>();

			if (event instanceof ModuleChangeEvent) {
				final ModuleChangeEvent moduleEvent = (ModuleChangeEvent) event;

				final IModule module = moduleEvent.getModule();

				if (module != null) {
					modules.add(module);
				}
			}

			if (!modules.isEmpty()) {
				updateModulesWithRebelProjects(modules, event);
			}
		}
	}

	/**
	 * 
	 * @param modules must not be null.
	 * @param event
	 */
	protected void updateModulesWithRebelProjects(List<IModule> modules, CloudServerEvent event) {

		for (IModule module : modules) {
			final IModule mod = module;
			final CloudServerEvent moduleEvent = event;
			// Only check remoting agent if it is a JRebel project as the
			// remoting check may
			// require multiple requests to the Cloud and may be a slow running
			// operation
			if (JRebelIntegrationUtility.isJRebelEnabled(module)) {

				final String consoleMessage = NLS.bind(Messages.CFRebelServerIntegration_UPDATING_JREBEL_REMOTING,
						module.getName());
				Job job = new Job(consoleMessage) {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							handleRebelProject(moduleEvent, mod, consoleMessage, monitor);
						}
						catch (CoreException e) {
							CloudFoundryPlugin.logError(e);
							return e.getStatus();
						}

						return Status.OK_STATUS;
					}
				};

				job.schedule();
			}
		}
	}

	protected void handleRebelProject(CloudServerEvent event, IModule module, String consoleMessage,
			IProgressMonitor monitor) throws CoreException {

		CloudFoundryServer cloudServer = event.getServer();

		if (consoleMessage != null) {
			CFRebelConsoleUtil.printToConsole(module, cloudServer, consoleMessage);
		}

		int eventType = event.getType();
		List<String> oldUrls = null;
		List<String> currentUrls = null;
		if (event instanceof AppUrlChangeEvent) {
			AppUrlChangeEvent appUrlEvent = (AppUrlChangeEvent) event;
			oldUrls = appUrlEvent.getOldUrls();
			currentUrls = appUrlEvent.getCurrentUrls();
		}
		else {

			// Get cached module first
			CloudFoundryApplicationModule cloudAppModule = cloudServer.getExistingCloudModule(module);

			if (cloudAppModule != null) {

				CFRebelConsoleUtil.printToConsole(cloudAppModule, cloudServer,
						Messages.CFRebelServerIntegration_UPDATING_APP_MODULE);

				// Update the module to get the latest URLS in Cloud Foundry
				cloudAppModule = cloudServer.getBehaviour().updateDeployedModule(module, monitor);

				CFRebelConsoleUtil.printToConsole(cloudAppModule, cloudServer,
						Messages.CFRebelServerIntegration_UPDATED_APP_MODULE);

				if (cloudAppModule != null && cloudAppModule.getDeploymentInfo() != null) {
					currentUrls = cloudAppModule.getDeploymentInfo().getUris();
				}
			}

			if (cloudAppModule == null) {
				String errorMessage = "Unable to update JRebel server URL for  " + module.getName() //$NON-NLS-1$
						+ ". No cloud application module found.  Please refresh the Cloud server instance " //$NON-NLS-1$
						+ cloudServer.getServer().getId() + " and try again."; //$NON-NLS-1$
				CloudFoundryPlugin.logError(errorMessage); 
				return;
			}
		}

		updateUrls(cloudServer, eventType, module, oldUrls, currentUrls, monitor);
	}

	private void updateUrls(CloudFoundryServer cloudServer, int eventType, IModule module, List<String> oldUrls,
			List<String> currentUrls, IProgressMonitor monitor) {
		CFRebelServerUrlHandler[] registry = getServerUrlRegistry();
		for (CFRebelServerUrlHandler handler : registry) {
			if (handler.updateUrls(cloudServer, eventType, module, oldUrls, currentUrls, monitor)) {
				break;
			}
		}
	}

	protected CFRebelServerUrlHandler[] getServerUrlRegistry() {
		return HANDLER_REGISTRY;
	}

}
