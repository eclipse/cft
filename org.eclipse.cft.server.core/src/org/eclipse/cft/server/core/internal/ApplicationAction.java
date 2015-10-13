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
package org.eclipse.cft.server.core.internal;

public enum ApplicationAction {
	RESTART, START("Run"), STOP, UPDATE_RESTART, PUSH; //$NON-NLS-1$

	private String displayName = ""; //$NON-NLS-1$

	private String shortDisplay = ""; //$NON-NLS-1$

	private ApplicationAction(String displayName, String shortDisplay) {
		this.displayName = displayName;
		this.shortDisplay = shortDisplay;
	}

	private ApplicationAction(String displayName) {
		this.displayName = displayName;
	}

	private ApplicationAction() {

	}

	public String getDisplayName() {
		return displayName;
	}

	public String getShortDisplay() {
		return shortDisplay;
	}

}