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
package org.eclipse.cft.server.standalone.ui.internal.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

import org.eclipse.cft.server.core.CFApplicationArchive;
import org.eclipse.cft.server.core.internal.CFConsoleHandler;
import org.eclipse.cft.server.core.internal.CloudFoundryProjectUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.application.ICloudFoundryArchiver;
import org.eclipse.cft.server.core.internal.application.ZipArchive;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.standalone.core.internal.application.DeploymentErrorHandler;
import org.eclipse.cft.server.standalone.core.internal.application.StandaloneConsole;
import org.eclipse.cft.server.standalone.ui.internal.Messages;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * Generates a Cloud Foundry client archive represent the Java application that
 * should be pushed to a Cloud Foundry server.
 * <p/>
 * Handles Spring boot application repackaging via Spring Boot loader tools
 * <p/>
 * Also supports packaged apps pointed to by the "path" property in an
 * application's manifest.yml
 * 
 */
public class JavaCloudFoundryArchiver implements ICloudFoundryArchiver {

	private static final String META_FOLDER_NAME = "META-INF"; //$NON-NLS-1$

	private static final String MANIFEST_FILE = "MANIFEST.MF"; //$NON-NLS-1$

	protected CFConsoleHandler getConsole() {
		return StandaloneConsole.getDefault();
	}

	protected DeploymentErrorHandler getErrorHandler(IModule actualModule, CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, CFConsoleHandler consoleHandler) {
		return new DeploymentErrorHandler(actualModule, appModule, cloudServer, consoleHandler);
	}

	protected JarArchivingUIHandler getArchivingHandler(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, CFConsoleHandler console, DeploymentErrorHandler errorHandler) {
		return new JarArchivingUIHandler(appModule, cloudServer, console, errorHandler);
	}

	public CFApplicationArchive getApplicationArchive(IModule module, IServer server, IModuleResource[] resources,
			IProgressMonitor monitor) throws CoreException {
		CloudFoundryServer cloudServer = CloudServerUtil.getCloudServer(server);
		Assert.isNotNull(module,
				"Unable to package standalone application. No WTP module found for application. Refresh server and try again."); //$NON-NLS-1$

		CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);

		Assert.isNotNull(appModule,
				"Unable to package standalone application. No cloud application module found. Refresh server and try again."); //$NON-NLS-1$

		// Bug 495814: Maven projects may go out of synch with filesystem,
		// especially if they are built
		// outside of Eclipse. This may result in missing dependencies and
		// resources in the packaged jar
		refreshProject(module, appModule, cloudServer, monitor);

		IProject project = getProject(appModule);
		String projectName = project != null ? project.getName() : "UNKNOWN PROJECT"; //$NON-NLS-1$

		CFApplicationArchive archive = null;

		CFConsoleHandler console = getConsole();
		DeploymentErrorHandler errorHandler = getErrorHandler(module, appModule, cloudServer, console);
		JarArchivingUIHandler archivingHandler = getArchivingHandler(appModule, cloudServer, console, errorHandler);

		File packagedFile = null;

		IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(appModule);

		if (javaProject == null) {
			errorHandler
					.handleApplicationDeploymentFailure(Messages.JavaCloudFoundryArchiver_ERROR_NO_JAVA_PROJ_RESOLVED);
		}

		JavaPackageFragmentRootHandler rootResolver = archivingHandler.getPackageFragmentRootHandler(javaProject,
				monitor);

		IType mainType = rootResolver.getMainType(monitor);

		if (mainType != null) {
			console.printToConsole(module, cloudServer,
					NLS.bind(Messages.JavaCloudFoundryArchiver_PACKAGING_MAIN_TYPE, mainType.getFullyQualifiedName()));
		}

		final IPackageFragmentRoot[] roots = rootResolver.getPackageFragmentRoots(monitor);

		if (roots == null || roots.length == 0) {
			errorHandler
					.handleApplicationDeploymentFailure(Messages.JavaCloudFoundryArchiver_ERROR_NO_PACKAGE_FRAG_ROOTS);
		}

		JarPackageData jarPackageData = archivingHandler.getJarPackageData(roots, mainType, monitor);

		boolean isBoot = CloudFoundryProjectUtil.isSpringBoot(appModule);

		// Search for existing MANIFEST.MF
		IFile metaFile = getManifest(roots, javaProject);

