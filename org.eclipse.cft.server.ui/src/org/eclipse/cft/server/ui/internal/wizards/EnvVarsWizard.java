/*******************************************************************************
 * Copyright (c) 2013, 2015 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.DeploymentInfoWorkingCopy;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;

/**
 * Allows an application's environment variables to be edited and set in the
 * app's deployment info. The environment variables are also set in the server.
 */
public class EnvVarsWizard extends Wizard {

	private final CloudFoundryServer cloudServer;

	private final CloudFoundryApplicationModule appModule;

	private DeploymentInfoWorkingCopy infoWorkingCopy;

	private CloudFoundryApplicationEnvVarWizardPage envVarPage;

	public EnvVarsWizard(CloudFoundryServer server, CloudFoundryApplicationModule appModule,
			DeploymentInfoWorkingCopy workingCopy) {

		Assert.isNotNull(server);
		Assert.isNotNull(appModule);
		Assert.isNotNull(workingCopy);

		this.infoWorkingCopy = workingCopy;
		this.cloudServer = server;
		setWindowTitle(server.getServer().getName());
		setNeedsProgressMonitor(true);
		this.appModule = appModule;
	}

	@Override
	public void addPages() {

		envVarPage = new CloudFoundryApplicationEnvVarWizardPage(cloudServer, infoWorkingCopy);
		envVarPage.setWizard(this);
		addPage(envVarPage);
	}

	@Override
	public boolean performFinish() {
		infoWorkingCopy.save();
		final IStatus[] result = new IStatus[1];
		try {

			envVarPage.setMessage(Messages.EnvVarsWizard_TEXT_ENV_VAR);
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					try {
						cloudServer
								.getBehaviour()
								.operations()
								.environmentVariablesUpdate(appModule.getLocalModule(),
										appModule.getDeployedApplicationName(), infoWorkingCopy.getEnvVariables())
								.run(monitor);
					}
					catch (CoreException e) {
						result[0] = e.getStatus();
					}
				}
			});
			envVarPage.setMessage(null);
		}
		catch (InvocationTargetException e) {
			result[0] = CloudFoundryPlugin.getErrorStatus(e);
		}
		catch (InterruptedException e) {
			result[0] = CloudFoundryPlugin.getErrorStatus(e);

		}
		if (result[0] != null && !result[0].isOK()) {
			envVarPage.setErrorMessage(Messages.EnvVarsWizard_ERROR_ENV_VAR + result[0].getMessage());
			return false;
		}
		else {
			return true;
		}
	}

}
