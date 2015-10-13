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
package org.eclipse.cft.server.core.internal.log;

public class TraceType {
	public static final LogContentType HTTP_OK = new LogContentType("httpok"); //$NON-NLS-1$

	public static final LogContentType HTTP_ERROR = new LogContentType("httperror"); //$NON-NLS-1$

	public static final LogContentType HTTP_GENERAL = new LogContentType("general"); //$NON-NLS-1$
}
