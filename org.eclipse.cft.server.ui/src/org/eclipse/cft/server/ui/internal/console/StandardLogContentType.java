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
package org.eclipse.cft.server.ui.internal.console;

import org.eclipse.cft.server.core.internal.log.LogContentType;

public class StandardLogContentType {

	/**
	 * Local std out. May be different that an application's log std out from
	 * the Cloud Foundry server. This allows local std out (for example, local
	 * progress messages) to be distinct from remote std out logs.
	 */

	public static final LogContentType STD_OUT = new LogContentType("stdout"); //$NON-NLS-1$

	/**
	 * Local std error. May be different that an application's log std error
	 * from the Cloud Foundry server. This allows local std error (for example,
	 * exceptions thrown locally) to be distinct from remote std error logs.
	 */
	public static final LogContentType STD_ERROR = new LogContentType("stderror"); //$NON-NLS-1$

	/**
	 * Generic application log type that is used for both loggregator and file
	 * log stream. NOTE: May be deprecated once file log streaming is removed.
	 */
	public static final LogContentType APPLICATION_LOG = new LogContentType("applicationlog"); //$NON-NLS-1$

	/**
	 * @deprecated only used by file log streaming, which is no longer supported.
	 */
	public static final LogContentType SHOW_EXISTING_LOGS = new LogContentType("existingLogs"); //$NON-NLS-1$

}
