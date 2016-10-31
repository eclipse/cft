/*******************************************************************************
 * Copyright (c) 2014, 2016 Pivotal Software, Inc. and others
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

/**
 * 
 * Abstraction used to indicate a trace type. May be used to distinguish
 * different types of trace messages (e.g. ERROR vs OK).
 */
public class LogContentType {

	/*
	 * Log content types that are specific to streaming or fetching recent logs
	 * of published applications that are currently running on the Cloud server
	 */
	public static final LogContentType APPLICATION_LOG_STS_ERROR = new LogContentType("applicationlogstderror"); //$NON-NLS-1$

	public static final LogContentType APPLICATION_LOG_STD_OUT = new LogContentType("applicationlogstdout"); //$NON-NLS-1$

	public static final LogContentType APPLICATION_LOG_UNKNOWN = new LogContentType("applicationlogunknown"); //$NON-NLS-1$

	private final String id;

	public LogContentType(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LogContentType other = (LogContentType) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		}
		else if (!id.equals(other.id))
			return false;
		return true;
	}

	public String toString() {
		return id;
	}

}
