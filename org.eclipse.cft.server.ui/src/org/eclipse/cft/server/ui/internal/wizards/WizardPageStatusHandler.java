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

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.wizard.WizardPage;

/**
 * 
 * Updates status of a wizard if the wizard components are defined by an
 * {@link WizardPage}
 *
 */
public class WizardPageStatusHandler extends WizardStatusHandler {

	private final WizardPage wizardPage;

	public WizardPageStatusHandler(WizardPage wizardPage) {
		this.wizardPage = wizardPage;
	}

	protected void update() {
		if (wizardPage.getWizard() != null && wizardPage.getWizard().getContainer() != null
				&& wizardPage.getWizard().getContainer().getCurrentPage() != null) {
			wizardPage.getWizard().getContainer().updateButtons();
		}
	}

	protected void setWizardError(String message) {
		wizardPage.setErrorMessage(message);
	}

	protected void setWizardInformation(String message) {
		wizardPage.setMessage(message, DialogPage.INFORMATION);
	}

	protected void setWizardMessage(String message) {
		wizardPage.setMessage(message, DialogPage.NONE);
	}

}
