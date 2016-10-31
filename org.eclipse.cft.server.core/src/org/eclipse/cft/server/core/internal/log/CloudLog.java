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
 * Contributors: Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.log;

import java.util.Date;

/**
 * A log message that contains the message and message type. This may be used to
 * model both local log messages (for example writing to local standard out) or
 * an actual loggregator application log for a published app. The
 * {@link LogContentType} indicates whether it is a local log or a log from a
 * published application that is being streamed to a console.
 *
 */
public class CloudLog {

	private final LogContentType logType;

	private final String message;

	private String appId;

	private Date timestamp;

	private String sourceName;

	private String sourceId;

	public CloudLog(String appId, String message, Date timestamp, LogContentType logType, String sourceName,
			String sourceId) {
		this.appId = appId;
		this.message = message;
		this.timestamp = timestamp;
		this.logType = logType;
		this.sourceName = sourceName;
		this.sourceId = sourceId;
	}

	public CloudLog(String message, LogContentType logType) {
		this.message = message;
		this.logType = logType;
	}


	public String getAppId() {
		return appId;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public String getSourceName() {
		return sourceName;
	}

	public String getSourceId() {
		return sourceId;
	}

	public LogContentType getLogType() {
		return logType;
	}

	public String getMessage() {
		return message;
	}

	public String toString() {
		return logType + " - " + message; //$NON-NLS-1$
	}

}
