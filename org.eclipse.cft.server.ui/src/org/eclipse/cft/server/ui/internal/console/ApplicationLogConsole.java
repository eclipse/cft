/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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

import java.util.List;

import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.log.CloudLog;
import org.eclipse.core.runtime.CoreException;

/**
 * Application Log console that manages loggregator streams for a deployed
 * application. This console should only be created and used for applications
 * that are already published, as it initialises loggregator support which
 * requires the application to exist in the Cloud server.
 * 
 * @author Steffen Pingel
 * @author Christian Dupuis
 * @author Nieraj Singh
 */
class ApplicationLogConsole extends CloudFoundryConsole {

	public ApplicationLogConsole(ConsoleConfig config) {
		super(config);
	}

	public synchronized void writeApplicationLogs(List<ApplicationLog> logs) {
		if (logs != null) {
			for (ApplicationLog log : logs) {
				writeApplicationLog(log);
			}
		}
	}

	/**
	 * Writes a loggregator application log to a corresponding console stream.
	 * This is different from {@link #writeToStream(CloudLog)} in the sense that
	 * the latter writes a local log and does not handle special cases for
	 * loggregator.
	 * @param log
	 */
	protected synchronized void writeApplicationLog(ApplicationLog log) {
		if (log == null) {
			return;
		}
		try {
			// Write to the application console stream directly, as the
			// Application log stream does
			// additional processing on the raw application log that may not be performed
			// by the base CloudFoundryConsole
			ConsoleStream stream = getStream(StandardLogContentType.APPLICATION_LOG);
			if (stream instanceof ApplicationLogConsoleStream) {
				((ApplicationLogConsoleStream) stream).write(log);
			}
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}

	}
}
