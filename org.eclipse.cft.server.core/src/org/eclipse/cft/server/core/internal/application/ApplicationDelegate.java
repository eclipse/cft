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
 *     IBM Corporation - combine IApplicationDelegate and ApplicationDelegate
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.application;

import org.eclipse.cft.server.core.AbstractApplicationDelegate;
import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.CloudUtil;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * An internal application delegate used by the framework that handles
 * {@link CloudFoundryApplicationModule}
 *
 */
public abstract class ApplicationDelegate extends AbstractApplicationDelegate {

	@Override
	public ApplicationDeploymentInfo getDefaultApplicationDeploymentInfo(IModule module, IServer server,
			IProgressMonitor monitor) throws CoreException {
		// Set default values.
		String appName = getCloudFoundryApplicationModule(module, server).getDeployedApplicationName();
		ApplicationDeploymentInfo deploymentInfo = new ApplicationDeploymentInfo(appName);
		deploymentInfo.setMemory(CloudUtil.DEFAULT_MEMORY);

		return deploymentInfo;
	}

	/**
	 * 
	 * @param module
	 * @param cloudServer
	 * @return non-null {@link CloudFoundryApplicationModule}
	 * @throws CoreException if application module could not be found (e.g. app
	 * does not exist or server is out of synch)
	 */
	protected CloudFoundryApplicationModule getCloudFoundryApplicationModule(IModule module,
			IServer server) throws CoreException {
		CloudFoundryServer cloudServer = getCloudServer(server);
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);
		if (appModule == null) {
			throw CloudErrorUtil.toCoreException(NLS.bind(Messages.ApplicationDelegate_NO_CLOUD_MODULE_FOUND,
					module.getName(), cloudServer.getServer().getId()));
		}
		return appModule;
	}

	@Override
	public IStatus validateDeploymentInfo(ApplicationDeploymentInfo deploymentInfo) {
		return CloudUtil.basicValidateDeploymentInfo(deploymentInfo);
	}

	@Override
	public ApplicationDeploymentInfo getExistingApplicationDeploymentInfo(IModule module,
			IServer server) throws CoreException {
		return CloudUtil
				.parseApplicationDeploymentInfo(
						getCloudFoundryApplicationModule(module, server).getApplication());
	}
	
	protected CloudFoundryServer getCloudServer(IServer server) throws CoreException {
		return CloudServerUtil.getCloudServer(server);
	}
}
