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
package org.eclipse.cft.server.core.internal.application;

import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.wst.server.core.IServer;

public enum ApplicationRunState {

	STARTED(IServer.STATE_STARTED, Messages.APPLICATION_MODULE_RUNSTATE_STARTED),

	STOPPED(IServer.STATE_STOPPED, Messages.APPLICATION_MODULE_RUNSTATE_STOPPED),
	
	STARTING(IServer.STATE_STARTING, Messages.APPLICATION_MODULE_RUNSTATE_STARTING),

	UNKNOWN(IServer.STATE_UNKNOWN, Messages.APPLICATION_MODULE_RUNSTATE_UNKNOWN);

	private final int moduleRunstate;

	private final String label;

	private ApplicationRunState(int moduleRunstate, String label) {
		this.moduleRunstate = moduleRunstate;
		this.label = label;
	}

	public int getModuleRunstate() {
		return moduleRunstate;
	}

	public String getLabel() {
		return label;
	}

	public static ApplicationRunState getRunState(int moduleRunState) {
		switch (moduleRunState) {
		case IServer.STATE_STARTED:
			return STARTED;
		case IServer.STATE_STOPPED:
			return STOPPED;
		case IServer.STATE_STARTING:
			return STARTING;
		default:
			return UNKNOWN;
		}
	}
}
