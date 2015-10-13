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

import java.util.List;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;

public interface IConsoleContents {

	/**
	 * Return a list of streams that provide content to the Cloud Foundry
	 * console. user.
	 * @param cloudServer
	 * @param appName
	 * @return
	 */
	public List<ICloudFoundryConsoleStream> getContents(CloudFoundryServer cloudServer, String appName,
			int instanceIndex);

}