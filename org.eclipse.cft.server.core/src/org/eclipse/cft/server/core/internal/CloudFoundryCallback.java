/*******************************************************************************
 * Copyright (c) 2012, 2017 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal;

import java.util.List;

import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.DeploymentConfiguration;
import org.eclipse.cft.server.core.internal.debug.ApplicationDebugLauncher;
import org.eclipse.cft.server.core.internal.jrebel.CFRebelServerIntegration;
import org.eclipse.cft.server.core.internal.log.CloudLog;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * Callback interface to support clients to hook into CloudFoundry Server
 * processes.
 * 
 * <p/>
 * INTERNAL API: Adopters should not extend or use as API may change.
 * @author Christian Dupuis
 * @author Steffen Pingel
 * @author Terry Denney
 */
public abstract class CloudFoundryCallback {

	public void printToConsole(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule, String message,
			boolean clearConsole, boolean isError) {
		// optional
	}

	public void trace(CloudLog log, boolean clear) {
		// optional
	}

	public void showTraceView(boolean showTrace) {
		// optional
	}

	public void startApplicationConsole(CloudFoundryServer cloudServer, CloudFoundryApplicationModule cloudModule,
			int showIndex, IProgressMonitor monitor) {

	}

	public abstract void applicationStarted(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule);

	public abstract void applicationStarting(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule);

	/**
	 * Show deployed application's Cloud Foundry log files locally.
	 * @param cloudServer
	 * @param cloudModule
	 * @param showIndex if -1 shows the first app instance
	 */
	public void showCloudFoundryLogs(CloudFoundryServer cloudServer, CloudFoundryApplicationModule cloudModule,
			int showIndex, IProgressMonitor monitor) {

	}

	/**
	 * Stops all consoles for the given application for all application
	 * instances.
	 * @param cloudModule
	 * @param cloudServer
	 */
	public abstract void stopApplicationConsole(CloudFoundryApplicationModule cloudModule,
			CloudFoundryServer cloudServer);

	public abstract void disconnecting(CloudFoundryServer server);

	public abstract void getCredentials(CloudFoundryServer server);

	/**
	 * Prepares an application to either be deployed, started or restarted. The
	 * main purpose to ensure that the application's deployment information is
	 * complete. If incomplete, it will prompt the user for missing information.
	 * @param monitor
	 * @return {@link DeploymentConfiguration} Defines local deployment
	 * configuration of the application, for example which deployment mode
	 * should be used like starting an application, restarting, etc..May be
	 * null. If null, the framework will attempt to determine an appropriate
	 * deployment configuration.
	 * @throws CoreException if failure while preparing the application for
	 * deployment
	 * @throws OperationCanceledException if the user cancelled deploying or
	 * starting the application. The application's deployment information should
	 * not be modified in this case.
	 */
	public abstract DeploymentConfiguration prepareForDeployment(CloudFoundryServer server,
			CloudFoundryApplicationModule module, IProgressMonitor monitor)
					throws CoreException, OperationCanceledException;

	public abstract void deleteServices(List<String> services, CloudFoundryServer cloudServer);

	public abstract void deleteApplication(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer);

	public boolean isAutoDeployEnabled() {
		return true;
	}

	public void displayAndLogError(IStatus status) {

	}

	public boolean prompt(final String title, final String message) {
		return false;
	}
	
	public boolean question(final String title, final String message) {
		return false;
	}

	public CFRebelServerIntegration getJRebelServerIntegration() {
		return null;
	}

	public ApplicationDebugLauncher getDebugLauncher(CloudFoundryServer cloudServer) {
		return null;
	}
	
	public void syncRunInUi(Runnable runnable) {
		
	}
	
	/** Returns true if login succeeds, false otherwise.*/
	public abstract boolean ssoLoginUserPrompt(CloudFoundryServer cloudServer);
}
