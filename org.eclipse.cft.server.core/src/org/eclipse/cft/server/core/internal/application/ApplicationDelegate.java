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
 *     IBM Corporation - combine IApplicationDelegate and ApplicationDelegate
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.application;

import org.eclipse.cft.server.core.AbstractApplicationDelegate;
import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.CloudUtil;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
	

	protected CloudFoundryApplicationModule getCloudFoundryApplicationModule(IModule module, IServer server)
			throws CoreException {
		return CloudServerUtil.getExistingCloudModule(module, server);
	}

	@Override
	public IStatus validateDeploymentInfo(ApplicationDeploymentInfo deploymentInfo) {
		return CloudUtil.basicValidateDeploymentInfo(deploymentInfo);
	}

	@Override
	public ApplicationDeploymentInfo getExistingApplicationDeploymentInfo(IModule module, IServer server)
			throws CoreException {
		return CloudUtil
				.parseApplicationDeploymentInfo(getCloudFoundryApplicationModule(module, server).getApplication());
	}
	
	/**
	 * NOTE: For INTERNAL use only. By default this is true. Note that this is
	 * distinct from {@link #requiresURL()}. Suggest URL determines if the
	 * framework should generate a suggested URL for the application when
	 * deploying the application for the first time, but does not enforce it's
	 * requirement.
	 * @param appModule
	 * @return true if default URL should be set. False otherwise
	 */
	public boolean suggestUrl(CloudFoundryApplicationModule appModule) {
		return true;
	}


	protected CloudFoundryServer getCloudServer(IServer server) throws CoreException {
		return CloudServerUtil.getCloudServer(server);
	}
}
