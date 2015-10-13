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

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;

public interface IDebugProvider {

	/**
	 * Resolve the connection descriptor. Throw {@link CoreException} if failed
	 * to resolve.
	 * @param appModule
	 * @param cloudServer
	 * @param monitor
	 * @return non-null descriptor.
	 * @throws CoreException if failed to resolve descriptor
	 * @throws OperationCanceledException if connection operation was canceled
	 */
	public DebugConnectionDescriptor getDebugConnectionDescriptor(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, IProgressMonitor monitor) throws CoreException, OperationCanceledException;

	/**
	 * Determine if the application is in a state where it can be launched. If
	 * true, the debug framework will proceed to configure and launch the
	 * application in debug mode. If false, the framework will stop the launch
	 * without errors.
	 * @param appModule
	 * @param cloudServer
	 * @return True if app should be debugged. False if debug should stop.
	 */
	public boolean canLaunch(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			IProgressMonitor monitor) throws CoreException;

	/**
	 * Return true if debug is supported for the given application running on
	 * the target cloud server. This is meant to be a fairly quick check
	 * therefore avoid long-running operations.
	 * @param appModule
	 * @param cloudServer
	 * @return true if debug is supported for the given app. False otherwise
	 */
	public boolean isDebugSupported(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer);

	/**
	 * Optional configuration ID to be used for launching the application in
	 * debug mode. Return null if the default launch configuration should be
	 * used.
	 * @see ILaunchConfigurationType
	 * @see ILaunchConfiguration
	 * @return Optional launch configuration ID or null otherwise if default is
	 * to be used.
	 */
	public String getLaunchConfigurationID();

	/**
	 * Perform any necessary configuration on the application before launching
	 * it in debug mode, For example, if environment variables need to be set in
	 * the application. Return true if configuration was successful and app is
	 * ready to be launched in debug mode . Return false if debug should not
	 * proceed. Throw {@link CoreException} if error occurred while configuring
	 * the application.
	 * @param appModule
	 * @param cloudServer
	 * @param monitor
	 * @throws CoreException
	 * @return true if application is ready to be launched. False if debug
	 * launch should stop.
	 */
	public boolean configureApp(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			IProgressMonitor monitor) throws CoreException;

}