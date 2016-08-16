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

import org.eclipse.cft.server.core.AbstractDebugProvider;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

public abstract class CloudFoundryDebugProvider extends AbstractDebugProvider {
	
	public abstract String getLaunchConfigurationType(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer);

	public String getLaunchLabel(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			int appInstance) {
		StringBuilder idBuffer = new StringBuilder();

		idBuffer.append(cloudServer.getUrl());
		idBuffer.append('-');
		idBuffer.append(cloudServer.getCloudFoundrySpace().getOrgName());
		idBuffer.append('-');
		idBuffer.append(cloudServer.getCloudFoundrySpace().getSpaceName());
		idBuffer.append('-');
		idBuffer.append(appModule.getDeployedApplicationName());
		idBuffer.append('-');
		idBuffer.append(appInstance);
		return idBuffer.toString();
	}

	public String getApplicationDebugLaunchId(IModule module, IServer server,
			int appInstance) throws CoreException {
		StringBuilder idBuffer = new StringBuilder();
		
		CloudFoundryServer cloudServer = getCloudServer(server);
		CloudFoundryApplicationModule appModule = getCloudFoundryApplicationModule(module, cloudServer);

		idBuffer.append(cloudServer.getUrl());
		idBuffer.append('-');
		idBuffer.append(cloudServer.getUsername());
		idBuffer.append('-');
		idBuffer.append(cloudServer.getCloudFoundrySpace().getOrgName());
		idBuffer.append('-');
		idBuffer.append(cloudServer.getCloudFoundrySpace().getSpaceName());
		idBuffer.append('-');
		idBuffer.append(appModule.getDeployedApplicationName());
		idBuffer.append('-');
		idBuffer.append(appInstance);
		return idBuffer.toString();
	}

	/**
	 * 
	 * @param host
	 * @param port
	 * @param timeout
	 * @param appName
	 * @param launchName
	 * @return non-null launch configuration to debug the given application
	 * name.
	 * @throws CoreException if unable to resolve launch configuration.
	 */
	public ILaunchConfiguration getLaunchConfiguration(IModule module,
			IServer server, int appInstance, int remoteDebugPort, IProgressMonitor monitor)
					throws CoreException {
		
		CloudFoundryServer cloudServer = getCloudServer(server);
		CloudFoundryApplicationModule appModule = getCloudFoundryApplicationModule(module, cloudServer);

		String launchLabel = getLaunchLabel(appModule, cloudServer, appInstance);
		launchLabel = launchLabel.replaceAll("@", "-");

		String launchType = getLaunchConfigurationType(appModule, cloudServer);

		ILaunchConfigurationType launchConfigType = DebugPlugin.getDefault().getLaunchManager()
				.getLaunchConfigurationType(launchType);

		if (launchConfigType != null) {

			IProject project = appModule.getLocalModule().getProject();

			// Create the launch configuration, whether the project exists
			// or not, as there may
			// not be a local project associated with the deployed app
			ILaunchConfiguration launchConfiguration = launchConfigType.newInstance(project, launchLabel);
			ILaunchConfigurationWorkingCopy wc = launchConfiguration.getWorkingCopy();

			if (project != null && project.isAccessible()) {
				wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
			}

			wc.setAttribute(CloudFoundryDebugDelegate.CLOUD_DEBUG_SERVER, cloudServer.getServerId());
			wc.setAttribute(CloudFoundryDebugDelegate.CLOUD_DEBUG_APP_NAME, appModule.getDeployedApplicationName());
			wc.setAttribute(CloudFoundryDebugDelegate.CLOUD_DEBUG_APP_INSTANCE, appInstance);
			wc.setAttribute(CloudFoundryDebugDelegate.CLOUD_DEBUG_REMOTE_DEBUG_PORT, remoteDebugPort);
			wc.setAttribute(CloudFoundryDebugDelegate.CLOUD_DEBUG_APP_LAUNCH_ID,
					getApplicationDebugLaunchId(module, server, appInstance));

			return launchConfiguration = wc.doSave();

		}
		else {
			throw CloudErrorUtil.toCoreException("No debug launch configuration found for - " + launchType); //$NON-NLS-1$
		}

	}
	
	/**
	 * Get the CloudFoundryServer for the given IServer
	 * @param server The server
	 * @return The CloudFoundryServer
	 * @throws CoreException
	 */
	protected CloudFoundryServer getCloudServer(IServer server) throws CoreException {
		return CloudServerUtil.getCloudServer(server);
	}
	
	/**
	 * Get the CloudFoundryApplicationModule for the given IModule
	 * @param module The module
	 * @param cloudServer The Cloud Foundry server
	 * @return non-null {@link CloudFoundryApplicationModule}
	 * @throws CoreException if application module could not be found (e.g. app
	 * does not exist or server is out of synch)
	 */
	protected CloudFoundryApplicationModule getCloudFoundryApplicationModule(IModule module,
			CloudFoundryServer cloudServer) throws CoreException {
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);
		if (appModule == null) {
			throw CloudErrorUtil.toCoreException(NLS.bind(Messages.CloudFoundryDebugProvider_NO_CLOUD_MODULE_FOUND,
					module.getName(), cloudServer.getServer().getId()));
		}
		return appModule;
	}

}