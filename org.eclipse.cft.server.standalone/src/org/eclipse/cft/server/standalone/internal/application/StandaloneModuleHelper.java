/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.standalone.internal.application;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.wst.server.core.IModule;

/**
 * Determines if a WST IModule corresponds to a Java standalone application.
 */
public class StandaloneModuleHelper {

	private final CloudFoundryApplicationModule appModule;

	private final IModule module;

	public StandaloneModuleHelper(CloudFoundryApplicationModule appModule) {
		this.appModule = appModule;
		this.module = appModule.getLocalModule();
	}

	public StandaloneModuleHelper(IModule module) {
		this.appModule = null;
		this.module = module;
	}

	public boolean isSupportedStandalone() {
		if (appModule == null && module == null) {
			return false;
		}

		boolean isStandalone = module != null
				&& StandaloneFacetHandler.ID_MODULE_STANDALONE.equals(module
						.getModuleType().getId());

		return isStandalone;
	}

	public Staging getStaging() {
		if (appModule == null) {
			return null;
		}
		Staging staging = appModule.getDeploymentInfo() != null  ? appModule.getDeploymentInfo().getStaging() : null;
		if (staging == null) {
			CloudApplication cloudApp = appModule.getApplication();
			if (cloudApp != null) {
				staging = cloudApp.getStaging();
			}
		}
		return staging;
	}

}
