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

import org.eclipse.cft.server.core.internal.ProviderPriority;

/**
 * Creates a client manager for any non-null URL. Since URL can be anything, its
 * not possible to be more specific with the URL check. This v1 client manager
 * is low priority, and contributors to the CFT framework can specify higher
 * priority client providers that are specific to certain servers
 */
public class V1ClientManagerProvider implements CFClientManagerProvider {

	@Override
	public CFClientManager getClientManager(String serverUrl) {
		if (serverUrl == null) {
			return null;
		}

		return new V1ClientManager();
	}

	@Override
	public ProviderPriority getPriority() {
		return ProviderPriority.LOW;
	}

}
