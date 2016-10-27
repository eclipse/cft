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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.cft.server.core.CFServiceOffering;
import org.eclipse.cft.server.core.EnvironmentVariable;
import org.eclipse.cft.server.core.ISshClientSupport;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.CloudServicesUtil;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.cft.server.core.internal.client.diego.CFInfo;
import org.eclipse.cft.server.core.internal.log.CFApplicationLogListener;
import org.eclipse.cft.server.core.internal.log.CFStreamingLogToken;
import org.eclipse.cft.server.core.internal.log.CFV1StreamingLogToken;
import org.eclipse.cft.server.core.internal.log.V1AppLogUtil;
import org.eclipse.cft.server.core.internal.spaces.CloudOrgsAndSpaces;
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
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * A request factory for requests based on the v1 CF Java client.
 */
public class ClientRequestFactory {

	protected final CloudFoundryServer cloudServer;

	protected CFInfo cachedInfo;


	protected CloudServerCFClient cfClient;

	public ClientRequestFactory(CloudFoundryServer cloudSever, CloudServerCFClient cfClient) throws CoreException {
		this.cloudServer = cloudSever;
		this.cfClient = cfClient;
	}

	public ClientRequest<?> getUpdateApplicationMemoryRequest(final CloudFoundryApplicationModule appModule,
			final int memory) {
		return new AppInStoppedStateAwareRequest<Void>(cloudServer, cfClient, NLS
				.bind(Messages.CloudFoundryServerBehaviour_UPDATE_APP_MEMORY, appModule.getDeployedApplicationName())) {
			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException {
				client.updateApplicationMemory(appModule.getDeployedApplicationName(), memory);
				return null;
			}
		};
	}

	public ClientRequest<?> updateApplicationDiego(final CloudFoundryApplicationModule appModule, final boolean diego) {

		String message;
		if (diego) {
			message = NLS.bind(Messages.CloudFoundryServerBehaviour_ENABLING_DIEGO,
					appModule.getDeployedApplicationName());
		}
		else {
			message = NLS.bind(Messages.CloudFoundryServerBehaviour_DISABLING_DIEGO,
					appModule.getDeployedApplicationName());
		}

		return new AppInStoppedStateAwareRequest<Void>(cloudServer, cfClient, message) {
			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException {
				client.updateApplicationDiego(appModule.getDeployedApplicationName(), diego);
				return null;
			}
		};
	}

	public ClientRequest<?> updateApplicationEnableSsh(final CloudFoundryApplicationModule appModule,
			final boolean enableSsh) {
		String message;
		if (enableSsh) {
			message = NLS.bind(Messages.CloudFoundryServerBehaviour_ENABLING_SSH,
					appModule.getDeployedApplicationName());
		}
		else {
			message = NLS.bind(Messages.CloudFoundryServerBehaviour_DISABLING_SSH,
					appModule.getDeployedApplicationName());
		}

		return new AppInStoppedStateAwareRequest<Void>(cloudServer, cfClient, message) {
			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException {
				client.updateApplicationEnableSsh(appModule.getDeployedApplicationName(), enableSsh);
				return null;
			}
		};
	}

	public ClientRequest<List<CloudRoute>> getRoutes(final String domainName) throws CoreException {

		return new V1ClientRequest<List<CloudRoute>>(cloudServer, cfClient, NLS.bind(Messages.ROUTES, domainName)) {
			@Override
			protected List<CloudRoute> runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {
				return client.getRoutes(domainName);
			}
		};
	}

	public ClientRequest<StartingInfo> restartApplication(final String appName, final String opLabel)
			throws CoreException {
		return new V1ClientRequest<StartingInfo>(cloudServer, cfClient, opLabel) {
			@Override
			protected StartingInfo runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException, OperationCanceledException {
				return client.restartApplication(appName);
			}
		};
	}

	public ClientRequest<?> deleteApplication(final String appName) {
		return new V1ClientRequest<Void>(cloudServer, cfClient, NLS.bind(Messages.DELETING_MODULE, appName)) {
			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException {
				CloudFoundryPlugin.logInfo("ClientRequestFactory.deleteApplication(...): appName:" + appName);
				client.deleteApplication(appName);
				return null;
			}
		};
	}

