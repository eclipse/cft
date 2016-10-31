/*******************************************************************************
 * Copyright (c) 2016 Pivotal Software, Inc. and others 
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
package org.eclipse.cft.server.core.internal.client;

import org.eclipse.cft.server.core.internal.ProviderPriority;
import org.eclipse.cft.server.core.internal.spaces.CloudFoundrySpace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IServer;

public interface CFClientProvider {

	public ProviderPriority getPriority();

	public boolean supports(String serverUrl, CFInfo info);

	/**
	 * Creates a non-null {@link CFClient} which is associated with the given
	 * cloud server instance.
	 * @param cloudServer
	 * @param credentials
	 * @param cloudFoundrySpace
	 * @return non-null client
	 * @throws CoreException if client failed to be created
	 */
	public CFClient getClient(IServer cloudServer, CFCloudCredentials credentials, CloudFoundrySpace cloudFoundrySpace,
			IProgressMonitor monitor) throws CoreException;

}
