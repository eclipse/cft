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
package org.eclipse.cft.server.tests.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.eclipse.cft.server.core.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.ApplicationAction;
import org.eclipse.cft.server.core.internal.application.ApplicationRunState;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.core.internal.client.DeploymentInfoWorkingCopy;
import org.eclipse.cft.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.cft.server.tests.util.CloudFoundryTestUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.internal.Server;

/**
 * Tests {@link ICloudFoundryOperation} in a target
 * {@link CloudFoundryServerBehaviour} obtained through
 * {@link CloudFoundryServerBehaviour#operations()} as well as refresh events
 * triggered by each of the operations.
 * <p/>
 * This may be a long running test suite as it involves multiple application
 * deployments as well as waiting for refresh operations to complete.
 *
 */
public class BehaviourOperationsTest extends AbstractAsynchCloudTest {

	public void testInstanceUpdate() throws Exception {
		// Test asynchronous Application instance update and that it triggers
		// a module refresh event
		String testName = "instanceUpdate";

		String expectedAppName = harness.getWebAppName(testName);

		IProject project = createWebApplicationProject();
		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(expectedAppName, project, startApp,
				harness.getDefaultBuildpack());

		assertEquals(1, appModule.getApplicationStats().getRecords().size());
		assertEquals(1, appModule.getInstanceCount());
		assertEquals(1, appModule.getInstancesInfo().getInstances().size());
		assertEquals(1, appModule.getDeploymentInfo().getInstances());

		cloudServer.getBehaviour().operations().instancesUpdate(appModule, 2).run(new NullProgressMonitor());

		// Get updated module
		appModule = cloudServer.getBehaviour().updateDeployedModule(appModule.getLocalModule(),
				new NullProgressMonitor());

		assertEquals(2, appModule.getApplicationStats().getRecords().size());
		assertEquals(2, appModule.getInstanceCount());
		assertEquals(2, appModule.getInstancesInfo().getInstances().size());
		assertEquals(2, appModule.getDeploymentInfo().getInstances());

		CloudApplication actualApp = getUpdatedApplication(expectedAppName);

		assertEquals(2, actualApp.getInstances());
	}

	public void testMemoryUpdate() throws Exception {

		String testName = "memoryUpdate";

		IProject project = createWebApplicationProject();
		boolean startApp = true;

		String expectedAppName = harness.getWebAppName(testName);
		CloudFoundryApplicationModule appModule = deployApplication(expectedAppName, project, startApp,
				harness.getDefaultBuildpack());

		final int changedMemory = 678;

		cloudServer.getBehaviour().operations().memoryUpdate(appModule, changedMemory).run(new NullProgressMonitor());

		// NOTE: in CFT, a "deployed" module is an a application that exists in Cloud
		// Foundry and has a
		// KNOWN run state. Apps with UNKNOWN state are not considered "deployed".
		// The issue to take into account when testing memory update, is that any
		// changes to memory require
		// app restaging on the Cloud Foundry side (on newer Cloud Foundry, restaging is
		// automatically done after
		// a memory update). However, during restaging, the app may be in "UNKNOWN"
		// state, so fetching an updated deployed
		// module below (commented out) may result in a NULL module right after
		// performing the memory update above.
		// To check that memory changes took effect and are reflected in the module
		// after
		// the memory update operation above,
		// simply fetch the existing module again and check the memory
		//
		// ISSUE: the commented code below may result in null module if app is in the
		// middle of re-staging
		// due to memory changes above
		// appModule =
		// cloudServer.getBehaviour().updateDeployedModule(appModule.getLocalModule(),
		// new NullProgressMonitor());

		appModule = cloudServer.getExistingCloudModule(appModule.getLocalModule());

		// Verify that the same module has been updated
		assertEquals(changedMemory, appModule.getDeploymentInfo().getMemory());
		assertEquals(changedMemory, appModule.getApplication().getMemory());

		assertEquals(changedMemory, appModule.getApplication().getMemory());
	}

