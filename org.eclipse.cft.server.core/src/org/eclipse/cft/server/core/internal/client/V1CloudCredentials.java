/*******************************************************************************
 * Copyright (c) 2016 Pivotal Software, Inc. and others 
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
package org.eclipse.cft.server.core.internal.client;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudUtil;
import org.eclipse.core.runtime.CoreException;

import com.fasterxml.jackson.core.JsonProcessingException;

public class V1CloudCredentials implements CFCloudCredentials {

	private final CloudCredentials v1Credentials;

	/**
	 * Create credentials using email and password.
	 *
	 * @param email email to authenticate with
	 * @param password the password
	 */
	public V1CloudCredentials(CloudCredentials v1Credentials) {
		this.v1Credentials = v1Credentials;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CFCloudCredentials#getUser()
	 */
	@Override
	public String getUser() {
		return v1Credentials.getEmail();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CFCloudCredentials#getPassword()
	 */
	@Override
	public String getPassword() {
		return v1Credentials.getPassword();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CFCloudCredentials#getAuthTokenAsJson()
	 */
	@Override
	public String getAuthTokenAsJson() throws CoreException{
		try {
			return CloudUtil.getTokenAsJson(v1Credentials.getToken());
		}
		catch (JsonProcessingException e) {
			throw CloudErrorUtil.toCoreException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CFCloudCredentials#getClientId()
	 */
	@Override
	public String getClientId() {
		return v1Credentials.getClientId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CFCloudCredentials#getClientSecret()
	 */
	@Override
	public String getClientSecret() {
		return v1Credentials.getClientSecret();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CFCloudCredentials#getProxyUser()
	 */
	@Override
	public String getProxyUser() {
		return v1Credentials.getProxyUser();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CFCloudCredentials#isProxyUserSet()
	 */
	@Override
	public boolean isProxyUserSet() {
		return v1Credentials.isProxyUserSet();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CFCloudCredentials#isRefreshable()
	 */
	@Override
	public boolean isRefreshable() {
		return v1Credentials.isRefreshable();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CFCloudCredentials#getPasscode()
	 */
	@Override
	public String getPasscode() {
		return v1Credentials.getPasscode();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CFCloudCredentials#isPasscodeSet()
	 */
	@Override
	public boolean isPasscodeSet() {
		return v1Credentials.isPasscodeSet();
	}

}
