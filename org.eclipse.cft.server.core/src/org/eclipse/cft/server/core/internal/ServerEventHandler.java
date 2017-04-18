/*******************************************************************************
 * Copyright (c) 2012, 2017 Pivotal Software, Inc. and others
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.cft.server.core.internal.application.ModuleChangeEvent;
import org.eclipse.cft.server.core.internal.client.ModulesUpdatedEvent;
import org.eclipse.cft.server.core.internal.client.ServicesUpdatedEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;

/**
 * Fires server refresh events. Only one handler is active per workbench runtime
 * session.
 * 
 */
public class ServerEventHandler {

	private static ServerEventHandler handler;

	public static ServerEventHandler getDefault() {
		if (handler == null) {
			handler = new ServerEventHandler();
		}
		return handler;
	}

	private final List<CloudServerListener> applicationListeners = new CopyOnWriteArrayList<CloudServerListener>();

	public synchronized void addServerListener(CloudServerListener listener) {
		if (listener != null && !applicationListeners.contains(listener)) {
			applicationListeners.add(listener);
		}
	}

	public synchronized void removeServerListener(CloudServerListener listener) {
		applicationListeners.remove(listener);
	}

	public void fireServicesUpdated(CloudFoundryServer server, List<CFServiceInstance> services) {
		fireServerEvent(new ServicesUpdatedEvent(server, CloudServerEvent.EVENT_SERVICES_UPDATED, services));
	}
	
	public void firePasswordUpdated(CloudFoundryServer server, IStatus status) {
		fireServerEvent(new CloudServerEvent(server, CloudServerEvent.EVENT_UPDATE_PASSWORD, status));
	}

	public void fireUpdateCompleted(CloudFoundryServer server) {
		fireServerEvent(new CloudServerEvent(server, CloudServerEvent.EVENT_UPDATE_COMPLETED));
	}
	
	public void fireUpdateStarting(CloudFoundryServer server) {
		fireServerEvent(new CloudServerEvent(server, CloudServerEvent.EVENT_UPDATE_STARTING));
	}
	
	public void fireAppInstancesChanged(CloudFoundryServer server, IModule module) {
		fireServerEvent(new ModuleChangeEvent(server, CloudServerEvent.EVENT_INSTANCES_UPDATED, module,
				Status.OK_STATUS));
	}

	public void fireModuleUpdated(CloudFoundryServer server, IModule module) {
		fireServerEvent(new ModuleChangeEvent(server, CloudServerEvent.EVENT_MODULE_UPDATED, module,
				Status.OK_STATUS));
	}
	
	public void fireModulesUpdated(CloudFoundryServer server, IModule[] modules) {
		fireServerEvent(new ModulesUpdatedEvent(server, CloudServerEvent.EVENT_MODULES_UPDATED, modules));
	}

	public void fireAppDeploymentChanged(CloudFoundryServer server, IModule module) {
		fireServerEvent(new ModuleChangeEvent(server, CloudServerEvent.EVENT_APP_DEPLOYMENT_CHANGED, module,
				Status.OK_STATUS));
	}

	public synchronized void fireServerEvent(CloudServerEvent event) {
		CloudServerListener[] listeners = applicationListeners.toArray(new CloudServerListener[0]);
		for (CloudServerListener listener : listeners) {
			listener.serverChanged(event);
		}
	}
}
