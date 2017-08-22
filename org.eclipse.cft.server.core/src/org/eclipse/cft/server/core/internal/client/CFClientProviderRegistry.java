/*******************************************************************************
 * Copyright (c) 2015, 2017 Pivotal Software, Inc. and others
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

import org.eclipse.cft.server.core.internal.ProviderPriority;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class CFClientProviderRegistry {

	final public static String EXTENSION_POINT = "org.eclipse.cft.server.core.client"; //$NON-NLS-1$

	final public static String PROVIDER_ELEMENT = "clientProvider"; //$NON-NLS-1$

	final public static String CLASS_ATTR = "class"; //$NON-NLS-1$

	// list of targets by priority
	private Map<ProviderPriority, List<CFClientProvider>> providersPerPriority = null;

	public static final CFClientProviderRegistry INSTANCE = new CFClientProviderRegistry();

	private CFClientProviderRegistry() {
	}

	public synchronized void load() throws CoreException {

		if (providersPerPriority == null) {
			providersPerPriority = new HashMap<>();

			IExtensionPoint extnPoint = Platform.getExtensionRegistry().getExtensionPoint(EXTENSION_POINT);
			if (extnPoint != null) {
				for (IExtension extension : extnPoint.getExtensions()) {
					for (IConfigurationElement config : extension.getConfigurationElements()) {
						if (PROVIDER_ELEMENT.equals(config.getName())) {
							CFClientProvider provider = (CFClientProvider) config.createExecutableExtension(CLASS_ATTR);
							if (provider != null) {

								List<CFClientProvider> prvlist = providersPerPriority.get(provider.getPriority());
								if (prvlist == null) {
									prvlist = new ArrayList<>();
									providersPerPriority.put(provider.getPriority(), prvlist);
								}
								if (!prvlist.contains(provider)) {
									prvlist.add(provider);
								}
							}
						}
					}
				}
			}
		}

	}

	/**
	 * Fetches a client provider for the give Cloud Foundry Server
	 * @param cloudFoundryServer the cloud server that requires client
	 * management
	 * @return client provider, or null if none registered for the given
	 * serverUrl
	 */
	public CFClientProvider getClientProvider(String serverUrl, CloudInfo info) throws CoreException {

		load();

		if (serverUrl != null) {
			for (ProviderPriority priority : ProviderPriority.values()) {

				List<CFClientProvider> providers = providersPerPriority.get(priority);
				if (providers != null) {
					for (CFClientProvider prvd : providers) {
						if (prvd.supports(serverUrl, info)) {
							return prvd;
						}
					}
				}
			}
		}
		return null;
	}
}
