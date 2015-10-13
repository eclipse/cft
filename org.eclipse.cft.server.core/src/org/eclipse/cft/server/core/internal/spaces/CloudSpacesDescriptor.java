/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal.spaces;

public class CloudSpacesDescriptor {

	private final String descriptorID;

	private final CloudOrgsAndSpaces spaces;

	public CloudSpacesDescriptor(CloudOrgsAndSpaces spaces, String userName, String password, String actualServerURL,
			boolean selfSigned) {
		this.spaces = spaces;
		descriptorID = getDescriptorID(userName, password, actualServerURL, selfSigned);

	}

	public CloudOrgsAndSpaces getOrgsAndSpaces() {
		return spaces;
	}

	public String getID() {
		return descriptorID;
	}

	public static String getDescriptorID(String userName, String password, String actualURL, boolean selfSigned) {
		if (userName == null || password == null || actualURL == null) {
			return null;
		}
		return userName + password + actualURL + selfSigned;
	}

}
