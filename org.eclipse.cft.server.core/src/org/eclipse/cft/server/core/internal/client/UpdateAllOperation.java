/*******************************************************************************
 * Copyright (c) 2016, 2017 Pivotal Software, Inc. and others
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;

/**
 * Updates all modules and services in the server
 *
 */
public class UpdateAllOperation extends CFOperation {

	public UpdateAllOperation(CloudFoundryServerBehaviour behaviour) {
		super(behaviour);
	}

	@Override
	public String getOperationName() {
		return Messages.UpdateAllOperation_OPERATION_MESSAGE;
	}

	protected boolean isCanceled(IProgressMonitor monitor) {
		return monitor != null && monitor.isCanceled();
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {

		CloudFoundryServer cloudServer = getBehaviour().getCloudFoundryServer();
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask(NLS.bind(Messages.CloudBehaviourOperations_REFRESHING_APPS_AND_SERVICES,
				cloudServer.getServer().getId()), 100);

		if (isCanceled(subMonitor)) {
			return;
		}

		// Get updated list of services
		List<CFServiceInstance> services = getBehaviour().getServices(subMonitor.newChild(20));
		ServerEventHandler.getDefault().fireServicesUpdated(cloudServer, services);

		if (isCanceled(subMonitor)) {
			return;
		}

		// Split refresh of apps into two parts:

		// 1. Faster update of apps with basic info to refresh Servers view
		// quicker
		List<CloudApplication> applications = updateBasicListOfApps(cloudServer, subMonitor.newChild(30));
		ServerEventHandler.getDefault().fireModulesUpdated(cloudServer, cloudServer.getServer().getModules());

		// 2. Slower update of apps with stats, service bindings, etc..
		updateCompleteApps(applications, cloudServer, subMonitor.newChild(70));

		subMonitor.worked(20);
	}

	protected List<CloudApplication> updateBasicListOfApps(CloudFoundryServer cloudServer, SubMonitor subMonitor)
			throws CoreException {
		// Get updated list of cloud applications from the server
		List<CloudApplication> applications = getBehaviour().getBasicApplications(subMonitor);

		// update applications and deployments from server
		Map<String, CloudApplication> deployedApplicationsByName = new LinkedHashMap<String, CloudApplication>();

		// Empty stats as this is a longer process and will be fetched
		// separately later
		Map<String, ApplicationStats> stats = new LinkedHashMap<String, ApplicationStats>();

		for (CloudApplication application : applications) {
			deployedApplicationsByName.put(application.getName(), application);
		}

		// [496759] - In two-stage refresh, do not update the cloud app mapping in existing modules as the cloud apps
		// in the first "basic" stage has incomplete information and may result in the associated module having validation errors
		// on incomplete information. Instead, wait for the second stage to do a complete Cloud app update in existing modules
		boolean updateCloudMappingInExistingModules = false;
		cloudServer.addAndDeleteModules(deployedApplicationsByName, stats, updateCloudMappingInExistingModules);

		// Skip modules that are starting
		cloudServer.updateModulesState(new int[] { IServer.STATE_STARTING });

		return applications;
	}

	protected void updateCompleteApps(List<CloudApplication> applications, CloudFoundryServer cloudServer,
			SubMonitor subMonitor) throws CoreException {
		if (applications == null) {
			return;
		}
		applications.parallelStream().forEach((cloudApp) -> {
			if (isCanceled(subMonitor)) {
				return;
			}
			try {
				CFV1Application updatedApplication = getBehaviour().getCompleteApplication(cloudApp, subMonitor);
				if (updatedApplication != null && updatedApplication.getStats() != null) {
					CloudFoundryApplicationModule appModule = cloudServer.updateModule(
							updatedApplication.getApplication(), updatedApplication.getApplication().getName(),
							updatedApplication.getStats(), subMonitor);
					if (appModule != null) {
						appModule.validateAndUpdateStatus();
						ServerEventHandler.getDefault().fireModuleUpdated(cloudServer, appModule.getLocalModule());
					}
				}
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
			subMonitor.worked(1);
		});
	}
}
