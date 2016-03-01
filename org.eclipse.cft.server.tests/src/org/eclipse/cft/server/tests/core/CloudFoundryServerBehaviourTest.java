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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.InstanceState;
import org.cloudfoundry.client.lib.domain.InstanceStats;
import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.internal.ApplicationInstanceRunningTracker;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.application.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.tests.util.CloudFoundryTestUtil;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 *
 * Each individual test creates only ONE application module, and checks that
 * only one module exists in the server instance.
 * @author Steffen Pingel
 * @author Nieraj Singh
 */
public class CloudFoundryServerBehaviourTest extends AbstractCloudFoundryTest {

	public void testBaseSetupConnect() throws Exception {

		assertEquals(IServer.STATE_STARTED, serverBehavior.getServer().getServerState());
		assertEquals(Collections.emptyList(), Arrays.asList(server.getModules()));
	}

	public void testDisconnect() throws Exception {
		assertEquals(IServer.STATE_STARTED, serverBehavior.getServer().getServerState());
		assertEquals(Collections.emptyList(), Arrays.asList(server.getModules()));
		serverBehavior.disconnect(new NullProgressMonitor());
		assertEquals(IServer.STATE_STOPPED, serverBehavior.getServer().getServerState());
		assertEquals(Collections.emptyList(), cloudServer.getExistingCloudModules());
	}

	public void testApplicationDeploymentInfo() throws Exception {

		String prefix = "testApplicationDeploymentInfo";

		String expectedAppName = harness.getDefaultWebAppName(prefix);

		CloudFoundryOperations client = getTestFixture().createExternalClient();
		client.login();
		CloudService service = getCloudServiceToCreate("sqlService", "elephantsql", "turtle");
		List<CloudService> servicesToBind = new ArrayList<CloudService>();
		servicesToBind.add(service);
		client.createService(service);

		EnvironmentVariable variable = new EnvironmentVariable();
		variable.setVariable("JAVA_OPTS");
		variable.setValue("-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n");
		List<EnvironmentVariable> vars = new ArrayList<EnvironmentVariable>();
		vars.add(variable);

		createWebApplicationProject();

		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix,
				CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, startApp, vars, servicesToBind);

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertNotNull(appModule.getApplication());
		assertNotNull(appModule.getDeploymentInfo());

		CloudApplication actualApp = appModule.getApplication();

		assertEquals(appModule.getDeployedApplicationName(), actualApp.getName());
		assertEquals(expectedAppName, actualApp.getName());

		ApplicationDeploymentInfo info = appModule.getDeploymentInfo();
		assertEquals(expectedAppName, info.getDeploymentName());

		// Verify that both the deployment info and the actual Cloud application
		// contain
		// the same information
		assertEquals(actualApp.getInstances(), info.getInstances());
		assertEquals(1, info.getInstances());

		String expectedUrl = harness.getExpectedDefaultURL(prefix);
		assertEquals(actualApp.getUris().get(0), info.getUris().get(0));
		assertEquals(expectedUrl, info.getUris().get(0));

		assertEquals(actualApp.getMemory(), info.getMemory());
		assertEquals(CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, info.getMemory());

		assertEquals("JAVA_OPTS", appModule.getDeploymentInfo().getEnvVariables().get(0).getVariable());
		assertEquals("-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n",
				appModule.getDeploymentInfo().getEnvVariables().get(0).getValue());

