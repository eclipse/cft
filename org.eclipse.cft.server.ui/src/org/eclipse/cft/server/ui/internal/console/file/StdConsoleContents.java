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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.swt.SWT;

public class StdConsoleContents implements IConsoleContents {

	public static final String STD_OUT_LOG = "logs/stdout.log"; //$NON-NLS-1$

	public static final String STD_ERROR_LOG = "logs/stderr.log"; //$NON-NLS-1$

	public List<ICloudFoundryConsoleStream> getContents(CloudFoundryServer cloudServer, CloudFoundryApplicationModule app,
			int instanceIndex) {
		String appName = app.getDeployedApplicationName();
		return getContents(cloudServer, appName, instanceIndex);
	}

	public List<ICloudFoundryConsoleStream> getContents(CloudFoundryServer cloudServer, String appName, int instanceIndex) {
		List<ICloudFoundryConsoleStream> contents = new ArrayList<ICloudFoundryConsoleStream>();
		contents.add(new StdLogFileConsoleStream(STD_ERROR_LOG, SWT.COLOR_RED, cloudServer, appName, instanceIndex));
		contents.add(new StdLogFileConsoleStream(STD_OUT_LOG, -1, cloudServer, appName, instanceIndex));
		return contents;
	}

}
