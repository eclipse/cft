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

import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.core.runtime.CoreException;

public abstract class PropertiesLoader {

	public static final String CLOUDFOUNDRY_TEST_CREDENTIALS_PROPERTY = "test.credentials";

	private static final PropertiesLoader[] LOADERS = new PropertiesLoader[] { new PropertiesLoaderFromFile(),
			new PropertiesLoaderFromEnvVar() };

	public abstract HarnessProperties getProperties() throws Exception;

	public static HarnessProperties loadProperties() throws Exception {

		HarnessProperties properties = null;
		Exception error = null;
		for (PropertiesLoader credentialsLoader : LOADERS) {
			try {
				properties = credentialsLoader.getProperties();
				if (properties != null) {
					break;
				}
			}
			catch (Exception e) {
				error = e;
			}
		}

		if (properties == null) {
			if (error != null) {
				throw error;
			}
			else {
				throw noPropertiesError();
			}
		}
		else {
			return properties;
		}
	}

	protected static CoreException noPropertiesError() {
		return CloudErrorUtil.toCoreException(
				"No Cloud properties found. Cloud information must be set in environment variables, OR defined in a properties file, and file passed by VM arg: -D"
						+ CLOUDFOUNDRY_TEST_CREDENTIALS_PROPERTY + "=[file path]");

	}

}
