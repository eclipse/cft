/*******************************************************************************
 * Copyright (c) 2016 Pivotal Software, Inc. and others
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
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * Updates all modules and services in the server
 *
 */
public class UpdateAllOperation extends BehaviourOperation {

	public UpdateAllOperation(CloudFoundryServerBehaviour behaviour) {
		super(behaviour, null);
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {

		CloudFoundryServer cloudServer = getBehaviour().getCloudFoundryServer();
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask(NLS.bind(Messages.CloudBehaviourOperations_REFRESHING_APPS_AND_SERVICES,
				cloudServer.getServer().getId()), 100);
		
		// Get updated list of services
		List<CFServiceInstance> services = getBehaviour().getServices(subMonitor.newChild(20));

		
		// Split refresh of apps into two parts:
		
		// 1. Faster update of apps with basic info to refresh Servers view quicker
		List<CloudApplication> applications = updateBasicListOfApps(cloudServer, subMonitor.newChild(30));
		
		// Notify UI to refresh the basic list of apps
		fireRefreshEvent(services);
		
		// 2. Slower update of apps with stats, service bindings, etc..
		updateCompleteApps(applications, cloudServer, subMonitor.newChild(70));
		
		fireRefreshEvent(services);
	

		subMonitor.worked(20);
	}
	
	protected void fireRefreshEvent(List<CFServiceInstance> services) throws CoreException {
		ServerEventHandler.getDefault().fireServerEvent(new AppsAndServicesRefreshEvent(getBehaviour().getCloudFoundryServer(),
				getModule(), CloudServerEvent.EVENT_SERVER_REFRESHED, services));
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

		cloudServer.addAndDeleteModules(deployedApplicationsByName, stats);

		// Skip modules that are starting
		cloudServer.updateModulesState(new int[] { IServer.STATE_STARTING });

		return applications;
	}

	protected void updateCompleteApps(List<CloudApplication> applications, CloudFoundryServer cloudServer,
			SubMonitor subMonitor) throws CoreException {
		Map<String, CloudApplication> deployedApplicationsByName = new LinkedHashMap<String, CloudApplication>();
		Map<String, ApplicationStats> stats = new LinkedHashMap<String, ApplicationStats>();

		for (CloudApplication toUpdate : applications) {
			CFV1Application updatedApplication = getBehaviour().getCompleteApplication(toUpdate, subMonitor);
			if (updatedApplication.getStats() != null) {
				stats.put(toUpdate.getName(), updatedApplication.getStats());
			}
			deployedApplicationsByName.put(toUpdate.getName(), updatedApplication.getApplication());
			subMonitor.worked(1);
		}

		cloudServer.addAndDeleteModules(deployedApplicationsByName, stats);

		// Skip modules that are starting
		cloudServer.updateModulesState(new int[] { IServer.STATE_STARTING });

		// Clear publish error
		for (IModule module : cloudServer.getServer().getModules()) {
			CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);
			if (appModule != null) {
				appModule.setStatus(null);
				appModule.validateDeploymentInfo();
			}
		}
	}
}
