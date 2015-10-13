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
package org.eclipse.cft.server.ui.internal.wizards;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.cft.server.ui.internal.CloudServerSpacesDelegate;
import org.eclipse.cft.server.ui.internal.CloudSpacesDelegate;
import org.eclipse.cft.server.ui.internal.CloudSpacesSelectionPart;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class CloudFoundryCloudSpaceWizardpage extends WizardPage {

	protected CloudSpacesDelegate cloudServerSpaceDelegate;

	protected final CloudFoundryServer cloudServer;

	protected CloudSpacesSelectionPart spacesPart;

	public CloudFoundryCloudSpaceWizardpage(CloudFoundryServer cloudServer, CloudServerSpacesDelegate cloudServerSpaceDelegate) {
		super(cloudServer.getServer().getName() + Messages.CloudFoundryCloudSpaceWizardpage_TEXT_ORG_AND_SPACES);
		this.cloudServer = cloudServer;
		this.cloudServerSpaceDelegate = cloudServerSpaceDelegate;
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
	}

	public void createControl(Composite parent) {

		spacesPart = new CloudSpacesSelectionPart(cloudServerSpaceDelegate, cloudServer, this);
		spacesPart.addPartChangeListener(new WizardPageStatusHandler(this));
		Control composite = spacesPart.createPart(parent);
		setControl(composite);
	}

	public boolean isPageComplete() {
		return cloudServerSpaceDelegate != null && cloudServerSpaceDelegate.hasSpace();
	}

	public void refreshListOfSpaces() {
		if (spacesPart != null) {
			spacesPart.setInput();
		}
	}

}
