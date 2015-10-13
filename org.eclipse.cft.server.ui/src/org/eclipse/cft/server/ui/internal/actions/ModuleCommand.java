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
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.actions;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public abstract class ModuleCommand extends BaseCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		initializeSelection(event);
		final CloudFoundryServer cloudServer = selectedServer != null ? (CloudFoundryServer) selectedServer
				.loadAdapter(CloudFoundryServer.class, null) : null;
		CloudFoundryApplicationModule appModule = cloudServer != null && selectedModule != null ? cloudServer
				.getExistingCloudModule(selectedModule) : null;
		if (selectedServer == null) {
			CloudFoundryPlugin.logError("No Cloud Foundry server instance available to run the selected action."); //$NON-NLS-1$
		}
		else if (appModule == null) {
			CloudFoundryPlugin.logError("No Cloud module resolved for the given selection."); //$NON-NLS-1$
		}
		else {
			run(appModule, cloudServer);
		}
		return null;
	}

	abstract protected void run(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer);

}
