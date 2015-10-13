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
import org.eclipse.cft.server.core.internal.debug.CloudFoundryProperties;
import org.eclipse.cft.server.core.internal.debug.DebugConnectionDescriptor;
import org.eclipse.cft.server.core.internal.debug.IDebugProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.core.IModule;

/**
 * Wrapper around a {@link IDebugProvider} that performs UI-aware provider
 * operations, for example prompting the user for additional information via
 * dialogues.
 */
class DebugUIProvider implements IDebugProvider {

	private final IDebugProvider provider;

	public DebugUIProvider(IDebugProvider provider) {
		this.provider = provider;
	}

	@Override
	public DebugConnectionDescriptor getDebugConnectionDescriptor(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, IProgressMonitor monitor) throws CoreException {
		return provider.getDebugConnectionDescriptor(appModule, cloudServer, monitor);
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
	public boolean configureApp(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			IProgressMonitor monitor) throws CoreException {
		IModule[] mod = new IModule[] { appModule.getLocalModule() };

		boolean shouldRestart = CloudFoundryProperties.isModuleStopped.testProperty(mod, cloudServer);

		// If the application cannot yet launch then prompt the user to restart
		// the app in order for
		// any changes that enables debug to be set in the Cloud container
		if (!provider.canLaunch(appModule, cloudServer, monitor)) {

			final boolean[] shouldProceed = new boolean[] { true };

			// Ask if the module should be restarted in its current state.
			Display.getDefault().syncExec(new Runnable() {

				public void run() {
					Shell shell = CloudUiUtil.getShell();
					shouldProceed[0] = MessageDialog.openQuestion(shell, Messages.DebugUIProvider_DEBUG_TITLE,
							Messages.DebugUIProvider_DEBUG_APP_RESTART_MESSAGE);
				}

			});

			shouldRestart = shouldProceed[0];
			if (!shouldProceed[0]) {
				return false;
			}
		}
		provider.configureApp(appModule, cloudServer, monitor);

		if (shouldRestart) {
			// Perform a full push and start
			cloudServer.getBehaviour().operations().applicationDeployment(mod, ApplicationAction.START).run(monitor);
		}
		return true;

	}
}