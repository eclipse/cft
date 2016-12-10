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

import org.eclipse.cft.server.core.internal.CloudErrorUtil;

public abstract class CredentialsLoader {

	private static final CredentialsLoader[] LOADERS = new CredentialsLoader[] { new PropertiesLoaderFromFile(),
			new PropertiesLoaderFromEnvVar() };

	public abstract CredentialProperties getCredentialProperties() throws Exception;

	public static CredentialProperties loadProperties() throws Exception {

		CredentialProperties properties = null;
		Exception error = null;
		for (CredentialsLoader credentialsLoader : LOADERS) {
			try {
				properties = credentialsLoader.getCredentialProperties();
				if (properties != null) {
					CloudFoundryTestFixture.log(properties.getSuccessLoadedMessage());
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
						"No Cloud credential properties found. Ensure Cloud account information is either set in environment variables or defined in a credentials file.");
			}
		}
		else {
			return properties;
		}
	}

}
