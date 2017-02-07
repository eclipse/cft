/*******************************************************************************
 * Copyright (c) 2016, 2017 Pivotal Software, Inc. and others
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

import org.eclipse.cft.server.core.internal.ValueValidationUtil;
import org.eclipse.core.runtime.Assert;

public class PropertiesLoaderFromEnvVar extends PropertiesLoader {
	private static final String CFT_TEST_SPACE = "CFT_TEST_SPACE";

	private static final String CFT_TEST_ORG = "CFT_TEST_ORG";

	private static final String CFT_TEST_PASSWORD = "CFT_TEST_PASSWORD";

	private static final String CFT_TEST_USER = "CFT_TEST_USER";

	private static final String CFT_TEST_URL = "CFT_TEST_URL";

	private static final String CFT_TEST_SKIP_SSL = "CFT_TEST_SKIP_SSL";

	private static final String CFT_TEST_BUILDPACK = "CFT_TEST_BUILDPACK";

	private static final String CFT_TEST_SERVICE_NAME = "CFT_TEST_SERVICE_NAME";

	private static final String CFT_TEST_SERVICE_TYPE = "CFT_TEST_SERVICE_TYPE";

	private static final String CFT_TEST_SERVICE_PLAN = "CFT_TEST_SERVICE_PLAN";

	public String getRequiredEnv(String name) throws Exception {
		String value = System.getenv(name);
		Assert.isLegal(!ValueValidationUtil.isEmpty(value), "The environment variable '" + name + "' must be set");
		return value;
	}

	private boolean getEnvBoolean(String name) {
		String value = System.getenv(name);
		if (value == null) {
			return false;
		}
		return Boolean.parseBoolean(value);
	}

	public String getEnv(String name) {
		return System.getenv(name);
	}

	@Override
	public HarnessProperties getProperties() throws Exception {
		String url = getRequiredEnv(CFT_TEST_URL);
		if (!url.startsWith("http")) {
			url = "http://" + url;
		}

		String user = getRequiredEnv(CFT_TEST_USER);
		String password = getRequiredEnv(CFT_TEST_PASSWORD);
		String org = getRequiredEnv(CFT_TEST_ORG);
		String space = getRequiredEnv(CFT_TEST_SPACE);
		String buildpack = getEnv(CFT_TEST_BUILDPACK);
		boolean skipSsl = getEnvBoolean(CFT_TEST_SKIP_SSL);
		String serviceName = getRequiredEnv(CFT_TEST_SERVICE_NAME);
		String serviceType = getRequiredEnv(CFT_TEST_SERVICE_TYPE);
		String servicePlan = getRequiredEnv(CFT_TEST_SERVICE_PLAN);

		HarnessProperties properties = HarnessPropertiesBuilder.instance().target(url, org, space, skipSsl)
				.credentials(user, password).buildpack(buildpack).service(serviceName, serviceType, servicePlan)
				.successfulLoadedMessage(getSuccessLoadedMessage()).build();
		return properties;

	}

	private String getSuccessLoadedMessage() {
		return "Successfully loaded Cloud account information from environment variables.";
	}

}
