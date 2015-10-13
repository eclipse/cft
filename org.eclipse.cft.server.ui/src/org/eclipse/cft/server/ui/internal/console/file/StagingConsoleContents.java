/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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

public class StagingConsoleContents implements IConsoleContents {

	/**
	 * Return a list of File contents that should be shown to the user, like
	 * console logs. The list determines the order in which they appear to the
	 * user.
	 * @param cloudServer
	 * @param app
	 * @return
	 */
	public List<ICloudFoundryConsoleStream> getContents(final CloudFoundryServer cloudServer, String appName,
			final int instanceIndex) {

		List<ICloudFoundryConsoleStream> contents = new ArrayList<ICloudFoundryConsoleStream>();

		contents.add(new StagingFileConsoleStream(cloudServer, appName, instanceIndex));

		return contents;
	}

}
