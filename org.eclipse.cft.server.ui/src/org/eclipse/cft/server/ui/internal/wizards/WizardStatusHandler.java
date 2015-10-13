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

import org.eclipse.cft.server.ui.internal.IPartChangeListener;
import org.eclipse.cft.server.ui.internal.PartChangeEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Handles status in a wizard, including display the status message and
 * refreshing the wizard UI state.
 * 
 */
public abstract class WizardStatusHandler implements IPartChangeListener {

	public void handleChange(PartChangeEvent event) {
		IStatus status = event != null ? event.getStatus() : null;
		handleChange(status);
	}

	public void handleChange(IStatus status) {
		if (status == null) {
			status = Status.OK_STATUS;
		}
		setWizardError(null);

		if (status.getSeverity() == IStatus.INFO) {
			setWizardInformation(status.getMessage());
		}
		else if (status.getSeverity() == IStatus.ERROR) {
			setWizardError(status.getMessage());
		}
		else if (status.getSeverity() == IStatus.OK && !Status.OK_STATUS.getMessage().equals(status.getMessage())) {
			// Do not display "OK" messages.
			setWizardMessage(status.getMessage());
		}

		update();
	}

	/**
	 * Update the wizard state and buttons
	 */
	abstract protected void update();

	abstract protected void setWizardError(String message);

	abstract protected void setWizardInformation(String message);

	abstract protected void setWizardMessage(String message);

}
