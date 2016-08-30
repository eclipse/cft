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
package org.eclipse.cft.server.core.internal.client;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.wst.server.core.IModule;

public class ModulesUpdatedEvent extends CloudServerEvent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final IModule[] modules;

	public ModulesUpdatedEvent(CloudFoundryServer server, int type, IModule[] modules) {
		super(server, type);;
		this.modules = modules;
	}

	public IModule[] getModules() {
		return modules;
	}
}
