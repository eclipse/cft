/*******************************************************************************
 * Copyright (c) 2013, 2016 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.ui.internal.wizards.ApplicationWizardDelegate;
import org.eclipse.cft.server.ui.internal.wizards.ApplicationWizardDescriptor;
import org.eclipse.cft.server.ui.internal.wizards.CloudFoundryApplicationEnvVarWizardPage;
import org.eclipse.cft.server.ui.internal.wizards.CloudFoundryApplicationServicesWizardPage;
import org.eclipse.cft.server.ui.internal.wizards.CloudFoundryApplicationWizardPage;
import org.eclipse.cft.server.ui.internal.wizards.CloudFoundryDeploymentWizardPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * Default delegate for any application that uses standard Java web deployment
 * properties for Cloud Foundry. For example, default Java web applications
 * require at least one mapped application URL, therefore the wizard delegate
 * will provide wizard pages that require that at least one mapped application
 * URL is set by the user in the UI.
 * 
 */
public class DefaultApplicationWizardDelegate extends ApplicationWizardDelegate {

	public List<IWizardPage> getWizardPages(ApplicationWizardDescriptor descriptor, IServer server, IModule module) {
		List<IWizardPage> defaultPages = new ArrayList<IWizardPage>();

		try {
			createPages(descriptor, server, module, defaultPages);
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError("Unable to open application deployment wizard for server type " + server.getId() //$NON-NLS-1$
					+ " and module " + module.getName(), e); //$NON-NLS-1$
		}
		return defaultPages;

	}

	protected void createPages(ApplicationWizardDescriptor applicationDescriptor, IServer server, IModule module,
			List<IWizardPage> defaultPages) throws CoreException {
		CloudFoundryServer cloudServer = CloudServerUtil.getCloudServer(server);
		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);

		ApplicationUrlLookupService urllookup = ApplicationUrlLookupService.getCurrentLookup(cloudServer);

		CloudFoundryDeploymentWizardPage deploymentPage = new CloudFoundryDeploymentWizardPage(cloudServer, appModule,
				applicationDescriptor, urllookup, this);

		CloudFoundryApplicationWizardPage applicationNamePage = new CloudFoundryApplicationWizardPage(cloudServer,
				appModule, applicationDescriptor);

		defaultPages.add(applicationNamePage);

		defaultPages.add(deploymentPage);

		CloudFoundryApplicationServicesWizardPage servicesPage = new CloudFoundryApplicationServicesWizardPage(
				cloudServer, appModule, applicationDescriptor);

		defaultPages.add(servicesPage);

		defaultPages.add(
				new CloudFoundryApplicationEnvVarWizardPage(cloudServer, applicationDescriptor.getDeploymentInfo()));

	}
}
