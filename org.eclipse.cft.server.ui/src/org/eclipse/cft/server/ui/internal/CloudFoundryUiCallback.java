/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. and others
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

import java.util.List;

import org.eclipse.cft.server.core.internal.CloudFoundryCallback;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.cft.server.core.internal.application.ModuleChangeEvent;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.DeploymentConfiguration;
import org.eclipse.cft.server.core.internal.debug.ApplicationDebugLauncher;
import org.eclipse.cft.server.core.internal.jrebel.CFRebelServerIntegration;
import org.eclipse.cft.server.core.internal.log.CloudLog;
import org.eclipse.cft.server.ui.internal.console.ConsoleManagerRegistry;
import org.eclipse.cft.server.ui.internal.console.StandardLogContentType;
import org.eclipse.cft.server.ui.internal.debug.ApplicationDebugUILauncher;
import org.eclipse.cft.server.ui.internal.wizards.CloudFoundryCredentialsWizard;
import org.eclipse.cft.server.ui.internal.wizards.DeleteServicesWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Terry Denney
 */
public class CloudFoundryUiCallback extends CloudFoundryCallback {

	@Override
	public void applicationStarted(final CloudFoundryServer server, final CloudFoundryApplicationModule cloudModule) {
		ServerEventHandler.getDefault().fireServerEvent(new ModuleChangeEvent(server,
				CloudServerEvent.EVENT_APP_STARTED, cloudModule.getLocalModule(), Status.OK_STATUS));
	}

	@Override
	public void startApplicationConsole(CloudFoundryServer cloudServer, CloudFoundryApplicationModule cloudModule,
			int showIndex, IProgressMonitor monitor) {
		if (cloudModule == null || cloudModule.getApplication() == null) {
			CloudFoundryPlugin.logError(
					"No application content to display to the console while starting application in the Cloud Foundry server."); //$NON-NLS-1$
			return;
		}
		if (showIndex < 0) {
			showIndex = 0;
		}
		SubMonitor subMonitor = SubMonitor.convert(monitor, cloudModule.getDeploymentInfo().getInstances() * 100);

		subMonitor.subTask(
				NLS.bind(Messages.CloudFoundryUiCallback_STARTING_CONSOLE, cloudModule.getDeployedApplicationName()));

		for (int i = 0; i < cloudModule.getDeploymentInfo().getInstances(); i++) {
			// Do not clear the console as pre application start information may
			// have been already sent to the console
			// output
			boolean shouldClearConsole = false;

			ConsoleManagerRegistry.getConsoleManager(cloudServer.getServer()).startConsole(cloudServer,
					StandardLogContentType.APPLICATION_LOG, cloudModule, i, i == showIndex, shouldClearConsole,
					subMonitor.newChild(100));
		}
	}

	@Override
	public void showCloudFoundryLogs(CloudFoundryServer cloudServer, CloudFoundryApplicationModule cloudModule,
			int showIndex, IProgressMonitor monitor) {
		ConsoleManagerRegistry.getConsoleManager(cloudServer.getServer()).showCloudFoundryLogs(cloudServer, cloudModule, showIndex,
				false, monitor);
	}

	@Override
	public void printToConsole(CloudFoundryServer cloudServer, CloudFoundryApplicationModule cloudModule,
			String message, boolean clearConsole, boolean isError) {
		ConsoleManagerRegistry.getConsoleManager(cloudServer.getServer()).writeToStandardConsole(message, cloudServer, cloudModule,
				0, clearConsole, isError);
	}

	@Override
	public void trace(CloudLog log, boolean clear) {
		ConsoleManagerRegistry.getInstance().trace(log, clear);
	}

	@Override
	public void showTraceView(boolean showTrace) {
		if (showTrace) {
			ConsoleManagerRegistry.getInstance().setTraceConsoleVisible();
		}
	}

