/*******************************************************************************
 * Copyright (c) 2016 Pivotal Software, Inc.
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
import java.util.Properties;

import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.ValueValidationUtil;
import org.eclipse.core.runtime.Assert;

public class PropertiesLoaderFromFile extends CredentialsLoader {

	public static final String CLOUDFOUNDRY_TEST_CREDENTIALS_PROPERTY = "test.credentials";

	public static final String PASSWORD_PROPERTY = "password";

	public static final String USEREMAIL_PROPERTY = "username";

	public static final String ORG_PROPERTY = "org";

	public static final String SPACE_PROPERTY = "space";

	public static final String URL_PROPERTY = "url";

	public static final String BUILDPACK_PROPERTY = "buildpack";

	public static final String SELF_SIGNED_CERTIFICATE_PROPERTY = "selfsigned";

	protected Properties readFromFile(String propertiesLocation) throws Exception {

		File propertiesFile = new File(propertiesLocation);

		InputStream fileInputStream = null;
		try {
			if (propertiesFile.exists() && propertiesFile.canRead()) {
				fileInputStream = new FileInputStream(propertiesFile);
				Properties properties = new Properties();
				properties.load(fileInputStream);
				return properties;
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

		return null;
	}

	protected String getUrl(Properties properties) {
		String url = getRequiredProperty(URL_PROPERTY, properties);
		if (!url.startsWith("http")) {
			url = "http://" + url;
		}
		return url;
	}

	protected boolean getSkipSsl(Properties properties) {
		String selfSignedVal = properties.getProperty(SELF_SIGNED_CERTIFICATE_PROPERTY);
		return "true".equals(selfSignedVal) || "TRUE".equals(selfSignedVal);
	}

	protected String getRequiredProperty(String property, Properties properties) {
		String value = properties.getProperty(property);
		Assert.isLegal(!ValueValidationUtil.isEmpty(value),
				"The property '" + property + "' must be set in a credentials text file and the file must passed as a VM arg:  \"-D"
						+ CLOUDFOUNDRY_TEST_CREDENTIALS_PROPERTY);
		return value;
	}

	@Override
	public CredentialProperties getCredentialProperties() throws Exception {
		String propertiesLocation = System.getProperty(CLOUDFOUNDRY_TEST_CREDENTIALS_PROPERTY);

		if (propertiesLocation == null) {
			throw CloudErrorUtil.toCoreException(
					"No Cloud Foundry credential properties file found. Ensure that the launch configuration arguments includes a property: "
							+ CLOUDFOUNDRY_TEST_CREDENTIALS_PROPERTY
							+ " that points to a file containing CF credentials. See Readme file in CFT test plugin.");
		}

		Properties properties = readFromFile(propertiesLocation);
		if (properties != null) {
			CredentialProperties credentialsProps = new CredentialProperties(getUrl(properties),
					getRequiredProperty(USEREMAIL_PROPERTY, properties),
					getRequiredProperty(PASSWORD_PROPERTY, properties), getRequiredProperty(ORG_PROPERTY, properties),
					getRequiredProperty(SPACE_PROPERTY, properties), properties.getProperty(BUILDPACK_PROPERTY),
					getSkipSsl(properties));

			String successfulLoadedMessage = "Successfully loaded Cloud account information from credentials file : "
					+ propertiesLocation;
			credentialsProps.setSuccessLoadedMessage(successfulLoadedMessage);
			return credentialsProps;
		}

		return null;
	}

}
