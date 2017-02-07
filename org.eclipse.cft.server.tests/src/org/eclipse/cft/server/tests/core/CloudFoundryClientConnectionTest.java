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

import java.net.URL;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryLoginHandler;
import org.eclipse.cft.server.tests.sts.util.StsTestUtil;
import org.eclipse.cft.server.tests.util.CloudFoundryTestFixture;
import org.eclipse.cft.server.tests.util.HarnessProperties;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Assert;

import junit.framework.TestCase;

/**
 * @author Terry Denney
 */
public class CloudFoundryClientConnectionTest extends TestCase {

	protected CloudFoundryTestFixture testFixture;

	@Override
	protected void setUp() throws Exception {
		testFixture = CloudFoundryTestFixture.getSafeTestFixture();
	}

	protected CloudFoundryTestFixture getTestFixture() {
		return testFixture;
	}

	public void testConnectToNonSecureUrl() throws Exception {

		HarnessProperties props = getTestFixture().getHarnessProperties();

		String url = props.getApiUrl();

		URL ur = new URL(url);
		String host = ur.getHost();
		String httpUrl = "http://" + host;

		CloudFoundryOperations client = StsTestUtil.createStandaloneClient(props.getUsername(), props.getPassword(),
				props.getOrg(), props.getSpace(), httpUrl, props.skipSslValidation());

		new CloudFoundryLoginHandler(client, null).login(new NullProgressMonitor());

		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);
	}

	public void testConnectToSecureUrl() throws Exception {
		HarnessProperties props = getTestFixture().getHarnessProperties();

		String url = props.getApiUrl();

		URL ur = new URL(url);
		String host = ur.getHost();
		String httpUrl = "https://" + host;

		CloudFoundryOperations client = StsTestUtil.createStandaloneClient(props.getUsername(), props.getPassword(),
				props.getOrg(), props.getSpace(), httpUrl, props.skipSslValidation());

		new CloudFoundryLoginHandler(client, null).login(new NullProgressMonitor());

		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);
	}

	public void testValidCredentials() throws Exception {

		HarnessProperties props = getTestFixture().getHarnessProperties();

		CloudFoundryOperations client = StsTestUtil.createStandaloneClient(props.getUsername(), props.getPassword(),
				props.getOrg(), props.getSpace(), props.getApiUrl(), props.skipSslValidation());

		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);

	}

	public void testValidCredentialsLoginHandler() throws Exception {

		HarnessProperties props = getTestFixture().getHarnessProperties();

		CloudFoundryOperations client = StsTestUtil.createStandaloneClient(props.getUsername(), props.getPassword(),
				props.getOrg(), props.getSpace(), props.getApiUrl(), props.skipSslValidation());

		CloudFoundryLoginHandler operationsHandler = new CloudFoundryLoginHandler(client, null);

		operationsHandler.login(new NullProgressMonitor());

		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);

	}

	public void testInvalidUsername() throws Exception {
		HarnessProperties props = getTestFixture().getHarnessProperties();

		String invalidUsername = "invalid@username";

		CloudFoundryException cfe = null;
		try {
			StsTestUtil.createStandaloneClient(invalidUsername, props.getPassword(), props.getOrg(), props.getSpace(),
					props.getApiUrl(), props.skipSslValidation());
		}
		catch (CloudFoundryException e) {
			cfe = e;
		}

		assertNotNull(cfe);
		assertTrue(CloudErrorUtil.getCloudFoundryErrorMessage(cfe).contains("403"));
	}

	public void testInvalidPassword() throws Exception {
		HarnessProperties props = getTestFixture().getHarnessProperties();

		String invalidPassword = "wrongpassword";

		CloudFoundryException cfe = null;
		try {
			StsTestUtil.createStandaloneClient(props.getUsername(), invalidPassword, props.getOrg(), props.getSpace(),
					props.getApiUrl(), props.skipSslValidation());
		}
		catch (CloudFoundryException e) {
			cfe = e;
		}

		assertNotNull(cfe);
		assertTrue(CloudErrorUtil.getCloudFoundryErrorMessage(cfe).contains("403"));
	}

	public void testInvalidOrg() throws Exception {
		HarnessProperties props = getTestFixture().getHarnessProperties();

		String wrongOrg = "wrongorg";

		IllegalArgumentException error = null;
		try {
			StsTestUtil.createStandaloneClient(props.getUsername(), props.getPassword(), wrongOrg, props.getSpace(),
					props.getApiUrl(), props.skipSslValidation());
		}
		catch (IllegalArgumentException e) {
			error = e;
		}

		assertNotNull(error);
		assertTrue(error.getMessage().toLowerCase().contains("no matching organization and space"));
	}

	public void testInvalidSpace() throws Exception {
		HarnessProperties props = getTestFixture().getHarnessProperties();

		String wrongSpace = "wrongSpace";

		IllegalArgumentException error = null;
		try {
			StsTestUtil.createStandaloneClient(props.getUsername(), props.getPassword(),
					props.getOrg(), wrongSpace, props.getApiUrl(), props.skipSslValidation());
		}
		catch (IllegalArgumentException e) {
			error = e;
		}

		assertNotNull(error);
		assertTrue(error.getMessage().toLowerCase().contains("no matching organization and space"));
	}
}
