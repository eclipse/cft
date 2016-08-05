/*******************************************************************************
 * Copyright (c) 2015, 2016 Pivotal Software, Inc. 
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cft.server.core.AbstractDebugProvider;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;

public class DebugProviderRegistry {
	
	private static final DebugProviderExtension[] providerExtensions = getExtensionProviders();
	private static final AbstractDebugProvider[] defaultProviders = getDefaultProviders();

	protected static AbstractDebugProvider getDebugProvider(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) {

		for (DebugProviderExtension provider : providerExtensions) {
			if (provider.isDebugSupported(cloudServer, appModule))
				return provider.getInstance();
		}
		
		for (AbstractDebugProvider provider : defaultProviders) {
			if (provider.isDebugSupported(appModule, cloudServer)) {
				return provider;
			}
		}
		return null;
	}

	public static AbstractDebugProvider getProvider(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) {
		AbstractDebugProvider provider = DebugProviderRegistry.getDebugProvider(appModule, cloudServer);
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
	public static AbstractDebugProvider getExistingProvider(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) throws CoreException {
		AbstractDebugProvider provider = DebugProviderRegistry.getProvider(appModule, cloudServer);

		if (provider == null) {
			throw CloudErrorUtil.toCoreException(NLS.bind(
					"Unable to find a debug provider for {0}. Please check if debug is supported for the given application in {1} ", //$NON-NLS-1$
					appModule.getDeployedApplicationName(), cloudServer.getServer().getId()));
		}
		return provider;
	}

	private static AbstractDebugProvider[] getDefaultProviders() {
		return new CloudFoundryDebugProvider[] { new NgrokDebugProvider(), new SshDebugProvider() };
	}
	
	private static DebugProviderExtension[] getExtensionProviders() {
		List<DebugProviderExtension> providers = new ArrayList<DebugProviderExtension>();
		IConfigurationElement[] configElements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.cft.server.core.debugProvider");
		for (IConfigurationElement elem : configElements) {
			providers.add(new DebugProviderExtension(elem));
		}
		return providers.toArray(new DebugProviderExtension[providers.size()]);
	}
	
	private static class DebugProviderExtension {
		
		private static final String DEBUG_PROVIDER_SERVER_ID_ATTR = "serverTypeId";
		private static final String DEBUG_PROVIDER_CLASS_ATTR = "class";
		private static final String DEBUG_PROVIDER_MODULE_ELEM = "module";
		private static final String DEBUG_PROVIDER_TYPE_ATTR = "type";
		
		IConfigurationElement elem;
		String serverTypeId = null;
		Set<String> moduleTypes = null;
		AbstractDebugProvider instance = null;
		
		public DebugProviderExtension(IConfigurationElement elem) {
			this.elem = elem;
		}
		
		public boolean isDebugSupported(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule) {
			String id = cloudServer.getServer().getServerType().getId();
			if (id != null && id.equals(getServerTypeId())) {
				String moduleType = appModule.getLocalModule().getModuleType().getId();
				if (moduleType != null && getModuleTypes().contains(moduleType)) {
					return getInstance().isDebugSupported(appModule, cloudServer);
				}
			}
			return false;
		}
		
		public AbstractDebugProvider getInstance() {
			if (instance == null) {
				try {
					instance = (AbstractDebugProvider) elem.createExecutableExtension(DEBUG_PROVIDER_CLASS_ATTR);
				} catch (Exception e) {
					CloudFoundryPlugin.logWarning(e.getMessage());
				}
			}
			return instance;
		}
		
		private String getServerTypeId() {
			if (serverTypeId == null) {
				serverTypeId = elem.getAttribute(DEBUG_PROVIDER_SERVER_ID_ATTR);
			}
			return serverTypeId;
		}
		
		private Set<String> getModuleTypes() {
			if (moduleTypes == null) {
				moduleTypes = new HashSet<String>();
				IConfigurationElement[] modules = elem.getChildren(DEBUG_PROVIDER_MODULE_ELEM);
				for (IConfigurationElement module : modules) {
					String type = module.getAttribute(DEBUG_PROVIDER_TYPE_ATTR);
					moduleTypes.add(type);
				}
			}
			return moduleTypes;
		}
	}
}
