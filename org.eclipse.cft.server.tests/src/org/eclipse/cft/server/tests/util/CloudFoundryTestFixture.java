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
package org.eclipse.cft.server.tests.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudUtil;
import org.eclipse.cft.server.core.internal.application.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.core.internal.spaces.CloudOrgsAndSpaces;
import org.eclipse.cft.server.tests.AllCloudFoundryTests;
import org.eclipse.cft.server.tests.server.TestServlet;
import org.eclipse.cft.server.tests.server.WebApplicationContainerBean;
import org.eclipse.cft.server.tests.sts.util.StsTestUtil;
import org.eclipse.cft.server.ui.internal.CloudUiUtil;
import org.eclipse.cft.server.ui.internal.ServerDescriptor;
import org.eclipse.cft.server.ui.internal.ServerHandler;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;
import org.osgi.framework.Bundle;

/**
 * Holds connection properties and provides utility methods for testing. The
 * Fixture is intended to set up parts of the CF Eclipse plugin prior to a test
 * case performing an operation, that would normally require user input via UI
 * (for example, the deployment wizard). The fixture is also responsible for
 * creating a per-test-case Harness, that sets up a server instance and web
 * project for deployment
 * <p/>
 * Only one instance of a test fixture is intended to exist for a set of test
 * cases, as opposed to a test {@link Harness} , which is created for EACH test
 * case. The fixture is not intended to hold per-test-case state, but rather
 * common properties (like credentials) that are only loaded once for a set of
 * test cases (in other words, for the entire junit runtime).
 * <p/>
 * If state needs to be held on a per-test-case basis, use the {@link Harness}
 * to store state.
 *
 * @author Steffen Pingel
 */
public class CloudFoundryTestFixture {

	public static final String DYNAMIC_WEBAPP_NAME = "basic-dynamic-webapp";

	public static final String PASSWORD_PROPERTY = "password";

	public static final String USEREMAIL_PROPERTY = "username";

	public static final String ORG_PROPERTY = "org";

	public static final String SPACE_PROPERTY = "space";

	public static final String URL_PROPERTY = "url";

	public static final String SELF_SIGNED_CERTIFICATE_PROPERTY = "selfsigned";

	public static final String CLOUDFOUNDRY_TEST_CREDENTIALS_PROPERTY = "test.credentials";

	private static boolean safeTearDown = false;

	/**
	 *
	 * The intention of the harness is to create a web project and server
	 * instance PER test case, and which holds state that is relevant only
	 * during the lifetime of a single test case. It should NOT hold state that
	 * is shared across different test cases. This means that a new harness
	 * should be created for each test case that is run.
	 * <p/>
	 * IMPORTANT: Since the harness is responsible for creating the server
	 * instance and project, to ensure proper behaviour of each test case, it's
	 * important to use the SAME harness instance throughout the entire test
	 * case.
	 * <p/>
	 * In addition, the harness provides a default web project creation, that
	 * can be reused in each test case, and deploys an application based on the
	 * same web project, but using different application names each time. Note
	 * that application names need not match the project name, as application
	 * names can be user defined rather than generated.
	 * <p/>
	 * The harness provides a mechanism to generate an application name and URL
	 * based on the default web project by requiring callers to pass in an
	 * application name "prefix"
	 * <p/>
	 * The purpose of the prefix is to reuse the same web project for each tests
	 * but assign a different name to avoid "URL taken/Host taken" errors in
	 * case the server does not clear the routing of the app by the time the
	 * next test runs that will deploy the same project. By having different
	 * names for each application deployment, the routing problem is avoided or
	 * minimised.
	 */
	public class Harness {

		private boolean projectCreated;

		private IServer server;

		private WebApplicationContainerBean webContainer;

		private String applicationDomain;

		private final String serverUrl;

		// Added to the application name in order to avoid host name taken
		// errors
		// Even when clearing routes, host name taken errors are occasionally
		// thrown
		// if the tests are run within short intervals of one another.
		private int randomPrefix = 0;

		private final CloudFoundryTestFixture fixture;

