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
package org.eclipse.cft.server.standalone.ui.internal.application;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.cft.server.core.internal.CFConsoleHandler;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.standalone.core.internal.application.DeploymentErrorHandler;
import org.eclipse.cft.server.standalone.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.CFUiUtil;
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
import org.springframework.boot.loader.tools.Libraries;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCallback;
import org.springframework.boot.loader.tools.LibraryScope;
import org.springframework.boot.loader.tools.Repackager;

class JarArchivingUIHandler {

	private CloudFoundryApplicationModule appModule;
	private CloudFoundryServer cloudServer;
	private CFConsoleHandler consoleHandler;
	private DeploymentErrorHandler errorHandler;

	public JarArchivingUIHandler(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			CFConsoleHandler consoleHandler, DeploymentErrorHandler errorHandler) {
		this.appModule = appModule;
		this.cloudServer = cloudServer;
		this.consoleHandler = consoleHandler;
		this.errorHandler = errorHandler;
	}

	protected String getTempJarPath(IModule module) throws CoreException {
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
			errorHandler.handleApplicationDeploymentFailure(
					NLS.bind(Messages.JavaCloudFoundryArchiver_ERROR_REPACKAGE_SPRING, e.getMessage()));
		}
	}

	protected JarPackageData getJarPackageData(IPackageFragmentRoot[] roots, IType mainType, IProgressMonitor monitor)
			throws CoreException {

		String filePath = getTempJarPath(appModule.getLocalModule());

		if (filePath == null) {
			errorHandler.handleApplicationDeploymentFailure("Failed to create temporary JAR file");
		}

		IPath location = new Path(filePath);

		// Note that if no jar builder is specified in the package data
		// then a default one is used internally by the data that does NOT
		// package any jar dependencies.
		JarPackageData packageData = new JarPackageData();

		packageData.setJarLocation(location);

		// Don't create a manifest. A repackager should determine if a
		// generated
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
							errorHandler.handleApplicationDeploymentFailure();
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
}