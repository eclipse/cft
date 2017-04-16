/*******************************************************************************
 * Copyright (c) 2016 Pivotal Software, Inc. and others
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

import java.io.File;
import java.util.zip.ZipFile;

import org.eclipse.cft.server.core.CFApplicationArchive;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.CloudUtil;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.model.IModuleResource;

public class WarApplicationArchiver implements ICloudFoundryArchiver {

	@Override
	public CFApplicationArchive getApplicationArchive(IModule module, IServer server, IModuleResource[] resources,
			IProgressMonitor monitor) throws CoreException {
		CloudFoundryApplicationModule appModule = CloudServerUtil.getExistingCloudModule(module, server);

		try {
			if (server instanceof Server) {
				File warFile = CloudUtil.createWarFile(new IModule[] { module }, (Server) server, monitor);

				CloudFoundryPlugin.trace("War file " + warFile.getName() + " created"); //$NON-NLS-1$ //$NON-NLS-2$

				return new ZipArchive(new ZipFile(warFile));
			}
			else {
				throw CloudErrorUtil.toCoreException("Expected server: " + server.getId() + " to be of type: "
						+ Server.class.getName() + ". Unable to generate WAR file and deploy the application to the selected server.");
			}
		}
		catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
					"Failed to create war file. " + //$NON-NLS-1$
							"\nApplication: " + appModule.getDeployedApplicationName() + //$NON-NLS-1$
							"\nModule: " + module.getName() + //$NON-NLS-1$
							"\nException: " + e.getMessage(), //$NON-NLS-1$
					e));
		}
	}

}
