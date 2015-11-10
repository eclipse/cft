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
package org.eclipse.cft.server.core.internal.debug;

import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;

public class DebugProviderRegistry {

	private static final CloudFoundryDebugProvider[] providers = getProviders();

	protected static CloudFoundryDebugProvider getDebugProvider(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) {

		for (CloudFoundryDebugProvider provider : providers) {
			if (provider.isDebugSupported(appModule, cloudServer)) {
				return provider;
			}
		}
		return null;
	}

	public static CloudFoundryDebugProvider getProvider(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) {
		CloudFoundryDebugProvider provider = DebugProviderRegistry.getDebugProvider(appModule, cloudServer);
		return provider;
	}

	/**
	 * Obtains a provider for the given server and app module. This handles
	 * error conditions when fetching a provider.
	 * <p/>
	 * Use this method to fetch provider if non-null provider is expected and
	 * error is thrown accordingly if provider cannot be found when it is
	 * expected.
	 * @param appModule
	 * @param cloudServer
	 * @return non-null provider.
	 * @throws CoreException if failed to obtain existing provider
	 */
	public static CloudFoundryDebugProvider getExistingProvider(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) throws CoreException {
		CloudFoundryDebugProvider provider = DebugProviderRegistry.getProvider(appModule, cloudServer);

		if (provider == null) {
			throw CloudErrorUtil.toCoreException(NLS.bind(
					"Unable to find a debug provider for {0}. Please check if debug is supported for the given application in {1} ", //$NON-NLS-1$
					appModule.getDeployedApplicationName(), cloudServer.getServer().getId()));
		}
		return provider;
	}

	private static CloudFoundryDebugProvider[] getProviders() {
		return new CloudFoundryDebugProvider[] { new NgrokDebugProvider(), new SshDebugProvider() };
	}
}
