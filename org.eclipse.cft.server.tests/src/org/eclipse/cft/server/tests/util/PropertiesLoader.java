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

public abstract class PropertiesLoader {

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
				throw CloudErrorUtil.toCoreException(
						"No Cloud properties found for the test harness. Ensure Cloud account information is either set in environment variables or defined in a properties file.");
			}
		}
		else {
			return properties;
		}
	}

}