		public Harness(String serverUrl, CloudFoundryTestFixture fixture) {
			this.serverUrl = serverUrl;
			this.fixture = fixture;
		}

		/**
		 * Creates a default web application project in the workspace, and
		 * prepares it for deployment by creating WST IModule for it. This does
		 * NOT do the actual application deployment to the CF server, it only
		 * prepares it for deployment locally.
		 * @return
		 * @throws Exception
		 */
		public IProject createDefaultProjectAndAddModule() throws Exception {
			IProject project = createProject(getDefaultWebAppProjectName());
			projectCreated = true;
			addModule(project);
			return project;
		}

		protected String getDomain() throws CoreException {
			if (applicationDomain == null) {

				List<CloudDomain> domains = getBehaviour().getDomainsForSpace(new NullProgressMonitor());

				// Get a default domain
				applicationDomain = domains.get(0).getName();
				applicationDomain = applicationDomain.replace("http://", "");
			}
			return applicationDomain;
		}

		public void addModule(IProject project) throws CoreException {
			Assert.isNotNull(server, "Invoke createServer() first");

			IModule[] modules = ServerUtil.getModules(project);
			IServerWorkingCopy wc = server.createWorkingCopy();
			wc.modifyModules(modules, new IModule[0], null);
			wc.save(true, null);
		}

		public IProject createProject(String projectName) throws CoreException, IOException {
			return StsTestUtil.createPredefinedProject(projectName, CloudFoundryTestFixture.PLUGIN_ID);
		}

		public IServer createServer() throws Exception {

			Assert.isTrue(server == null, "createServer() already invoked");

			server = handler.createServer(new NullProgressMonitor(), ServerHandler.ALWAYS_OVERWRITE);
			IServerWorkingCopy serverWC = server.createWorkingCopy();
			CloudFoundryServer cloudFoundryServer = (CloudFoundryServer) serverWC.loadAdapter(CloudFoundryServer.class,
					null);
			cloudFoundryServer.setPassword(credentials.password);
			cloudFoundryServer.setUsername(credentials.userEmail);

			cloudFoundryServer.setUrl(getUrl());
			cloudFoundryServer.setSelfSignedCertificate(credentials.selfSignedCertificate);

			setCloudSpace(cloudFoundryServer, credentials.organization, credentials.space);

			serverWC.save(true, null);
			return server;
		}

		protected void setCloudSpace(CloudFoundryServer cloudServer, String orgName, String spaceName)
				throws CoreException {
			CloudOrgsAndSpaces spaces = CloudUiUtil.getCloudSpaces(cloudServer.getUsername(), cloudServer.getPassword(),
					cloudServer.getUrl(), false, cloudServer.getSelfSignedCertificate(), null);
			Assert.isTrue(spaces != null, "Failed to resolve orgs and spaces.");
			Assert.isTrue(spaces.getDefaultCloudSpace() != null,
					"No default space selected in cloud space lookup handler.");

			CloudSpace cloudSpace = spaces.getSpace(orgName, spaceName);
			if (cloudSpace == null) {
				throw CloudErrorUtil.toCoreException(
						"Failed to resolve cloud space when running junits: " + orgName + " - " + spaceName);
			}
			cloudServer.setSpace(cloudSpace);
		}

		public String getUrl() {
			if (webContainer != null) {
				return "http://localhost:" + webContainer.getPort() + "/";
			}
			else {
				return this.serverUrl;
			}
		}

		protected CloudFoundryServerBehaviour getBehaviour() {
			return (CloudFoundryServerBehaviour) server.loadAdapter(CloudFoundryServerBehaviour.class, null);
		}

		public void setup() throws Exception {

			Random random = new Random(100);
			randomPrefix = Math.abs(random.nextInt(1000000));

			// Clean up all projects from workspace
			StsTestUtil.cleanUpProjects();
		}

		public void deleteService(CloudService serviceToDelete) throws CoreException {
			CloudFoundryServerBehaviour serverBehavior = getBehaviour();

			String serviceName = serviceToDelete.getName();
			List<String> services = new ArrayList<String>();
			services.add(serviceName);

			serverBehavior.operations().deleteServices(services).run(new NullProgressMonitor());
		}

