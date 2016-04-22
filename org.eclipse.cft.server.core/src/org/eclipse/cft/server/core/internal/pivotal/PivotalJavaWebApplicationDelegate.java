/*******************************************************************************
 * Copyright (c) 2015, 2016 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.core.internal.pivotal;

import java.util.Arrays;

import org.cloudfoundry.client.lib.domain.Staging;
import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudApplicationURL;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.application.ICloudFoundryServerApplicationDelegate;
import org.eclipse.cft.server.core.internal.application.JavaWebApplicationDelegate;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;

/**
 * Java Web applications are the standard type of applications supported on
 * Cloud Foundry. They include Spring, Grails, Lift and Java Web.
 * <p/>
 * This application delegate supports the above Java Web frameworks.
 */
public class PivotalJavaWebApplicationDelegate extends JavaWebApplicationDelegate
		implements ICloudFoundryServerApplicationDelegate {

	public PivotalJavaWebApplicationDelegate() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cft.server.core.internal.application.
	 * JavaWebApplicationDelegate#getDefaultApplicationDeploymentInfo(org.
	 * eclipse.wst.server.core.IModule,
	 * org.eclipse.cft.server.core.internal.CloudFoundryServer,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public ApplicationDeploymentInfo getDefaultApplicationDeploymentInfo(IModule module, CloudFoundryServer cloudServer,
			IProgressMonitor monitor) throws CoreException {
		ApplicationDeploymentInfo info = super.getDefaultApplicationDeploymentInfo(module, cloudServer, monitor);

		info.setStaging(new Staging(null, PivotalConstants.PIVOTAL_WEB_SERVICES_JAVA_BUILDPACK));
		info.setMemory(PivotalConstants.PIVOTAL_DEFAULT_MEMORY);
		// Set a default URL for the application.
		if ((info.getUris() == null || info.getUris().isEmpty()) && info.getDeploymentName() != null) {

			CloudApplicationURL url = ApplicationUrlLookupService.update(cloudServer, monitor)
					.getDefaultApplicationURL(info.getDeploymentName());
			info.setUris(Arrays.asList(url.getUrl()));
		}
		return info;
	}

	@Override
	public String getServerUri() {
		return PivotalConstants.PIVOTAL_WEB_SERVICES_URI;
	}
}
