/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc.
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
import org.eclipse.cft.server.tests.util.CloudFoundryTestFixture.CredentialProperties;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Assert;

import junit.framework.TestCase;

/**
 * @author Terry Denney
 */
public class CloudFoundryClientConnectionTest extends TestCase {

	public void testConnectToNonSecureUrl() throws Exception {

		String url = getTestFixture().getUrl();

		URL ur = new URL(url);
		String host = ur.getHost();
		String httpUrl = "http://" + host;

		CredentialProperties credentials = getTestFixture().getCredentials();

		CloudFoundryOperations client = StsTestUtil.createStandaloneClient(credentials.userEmail, credentials.password,
				credentials.organization, credentials.space, httpUrl, getTestFixture().getSelfSignedCertificate());

		new CloudFoundryLoginHandler(client).login(new NullProgressMonitor());

		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);
	}

	public void testConnectToSecureUrl() throws Exception {
		String url = getTestFixture().getUrl();

		URL ur = new URL(url);
		String host = ur.getHost();
		String httpUrl = "https://" + host;

		CredentialProperties credentials = getTestFixture().getCredentials();

		CloudFoundryOperations client = StsTestUtil.createStandaloneClient(credentials.userEmail, credentials.password,
				credentials.organization, credentials.space, httpUrl, getTestFixture().getSelfSignedCertificate());

		new CloudFoundryLoginHandler(client).login(new NullProgressMonitor());

		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);
	}

	public void testValidCredentials() throws Exception {

		CredentialProperties credentials = getTestFixture().getCredentials();

		CloudFoundryOperations client = StsTestUtil.createStandaloneClient(credentials.userEmail, credentials.password,
				credentials.organization, credentials.space, getTestFixture().getUrl(),
				getTestFixture().getSelfSignedCertificate());

		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);

	}

	public void testValidCredentialsLoginHandler() throws Exception {

		CredentialProperties credentials = getTestFixture().getCredentials();

		CloudFoundryOperations client = StsTestUtil.createStandaloneClient(credentials.userEmail, credentials.password,
				credentials.organization, credentials.space, getTestFixture().getUrl(),
				getTestFixture().getSelfSignedCertificate());

		CloudFoundryLoginHandler operationsHandler = new CloudFoundryLoginHandler(client);

		operationsHandler.login(new NullProgressMonitor());

		CloudInfo cloudInfo = client.getCloudInfo();
		Assert.assertNotNull(cloudInfo);

	}

	public void testInvalidUsername() throws Exception {
		CredentialProperties credentials = getTestFixture().getCredentials();

		String invalidUsername = "invalid@username";

		CloudFoundryException cfe = null;
		try {
			StsTestUtil.createStandaloneClient(invalidUsername, credentials.password, credentials.organization,
					credentials.space, getTestFixture().getUrl(), getTestFixture().getSelfSignedCertificate());
		}
		catch (CloudFoundryException e) {
			cfe = e;
		}

		assertNotNull(cfe);
		assertTrue(CloudErrorUtil.getCloudFoundryErrorMessage(cfe).contains("403"));
	}

	public void testInvalidPassword() throws Exception {
		CredentialProperties credentials = getTestFixture().getCredentials();

		String invalidPassword = "wrongpassword";

		CloudFoundryException cfe = null;
		try {
			StsTestUtil.createStandaloneClient(credentials.userEmail, invalidPassword, credentials.organization,
					credentials.space, getTestFixture().getUrl(), getTestFixture().getSelfSignedCertificate());
		}
		catch (CloudFoundryException e) {
			cfe = e;
		}

		assertNotNull(cfe);
		assertTrue(CloudErrorUtil.getCloudFoundryErrorMessage(cfe).contains("403"));
	}

	public void testInvalidOrg() throws Exception {
		CredentialProperties credentials = getTestFixture().getCredentials();

		String wrongOrg = "wrongorg";

		IllegalArgumentException error = null;
		try {
			StsTestUtil.createStandaloneClient(credentials.userEmail, credentials.password, wrongOrg, credentials.space,
					getTestFixture().getUrl(), getTestFixture().getSelfSignedCertificate());
		}
		catch (IllegalArgumentException e) {
			error = e;
		}

		assertNotNull(error);
		assertTrue(error.getMessage().toLowerCase().contains("no matching organization and space"));
	}

	public void testInvalidSpace() throws Exception {
		CredentialProperties credentials = getTestFixture().getCredentials();

		String wrongSpace = "wrongSpace";

		IllegalArgumentException error = null;
		try {
			StsTestUtil.createStandaloneClient(credentials.userEmail, credentials.password, credentials.organization,
					wrongSpace, getTestFixture().getUrl(), getTestFixture().getSelfSignedCertificate());
		}
		catch (IllegalArgumentException e) {
			error = e;
		}

		assertNotNull(error);
		assertTrue(error.getMessage().toLowerCase().contains("no matching organization and space"));
	}

	protected CloudFoundryTestFixture getTestFixture() throws CoreException {
		return CloudFoundryTestFixture.getTestFixture();
	}
}
