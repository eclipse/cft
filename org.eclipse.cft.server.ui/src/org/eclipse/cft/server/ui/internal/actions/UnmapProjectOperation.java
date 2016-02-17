/*******************************************************************************
 * Copyright (c) 2015, 2016 Pivotal Software, Inc. 
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
import org.eclipse.cft.server.core.internal.ModuleCache;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.cft.server.core.internal.ModuleCache.ServerData;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerUtil;

public class UnmapProjectOperation implements ICloudFoundryOperation {

	private final CloudFoundryApplicationModule appModule;

	private final CloudFoundryServer cloudServer;

	public UnmapProjectOperation(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		this.appModule = appModule;
		this.cloudServer = cloudServer;
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {
		IServer server = cloudServer.getServer();

		IServerWorkingCopy wc = server.createWorkingCopy();

		ModuleCache moduleCache = CloudFoundryPlugin.getModuleCache();
		ServerData data = moduleCache.getData(cloudServer.getServerOriginal());
		data.tagForReplace(appModule);

		ServerUtil.modifyModules(wc, new IModule[0], new IModule[] { appModule.getLocalModule() }, monitor);
		wc.save(true, monitor);

		CloudFoundryApplicationModule updatedModule = cloudServer
				.getExistingCloudModule(appModule.getDeployedApplicationName());

		if (updatedModule != null) {
			// Do a complete update of the module with Cloud information since
			// the link/unlink project may re-create
			// the module
			cloudServer.getBehaviour().operations().updateModule(updatedModule.getLocalModule()).run(monitor);
		}

		ServerEventHandler.getDefault().fireServerRefreshed(cloudServer);
	}

}