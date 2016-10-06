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
package org.eclipse.cft.server.core.internal;

import java.util.EventObject;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;

/**
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Leo Dos Santos
 * @author Terry Denney
 */
public class CloudServerEvent extends EventObject {

	public static final int EVENT_INSTANCES_UPDATED = 100;

	public static final int EVENT_SERVICES_UPDATED = 200;

	public static final int EVENT_UPDATE_PASSWORD = 300;

	public static final int EVENT_MODULE_UPDATED = 310;
	
	public static final int EVENT_MODULES_UPDATED = 320;
	
	public static final int EVENT_UPDATE_STARTING = 400;

	public static final int EVENT_UPDATE_COMPLETED = 401;
	
	public static final int EVENT_SERVER_CONNECTED = 403;

	public static final int EVENT_SERVER_DISCONNECTED = 406;

	public static final int EVENT_APP_DEPLOYMENT_CHANGED = 410;

	public static final int EVENT_APP_DELETED = 420;

	public static final int EVENT_APP_STOPPED = 425;

	public static final int EVENT_APP_STARTED = 427;

	public static final int EVENT_APP_URL_CHANGED = 430;

	public static final int EVENT_APP_DEBUG = 500;

	public static final int EVENT_CLOUD_OP_ERROR = 600;

	public static final int EVENT_JREBEL_REMOTING_UPDATE = 700;

	private static final long serialVersionUID = 1L;

	private int type = -1;

	private IStatus status;

	public CloudServerEvent(CloudFoundryServer server) {
		this(server, -1);
	}

	public CloudServerEvent(CloudFoundryServer server, int type) {
		this(server, type, null);
	}

	public CloudServerEvent(CloudFoundryServer server, int type, IStatus status) {
		super(server);
		Assert.isNotNull(server);
		this.type = type;
		this.status = status;
	}

	public CloudFoundryServer getServer() {
		return (CloudFoundryServer) getSource();
	}

	public int getType() {
		return type;
	}

	public IStatus getStatus() {
		return status;
	}

	@Override
	public String toString() {
		return "CloudServerEvent [type=" + type + ", server=" + getServer().getServer().getId() + ")]";
	}
}