	public ClientRequest<?> getUpdateAppUrlsRequest(final String appName, final List<String> urls) {
		return new AppInStoppedStateAwareRequest<Void>(cloudServer, cfClient,
				NLS.bind(Messages.CloudFoundryServerBehaviour_UPDATE_APP_URLS, appName)) {
			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException {

				// Look up the existing urls locally first to avoid a client
				// call
				CloudFoundryApplicationModule existingAppModule = cloudServer.getExistingCloudModule(appName);

				List<String> oldUrls = existingAppModule != null && existingAppModule.getDeploymentInfo() != null
						? existingAppModule.getDeploymentInfo().getUris() : null;

				if (oldUrls == null) {
					oldUrls = getCloudApplication(appName).run(monitor).getUris();
				}

				client.updateApplicationUris(appName, urls);

				if (existingAppModule != null) {
					ServerEventHandler.getDefault()
							.fireServerEvent(new AppUrlChangeEvent(cloudServer, CloudServerEvent.EVENT_APP_URL_CHANGED,
									existingAppModule.getLocalModule(), Status.OK_STATUS, oldUrls, urls));

				}
				return null;
			}
		};
	}

	public ClientRequest<?> getUpdateServicesRequest(final String appName, final List<String> services) {
		return new StagingAwareRequest<Void>(cloudServer, cfClient,
				NLS.bind(Messages.CloudFoundryServerBehaviour_UPDATE_SERVICE_BINDING, appName)) {
			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException {
				client.updateApplicationServices(appName, services);
				return null;
			}
		};
	}

