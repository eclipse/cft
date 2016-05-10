/*******************************************************************************
 * Copyright (c) 2012, 2016 Pivotal Software, Inc. and others
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
 *     IBM - Turning into base WAR packaging provider.
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.application;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.CFApplicationArchive;
import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudApplicationURL;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryProjectUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudUtil;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * Java Web applications are the standard type of applications supported on
 * Cloud Foundry. They include Spring, Lift and Java Web.
 * <p/>
 * This application delegate supports the above Java Web frameworks.
 */
public class JavaWebApplicationDelegate extends ApplicationDelegate {

	public JavaWebApplicationDelegate() {

	}

	public boolean requiresURL() {
		// All Java Web applications require a URL when pushed to a CF server
		return true;
	}

	public boolean providesApplicationArchive(IModule module) {
		// Returns a default WAR archive package
		return true;
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

		CloudFoundryApplicationModule appModule = getCloudFoundryApplicationModule(module, server);
		CloudFoundryServer cloudServer = getCloudServer(server);
		CFApplicationArchive manifestArchive = getArchiveFromManifest(appModule, cloudServer);
		if (manifestArchive != null) {
			return manifestArchive;
		}
		try {
			File warFile = CloudUtil.createWarFile(new IModule[] { appModule.getLocalModule() },
					(Server) cloudServer.getServer(), monitor);

			CloudFoundryPlugin.trace("War file " + warFile.getName() + " created"); //$NON-NLS-1$ //$NON-NLS-2$

			return new ZipArchive(new ZipFile(warFile));
		}
		catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
					"Failed to create war file. " + //$NON-NLS-1$
							"\nApplication: " + appModule.getApplication().getName() + //$NON-NLS-1$
							"\nModule: " + appModule.getName() + //$NON-NLS-1$
							"\nException: " + e.getMessage(), //$NON-NLS-1$
					e));
		}
	}

	@Override
	public IStatus validateDeploymentInfo(ApplicationDeploymentInfo deploymentInfo) {

		IStatus status = super.validateDeploymentInfo(deploymentInfo);
		if (status.isOK() && ((deploymentInfo.getUris() == null || deploymentInfo.getUris().isEmpty()))) {
			String errorMessage = Messages.JavaWebApplicationDelegate_ERROR_NO_MAPPED_APP_URL;
			status = CloudFoundryPlugin.getErrorStatus(errorMessage);
		}

		return status;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.cft.server.core.internal.application.ApplicationDelegate#
	 * getDefaultApplicationDeploymentInfo(org.eclipse.wst.server.core.IModule,
	 * org.eclipse.cft.server.core.internal.CloudFoundryServer,
	 * org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public ApplicationDeploymentInfo getDefaultApplicationDeploymentInfo(IModule module, IServer server,
			IProgressMonitor monitor) throws CoreException {
		ApplicationDeploymentInfo info = super.getDefaultApplicationDeploymentInfo(module, server, monitor);

		// Set a default URL for the application.
		if ((info.getUris() == null || info.getUris().isEmpty()) && info.getDeploymentName() != null) {

			CloudApplicationURL url = ApplicationUrlLookupService.update(getCloudServer(server), monitor)
					.getDefaultApplicationURL(info.getDeploymentName());
			info.setUris(Arrays.asList(url.getUrl()));
		}
		return info;
	}

	public static CFApplicationArchive getArchiveFromManifest(CloudFoundryApplicationModule appModule,
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
