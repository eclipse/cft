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
import java.io.IOException;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.cft.server.core.CFApplicationArchive;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryProjectUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * Reads an application archiving (e.g. jar or war file) from the "path"
 * property in an application's manifest.yml, if it exists in the application's
 * project.
 */
public class ManifestApplicationArchiver implements ICloudFoundryArchiver {

	@Override
	public CFApplicationArchive getApplicationArchive(IModule module, IServer server, IModuleResource[] resources,
			IProgressMonitor monitor) throws CoreException {
		CloudFoundryServer cloudServer = CloudServerUtil.getCloudServer(server);
		CloudFoundryApplicationModule appModule = CloudServerUtil.getCloudFoundryApplicationModule(module, server);
		return getArchiveFromManifest(appModule, cloudServer);
	}

	public CFApplicationArchive getArchiveFromManifest(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) throws CoreException {
		String archivePath = null;
		ManifestParser parser = new ManifestParser(appModule, cloudServer);
		// Read the path again instead of deployment info, as a user may be
		// correcting the path after the module was creating and simply
		// attempting to push it again without the
		// deployment wizard
		if (parser.hasManifest()) {
			archivePath = parser.getApplicationProperty(null, ManifestParser.PATH_PROP);
		}

		File packagedFile = null;
		if (archivePath != null) {
			// Only support paths that point to archive files
			IPath path = new Path(archivePath);
			if (path.getFileExtension() != null) {
				// Check if it is project relative first
				IFile projectRelativeFile = null;
				IProject project = CloudFoundryProjectUtil.getProject(appModule);

				if (project != null) {
					projectRelativeFile = project.getFile(archivePath);
				}

				if (projectRelativeFile != null && projectRelativeFile.exists()) {
					packagedFile = projectRelativeFile.getLocation().toFile();
				}
				else {
					// See if it is an absolute path
					File absoluteFile = new File(archivePath);
					if (absoluteFile.exists() && absoluteFile.canRead()) {
						packagedFile = absoluteFile;
					}
				}
			}
			// If a path is specified but no file found stop further deployment
			if (packagedFile == null) {
				String message = NLS.bind(Messages.JavaWebApplicationDelegate_ERROR_FILE_NOT_FOUND_MANIFEST_YML,
						archivePath);
				throw CloudErrorUtil.toCoreException(message);
			}
			else {
				try {
					return new ZipArchive(new ZipFile(packagedFile));
				}
				catch (ZipException e) {
					throw CloudErrorUtil.toCoreException(e);
				}
				catch (IOException e) {
					throw CloudErrorUtil.toCoreException(e);
				}
			}
		}
		return null;
	}
}
