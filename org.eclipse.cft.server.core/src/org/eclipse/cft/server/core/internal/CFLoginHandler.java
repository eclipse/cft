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
package org.eclipse.cft.server.core.internal;

import org.eclipse.cft.server.core.internal.client.AbstractWaitWithProgressJob;
import org.eclipse.cft.server.core.internal.client.CFClient;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

public class CFLoginHandler {

	private final CFClient client;

	private static final String DEFAULT_PROGRESS_LABEL = Messages.CloudFoundryLoginHandler_LABEL_PERFORM_CF_OPERATION;

	private static final int DEFAULT_PROGRESS_TICKS = 100;

	private CloudFoundryServer server;
	
	/**
	 * 
	 * @param client must not be null
	 * @param cloudServer can be null if no server has been created yet
	 */
	public CFLoginHandler(CFClient client, CloudFoundryServer server) {
		this.client = client;
		this.server = server;
	}

	/**
	 * Attempts to log in once. If login fails, Core exception is thrown
	 * @throws CoreException if login failed. The reason for the login failure
	 * is contained in the core exception's
	 * @return accessToken in JSON form
	 */
	public String login(IProgressMonitor monitor) throws CoreException {
		return login(monitor, 1, 0);
	}

	/**
	 * Attempts a log in for the specified amount of attempts, and waits by the
	 * specified sleep time between each attempt. If at the end of the attempts,
	 * login has failed, Core exception is thrown.
	 */
	public String login(IProgressMonitor monitor, int tries, long sleep) throws CoreException {
		return internalLogin(monitor, tries, sleep);
	}

	protected String internalLogin(IProgressMonitor monitor, int tries, long sleep) throws CoreException {
		return new AbstractWaitWithProgressJob<String>(tries, sleep) {

			@Override
			protected String runInWait(IProgressMonitor monitor) throws CoreException {
				// Do not wrap CloudFoundryException or RestClientException in a
				// CoreException.
				// as they are uncaught exceptions and can be inspected directly
				// by the shouldRetryOnError(..) method.
				String tokenValue = client.login();
				
				// Save the token for both SSO and credentials.
				if(server != null) {
					// Store the SSO token in the server
					server.setAndSaveToken(tokenValue);					
				}
				
				return tokenValue;
				
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
}
