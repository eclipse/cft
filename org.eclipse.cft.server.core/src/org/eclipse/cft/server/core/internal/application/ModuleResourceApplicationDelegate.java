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

import java.util.Arrays;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.eclipse.cft.server.core.AbstractApplicationDelegate;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * 
 * Contains a default implementation for generating a resources archive of all
 * the resources that are to be pushed to the Cloud Foundry server.
 * 
 */
public abstract class ModuleResourceApplicationDelegate extends AbstractApplicationDelegate {

	public ModuleResourceApplicationDelegate() {

	}

	public boolean providesApplicationArchive(IModule module) {
		return true;
	}


	
	/**
	 * NOTE: For INTERNAL use only. API may change. Framework adopters should not override or invoke.
	 * @param appModule
	 * @return true if default URL should be set. False otherwise
	 */
	public boolean shouldSetDefaultUrl(CloudFoundryApplicationModule appModule) {
		return requiresURL();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cft.server.core.internal.application.
	 * AbstractApplicationDelegate
	 * #getApplicationArchive(org.eclipse.cft.internal
	 * .server.core.client.CloudFoundryApplicationModule,
	 * org.eclipse.wst.server.core.model.IModuleResource[])
	 */
	public ApplicationArchive getApplicationArchive(CloudFoundryApplicationModule module,
			IModuleResource[] moduleResources) throws CoreException {
		return new ModuleResourceApplicationArchive(module.getLocalModule(), Arrays.asList(moduleResources));
	}
}
