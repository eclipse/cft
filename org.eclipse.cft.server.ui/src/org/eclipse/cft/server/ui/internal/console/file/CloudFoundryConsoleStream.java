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

import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * Base console stream that writes to an Eclipse IO console stream. The console
 * stream may be accessed by different threads therefore write and stop
 * operations should be synchronized.
 *
 */
public abstract class CloudFoundryConsoleStream extends BaseConsoleStream implements ICloudFoundryConsoleStream {

	private final int swtConsoleColour;

	protected final String appName;

	protected final int instanceIndex;

	protected final CloudFoundryServer server;

	public CloudFoundryConsoleStream(CloudFoundryServer server, int swtColour, String appName, int instanceIndex) {
		this.server = server;
		this.appName = appName;
		this.instanceIndex = instanceIndex;
		this.swtConsoleColour = swtColour;
	}

	public CloudFoundryServer getServer() {
		return server;
	}

	public synchronized String write(IProgressMonitor monitor) throws CoreException {

		String content = getContent(monitor);

		return write(content);
	}

	public synchronized String write(String content) throws CoreException {
		IOConsoleOutputStream activeOutStream = getActiveOutputStream();

		if (activeOutStream != null && content != null && content.length() > 0) {
			try {
				activeOutStream.write(content);
				return content;
			}
			catch (IOException e) {
				throw CloudErrorUtil.toCoreException(e);
			}
		}

		return null;
	}

	protected void doInitialiseStream(IOConsoleOutputStream outputStream) {
		outputStream.setColor(Display.getDefault().getSystemColor(swtConsoleColour));
	}

	/*
	 * @Overrride
	 */
	public String toString() {
		return getContentType().toString();
	}

	abstract protected String getContent(IProgressMonitor monitor) throws CoreException;

}
