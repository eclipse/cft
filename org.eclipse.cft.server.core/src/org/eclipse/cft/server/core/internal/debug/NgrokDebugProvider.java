/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal.debug;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryProjectUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.application.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.client.AbstractWaitWithProgressJob;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Performs a connection to a given server and module. Handles network timeouts,
 * including retrying if connections failed.
 */
public class NgrokDebugProvider extends CloudFoundryDebugProvider {

	public static final String JAVA_OPTS = "JAVA_OPTS"; //$NON-NLS-1$

	/**
	 * @return non-null file content
	 * @throws CoreException if error occurred, or file not found
	 */
	public static String getFileContent(final CloudFoundryApplicationModule appModule,
			final CloudFoundryServer cloudServer, final String outputFilePath, IProgressMonitor monitor)
					throws CoreException, OperationCanceledException {

		final int attempts = 100;
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100 * attempts);
		String content = null;
		CoreException error = null;
		try {
			content = new AbstractWaitWithProgressJob<String>(attempts, 3000, true) {

				protected String runInWait(IProgressMonitor monitor) throws CoreException {
					if (monitor.isCanceled()) {
						return null;
					}
					SubMonitor subMonitor = SubMonitor.convert(monitor);
					CloudApplication app = null;
					try {
						app = cloudServer.getBehaviour().getCloudApplication(appModule.getDeployedApplicationName(),
								subMonitor.newChild(50));
					}
					catch (CoreException e) {
						// Handle app errors separately
						CloudFoundryPlugin.logError(e);
					}

					// Stop checking for the file if the application no longer
					// exists or is not running
					if (app != null && app.getState() == AppState.STARTED) {
						return cloudServer.getBehaviour().getFile(appModule.getDeployedApplicationName(), 0,
								outputFilePath, subMonitor.newChild(50));
					}
					else {
						return null;
					}
				}

				// Any result is valid for this operation, as errors are handled
				// via exception
				protected boolean isValid(String result) {
					return true;
				}

			}.run(subMonitor);
		}
		catch (CoreException e) {
			error = e;
		}

		if (subMonitor.isCanceled()) {
			throw new OperationCanceledException();
		}

		if (content == null) {
			String message = "Failed to connect debugger to Cloud application - Timed out fetching ngrok output file for: "//$NON-NLS-1$
					+ appModule.getDeployedApplicationName()
					+ ". Please verify that the ngrok output file exists in the Cloud or that the application is running correctly.";//$NON-NLS-1$
			if (error != null) {
				throw CloudErrorUtil.asCoreException(message, error, false);
			}
			else {
				throw CloudErrorUtil.toCoreException(message);
			}
		}

		return content;
	}

	@Override
	public boolean isDebugSupported(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(appModule);
		return javaProject != null && javaProject.exists() && containsDebugFiles(javaProject);
	}

	public static boolean containsDebugOption(EnvironmentVariable var) {
		return var != null && var.getValue() != null && JAVA_OPTS.equals(var.getVariable())
				&& (var.getValue().contains("-Xdebug") || var.getValue().contains("-Xrunjdwp")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected boolean containsDebugFiles(IJavaProject project) {
		try {

			IClasspathEntry[] entries = project.getResolvedClasspath(true);

			if (entries != null) {
				for (IClasspathEntry entry : entries) {
					if (entry != null && entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						IPath projectPath = project.getPath();
						IPath relativePath = entry.getPath().makeRelativeTo(projectPath);
						IFolder folder = project.getProject().getFolder(relativePath);
						if (getFile(folder, ".profile.d", "ngrok.sh") != null) {//$NON-NLS-1$ //$NON-NLS-2$
							return true;
						}
					}
				}
			}
		}
		catch (JavaModelException e) {
			CloudFoundryPlugin.logError(e);
		}
		catch (CoreException ce) {
			CloudFoundryPlugin.logError(ce);
		}
		return false;
	}

	public static IFile getFile(IResource resource, String containingFolderName, String fileName) throws CoreException {

		if (resource == null || !resource.exists()) {
			return null;
		}
		if (resource instanceof IFile && resource.getName().equals(fileName) && resource.getParent() != null
				&& resource.getParent().getName().equals(containingFolderName)) {
			return (IFile) resource;
		}
		else if (resource instanceof IContainer) {
			IContainer container = (IContainer) resource;
			IResource[] children = container.members();

			if (children != null) {
				for (IResource child : children) {

					IFile file = getFile(child, containingFolderName, fileName);
					if (file != null) {
						return file;
					}
				}
			}
		}

		return null;
	}

	@Override
	public String getLaunchConfigurationType(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		return NgrokDebugLaunchConfigDelegate.LAUNCH_CONFIGURATION_ID;
	}

}
