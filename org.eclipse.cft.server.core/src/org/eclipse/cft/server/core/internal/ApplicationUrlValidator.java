/*******************************************************************************
 * Copyright (c) 2012, 2017 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.core.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Validates whether a url name value, and optionally a start command if an app
 * is a standalone app, are valid. If a standalone app, URL is optional, which
 * is also checked by this validator
 * <p/>
 * Valid url names should not include the protocol (e.g. http://www.google.com)
 * or queries in the name valid names are:
 * <p/>
 * www.google.com
 * <p/>
 * www$.google.com
 * <p/>
 * www.google.com4
 * <p/>
 * names with trailing or ending spaces, or spaces in between the name segments
 * are invalid.
 */
public class ApplicationUrlValidator {



	public ApplicationUrlValidator() {
	}

	public IStatus isValid(String url) {
		// Check URL validity
		String errorMessage = null;

		if (StringUtils.isEmpty(url)) {
			errorMessage = Messages.EMPTY_URL_ERROR;
		}
		else if (new URLNameValidation(url).hasInvalidCharacters()) {
			errorMessage = Messages.INVALID_CHARACTERS_ERROR;
		}

		IStatus status = errorMessage != null ? CloudFoundryPlugin.getErrorStatus(errorMessage) : Status.OK_STATUS;

		return status;
	}
}
