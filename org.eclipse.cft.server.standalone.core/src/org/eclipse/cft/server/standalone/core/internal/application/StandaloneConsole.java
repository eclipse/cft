/*******************************************************************************
 * Copyright (c) 2016 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.standalone.core.internal.application;

import org.eclipse.cft.server.core.internal.CFConsoleHandler;
import org.eclipse.cft.server.standalone.core.internal.Messages;

public class StandaloneConsole extends CFConsoleHandler {

	private static CFConsoleHandler instance;

	public StandaloneConsole() {
		super(Messages.StandaloneConsole_PREFIX);
	}

	public static CFConsoleHandler getDefault() {
		if (instance == null) {
			instance = new StandaloneConsole();
		}
		return instance;
	}

}
