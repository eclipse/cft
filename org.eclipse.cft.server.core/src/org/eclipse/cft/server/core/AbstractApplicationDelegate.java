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
 *     IBM Corporation - combine IApplicationDelegate and ApplicationDelegate
 ********************************************************************************/
package org.eclipse.cft.server.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;

/**
 * API that application contributions through the extension point:
 * <p/>
 * org.eclipse.cft.server.core.application
 * <p/>
 * are required to implement. Applications are represented as Eclipse WST
 * IModules. The main contributions for an application is:
 * 
 * <ul>
 * <li>Whether the application requires a URL to be set before being published.
 * URLs are set through the Cloud Foundry plugin UI. In most case, this is true,
 * except Java standalone applications</li>
 * <li>Optionally, an archiving mechanism for the application's resources that
 * should be pushed to a Cloud Foundry server</li>
 * 
 * </ul>
 * 
 */
public abstract class AbstractApplicationDelegate {

	/**
	 * 
	 * In most cases, this is true. Java Web type applications require a URL,
	 * but some application types, like Java Standalone do not
	 * @return true if the application requires that a URL be set when
	 * publishing the application. False otherwise.
	 */
	public abstract boolean requiresURL();

	/**
	 * A light-weight way of telling the framework whether this application
	 * delegate contributes an application archive for application resource
	 * serialisation when pushing the application's resources to a Cloud Foundry
	 * Server. If false, it means the framework should create it's default
	 * payload for the application (typically by using a .war file).
	 * @param module corresponding to an application that should be published to
	 * a Cloud Foundry server
	 * @return true if this delegate provides its own application serialisation
	 * mechanism, or false otherwise
	 */
	public abstract boolean providesApplicationArchive(IModule module);

	/**
	 * An application archive generates input streams for an application's files
	 * when the Cloud Foundry framework is ready to push an application to a
	 * Cloud Foundry server. Files are represented by IModuleResource, and the
	 * archive generates an input stream for each IModuleResource.
	 * 
	 * <p/>
	 * In addition, the application archive is also used to calculate sha1 hash
	 * codes for each application file so that the Cloud Foundry server can
	 * determine what resources have changed prior to the Cloud Foundry
	 * framework pushing any changes to the application.
	 * <p/>
	 * For Java Web type applications (Spring, Java Web), it is not
	 * necessary to provide an explicit application archive, as the Cloud
	 * Foundry plugin framework generates .war files for such applications and
	 * uses a built-in default application archive that reads the .war file.
	 * <p/>
	 * However for some other application types like for example Java
	 * standalone, .war files are not generated, and therefore serialisation of
	 * the application files is performed through an application archive
	 * specific to Java standalone applications
	 * 
	 * <p/>
	 * The default implementation provides a general way to generated an
	 * application archive from IModuleResources for a given IModule. Subclasses
	 * can override this and provide their own archive which converts
	 * IModuleResource into archive entries.
	 * 
	 * <p/>
	 * Alternately, subclasses can return null if no application archive needs
	 * to be used and the framework .war file generation should be used instead.
	 * 
	 * @param module for the application that needs to be published.
	 * @param server server where application should be deployed (or where
	 * it is currently deployed, and app resources need to be updated).
	 * @param moduleResources corresponding module resources for the module that
	 * needs to be published. These module resources are typically used to
	 * generated the archive
	 * @return Application archive for the give module resources, or null if no
	 * archive is required and the framework should create a .war file for the
	 * application
	 * @throws CoreException if the application delegate provides an application
	 * archive but it failed to create one.
	 */
	public abstract CFApplicationArchive getApplicationArchive(IModule module, IServer server,
			IModuleResource[] moduleResources, IProgressMonitor monitor) throws CoreException;

	/**
	 * {@link IStatus#OK} If the deployment information is valid. Otherwise
	 * return {@link IStatus#ERROR} if error. Must NOT return a null status.
	 * @param deploymentInfo
	 * @return non-null status.
	 */
	public abstract IStatus validateDeploymentInfo(ApplicationDeploymentInfo deploymentInfo);

	/**
	 * Resolve an application deployment for an existing application. Return null
	 * if it cannot be resolved or application no longer exists. If returning non-null value, it should always
	 * be a new copy of a deployment info.
	 * @param module application that already exists in the server
	 * @param server where application exists.
	 * @return A new copy of the deployment information for an existing
	 * application, or null if it cannot be resolved.
	 */
	public abstract ApplicationDeploymentInfo getExistingApplicationDeploymentInfo(IModule appModule,
			IServer server) throws CoreException;
	

	/**
	 * Get a default application deployment information, regardless of whether
	 * the application exists or not. It should contain default settings
	 * for this type of application (e.g. memory, default URL, if necessary,
	 * etc..). Should not be null.
	 * @param module
	 * @param server where application either exists or may be deployed to
	 * @return Non-null application deployment information with default values.
	 * @throws CoreException if failed to resolve a default application deployment info
	 */
	public abstract ApplicationDeploymentInfo getDefaultApplicationDeploymentInfo(IModule module,
			IServer server, IProgressMonitor monitor) throws CoreException;
}
