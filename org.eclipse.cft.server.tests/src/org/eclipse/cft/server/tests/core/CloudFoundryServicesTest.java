/*******************************************************************************
 * Copyright (c) 2012, 2017 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.tests.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.CloudServicesUtil;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.ServicesUpdatedEvent;
import org.eclipse.cft.server.tests.util.CloudFoundryTestUtil;
import org.eclipse.cft.server.tests.util.ModulesRefreshListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;

public class CloudFoundryServicesTest extends AbstractCloudFoundryServicesTest {

	public void testCreateAndDeleteService() throws Exception {

		String expectedServiceName = harness.getProperties().serviceToCreate().serviceName;
		String expectedServicePlan = harness.getProperties().serviceToCreate().servicePlan;
		String expectedServiceType = harness.getProperties().serviceToCreate().serviceType;

		CFServiceInstance toCreate = asCFServiceInstance(expectedServiceName, expectedServicePlan, expectedServiceType);
		serverBehavior.operations().createServices(new CFServiceInstance[] { toCreate }).run(new NullProgressMonitor());

		// Verify it was created
		CFServiceInstance existingService = getExistingCloudService(expectedServiceName);
		assertEquals(expectedServiceName, existingService.getName());
		assertEquals(expectedServiceType, existingService.getService());
		assertEquals(expectedServicePlan, existingService.getPlan());

		// Delete the service
		List<String> toDelete = new ArrayList<String>();
		toDelete.add(expectedServiceName);

		serverBehavior.operations().deleteServices(toDelete).run(new NullProgressMonitor());

		existingService = getExistingCloudService(expectedServiceName);
		assertNull("Expected service to be deleted but it still exists: " + expectedServiceName, existingService);
	}

	public void testServiceBindingInDeploymentInfo() throws Exception {
		String expectedServiceName = harness.getProperties().serviceToCreate().serviceName;
		String expectedServicePlan = harness.getProperties().serviceToCreate().servicePlan;
		String expectedServiceType = harness.getProperties().serviceToCreate().serviceType;

		CFServiceInstance toCreate = asCFServiceInstance(expectedServiceName, expectedServicePlan, expectedServiceType);
		serverBehavior.operations().createServices(new CFServiceInstance[] { toCreate }).run(new NullProgressMonitor());

		String testName = "serviceDeploymentInfo";
		IProject project = createWebApplicationProject();

		boolean startApp = true;
		String expectedAppName = harness.getWebAppName(testName);
		CloudFoundryApplicationModule appModule = deployApplication(expectedAppName, project, startApp,
				harness.getDefaultBuildpack());

		CloudApplication app = appModule.getApplication();

		getBindServiceOp(appModule, toCreate).run(new NullProgressMonitor());

		assertServiceBound(toCreate.getName(), app);

		// Get Updated cloud module
		CloudFoundryApplicationModule updatedModule = cloudServer.getBehaviour()
				.updateDeployedModule(appModule.getLocalModule(), new NullProgressMonitor());
		List<CFServiceInstance> boundServices = updatedModule.getDeploymentInfo().getServices();
		assertEquals(1, boundServices.size());
		assertEquals(toCreate.getName(), boundServices.get(0).getName());
	}

	public void testServiceBindingUnbindingAppStarted() throws Exception {
		String expectedServiceName = harness.getProperties().serviceToCreate().serviceName;
		String expectedServicePlan = harness.getProperties().serviceToCreate().servicePlan;
		String expectedServiceType = harness.getProperties().serviceToCreate().serviceType;

		CFServiceInstance toCreate = asCFServiceInstance(expectedServiceName, expectedServicePlan, expectedServiceType);
		serverBehavior.operations().createServices(new CFServiceInstance[] { toCreate }).run(new NullProgressMonitor());

		String testName = "serviceAppStarted";
		IProject project = createWebApplicationProject();
		boolean startApp = true;
		String expectedAppName = harness.getWebAppName(testName);
		CloudFoundryApplicationModule appModule = deployApplication(expectedAppName, project, startApp,
				harness.getDefaultBuildpack());

		getBindServiceOp(appModule, toCreate).run(new NullProgressMonitor());

		appModule = cloudServer.getBehaviour().updateDeployedModule(appModule.getLocalModule(),
				new NullProgressMonitor());
		assertServiceBound(toCreate.getName(), appModule.getApplication());
		assertEquals(1, appModule.getDeploymentInfo().getServices().size());
		assertEquals(toCreate.getName(), appModule.getDeploymentInfo().getServices().get(0).getName());

		getUnbindServiceOp(appModule, toCreate).run(new NullProgressMonitor());

		appModule = cloudServer.getBehaviour().updateDeployedModule(appModule.getLocalModule(),
				new NullProgressMonitor());

		assertServiceNotBound(toCreate.getName(), appModule.getApplication());
		assertEquals(0, appModule.getDeploymentInfo().getServices().size());

	}

	public void testServiceBindingUnbindingAppStopped() throws Exception {
		String expectedServiceName = harness.getProperties().serviceToCreate().serviceName;
		String expectedServicePlan = harness.getProperties().serviceToCreate().servicePlan;
		String expectedServiceType = harness.getProperties().serviceToCreate().serviceType;

		CFServiceInstance toCreate = asCFServiceInstance(expectedServiceName, expectedServicePlan, expectedServiceType);
		serverBehavior.operations().createServices(new CFServiceInstance[] { toCreate }).run(new NullProgressMonitor());

		String testName = "serviceAppStopped";
		IProject project = createWebApplicationProject();

		boolean startApp = false;
		String expectedAppName = harness.getWebAppName(testName);
		CloudFoundryApplicationModule appModule = deployApplication(expectedAppName, project, startApp,
				harness.getDefaultBuildpack());

		getBindServiceOp(appModule, toCreate).run(new NullProgressMonitor());

		appModule = cloudServer.getBehaviour().updateDeployedModule(appModule.getLocalModule(),
				new NullProgressMonitor());
		assertServiceBound(toCreate.getName(), appModule.getApplication());
		assertEquals(1, appModule.getDeploymentInfo().getServices().size());
		assertEquals(toCreate.getName(), appModule.getDeploymentInfo().getServices().get(0).getName());

		getUnbindServiceOp(appModule, toCreate).run(new NullProgressMonitor());

		appModule = cloudServer.getBehaviour().updateDeployedModule(appModule.getLocalModule(),
				new NullProgressMonitor());
		assertServiceNotBound(toCreate.getName(), appModule.getApplication());
		assertEquals(0, appModule.getDeploymentInfo().getServices().size());
	}

	public void testServiceCreationServicesInEvent() throws Exception {

		String expectedServiceName = harness.getProperties().serviceToCreate().serviceName;
		String expectedServicePlan = harness.getProperties().serviceToCreate().servicePlan;
		String expectedServiceType = harness.getProperties().serviceToCreate().serviceType;

		CFServiceInstance toCreate = asCFServiceInstance(expectedServiceName, expectedServicePlan, expectedServiceType);

		ModulesRefreshListener refreshListener = new ModulesRefreshListener(cloudServer,
				CloudServerEvent.EVENT_SERVICES_UPDATED);

		serverBehavior.operations().createServices(new CFServiceInstance[] { toCreate }).run(new NullProgressMonitor());

		// Verify it was created
		CFServiceInstance existingService = getExistingCloudService(expectedServiceName);
		assertEquals(expectedServiceName, existingService.getName());
		assertEquals(expectedServiceType, existingService.getService());
		assertEquals(expectedServicePlan, existingService.getPlan());

		// Verify the event after module refreshed
		assertTrue(refreshListener.modulesRefreshed(new NullProgressMonitor()));
		assertEquals(CloudServerEvent.EVENT_SERVICES_UPDATED, refreshListener.getMatchedEvent().getType());

		assertTrue("Expected " + ServicesUpdatedEvent.class,
				refreshListener.getMatchedEvent() instanceof ServicesUpdatedEvent);

		// Test the event that it contains the correct information
		ServicesUpdatedEvent cloudEvent = (ServicesUpdatedEvent) refreshListener.getMatchedEvent();
		CFServiceInstance serviceFromEvent = getServiceFromEvent(cloudEvent, expectedServiceName);

		assertEquals(expectedServiceName, serviceFromEvent.getName());
		assertEquals(expectedServiceType, serviceFromEvent.getService());
		assertEquals(expectedServicePlan, serviceFromEvent.getPlan());

		refreshListener.dispose();
	}

	public void testServiceDeletionServicesInEvent() throws Exception {

		String expectedServiceName = harness.getProperties().serviceToCreate().serviceName;
		String expectedServicePlan = harness.getProperties().serviceToCreate().servicePlan;
		String expectedServiceType = harness.getProperties().serviceToCreate().serviceType;

		CFServiceInstance toCreate = asCFServiceInstance(expectedServiceName, expectedServicePlan, expectedServiceType);

		serverBehavior.operations().createServices(new CFServiceInstance[] { toCreate }).run(new NullProgressMonitor());

		// Verify it was created
		CFServiceInstance existingService = getExistingCloudService(expectedServiceName);
		assertEquals(expectedServiceName, existingService.getName());
		assertEquals(expectedServiceType, existingService.getService());
		assertEquals(expectedServicePlan, existingService.getPlan());

		// Add the refresh listener to listen for the deletion event AFTER
		// services has been created, because service
		// creation also fires service events
		ModulesRefreshListener refreshListener = new ModulesRefreshListener(cloudServer,
				CloudServerEvent.EVENT_SERVICES_UPDATED);
		serverBehavior.operations().deleteServices(Arrays.asList(new String[] { expectedServiceName }))
				.run(new NullProgressMonitor());

		assertTrue(refreshListener.modulesRefreshed(new NullProgressMonitor()));
		assertEquals(CloudServerEvent.EVENT_SERVICES_UPDATED, refreshListener.getMatchedEvent().getType());
		assertTrue("Expected " + ServicesUpdatedEvent.class,
				refreshListener.getMatchedEvent() instanceof ServicesUpdatedEvent);

		ServicesUpdatedEvent cloudEvent = (ServicesUpdatedEvent) refreshListener.getMatchedEvent();

		CFServiceInstance serviceFromEvent = getServiceFromEvent(cloudEvent, expectedServiceName);

		assertNull("Deleted service not expected in cloud service refresh event", serviceFromEvent);

		refreshListener.dispose();
	}

	public void testExternallyCreatedService() throws Exception {

		// Create services outside of CFT
		CloudFoundryOperations externalClient = getTestFixture().createExternalClient();
		externalClient.login();

		String expectedServiceName = harness.getProperties().serviceToCreate().serviceName;
		String expectedServicePlan = harness.getProperties().serviceToCreate().servicePlan;
		String expectedServiceType = harness.getProperties().serviceToCreate().serviceType;

		CFServiceInstance toCreate = asCFServiceInstance(expectedServiceName, expectedServicePlan, expectedServiceType);
		externalClient.createService(CloudServicesUtil.asLegacyV1Service(toCreate));

		// Update all operation in the server fires various events, therefore
		// have refresh listeners for
		// the events that are
		// of interest
		ModulesRefreshListener servicesUpdateCompletedListener = new ModulesRefreshListener(cloudServer,
				CloudServerEvent.EVENT_SERVICES_UPDATED);

		ModulesRefreshListener updateCompletedListener = new ModulesRefreshListener(cloudServer,
				CloudServerEvent.EVENT_UPDATE_COMPLETED);

		// Refresh server to update with the externally created services
		serverBehavior.asyncUpdateAll();

		// Wait for the async op to complete
		assertTrue(servicesUpdateCompletedListener.modulesRefreshed(new NullProgressMonitor()));
		assertTrue(updateCompletedListener.modulesRefreshed(new NullProgressMonitor()));

		// Test the refresh events contain the externally created service
		assertEquals(CloudServerEvent.EVENT_UPDATE_COMPLETED, updateCompletedListener.getMatchedEvent().getType());
		assertEquals(CloudServerEvent.EVENT_SERVICES_UPDATED,
				servicesUpdateCompletedListener.getMatchedEvent().getType());

		assertTrue("Expected " + ServicesUpdatedEvent.class,
				servicesUpdateCompletedListener.getMatchedEvent() instanceof ServicesUpdatedEvent);

		ServicesUpdatedEvent cloudEvent = (ServicesUpdatedEvent) servicesUpdateCompletedListener.getMatchedEvent();
		CFServiceInstance serviceFromEvent = getServiceFromEvent(cloudEvent, expectedServiceName);

		assertEquals(expectedServiceName, serviceFromEvent.getName());
		assertEquals(expectedServiceType, serviceFromEvent.getService());
		assertEquals(expectedServicePlan, serviceFromEvent.getPlan());

		// Finally test the CFT server behaviour services API to ensure that it
		// fetches
		// the externally created service

		CFServiceInstance existingService = getExistingCloudService(expectedServiceName);
		assertEquals(expectedServiceName, existingService.getName());
		assertEquals(expectedServiceType, existingService.getService());
		assertEquals(expectedServicePlan, existingService.getPlan());
	}

	public void testNoService() throws Exception {

		String expectedServiceName = harness.getProperties().serviceToCreate().serviceName;
		// There should be no services by default to test that there was proper
		// tear down
		CFServiceInstance existingService = getExistingCloudService(expectedServiceName);
		assertNull("Expected test service not to exist: " + expectedServiceName, existingService);

	}

	public void testServiceBindingDuringAppDeployment() throws Exception {

		String testName = "serviceAppDeployment";

		String expectedAppName = harness.getWebAppName(testName);

		CloudFoundryOperations externalClient = getTestFixture().createExternalClient();
		externalClient.login();

		String expectedServiceName = harness.getProperties().serviceToCreate().serviceName;
		String expectedServicePlan = harness.getProperties().serviceToCreate().servicePlan;
		String expectedServiceType = harness.getProperties().serviceToCreate().serviceType;

		CFServiceInstance toCreate = asCFServiceInstance(expectedServiceName, expectedServicePlan, expectedServiceType);

		List<CFServiceInstance> servicesToBind = new ArrayList<CFServiceInstance>();
		servicesToBind.add(toCreate);
		externalClient.createService(CloudServicesUtil.asLegacyV1Service(toCreate));

		IProject project = createWebApplicationProject();

		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(expectedAppName, project,
				CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, CloudFoundryTestUtil.DEFAULT_TEST_DISK_QUOTA, startApp,
				/* no vars */ null, servicesToBind, harness.getDefaultBuildpack());

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertNotNull(appModule.getApplication());
		assertNotNull(appModule.getDeploymentInfo());

		CloudApplication actualApp = appModule.getApplication();

		assertEquals(appModule.getDeployedApplicationName(), actualApp.getName());
		assertEquals(expectedAppName, actualApp.getName());

		ApplicationDeploymentInfo info = appModule.getDeploymentInfo();
		assertEquals(expectedAppName, info.getDeploymentName());

		assertEquals(expectedServiceName, appModule.getDeploymentInfo().getServices().get(0).getName());
		assertEquals(expectedServiceName, actualApp.getServices().get(0));

	}

	protected CFServiceInstance getServiceFromEvent(ServicesUpdatedEvent cloudEvent, String expectedServiceName) {
		List<CFServiceInstance> services = cloudEvent.getServices();
		if (services != null) {
			for (CFServiceInstance cfServiceInstance : services) {
				if (cfServiceInstance.getName().equals(expectedServiceName)) {
					return cfServiceInstance;
				}
			}
		}
		return null;
	}

}
