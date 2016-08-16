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

package org.eclipse.cft.server.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * API that application contributions through the extension point:
 * <p/>
 * org.eclipse.cft.server.core.debugProvider
 * <p/>
 * are required to implement.  The debug provider is responsible for
 * creating and initializing the debug launch configuration for servers
 * and modules that it supports.
 */
public abstract class AbstractDebugProvider {
	
	/**
	 * Return true if debug is supported for the given application running on
	 * the target cloud server. This is meant to be a fairly quick check
	 * therefore avoid long-running operations.
	 * @param module The application module to be debugged
	 * @param server The server the module is deployed on
	 * @return true if debug is supported for the given app. False otherwise
	 */
	public abstract boolean isDebugSupported(IModule module, IServer server);
	
	/**
	 * Get the launch id for the given server and application instance.
	 * @param module The application module
	 * @param server The server the module is deployed on
	 * @param appInstance The application instance
	 * @return The launch id
	 */
	public abstract String getApplicationDebugLaunchId(IModule module, IServer server,
			int appInstance) throws CoreException;

	/**
	 * Create and initialize the launch configuration.
	 * @param module The application module to be debugged
	 * @param server The server the module is deployed on
	 * @param appInstance The application instance id
	 * @param remoteDebugPort The remote debug port to use
	 * @param monitor A progress monitor
	 * @return A non-null launch configuration to debug the given application
	 * name.
	 * @throws CoreException If unable to resolve launch configuration.
	 */
	public abstract ILaunchConfiguration getLaunchConfiguration(IModule module,
			IServer server, int appInstance, int remoteDebugPort, IProgressMonitor monitor)
					throws CoreException;
}
