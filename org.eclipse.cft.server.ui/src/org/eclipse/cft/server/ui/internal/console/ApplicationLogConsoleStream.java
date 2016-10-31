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
package org.eclipse.cft.server.ui.internal.console;

import static org.eclipse.cft.server.core.internal.log.LogContentType.APPLICATION_LOG_STD_OUT;
import static org.eclipse.cft.server.core.internal.log.LogContentType.APPLICATION_LOG_STS_ERROR;
import static org.eclipse.cft.server.core.internal.log.LogContentType.APPLICATION_LOG_UNKNOWN;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.core.internal.log.CFApplicationLogListener;
import org.eclipse.cft.server.core.internal.log.CFStreamingLogToken;
import org.eclipse.cft.server.core.internal.log.CloudLog;
import org.eclipse.cft.server.core.internal.log.LogContentType;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * Unlike {@link SingleConsoleStream}, that is associated with only one stream
 * content type, the application log console stream is actually a collection of
 * separate streams, one for each type of CF log content type (e.g. STDOUT,
 * STDERROR,..) , all which are received through the same CF log listener and
 * managed by one single CF log token. This is why there aren't separate
 * {@link ConsoleStream} for each CF log content type, as all CF log content are
 * received through the same callback registered in the
 * {@link CloudFoundryServerBehaviour}
 * 
 * 
 * <p/>
 * Closing the manager closes all active streams, as well as cancels any further
 * loggregator callbacks.
 * 
 *
 */
public class ApplicationLogConsoleStream extends ConsoleStream {

	private CFStreamingLogToken streamingToken;

	private Map<LogContentType, ConsoleStream> logStreams = new HashMap<LogContentType, ConsoleStream>();

	private ConsoleConfig consoleDescriptor;

	public ApplicationLogConsoleStream() {

	}

	public synchronized void close() {
		if (logStreams != null) {
			for (Entry<LogContentType, ConsoleStream> entry : logStreams.entrySet()) {
				entry.getValue().close();
			}
			logStreams.clear();
		}
		if (streamingToken != null) {
			streamingToken.cancel();
			streamingToken = null;
		}
	}

	public synchronized void initialiseStream(ConsoleConfig descriptor) throws CoreException {

		if (descriptor == null) {
			throw CloudErrorUtil.toCoreException(Messages.ERROR_FAILED_INITIALISE_APPLICATION_LOG_STREAM);
		}
		this.consoleDescriptor = descriptor;

		if (streamingToken == null) {

			CloudFoundryServerBehaviour behaviour = consoleDescriptor.getCloudServer().getBehaviour();

			streamingToken = behaviour.startAppLogStreaming(
					consoleDescriptor.getCloudApplicationModule().getDeployedApplicationName(),
					new ApplicationLogConsoleListener(), new NullProgressMonitor());

		}
	}

	@Override
	public synchronized boolean isActive() {
		return streamingToken != null;
	}

	@Override
	protected IOConsoleOutputStream getOutputStream(LogContentType type) {

		ConsoleStream consoleStream = getApplicationLogStream(type);
		if (consoleStream != null && consoleStream.isActive()) {
			return consoleStream.getOutputStream(type);
		}
		return null;
	}

	/**
	 * Gets the stream associated with the given cloud log type
	 * @param log
	 * @return stream associated with the given cloud log type, or null if log
	 * type is not supported
	 */
	protected synchronized ConsoleStream getApplicationLogStream(LogContentType type) {

		if (type == null) {
			return null;
		}

		ConsoleStream stream = logStreams.get(type);
		if (stream == null) {

			int swtColour = -1;

			if (APPLICATION_LOG_STS_ERROR.equals(type)) {
				swtColour = SWT.COLOR_RED;
			}
			else if (APPLICATION_LOG_STD_OUT.equals(type)) {
				swtColour = SWT.COLOR_DARK_GREEN;
			}
			else if (APPLICATION_LOG_UNKNOWN.equals(type)) {
				swtColour = SWT.COLOR_BLACK;
			}

			if (swtColour > -1) {

				try {
					stream = new SingleConsoleStream(new UILogConfig(swtColour));
					stream.initialiseStream(consoleDescriptor);
					logStreams.put(type, stream);
				}
				catch (CoreException e) {
					CloudFoundryPlugin.logError(e);
				}
			}
		}
		return stream;
	}

	/**
	 * Listener that receives loggregator content and sends it to the
	 * appropriate stream.
	 *
	 */
	public class ApplicationLogConsoleListener implements CFApplicationLogListener {

		public void onMessage(CloudLog appLog) {
			if (isActive()) {
				try {
					write(appLog);
				}
				catch (CoreException e) {
					onError(e);
				}
			}
		}

		public void onComplete() {
			// Nothing for now
		}

		public void onError(Throwable exception) {
			// Only log errors if the stream manager is active. This prevents
			// errors
			// to be continued to be displayed by the asynchronous loggregator
			// callback after the stream
			// manager has closed.
			if (isActive()) {
				CloudFoundryPlugin.logError(NLS.bind(Messages.ERROR_APPLICATION_LOG,
						consoleDescriptor.getCloudApplicationModule().getDeployedApplicationName(),
						exception.getMessage()), exception);
			}
		}
	}

	/**
	 * Writes a CF application log to the console. The content type of the
	 * application log is resolved first and a corresponding stream is fetched
	 * or created as part of streaming the log message to the console.
	 */
	public synchronized void write(CloudLog log) throws CoreException {
		if (log == null) {
			return;
		}

		IOConsoleOutputStream activeOutStream = getOutputStream(log.getLogType());

		if (activeOutStream != null && log.getMessage() != null) {
			try {
				activeOutStream.write(log.getMessage());
			}
			catch (IOException e) {
				throw CloudErrorUtil.toCoreException(e);
			}
		}
	}

}
