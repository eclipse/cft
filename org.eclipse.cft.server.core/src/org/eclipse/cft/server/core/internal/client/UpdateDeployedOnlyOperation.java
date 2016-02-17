/*******************************************************************************
 * Copyright (c) 2016 Pivotal Software, Inc. 
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
 * Updates a module only if it is deployed (it exists in the Cloud target and
 * publish state is known in the IServer) and notifies when the operation is
 * completed.
 *
 */
public class UpdateDeployedOnlyOperation extends UpdateModuleOperation {

	public UpdateDeployedOnlyOperation(CloudFoundryServerBehaviour behaviour, IModule module) {
		super(behaviour, module);
	}

	protected CloudFoundryApplicationModule updateModule(IProgressMonitor monitor) throws CoreException {
		return getBehaviour().updateDeployedModule(getModule(), monitor);
	}

}
