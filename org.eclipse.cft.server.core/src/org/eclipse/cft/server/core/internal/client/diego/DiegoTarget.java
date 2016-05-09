/*******************************************************************************
 * Copyright (c) 2015, 2016 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.core.internal.client.diego;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudFoundryServerTarget;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.client.ClientRequestFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IServer;

public class DiegoTarget extends CloudFoundryServerTarget {

	@Override
	public ClientRequestFactory createRequestFactory(IServer server) throws CoreException {
		CloudFoundryServer cloudServer = CloudServerUtil.getCloudServer(server);
		return new DiegoRequestFactory(cloudServer.getBehaviour());
	}

	@Override
	public boolean supports(IServer server) throws CoreException {
		// To check if Server is "Diego", check if the server supports
		// SSH. If so, assume it is a Diego target
		// as SSH is only available in Diego or more recent Cloud Foundry
		return createRequestFactory(server).supportsSsh();
	}

}
