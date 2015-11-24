/*******************************************************************************
 * Copied from Spring Tool Suite. Original license:
 * 
 * Copyright (c) 2015 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.cft.server.core.internal.client;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

abstract public class BehaviourRequest<T> extends LocalServerRequest<T> {
	
	protected final CloudFoundryServerBehaviour behaviour;

	public BehaviourRequest(String label, CloudFoundryServerBehaviour behaviour) {
		super(label);
		this.behaviour = behaviour;
	}

	@Override
	protected CloudFoundryOperations getClient(IProgressMonitor monitor) throws CoreException {
		return this.behaviour.getClient(monitor);
	}

	@Override
	protected CloudFoundryServer getCloudServer() throws CoreException {
		return this.behaviour.getCloudFoundryServer();
	}

}