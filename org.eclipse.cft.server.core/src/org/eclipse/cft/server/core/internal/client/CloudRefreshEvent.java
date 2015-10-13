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
package org.eclipse.cft.server.core.internal.client;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.application.ModuleChangeEvent;
import org.eclipse.wst.server.core.IModule;

public class CloudRefreshEvent extends ModuleChangeEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final List<CloudService> services;

	public CloudRefreshEvent(CloudFoundryServer server, IModule module, int type, List<CloudService> services) {
		super(server, type, module, null);
		this.services = services;
	}

	public List<CloudService> getServices() {
		return services;
	}

}