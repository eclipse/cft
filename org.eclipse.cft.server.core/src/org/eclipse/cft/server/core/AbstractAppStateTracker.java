/*******************************************************************************
 * Copyright (c) 2014, 2016 Pivotal Software, Inc. and others
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
 *     IBM - initial API and implementation
 ********************************************************************************/

package org.eclipse.cft.server.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * App state tracker abstract class that all app state tracker should extends.
 * @author eyuen
 */
public abstract class AbstractAppStateTracker {
	
	protected IServer server;
	
	/**
	 * Get the current application state
	 * @param appModule the application to check the state on
	 * @return the application state. The state is expected to be server state constants that are defined in
	 * org.eclipse.wst.server.core.IServer, e.g. IServer.STATE_STARTED.
	 */
	public abstract int getApplicationState(ICloudFoundryApplicationModule appModule);

	/**
	 * initialize the server object for tracking purpose
	 * @param curServer the server to be tracked.
	 */
	public void setServer(IServer curServer) {
		server = curServer;
	}

	/**
	 * Start tracking the given application module and this can also be used to start the initialization
	 * of the tracking, e.g. start monitoring the console output.
	 * @param module The application module to be tracked
	 */
	public abstract void startTracking(IModule module);

	/**
	 * Stop tracking the given application module and this can also be used to clean up
	 * the tracking, e.g. stop monitoring the console output.
	 * @param module The application module to be tracked
	 */
	public abstract void stopTracking(IModule module);

	/**
	 * Start tracking the given application module and this can also be used to start the initialization.
	 * Intended to be overridden.  If not overridden, then calls original implemented startTracking(CloudFoundryApplicationModule appModule).  
	 * Overrides should not have to call super.
	 * of the tracking, e.g. start monitoring the console output.
	 * @param module The application module to be tracked
	 * @param monitor The progress monitor to allow for canceling tracking
	 */
	public void startTracking(IModule module, IProgressMonitor monitor) {
		startTracking(module);
	}

	/**
	 * Stop tracking the given application module and this can also be used to clean up
 	 * Intended to be overridden.  If not overridden, then calls original implemented stopTracking(CloudFoundryApplicationModule appModule).  
	 * Overrides should not have to call super.
	 * the tracking, e.g. stop monitoring the console output.
	 * @param appModule The application module to be tracked
	 * @param monitor The progress monitor to allow for canceling tracking
	 */
	public void stopTracking(IModule module, IProgressMonitor monitor) {
		stopTracking(module);
	}
}
