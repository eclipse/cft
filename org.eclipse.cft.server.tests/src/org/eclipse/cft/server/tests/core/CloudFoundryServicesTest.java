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
import org.eclipse.cft.server.core.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.CloudServicesUtil;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.ServicesUpdatedEvent;
import org.eclipse.cft.server.tests.util.CloudFoundryTestUtil;
import org.eclipse.cft.server.tests.util.ModulesRefreshListener;
import org.eclipse.core.runtime.NullProgressMonitor;

public class CloudFoundryServicesTest extends AbstractCloudFoundryServicesTest {

	public void testCreateAndDeleteService() throws Exception {

		String expectedServiceName = harness.getProperties().serviceToCreate().serviceName;
		String expectedServicePlan = harness.getProperties().serviceToCreate().servicePlan;
		String expectedServiceType = harness.getProperties().serviceToCreate().serviceType;

		CFServiceInstance toCreate = asCFServiceInstance(expectedServiceName, expectedServicePlan, expectedServiceType);
		serverBehavior.operations().createServices(new CFServiceInstance[] { toCreate }).run(new NullProgressMonitor());

		// Verify it was created
		List<CFServiceInstance> existingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected 1 service", existingServices.size() == 1);
		assertEquals(expectedServiceName, existingServices.get(0).getName());
		assertEquals(expectedServiceType, existingServices.get(0).getService());
		assertEquals(expectedServicePlan, existingServices.get(0).getPlan());

		// Delete the service
		List<String> toDelete = new ArrayList<String>();
		toDelete.add(expectedServiceName);

		serverBehavior.operations().deleteServices(toDelete).run(new NullProgressMonitor());

		List<CFServiceInstance> remainingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected empty list of services", remainingServices.isEmpty());
	}

	public void testServiceBindingInDeploymentInfo() throws Exception {
		String expectedServiceName = harness.getProperties().serviceToCreate().serviceName;
		String expectedServicePlan = harness.getProperties().serviceToCreate().servicePlan;
		String expectedServiceType = harness.getProperties().serviceToCreate().serviceType;

		CFServiceInstance toCreate = asCFServiceInstance(expectedServiceName, expectedServicePlan, expectedServiceType);
		serverBehavior.operations().createServices(new CFServiceInstance[] { toCreate }).run(new NullProgressMonitor());

		String prefix = "testServiceBindingInDeploymentInfo";
		createWebApplicationProject();

		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp, harness.getDefaultBuildpack());

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

		String prefix = "testSBUAppStarted";
		createWebApplicationProject();
		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp, harness.getDefaultBuildpack());

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

		String prefix = "testSBUAppStopped";
		createWebApplicationProject();

		boolean startApp = false;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp, harness.getDefaultBuildpack());

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
		List<CFServiceInstance> existingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertEquals(expectedServiceName, existingServices.get(0).getName());

		// Verify the event after module refreshed
		assertTrue(refreshListener.modulesRefreshed(new NullProgressMonitor()));
		assertEquals(CloudServerEvent.EVENT_SERVICES_UPDATED, refreshListener.getMatchedEvent().getType());

		assertTrue("Expected " + ServicesUpdatedEvent.class,
				refreshListener.getMatchedEvent() instanceof ServicesUpdatedEvent);

		// Test the event that it contains the correct information
		ServicesUpdatedEvent cloudEvent = (ServicesUpdatedEvent) refreshListener.getMatchedEvent();
		List<CFServiceInstance> serviceInEvent = cloudEvent.getServices();
		assertTrue("Expected 1 created service in cloud refresh event", serviceInEvent.size() == 1);
		assertEquals(expectedServiceName, serviceInEvent.get(0).getName());
		assertEquals(expectedServiceType, serviceInEvent.get(0).getService());
		assertEquals(expectedServicePlan, serviceInEvent.get(0).getPlan());

		refreshListener.dispose();
	}

	public void testServiceDeletionServicesInEvent() throws Exception {

		String expectedServiceName = harness.getProperties().serviceToCreate().serviceName;
		String expectedServicePlan = harness.getProperties().serviceToCreate().servicePlan;
		String expectedServiceType = harness.getProperties().serviceToCreate().serviceType;

		CFServiceInstance toCreate = asCFServiceInstance(expectedServiceName, expectedServicePlan, expectedServiceType);

		serverBehavior.operations().createServices(new CFServiceInstance[] { toCreate }).run(new NullProgressMonitor());

		// Verify it was created
		List<CFServiceInstance> existingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertEquals(expectedServiceName, existingServices.get(0).getName());

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
		List<CFServiceInstance> eventServices = cloudEvent.getServices();

		assertTrue("Expected 0 service in cloud refresh event", eventServices.size() == 0);

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
		List<CFServiceInstance> eventServices = cloudEvent.getServices();
		assertTrue("Expected 1 service in cloud refresh event", eventServices.size() == 1);
		assertEquals(expectedServiceName, eventServices.get(0).getName());
		assertEquals(expectedServiceType, eventServices.get(0).getService());
		assertEquals(expectedServicePlan, eventServices.get(0).getPlan());

		// Finally test the CFT server behaviour services API to ensure that it
		// fetches
		// the externally created service

		List<CFServiceInstance> existingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected 1 service", existingServices.size() == 1);
		assertEquals(expectedServiceName, existingServices.get(0).getName());
		assertEquals(expectedServiceType, existingServices.get(0).getService());
		assertEquals(expectedServicePlan, existingServices.get(0).getPlan());
	}

	public void testNoService() throws Exception {

		// There should be no services by default. make sure there was proper
		// tear down
		List<CFServiceInstance> existingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected 0 services", existingServices.size() == 0);
	}

	public void testServiceBindingDuringAppDeployment() throws Exception {

		String prefix = "testApplicationDeploymentInfo";

		String expectedAppName = harness.getWebAppName(prefix);

		CloudFoundryOperations externalClient = getTestFixture().createExternalClient();
		externalClient.login();

		String expectedServiceName = harness.getProperties().serviceToCreate().serviceName;
		String expectedServicePlan = harness.getProperties().serviceToCreate().servicePlan;
		String expectedServiceType = harness.getProperties().serviceToCreate().serviceType;

		CFServiceInstance toCreate = asCFServiceInstance(expectedServiceName, expectedServicePlan, expectedServiceType);

		List<CFServiceInstance> servicesToBind = new ArrayList<CFServiceInstance>();
		servicesToBind.add(toCreate);
		externalClient.createService(CloudServicesUtil.asLegacyV1Service(toCreate));

		EnvironmentVariable variable = new EnvironmentVariable();
		variable.setVariable("JAVA_OPTS");
		variable.setValue("-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n");
		List<EnvironmentVariable> vars = new ArrayList<EnvironmentVariable>();
		vars.add(variable);

		createWebApplicationProject();

		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix,
				CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, startApp, vars, servicesToBind,
				harness.getDefaultBuildpack());

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

}
