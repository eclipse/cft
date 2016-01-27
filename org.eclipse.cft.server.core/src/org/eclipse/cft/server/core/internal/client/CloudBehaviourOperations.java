/*******************************************************************************
 * Copyright (c) 2015, 2016 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.eclipse.cft.server.core.internal.ApplicationAction;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.cft.server.core.internal.application.EnvironmentVariable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.internal.Server;

/**
 * 
 * Creates Cloud operations defined by {@link ICloudFoundryOperation} for start,
 * stopping, publishing, scaling applications, as well as creating, deleting,
 * and binding services.
 * <p/>
 * {@link ICloudFoundryOperation} should be used for performing Cloud operations
 * that require firing server and module refresh events.
 */
public class CloudBehaviourOperations {

	public static String INTERNAL_ERROR_NO_WST_MODULE = "Internal Error: No WST IModule specified - Unable to deploy or start application"; //$NON-NLS-1$

	private final CloudFoundryServerBehaviour behaviour;

	public CloudBehaviourOperations(CloudFoundryServerBehaviour behaviour) {
		this.behaviour = behaviour;
	}

	/**
	 * Get operation to create a list of services
	 * @param services
	 * @param monitor
	 * @throws CoreException if operation was not created
	 */
	public ICloudFoundryOperation createServices(final CloudService[] services) throws CoreException {
		return new UpdateServicesOperation(behaviour.getRequestFactory().getCreateServicesRequest(services), behaviour);
	}

	/**
	 * Gets an operation to delete Services
	 * @param services
	 * @throws CoreException if operation was not created.
	 */
	public ICloudFoundryOperation deleteServices(final List<String> services) throws CoreException {
		return new UpdateServicesOperation(behaviour.getRequestFactory().getDeleteServicesRequest(services), behaviour);
	}