	public void testEnvVarUpdate() throws Exception {

		String testName = "envVarUpdate";

		String expectedAppName = harness.getWebAppName(testName);

		IProject project = createWebApplicationProject();
		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(expectedAppName, project, startApp,
				harness.getDefaultBuildpack());

		EnvironmentVariable variable = new EnvironmentVariable();
		variable.setVariable("JAVA_OPTS");
		variable.setValue("-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n");
		List<EnvironmentVariable> vars = new ArrayList<EnvironmentVariable>();
		vars.add(variable);
		DeploymentInfoWorkingCopy cp = appModule.resolveDeploymentInfoWorkingCopy(new NullProgressMonitor());
		cp.setEnvVariables(vars);
		cp.save();

		cloudServer.getBehaviour().operations().environmentVariablesUpdate(appModule.getLocalModule(),
				appModule.getDeployedApplicationName(), cp.getEnvVariables()).run(new NullProgressMonitor());

		// Get updated module
		appModule = cloudServer.getBehaviour().updateDeployedModule(appModule.getLocalModule(),
				new NullProgressMonitor());
		Map<String, String> actualVars = getUpdatedApplication(expectedAppName).getEnvAsMap();
		assertEquals(vars.size(), actualVars.size());
		Map<String, String> expectedAsMap = new HashMap<String, String>();

		for (EnvironmentVariable v : vars) {
			String actualValue = actualVars.get(v.getVariable());
			assertEquals(v.getValue(), actualValue);
			expectedAsMap.put(v.getVariable(), v.getValue());
		}

		// Also verify that the env vars are set in deployment info
		assertEquals(vars.size(), appModule.getDeploymentInfo().getEnvVariables().size());

		List<EnvironmentVariable> deploymentInfoVars = appModule.getDeploymentInfo().getEnvVariables();

		for (EnvironmentVariable var : deploymentInfoVars) {
			String expectedValue = expectedAsMap.get(var.getVariable());
			assertEquals(var.getValue(), expectedValue);
		}
	}

	public void testAppURLUpdate() throws Exception {

		String testName = "urlUpdate";
		final String expectedAppName = harness.getWebAppName(testName);

		IProject project = createWebApplicationProject();
		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(expectedAppName, project, startApp,
				harness.getDefaultBuildpack());

		String expectedURL = harness.generateAppUrl(expectedAppName);
		assertEquals(expectedURL, appModule.getDeploymentInfo().getUris().get(0));

		String changedURL = harness.generateAppUrl("changedtestAppURLUpdate");
		final List<String> expectedUrls = new ArrayList<String>();
		expectedUrls.add(changedURL);

		cloudServer.getBehaviour().operations().mappedUrlsUpdate(expectedAppName, expectedUrls)
				.run(new NullProgressMonitor());

		// Get updated module
		appModule = cloudServer.getBehaviour().updateDeployedModule(appModule.getLocalModule(),
				new NullProgressMonitor());
		assertEquals(expectedUrls, appModule.getDeploymentInfo().getUris());
		assertEquals(expectedUrls, appModule.getApplication().getUris());
	}

