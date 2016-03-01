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

import java.util.List;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.application.EnvironmentVariable;
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
		String projectName = harness.getDefaultWebAppProjectName();

		assertEquals(project.getName(), projectName);

		IModule module = getModule(projectName);

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
	 * @param appPrefix
	 * @return
	 * @throws Exception
	 */
	protected CloudFoundryApplicationModule assertApplicationIsDeployed(String appPrefix) throws Exception {
		// Get the local WST IModule. NOTE that the PROJECT name needs to be
		// used as opposed to the
		// app name, as the project name and app name may differ, and the
		// IModule is mapped to the project.
		IModule module = getModule(harness.getDefaultWebAppProjectName());

		// Once the application is started, verify that the Cloud module is
		// valid,
		// and mapped to
		// an actual CloudApplication representing the deployed application.
		CloudFoundryApplicationModule appModule = assertCloudFoundryModuleExists(module, appPrefix);

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
	 * @param appPrefix
	 * @return
	 * @throws Exception
	 */
	protected CloudFoundryApplicationModule assertCloudFoundryModuleExists(IModule module, String appPrefix)
			throws Exception {
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);

		assertNotNull("Expected non-null Cloud Foundry application module", appModule);

		// The deployed application name in the Cloud module MUST match the
		// expected application name
		assertEquals(harness.getDefaultWebAppName(appPrefix), appModule.getDeployedApplicationName());

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
	 * @param appPrefix
	 * @param deployStopped if the app is to be deployed in stopped mode.
	 * @return
	 * @throws Exception
	 */
	protected CloudFoundryApplicationModule deployApplication(String appPrefix, boolean startApp) throws Exception {
		return deployApplication(appPrefix, CloudFoundryTestUtil.DEFAULT_TEST_APP_MEMORY, startApp, null, null);
	}

	protected CloudFoundryApplicationModule deployApplication(String appPrefix, int memory, boolean startApp,
			List<EnvironmentVariable> variables, List<CloudService> services) throws Exception {

		String projectName = harness.getDefaultWebAppProjectName();

		String expectedAppName = harness.getDefaultWebAppName(appPrefix);

		// Configure the test fixture for deployment.
		// This step is a substitute for the Application deployment wizard
		getTestFixture().configureForApplicationDeployment(expectedAppName, memory, startApp, variables, services);

		IModule module = getModule(projectName);

		assertNotNull("Expected non-null IModule when deploying application", module);

		// Do not test the WTP publish path as errors do not propagate to the
		// test case and failures will not correctly be shown in test results.
		// serverBehavior.publish(IServer.PUBLISH_INCREMENTAL, new
		// NullProgressMonitor());

		IStatus status = serverBehavior.publishAdd(module.getName(), new NullProgressMonitor());
		if (!status.isOK()) {
			throw new CoreException(status);
		}

		CloudFoundryApplicationModule appModule = assertApplicationIsDeployed(appPrefix);

		// Do a separate check to verify that there is in fact a
		// CloudApplication for the
		// given app (i.e. verify that is is indeed deployed, even though this
		// has been checked
		// above, this is another way to verify all is OK.
		CloudApplication actualCloudApp = getUpdatedApplication(expectedAppName);

		assertNotNull("Expected non-null CloudApplication when checking if application is deployed", actualCloudApp);
		assertEquals(actualCloudApp.getName(), expectedAppName);

		return appModule;
	}

	protected IModule getModule(String projectName) {

		IModule[] modules = server.getModules();
		for (IModule module : modules) {
			if (projectName.equals(module.getName())) {
				return module;
			}
		}
		return null;
	}

	protected CloudServiceOffering getServiceConfiguration(String vendor) throws CoreException {
		List<CloudServiceOffering> serviceConfigurations = serverBehavior
				.getServiceOfferings(new NullProgressMonitor());
		if (serviceConfigurations != null) {
			for (CloudServiceOffering serviceConfiguration : serviceConfigurations) {
				if (vendor.equals(serviceConfiguration.getLabel())) {
					return serviceConfiguration;
				}
			}
		}
		return null;
	}

	protected CloudService getCloudServiceToCreate(String name, String label, String plan) throws CoreException {
		CloudServiceOffering serviceConfiguration = getServiceConfiguration(label);
		if (serviceConfiguration != null) {
			CloudService service = new CloudService();
			service.setName(name);
			service.setLabel(label);
			service.setVersion(serviceConfiguration.getVersion());

			boolean planExists = false;

			List<CloudServicePlan> plans = serviceConfiguration.getCloudServicePlans();

			for (CloudServicePlan pln : plans) {
				if (plan.equals(pln.getName())) {
					planExists = true;
					break;
				}
			}

			if (!planExists) {
				throw CloudErrorUtil.toCoreException("No plan: " + plan + " found for service :" + label);
			}
			service.setPlan(plan);

			return service;
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

}
