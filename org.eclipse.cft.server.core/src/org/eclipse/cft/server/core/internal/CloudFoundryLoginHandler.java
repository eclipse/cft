/*******************************************************************************
 * Copyright (c) 2012, 2016 Pivotal Software, Inc. and others 
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
package org.eclipse.cft.server.core.internal;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.eclipse.cft.server.core.internal.client.AbstractWaitWithProgressJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CloudFoundryLoginHandler {

	private final CloudFoundryOperations operations;

	private static final String DEFAULT_PROGRESS_LABEL = Messages.CloudFoundryLoginHandler_LABEL_PERFORM_CF_OPERATION;

	private static final int DEFAULT_PROGRESS_TICKS = 100;

	// Optional
	private CloudFoundryServer server;
	
	/**
	 * 
	 * @param operations must not be null
	 * @param cloudServer can be null if no server has been created yet
	 */
	public CloudFoundryLoginHandler(CloudFoundryOperations operations, CloudFoundryServer server) {
		this.operations = operations;
		this.server = server;
	}

	/**
	 * Attempts to log in once. If login fails, Core exception is thrown
	 * @throws CoreException if login failed. The reason for the login failure
	 * is contained in the core exception's
	 */
	public OAuth2AccessToken login(IProgressMonitor monitor) throws CoreException {
		return login(monitor, 1, 0);
	}

	/**
	 * Attempts a log in for the specified amount of attempts, and waits by the
	 * specified sleep time between each attempt. If at the end of the attempts,
	 * login has failed, Core exception is thrown.
	 */
	public OAuth2AccessToken login(IProgressMonitor monitor, int tries, long sleep) throws CoreException {
		return internalLogin(monitor, tries, sleep);
	}

	protected OAuth2AccessToken internalLogin(IProgressMonitor monitor, int tries, long sleep) throws CoreException {
		return new AbstractWaitWithProgressJob<OAuth2AccessToken>(tries, sleep) {

			@Override
			protected OAuth2AccessToken runInWait(IProgressMonitor monitor) throws CoreException {
				// Do not wrap CloudFoundryException or RestClientException in a
				// CoreException.
				// as they are uncaught exceptions and can be inspected directly
				// by the shouldRetryOnError(..) method.
				OAuth2AccessToken token = operations.login();
				
				if(server != null && server.isSso()) {
					// Store the SSO token in the server
					try {
						String tokenValue = new ObjectMapper().writeValueAsString(token);
						server.setAndSaveToken(tokenValue);
					}
					catch (JsonProcessingException e) {
						CloudFoundryPlugin.logWarning(e.getMessage());
					}					
				}
				
				return token;
				
			}

			@Override
			protected boolean shouldRetryOnError(Throwable t) {
				return shouldAttemptClientLogin(t);
			}

		}.run(monitor);
	}

	protected SubMonitor getProgressMonitor(IProgressMonitor progressMonitor) {
		return progressMonitor instanceof SubMonitor ? (SubMonitor) progressMonitor : SubMonitor.convert(
				progressMonitor, DEFAULT_PROGRESS_LABEL, DEFAULT_PROGRESS_TICKS);
	}

	public boolean shouldAttemptClientLogin(Throwable t) {
		return CloudErrorUtil.getInvalidCredentialsError(t) != null;
	}

	/**
	 * 
	 * @return true if there was a proxy update. False any other case.
	 * @throws CoreException
	 */
	public boolean updateProxyInClient(CloudFoundryOperations client) throws CoreException {
		// if (client != null && cloudURL != null) {
		// try {
		// URL actualUrl = new URL(cloudURL);
		// HttpProxyConfiguration proxyConfiguration =
		// CloudFoundryClientFactory.getProxy(actualUrl);
		// // FIXNS: As of CF Java client-lib version 1.0.2, update proxy
		// // API has been removed. Therefore unless a new client
		// // is created on proxy change, or the client indirectly detects
		// // proxy changes via system properties
		// // Proxy support for CF Eclipse will not work unless a user
		// // reconnects the server instance when the client
		// // is created.
		// client.updateHttpProxyConfiguration(proxyConfiguration);
		//
		// return true;
		// }
		// catch (MalformedURLException e) {
		// throw
		// CloudErrorUtil.toCoreException("Failed to update proxy settings due to "
		// + e.getMessage(), e);
		// }
		// }
		return false;
	}

}
