/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.ValueValidationUtil;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.ui.internal.wizards.ApplicationWizardDelegate;
import org.eclipse.cft.server.ui.internal.wizards.ApplicationWizardDescriptor;
import org.eclipse.cft.server.ui.internal.wizards.CloudFoundryDeploymentWizardPage;

public class StandaloneDeploymentWizardPage extends
		CloudFoundryDeploymentWizardPage {

	public StandaloneDeploymentWizardPage(CloudFoundryServer server,
			CloudFoundryApplicationModule module,
			ApplicationWizardDescriptor descriptor,
			ApplicationUrlLookupService urlLookup,
			ApplicationWizardDelegate delegate) {
		super(server, module, descriptor, urlLookup, delegate);
	}

	@Override
	protected void setUrlInDescriptor(String url) {

		if (ValueValidationUtil.isEmpty(url)) {
			// Set an empty list if URL is empty as it can cause problems when
			// deploying a standalone application
			List<String> urls = new ArrayList<String>();

			descriptor.getDeploymentInfo().setUris(urls);
			return;
		}
		super.setUrlInDescriptor(url);
	}

}
