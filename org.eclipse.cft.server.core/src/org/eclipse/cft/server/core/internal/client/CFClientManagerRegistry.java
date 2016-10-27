/*******************************************************************************
 * Copyright (c) 2015, 2016 Pivotal Software, Inc. and others
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.ProviderPriority;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;

public class CFClientManagerRegistry {

	// list of targets by priority
	private Map<ProviderPriority, List<CFClientManagerProvider>> providersPerPriority = new HashMap<>();

	public CFClientManagerRegistry() {

	}

	public synchronized void register(CFClientManagerProvider provider) {

		if (provider != null) {
			List<CFClientManagerProvider> prvlist = providersPerPriority.get(provider.getPriority());
			if (prvlist == null) {
				prvlist = new ArrayList<>();
				providersPerPriority.put(provider.getPriority(), prvlist);
			}
			if (!prvlist.contains(provider)) {
				prvlist.add(provider);
			}
		}
	}

	/**
	 * Fetches a client manager for the give Cloud Foundry Server
	 * @param cloudFoundryServer the cloud server that requires client
	 * management
	 * @return non-null client manager.
	 * @throws CoreException if no client manager is found for the given server
	 */
	public synchronized CFClientManager getClientManager(String serverUrl) throws CoreException {

		CFClientManager manager = null;

		if (serverUrl != null) {
			for (ProviderPriority priority : ProviderPriority.values()) {

				List<CFClientManagerProvider> providers = providersPerPriority.get(priority);
				if (providers != null) {
					for (CFClientManagerProvider provider : providers) {
						manager = provider.getClientManager(serverUrl);
						if (manager != null) {
							break;
						}
					}
				}
				if (manager != null) {
					break;
				}
			}
		}

		if (manager == null) {
			throw CloudErrorUtil.toCoreException(
					NLS.bind(Messages.CloudFoundryTargetManager_NO_TARGET_DEFINITION_FOUND, serverUrl));
		}
		return manager;
	}
}
