/*******************************************************************************
 * Copyright (c) 2013, 2015 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.standalone.ui.internal;

import java.util.List;

import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.ui.DefaultApplicationWizardDelegate;
import org.eclipse.cft.server.ui.internal.wizards.ApplicationWizardDescriptor;
import org.eclipse.cft.server.ui.internal.wizards.CloudFoundryApplicationEnvVarWizardPage;
import org.eclipse.cft.server.ui.internal.wizards.CloudFoundryApplicationServicesWizardPage;
import org.eclipse.cft.server.ui.internal.wizards.CloudFoundryApplicationWizardPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

public class StandaloneApplicationWizardDelegate extends DefaultApplicationWizardDelegate {

	public StandaloneApplicationWizardDelegate() {
	}

	protected void createPages(ApplicationWizardDescriptor descriptor, IServer server, IModule module,
			List<IWizardPage> defaultPages) throws CoreException {
		CloudFoundryServer cloudServer = CloudServerUtil.getCloudServer(server);
		CloudFoundryApplicationModule applicationModule = cloudServer.getExistingCloudModule(module);
		ApplicationUrlLookupService urllookup = ApplicationUrlLookupService.getCurrentLookup(cloudServer);

		StandaloneDeploymentWizardPage deploymentPage = new StandaloneDeploymentWizardPage(cloudServer,
				applicationModule, descriptor, urllookup, this);

		CloudFoundryApplicationWizardPage applicationNamePage = new CloudFoundryApplicationWizardPage(cloudServer,
				applicationModule, descriptor);

		defaultPages.add(applicationNamePage);

		defaultPages.add(deploymentPage);

		CloudFoundryApplicationServicesWizardPage servicesPage = new CloudFoundryApplicationServicesWizardPage(
				cloudServer, applicationModule, descriptor);

		defaultPages.add(servicesPage);

		defaultPages.add(new CloudFoundryApplicationEnvVarWizardPage(cloudServer, descriptor.getDeploymentInfo()));
	}

}
