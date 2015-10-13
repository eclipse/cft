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

import org.cloudfoundry.client.lib.RestLogEntry;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;

/**
 * Cloud tracer that fires trace events when a trace request is received from
 * the Cloud Foundry trace framework.
 *
 */
public abstract class CloudTracer implements ICloudTracer {

	public void traceNewLogEntry(RestLogEntry restLogEntry) {

		if (restLogEntry == null || !HttpTracer.getCurrent().isEnabled()) {
			return;
		}
		doTrace(restLogEntry);
	}

	/**
	 * 
	 * @param restLogEntry non-null log entry, invoked only when tracing is
	 * enabled.
	 */
	abstract void doTrace(RestLogEntry restLogEntry);

	/**
	 * Utility method for tracing a message based on a {@link LogContentType}.
	 * When invoked, it will notify listeners of a trace event.
	 * @param message if null, nothing is traced.
	 * @param type of trace. This allows listeners to perform a specific type of
	 * operation on the trace message.
	 */
	protected void fireTraceEvent(CloudLog log) {
		if (log == null) {
			return;
		}

		try {
			CloudFoundryPlugin.getCallback().trace(log, false);
		}
		catch (Throwable t) {
			// Failure in tracing. Catch as to not prevent any further framework
			// operations.
			CloudFoundryPlugin.logError(t);
		}
	}

	/**
	 * 
	 * @return current cloud tracer used to stream trace content. Should never
	 * be null.
	 */
	public static ICloudTracer getCurrentCloudTracer() {
		// Add option for Framework here to load third-party tracers. For now,
		// just return a default tracer
		return new DefaultCloudTracer();
	}

}
