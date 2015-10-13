/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.ValidationEvents;
import org.eclipse.cft.server.ui.internal.CloudSpacesDelegate;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.ServerWizardValidator;
import org.eclipse.cft.server.ui.internal.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

/**
 * Validates credentials (username, password) as well as server URL both locally
 * and remotely. This validator should be used when username, password and
 * server URL are expected to change, for example, if the validator is used in a
 * UI component allows users to modify these values.
 *
 */
public class CredentialsWizardValidator extends ServerWizardValidator {

	public CredentialsWizardValidator(CloudFoundryServer cloudServer, CloudSpacesDelegate cloudServerSpaceDelegate) {
		super(cloudServer, cloudServerSpaceDelegate);
	}

	@Override
	protected ValidationStatus validateLocally() {

		String userName = getCloudFoundryServer().getUsername();
		String password = getCloudFoundryServer().getPassword();
		String url = getCloudFoundryServer().getUrl();
		String message = null;

		boolean valuesFilled = false;
		int validationEventType = ValidationEvents.VALIDATION;

		if (userName == null || userName.trim().length() == 0) {
			message = Messages.ENTER_AN_EMAIL;
		}
		else if (password == null || password.trim().length() == 0) {
			message = Messages.ENTER_A_PASSWORD;
		}
		else if (url == null || url.trim().length() == 0) {
			message = NLS.bind(Messages.SELECT_SERVER_URL, getSpaceDelegate().getServerServiceName());
		}
		else {
			valuesFilled = true;
			message = Messages.SERVER_WIZARD_VALIDATOR_CLICK_TO_VALIDATE;
		}

		// Missing values should appear as INFO in the wizard, as to not show an
		// error when the wizard credentials page first opens. INFO will still
		// keep
		// wizard buttons disabled until an OK status is sent.
		int statusType = valuesFilled ? IStatus.OK : IStatus.INFO;

		IStatus status = CloudFoundryPlugin.getStatus(message, statusType);
		return getValidationStatus(status, validationEventType);
	}

}
