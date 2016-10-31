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

import org.eclipse.core.runtime.CoreException;

public interface CFCloudCredentials {

	/**
	 * Get the email.
	 *
	 * @return the email
	 */
	String getUser();

	/**
	 * Get the password.
	 *
	 * @return the password
	 */
	String getPassword();

	/**
	 * Get the the full auth token in JSON format.
	 *
	 * @return the token. May be null if credentials (username/password) or
	 * one-time passcode are passed instead
	 */
	String getAuthTokenAsJson() throws CoreException;

	/**
	 * Get the client ID.
	 *
	 * @return the client ID
	 */
	String getClientId();

	/**
	 * Get the client secret
	 *
	 * @return the client secret
	 */
	String getClientSecret();

	/**
	 * Get the proxy user.
	 *
	 * @return the proxy user
	 */
	String getProxyUser();

	/**
	 * Is this a proxied set of credentials?
	 *
	 * @return whether a proxy user is set
	 */
	boolean isProxyUserSet();

	/**
	 * Indicates weather the token stored in the cloud credentials can be
	 * refreshed or not. This is useful when the token stored in this object was
	 * obtained via implicit OAuth2 authentication and therefore can not be
	 * refreshed.
	 *
	 * @return weather the token can be refreshed
	 */
	boolean isRefreshable();

	/**
	 * Get the OTP passcode
	 *
	 * @return the passcode
	 */
	String getPasscode();

	/**
	 * Is this a authentication attempt with OTP passcode?
	 *
	 * @return whether a passcode is set
	 */
	boolean isPasscodeSet();

}