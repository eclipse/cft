/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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

public class ValidationEvents {

	/**
	 * Indicates that account values (e.g. username, password, server URL, cloud
	 * space), has been filled locally. This should only trigger local
	 * credential validation, not a remote server authorization or cloud space
	 * validation.
	 */
	public static final int CREDENTIALS_FILLED = 1000;

	/**
	 * Indicates a self-signed server has been detected
	 */
	public static final int SELF_SIGNED = 1001;

	/**
	 * Indicates an event where remote server authorisation of credentials is
	 * required. This is specialisation of {@link #VALIDATION} in the sense an
	 * explicit server authorisation is required, and therefore has higher
	 * priority than a {@link #VALIDATION} event
	 */
	public static final int SERVER_AUTHORISATION = 1002;

	/**
	 * Indicates that validation has been requested or completed. The reason
	 * that the same event is used to both indicate a validation request as well
	 * as a completion is that
	 */
	public static final int VALIDATION = 1003;

	public static final int EVENT_NONE = -1;
}
