/*******************************************************************************
 * Copyright (c) 2013, 2016 Pivotal Software, Inc. and others
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

import org.eclipse.cft.server.core.CFApplicationArchive;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * 
 * Contains a default implementation for generating a resources archive of all
 * the resources that are to be pushed to the Cloud Foundry server.
 * 
 */
public abstract class ModuleResourceApplicationDelegate extends ApplicationDelegate {

	public ModuleResourceApplicationDelegate() {

	}

	public boolean providesApplicationArchive(IModule module) {
		return true;
	}

	/**
	 * NOTE: For INTERNAL use only. API may change. Framework adopters should
	 * not override or invoke.
	 * @param appModule
	 * @return true if default URL should be set. False otherwise
	 */
	public boolean shouldSetDefaultUrl(CloudFoundryApplicationModule appModule) {
		return requiresURL();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cft.server.core.AbstractApplicationDelegate#
	 * getApplicationArchive(org.eclipse.wst.server.core.IModule,
	 * org.eclipse.wst.server.core.IServer,
	 * org.eclipse.wst.server.core.model.IModuleResource[],
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public CFApplicationArchive getApplicationArchive(IModule module, IServer server, IModuleResource[] moduleResources,
			IProgressMonitor monitor) throws CoreException {
		return new ModuleResourceApplicationArchive(module, Arrays.asList(moduleResources));
	}
}
