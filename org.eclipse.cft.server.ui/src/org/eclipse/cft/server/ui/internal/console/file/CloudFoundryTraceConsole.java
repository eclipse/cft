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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.log.LogContentType;
import org.eclipse.cft.server.core.internal.log.TraceType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

/**
 * Cloud Foundry console that displays HTTP trace messages. A different output
 * stream is created for each {@link ITraceType}, thus allowing different
 * formats for different trace messages.
 *
 */
public class CloudFoundryTraceConsole {
	public static final String CLOUD_FOUNDRY_TRACE_CONSOLE_NAME = "Cloud Foundry Trace"; //$NON-NLS-1$

	static final String TRACE_CONSOLE_ID = "org.eclipse.cft.server.trace"; //$NON-NLS-1$

	static final private TraceUIType[] TRACE_TYPES = new TraceUIType[] {
			new TraceUIType(TraceType.HTTP_OK, SWT.COLOR_BLUE), new TraceUIType(TraceType.HTTP_ERROR, SWT.COLOR_RED),
			new TraceUIType(TraceType.HTTP_GENERAL, SWT.COLOR_BLACK) };

	private final MessageConsole messageConsole;

	private Map<LogContentType, TraceConsoleStream> streams;

	public CloudFoundryTraceConsole(MessageConsole messageConsole) {
		this.messageConsole = messageConsole;
	}

	public MessageConsole getMessageConsole() {
		return messageConsole;
	}

	public synchronized void close() {
		if (streams != null) {
			for (Entry<LogContentType, TraceConsoleStream> entry : streams.entrySet()) {
				entry.getValue().close();
			}
			streams.clear();
		}
	}

	public synchronized void initialiseStreams() {

		if (streams != null && !streams.isEmpty()) {
			return;
		}

		streams = new HashMap<LogContentType, TraceConsoleStream>();

		for (TraceUIType type : TRACE_TYPES) {
			IOConsoleOutputStream outStream = getMessageConsole().newOutputStream();

			if (outStream != null) {
				TraceConsoleStream stream = new TraceConsoleStream(type);
				stream.initialiseStream(outStream);
				streams.put(type.getTraceType(), stream);
			}
		}

	}

	public synchronized void tail(String message, LogContentType type) {
		if (message == null || type == null) {
			return;
		}

		write(message, type);
	}

	protected synchronized void write(String message, LogContentType type) {
		TraceConsoleStream stream = streams != null ? streams.get(type) : null;
		if (stream != null) {
			stream.write(message);
		}
	}

	static class TraceConsoleStream extends BaseConsoleStream {

		private final TraceUIType type;

		public TraceConsoleStream(TraceUIType type) {
			this.type = type;
		}

		protected void doInitialiseStream(IOConsoleOutputStream outputStream) {
			outputStream.setColor(Display.getDefault().getSystemColor(type.getDisplayColour()));
		};

		public synchronized void write(String message) {
			if (message == null) {
				return;
			}
			IOConsoleOutputStream outStream = getActiveOutputStream();
			if (outStream != null) {
				try {
					outStream.write(message);
				}
				catch (IOException e) {
					CloudFoundryPlugin.logError(e);
					close();
				}
			}
		}

	}

	static class TraceUIType {

		private final int swtColour;

		private final LogContentType type;

		private TraceUIType(LogContentType type, int swtColour) {
			this.swtColour = swtColour;
			this.type = type;
		}

		public int getDisplayColour() {
			return swtColour;
		}

		public LogContentType getTraceType() {
			return type;
		}
	}
}
