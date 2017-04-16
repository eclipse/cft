/*******************************************************************************
 * Copyright (c) 2015, 2017 Pivotal Software, Inc. and others
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;

/**
 * Performs a {@link BaseClientRequest} that updates an existing published
 * application. After the request is performed it will fire an event indicating
 * that the published application has been updated (e.g. memory scaled, mapped
 * URL changed, etc.)
 *
 */
public class ApplicationUpdateOperation extends ModulesOperation {

	private final BaseClientRequest<?> request;

	public ApplicationUpdateOperation(BaseClientRequest<?> request, CloudFoundryServerBehaviour behaviour, IModule module) {
		super(behaviour, module);
		this.request = request;
	}

	@Override
	public void runOnVerifiedModule(IProgressMonitor monitor) throws CoreException {
		request.run(monitor);
		getBehaviour().asyncUpdateDeployedModule(getFirstModule());
	}

	@Override
	public String getOperationName() {
		return this.request.getRequestLabel();
	}
}
