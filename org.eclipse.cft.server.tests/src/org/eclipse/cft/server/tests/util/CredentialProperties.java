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

public class CredentialProperties {

	/**
	 *
	 */

	public final String userEmail;

	public final String password;

	public final String organization;

	public final String space;

	public final String url;

	public final boolean selfSignedCertificate;

	public final String buildPack;

	private String successLoadedMessage;

	public CredentialProperties(String url, String userEmail, String password, String organization, String space,
			String buildPack, boolean selfSignedCertificate) {
		this.url = url;
		this.userEmail = userEmail;
		this.password = password;
		this.organization = organization;
		this.space = space;
		this.selfSignedCertificate = selfSignedCertificate;
		this.buildPack = buildPack;
	}

	public void setSuccessLoadedMessage(String successLoadedMessage) {
		this.successLoadedMessage = successLoadedMessage;
	}

	public String getSuccessLoadedMessage() {
		return successLoadedMessage;
	}

}
