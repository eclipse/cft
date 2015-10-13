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

import java.io.StringWriter;

import org.cloudfoundry.client.lib.RestLogEntry;

/**
 * General-purpose tracer that parses a {@link RestLogEntry} into various String
 * traces, and assigns a {@link LogContentType} to each section of the log entry.
 *
 */
public class DefaultCloudTracer extends CloudTracer {

	static final String HTTP_TRACE_STATUS = "HTTP STATUS"; //$NON-NLS-1$

	static final String HTTP_TRACE_REQUEST = "REQUEST"; //$NON-NLS-1$

	static final String ERROR_STATUS = "ERROR"; //$NON-NLS-1$

	static final String TRACE_SEPARATOR = " :: "; //$NON-NLS-1$

	static final String SPACE = " "; //$NON-NLS-1$

	protected void doTrace(RestLogEntry restLogEntry) {

		StringWriter writer = new StringWriter();
		boolean isError = restLogEntry.getStatus() != null && ERROR_STATUS.equals(restLogEntry.getStatus());

		writer.append(restLogEntry.getStatus());

		writer.append(SPACE);
		writer.append(TRACE_SEPARATOR);
		writer.append(SPACE);

		writer.append(HTTP_TRACE_STATUS);
		writer.append(':');
		writer.append(SPACE);
		writer.append(restLogEntry.getHttpStatus().name());

		fireTraceEvent(getCloudLog(writer.toString(), isError ? TraceType.HTTP_ERROR : TraceType.HTTP_OK));

		writer = new StringWriter();
		writer.append(SPACE);
		writer.append(TRACE_SEPARATOR);
		writer.append(SPACE);
		writer.append(HTTP_TRACE_REQUEST);
		writer.append(':');
		writer.append(SPACE);
		writer.append(restLogEntry.getMethod().toString());

		writer.append(' ');
		writer.append(restLogEntry.getUri().toString());
		writer.append(TRACE_SEPARATOR);
		writer.append(restLogEntry.getMessage());
		writer.append('\n');

		fireTraceEvent(getCloudLog(writer.toString(), TraceType.HTTP_GENERAL));
	}

	protected CloudLog getCloudLog(String log, LogContentType type) {
		return new CloudLog(log, type);
	}

}
