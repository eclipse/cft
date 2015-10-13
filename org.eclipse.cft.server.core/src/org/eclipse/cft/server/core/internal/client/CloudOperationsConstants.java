/*******************************************************************************
 * Copyright (c) 2014, 2015 Pivotal Software, Inc. 
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

public class CloudOperationsConstants {

	/*
	 * Intervals are how long a thread should sleep before moving to the next
	 * iteration, or how long a refresh operation should wait before refreshing
	 * the deployed apps.
	 */
	public static final long DEFAULT_INTERVAL = 60 * 1000;

	public static final long SHORT_INTERVAL = 5 * 1000;

	public static final long MEDIUM_INTERVAL = 10 * 1000;

	public static final long ONE_SECOND_INTERVAL = 1000;

	public static final long LOGIN_INTERVAL = 2000;

	public static final long DEPLOYMENT_TIMEOUT = 10 * 60 * 1000;

	public static final long UPLOAD_TIMEOUT = 60 * 1000;

	public static final long DEFAULT_CF_CLIENT_REQUEST_TIMEOUT = 15 * 1000;
}
