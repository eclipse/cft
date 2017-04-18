/*******************************************************************************
 * Copyright (c) 2016, 2017 Pivotal Software, Inc. and others
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
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

public class ApplicationArchiverFactory {

	public ApplicationArchiverFactory() {

	}

	public ICloudFoundryArchiver getWarApplicationArchiver() {
		return new WarApplicationArchiver();
	}

	public ICloudFoundryArchiver getManifestApplicationArchiver() {
		return new ManifestApplicationArchiver();
	}

	/**
	 * True if the given module can be archived using the framework's default
	 * Manifest archiver. False otherwise (e.g. no associated accessible
	 * project, or project for the module has no manifest.yml file).
	 * @param module
	 * @param server
	 * @return
	 */
	public boolean supportsManifestArchiving(IModule module, IServer server) {
		try {
			CloudFoundryApplicationModule appModule = CloudServerUtil.getCloudModule(module, server);
			CloudFoundryServer cloudServer = CloudServerUtil.getCloudServer(server);
			if (appModule != null && cloudServer != null) {
				ManifestParser parser = new ManifestParser(appModule, cloudServer);
				return parser.hasManifest();
			}
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return false;
	}
}
