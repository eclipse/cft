/*******************************************************************************
 * Copyright (c) 2012, 2016 Pivotal Software, Inc.
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
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.CloudServicesUtil;
import org.eclipse.cft.server.core.internal.client.CFServiceInstance;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.CloudRefreshEvent;
import org.eclipse.cft.server.tests.util.ModulesRefreshListener;
import org.eclipse.core.runtime.NullProgressMonitor;

public class CloudFoundryServicesTest extends AbstractCloudFoundryServicesTest {

	public static final String SERVICE_NAME = "cfEclipseRegressionTestService";

	public void testCreateAndDeleteService() throws Exception {
		CFServiceInstance service = createDefaultService();
		assertServiceExists(service);

		List<CFServiceInstance> existingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected 1 service", existingServices.size() == 1);
		assertEquals(SERVICE_NAME, existingServices.get(0).getName());
		assertEquals("elephantsql", existingServices.get(0).getService());
		assertEquals("turtle", existingServices.get(0).getPlan());

		assertServiceExists(existingServices.get(0));

		deleteService(service);
		assertServiceNotExist(SERVICE_NAME);

		existingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected empty list of services", existingServices.isEmpty());
	}

	public void testServiceBindingInDeploymentInfo() throws Exception {
		CFServiceInstance serviceToCreate = createDefaultService();
		String prefix = "testServiceBindingInDeploymentInfo";
		createWebApplicationProject();

		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp, harness.getDefaultBuildpack());

		CloudApplication app = appModule.getApplication();

		getBindServiceOp(appModule, serviceToCreate).run(new NullProgressMonitor());

		assertServiceBound(serviceToCreate.getName(), app);

		// Get Updated cloud module
		CloudFoundryApplicationModule updatedModule = cloudServer.getBehaviour()
				.updateDeployedModule(appModule.getLocalModule(), new NullProgressMonitor());
		List<CFServiceInstance> boundServices = updatedModule.getDeploymentInfo().getServices();
		assertEquals(1, boundServices.size());
		assertEquals(serviceToCreate.getName(), boundServices.get(0).getName());
	}

	public void testServiceBindingUnbindingAppStarted() throws Exception {
		CFServiceInstance service = createDefaultService();

		String prefix = "testServiceBindingUnbindingAppStarted";
		createWebApplicationProject();
		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp, harness.getDefaultBuildpack());

		getBindServiceOp(appModule, service).run(new NullProgressMonitor());

		appModule = cloudServer.getBehaviour().updateDeployedModule(appModule.getLocalModule(),
				new NullProgressMonitor());
		assertServiceBound(service.getName(), appModule.getApplication());
		assertEquals(1, appModule.getDeploymentInfo().getServices().size());
		assertEquals(SERVICE_NAME, appModule.getDeploymentInfo().getServices().get(0).getName());

		getUnbindServiceOp(appModule, service).run(new NullProgressMonitor());

		appModule = cloudServer.getBehaviour().updateDeployedModule(appModule.getLocalModule(),
				new NullProgressMonitor());

		assertServiceNotBound(service.getName(), appModule.getApplication());
		assertEquals(0, appModule.getDeploymentInfo().getServices().size());

	}

	public void testServiceBindingUnbindingAppStopped() throws Exception {
		CFServiceInstance service = createDefaultService();

		String prefix = "testServiceBindingUnbindingAppStopped";
		createWebApplicationProject();

		boolean startApp = false;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp, harness.getDefaultBuildpack());

		getBindServiceOp(appModule, service).run(new NullProgressMonitor());

		appModule = cloudServer.getBehaviour().updateDeployedModule(appModule.getLocalModule(),
				new NullProgressMonitor());
		assertServiceBound(service.getName(), appModule.getApplication());
		assertEquals(1, appModule.getDeploymentInfo().getServices().size());
		assertEquals(SERVICE_NAME, appModule.getDeploymentInfo().getServices().get(0).getName());

		getUnbindServiceOp(appModule, service).run(new NullProgressMonitor());

		appModule = cloudServer.getBehaviour().updateDeployedModule(appModule.getLocalModule(),
				new NullProgressMonitor());
		assertServiceNotBound(service.getName(), appModule.getApplication());
		assertEquals(0, appModule.getDeploymentInfo().getServices().size());
	}

	public void testServiceCreationEvent() throws Exception {
		// Test service creation operation and that asynchronous service
		// creation triggers a service change event

		CFServiceInstance service = getCloudServiceToCreate(SERVICE_NAME, "elephantsql", "turtle");
		serverBehavior.operations().createServices(new CFServiceInstance[] { service })
				.run(new NullProgressMonitor());
		assertServiceExists(service);

		List<CFServiceInstance> existingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertEquals(SERVICE_NAME, existingServices.get(0).getName());
	}

	public void testServiceDeletionEvent() throws Exception {
		// Test service deletion operation and that asynchronous service
		// creation triggers a service change event

		final CFServiceInstance service = createDefaultService();
		assertServiceExists(service);

		String serviceName = service.getName();
		List<String> services = new ArrayList<String>();
		services.add(serviceName);

		serverBehavior.operations().deleteServices(services).run(new NullProgressMonitor());

		assertServiceNotExist(SERVICE_NAME);

		List<CFServiceInstance> existingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected empty list of services", existingServices.isEmpty());
	}

	public void testServiceCreationOp() throws Exception {

		CFServiceInstance toCreate = getCloudServiceToCreate("testServiceCreationOp", "elephantsql", "turtle");
		CFServiceInstance[] services = new CFServiceInstance[] { toCreate };

		serverBehavior.operations().createServices(services).run(new NullProgressMonitor());

		List<CFServiceInstance> existing = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected 1 service", existing.size() == 1);
		assertEquals("testServiceCreationOp", existing.get(0).getName());
		assertEquals("elephantsql", existing.get(0).getService());
		assertEquals("turtle", existing.get(0).getPlan());

		assertServiceExists(existing.get(0));
	}

	public void testServiceDeletionOp() throws Exception {

		CFServiceInstance toCreate = getCloudServiceToCreate("testServiceDeletionOp", "elephantsql", "turtle");
		CFServiceInstance[] services = new CFServiceInstance[] { toCreate };

		serverBehavior.operations().createServices(services).run(new NullProgressMonitor());

		List<CFServiceInstance> existing = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected 1 service", existing.size() == 1);
		assertEquals("testServiceDeletionOp", existing.get(0).getName());
		assertEquals("elephantsql", existing.get(0).getService());
		assertEquals("turtle", existing.get(0).getPlan());

		assertServiceExists(existing.get(0));

		List<String> toDelete = new ArrayList<String>();
		toDelete.add("testServiceDeletionOp");

		serverBehavior.operations().deleteServices(toDelete).run(new NullProgressMonitor());

		List<CFServiceInstance> remainingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected empty list of services", remainingServices.isEmpty());
	}

	public void testServiceCreationServicesInEvent() throws Exception {

		CFServiceInstance toCreate = getCloudServiceToCreate("testServiceCreationServicesInEvent", "elephantsql",
				"turtle");
		CFServiceInstance[] services = new CFServiceInstance[] { toCreate };

		ModulesRefreshListener refreshListener = new ModulesRefreshListener(cloudServer,
				CloudServerEvent.EVENT_UPDATE_SERVICES);

		serverBehavior.operations().createServices(services).run(new NullProgressMonitor());

		assertTrue(refreshListener.modulesRefreshed(new NullProgressMonitor()));
		assertEquals(CloudServerEvent.EVENT_UPDATE_SERVICES, refreshListener.getMatchedEvent().getType());

		assertTrue("Expected " + CloudRefreshEvent.class,
				refreshListener.getMatchedEvent() instanceof CloudRefreshEvent);
		CloudRefreshEvent cloudEvent = (CloudRefreshEvent) refreshListener.getMatchedEvent();
		List<CFServiceInstance> existing = cloudEvent.getServices();
		assertTrue("Expected 1 created service in cloud refresh event", existing.size() == 1);
		assertEquals("testServiceCreationServicesInEvent", existing.get(0).getName());
		assertEquals("elephantsql", existing.get(0).getService());
		assertEquals("turtle", existing.get(0).getPlan());

		assertServiceExists(existing.get(0));

		refreshListener.dispose();
	}

	public void testServiceDeletionServicesInEvent() throws Exception {

		CFServiceInstance service = createCloudService("testServiceDeletionServicesInEvent", "elephantsql",
				"turtle");
		assertServiceExists(service);
		service = createCloudService("testAnotherService", "elephantsql", "turtle");
		List<CFServiceInstance> services = serverBehavior.getServices(new NullProgressMonitor());
		assertEquals("Expected 2 services", 2, services.size());

		ModulesRefreshListener refreshListener = new ModulesRefreshListener(cloudServer,
				CloudServerEvent.EVENT_UPDATE_SERVICES);

		serverBehavior.operations().deleteServices(Arrays.asList(new String[] { "testServiceDeletionServicesInEvent" }))
				.run(new NullProgressMonitor());

		assertTrue(refreshListener.modulesRefreshed(new NullProgressMonitor()));
		assertEquals(CloudServerEvent.EVENT_UPDATE_SERVICES, refreshListener.getMatchedEvent().getType());
		assertTrue("Expected " + CloudRefreshEvent.class,
				refreshListener.getMatchedEvent() instanceof CloudRefreshEvent);

		CloudRefreshEvent cloudEvent = (CloudRefreshEvent) refreshListener.getMatchedEvent();
		List<CFServiceInstance> eventServices = cloudEvent.getServices();

		assertTrue("Expected 1 service in cloud refresh event", eventServices.size() == 1);
		assertEquals("testAnotherService", eventServices.get(0).getName());
		assertEquals("elephantsql", eventServices.get(0).getService());
		assertEquals("turtle", eventServices.get(0).getPlan());

		assertServiceExists(eventServices.get(0));

		refreshListener.dispose();
	}

	public void testExternalCreatedServiceRefresh() throws Exception {
		CloudFoundryOperations client = getTestFixture().createExternalClient();
		client.login();
		CFServiceInstance service = getCloudServiceToCreate("testExternalCreatedServiceRefresh", "elephantsql",
				"turtle");

		client.createService(CloudServicesUtil.asLegacyV1Service(service));

		ModulesRefreshListener refreshListener = new ModulesRefreshListener(cloudServer,
				CloudServerEvent.EVENT_SERVER_REFRESHED);

		serverBehavior.getOperationsScheduler().updateAll();

		assertTrue(refreshListener.modulesRefreshed(new NullProgressMonitor()));

		assertEquals(CloudServerEvent.EVENT_SERVER_REFRESHED, refreshListener.getMatchedEvent().getType());
		assertTrue("Expected " + CloudRefreshEvent.class,
				refreshListener.getMatchedEvent() instanceof CloudRefreshEvent);

		CloudRefreshEvent cloudEvent = (CloudRefreshEvent) refreshListener.getMatchedEvent();
		List<CFServiceInstance> eventServices = cloudEvent.getServices();
		assertTrue("Expected 1 service in cloud refresh event", eventServices.size() == 1);
		assertEquals("testExternalCreatedServiceRefresh", eventServices.get(0).getName());
		assertEquals("elephantsql", eventServices.get(0).getService());
		assertEquals("turtle", eventServices.get(0).getPlan());
	}

	public void testExternalCreatedServiceBehaviour() throws Exception {
		CloudFoundryOperations client = getTestFixture().createExternalClient();
		client.login();
		CFServiceInstance service = getCloudServiceToCreate("testExternalCreatedServiceBehaviour", "elephantsql",
				"turtle");

		client.createService(CloudServicesUtil.asLegacyV1Service(service));

		List<CFServiceInstance> existingServices = serverBehavior.getServices(new NullProgressMonitor());
		assertTrue("Expected 1 service", existingServices.size() == 1);
		assertEquals("testExternalCreatedServiceBehaviour", existingServices.get(0).getName());
		assertEquals("elephantsql", existingServices.get(0).getService());
		assertEquals("turtle", existingServices.get(0).getPlan());
	}

	public void testNoService() throws Exception {
		// There should be no service with this name. make sure there was proper
		// tear down
		assertServiceNotExist(SERVICE_NAME);
	}

	protected CFServiceInstance createDefaultService() throws Exception {
		return createCloudService(SERVICE_NAME, "elephantsql", "turtle");
	}
}
