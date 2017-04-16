/*******************************************************************************
 * Copyright (c) 2016, 2017 Pivotal Software, Inc. and others 
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

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;

/**
 * Updates a module in the IServer, whether the module is deployed or not, and
 * notifies when the operation is completed.
 *
 */
@SuppressWarnings({"restriction"})
public class UpdateModuleOperation extends ModulesOperation {

	public UpdateModuleOperation(CloudFoundryServerBehaviour behaviour, IModule module) {
		super(behaviour, module);
	}

	@Override
	public String getOperationName() {
		CloudFoundryApplicationModule appModule = getCloudModule(getFirstModule());
		String name = appModule != null ? appModule.getDeployedApplicationName() : getFirstModule().getName();
		return NLS.bind(Messages.UpdateModuleOperation_OPERATION_MESSAGE, name);
	}

	@Override
	public void runOnVerifiedModule(IProgressMonitor monitor) throws CoreException {

		if (shouldUpdateInServer()) {
			updateModule(monitor);
		}

		// Fire the event even if updates do not occur in server, as to
		// notify interested parties (e.g UI) that an update
		// module operation was run anyway
		ServerEventHandler.getDefault().fireModuleUpdated(getBehaviour().getCloudFoundryServer(), getFirstModule());
	}

	protected CloudFoundryApplicationModule updateModule(IProgressMonitor monitor) throws CoreException {
		return getBehaviour().updateModuleWithAllCloudInfo(getFirstModule(), monitor);

	}

	protected boolean shouldUpdateInServer() {
		try {
			if (getBehaviour().getCloudFoundryServer().getServer() instanceof Server) {
				Server server = (Server) getBehaviour().getCloudFoundryServer().getServer();
				return IServer.STATE_STARTING != server.getModuleState(new IModule[] { getFirstModule() });
			}
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return true;
	}
}
