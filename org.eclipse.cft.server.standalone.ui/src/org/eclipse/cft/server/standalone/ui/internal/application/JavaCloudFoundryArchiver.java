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
import java.lang.reflect.InvocationTargetException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

import org.eclipse.cft.server.core.CFApplicationArchive;
import org.eclipse.cft.server.core.internal.CFConsoleHandler;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryProjectUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.application.JavaWebApplicationDelegate;
import org.eclipse.cft.server.core.internal.application.ZipArchive;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.standalone.core.internal.application.ICloudFoundryArchiver;
import org.eclipse.cft.server.standalone.core.internal.application.StandaloneConsole;
import org.eclipse.cft.server.standalone.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.CFUiUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarRsrcUrlBuilder;
import org.eclipse.jdt.ui.jarpackager.IJarBuilder;
import org.eclipse.jdt.ui.jarpackager.IJarExportRunnable;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.springframework.boot.loader.tools.Libraries;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCallback;
import org.springframework.boot.loader.tools.LibraryScope;
import org.springframework.boot.loader.tools.Repackager;

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

	private CloudFoundryApplicationModule appModule;

	private IModule actualModule;

	private CloudFoundryServer cloudServer;

	private boolean initialized = false;

	private static final String META_FOLDER_NAME = "META-INF"; //$NON-NLS-1$

	private static final String MANIFEST_FILE = "MANIFEST.MF"; //$NON-NLS-1$

	public void initialize(IModule module, IServer server) throws CoreException {
		this.cloudServer = CloudServerUtil.getCloudServer(server);
		this.actualModule = module;
		Assert.isNotNull(module,
				"Unable to package standalone application. No WTP module found for application. Refresh server and try again."); //$NON-NLS-1$

		this.appModule = cloudServer.getExistingCloudModule(module);

		Assert.isNotNull(appModule,
				"Unable to package standalone application. No cloud application module found. Refresh server and try again."); //$NON-NLS-1$
		
		// Need to know whether initalized or not to mimic the earlier behavior
		// where the it was
		// initialized within the constructor. Now it is being created from an
		// extension point.
		initialized = true;
	}

	protected CFConsoleHandler getConsole() {
		return StandaloneConsole.getDefault();
	}

	public CFApplicationArchive getApplicationArchive(IProgressMonitor monitor) throws CoreException {

		if (!initialized) {
			// Seems like initialize() wasn't invoked prior to this call
			throw CloudErrorUtil.toCoreException(Messages.JavaCloudFoundryArchiver_ERROR_ARCHIVER_NOT_INITIALIZED);
		}

		// Bug 495814: Maven projects may go out of synch with filesystem,
		// especially if they are built
		// outside of Eclipse. This may result in missing dependencies and
		// resources in the packaged jar
		refreshProject(monitor);

		IProject project = getProject();
		String projectName = project != null ? project.getName() : "UNKNOWN PROJECT"; //$NON-NLS-1$

		CFApplicationArchive archive = JavaWebApplicationDelegate.getArchiveFromManifest(appModule, cloudServer);

		if (archive != null) {
			getConsole().printToConsole(actualModule, cloudServer,
					NLS.bind(Messages.JavaCloudFoundryArchiver_FOUND_ARCHIVE_FROM_MANIFEST, archive.getName()));
		} else {

			File packagedFile = null;

			IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(appModule);

			if (javaProject == null) {
				handleApplicationDeploymentFailure(Messages.JavaCloudFoundryArchiver_ERROR_NO_JAVA_PROJ_RESOLVED);
			}

			JavaPackageFragmentRootHandler rootResolver = getPackageFragmentRootHandler(javaProject, monitor);

			IType mainType = rootResolver.getMainType(monitor);

			if (mainType != null) {
				getConsole().printToConsole(actualModule, cloudServer, NLS
						.bind(Messages.JavaCloudFoundryArchiver_PACKAGING_MAIN_TYPE, mainType.getFullyQualifiedName()));
			}

			final IPackageFragmentRoot[] roots = rootResolver.getPackageFragmentRoots(monitor);

			if (roots == null || roots.length == 0) {
				handleApplicationDeploymentFailure(Messages.JavaCloudFoundryArchiver_ERROR_NO_PACKAGE_FRAG_ROOTS);
			}

			JarPackageData jarPackageData = getJarPackageData(roots, mainType, monitor);

			boolean isBoot = CloudFoundryProjectUtil.isSpringBoot(appModule);

			// Search for existing MANIFEST.MF
			IFile metaFile = getManifest(roots, javaProject);

			// Only use existing manifest files for non-Spring boot, as Spring
			// boot repackager will
			// generate it own manifest file.
			if (!isBoot && metaFile != null) {
				// If it is not a boot project, use a standard library jar
				// builder
				jarPackageData.setJarBuilder(getDefaultLibJarBuilder());

				jarPackageData.setManifestLocation(metaFile.getFullPath());
				jarPackageData.setSaveManifest(false);
				jarPackageData.setGenerateManifest(false);
				// Check manifest accessibility through the jar package data
				// API
				// to verify the packaging won't fail
				if (!jarPackageData.isManifestAccessible()) {
					handleApplicationDeploymentFailure(
							NLS.bind(Messages.JavaCloudFoundryArchiver_ERROR_MANIFEST_NOT_ACCESSIBLE,
									metaFile.getLocation().toString()));
				}

				InputStream inputStream = null;
				try {

					inputStream = new FileInputStream(metaFile.getLocation().toFile());
					Manifest manifest = new Manifest(inputStream);
					Attributes att = manifest.getMainAttributes();
					if (att.getValue("Main-Class") == null) { //$NON-NLS-1$
						handleApplicationDeploymentFailure(
								Messages.JavaCloudFoundryArchiver_ERROR_NO_MAIN_CLASS_IN_MANIFEST);
					}
				} catch (FileNotFoundException e) {
					handleApplicationDeploymentFailure(NLS.bind(
							Messages.JavaCloudFoundryArchiver_ERROR_FAILED_READ_MANIFEST, e.getLocalizedMessage()));

				} catch (IOException e) {
					handleApplicationDeploymentFailure(NLS.bind(
							Messages.JavaCloudFoundryArchiver_ERROR_FAILED_READ_MANIFEST, e.getLocalizedMessage()));

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
				getConsole().printToConsole(actualModule, cloudServer,
						NLS.bind(Messages.JavaCloudFoundryArchiver_PACKAGING_APPLICATION, projectName));

				packagedFile = packageApplication(jarPackageData, monitor);
			} catch (CoreException e) {
				handleApplicationDeploymentFailure(
						NLS.bind(Messages.JavaCloudFoundryArchiver_ERROR_JAVA_APP_PACKAGE, e.getMessage()));
			}

			if (packagedFile == null || !packagedFile.exists()) {
				handleApplicationDeploymentFailure(Messages.JavaCloudFoundryArchiver_ERROR_NO_PACKAGED_FILE_CREATED);
			} else {
				getConsole().printToConsole(actualModule, cloudServer,
						NLS.bind(Messages.JavaCloudFoundryArchiver_PACKAGING_APPLICATION_COMPLETED, projectName,
								packagedFile.getAbsolutePath()));
			}

			if (isBoot) {
				getConsole().printToConsole(actualModule, cloudServer,
						Messages.JavaCloudFoundryArchiver_REPACKAGING_SPRING_BOOT_APP);

				bootRepackage(roots, packagedFile);
			}

			// At this stage a packaged file should have been created or found
			try {
				archive = new ZipArchive(new ZipFile(packagedFile));
			} catch (IOException ioe) {
				handleApplicationDeploymentFailure(
						NLS.bind(Messages.JavaCloudFoundryArchiver_ERROR_CREATE_CF_ARCHIVE, ioe.getMessage()));
			}
		}

		return archive;
	}

	protected IProject getProject() {
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

	protected void refreshProject(IProgressMonitor monitor) throws CoreException {

		IProject project = getProject();
		if (project != null && project.isAccessible()) {
			getConsole().printToConsole(actualModule, cloudServer,
					NLS.bind(Messages.JavaCloudFoundryArchiver_REFRESHING_PROJECT, project.getName()));
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
	}

	protected IJarBuilder getDefaultLibJarBuilder() {
		return new FatJarRsrcUrlBuilder() {

			public void writeRsrcUrlClasses() throws IOException {
				// Do not unpack and repackage the Eclipse jar loader
			}
		};
	}

	protected JavaPackageFragmentRootHandler getPackageFragmentRootHandler(IJavaProject javaProject,
			IProgressMonitor monitor) throws CoreException {

		return new JavaPackageFragmentRootHandler(javaProject, cloudServer);
	}

	protected void bootRepackage(final IPackageFragmentRoot[] roots, File packagedFile) throws CoreException {
		Repackager bootRepackager = new Repackager(packagedFile);
		try {
			bootRepackager.repackage(new Libraries() {

				public void doWithLibraries(LibraryCallback callBack) throws IOException {
					for (IPackageFragmentRoot root : roots) {

						if (root.isArchive()) {

							File rootFile = new File(root.getPath().toOSString());
							if (rootFile.exists()) {
								callBack.library(new Library(rootFile, LibraryScope.COMPILE));
							}
						}
					}
				}
			});
		} catch (IOException e) {
			handleApplicationDeploymentFailure(
					NLS.bind(Messages.JavaCloudFoundryArchiver_ERROR_REPACKAGE_SPRING, e.getMessage()));
		}
	}

	protected JarPackageData getJarPackageData(IPackageFragmentRoot[] roots, IType mainType, IProgressMonitor monitor)
			throws CoreException {

		String filePath = getTempJarPath(appModule.getLocalModule());

		if (filePath == null) {
			handleApplicationDeploymentFailure();
		}

		IPath location = new Path(filePath);

		// Note that if no jar builder is specified in the package data
		// then a default one is used internally by the data that does NOT
		// package any jar dependencies.
		JarPackageData packageData = new JarPackageData();

		packageData.setJarLocation(location);

		// Don't create a manifest. A repackager should determine if a generated
		// manifest is necessary
		// or use a user-defined manifest.
		packageData.setGenerateManifest(false);

		// Since user manifest is not used, do not save to manifest (save to
		// manifest saves to user defined manifest)
		packageData.setSaveManifest(false);

		packageData.setManifestMainClass(mainType);
		packageData.setElements(roots);
		return packageData;
	}

	protected File packageApplication(final JarPackageData packageData, IProgressMonitor monitor) throws CoreException {

		int progressWork = 10;
		final SubMonitor subProgress = SubMonitor.convert(monitor, progressWork);

		final File[] createdFile = new File[1];

		final CoreException[] error = new CoreException[1];
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				try {

					Shell shell = CFUiUtil.getShell();

					IJarExportRunnable runnable = packageData.createJarExportRunnable(shell);
					try {
						runnable.run(subProgress);

						File file = new File(packageData.getJarLocation().toString());
						if (!file.exists()) {
							handleApplicationDeploymentFailure();
						} else {
							createdFile[0] = file;
						}

					} catch (InvocationTargetException e) {
						throw CloudErrorUtil.toCoreException(e);
					} catch (InterruptedException ie) {
						throw CloudErrorUtil.toCoreException(ie);
					} finally {
						subProgress.done();
					}
				} catch (CoreException e) {
					error[0] = e;
				}
			}

		});
		if (error[0] != null) {
			throw error[0];
		}

		return createdFile[0];
	}

	protected void handleApplicationDeploymentFailure(String errorMessage) throws CoreException {
		if (errorMessage == null) {
			errorMessage = Messages.JavaCloudFoundryArchiver_ERROR_CREATE_PACKAGED_FILE;
		}
		errorMessage = errorMessage + " - " //$NON-NLS-1$
				+ appModule.getDeployedApplicationName() + ". Unable to package application for deployment."; //$NON-NLS-1$
		getConsole().printErrorToConsole(actualModule, cloudServer, errorMessage);
		throw CloudErrorUtil.toCoreException(errorMessage);
	}

	protected void handleApplicationDeploymentFailure() throws CoreException {
		handleApplicationDeploymentFailure(null);
	}

	public static String getTempJarPath(IModule module) throws CoreException {
		try {
			File tempFolder = File.createTempFile("tempFolderForJavaAppJar", //$NON-NLS-1$
					null);
			tempFolder.delete();
			tempFolder.mkdirs();

			if (!tempFolder.exists()) {
				throw CloudErrorUtil.toCoreException(
						NLS.bind(Messages.JavaCloudFoundryArchiver_ERROR_CREATE_TEMP_DIR, tempFolder.getPath()));
			}

			File targetFile = new File(tempFolder, module.getName() + ".jar"); //$NON-NLS-1$
			targetFile.deleteOnExit();

			String path = new Path(targetFile.getAbsolutePath()).toString();

			return path;

		} catch (IOException io) {
			CloudErrorUtil.toCoreException(io);
		}
		return null;
	}
}