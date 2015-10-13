/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software Inc and IBM Corporation. 
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
package org.eclipse.cft.server.ui.internal.wizards;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.ui.ICloudFoundryServiceWizardIconProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class CloudFoundryServiceWizardIconProviderRegistry {

	private static final String ELEMENT = "serviceIconProvider"; //$NON-NLS-1$

	public static final String EXTENSION_POINT = "org.eclipse.cft.server.ui.serviceWizardIconProvider"; //$NON-NLS-1$

	// Static variables
	private static CloudFoundryServiceWizardIconProviderRegistry instance = new CloudFoundryServiceWizardIconProviderRegistry();

	public static CloudFoundryServiceWizardIconProviderRegistry getInstance() {
		synchronized (instance) {
			// Load on first access
			if (!instance.isLoaded) {
				instance.load();
			}

		}
		return instance;
	}

	// Member variables

	private Map<String /* server runtime type id */, ICloudFoundryServiceWizardIconProvider> providers = new HashMap<String, ICloudFoundryServiceWizardIconProvider>();

	boolean isLoaded = false;

	public ICloudFoundryServiceWizardIconProvider getIconProvider(String runtimeTypeId) {
		synchronized (this) {
			return providers.get(runtimeTypeId.toLowerCase());
		}
	}

	private void load() {

		isLoaded = true;

		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(EXTENSION_POINT);

		if (extensionPoint == null) {
			CloudFoundryPlugin.logError("Failed to load Cloud Foundry service icon extension providers from: " //$NON-NLS-1$
					+ EXTENSION_POINT);
		}
		else {

			for (IExtension extension : extensionPoint.getExtensions()) {
				for (IConfigurationElement config : extension.getConfigurationElements()) {

					if (ELEMENT.equals(config.getName())) {
						try {
							ICloudFoundryServiceWizardIconProvider provider = (ICloudFoundryServiceWizardIconProvider) config
									.createExecutableExtension("providerClass"); //$NON-NLS-1$

							String runtimeTypeId =config.getAttribute("runtimeTypeId"); //$NON-NLS-1$
							
							if(runtimeTypeId != null) {
								providers.put(runtimeTypeId, provider);
							} else {
								CloudFoundryPlugin.logError("Invalid server icon entry from:"+EXTENSION_POINT); //$NON-NLS-1$
							}

						}
						catch (CoreException e) {
							e.printStackTrace();
						}

					}
				}
			}
		}
	}

}
