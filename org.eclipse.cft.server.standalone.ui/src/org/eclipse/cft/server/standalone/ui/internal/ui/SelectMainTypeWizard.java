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
package org.eclipse.cft.server.standalone.ui.internal.ui;

import java.util.List;

import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.wizard.Wizard;

/**
 * 
 */
public class SelectMainTypeWizard extends Wizard {

	private final String serverID;

	private final List<IType> mainTypes;

	private SelectMainTypeWizardPage page;

	public SelectMainTypeWizard(String serverID, List<IType> mainTypes) {
		this.serverID = serverID;
		this.mainTypes = mainTypes;
	}

	@Override
	public void addPages() {
		page = new SelectMainTypeWizardPage(mainTypes,
				CloudFoundryImages.getWizardBanner(serverID));
		addPage(page);
	}

	public boolean performFinish() {
		return page != null && page.getSelectedMainType() != null;
	}

	public IType getSelectedMainType() {
		return page != null ? page.getSelectedMainType() : null;
	}
}
