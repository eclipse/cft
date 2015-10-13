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
 *     IBM - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal;

import org.eclipse.cft.server.core.AbstractCloudFoundryUrl;

/**
 * Represents a user defined CloudFoundry server Url which is used
 * in several sections of the code.
 */
public class UserDefinedCloudFoundryUrl extends AbstractCloudFoundryUrl {
	private boolean selfSigned;

	public UserDefinedCloudFoundryUrl(String name, String url, boolean selfSigned) {
		super (name, url, null, null);
		this.selfSigned = selfSigned;
	}
	
	/**
	 * Must always return true
	 */
	@Override
	public boolean getUserDefined() {
		return true;
	}
	
	/**
	 * Returns true or false, depending on the value of self-signed boolean passed
	 * in constructor
	 */
	@Override
	public boolean getSelfSigned() {
		return selfSigned;
	}
}
