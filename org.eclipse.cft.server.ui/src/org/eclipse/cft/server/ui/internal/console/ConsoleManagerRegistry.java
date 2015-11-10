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

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.log.CloudLog;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;

/**
 * 
 * Registry that loads console managers for different servers. For example,
 * certain servers may use log file streaming whereas others application log
 * callbacks for loggregator. Based on the server, the registry will load the
 * appropriate console manager.
 * <p/>
 * In addition, the registry also manages a common trace console that is
 * applicable to any server.
 */
public class ConsoleManagerRegistry {

	public static final String CLOUD_FOUNDRY_TRACE_CONSOLE_NAME = "Cloud Foundry Trace"; //$NON-NLS-1$

	static final String TRACE_CONSOLE_ID = "org.eclipse.cft.server.trace"; //$NON-NLS-1$

	private static ConsoleManagerRegistry registry;

	private CloudFoundryConsole traceConsole;

	private IConsoleManager consoleManager;

	/**
	 * Loggregator-supporting console manager
	 */
	private CloudConsoleManager appConsoleManager = new ApplicationLogConsoleManager();

	private final IConsoleListener listener = new IConsoleListener() {

		public void consolesAdded(IConsole[] consoles) {
			// ignore

		}

		public void consolesRemoved(IConsole[] consoles) {
			for (IConsole console : consoles) {
				if (TRACE_CONSOLE_ID.equals(console.getType()) && (traceConsole != null)) {
					traceConsole.stop();
					traceConsole = null;
				}
			}
		}
	};

	public ConsoleManagerRegistry() {
		consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		consoleManager.addConsoleListener(listener);
	}

	public static ConsoleManagerRegistry getInstance() {
		if (registry == null) {
			registry = new ConsoleManagerRegistry();
		}
		return registry;
	}

	public static CloudConsoleManager getConsoleManager(CloudFoundryServer cloudServer) {
		return getInstance().getCloudConsoleManager(cloudServer);
	}

	/*
	 * INSTANCE METHODS
	 */
	/**
	 * Returns a console manager appropriate for the given server. If the server
	 * uses log file streaming , then a file console manager is returned.
	 * <p/>
	 * Otherwise, if the server uses loggregator or log callbacks, or it's not
	 * possible to determine the logging mechanism of the server, by default an
	 * application log console manager (i.e. a console manager that uses
	 * callbacks to obtain application logs) is returned.
	 * @param cloudServer
	 * @return non-null console manager based on the server type.
	 */
	public CloudConsoleManager getCloudConsoleManager(CloudFoundryServer cloudServer) {
		return appConsoleManager;
	}

	/**
	 * Makes the general Cloud Foundry trace console visible in the console
	 * view.
	 */
	public void setTraceConsoleVisible() {
		CloudFoundryConsole console = getTraceConsoleStream();
		if (console != null) {
			consoleManager.showConsoleView(console.getConsole());
		}
	}

	/**
	 * Sends a trace message to a Cloud Foundry trace console.
	 * 
	 * @param log if null, nothing is written to trace console.
	 * @param clear whether trace console should be cleared prior to displaying
	 * the trace message.
	 */
	public void trace(CloudLog log, boolean clear) {
		if (log == null) {
			return;
		}
		try {
			CloudFoundryConsole console = getTraceConsoleStream();

			if (console != null) {
				// Do not make trace visible as another console may be visible
				// while
				// tracing is occuring.
				console.writeToStream(log);
			}
		}
		catch (Throwable e) {
			CloudFoundryPlugin.logError(e);
		}
	}

	protected synchronized CloudFoundryConsole getTraceConsoleStream() {

		if (traceConsole == null) {
			MessageConsole messageConsole = null;
			for (IConsole console : ConsolePlugin.getDefault().getConsoleManager().getConsoles()) {
				if (console instanceof MessageConsole && console.getName().equals(CLOUD_FOUNDRY_TRACE_CONSOLE_NAME)) {
					messageConsole = (MessageConsole) console;
				}
			}
			if (messageConsole == null) {
				messageConsole = new MessageConsole(CLOUD_FOUNDRY_TRACE_CONSOLE_NAME, TRACE_CONSOLE_ID, null, true);
			}
			traceConsole = new CloudFoundryConsole(new ConsoleConfig(messageConsole, null, null));

			ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { messageConsole });

		}

		return traceConsole;
	}

}
