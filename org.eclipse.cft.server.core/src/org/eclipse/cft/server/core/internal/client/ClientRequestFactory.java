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
 *     IBM - Bug 485697 - Implement host name taken check in CF wizards
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.client;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.cft.server.core.CFServiceOffering;
import org.eclipse.cft.server.core.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.CloudServicesUtil;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.cft.server.core.internal.client.diego.CFInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A general pre-Diego Client request factory based off the v1 Cloud Foundry
 * Java client.
 *
 */
public class ClientRequestFactory {

	protected final CloudFoundryServerBehaviour behaviour;

	protected CFInfo cachedInfo;

	public ClientRequestFactory(CloudFoundryServerBehaviour behaviour) {
		this.behaviour = behaviour;
	}

	public BaseClientRequest<?> getUpdateApplicationMemoryRequest(final CloudFoundryApplicationModule appModule,
			final int memory) {
		return new AppInStoppedStateAwareRequest<Void>(NLS.bind(Messages.CloudFoundryServerBehaviour_UPDATE_APP_MEMORY,
				appModule.getDeployedApplicationName()), behaviour) {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.updateApplicationMemory(appModule.getDeployedApplicationName(), memory);
				return null;
			}
		};
	}

	public BaseClientRequest<?> updateApplicationDiego(final CloudFoundryApplicationModule appModule, final boolean diego) {
		
		String message;
		if(diego) {
			message = NLS.bind(Messages.CloudFoundryServerBehaviour_ENABLING_DIEGO,
					appModule.getDeployedApplicationName());
		} else {
			message = NLS.bind(Messages.CloudFoundryServerBehaviour_DISABLING_DIEGO,
					appModule.getDeployedApplicationName());			
		}
		
		return new AppInStoppedStateAwareRequest<Void>(message, behaviour) {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.updateApplicationDiego(appModule.getDeployedApplicationName(), diego);
				return null;
			}
		};
	}


	public BaseClientRequest<?> updateApplicationEnableSsh(final CloudFoundryApplicationModule appModule, final boolean enableSsh) {
		String message;
		if(enableSsh) {
			message = NLS.bind(Messages.CloudFoundryServerBehaviour_ENABLING_SSH,
					appModule.getDeployedApplicationName());
		} else {
			message = NLS.bind(Messages.CloudFoundryServerBehaviour_DISABLING_SSH,
					appModule.getDeployedApplicationName());			
		}
		
		return new AppInStoppedStateAwareRequest<Void>(message, behaviour) {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.updateApplicationEnableSsh(appModule.getDeployedApplicationName(), enableSsh);
				return null;
			}
		};
	}

	public BaseClientRequest<List<CloudRoute>> getRoutes(final String domainName) throws CoreException {

		return new BehaviourRequest<List<CloudRoute>>(NLS.bind(Messages.ROUTES, domainName), behaviour) {
			@Override
			protected List<CloudRoute> doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getRoutes(domainName);
			}
		};
	}

	public BaseClientRequest<StartingInfo> restartApplication(final String appName, final String opLabel)
			throws CoreException {
		return new BehaviourRequest<StartingInfo>(opLabel, behaviour) {
			@Override
			protected StartingInfo doRun(final CloudFoundryOperations client, SubMonitor progress)
					throws CoreException, OperationCanceledException {
				return client.restartApplication(appName);
			}
		};
	}

	public BaseClientRequest<?> deleteApplication(final String appName) {
		return new BehaviourRequest<Void>(NLS.bind(Messages.DELETING_MODULE, appName), behaviour) {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				CloudFoundryPlugin.logInfo("ClientRequestFactory.deleteApplication(...): appName:"+appName);
				client.deleteApplication(appName);
				return null;
			}
		};
	}

	public BaseClientRequest<?> getUpdateAppUrlsRequest(final String appName, final List<String> urls) {
		return new AppInStoppedStateAwareRequest<Void>(
				NLS.bind(Messages.CloudFoundryServerBehaviour_UPDATE_APP_URLS, appName), behaviour) {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {

				// Look up the existing urls locally first to avoid a client
				// call
				CloudFoundryApplicationModule existingAppModule = behaviour.getCloudFoundryServer()
						.getExistingCloudModule(appName);

				List<String> oldUrls = existingAppModule != null && existingAppModule.getDeploymentInfo() != null
						? existingAppModule.getDeploymentInfo().getUris() : null;

				if (oldUrls == null) {
					oldUrls = behaviour.getCloudApplication(appName, progress).getUris();
				}

				client.updateApplicationUris(appName, urls);

				if (existingAppModule != null) {
					ServerEventHandler.getDefault()
							.fireServerEvent(new AppUrlChangeEvent(behaviour.getCloudFoundryServer(),
									CloudServerEvent.EVENT_APP_URL_CHANGED, existingAppModule.getLocalModule(),
									Status.OK_STATUS, oldUrls, urls));

				}
				return null;
			}
		};
	}

	public BaseClientRequest<?> getUpdateServicesRequest(final String appName, final List<String> services) {
		return new StagingAwareRequest<Void>(
				NLS.bind(Messages.CloudFoundryServerBehaviour_UPDATE_SERVICE_BINDING, appName), behaviour) {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {

				client.updateApplicationServices(appName, services);
				return null;
			}
		};
	}

	public BaseClientRequest<Void> getUpdateEnvVarRequest(final String appName,
			final List<EnvironmentVariable> variables) {
		final String label = NLS.bind(Messages.CloudFoundryServerBehaviour_UPDATE_ENV_VARS, appName);
		return new BehaviourRequest<Void>(label, behaviour) {

			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				// Update environment variables.
				Map<String, String> varsMap = new HashMap<String, String>();

				if (variables != null) {
					for (EnvironmentVariable var : variables) {
						varsMap.put(var.getVariable(), var.getValue());
					}
				}

				client.updateApplicationEnv(appName, varsMap);

				return null;
			}

		};
	}

	public BaseClientRequest<List<CFServiceInstance>> getDeleteServicesRequest(final List<String> services) {
		return new BehaviourRequest<List<CFServiceInstance>>(Messages.CloudFoundryServerBehaviour_DELETE_SERVICES,
				behaviour) {
			@Override
			protected List<CFServiceInstance> doRun(CloudFoundryOperations client, SubMonitor progress)
					throws CoreException {

				SubMonitor serviceProgress = SubMonitor.convert(progress, services.size());

				List<String> boundServices = new ArrayList<String>();
				for (String service : services) {
					serviceProgress.subTask(NLS.bind(Messages.CloudFoundryServerBehaviour_DELETING_SERVICE, service));

					boolean shouldDelete = true;
					try {
						CloudServiceInstance instance = client.getServiceInstance(service);
						List<CloudServiceBinding> bindings = (instance != null) ? instance.getBindings() : null;
						shouldDelete = bindings == null || bindings.isEmpty();
					}
					catch (Throwable t) {
						// If it is a server or network error, it will still be
						// caught below
						// when fetching the list of apps:
						// [96494172] - If fetching service instances fails, try
						// finding an app with the bound service through the
						// list of
						// apps. This is treated as an alternate way only if the
						// primary form fails as fetching list of
						// apps may be potentially slower
						List<CloudApplication> apps = behaviour.getApplications(progress);
						if (apps != null) {
							for (int i = 0; shouldDelete && i < apps.size(); i++) {
								CloudApplication app = apps.get(i);
								if (app != null) {
									List<String> appServices = app.getServices();
									if (appServices != null) {
										for (String appServ : appServices) {
											if (service.equals(appServ)) {
												shouldDelete = false;
												break;
											}
										}
									}
								}
							}
						}
					}

					if (shouldDelete) {
						client.deleteService(service);
					}
					else {
						boundServices.add(service);
					}
					serviceProgress.worked(1);
				}
				if (!boundServices.isEmpty()) {
					StringWriter writer = new StringWriter();
					int size = boundServices.size();
					for (int i = 0; i < size; i++) {
						writer.append(boundServices.get(i));
						if (i < size - 1) {
							writer.append(',');
							writer.append(' ');
						}
					}
					String boundServs = writer.toString();
					CloudFoundryPlugin.getCallback().displayAndLogError(CloudFoundryPlugin.getErrorStatus(
							NLS.bind(Messages.CloudFoundryServerBehaviour_ERROR_DELETE_SERVICES_BOUND, boundServs)));

				}
				return CloudServicesUtil.asServiceInstances(client.getServices());
			}
		};
	}

	public BaseClientRequest<List<CFServiceInstance>> getCreateServicesRequest(final CFServiceInstance[] services) {
		return new BehaviourRequest<List<CFServiceInstance>>(Messages.CloudFoundryServerBehaviour_CREATE_SERVICES,
				behaviour) {
			@Override
			protected List<CFServiceInstance> doRun(CloudFoundryOperations client, SubMonitor progress)
					throws CoreException {

				SubMonitor serviceProgress = SubMonitor.convert(progress, services.length);

				for (CFServiceInstance service : services) {
					serviceProgress.subTask(
							NLS.bind(Messages.CloudFoundryServerBehaviour_CREATING_SERVICE, service.getName()));
					client.createService(CloudServicesUtil.asLegacyV1Service(service));
					serviceProgress.worked(1);
				}
				return CloudServicesUtil.asServiceInstances(client.getServices());
			}
		};
	}

	public BaseClientRequest<CloudApplication> getCloudApplication(final String appName) throws CoreException {

		final String serverId = behaviour.getCloudFoundryServer().getServer().getId();
		return new ApplicationRequest<CloudApplication>(
				NLS.bind(Messages.CloudFoundryServerBehaviour_GET_APPLICATION, appName), behaviour) {
			@Override
			protected CloudApplication doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getApplication(appName);
			}

			@Override
			protected String get503Error(Throwable error) {
				return NLS.bind(Messages.CloudFoundryServerBehaviour_ERROR_GET_APPLICATION_SERVER_503, appName,
						serverId);
			}
		};
	}

	public BaseClientRequest<?> deleteRoute(final List<CloudRoute> routes) throws CoreException {

		if (routes == null || routes.isEmpty()) {
			return null;
		}
		return new BehaviourRequest<Void>("Deleting routes", behaviour) { //$NON-NLS-1$
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				for (CloudRoute route : routes) {
					client.deleteRoute(route.getHost(), route.getDomain().getName());
				}
				return null;

			}
		};
	}

	public BaseClientRequest<?> register(final String email, final String password) {
		return new BehaviourRequest<Void>("Registering account", behaviour) { //$NON-NLS-1$
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.register(email, password);
				return null;
			}
		};
	}

	/** Log-in to server and store token as needed */
	public BaseClientRequest<?> connect() throws CoreException {
		final CloudFoundryServer cloudServer = behaviour.getCloudFoundryServer();
		
		return new BehaviourRequest<Void>("Login to " + behaviour.getCloudFoundryServer().getUrl(), behaviour) { //$NON-NLS-1$
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				OAuth2AccessToken token = client.login();
				if(cloudServer.isSso()) {
					try {
						String tokenValue = new ObjectMapper().writeValueAsString(token);
						cloudServer.setAndSaveToken(tokenValue);
					}
					catch (JsonProcessingException e) {
						CloudFoundryPlugin.logWarning(e.getMessage());
					}
				}

				return null;
			}
		};
	}

	/**
	 * Updates an the number of application instances in the Cloud space, but
	 * does not update the associated application module. Does not restart the
	 * application if the application is already running. The CF server does
	 * allow instance scaling to occur while the application is running.
	 * @param module representing the application. must not be null or empty
	 * @param instanceCount must be 1 or higher.
	 * @param monitor
	 * @throws CoreException if error occurred during or after instances are
	 * updated.
	 */
	public BaseClientRequest<?> updateApplicationInstances(final String appName, final int instanceCount)
			throws CoreException {
		return new AppInStoppedStateAwareRequest<Void>("Updating application instances", behaviour) { //$NON-NLS-1$
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.updateApplicationInstances(appName, instanceCount);
				return null;
			}
		};

	}

	public BaseClientRequest<?> updatePassword(final String newPassword) throws CoreException {
		return new BehaviourRequest<Void>("Updating password", behaviour) { //$NON-NLS-1$

			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.updatePassword(newPassword);
				return null;
			}

		};
	}

	public BaseClientRequest<List<ApplicationLog>> getRecentApplicationLogs(final String appName) throws CoreException {

		return new BehaviourRequest<List<ApplicationLog>>("Getting existing application logs for: " + appName, //$NON-NLS-1$
				behaviour) {

			@Override
			protected List<ApplicationLog> doRun(CloudFoundryOperations client, SubMonitor progress)
					throws CoreException {
				List<ApplicationLog> logs = null;
				if (appName != null) {
					logs = client.getRecentLogs(appName);
				}
				if (logs == null) {
					logs = Collections.emptyList();
				}
				return logs;
			}
		};
	}

	public BaseClientRequest<ApplicationStats> getApplicationStats(final String appName) throws CoreException {
		return new StagingAwareRequest<ApplicationStats>(
				NLS.bind(Messages.CloudFoundryServerBehaviour_APP_STATS, appName), behaviour) {
			@Override
			protected ApplicationStats doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				try {
					return client.getApplicationStats(appName);
				}
				catch (RestClientException ce) {
					// Stats may not be available if app is still stopped or
					// starting
					if (CloudErrorUtil.isAppStoppedStateError(ce) || CloudErrorUtil.getBadRequestException(ce) != null
							|| CloudErrorUtil.is503Error(ce)) {
						return null;
					}
					throw ce;
				}
			}
		};
	}

	public BaseClientRequest<InstancesInfo> getInstancesInfo(final String applicationId) throws CoreException {
		return new StagingAwareRequest<InstancesInfo>(
				NLS.bind(Messages.CloudFoundryServerBehaviour_APP_INFO, applicationId), behaviour) {
			@Override
			protected InstancesInfo doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				try {
					return client.getApplicationInstances(applicationId);
				}
				catch (RestClientException ce) {
					// Info may not be available if app is still stopped or
					// starting
					if (CloudErrorUtil.isAppStoppedStateError(ce)
							|| CloudErrorUtil.getBadRequestException(ce) != null) {
						return null;
					}
					throw ce;
				}
			}
		};
	}

	/**
	 * A relatively fast way to fetch all applications in the active session
	 * Cloud space, that contains basic information about each apps.
	 * <p/>
	 * Information that may be MISSING from the list for each app: service
	 * bindings, mapped URLs, and app instances.
	 * @return request
	 * @throws CoreException
	 */
	public BaseClientRequest<List<CloudApplication>> getBasicApplications() throws CoreException {
		final String serverId = behaviour.getCloudFoundryServer().getServer().getId();
		return new BehaviourRequest<List<CloudApplication>>(
				NLS.bind(Messages.CloudFoundryServerBehaviour_GET_ALL_APPS, serverId), behaviour) {

			@Override
			protected List<CloudApplication> doRun(CloudFoundryOperations client, SubMonitor progress)
					throws CoreException {
				AdditionalV1Operations externalClient = behaviour.getAdditionalV1ClientOperations(progress);
				return externalClient.getBasicApplications();
			}

		};
	}

	public BaseClientRequest<CFV1Application> getCompleteApplication(final CloudApplication application)
			throws CoreException {
		return new ApplicationRequest<CFV1Application>(
				NLS.bind(Messages.CloudFoundryServerBehaviour_GET_APPLICATION, application.getName()), behaviour) {

			@Override
			protected String get503Error(Throwable rce) {
				return rce.getMessage();
			}

			@Override
			protected CFV1Application doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				AdditionalV1Operations externalClient = behaviour.getAdditionalV1ClientOperations(progress);
				return externalClient.getCompleteApplication(application);
			}
		};

	}

	/**
	 * Fetches list of all applications in the Cloud space. No module updates
	 * occur, as this is a low-level API meant to interact with the underlying
	 * client directly. Callers should be responsible to update associated
	 * modules. Note that this may be a long-running operation. If fetching a
	 * known application , it is recommended to call
	 * {@link #getCloudApplication(String, IProgressMonitor)} or
	 * {@link #updateModuleWithBasicCloudInfo(IModule, IProgressMonitor)} as it
	 * may be potentially faster
	 * @param monitor
	 * @return List of all applications in the Cloud space.
	 * @throws CoreException
	 */
	public BaseClientRequest<List<CloudApplication>> getApplications() throws CoreException {

		final String serverId = behaviour.getCloudFoundryServer().getServer().getId();

		final String label = NLS.bind(Messages.CloudFoundryServerBehaviour_GET_ALL_APPS, serverId);

		return new ApplicationRequest<List<CloudApplication>>(label, behaviour) {
			@Override
			protected List<CloudApplication> doRun(CloudFoundryOperations client, SubMonitor progress)
					throws CoreException {
				return client.getApplications();
			}

			@Override
			protected String get503Error(Throwable error) {
				return NLS.bind(Messages.CloudFoundryServerBehaviour_ERROR_GET_APPLICATIONS_SERVER, serverId);
			}

		};
	}

	/**
	 * For testing only.
	 */
	public BaseClientRequest<?> deleteAllApplications() throws CoreException {
		return new BehaviourRequest<Object>("Deleting all applications", behaviour) { //$NON-NLS-1$
			@Override
			protected Object doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.deleteAllApplications();
				return null;
			}
		};
	}

	public BaseClientRequest<List<CFServiceInstance>> getServices() throws CoreException {

		final String label = NLS.bind(Messages.CloudFoundryServerBehaviour_GET_ALL_SERVICES,
				behaviour.getCloudFoundryServer().getServer().getId());
		return new BehaviourRequest<List<CFServiceInstance>>(label, behaviour) {
			@Override
			protected List<CFServiceInstance> doRun(CloudFoundryOperations client, SubMonitor progress)
					throws CoreException {
				return CloudServicesUtil.asServiceInstances(client.getServices());
			}
		};
	}

	public BaseClientRequest<List<CFServiceOffering>> getServiceOfferings() throws CoreException {
		return new BehaviourRequest<List<CFServiceOffering>>("Getting available service options", behaviour) { //$NON-NLS-1$
			@Override
			protected List<CFServiceOffering> doRun(CloudFoundryOperations client, SubMonitor progress)
					throws CoreException {
				return CloudServicesUtil.asServiceOfferings(client.getServiceOfferings());
			}
		};
	}

	public BaseClientRequest<List<CloudDomain>> getDomainsForSpace() throws CoreException {

		return new BehaviourRequest<List<CloudDomain>>(Messages.CloudFoundryServerBehaviour_DOMAINS_FOR_SPACE,
				behaviour) {
			@Override
			protected List<CloudDomain> doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getDomains();
			}
		};
	}

	public BaseClientRequest<List<CloudDomain>> getDomainsFromOrgs() throws CoreException {
		return new BehaviourRequest<List<CloudDomain>>("Getting domains for orgs", behaviour) { //$NON-NLS-1$
			@Override
			protected List<CloudDomain> doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getDomainsForOrg();
			}
		};
	}

	public BaseClientRequest<?> stopApplication(final String message, final CloudFoundryApplicationModule cloudModule) {
		return new BehaviourRequest<Void>(message, behaviour) {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.stopApplication(cloudModule.getDeployedApplicationName());
				return null;
			}
		};
	}

	public BaseClientRequest<String> getFile(final CloudApplication app, final int instanceIndex, final String path,
			boolean isDir) throws CoreException {
		String label = NLS.bind(Messages.CloudFoundryServerBehaviour_FETCHING_FILE, path, app.getName());
		return new FileRequest<String>(label, behaviour) {
			@Override
			protected String doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return client.getFile(app.getName(), instanceIndex, path);
			}
		};
	}

	/**
	 * Check if the 'host' in the 'domainName' is reserved (route owned by us or
	 * someone else), and if not reserve it. Clients are expected to call
	 * {@link #deleteRoute(String, String)} after to remove any unused routes.
	 * 
	 * @see deleteRoute(String, String)
	 * @param host - the Subdomain of the deployed URL
	 * @param domainName - the domainName part of the deployed URL
	 * @param deleteRoute - true to delete the route (if it was created in this
	 * method), false to reserve it and leave deletion to the calling method if
	 * necessary
	 * @return true if the route was created, false otherwise
	 */
	public BaseClientRequest<Boolean> reserveRouteIfAvailable(final String host, final String domainName) {
		return new BehaviourRequest<Boolean>(
				Messages.bind(Messages.CloudFoundryServerBehaviour_CHECKING_HOSTNAME_AVAILABLE, host), behaviour) {
			@Override
			protected Boolean doRun(CloudFoundryOperations client, SubMonitor progress) {

				// Check if the route can be added. If successful, then it is
				// not taken.
				try {
					client.addRoute(host, domainName);
				}
				catch (CloudFoundryException t) {
					// addRoute will throw a CloudFoundryException indicating
					// the route is taken; but we should also return false for
					// any other
					// exceptions that might be thrown here.
					return false;
				}

				return true;
			}
		};
	}

	/**
	 * Delete the route.
	 * 
	 * @see checkHostTaken(String, String, boolean) {
	 * @param host - the Subdomain of the deployed URL
	 * @param domainName - the domainName part of the deployed URL
	 * @return
	 */
	public BaseClientRequest<Void> deleteRoute(final String host, final String domainName) {
		return new BehaviourRequest<Void>(
				Messages.bind(Messages.CloudFoundryServerBehaviour_CLEANING_UP_RESERVED_HOSTNAME, host), behaviour) {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				client.deleteRoute(host, domainName);
				return null;
			}
		};
	}

	public BaseClientRequest<List<String>> getBuildpacks() {
		return new BehaviourRequest<List<String>>(Messages.ClientRequestFactory_BUILDPACKS, behaviour) {
			@Override
			protected List<String> doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				return BuildpackSupport.create(client, getCloudInfo(), getCloudServer().getProxyConfiguration(), 
						behaviour.getCloudFoundryServer(), getCloudServer().isSelfSigned()).getBuildpacks();
			}
		};
	}

	public CFInfo getCloudInfo() throws CoreException {
		// cache the info to avoid frequent network connection to Cloud Foundry.
		if (cachedInfo == null) {
			CloudFoundryServer cloudServer = behaviour.getCloudFoundryServer();
			cachedInfo = new CFInfo(new CloudCredentials(cloudServer.getUsername(), cloudServer.getPassword()),
					cloudServer.getUrl(), cloudServer.getProxyConfiguration(), cloudServer.isSelfSigned());
		}
		return cachedInfo;
	}

	public boolean supportsSsh() {
		return false;
	}

	public AdditionalV1Operations createAdditionalV1Operations(CloudFoundryOperations client, CloudSpace sessionSpace,
			CFInfo cloudInfo, HttpProxyConfiguration httpProxyConfiguration, boolean selfSigned) throws CoreException {
		return new AdditionalV1Operations(client, sessionSpace, getCloudInfo(), httpProxyConfiguration, behaviour.getCloudFoundryServer(), selfSigned);
	}

}
