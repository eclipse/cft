/*******************************************************************************
 * Copyright (c) 2012, 2016 Pivotal Software, Inc. and others
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
 *     IBM - Turning into base WAR packaging provider.
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.application;

import java.util.Arrays;

import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.CFApplicationArchive;
import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudApplicationURL;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * Java Web applications are the standard type of applications supported on
 * Cloud Foundry. They include Spring, Lift and Java Web.
 * <p/>
 * This application delegate supports the above Java Web frameworks.
 */
public class JavaWebApplicationDelegate extends ApplicationDelegate {

	public JavaWebApplicationDelegate() {

	}

	public boolean requiresURL() {
		// All Java Web applications require a URL when pushed to a CF server
		return true;
	}

	public boolean providesApplicationArchive(IModule module) {
		// Returns a default WAR archive package
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cft.server.core.AbstractApplicationDelegate#
	 * getApplicationArchive(org.eclipse.wst.server.core.IModule,
	 * org.eclipse.wst.server.core.IServer,
	 * org.eclipse.wst.server.core.model.IModuleResource[],
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public CFApplicationArchive getApplicationArchive(IModule module, IServer server, IModuleResource[] moduleResources,
			IProgressMonitor monitor) throws CoreException {
		return ApplicationRegistry.getArchiverFactory().getWarApplicationArchiver().getApplicationArchive(module,
				server, moduleResources, monitor);
	}

	@Override
	public IStatus validateDeploymentInfo(ApplicationDeploymentInfo deploymentInfo) {

		IStatus status = super.validateDeploymentInfo(deploymentInfo);
		if (status.isOK() && ((deploymentInfo.getUris() == null || deploymentInfo.getUris().isEmpty()))) {
			String errorMessage = Messages.JavaWebApplicationDelegate_ERROR_NO_MAPPED_APP_URL;
			status = CloudFoundryPlugin.getErrorStatus(errorMessage);
		}

		return status;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.cft.server.core.internal.application.ApplicationDelegate#
	 * getDefaultApplicationDeploymentInfo(org.eclipse.wst.server.core.IModule,
	 * org.eclipse.cft.server.core.internal.CloudFoundryServer,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public ApplicationDeploymentInfo getDefaultApplicationDeploymentInfo(IModule module, IServer server,
			IProgressMonitor monitor) throws CoreException {
		ApplicationDeploymentInfo info = super.getDefaultApplicationDeploymentInfo(module, server, monitor);

		// Set a default URL for the application.
		if ((info.getUris() == null || info.getUris().isEmpty()) && info.getDeploymentName() != null) {

			CloudApplicationURL url = ApplicationUrlLookupService.update(getCloudServer(server), monitor)
					.getDefaultApplicationURL(info.getDeploymentName());
			info.setUris(Arrays.asList(url.getUrl()));
		}
		return info;
	}

}