		public List<CloudService> getAllServices() throws CoreException {
			List<CloudService> services = getBehaviour().getServices(new NullProgressMonitor());
			if (services == null) {
				services = new ArrayList<CloudService>(0);
			}
			return services;
		}

		private void clearTestDomainAndRoutes() throws Exception {
			CloudFoundryOperations client = createExternalClient();
			client.login();
			String domain = getDomain();
			if (domain != null) {
				List<CloudRoute> routes = client.getRoutes(domain);
				for (CloudRoute route : routes) {
					client.deleteRoute(route.getHost(), route.getDomain().getName());
				}
			}
		}

		public void deleteAllServices() throws CoreException {
			List<CloudService> services = getAllServices();
			for (CloudService service : services) {
				deleteService(service);
				CloudFoundryTestUtil.waitIntervals(1000);
			}
		}

		public void dispose() throws Exception {
			if (webContainer != null) {

				// FIXNS: Commented out because of STS-3159
				// webContainer.stop();
			}

			if (server != null) {
				CloudFoundryServerBehaviour cloudFoundryServer = (CloudFoundryServerBehaviour) server
						.loadAdapter(CloudFoundryServerBehaviour.class, null);
				if (projectCreated && fixture.safeTearDown()) {
					// Dont let errors in CF cleanup stop server cleanup in the
					// workspace
					try {
						cloudFoundryServer.deleteAllApplications(null);
						deleteAllServices();
						clearTestDomainAndRoutes();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
				try {
					cloudFoundryServer.disconnect(null);
				}
				catch (CoreException e) {
					e.printStackTrace();
				}
			}
			try {
				handler.deleteServerAndRuntime(new NullProgressMonitor());
			}
			catch (CoreException e) {
				e.printStackTrace();
			}

			if (projectCreated) {
				StsTestUtil.deleteAllProjects();
			}
		}

		/**
		 * Given a prefix for an application name (e.g. "test01" in
		 * "test01myprojectname") constructs the expected URL based on the
		 * default web project name ("myprojectname") and the domain. E.g:
		 *
		 * <p/>
		 * Arg = test01
		 * <p/>
		 * Default project name = myprojectname
		 * <p/>
		 * domain = run.pivotal.io URL = test01myprojectname.run.pivotal.io
		 * <p/>
		 * The purpose of the prefix is to reuse the same web project for each
		 * tests but assign a different name to avoid "URL taken/Host taken"
		 * errors in case the server does not clear the routing of the app by
		 * the time the next test runs that will deploy the same project. By
		 * having different names for each application deployment, the routing
		 * problem is avoided or minimised.
		 * @param appPrefix
		 * @return
		 * @throws CoreException
		 */
		public String getExpectedDefaultURL(String appPrefix) throws CoreException {
			return getDefaultWebAppName(appPrefix) + '.' + getDomain();
		}

		public String getDefaultWebAppProjectName() {
			return DYNAMIC_WEBAPP_NAME;
		}

		public String getDefaultWebAppName(String appPrefix) {
			return appPrefix + '_' + randomPrefix + '_' + getDefaultWebAppProjectName();
		}

		public TestServlet startMockServer() throws Exception {
			Bundle bundle = Platform.getBundle(AllCloudFoundryTests.PLUGIN_ID);
			URL resourceUrl = bundle.getResource("webapp");
			URL localURL = FileLocator.toFileURL(resourceUrl);
			File file = new File(localURL.getFile());
			webContainer = new WebApplicationContainerBean(file);

			// FIXNS: Commented out because of STS-3159
			// webContainer.start();
			return getServer();
		}

		public TestServlet getServer() {
			return TestServlet.getInstance();
		}

	}

	public static final String PLUGIN_ID = "org.eclipse.cft.server.tests";

	private static CloudFoundryTestFixture current;

	/**
	 * Performs safety validations against a CF target (e.g. making sure it is
	 * empty before deleting apps). Should be called only once per setup as each
	 * call performs a safety validation on the Cloud Target.
	 * @return non-null Test fixture that contains a harness for CF testing
	 * @throws Exception if failed to obtain a safe test fixture in which to
	 * perform CF tests
	 */
	public static CloudFoundryTestFixture getSafeTestFixture() throws Exception {
		if (current == null) {
			CredentialProperties credentials = getCredentialsFromProperties(getDefaultCloudTargetDomain());
			current = new CloudFoundryTestFixture(credentials);
		}
		current.verifySafeCloudTarget();
		return current;
	}

	/**
	 *
	 * @return a default Cloud target domain. Note that this is not the full
	 * Cloud API URL, but just a Cloud domain
	 */
	public static String getDefaultCloudTargetDomain() {
		return "run.pivotal.io";
	}

	public static CloudFoundryOperations createExternalClient(CredentialProperties cred) throws Exception {
		StsTestUtil.validateCredentials(cred);
		return StsTestUtil.createStandaloneClient(cred, cred.url);
	}

	public CloudFoundryOperations createExternalClient() throws Exception {
		return createExternalClient(getCredentialProperties());
	}

	/**
	 * This test fixture hould not be used to configure to application
	 * deployment.
	 * @return
	 * @throws CoreException
	 */
	public void baseConfiguration() throws Exception {
		configureForApplicationDeployment(null, CloudUtil.DEFAULT_MEMORY, false);
	}

	/**
	 * Configures a test fixture to deploy an application with the given
	 * application name. The full application name must be used.
	 * @param fullApplicationName
	 * @param deployStopped true if the application should be deployed in
	 * stopped state. False if it should also be started
	 * @return
	 * @throws CoreException
	 */
	public void configureForApplicationDeployment(String fullApplicationName, int memory, boolean deployStopped)
			throws Exception {
		CloudFoundryPlugin.setCallback(new TestCallback(fullApplicationName, memory, deployStopped));
	}

	public void configureForApplicationDeployment(String fullApplicationName, int memory, boolean deployStopped,
			List<EnvironmentVariable> variables, List<CloudService> services) throws Exception {
		CloudFoundryPlugin
				.setCallback(new TestCallback(fullApplicationName, memory, deployStopped, variables, services));
	}

	private final ServerHandler handler;

	private final CredentialProperties credentials;

	/**
	 * This will create a Cloud server instances based either on the URL in a
	 * properties file that also contains the credentials, or the default server
	 * domain passed into the fixture. The URL in the properties file is
	 * optional, if it is not found, the default server Domain will be used
	 * instead
	 * @param serverDomain default domain to use for the Cloud space.
	 */
	public CloudFoundryTestFixture(CredentialProperties credentials) {
		this.credentials = credentials;

		ServerDescriptor descriptor = new ServerDescriptor("server") {
			{
				setRuntimeTypeId("org.cloudfoundry.cloudfoundryserver.test.runtime.10");
				setServerTypeId("org.cloudfoundry.cloudfoundryserver.test.10");
				setRuntimeName("Cloud Foundry Test Runtime");
				setServerName("Cloud Foundry Test Server");
				setForceCreateRuntime(true);
			}
		};
		handler = new ServerHandler(descriptor);
	}

	/**
	 * A safe target is a target that can be tested against. In particular, the
	 * target should not contain existing applications and services. This is to
	 * avoid accidentally deleting existing applications and services in shared
	 * Cloud targets during setup or teardown of junits.
	 * @throws CoreException if target is not safe to run junits
	 */
	public void verifySafeCloudTarget() throws Exception {
		try {
			checkSafeTarget(getCredentialProperties());
			safeTearDown = true;
		}
		catch (Exception e) {
			// Be sure to set this flag to avoid deletion of apps and
			// services on junit tear down even when it is not a safe target.
			safeTearDown = false;
			throw e;
		}
	}

	/**
	 *
	 * @return true if it is safe to tear down. False if tear down should be
	 * aborted (to avoid deleting apps and services in the CF target).
	 */
	public boolean safeTearDown() {
		return safeTearDown;
	}

	protected static void checkSafeTarget(CredentialProperties cred) throws Exception {

		StsTestUtil.validateCredentials(cred);

		// To avoid junits deleting contents of a target by accident, ensure
		// the target is empty
		CloudFoundryOperations ops = createExternalClient(cred);

		List<CloudApplication> apps = ops.getApplications();
		if (apps != null && !apps.isEmpty()) {
			throw CloudErrorUtil.toCoreException(NLS.bind(
					"Empty Cloud target required to run junits. Existing number of applications {0} found in: server = {1}, org = {2}, space = {3}",
					new Object[] { apps.size(), cred.url, cred.organization, cred.space }));
		}
		List<CloudService> services = ops.getServices();
		if (services != null && !services.isEmpty()) {
			throw CloudErrorUtil.toCoreException(NLS.bind(
					"Empty Cloud target required to run junits. Existing number of services {0} found in: server = {1}, org = {2}, space = {3}",
					new Object[] { services.size(), cred.url, cred.organization, cred.space }));
		}

	}

	public CredentialProperties getCredentialProperties() {
		return this.credentials;
	}

	/**
	 * New harness is created. To ensure proper behaviour for each test case.
	 * Create the harness in the test setup, and use this SAME harness
	 * throughout the lifetime of the same test case. Therefore, a harness
	 * should ideally only be created ONCE throughout the lifetime of a single
	 * test case.
	 * @return new Harness. Never null
	 */
	public Harness createHarness() {
		return new Harness(getCredentialProperties().url, this);
	}

	public static class CredentialProperties {

		public final String userEmail;

		public final String password;

		public final String organization;

		public final String space;

		public final String url;

		public final boolean selfSignedCertificate;

		public CredentialProperties(String url, String userEmail, String password, String organization, String space,
				boolean selfSignedCertificate) {
			this.url = url;
			this.userEmail = userEmail;
			this.password = password;
			this.organization = organization;
			this.space = space;
			this.selfSignedCertificate = selfSignedCertificate;
		}

	}

	/**
	 * Reads properties to connect to a CF target (e.g API URL, org, space,
	 * username, password). If the properties does not include a API URL, the
	 * passed defaultDomain will be used to construct a API URL Returns non-null
	 * credentials, although values of the credentials may be empty if failed to
	 * read credentials
	 * @return
	 */
	private static CredentialProperties getCredentialsFromProperties(String defaultDomain) throws CoreException {
		String propertiesLocation = System.getProperty(CLOUDFOUNDRY_TEST_CREDENTIALS_PROPERTY);
		String userEmail = null;
		String password = null;
		String org = null;
		String space = null;
		String url = null;
		boolean selfSignedCertificate = false;
		if (propertiesLocation != null) {

			File propertiesFile = new File(propertiesLocation);

			InputStream fileInputStream = null;
			try {
				if (propertiesFile.exists() && propertiesFile.canRead()) {
					fileInputStream = new FileInputStream(propertiesFile);
					Properties properties = new Properties();
					properties.load(fileInputStream);
					userEmail = properties.getProperty(USEREMAIL_PROPERTY);
					password = properties.getProperty(PASSWORD_PROPERTY);
					org = properties.getProperty(ORG_PROPERTY);
					space = properties.getProperty(SPACE_PROPERTY);
					url = properties.getProperty(URL_PROPERTY);
					String selfSignedVal = properties.getProperty(SELF_SIGNED_CERTIFICATE_PROPERTY);

					selfSignedCertificate = "true".equals(selfSignedVal) || "TRUE".equals(selfSignedVal);
				}
			}
			catch (FileNotFoundException e) {
				throw CloudErrorUtil.toCoreException(e);
			}
			catch (IOException e) {
				throw CloudErrorUtil.toCoreException(e);
			}
			finally {
				try {
					if (fileInputStream != null) {
						fileInputStream.close();
					}
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		if (url == null) {
			url = "http://api." + defaultDomain;
		}
		else if (!url.startsWith("http")) {
			url = "http://" + url;
		}

		CredentialProperties cred = new CredentialProperties(url, userEmail, password, org, space,
				selfSignedCertificate);
		StsTestUtil.validateCredentials(cred);
		return cred;

	}

}
