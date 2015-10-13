/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal.application;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

/**
 * 
 * Provider that wraps around values and classes that are created from an
 * extension point configuration element
 * 
 * @param <T> Type of delegate that this provider is creating from an extension
 * point definition
 */
public class FrameworkProvider<T> {

	private static final String CLASS_ELEMENT = "class"; //$NON-NLS-1$

	private static final String PROVIDER_ID_ATTRIBUTE = "providerID"; //$NON-NLS-1$

	private T delegate;

	protected final IConfigurationElement configurationElement;

	private String providerID;

	private final String extensionPointID;

	protected FrameworkProvider(IConfigurationElement configurationElement, String extensionPointID) {
		this.configurationElement = configurationElement;
		this.extensionPointID = extensionPointID;
	}

	public String getProviderID() {
		if (providerID == null && configurationElement != null) {
			providerID = configurationElement.getAttribute(PROVIDER_ID_ATTRIBUTE);
		}
		return providerID;
	}

	public T getDelegate() {
		if (delegate == null && configurationElement != null) {
			try {
				Object object = configurationElement.createExecutableExtension(CLASS_ELEMENT);
				if (object == null) {
					CloudFoundryPlugin
							.logError("No delegate class found. Must implement a delegate class. See extension point: " //$NON-NLS-1$
									+ extensionPointID + " for more details."); //$NON-NLS-1$
				}
				else {
					delegate = (T) object;
				}
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
		}
		return delegate;
	}

}
