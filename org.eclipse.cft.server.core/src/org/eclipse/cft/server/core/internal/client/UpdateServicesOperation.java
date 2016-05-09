/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
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

import java.util.List;

import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class UpdateServicesOperation extends BehaviourOperation {

	private final BaseClientRequest<List<CFServiceInstance>> request;

	public UpdateServicesOperation(BaseClientRequest<List<CFServiceInstance>> request, CloudFoundryServerBehaviour behaviour) {
		super(behaviour, null);
		this.request = request;
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {
		List<CFServiceInstance> existingServices = request.run(monitor);
		ServerEventHandler.getDefault().fireServicesUpdated(getBehaviour().getCloudFoundryServer(),
				existingServices);
	}

}
