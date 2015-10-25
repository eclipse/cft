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
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal;

import org.eclipse.cft.server.core.internal.ApplicationAction;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.debug.AbstractDebugSshProvider;
import org.eclipse.cft.server.core.internal.debug.CloudFoundryProperties;
import org.eclipse.cft.server.core.internal.debug.DebugConnectionDescriptor;
import org.eclipse.cft.server.core.internal.debug.IDebugProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;

/**
 * Wrapper around a {@link IDebugProvider} that performs UI-aware provider
 * operations, for example prompting the user for additional information via
 * dialogues.
 */
public class DebugUISshProvider extends AbstractDebugSshProvider {

	private final IDebugProvider provider;

	public DebugUISshProvider(IDebugProvider provider) {
		this.provider = provider;
	}

	@Override
	public DebugConnectionDescriptor getDebugConnectionDescriptor(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, int debugPort, int instance, IProgressMonitor monitor)
					throws CoreException {
		return provider.getDebugConnectionDescriptor(appModule, cloudServer, debugPort, instance, monitor);
	}

	@Override
	public boolean canLaunch(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			IProgressMonitor monitor) throws CoreException {
		return provider.canLaunch(appModule, cloudServer, monitor);
	}

	@Override
	public boolean isDebugSupported(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		return provider.isDebugSupported(appModule, cloudServer);
	}

	@Override
	public String getLaunchConfigurationID() {
		return provider.getLaunchConfigurationID();
	}

	@Override
	public boolean configureApp(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer, int debugPort,
			IProgressMonitor monitor) throws CoreException {
		IModule[] mod = new IModule[] { appModule.getLocalModule() };

		boolean shouldRestart = CloudFoundryProperties.isModuleStopped.testProperty(mod, cloudServer);

		shouldRestart |= provider.configureApp(appModule, cloudServer, debugPort, monitor);

		if (shouldRestart) {
			// Perform a full push and start
			printToConsole(appModule, cloudServer,
					"Restarting application in debug mode - " + appModule.getDeployedApplicationName(), false); //$NON-NLS-1$
			cloudServer.getBehaviour().operations().applicationDeployment(mod, ApplicationAction.START, false)
					.run(monitor);
		}
		else {
			printToConsole(appModule, cloudServer,
					"Application already running in debug mode - " + appModule.getDeployedApplicationName(), false); //$NON-NLS-1$

		}

		printToConsole(appModule, cloudServer, "Connecting debugger to - " + appModule.getDeployedApplicationName(), //$NON-NLS-1$
				false);

		return true;

	}

}