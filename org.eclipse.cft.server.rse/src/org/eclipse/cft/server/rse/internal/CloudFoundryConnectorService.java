/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.rse.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.subsystems.BasicConnectorService;

/**
 * @author Leo Dos Santos
 * @author Christian Dupuis
 */
public class CloudFoundryConnectorService extends BasicConnectorService {

	public CloudFoundryConnectorService(IHost host) {
		super("Cloud Service Connector", "Manages connections to a cloud service", host, 80); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public boolean isConnected() {
		return true;
	}

	@Override
	protected void internalConnect(IProgressMonitor monitor) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	protected void internalDisconnect(IProgressMonitor monitor) throws Exception {
		// TODO Auto-generated method stub

	}

}