	@Override
	public void applicationStarting(final CloudFoundryServer server, final CloudFoundryApplicationModule cloudModule) {

		// Only show the starting info for the first instance that is shown.
		// Not necessary to show staging
		// for instances that are not shown in the console.
		// FIXNS: Streaming of staging logs no longer works using CF client for
		// CF 1.6.0. Disabling til future
		// if (cloudModule.getStartingInfo() != null &&
		// cloudModule.getStartingInfo().getStagingFile() != null
		// && cloudModule.getApplication().getInstances() > 0) {
		//
		// boolean clearConsole = false;
		// StagingLogConsoleContent stagingContent = new
		// StagingLogConsoleContent(cloudModule.getStartingInfo(),
		// server);
		// ConsoleManager.getInstance().startConsole(server, new
		// ConsoleContents(stagingContent),
		// cloudModule.getApplication(), 0, true, clearConsole);
		//
		// }

	}

	@Override
	public void deleteApplication(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {
		stopApplicationConsole(cloudModule, cloudServer);
	}

	public void stopApplicationConsole(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {
		if (cloudModule == null) {
			return;
		}

		// If application is not deployed (i.e., there are no application
		// instances), still stop the console as
		// a console may have been created for the application even if it failed
		// to deploy or start.
		int totalInstances = cloudModule.isDeployed() ? cloudModule.getApplication().getInstances() : 0;
		int instance = 0;
		do {
			ConsoleManagerRegistry.getConsoleManager(cloudServer.getServer()).stopConsole(cloudServer.getServer(), cloudModule,
					instance);
			++instance;
		} while (instance < totalInstances);

	}

	@Override
	public void disconnecting(CloudFoundryServer cloudServer) {
		ConsoleManagerRegistry.getConsoleManager(cloudServer.getServer()).stopConsoles();
	}

	@Override
	public void getCredentials(final CloudFoundryServer server) {
		Display.getDefault().syncExec(new Runnable() {

			public void run() {
				CloudFoundryCredentialsWizard wizard = new CloudFoundryCredentialsWizard(server);
				WizardDialog dialog = new WizardDialog(
						PlatformUI.getWorkbench().getModalDialogShellProvider().getShell(), wizard);
				dialog.open();
			}
		});

		if (server.getUsername() == null || server.getUsername().length() == 0 || server.getPassword() == null
				|| server.getPassword().length() == 0 || server.getUrl() == null || server.getUrl().length() == 0) {
			throw new OperationCanceledException();
		}
	}

	@Override
	public DeploymentConfiguration prepareForDeployment(CloudFoundryServer server, CloudFoundryApplicationModule module,
			IProgressMonitor monitor) throws CoreException {
		return new ApplicationDeploymentUIHandler().prepareForDeployment(server, module, monitor);
	}

	@Override
	public void deleteServices(final List<String> services, final CloudFoundryServer cloudServer) {
		if (services == null || services.isEmpty()) {
			return;
		}

		Display.getDefault().syncExec(new Runnable() {

			public void run() {
				DeleteServicesWizard wizard = new DeleteServicesWizard(cloudServer, services);
				WizardDialog dialog = new WizardDialog(Display.getCurrent().getActiveShell(), wizard);
				dialog.open();
			}

		});
	}

	@Override
	public void displayAndLogError(final IStatus status) {
		if (status != null && status.getSeverity() == IStatus.ERROR) {

			CloudFoundryPlugin.log(status);

			Display.getDefault().asyncExec(new Runnable() {

				@Override
				public void run() {
					Shell shell = CFUiUtil.getShell();
					if (shell != null) {
						MessageDialog.openError(shell, Messages.CloudFoundryUiCallback_ERROR_CALLBACK_TITLE,
								status.getMessage());
					}
				}
			});
		}
	}

	public boolean prompt(final String title, final String message) {
		final boolean[] shouldContinue = new boolean[] { false };
		Display.getDefault().syncExec(new Runnable() {

			public void run() {

				Shell shell = CFUiUtil.getShell();
				if (shell != null) {
					shouldContinue[0] = MessageDialog.openConfirm(shell, title, message);
				}
			}
		});
		return shouldContinue[0];
	}

	@Override
	public CFRebelServerIntegration getJRebelServerIntegration() {
		CFRebelServerIntegration integration = new CloudRebelUIServerIntegration();
		return integration;
	}

	public ApplicationDebugLauncher getDebugLauncher(CloudFoundryServer cloudServer) {
		return new ApplicationDebugUILauncher();
	}
}
