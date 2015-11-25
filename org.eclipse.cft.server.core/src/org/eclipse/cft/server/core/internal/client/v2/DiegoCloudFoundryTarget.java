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
package org.eclipse.cft.server.core.internal.client.v2;

import org.eclipse.cft.server.core.internal.CloudFoundryServerTarget;
import org.eclipse.cft.server.core.internal.client.ClientRequestFactory;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;

public class DiegoCloudFoundryTarget extends CloudFoundryServerTarget {

	@Override
	public String getServerUri() {
		return ALL_SERVERS;
	}

	@Override
	public ClientRequestFactory getRequestFactory(CloudFoundryServerBehaviour behaviour) {
		return new DiegoRequestFactory(behaviour);
	}

	@Override
	public boolean supportsSsh() {
		return true;
	}

}
