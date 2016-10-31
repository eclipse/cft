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

import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudUtil;
import org.eclipse.cft.server.core.internal.log.CFApplicationLogListener;
import org.eclipse.cft.server.core.internal.log.CFStreamingLogToken;
import org.eclipse.cft.server.core.internal.log.CloudLog;
import org.eclipse.core.runtime.CoreException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

/**
 * Hybrid client that uses authentication from v1 client to authenticate the
 * other client.
 *
 */
public class HybridClient implements CFClient {

	private final CloudFoundryOperations v1Client;

	private final CFClient otherClient;

	public HybridClient(CloudFoundryOperations v1Client, CFClient otherClient) {
		this.v1Client = v1Client;
		this.otherClient = otherClient;
	}

	@Override
	public String login() throws CoreException {
		try {
			// Make sure to save the token if the v1 client did a login
			OAuth2AccessToken oauth = v1Client.login();
			if (oauth != null) {
				String asJson = CloudUtil.getTokenAsJson(oauth);
				
				this.otherClient.login();

				return asJson;
			}
		}
		catch (Throwable e) {
			throw CloudErrorUtil.toCoreException(e);
		}
		throw CloudErrorUtil.toCoreException(
				"Failed to login using v1 client. No OAuth2AccessToken resolved. Check if credentials or passcode are valid."); //$NON-NLS-1$
	}

	@Override
	public CFStreamingLogToken streamLogs(String appName, CFApplicationLogListener listener) throws CoreException {
		return otherClient.streamLogs(appName, listener);
	}

	@Override
	public List<CloudLog> getRecentLogs(String appName) throws CoreException {
		return otherClient.getRecentLogs(appName);
	}

}
