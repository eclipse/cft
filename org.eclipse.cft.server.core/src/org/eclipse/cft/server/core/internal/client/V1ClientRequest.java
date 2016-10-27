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

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

abstract public class V1ClientRequest<T> extends ClientRequest<T> {

	public V1ClientRequest(CloudFoundryServer cloudServer, CloudServerCFClient client, String label) {
		super(cloudServer, client, label);
	}

	protected T runRequest(CloudServerCFClient client, IProgressMonitor monitor) throws CoreException {
		CloudFoundryOperations v1Client = client.getV1Operations();
		if (v1Client != null) {
			return runV1Request(v1Client, monitor);
		}
		else {
			throw CloudErrorUtil.toCoreException(
					"Internal Framework Error: Cloud Foundry Java client version v1 encountered, but could not resolve the underlying v1 operations.");
		}
	}

	protected abstract T runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException;

}