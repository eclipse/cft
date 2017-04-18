
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates whether a url name value.
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
 * 
 */
public class URLNameValidation {

	private final String value;

	public URLNameValidation(String value) {
		this.value = value;
	}

	private final Pattern VALID_CHARS = Pattern.compile("[A-Za-z\\$_0-9\\-.]+"); //$NON-NLS-1$



	public boolean hasInvalidCharacters() {
		if (!StringUtils.isEmpty(value)) {
			Matcher matcher = VALID_CHARS.matcher(value);
			return !matcher.matches();
		}
		return true;
	}

}
