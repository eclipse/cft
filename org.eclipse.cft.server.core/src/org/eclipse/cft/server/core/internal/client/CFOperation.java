/*******************************************************************************
 * Copyright (c) 2017 Pivotal Software, Inc. and others
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

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.NotFinishedStagingException;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;

public abstract class CFOperation implements ICloudFoundryOperation {

	private final CloudFoundryServerBehaviour behaviour;

	public CFOperation(CloudFoundryServerBehaviour behaviour) {
		this.behaviour = behaviour;
	}

	public abstract String getOperationName();

	public CloudFoundryServerBehaviour getBehaviour() {
		return behaviour;
	}

	/**
	 * 
	 * @param module
	 * @return Cloud module for the given IModule, or null if it does not exist
	 * (e.g. may not yet be published)
	 */
	protected CloudFoundryApplicationModule getCloudModule(IModule module) {
		try {
			return CloudServerUtil.getCloudModule(module, behaviour.getCloudFoundryServer().getServer());
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return null;
	}
	
	protected void logNonModuleError(CoreException e) {
		IStatus status = Status.OK_STATUS;
		CloudFoundryException cfe = e.getCause() instanceof CloudFoundryException ? (CloudFoundryException) e
				.getCause() : null;
		if (cfe instanceof NotFinishedStagingException) {
			status = CloudFoundryPlugin.getStatus(Messages.CFOperation_WARNING_RESTART_APP, IStatus.WARNING);
		}
		else if (shouldLogException(e)) {
			status = e.getStatus();
		}
//		else {
//			status = CloudFoundryPlugin.getStatus(e.getMessage(), IStatus.CANCEL);
//		}
		if (!status.isOK()) {
			CloudFoundryPlugin.log(status);
		}
	}
	
	protected boolean shouldLogException(CoreException e) {
		return true;
	}
}
