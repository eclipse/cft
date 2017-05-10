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

import java.util.List;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.cft.server.core.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.tests.server.TestServlet;
import org.eclipse.cft.server.tests.util.CloudFoundryTestFixture;
import org.eclipse.cft.server.tests.util.CloudFoundryTestFixture.Harness;
import org.eclipse.cft.server.tests.util.CloudFoundryTestUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

import junit.framework.TestCase;

/**
 * The base test setup for all Cloud Foundry tests involves creating a NEW
 * harness each time {@link #setUp()} is run, which mean, a new harness is
 * created for each test case. The harness is responsible for creating the
 * server instance used to deploy applications during each test case, so it is
 * IMPORTANT to use the same harness that is created during setup through the
 * entire test case, including any helper methods invoked by the test case.
 *
 */
public abstract class AbstractCloudFoundryTest extends TestCase {

	/**
	 * Since each {@link #setUp()} is invoked on EACH test case, be sure to
	 * reference the harness created during setup as to use the same harness
	 * that created the server instance throughout all the helper methods that
	 * may be invoked by a particular test case.
	 */

	protected Harness harness;

	protected IServer server;

	protected CloudFoundryServerBehaviour serverBehavior;

	protected CloudFoundryServer cloudServer;

	protected TestServlet testServlet;

	protected CloudFoundryTestFixture testFixture;

	protected void debug(String message) {
		System.out.println(message);
	}

	@Override
	protected void setUp() throws Exception {

		// This same harness MUST be used through out the entire lifetime of the
		// test case,
		// therefore if the test case or any helper methods need to reference
		// the harness
		// they must all reference this instance variable.
		testFixture = createTestFixture();
		testFixture.baseConfiguration();
		harness = testFixture.createHarness();
		server = harness.createServer();
		cloudServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
		serverBehavior = (CloudFoundryServerBehaviour) server.loadAdapter(CloudFoundryServerBehaviour.class, null);

		harness.setup();
		connectClient();
	}

	/**
	 * Connects to the server via a session client that is reset in the CF
	 * server instance. Credentials are not changed, either locally or remotely.
	 * This only resets and reconnects the Java client used by the server
	 * instance in the test harness.
	 * @return
	 * @throws CoreException
	 */
	protected void connectClient() throws CoreException {
		connectClient(null);
	}

	/**
	 * Resets the client in the server behaviour based on the given credentials.
	 * @param username
	 * @param password
	 * @throws CoreException
	 */
	protected void connectClient(String username, String password) throws CoreException {
		CloudCredentials credentials = new CloudCredentials(username, password);
		connectClient(credentials);
	}

	/**
	 * Resets the client in the server behaviour based on the given credentials.
	 * This will disconnect any existing session. If passing null, the stored
	 * server credentials will be used instead
	 * @param credentials
	 * @return
	 * @throws CoreException
	 */
	protected void connectClient(CloudCredentials credentials) throws CoreException {
		serverBehavior.disconnect(new NullProgressMonitor());
		serverBehavior.resetClient(credentials, new NullProgressMonitor());
		serverBehavior.connect(new NullProgressMonitor());
	}

	@Override
	protected void tearDown() throws Exception {
		harness.dispose();
	}

	/**
	 *
	 * Creates an application project based on the default project name defined
	 * in the test fixture harness. Does NOT deploy the application. Also
	 * creates the {@link IModule} associated with the application (but it does
	 * not create the actual Cloud Foundry module or application, as these are
	 * only created upon actual deployment)
	 *
	 * @throws Exception
	 */
	protected IProject createWebApplicationProject() throws Exception {

		// Create the default web project in the workspace and create the local
		// IModule for it. This does NOT create the application remotely, it
		// only
		// creates a "pre-deployment" WST IModule for the given project.
		IProject project = harness.createDefaultProjectAndAddModule();
		String projectName = project.getName();

		IModule module = getWstModule(projectName);

		IModule[] modules = new IModule[] { module };

		int moduleState = server.getModulePublishState(modules);
		assertTrue(IServer.PUBLISH_STATE_UNKNOWN == moduleState || IServer.PUBLISH_STATE_NONE == moduleState);

		// Verify that the WST module that exists matches the app project app

		assertNotNull(module);
		assertTrue(module.getName().equals(projectName));
		return project;

	}

	/**
	 * Asserts that the application is deployed although it may not necessarily
	 * be started.
	 * @param appName
	 * @return
	 * @throws Exception
	 */
	protected CloudFoundryApplicationModule assertApplicationIsDeployed(String appName, IProject project)
			throws Exception {
		// Get the local WST IModule. NOTE that the PROJECT name needs to be
		// used as opposed to the
		// app name, as the project name and app name may differ, and the
		// IModule is mapped to the project.
		IModule module = getWstModule(project.getName());

		// Once the application is started, verify that the Cloud module is
		// valid,
		// and mapped to
		// an actual CloudApplication representing the deployed application.
		CloudFoundryApplicationModule appModule = assertCloudFoundryModuleExists(module, appName);

		assertNotNull("No Cloud Application mapping in Cloud module. Failed to refresh deployed application",
				appModule.getApplication());

		return appModule;
	}

