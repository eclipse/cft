/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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
import java.util.List;

import org.eclipse.cft.server.core.AbstractApplicationDelegate;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.application.ApplicationRegistry;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.DeploymentInfoWorkingCopy;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;

/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class MappedURLsWizard extends Wizard {

	private final String appName;

	private final CloudFoundryServer cloudServer;

	private List<String> existingURIs;

	private CloudFoundryApplicationModule applicationModule;

	private MappedURLsWizardPage page;

	public MappedURLsWizard(CloudFoundryServer cloudServer, CloudFoundryApplicationModule applicationModule,
			List<String> existingURIs) {
		Assert.isNotNull(applicationModule);
		this.cloudServer = cloudServer;
		this.appName = applicationModule.getDeployedApplicationName();
		this.applicationModule = applicationModule;
		this.existingURIs = existingURIs;

		setWindowTitle(Messages.MappedURLsWizard_TITLE_MOD_MAPPED_URL);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		page = new MappedURLsWizardPage(cloudServer, existingURIs, applicationModule);
		addPage(page);
	}

	public List<String> getURLs() {
		return page.getURLs();
	}

	public boolean requiresURL() {
		IModule localModule = applicationModule.getLocalModule();

		if (localModule == null) {
			return true;
		}

		AbstractApplicationDelegate delegate = ApplicationRegistry.getApplicationDelegate(localModule);

		return delegate == null || delegate.requiresURL();
	}

	@Override
	public boolean performFinish() {
		page.setErrorMessage(null);

		final IStatus[] result = new IStatus[1];

		IRunnableWithProgress runnable = null;

		page.setMessage(Messages.MappedURLsWizard_TEXT_UPDATE_URL);

		// If the app module is not deployed, set the URIs in the deployment
		// descriptor.
		if (!applicationModule.isDeployed()) {
			runnable = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					try {
						DeploymentInfoWorkingCopy wc = applicationModule.resolveDeploymentInfoWorkingCopy(monitor);
						wc.setUris(page.getURLs());
						wc.save();
					}
					catch (CoreException e) {
						result[0] = e.getStatus();
					}
				}
			};
		}
		else {
			runnable = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					try {
						cloudServer.getBehaviour().operations()
								.mappedUrlsUpdate(appName, page.getURLs()).run(monitor);
					}
					catch (CoreException e) {
						result[0] = e.getStatus();
					}
				}
			};
		}

		try {
			getContainer().run(true, true, runnable);
		}
		catch (InvocationTargetException e) {
			result[0] = CloudFoundryPlugin.getErrorStatus(e);
		}
		catch (InterruptedException e) {
			result[0] = CloudFoundryPlugin.getErrorStatus(e);
		}

		if (result[0] != null && !result[0].isOK()) {
			page.setErrorMessage(NLS.bind(Messages.MappedURLsWizard_ERROR_CHANGE_URL, result[0].getMessage()));
			return false;
		}
		else {
			return true;
		}

	}
}
