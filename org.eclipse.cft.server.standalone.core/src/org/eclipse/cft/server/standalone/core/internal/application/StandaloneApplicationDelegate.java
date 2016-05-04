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
 ********************************************************************************/
package org.eclipse.cft.server.standalone.core.internal.application;

import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.CFApplicationArchive;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryProjectUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.application.ModuleResourceApplicationDelegate;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * 
 * Determines if a give module is a Java standalone application. Also provides
 * an archiving mechanism that is specific to Java standalone applications.
 * 
 */
public class StandaloneApplicationDelegate extends ModuleResourceApplicationDelegate {

	public StandaloneApplicationDelegate() {

	}

	@Override
	public boolean shouldSetDefaultUrl(CloudFoundryApplicationModule appModule) {
		return CloudFoundryProjectUtil.isSpringBoot(appModule);
	}

	public boolean requiresURL() {
		// URLs are optional for Java standalone applications
		return false;
	}

	@Override
	public IStatus validateDeploymentInfo(ApplicationDeploymentInfo deploymentInfo) {

		IStatus status = super.validateDeploymentInfo(deploymentInfo);
		if (status.isOK() && ((deploymentInfo.getUris() == null || deploymentInfo.getUris().isEmpty()))) {
			String errorMessage = Messages.JavaWebApplicationDelegate_ERROR_NO_MAPPED_APP_URL;
			status = CloudFoundryPlugin.getStatus(errorMessage, IStatus.WARNING);
		}

		return status;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cft.server.core.internal.application.
	 * ModuleResourceApplicationDelegate#getApplicationArchive(org.eclipse.wst.
	 * server.core.IModule, org.eclipse.wst.server.core.IServer,
	 * org.eclipse.wst.server.core.model.IModuleResource[],
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	public CFApplicationArchive getApplicationArchive(IModule module, IServer server, IModuleResource[] moduleResources,
			IProgressMonitor monitor) throws CoreException {
		CloudFoundryApplicationModule appModule = getCloudFoundryApplicationModule(module, server);
		CloudFoundryServer cloudServer = getCloudServer(server);
		ICloudFoundryArchiver archiver = CloudFoundryArchiverRegistry.INSTANCE.createArchiver(appModule, cloudServer);
		return archiver.getApplicationArchive(monitor);
	}

}
