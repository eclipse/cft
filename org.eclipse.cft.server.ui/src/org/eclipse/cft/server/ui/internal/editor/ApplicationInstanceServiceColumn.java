/*******************************************************************************
 * Copyright (c) 2012, 2016 Pivotal Software, Inc. and others 
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
package org.eclipse.cft.server.ui.internal.editor;

import org.eclipse.cft.server.ui.internal.Messages;

public enum ApplicationInstanceServiceColumn {

	Name(125, Messages.TableColumn_NAME), 
	Service(100, Messages.TableColumn_SERVICE), 
	Plan(75, Messages.TableColumn_PLAN), 
	Version(75, Messages.TableColumn_VERSION);

	private final int width;

	private final String userFacingName;

	private ApplicationInstanceServiceColumn(int width, String userFacingName) {
		this.width = width;
		this.userFacingName = userFacingName;
	}

	public int getWidth() {
		return width;
	}

	public String getUserFacingName() {
		return userFacingName;
	}
}