	public ClientRequest<Void> getUpdateEnvVarRequest(final String appName, final List<EnvironmentVariable> variables) {
		final String label = NLS.bind(Messages.CloudFoundryServerBehaviour_UPDATE_ENV_VARS, appName);
		return new V1ClientRequest<Void>(cloudServer, cfClient, label) {

			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException {
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

	public ClientRequest<List<CFServiceInstance>> getDeleteServicesRequest(final List<String> services) {
		return new V1ClientRequest<List<CFServiceInstance>>(cloudServer, cfClient,
				Messages.CloudFoundryServerBehaviour_DELETE_SERVICES) {
			@Override
			protected List<CFServiceInstance> runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {

				SubMonitor serviceProgress = SubMonitor.convert(monitor, services.size());

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
						List<CloudApplication> apps = getApplications().run(monitor);
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

	public ClientRequest<List<CFServiceInstance>> getCreateServicesRequest(final CFServiceInstance[] services) {
		return new V1ClientRequest<List<CFServiceInstance>>(cloudServer, cfClient,
				Messages.CloudFoundryServerBehaviour_CREATE_SERVICES) {
			@Override
			protected List<CFServiceInstance> runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {

				SubMonitor serviceProgress = SubMonitor.convert(monitor, services.length);

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

	public ClientRequest<CloudApplication> getCloudApplication(final String appName) throws CoreException {

		final String serverId = cloudServer.getServer().getId();
		return new ApplicationRequest<CloudApplication>(cloudServer, cfClient,
				NLS.bind(Messages.CloudFoundryServerBehaviour_GET_APPLICATION, appName)) {
			@Override
			protected CloudApplication runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {
				return client.getApplication(appName);
			}

			@Override
			protected String get503Error(Throwable error) {
				return NLS.bind(Messages.CloudFoundryServerBehaviour_ERROR_GET_APPLICATION_SERVER_503, appName,
						serverId);
			}
		};
	}

	public ClientRequest<?> deleteRoute(final List<CloudRoute> routes) throws CoreException {

		if (routes == null || routes.isEmpty()) {
			return null;
		}
		return new V1ClientRequest<Void>(cloudServer, cfClient, "Deleting routes") { //$NON-NLS-1$
			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException {
				for (CloudRoute route : routes) {
					client.deleteRoute(route.getHost(), route.getDomain().getName());
				}
				return null;

			}
		};
	}

	public ClientRequest<?> register(final String email, final String password) {
		return new V1ClientRequest<Void>(cloudServer, cfClient, "Registering account") { //$NON-NLS-1$
			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException {
				client.register(email, password);
				return null;
			}
		};
	}

	/** Log-in to server and store token as needed */
	public ClientRequest<CFAccessToken> connect() throws CoreException {

		return new V1ClientRequest<CFAccessToken>(cloudServer, cfClient, "Login to " + cloudServer.getUrl()) { //$NON-NLS-1$
			@Override
			protected CFAccessToken runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {
				OAuth2AccessToken token = client.login();
				if (cloudServer.isSso()) {
					try {
						String tokenValue = new ObjectMapper().writeValueAsString(token);
						cloudServer.setAndSaveToken(tokenValue);
					}
					catch (JsonProcessingException e) {
						CloudFoundryPlugin.logWarning(e.getMessage());
					}
				}

				return new V1AccessToken(token);
			}
		};
	}

	public ClientRequest<CFStreamingLogToken> addApplicationLogListener(final String appName,
			final CFApplicationLogListener listener) {
		if (appName != null && listener != null) {
			return new V1ClientRequest<CFStreamingLogToken>(cloudServer, cfClient,
					Messages.ADDING_APPLICATION_LOG_LISTENER) {
				@Override
				protected CFStreamingLogToken runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
						throws CoreException {
					StreamingLogToken token = client.streamLogs(appName, new ApplicationLogListener() {

						@Override
						public void onMessage(ApplicationLog log) {
							listener.onMessage(V1AppLogUtil.getLogFromV1(log));
						}

						@Override
						public void onError(Throwable exception) {
							listener.onError(exception);
						}

						@Override
						public void onComplete() {
							listener.onComplete();
						}
					});
					if (token != null) {
						return new CFV1StreamingLogToken(token);
					}
					return null;
				}

			};

		}
		return null;
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
	public ClientRequest<?> updateApplicationInstances(final String appName, final int instanceCount)
			throws CoreException {
		return new AppInStoppedStateAwareRequest<Void>(cloudServer, cfClient, "Updating application instances") { //$NON-NLS-1$
			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException {
				client.updateApplicationInstances(appName, instanceCount);
				return null;
			}
		};

	}

	public ClientRequest<?> updatePassword(final String newPassword) throws CoreException {
		return new V1ClientRequest<Void>(cloudServer, cfClient, "Updating password") { //$NON-NLS-1$

			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException {
				client.updatePassword(newPassword);
				return null;
			}

		};
	}

	public ClientRequest<List<ApplicationLog>> getRecentApplicationLogs(final String appName) throws CoreException {

		return new V1ClientRequest<List<ApplicationLog>>(cloudServer, cfClient,
				"Getting existing application logs for: " + appName //$NON-NLS-1$
		) {

			@Override
			protected List<ApplicationLog> runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
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

	public ClientRequest<ApplicationStats> getApplicationStats(final String appName) throws CoreException {
		return new StagingAwareRequest<ApplicationStats>(cloudServer, cfClient,
				NLS.bind(Messages.CloudFoundryServerBehaviour_APP_STATS, appName)) {
			@Override
			protected ApplicationStats runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {
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

	public ClientRequest<InstancesInfo> getInstancesInfo(final String applicationId) throws CoreException {
		return new StagingAwareRequest<InstancesInfo>(cloudServer, cfClient,
				NLS.bind(Messages.CloudFoundryServerBehaviour_APP_INFO, applicationId)) {
			@Override
			protected InstancesInfo runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {
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
	public ClientRequest<List<CloudApplication>> getBasicApplications() throws CoreException {
		final String serverId = cloudServer.getServer().getId();
		return new V1ClientRequest<List<CloudApplication>>(cloudServer, cfClient,
				NLS.bind(Messages.CloudFoundryServerBehaviour_GET_ALL_APPS, serverId)) {

			@Override
			protected List<CloudApplication> runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {
				AdditionalV1Operations additionalSupport = cfClient.getAdditionalV1Operations(monitor);
				return additionalSupport.getBasicApplications();
			}

		};
	}

	public ClientRequest<CFV1Application> getCompleteApplication(final CloudApplication application)
			throws CoreException {
		return new ApplicationRequest<CFV1Application>(cloudServer, cfClient,
				NLS.bind(Messages.CloudFoundryServerBehaviour_GET_APPLICATION, application.getName())) {

			@Override
			protected String get503Error(Throwable rce) {
				return rce.getMessage();
			}

			@Override
			protected CFV1Application runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {
				AdditionalV1Operations additionalSupport = cfClient.getAdditionalV1Operations(monitor);
				return additionalSupport.getCompleteApplication(application);
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
	public ClientRequest<List<CloudApplication>> getApplications() throws CoreException {

		final String serverId = cloudServer.getServer().getId();

		final String label = NLS.bind(Messages.CloudFoundryServerBehaviour_GET_ALL_APPS, serverId);

		return new ApplicationRequest<List<CloudApplication>>(cloudServer, cfClient, label) {
			@Override
			protected List<CloudApplication> runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
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
	public ClientRequest<?> deleteAllApplications() throws CoreException {
		return new V1ClientRequest<Object>(cloudServer, cfClient, "Deleting all applications") { //$NON-NLS-1$
			@Override
			protected Object runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {
				client.deleteAllApplications();
				return null;
			}
		};
	}

	public ClientRequest<List<CFServiceInstance>> getServices() throws CoreException {

		final String label = NLS.bind(Messages.CloudFoundryServerBehaviour_GET_ALL_SERVICES,
				cloudServer.getServer().getId());
		return new V1ClientRequest<List<CFServiceInstance>>(cloudServer, cfClient, label) {
			@Override
			protected List<CFServiceInstance> runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {
				return CloudServicesUtil.asServiceInstances(client.getServices());
			}
		};
	}

	public ClientRequest<List<CFServiceOffering>> getServiceOfferings() throws CoreException {
		return new V1ClientRequest<List<CFServiceOffering>>(cloudServer, cfClient,
				"Getting available service options") { //$NON-NLS-1$
			@Override
			protected List<CFServiceOffering> runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {
				return CloudServicesUtil.asServiceOfferings(client.getServiceOfferings());
			}
		};
	}

	public ClientRequest<List<CloudDomain>> getDomainsForSpace() throws CoreException {

		return new V1ClientRequest<List<CloudDomain>>(cloudServer, cfClient,
				Messages.CloudFoundryServerBehaviour_DOMAINS_FOR_SPACE) {
			@Override
			protected List<CloudDomain> runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {
				return client.getDomains();
			}
		};
	}

	public ClientRequest<List<CloudDomain>> getDomainsFromOrgs() throws CoreException {
		return new V1ClientRequest<List<CloudDomain>>(cloudServer, cfClient, "Getting domains for orgs") { //$NON-NLS-1$
			@Override
			protected List<CloudDomain> runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {
				return client.getDomainsForOrg();
			}
		};
	}

	public ClientRequest<?> stopApplication(final String message, final CloudFoundryApplicationModule cloudModule) {
		return new V1ClientRequest<Void>(cloudServer, cfClient, message) {
			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException {
				client.stopApplication(cloudModule.getDeployedApplicationName());
				return null;
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
	public ClientRequest<Boolean> reserveRouteIfAvailable(final String host, final String domainName) {
		return new V1ClientRequest<Boolean>(cloudServer, cfClient,
				Messages.bind(Messages.CloudFoundryServerBehaviour_CHECKING_HOSTNAME_AVAILABLE, host)) {
			@Override
			protected Boolean runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {

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
	public ClientRequest<Void> deleteRoute(final String host, final String domainName) {
		return new V1ClientRequest<Void>(cloudServer, cfClient,
				Messages.bind(Messages.CloudFoundryServerBehaviour_CLEANING_UP_RESERVED_HOSTNAME, host)) {
			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException {
				client.deleteRoute(host, domainName);
				return null;
			}
		};
	}

	public ClientRequest<List<String>> getBuildpacks() {
		return new V1ClientRequest<List<String>>(cloudServer, cfClient, Messages.ClientRequestFactory_BUILDPACKS) {
			@Override
			protected List<String> runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {
				return BuildpackSupport.create(client, getCloudInfo(), getCloudServer().getProxyConfiguration(),
						getCloudServer(), getCloudServer().isSelfSigned()).getBuildpacks();
			}

		};
	}

	public ClientRequest<Void> createApplication(final String appName, final Staging staging, final int memory,
			final List<String> uris, final List<String> services) {

		return new V1ClientRequest<Void>(cloudServer, cfClient,
				NLS.bind(Messages.ClientRequestFactory_CREATE_APP, appName)) {
			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException {
				client.createApplication(appName, staging, memory, uris, services);
				return null;
			}

		};
	}

	public ClientRequest<Void> uploadApplication(final String appName, final ApplicationArchive v1ArchiveWrapper,
			final UploadStatusCallback uploadStatusCallback) {
		return new V1ClientRequest<Void>(cloudServer, cfClient,
				NLS.bind(Messages.ClientRequestFactory_UPLOADING_APP, appName)) {
			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException {
				try {
					client.uploadApplication(appName, v1ArchiveWrapper, uploadStatusCallback);
				}
				catch (IOException e) {
					throw CloudErrorUtil.toCoreException(e);
				}
				return null;
			}
		};

	}

	public ClientRequest<Void> stopApplication(final CloudFoundryApplicationModule cloudModule,
			String stoppingApplicationMessage) {
		return new V1ClientRequest<Void>(cloudServer, cfClient, stoppingApplicationMessage) {
			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor monitor) throws CoreException {
				client.stopApplication(cloudModule.getDeployedApplicationName());
				return null;
			}

		};
	}

	private CFInfo getCloudInfo() throws CoreException {
		return cfClient.getCloudInfo();
	}

	public ClientRequest<String> getFile(final CloudApplication app, final int instanceIndex, final String path,
			final boolean isDir) throws CoreException {

		// If ssh is not supported, try the default legacy file fetching

		String label = NLS.bind(Messages.CloudFoundryServerBehaviour_FETCHING_FILE, path, app.getName());
		return new V1ClientRequest<String>(cloudServer, cfClient, label) {
			@Override
			protected String runV1Request(CloudFoundryOperations client, IProgressMonitor progress)
					throws CoreException {

				if (path == null) {
					return null;
				}

				if (!cfClient.supportsSsh()) {
					return client.getFile(app.getName(), instanceIndex, path);
				}

				ISshClientSupport ssh = cloudServer.getBehaviour().getSshClientSupport(progress);
				if (ssh == null) {
					return null;
				}

				Session session = ssh.connect(app.getName(), instanceIndex, cloudServer.getServer(), progress);

				String command = isDir ? "ls -p " + path //$NON-NLS-1$
						// Basic work-around to scp which doesn't appear to work
						// well. Returns empty content for existing files.
						: "cat " + path; //$NON-NLS-1$

				try {
					Channel channel = session.openChannel("exec"); //$NON-NLS-1$
					((ChannelExec) channel).setCommand(command);

					return getContent(channel);
				}
				catch (JSchException e) {
					throw CloudErrorUtil.toCoreException(e);
				}
				finally {
					session.disconnect();
				}
			}
		};
	}

	protected String getContent(Channel channel) throws CoreException {
		InputStream in = null;
		OutputStream outStream = null;
		try {
			in = channel.getInputStream();
			channel.connect();

			if (in != null) {

				ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
				outStream = new BufferedOutputStream(byteArrayOut);
				byte[] buffer = new byte[4096];
				int bytesRead = -1;

				while ((bytesRead = in.read(buffer)) != -1) {
					outStream.write(buffer, 0, bytesRead);
				}
				outStream.flush();
				byteArrayOut.flush();

				return byteArrayOut.toString();
			}
		}
		catch (IOException e) {
			throw CloudErrorUtil.toCoreException(e);
		}
		catch (JSchException e) {
			throw CloudErrorUtil.toCoreException(e);
		}
		finally {
			channel.disconnect();
			try {
				if (in != null) {
					in.close();
				}
				if (outStream != null) {
					outStream.close();
				}
			}
			catch (IOException e) {
				// Don't prevent any operation from completing if streams don't
				// close. Just log error
				CloudFoundryPlugin.logError(e);
			}
		}
		return null;
	}

	public ClientRequest<CloudOrgsAndSpaces> getCloudSpaces() {
		V1ClientRequest<CloudOrgsAndSpaces> request = new V1ClientRequest<CloudOrgsAndSpaces>(cloudServer, cfClient,
				Messages.GETTING_ORGS_AND_SPACES) {

			@Override
			protected CloudOrgsAndSpaces runV1Request(CloudFoundryOperations client, IProgressMonitor monitor)
					throws CoreException {
				List<CloudSpace> foundSpaces = client.getSpaces();
				if (foundSpaces != null && !foundSpaces.isEmpty()) {
					List<CloudSpace> actualSpaces = new ArrayList<CloudSpace>(foundSpaces);
					CloudOrgsAndSpaces orgsAndSpaces = new CloudOrgsAndSpaces(actualSpaces);
					return orgsAndSpaces;
				}

				return null;
			}
		};
		return request;
	}

	abstract class BehaviourRequest<T> extends V1ClientRequest<T> {

		public BehaviourRequest(CloudFoundryServer cloudServer, String label) {
			super(cloudServer, ClientRequestFactory.this.cfClient, label);
		}

	}

}