		// Only use existing manifest files for non-Spring boot, as Spring
		// boot repackager will
		// generate it own manifest file.
		if (!isBoot && metaFile != null) {
			// If it is not a boot project, use a standard library jar
			// builder
			jarPackageData.setJarBuilder(archivingHandler.getDefaultLibJarBuilder());

			jarPackageData.setManifestLocation(metaFile.getFullPath());
			jarPackageData.setSaveManifest(false);
			jarPackageData.setGenerateManifest(false);
			// Check manifest accessibility through the jar package data
			// API
			// to verify the packaging won't fail
			if (!jarPackageData.isManifestAccessible()) {
				errorHandler.handleApplicationDeploymentFailure(
						NLS.bind(Messages.JavaCloudFoundryArchiver_ERROR_MANIFEST_NOT_ACCESSIBLE,
								metaFile.getLocation().toString()));
			}

			InputStream inputStream = null;
			try {

				inputStream = new FileInputStream(metaFile.getLocation().toFile());
				Manifest manifest = new Manifest(inputStream);
				Attributes att = manifest.getMainAttributes();
				if (att.getValue("Main-Class") == null) { //$NON-NLS-1$
					errorHandler.handleApplicationDeploymentFailure(
							Messages.JavaCloudFoundryArchiver_ERROR_NO_MAIN_CLASS_IN_MANIFEST);
				}
			} catch (FileNotFoundException e) {
				errorHandler.handleApplicationDeploymentFailure(NLS
						.bind(Messages.JavaCloudFoundryArchiver_ERROR_FAILED_READ_MANIFEST, e.getLocalizedMessage()));

			} catch (IOException e) {
				errorHandler.handleApplicationDeploymentFailure(NLS
						.bind(Messages.JavaCloudFoundryArchiver_ERROR_FAILED_READ_MANIFEST, e.getLocalizedMessage()));

			} finally {

				if (inputStream != null) {
					try {
						inputStream.close();

					} catch (IOException io) {
						// Ignore
					}
				}
			}

		} else {
			// Otherwise generate a manifest file. Note that manifest files
			// are only generated in the temporary jar meant only for
			// deployment.
			// The associated Java project is no modified.
			jarPackageData.setGenerateManifest(true);

			// This ensures that folders in output folders appear at root
			// level
			// Example: src/main/resources, which is in the project's
			// classpath, contains non-Java templates folder and
			// has output folder target/classes. If not exporting output
			// folder,
			// templates will be packaged in the jar using this path:
			// resources/templates
			// This may cause problems with the application's dependencies
			// if they are looking for just /templates at top level of the
			// jar
			// If exporting output folders, templates folder will be
			// packaged at top level in the jar.
			jarPackageData.setExportOutputFolders(true);
		}

		try {
			console.printToConsole(module, cloudServer,
					NLS.bind(Messages.JavaCloudFoundryArchiver_PACKAGING_APPLICATION, projectName));

			packagedFile = archivingHandler.packageApplication(jarPackageData, monitor);
		} catch (CoreException e) {
			errorHandler.handleApplicationDeploymentFailure(
					NLS.bind(Messages.JavaCloudFoundryArchiver_ERROR_JAVA_APP_PACKAGE, e.getMessage()));
		}

		if (packagedFile == null || !packagedFile.exists()) {
			errorHandler.handleApplicationDeploymentFailure(
					Messages.JavaCloudFoundryArchiver_ERROR_NO_PACKAGED_FILE_CREATED);
		} else {
			console.printToConsole(module, cloudServer,
					NLS.bind(Messages.JavaCloudFoundryArchiver_PACKAGING_APPLICATION_COMPLETED, projectName,
							packagedFile.getAbsolutePath()));
		}

		if (isBoot) {
			console.printToConsole(module, cloudServer, Messages.JavaCloudFoundryArchiver_REPACKAGING_SPRING_BOOT_APP);

			archivingHandler.bootRepackage(roots, packagedFile);
		}

		// At this stage a packaged file should have been created or found
		try {
			archive = new ZipArchive(new ZipFile(packagedFile));
		} catch (IOException ioe) {
			errorHandler.handleApplicationDeploymentFailure(
					NLS.bind(Messages.JavaCloudFoundryArchiver_ERROR_CREATE_CF_ARCHIVE, ioe.getMessage()));
		}

		return archive;
	}

	protected IProject getProject(CloudFoundryApplicationModule appModule) {
		return CloudFoundryProjectUtil.getProject(appModule);
	}

	/**
	 * 
	 * @param resource
	 *            that may contain a META-INF folder
	 * @return META-INF folder, if found. Null otherwise
	 * @throws CoreException
	 */
	protected IFolder getMetaFolder(IResource resource) throws CoreException {
		if (!(resource instanceof IContainer)) {
			return null;
		}
		IContainer folder = (IContainer) resource;
		// Only look for META-INF folder at top-level in the given container.
		IResource[] members = folder.members();
		if (members != null) {
			for (IResource mem : members) {
				if (META_FOLDER_NAME.equals(mem.getName()) && mem instanceof IFolder) {
					return (IFolder) mem;
				}
			}
		}
		return null;
	}

	protected IFile getManifest(IPackageFragmentRoot[] roots, IJavaProject javaProject) throws CoreException {

		IFolder metaFolder = null;
		for (IPackageFragmentRoot root : roots) {
			if (!root.isArchive() && !root.isExternal()) {
				IResource resource = root.getResource();
				metaFolder = getMetaFolder(resource);
				if (metaFolder != null) {
					break;
				}
			}
		}

		// Otherwise look for manifest file in the java project:
		if (metaFolder == null) {
			metaFolder = getMetaFolder(javaProject.getProject());
		}

		if (metaFolder != null) {
			IResource[] members = metaFolder.members();
			if (members != null) {
				for (IResource mem : members) {
					if (MANIFEST_FILE.equals(mem.getName().toUpperCase()) && mem instanceof IFile) {
						return (IFile) mem;
					}
				}
			}
		}

		return null;

	}

	protected void refreshProject(IModule module, CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, IProgressMonitor monitor) throws CoreException {

		IProject project = getProject(appModule);
		if (project != null && project.isAccessible()) {
			getConsole().printToConsole(module, cloudServer,
					NLS.bind(Messages.JavaCloudFoundryArchiver_REFRESHING_PROJECT, project.getName()));
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
	}
}