	/**
	 * Gets an operation to update the number of application instances. The
	 * operation does not restart the application if the application is already
	 * running. The CF server does allow instance scaling to occur while the
	 * application is running.
	 * @param module representing the application. must not be null or empty
	 * @param instanceCount must be 1 or higher.
	 * @throws CoreException if operation was not created
	 */
	public ICloudFoundryOperation instancesUpdate(final CloudFoundryApplicationModule appModule,
			final int instanceCount) throws CoreException {

		return new BehaviourOperation(behaviour, appModule.getLocalModule()) {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				String appName = appModule.getDeployedApplicationName();

				// Update the instances in the Cloud space
				getBehaviour().updateApplicationInstances(appName, instanceCount, monitor);

				// Refresh the module with the new instances information
				getBehaviour().updateDeployedModule(appModule.getLocalModule(), monitor);

				// Fire a separate instances update event to notify listener who
				// are specifically listening
				// to instance changes that do not require a full application
				// refresh event.
				ServerEventHandler.getDefault().fireAppInstancesChanged(behaviour.getCloudFoundryServer(), getModule());

				// Schedule another refresh application operation as instances
				// may take
				// time to be updated (the new instances may have to be
				// restarted in the Cloud Space)
				getBehaviour().getRefreshHandler().schedulesRefreshApplication(getModule());
			}

		};
	}

	/**
	 * Gets an operation that updates an application's memory. The operation
	 * does not restart an application if the application is currently running.
	 * The CF server does allow memory scaling to occur while the application is
	 * running.
	 * @param module must not be null or empty
	 * @param memory must be above zero.
	 * @throws CoreException if operation was not created
	 */
	public ICloudFoundryOperation memoryUpdate(final CloudFoundryApplicationModule appModule, final int memory)
			throws CoreException {
		return new ApplicationUpdateOperation(
				behaviour.getRequestFactory().getUpdateApplicationMemoryRequest(appModule, memory), behaviour,
				appModule);
	}

	/**
	 * Gets an operation to update the application's URL mapping.
	 * @throws CoreException if failed to create the operation
	 */
	public ICloudFoundryOperation mappedUrlsUpdate(final String appName, final List<String> urls) throws CoreException {

		final CloudFoundryApplicationModule appModule = behaviour.getCloudFoundryServer()
				.getExistingCloudModule(appName);

		if (appModule != null) {
			return new ApplicationUpdateOperation(behaviour.getRequestFactory().getUpdateAppUrlsRequest(appName, urls),
					behaviour, appModule.getLocalModule());
		}
		else {
			throw CloudErrorUtil.toCoreException(
					"Expected an existing Cloud application module but found none. Unable to update application URLs"); //$NON-NLS-1$
		}
	}

	/**
	 * Gets an operation to update the service bindings of an application
	 * @throws CoreException if operation was not created
	 */
	public ICloudFoundryOperation bindServices(final CloudFoundryApplicationModule appModule,
			final List<String> services) throws CoreException {
		return new ApplicationUpdateOperation(behaviour.getRequestFactory().getUpdateServicesRequest(
				appModule.getDeployedApplicationName(), services), behaviour, appModule.getLocalModule());
	}

	/**
	 * Gets an operation that updates the application's environment variables.
	 * Note that the application needs to first exist in the server, and be in a
	 * state that will accept environment variable changes (either stopped, or
	 * running after staging has completed).
	 * 
	 * @throws CoreException if operation was not created
	 */
	public ICloudFoundryOperation environmentVariablesUpdate(IModule module, String appName,
			List<EnvironmentVariable> variables) throws CoreException {
		BaseClientRequest<Void> request = behaviour.getRequestFactory().getUpdateEnvVarRequest(appName, variables);
		return new ApplicationUpdateOperation(request, behaviour, module);
	}

	/**
	 * Returns an executable application operation based on the given Cloud
	 * Foundry application module and an application start mode (
	 * {@link ApplicationAction} ).
	 * <p/>
	 * Throws error if failure occurred while attempting to resolve an
	 * operation. If no operation is resolved and no errors occurred while
	 * attempting to resolve an operation, null is returned, meaning that no
	 * operation is currently defined for the given deployment mode.
	 * <p/>
	 * It does NOT execute the operation.
	 * @param application
	 * @param action
	 * @return resolved executable operation associated with the given
	 * deployment mode, or null if an operation could not be resolved.
	 * @throws CoreException
	 */
	public ICloudFoundryOperation applicationDeployment(CloudFoundryApplicationModule application,
			ApplicationAction action) throws CoreException {
		IModule[] modules = new IModule[] { application.getLocalModule() };

		return applicationDeployment(modules, action, true);
	}

	public ICloudFoundryOperation applicationDeployment(IModule[] modules, ApplicationAction action)
			throws CoreException {
		return applicationDeployment(modules, action, true);
	}

	/**
	 * Resolves an {@link ICloudFoundryOperation} that performs a start, stop,
	 * restart or push operation for the give modules and specified
	 * {@link ApplicationAction}.
	 * <p/>
	 * If no operation can be specified, throws {@link CoreException}
	 * @param modules
	 * @param action
	 * @return Non-null application operation.
	 * @throws CoreException if operation cannot be resolved.
	 */
	public ICloudFoundryOperation applicationDeployment(IModule[] modules, ApplicationAction action,
			boolean clearConsole) throws CoreException {

		if (modules == null || modules.length == 0) {
			throw CloudErrorUtil.toCoreException(INTERNAL_ERROR_NO_WST_MODULE);
		}
		ICloudFoundryOperation operation = null;
		// Set the deployment mode
		switch (action) {
		case START:
			boolean incrementalPublish = false;
			// A start operation that always performs a full publish
			operation = new StartOperation(behaviour, incrementalPublish, modules, clearConsole);
			break;
		case STOP:
			operation = new StopApplicationOperation(behaviour, modules);
			break;
		case RESTART:
			operation = new RestartOperation(behaviour, modules, clearConsole);
			break;
		case UPDATE_RESTART:
			// Check the full publish preference to determine if full or
			// incremental publish should be done when starting an application
			operation = new StartOperation(behaviour, CloudFoundryPlugin.getDefault().getIncrementalPublish(), modules,
					clearConsole);
			break;
		case PUSH:
			operation = new PushApplicationOperation(behaviour, modules, clearConsole);
			break;
		}

		if (operation == null) {
			throw CloudErrorUtil.toCoreException("Internal Error: Unable to resolve a Cloud application operation."); //$NON-NLS-1$
		}
		return operation;
	}

	/**
	 * Refreshes all modules, services, and the instance info and stats for the
	 * given optional module. If null is passed only the list of modules and
	 * services is refreshed.
	 * <p/>
	 * This may be a long running operation
	 * @return Non-null operation
	 */
	public BehaviourOperation refreshAll(final IModule module) {
		return new BehaviourOperation(behaviour, module) {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {

				CloudFoundryServer cloudServer = getBehaviour().getCloudFoundryServer();

				SubMonitor subMonitor = SubMonitor.convert(monitor);
				subMonitor.beginTask(NLS.bind(Messages.CloudBehaviourOperations_REFRESHING_APPS_AND_SERVICES,
						cloudServer.getServer().getId()), 100);

				if (getModule() != null) {
					getBehaviour().updateDeployedModule(getModule(), subMonitor.newChild(40));
				}
				else {
					subMonitor.worked(40);
				}
				// Get updated list of cloud applications from the server
				List<CloudApplication> applications = getBehaviour().getApplications(subMonitor.newChild(20));

				// update applications and deployments from server
				Map<String, CloudApplication> deployedApplicationsByName = new LinkedHashMap<String, CloudApplication>();
				Map<String, ApplicationStats> stats = new LinkedHashMap<String, ApplicationStats>();

				for (CloudApplication application : applications) {
					ApplicationStats sts = getBehaviour().getApplicationStats(application.getName(), subMonitor);
					stats.put(application.getName(), sts);
					deployedApplicationsByName.put(application.getName(), application);
				}

				cloudServer.updateModules(deployedApplicationsByName, stats);

				// Clear publish error
				Server server = (Server) cloudServer.getServer();

				for (IModule module : server.getModules()) {
					CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);
					if (appModule != null) {
						appModule.setStatus(null);
						appModule.validateDeploymentInfo();
					}
				}

				List<CloudService> services = getBehaviour().getServices(subMonitor.newChild(20));

				ServerEventHandler.getDefault()
						.fireServerEvent(new CloudRefreshEvent(getBehaviour().getCloudFoundryServer(), getModule(),
								CloudServerEvent.EVENT_SERVER_REFRESHED, services));

				subMonitor.worked(20);
			}
		};
	}

	public BehaviourOperation refreshForDeploymentChange(final IModule module) {
		return new BehaviourOperation(behaviour, module) {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {

				if (module == null) {
					throw CloudErrorUtil.toCoreException("Internal Error: No module to refresh in - " + //$NON-NLS-1$
							getBehaviour().getCloudFoundryServer().getServerId());
				}

				getBehaviour().updateDeployedModule(module, monitor);
				ServerEventHandler.getDefault().fireAppDeploymentChanged(behaviour.getCloudFoundryServer(), module);
			}
		};
	}

	/**
	 * Updates a deployed module (deployed means that the module is associated
	 * with an existing Cloud application and deployment run state is known). If
	 * the module is not deployed, no update is performed. This method is used
	 * when the caller wants to guarantee that only Cloud information about the
	 * application is updated in the module IFF the module is already known to
	 * be deployed (e.g. Cloud application exists and the runstate of the
	 * application is known: started, stopped, etc..)
	 * @param module
	 * @return Non-null operation.
	 */
	public BehaviourOperation updateDeployedModule(final IModule module) {

		return new BehaviourOperation(behaviour, module) {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {

				if (module == null) {
					throw CloudErrorUtil.toCoreException("Internal Error: No module to update in - " + //$NON-NLS-1$
							getBehaviour().getCloudFoundryServer().getServerId());
				}

				CloudFoundryApplicationModule appModule = getBehaviour().updateDeployedModule(module, monitor);

				// Clear the publish errors for now
				if (appModule != null) {
					appModule.setStatus(null);
					appModule.validateDeploymentInfo();
				}

				ServerEventHandler.getDefault().fireApplicationRefreshed(behaviour.getCloudFoundryServer(), module);
			}
		};
	}

	/**
	 * Updates the given module with complete Cloud information
	 * about the application including application deployment state.
	 * @param module
	 * @return Non-null operation.
	 */
	public BehaviourOperation updateModuleWithAllCloudInfo(final IModule module) {

		return new BehaviourOperation(behaviour, module) {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {

				if (module == null) {
					throw CloudErrorUtil.toCoreException("Internal Error: No module to update in - " + //$NON-NLS-1$
							getBehaviour().getCloudFoundryServer().getServerId());
				}

				CloudFoundryApplicationModule appModule = getBehaviour().updateModuleWithAllCloudInfo(module, monitor);
				// Clear the publish errors for now
				if (appModule != null) {
					appModule.setStatus(null);
					appModule.validateDeploymentInfo();
				}

				ServerEventHandler.getDefault().fireApplicationRefreshed(behaviour.getCloudFoundryServer(), module);
			}
		};
	}

	public ICloudFoundryOperation deleteModules(IModule[] modules, final boolean deleteServices) {
		return new DeleteModulesOperation(behaviour, modules, deleteServices);
	}

}
