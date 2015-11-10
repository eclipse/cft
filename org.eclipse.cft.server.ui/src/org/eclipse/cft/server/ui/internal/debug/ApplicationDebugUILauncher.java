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
package org.eclipse.cft.server.ui.internal.debug;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.debug.ApplicationDebugLauncher;
import org.eclipse.cft.server.core.internal.debug.CloudFoundryDebugProvider;
import org.eclipse.cft.server.core.internal.debug.CloudFoundryProperties;
import org.eclipse.cft.server.core.internal.debug.DebugProviderRegistry;
import org.eclipse.cft.server.ui.internal.CloudUiUtil;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.core.IModule;

public class ApplicationDebugUILauncher extends ApplicationDebugLauncher {

	public boolean shouldRestartApp(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {

		IModule[] mod = new IModule[] { appModule.getLocalModule() };

		final boolean shouldRestart[] = { true };

		if (!CloudFoundryProperties.isModuleStopped.testProperty(mod, cloudServer)) {

			// Ask if the module should be restarted in its current state.
			Display.getDefault().syncExec(new Runnable() {

				public void run() {
					Shell shell = CloudUiUtil.getShell();
					shouldRestart[0] = MessageDialog.openQuestion(shell, Messages.DebugUIProvider_DEBUG_TITLE,
							Messages.DebugUIProvider_DEBUG_APP_RESTART_MESSAGE);
				}

			});
		}

		return shouldRestart[0];
	}

	@Override
	public void launch(final CloudFoundryApplicationModule appModule, final CloudFoundryServer cloudServer,
			final int appInstance, final int remoteDebugPort) throws CoreException {
		final CloudFoundryDebugProvider provider = DebugProviderRegistry.getExistingProvider(appModule, cloudServer);

		Job job = new Job("Launching debug - " + appModule.getDeployedApplicationName()) { //$NON-NLS-1$

			protected IStatus run(IProgressMonitor monitor) {
				try {

					ILaunchConfiguration launchConfiguration = provider.getLaunchConfiguration(appModule, cloudServer,
							appInstance, remoteDebugPort, monitor);

					DebugUITools.launch(launchConfiguration, ILaunchManager.DEBUG_MODE);
					DebugUITools.setLaunchPerspective(launchConfiguration.getType(), ILaunchManager.DEBUG_MODE,
							IDebugUIConstants.ID_DEBUG_PERSPECTIVE);
					fireDebugChanged(cloudServer, appModule, Status.OK_STATUS);

				}
				catch (OperationCanceledException e) {
					// do nothing, debug should be cancelled without error
				}
				catch (CoreException ce) {
					CloudFoundryPlugin.getCallback().displayAndLogError(ce.getStatus());
				}
				return Status.OK_STATUS;
			}
		};

		job.setSystem(true);
		job.setPriority(Job.INTERACTIVE);
		job.schedule();

	}

}
