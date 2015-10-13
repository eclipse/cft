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
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.application;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;

/**
 * Event indicating any type of change on a given module, whether the publish
 * state has changed (added or removed), module is started/stopped, or any
 * service bindings, URL mapping changes, or scaling that occurs for the
 * associated Cloud application. It may also be used to indicate that the
 * module's application has been updated from Cloud space.
 *
 */
public class ModuleChangeEvent extends CloudServerEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final IStatus status;

	private final IModule module;

	public ModuleChangeEvent(CloudFoundryServer server, int type, IModule module, IStatus status) {
		super(server, type);
		this.status = status != null ? status : Status.OK_STATUS;
		this.module = module;
	}

	public IStatus getStatus() {
		return status;
	}

	public IModule getModule() {
		return module;
	}

}
