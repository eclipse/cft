/*******************************************************************************
 * Copyright (c) 2017 Pivotal Software, Inc. and others
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

public class HarnessPropertiesBuilder {

	private HarnessPropertiesImpl properties;

	public HarnessPropertiesBuilder target(String apiUrl, String org, String space, boolean selfSignedCertificate) {
		getProperties().setTarget(apiUrl, org, space, selfSignedCertificate);
		return this;
	}

	public HarnessPropertiesBuilder buildpack(String buildpack) {
		getProperties().setBuildpack(buildpack);
		return this;
	}

	public HarnessPropertiesBuilder service(String name, String type, String plan) {
		getProperties().setService(new HarnessCFService(name, type, plan));
		return this;
	}

	public HarnessPropertiesBuilder credentials(String userEmail, String password) {
		getProperties().setCredentials(userEmail, password);
		return this;
	}

	public HarnessPropertiesBuilder successfulLoadedMessage(String message) {
		getProperties().setSuccessLoadedMessage(message);
		return this;
	}

	public HarnessProperties build() {
		return getProperties();
	}

	public static HarnessPropertiesBuilder instance() {
		return new HarnessPropertiesBuilder();
	}

	private HarnessPropertiesImpl getProperties() {
		if (properties == null) {
			properties = new HarnessPropertiesImpl();
		}
		return properties;
	}

	//////////////////////////////////////////////////

	public static class HarnessCFService {

		public final String serviceType;

		public final String servicePlan;

		public final String serviceName;

		public HarnessCFService(String serviceName, String ServiceType, String servicePlan) {
			this.serviceName = serviceName;
			this.serviceType = ServiceType;
			this.servicePlan = servicePlan;
		}
	}

	private class HarnessPropertiesImpl implements HarnessProperties {

		/**
		 *
		 */
		public String userEmail;

		public String password;

		public String apiUrl;

		public String org;

		public String space;

		public boolean skipSslValidation;

		private String buildpack;

		private String successLoadedMessage;

		private HarnessCFService cfService;

		public HarnessPropertiesImpl() {

		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.eclipse.cft.server.tests.util.HarnessProperties#getUsername()
		 */
		@Override
		public String getUsername() {
			return this.userEmail;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.eclipse.cft.server.tests.util.HarnessProperties#getPassword()
		 */
		@Override
		public String getPassword() {
			return this.password;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.cft.server.tests.util.HarnessProperties#getApiUrl()
		 */
		@Override
		public String getApiUrl() {
			return this.apiUrl;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.cft.server.tests.util.HarnessProperties#getOrg()
		 */
		@Override
		public String getOrg() {
			return this.org;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.cft.server.tests.util.HarnessProperties#getSpace()
		 */
		@Override
		public String getSpace() {
			return this.space;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.eclipse.cft.server.tests.util.HarnessProperties#skipSslValidation
		 * ()
		 */
		@Override
		public boolean skipSslValidation() {
			return this.skipSslValidation;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.eclipse.cft.server.tests.util.HarnessProperties#getBuildpack()
		 */
		@Override
		public String getBuildpack() {
			return this.buildpack;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.cft.server.tests.util.HarnessProperties#
		 * getSuccessLoadedMessage()
		 */
		@Override
		public String getSuccessLoadedMessage() {
			return successLoadedMessage;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.cft.server.tests.util.HarnessProperties#getService()
		 */
		@Override
		public HarnessCFService serviceToCreate() {
			return this.cfService;
		}

		private void setService(HarnessCFService cfService) {
			this.cfService = cfService;

		}

		private void setBuildpack(String buildpack) {
			this.buildpack = buildpack;
		}

		private void setSuccessLoadedMessage(String successLoadedMessage) {
			this.successLoadedMessage = successLoadedMessage;
		}

		private void setCredentials(String userEmail, String password) {
			this.userEmail = userEmail;
			this.password = password;
		}

		private void setTarget(String apiUrl, String org, String space, boolean skipSslValidation) {
			this.apiUrl = apiUrl;
			this.org = org;
			this.space = space;
			this.skipSslValidation = skipSslValidation;

		}

	}

}