	public void testStopApplication() throws Exception {
		String testName = "stopApp";

		IProject project = createWebApplicationProject();
		// Deploy and start the app without the refresh listener
		boolean startApp = true;
		String expectedAppName = harness.getWebAppName(testName);
		CloudFoundryApplicationModule appModule = deployApplication(expectedAppName, project, startApp,
				harness.getDefaultBuildpack());

		assertTrue("Expected application to be started",
				appModule.getApplication().getState().equals(AppState.STARTED));
		assertTrue("Expected application to be started", appModule.getState() == Server.STATE_STARTED);
		assertTrue("Expected application to be started", appModule.getRunState() == ApplicationRunState.STARTED);

		// Stop the app
		cloudServer.getBehaviour().stopModule(new IModule[] { appModule.getLocalModule() }, new NullProgressMonitor());

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertTrue("Expected application to be stopped", appModule.getRunState().equals(ApplicationRunState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == Server.STATE_STOPPED);

	}

	public void testStartApplication() throws Exception {

		String testName = "startApp";

		IProject project = createWebApplicationProject();
		boolean startApplication = false;
		String expectedAppName = harness.getWebAppName(testName);
		CloudFoundryApplicationModule appModule = deployApplication(expectedAppName, project, startApplication,
				harness.getDefaultBuildpack());

		assertTrue("Expected application to be stopped",
				appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == Server.STATE_STOPPED);
		assertTrue("Expected application to be stopped", appModule.getRunState() == ApplicationRunState.STOPPED);

		cloudServer.getBehaviour().operations().applicationDeployment(appModule, ApplicationAction.START)
				.run(new NullProgressMonitor());

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertTrue("Expected application to be started",
				appModule.getApplication().getState().equals(AppState.STARTED));
		assertTrue("Expected application to be started", appModule.getState() == Server.STATE_STARTED);

		// Verify that instances info is available
		assertEquals("Expected instances information for running app", 1,
				appModule.getInstancesInfo().getInstances().size());
		assertNotNull("Expected instances information for running app",
				appModule.getInstancesInfo().getInstances().get(0).getSince());

		assertEquals("Expected instance stats for running app", 1, appModule.getApplicationStats().getRecords().size());

	}

	public void testRestartApplication() throws Exception {

		String testName = "restartApp";

		IProject project = createWebApplicationProject();
		boolean startApp = false;
		String expectedAppName = harness.getWebAppName(testName);
		CloudFoundryApplicationModule appModule = deployApplication(expectedAppName, project, startApp,
				harness.getDefaultBuildpack());

		assertTrue("Expected application to be stopped",
				appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == Server.STATE_STOPPED);
		assertTrue("Expected application to be stopped", appModule.getRunState() == ApplicationRunState.STOPPED);

		cloudServer.getBehaviour().operations().applicationDeployment(appModule, ApplicationAction.RESTART)
				.run(new NullProgressMonitor());

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertTrue("Expected application to be started",
				appModule.getApplication().getState().equals(AppState.STARTED));
		assertTrue("Expected application to be started", appModule.getState() == Server.STATE_STARTED);
		assertTrue("Expected application to be started", appModule.getRunState() == ApplicationRunState.STARTED);

		// Verify that instances info is available
		assertEquals("Expected instances information for running app", 1,
				appModule.getInstancesInfo().getInstances().size());
		assertNotNull("Expected instances information for running app",
				appModule.getInstancesInfo().getInstances().get(0).getSince());

		assertEquals("Expected instance stats for running app", 1, appModule.getApplicationStats().getRecords().size());

	}

	public void testUpdateRestartApplication() throws Exception {

		String testName = "updateRestart";

		IProject project = createWebApplicationProject();

		boolean startApplication = false;
		String expectedAppName = harness.getWebAppName(testName);
		CloudFoundryApplicationModule appModule = deployApplication(expectedAppName, project, startApplication,
				harness.getDefaultBuildpack());

		assertTrue("Expected application to be stopped",
				appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == Server.STATE_STOPPED);
		assertTrue("Expected application to be stopped", appModule.getRunState() == ApplicationRunState.STOPPED);

		cloudServer.getBehaviour().operations().applicationDeployment(appModule, ApplicationAction.UPDATE_RESTART)
				.run(new NullProgressMonitor());

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertTrue("Expected application to be started",
				appModule.getApplication().getState().equals(AppState.STARTED));
		assertEquals("Expected application to be started", appModule.getState(), Server.STATE_STARTED);
		assertTrue("Expected application to be started", appModule.getRunState() == ApplicationRunState.STARTED);

		// Verify that instances info is available
		assertEquals("Expected instances information for running app", 1,
				appModule.getInstancesInfo().getInstances().size());
		assertNotNull("Expected instances information for running app",
				appModule.getInstancesInfo().getInstances().get(0).getSince());

		assertEquals("Expected instance stats for running app", 1, appModule.getApplicationStats().getRecords().size());
	}

	public void testApplicationStopState() throws Exception {

		String testName = "stopState";
		String expectedAppName = harness.getWebAppName(testName);
		IProject project = createWebApplicationProject();
		boolean startApp = false;
		getTestFixture().configureForApplicationDeployment(expectedAppName,
				CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, startApp);

		IModule module = getWstModule(project.getName());

		cloudServer.getBehaviour().operations().applicationDeployment(new IModule[] { module }, ApplicationAction.PUSH)
				.run(new NullProgressMonitor());

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(expectedAppName);

		// Check all the API that return stopped state, including state
		// in the local server as well as in Cloud Foundry

		// Test the deprecated API. Should still return correct state
		assertTrue("Expected application to be stopped", appModule.getState() == Server.STATE_STOPPED);

		// This is the old v1 client app state.
		assertTrue("Expected application to be stopped in v1 CF client",
				appModule.getApplication().getState().equals(AppState.STOPPED));

		// This is the WST module state in the server
		assertTrue("Expected application to be stopped in the WST CF server",
				appModule.getStateInServer() == Server.STATE_STOPPED);

		// This is CFT application run state, independent of client
		assertTrue("Expected application to be stopped in CFT framework",
				appModule.getRunState() == ApplicationRunState.STOPPED);
	}

	public void testApplicationStartState() throws Exception {

		String testName = "startState";

		String expectedAppName = harness.getWebAppName(testName);
		IProject project = createWebApplicationProject();
		boolean startApp = true;

		getTestFixture().configureForApplicationDeployment(expectedAppName, startApp);

		IModule module = getWstModule(project.getName());

		cloudServer.getBehaviour().operations().applicationDeployment(new IModule[] { module }, ApplicationAction.PUSH)
				.run(new NullProgressMonitor());

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(expectedAppName);

		// Check all the API that return started state, including started state
		// in the local server as well as in Cloud Foundry

		// Test the deprecated API. Should still return correct state
		assertTrue("Expected application to be started", appModule.getState() == Server.STATE_STARTED);

		// This is the old v1 client app state.
		assertTrue("Expected application to be started in v1 CF client",
				appModule.getApplication().getState().equals(AppState.STARTED));

		// This is the WST module state in the server
		assertTrue("Expected application to be started in the WST CF server",
				appModule.getStateInServer() == Server.STATE_STARTED);

		// This is CFT application run state, independent of client
		assertTrue("Expected application to be started in CFT framework",
				appModule.getRunState() == ApplicationRunState.STARTED);

		// Verify that instances info is available
		assertEquals("Expected instances information for running app", 1,
				appModule.getInstancesInfo().getInstances().size());
		assertNotNull("Expected instances information for running app",
				appModule.getInstancesInfo().getInstances().get(0).getSince());

		assertEquals("Expected instance stats for running app", 1, appModule.getApplicationStats().getRecords().size());
	}
}
