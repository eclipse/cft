package org.eclipse.cft.server.core;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;

public abstract class AbstractDebugProvider {
	
	/**
	 * Return true if debug is supported for the given application running on
	 * the target cloud server. This is meant to be a fairly quick check
	 * therefore avoid long-running operations.
	 * @param appModule The application module to be debugged
	 * @param cloudServer The server the module is deployed on
	 * @return true if debug is supported for the given app. False otherwise
	 */
	public abstract boolean isDebugSupported(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer);
	
	/**
	 * Get the launch id for the given server and application instance.
	 * @param appModule The application module
	 * @param cloudServer The server the module is deployed on
	 * @param appInstance The application instance
	 * @return The launch id
	 */
	public abstract String getApplicationDebugLaunchId(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			int appInstance);

	/**
	 * Create and initialize the launch configuration.
	 * @param appModule The application module to be debugged
	 * @param cloudServer The server the module is deployed on
	 * @param appInstance The application instance id
	 * @param remoteDebugPort The remote debug port to use
	 * @param monitor A progress monitor
	 * @return A non-null launch configuration to debug the given application
	 * name.
	 * @throws CoreException If unable to resolve launch configuration.
	 */
	public abstract ILaunchConfiguration getLaunchConfiguration(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, int appInstance, int remoteDebugPort, IProgressMonitor monitor)
					throws CoreException;
}
