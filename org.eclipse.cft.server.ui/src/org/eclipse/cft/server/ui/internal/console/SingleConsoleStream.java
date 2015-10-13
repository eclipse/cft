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
package org.eclipse.cft.server.ui.internal.console;

import java.io.IOException;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.log.LogContentType;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

public class SingleConsoleStream extends ConsoleStream {

	private final UILogConfig config;

	protected IOConsoleOutputStream outputStream;

	private MessageConsole console;

	public SingleConsoleStream(UILogConfig config) {
		this.config = config;
	}

	public synchronized boolean isActive() {
		if (this.console != null) {
			// Check if the console still exists before checking if the
			// console's
			// output stream is open
			IConsole[] activeConsoles = ConsolePlugin.getDefault().getConsoleManager().getConsoles();

			boolean isActive = false;
			if (activeConsoles != null) {
				for (IConsole activeConsole : activeConsoles) {
					if (activeConsole.equals(this.console)) {
						isActive = true;
						break;
					}
				}
			}

			if (isActive && outputStream != null && !outputStream.isClosed()) {
				return true;
			}
			else {
				// Console is no longer available. To avoid repeated checks on
				// the
				// console manager, clear the console reference
				close();
			}
		}
		return false;
	}

	/**
	 * Returns an active outputstream associated with the given log
	 * @return Returns the output stream IFF it is active. Returns null
	 * otherwise.
	 */
	protected synchronized IOConsoleOutputStream getOutputStream(LogContentType type) {
		if (isActive()) {
			return outputStream;
		}
		return null;
	}

	public synchronized void close() {
		if (outputStream != null && !outputStream.isClosed()) {
			try {
				outputStream.close();
			}
			catch (IOException e) {
				CloudFoundryPlugin.logError("Failed to close console output stream due to: " + e.getMessage(), e); //$NON-NLS-1$
			}
		}
		this.console = null;
	}

	public synchronized void initialiseStream(ConsoleConfig descriptor) throws CoreException {
		this.console = descriptor.getMessageConsole();
		this.outputStream = console.newOutputStream();
		if (isActive()) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					doInitialiseStream(outputStream);
				}
			});
		}
	}

	protected void doInitialiseStream(IOConsoleOutputStream outputStream) {
		if (config != null) {
			outputStream.setColor(Display.getDefault().getSystemColor(config.getDisplayColour()));
		}
	}

}
