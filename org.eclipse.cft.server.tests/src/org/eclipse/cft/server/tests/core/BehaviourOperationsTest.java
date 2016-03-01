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
import org.eclipse.cft.server.core.internal.ApplicationAction;
import org.eclipse.cft.server.core.internal.application.EnvironmentVariable;
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
		String prefix = "testInstanceUpdate";

		String expectedAppName = harness.getDefaultWebAppName(prefix);

		createWebApplicationProject();
		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp);

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

		String prefix = "testMemoryUpdate";

		createWebApplicationProject();
		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp);

		final int changedMemory = 678;

		cloudServer.getBehaviour().operations().memoryUpdate(appModule, changedMemory).run(new NullProgressMonitor());

		// Get updated module
		appModule = cloudServer.getBehaviour().updateDeployedModule(appModule.getLocalModule(),
				new NullProgressMonitor());
		// Verify that the same module has been updated
		assertEquals(changedMemory, appModule.getDeploymentInfo().getMemory());
		assertEquals(changedMemory, appModule.getApplication().getMemory());

		assertEquals(changedMemory, appModule.getApplication().getMemory());
	}

	public void testEnvVarUpdate() throws Exception {

		String prefix = "testEnvVarUpdate";

		String expectedAppName = harness.getDefaultWebAppName(prefix);

		createWebApplicationProject();
		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp);

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

		String prefix = "testAppURLUpdate";
		final String expectedAppName = harness.getDefaultWebAppName(prefix);

		createWebApplicationProject();
		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp);

		String expectedURL = harness.getExpectedDefaultURL(prefix);
		assertEquals(expectedURL, appModule.getDeploymentInfo().getUris().get(0));

		String changedURL = harness.getExpectedDefaultURL("changedtestAppURLUpdate");
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
		String prefix = "testStopApplication";

		createWebApplicationProject();
		// Deploy and start the app without the refresh listener
		boolean startApp = true;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp);

		assertTrue("Expected application to be started",
				appModule.getApplication().getState().equals(AppState.STARTED));
		assertTrue("Expected application to be started", appModule.getState() == Server.STATE_STARTED);

		// Stop the app
		cloudServer.getBehaviour().operations().applicationDeployment(appModule, ApplicationAction.STOP)
				.run(new NullProgressMonitor());

		appModule = cloudServer.getExistingCloudModule(appModule.getDeployedApplicationName());

		assertTrue("Expected application to be stopped",
				appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == Server.STATE_STOPPED);

	}

	public void testStartApplication() throws Exception {

		String prefix = "testStartApplication";

		createWebApplicationProject();
		boolean startApplication = false;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApplication);

		assertTrue("Expected application to be stopped",
				appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == Server.STATE_STOPPED);

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

		String prefix = "testRestartApplication";

		createWebApplicationProject();
		boolean startApp = false;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApp);

		assertTrue("Expected application to be stopped",
				appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == Server.STATE_STOPPED);

		cloudServer.getBehaviour().operations().applicationDeployment(appModule, ApplicationAction.RESTART)
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

	public void testUpdateRestartApplication() throws Exception {

		String prefix = "testUpdateRestartApplication";

		createWebApplicationProject();

		boolean startApplication = false;
		CloudFoundryApplicationModule appModule = deployApplication(prefix, startApplication);

		assertTrue("Expected application to be stopped",
				appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == Server.STATE_STOPPED);

		cloudServer.getBehaviour().operations().applicationDeployment(appModule, ApplicationAction.UPDATE_RESTART)
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

	public void testPushApplicationStopMode() throws Exception {

		String prefix = "testPushApplicationStopMode";
		String expectedAppName = harness.getDefaultWebAppName(prefix);
		IProject project = createWebApplicationProject();
		boolean startApp = false;
		getTestFixture().configureForApplicationDeployment(expectedAppName,
				CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, startApp);

		IModule module = getModule(project.getName());

		cloudServer.getBehaviour().operations().applicationDeployment(new IModule[] { module }, ApplicationAction.PUSH)
				.run(new NullProgressMonitor());

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(expectedAppName);

		assertTrue("Expected application to be stopped",
				appModule.getApplication().getState().equals(AppState.STOPPED));
		assertTrue("Expected application to be stopped", appModule.getState() == Server.STATE_STOPPED);
	}

	public void testPushApplicationStartMode() throws Exception {

		String prefix = "testPushApplicationStartMode";

		String expectedAppName = harness.getDefaultWebAppName(prefix);
		IProject project = createWebApplicationProject();
		boolean startApp = true;

		getTestFixture().configureForApplicationDeployment(expectedAppName, startApp);

		IModule module = getModule(project.getName());

		cloudServer.getBehaviour().operations().applicationDeployment(new IModule[] { module }, ApplicationAction.PUSH)
				.run(new NullProgressMonitor());

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(expectedAppName);

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

}