	/**
	 * Verifies that a {@link CloudFoundryApplicationModule} associated with the
	 * given {@link IModule} exists. Note that
	 * {@link CloudFoundryApplicationModule} are ONLY created after an attempted
	 * deployment or application start/restart. This should only be called after
	 * an app has been:
	 * <p/>
	 * 1. Created locally
	 * <p/>
	 * 2. Deployed or started
	 * @param module
	 * @param testName
	 * @return
	 * @throws Exception
	 */
	protected CloudFoundryApplicationModule assertCloudFoundryModuleExists(IModule module, String appName)
			throws Exception {
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);

		assertNotNull("Expected non-null Cloud Foundry application module", appModule);

		// The deployed application name in the Cloud module MUST match the
		// expected application name
		assertEquals(appName, appModule.getDeployedApplicationName());

		return appModule;
	}

	/**
	 * Finds the {@link CloudApplication} for an already deployed application
	 * with the given name. The name MUST be the full application name.
	 * @param appName. Must pass the FULL application name, not just the app
	 * name prefix.
	 * @return Application if found, or null if application is no longer in
	 * server
	 */
	protected CloudApplication getUpdatedApplication(String appName) throws CoreException {
		return serverBehavior.getCloudApplication(appName, new NullProgressMonitor());

	}

	/**
	 * Deploys an app with the given prefix name and asserts it is deployed
	 * @param testName app name will be generated based on the test name
	 * @param deployStopped if the app is to be deployed in stopped mode.
	 * @return
	 * @throws Exception
	 */
	protected CloudFoundryApplicationModule deployApplication(String appName, IProject project, boolean startApp,
			String buildpack) throws Exception {
		return deployApplication(appName, project, CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY,
				CloudFoundryTestUtil.DEFAULT_TEST_DISK_QUOTA, startApp, null, null, buildpack);
	}

	protected CloudFoundryApplicationModule deployApplication(String appName, IProject project, int memory,
			int diskQuota, boolean startApp, List<EnvironmentVariable> variables, List<CFServiceInstance> services,
			String buildpack) throws Exception {

		if (buildpack != null) {
			debug("Using buildpack: " + buildpack + " for app " + appName);
		}

		// Configure the test fixture for deployment.
		// This step is a substitute for the Application deployment wizard
		getTestFixture().configureForApplicationDeployment(appName, memory, diskQuota, startApp, variables, services,
				buildpack);

		IModule module = getWstModule(project.getName());

		assertNotNull("Expected non-null IModule when deploying application", module);

		// Do not test the WTP publish path as errors do not propagate to the
		// test case and failures will not correctly be shown in test results.
		// serverBehavior.publish(IServer.PUBLISH_INCREMENTAL, new
		// NullProgressMonitor());

		IStatus status = serverBehavior.publishAdd(module.getName(), new NullProgressMonitor());
		if (!status.isOK()) {
			throw new CoreException(status);
		}

		CloudFoundryApplicationModule appModule = assertApplicationIsDeployed(appName, project);

		// Do a separate check to verify that there is in fact a
		// CloudApplication for the
		// given app (i.e. verify that is is indeed deployed, even though this
		// has been checked
		// above, this is another way to verify all is OK.
		CloudApplication actualCloudApp = getUpdatedApplication(appName);

		assertNotNull("Expected non-null CloudApplication when checking if application is deployed", actualCloudApp);
		assertEquals(actualCloudApp.getName(), appName);

		return appModule;
	}

	/**
	 * Get the underlying WST/WTP IModule. IModules always use project name even
	 * if the Cloud app name is different.
	 * @param projectName
	 * @return
	 */
	protected IModule getWstModule(String projectName) {

		IModule[] modules = server.getModules();
		for (IModule module : modules) {
			if (projectName.equals(module.getName())) {
				return module;
			}
		}
		return null;
	}

	/**
	 *
	 * @return creates a test fixture for use PER setup
	 * @throws Exception
	 */
	protected CloudFoundryTestFixture createTestFixture() throws Exception {
		return CloudFoundryTestFixture.getSafeTestFixture();
	}

	protected CloudFoundryTestFixture getTestFixture() {
		return testFixture;
	}

	protected CloudApplication getAppFromExternalClient(String expectedAppName) throws Exception {

		CloudFoundryOperations client = getTestFixture().createExternalClient();
		client.login();
		List<CloudApplication> deployedApplications = client.getApplications();
		if (deployedApplications != null) {
			for (CloudApplication cloudApplication : deployedApplications) {
				if (cloudApplication.getName().equals(expectedAppName)) {
					return cloudApplication;
				}
			}
		}

		return null;
	}

}
