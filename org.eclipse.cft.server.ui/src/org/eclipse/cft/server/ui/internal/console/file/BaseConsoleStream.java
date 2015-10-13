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
package org.eclipse.cft.server.ui.internal.console.file;

import java.io.IOException;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * Basic console stream that manages an output stream to an Eclipse console,
 * including initialising the stream, as well as closing streams.
 */
public abstract class BaseConsoleStream {

	private IOConsoleOutputStream outputStream;

	public synchronized void initialiseStream(IOConsoleOutputStream outputStream) {
		this.outputStream = outputStream;
		if (this.outputStream != null && !this.outputStream.isClosed()) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					doInitialiseStream(BaseConsoleStream.this.outputStream);
				}
			});
		}
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
	}

	public synchronized boolean isActive() {
		return outputStream != null && !outputStream.isClosed();
	}

	/**
	 * Returns the output stream IFF it is active. Conditions for determining if
	 * a stream is active is done through {@link #isActive()}
	 * @return Returns the output stream IFF it is active. Returns null
	 * otherwise.
	 */
	protected synchronized IOConsoleOutputStream getActiveOutputStream() {
		if (isActive()) {
			return outputStream;
		}
		return null;
	}

	protected void doInitialiseStream(IOConsoleOutputStream outputStream) {
		// Subclasses can override if necessary.
	};
}
