/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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

import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;

public abstract class CloudFoundryAwareWizardPage extends WizardPage {

	protected CloudFoundryAwareWizardPage(String pageName, String title, String description, ImageDescriptor banner) {
		super(pageName);
		if (title != null) {
			setTitle(title);
		}
		if (description != null) {
			setDescription(description);
		}
		if (banner == null) {
			banner = CloudFoundryImages.DEFAULT_WIZARD_BANNER;
		}
		setImageDescriptor(banner);

	}

}
