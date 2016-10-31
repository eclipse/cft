/*******************************************************************************
 * Copyright (c) 2016 Pivotal Software, Inc. 
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

import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ApplicationLog.MessageType;

public class AppLogUtil {

	public static LogContentType getTypeFromV1(MessageType v1Type) {
		if (v1Type == null) {
			return LogContentType.APPLICATION_LOG_UNKNOWN;
		}
		switch (v1Type) {
		case STDOUT:
			return LogContentType.APPLICATION_LOG_STD_OUT;
		case STDERR:
			return LogContentType.APPLICATION_LOG_STS_ERROR;
		default:
			return LogContentType.APPLICATION_LOG_UNKNOWN;
		}
	}

	public static MessageType getV1Type(LogContentType logType) {
		if (logType == LogContentType.APPLICATION_LOG_STS_ERROR) {
			return MessageType.STDERR;
		}
		else {
			return MessageType.STDOUT;
		}
	}

	public static CloudLog getLogFromV1(ApplicationLog v1Log) {
		return new CloudLog(v1Log.getAppId(), format(v1Log.getMessage()), v1Log.getTimestamp(),
				AppLogUtil.getTypeFromV1(v1Log.getMessageType()), v1Log.getSourceName(), v1Log.getSourceId());

	}

	public static ApplicationLog getV1Log(CloudLog log) {
		return new ApplicationLog(log.getAppId(), log.getMessage(), log.getTimestamp(), getV1Type(log.getLogType()),
				log.getSourceName(), log.getSourceId());
	}

	public static String format(String message) {
		if (message.contains("\n") || message.contains("\r")) //$NON-NLS-1$ //$NON-NLS-2$
		{
			return message;
		}
		return message + '\n';
	}

}
