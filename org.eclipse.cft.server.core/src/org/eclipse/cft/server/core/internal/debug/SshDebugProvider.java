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
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.debug;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryProjectUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * Performs a connection to a given server and module. Handles network timeouts,
 * including retrying if connections failed.
 */
public class SshDebugProvider extends CloudFoundryDebugProvider {

	@Override
	public boolean isDebugSupported(IModule module, IServer server) {
		try {
			CloudFoundryServer cloudServer = getCloudServer(server);
			CloudFoundryApplicationModule appModule = getCloudFoundryApplicationModule(module, cloudServer);
			IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(appModule);
			return javaProject != null && javaProject.exists() && cloudServer.getBehaviour().supportsSsh();
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logWarning(e.getMessage());
		}
		return false;
	}

	@Override
	public String getLaunchConfigurationType(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		return SshDebugLaunchConfigDelegate.LAUNCH_CONFIGURATION_ID;
	}

}