		assertTrue(actualApp.getEnvAsMap().containsKey("JAVA_OPTS"));
		assertEquals("-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n",
				actualApp.getEnvAsMap().get("JAVA_OPTS"));
		assertEquals("sqlService", appModule.getDeploymentInfo().getServices().get(0).getName());
		assertEquals("sqlService", actualApp.getServices().get(0));

	}

	public void testCloudFoundryModuleCreationNonWSTPublish() throws Exception {
		// Test that a cloud foundry module is created when an application is
		// pushed
		// using framework API rather than test harness API.

		String appPrefix = "testCloudFoundryModuleCreationNonWSTPublish";
		createWebApplicationProject();

		// Configure the test fixture for deployment.
		// This step is a substitute for the Application deployment wizard
		String projectName = harness.getDefaultWebAppProjectName();
		String expectedAppName = harness.getDefaultWebAppName(appPrefix);
		getTestFixture().configureForApplicationDeployment(expectedAppName,
				CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, false);

		IModule module = getModule(projectName);

		assertNotNull("Expected non-null IModule when deploying application", module);

		// Publish with non-WST publish API (i.e. publish that is not invoked by
		// WST framework)
		serverBehavior.publishAdd(module.getName(), new NullProgressMonitor());

		Collection<CloudFoundryApplicationModule> appModules = cloudServer.getExistingCloudModules();
		assertNotNull("Expected list of cloud modules after deploying: " + appPrefix, appModules);
		assertTrue("Expected one application module for " + appPrefix + " but got: " + appModules.size(),
				appModules.size() == 1);

		CloudFoundryApplicationModule applicationModule = appModules.iterator().next();
		assertEquals(expectedAppName, applicationModule.getDeployedApplicationName());

	}

	public void testCloudFoundryModuleCreationWSTPublish() throws Exception {
		// Test that a cloud foundry module is created when an application is
		// pushed
		// using framework API rather than test harness API.

		String appPrefix = "testCloudFoundryModuleCreationWSTPublish";
		createWebApplicationProject();

		// Configure the test fixture for deployment.
		// This step is a substitute for the Application deployment wizard
		String projectName = harness.getDefaultWebAppProjectName();
		String expectedAppName = harness.getDefaultWebAppName(appPrefix);
		getTestFixture().configureForApplicationDeployment(expectedAppName,
				CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, false);

		IModule module = getModule(projectName);

		assertNotNull("Expected non-null IModule when deploying application", module);

		// Publish through WST publish method
		serverBehavior.publish(IServer.PUBLISH_INCREMENTAL, new NullProgressMonitor());

		Collection<CloudFoundryApplicationModule> appModules = cloudServer.getExistingCloudModules();
		assertNotNull("Expected list of cloud modules after deploying: " + appPrefix, appModules);
		assertTrue("Expected one application module for " + appPrefix + " but got: " + appModules.size(),
				appModules.size() == 1);

		CloudFoundryApplicationModule applicationModule = appModules.iterator().next();
		assertEquals(expectedAppName, applicationModule.getDeployedApplicationName());

	}

	public void testModuleStartStateInServer() throws Exception {
		// Tests whether the helper method to deploy and start an app matches
		// expected
		// app state.
		String prefix = "testModuleStartStateInServer";
		createWebApplicationProject();

		boolean startApp = true;

		deployApplication(prefix, startApp);

		// Verify it is deployed
		CloudFoundryApplicationModule appModule = assertApplicationIsDeployed(prefix);
		assertNotNull("Expected non-null Cloud Foundry application module", appModule);

		IModule module = getModule(harness.getDefaultWebAppProjectName());

		// Now CHECK that the expected conditions in the helper method assert to
		// expected values
		appModule = assertCloudFoundryModuleExists(module, prefix);

		assertNotNull(
				"No Cloud Application mapping in Cloud module. Application mapping failed or application did not deploy",
				appModule.getApplication());

		assertEquals(IServer.STATE_STARTED, appModule.getState());
		assertEquals(AppState.STARTED, appModule.getApplication().getState());

		// Check the module state in the WST server is correct
		int moduleState = server.getModuleState(new IModule[] { module });
		assertEquals(IServer.STATE_STARTED, moduleState);

	}

	public void testCreateDeployAppHelpersStopMode() throws Exception {
		String prefix = "testCreateDeployAppHelpersStopMode";
		createWebApplicationProject();

		boolean startApp = false;

		deployApplication(prefix, startApp);

		// Invoke the helper method
		CloudFoundryApplicationModule appModule = assertApplicationIsDeployed(prefix);
		assertNotNull("Expected non-null Cloud Foundry application module", appModule);

		// Now CHECK that the expected conditions in the helper method assert to
		// expected values
		IModule module = getModule(harness.getDefaultWebAppProjectName());

		appModule = assertCloudFoundryModuleExists(module, prefix);

		assertNotNull(
				"No Cloud Application mapping in Cloud module. Application mapping failed or application did not deploy",
				appModule.getApplication());

		assertEquals(IServer.STATE_STOPPED, appModule.getState());
		assertEquals(AppState.STOPPED, appModule.getApplication().getState());

		// Check the module state in the WST server is correct
		int moduleState = server.getModuleState(new IModule[] { module });
		assertEquals(IServer.STATE_STOPPED, moduleState);

		int state = new ApplicationInstanceRunningTracker(appModule, cloudServer).track(new NullProgressMonitor());

		assertTrue("Expected application to be stopped, but server behaviour indicated it is running",
				state == IServer.STATE_STOPPED);

	}

	public void testWSTBehaviourStopModule() throws Exception {

		// Tests WST overridden method in Cloud behaviour to stop modules
		String prefix = "testWSTBehaviourStopModule";
		createWebApplicationProject();
		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp);

		assertTrue("Expected application to be started",
				appModule.getApplication().getState().equals(AppState.STARTED));

		serverBehavior.stopModule(new IModule[] { appModule.getLocalModule() }, new NullProgressMonitor());

		// Check stop values are correct both in the CloudApplication and the
		// Cloud module wrapper
		assertTrue("Expected application to be stopped",
				appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == IServer.STATE_STOPPED);
	}

	public void testStartAndStopModule() throws Exception {
		String prefix = "testStartAndStopModule";
		createWebApplicationProject();
		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp);

		assertTrue("Expected application to be started", appModule.getState() == IServer.STATE_STARTED);

		serverBehavior.stopModule(new IModule[] { appModule.getLocalModule() }, new NullProgressMonitor());

		assertTrue("Expected application to be stopped",
				appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == IServer.STATE_STOPPED);

		serverBehavior.startModule(new IModule[] { appModule.getLocalModule() }, new NullProgressMonitor());

		assertEquals(IServer.STATE_STARTED, appModule.getState());
		assertEquals(AppState.STARTED, appModule.getApplication().getState());
	}

	public void testApplicationStartedInstanceRunningTracker() throws Exception {
		// Tests the server behaviour API that checks if the application is
		// running
		String prefix = "testApplicationInstanceRunningTracker";
		createWebApplicationProject();

		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp);

		// The tracker should detect the application is started
		int state = new ApplicationInstanceRunningTracker(appModule, cloudServer).track(new NullProgressMonitor());

		assertTrue("Expected application to be started, but instance tracker indicated it is stopped",
				state == IServer.STATE_STARTED);

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		// The following are the expected conditions for the server behaviour to
		// determine that the app is running
		String appName = harness.getDefaultWebAppName(prefix);

		// Verify start states in the module are correct
		assertTrue(appModule.getState() == IServer.STATE_STARTED);
		assertEquals(InstanceState.RUNNING, appModule.getApplicationStats().getRecords().get(0).getState());

		// Check directly via CF that the start states are correct
		List<InstanceStats> stats = serverBehavior.getApplicationStats(appName, new NullProgressMonitor()).getRecords();
		assertEquals(1, stats.size());
		assertEquals(1, serverBehavior.getInstancesInfo(appName, new NullProgressMonitor()).getInstances().size());
		assertEquals(InstanceState.RUNNING, stats.get(0).getState());
	}

	public void testApplicationStoppedInstanceRunningTracker() throws Exception {

		String prefix = "testApplicationStoppedInstanceRunningTracker";
		createWebApplicationProject();

		// Ensure app is deployed in STOP mode
		boolean startApp = false;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp);

		// The tracker should detect the application is started
		int state = new ApplicationInstanceRunningTracker(appModule, cloudServer).track(new NullProgressMonitor());

		assertTrue("Expected application to be stopped, but instance tracker indicates it is started",
				state == IServer.STATE_STOPPED);

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		// Verify start states in the module are correct
		assertTrue(appModule.getState() == IServer.STATE_STOPPED);
		assertTrue(appModule.getApplication().getState() == AppState.STOPPED);
	}

	public void testApplicationModuleRunningState() throws Exception {

		String prefix = "testApplicationModuleRunningState";
		createWebApplicationProject();

		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp);

		// The following are the expected conditions for the server behaviour to
		// determine that the app is running
		String appName = harness.getDefaultWebAppName(prefix);

		// Verify start states in the module are correct
		assertTrue(appModule.getState() == IServer.STATE_STARTED);
		assertEquals(InstanceState.RUNNING, appModule.getApplicationStats().getRecords().get(0).getState());

		// Check directly via CF that the start states are correct
		List<InstanceStats> stats = serverBehavior.getApplicationStats(appName, new NullProgressMonitor()).getRecords();
		assertEquals(1, stats.size());
		assertEquals(1, serverBehavior.getInstancesInfo(appName, new NullProgressMonitor()).getInstances().size());
		assertEquals(InstanceState.RUNNING, stats.get(0).getState());
	}

	public void testStartModuleInvalidPassword() throws Exception {

		String prefix = "testStartModuleInvalidPassword";

		createWebApplicationProject();

		try {
			CloudFoundryServer cloudServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);

			String userName = cloudServer.getUsername();
			CloudCredentials credentials = new CloudCredentials(userName, "invalid-password");
			connectClient(credentials);

			boolean startApp = true;
			deployApplication(prefix, startApp);

			fail("Expected CoreException due to invalid password");
		}
		catch (Throwable e) {
			assertTrue(e.getMessage().contains("403 Access token denied"));
		}

		connectClient();

		// Should now deploy without errors
		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp);
		assertTrue(appModule.getState() == IServer.STATE_STARTED);
	}

	public void testStartModuleInvalidUsername() throws Exception {

		String prefix = "startModuleInvalidUsername";

		createWebApplicationProject();

		try {
			CloudFoundryServer cloudServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);

			CloudCredentials credentials = new CloudCredentials("invalidusername", cloudServer.getPassword());
			connectClient(credentials);

			boolean startApp = true;
			deployApplication(prefix, startApp);

			fail("Expected CoreException due to invalid password");
		}
		catch (Throwable e) {
			assertTrue(e.getMessage().contains("403 Access token denied"));
		}

		connectClient();

		// Should now deploy without errors
		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp);
		assertTrue(appModule.getState() == IServer.STATE_STARTED);
	}

	/*
	 * Creates an application through the Cloud Foundry server instance, and
	 * then deletes the application using an external standalone client not
	 * associated with the Cloud Foundry server instance to simulate deleting
	 * the application outside of Eclipse or the runtime workbench.
	 */
	public void testDeleteModuleExternally() throws Exception {

		String prefix = "testDeleteModuleExternally";
		String appName = harness.getDefaultWebAppName(prefix);
		createWebApplicationProject();

		boolean startApp = true;
		deployApplication(prefix, startApp);

		List<CloudApplication> applications = serverBehavior.getApplications(new NullProgressMonitor());
		boolean found = false;

		for (CloudApplication application : applications) {
			if (application.getName().equals(appName)) {
				found = true;
				break;
			}
		}
		assertTrue(found);

		// Now create a separate external standalone client (external to the WST
		// CF Server instance) to delete the app

		CloudFoundryOperations client = getTestFixture().createExternalClient();

		client.login();
		client.deleteApplication(appName);

		applications = serverBehavior.getApplications(new NullProgressMonitor());
		found = false;

		for (CloudApplication application : applications) {
			if (application.getName().equals(appName)) {
				found = true;
				break;
			}
		}
		assertFalse(found);
	}

	public void testCloudModulesClearedOnDisconnect() throws Exception {
		// Test the following:
		// Create an application and deploy it.
		// Then disconnect and verify that the app module cache is cleared.

		String appPrefix = "testCloudModulesClearedOnDisconnect";
		createWebApplicationProject();
		boolean startApp = true;
		deployApplication(appPrefix, startApp);

		// Cloud module should have been created.
		Collection<CloudFoundryApplicationModule> appModules = cloudServer.getExistingCloudModules();
		assertEquals(harness.getDefaultWebAppName(appPrefix),
				appModules.iterator().next().getDeployedApplicationName());

		serverBehavior.disconnect(new NullProgressMonitor());

		appModules = cloudServer.getExistingCloudModules();

		assertTrue("Expected empty list of cloud application modules after server disconnect", appModules.isEmpty());
	}
}
