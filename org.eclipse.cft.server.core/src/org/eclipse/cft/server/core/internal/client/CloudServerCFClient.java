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
 * Contributors: Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.client;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.cft.server.core.CFServiceOffering;
import org.eclipse.cft.server.core.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour.CancellableRequestThread;
import org.eclipse.cft.server.core.internal.client.diego.CFInfo;
import org.eclipse.cft.server.core.internal.log.CFApplicationLogListener;
import org.eclipse.cft.server.core.internal.log.CFStreamingLogToken;
import org.eclipse.cft.server.core.internal.log.CloudLog;
import org.eclipse.cft.server.core.internal.log.V1AppLogUtil;
import org.eclipse.cft.server.core.internal.spaces.CloudFoundrySpace;
import org.eclipse.cft.server.core.internal.spaces.CloudOrgsAndSpaces;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;

/**
 * 
 * Contains all client-related operations, like application and service
 * management, and any other operation that requires a connection to a Cloud
 * Foundry server, and that are associated with an existing CloudFoundryServer
 * instance.
 * 
 * <p/>
 * This client is specifically created in association with an existing
 * CloudFoundryServer instance, and it is intended to be the session client for
 * that server instance
 * <p/>
 * To create a standalone client not associated with a CloudFoundryServer
 * instance, user {@link CFClient} instead
 *
 */
public class CloudServerCFClient extends CFClient {

	private CloudFoundryServer cloudServer;

	private ClientRequestFactory requestFactory;

	private AdditionalV1Operations additionalV1Operations;

	private CFInfo cachedInfo;

	private CloudBehaviourOperations cloudBehaviourOperations;

	public CloudServerCFClient(CloudFoundryOperations v1Operations, CloudFoundryServer cloudServer) throws CoreException {
		super(v1Operations);
		this.cloudServer = cloudServer;
		this.requestFactory = new ClientRequestFactory(cloudServer, this);
		// This may not belong here, but it is tied to the client, and to ensure
		// the behaviour
		// uses the same client associated with the cloud server instance, the
		// client owns
		// the cloud behaviour operations.
		this.cloudBehaviourOperations = new CloudBehaviourOperations(cloudServer.getBehaviour(), this, this.requestFactory);
	}

	public ClientRequestFactory getClientRequestFactory() {
		return requestFactory;
	}

	public AdditionalV1Operations getAdditionalV1Operations(IProgressMonitor monitor) throws CoreException {

		if (additionalV1Operations != null) {
			return additionalV1Operations;
		}

		HttpProxyConfiguration httpProxyConfiguration = cloudServer.getProxyConfiguration();
		CloudSpace sessionSpace = null;
		CloudFoundrySpace storedSpace = cloudServer.getCloudFoundrySpace();
		boolean selfSigned = cloudServer.isSelfSigned();

		// Fetch the session spac if it is not available from the server, as it
		// is required for the additional v1 operations
		if (storedSpace != null) {
			sessionSpace = storedSpace.getSpace();
			if (sessionSpace == null && storedSpace.getOrgName() != null && storedSpace.getSpaceName() != null) {
				CloudOrgsAndSpaces spacesFromCF = getCloudSpaces(monitor);
				if (spacesFromCF != null) {
					sessionSpace = spacesFromCF.getSpace(storedSpace.getOrgName(), storedSpace.getSpaceName());
				}
			}
		}

		if (sessionSpace == null) {
			throw CloudErrorUtil.toCoreException("No Cloud space resolved for " + cloudServer.getServer().getId() //$NON-NLS-1$
					+ ". Please verify that the server is connected and refreshed and try again."); //$NON-NLS-1$
		}
		additionalV1Operations = new AdditionalV1Operations(v1Operations, sessionSpace, getCloudInfo(),
				httpProxyConfiguration, cloudServer, selfSigned);

		return additionalV1Operations;
	}

	public void unRegisterRestLogListener(CFRestLogListener logListener) {
		// TODO Auto-generated method stub

	}

	public void registerRestLogListener(CFRestLogListener logListener) {
		// TODO

	}

	public List<String> getBuildpacks(IProgressMonitor monitor) throws CoreException {
		return runRequest(requestFactory.getBuildpacks(), monitor);
	}

	public void connect(IProgressMonitor monitor) throws CoreException {
		runRequest(requestFactory.connect(), monitor);
	}

	public List<CloudDomain> getDomainsFromOrgs(IProgressMonitor monitor) throws CoreException {
		return runRequest(requestFactory.getDomainsFromOrgs(), monitor);
	}

	public List<CloudDomain> getDomainsForSpace(IProgressMonitor monitor) throws CoreException {
		return runRequest(requestFactory.getDomainsForSpace(), monitor);
	}

	public List<CloudApplication> getApplications(IProgressMonitor monitor) throws CoreException {
		return runRequest(requestFactory.getApplications(), monitor);
	}

	public List<CloudApplication> getBasicApplications(IProgressMonitor monitor) throws CoreException {
		return runRequest(requestFactory.getBasicApplications(), monitor);
	}

	public CFV1Application getCompleteApplication(CloudApplication application, IProgressMonitor monitor)
			throws CoreException {
		return runRequest(requestFactory.getCompleteApplication(application), monitor);
	}

	public ApplicationStats getApplicationStats(String appName, IProgressMonitor monitor) throws CoreException {
		return runRequest(requestFactory.getApplicationStats(appName), monitor);
	}

	public InstancesInfo getInstancesInfo(String applicationId, IProgressMonitor monitor) throws CoreException {
		return runRequest(requestFactory.getInstancesInfo(applicationId), monitor);
	}

	public String getFile(CloudApplication app, int instanceIndex, String path, boolean isDir, IProgressMonitor monitor)
			throws CoreException {
		return runRequest(requestFactory.getFile(app, instanceIndex, path, isDir), monitor);
	}

	public List<CFServiceOffering> getServiceOfferings(IProgressMonitor monitor) throws CoreException {
		return runRequest(requestFactory.getServiceOfferings(), monitor);
	}

	public void deleteAllApplications(IProgressMonitor monitor) throws CoreException {
		runRequest(requestFactory.deleteAllApplications(), monitor);
	}

	public List<CFServiceInstance> getServices(IProgressMonitor monitor) throws CoreException {
		return runRequest(requestFactory.getServices(), monitor);
	}

	public List<CloudLog> getRecentApplicationLogs(String appName, IProgressMonitor monitor) throws CoreException {
		List<ApplicationLog> v1logs = runRequest(requestFactory.getRecentApplicationLogs(appName), monitor);
		List<CloudLog> cftLogs = new ArrayList<CloudLog>();
		if (v1logs != null) {
			for (ApplicationLog log : v1logs) {
				cftLogs.add(V1AppLogUtil.getLogFromV1(log));
			}
		}
		return cftLogs;
	}

	public void updateApplicationInstances(String appName, int instanceCount, IProgressMonitor monitor)
			throws CoreException {
		runRequest(requestFactory.updateApplicationInstances(appName, instanceCount), monitor);
	}

	public void updatePassword(String newPassword, IProgressMonitor monitor) throws CoreException {
		runRequest(requestFactory.updatePassword(newPassword), monitor);
	}

	public void register(String email, String password, IProgressMonitor monitor) throws CoreException {
		runRequest(requestFactory.register(email, password), monitor);
	}

	public void deleteApplication(String appName, IProgressMonitor monitor) throws CoreException {
		runRequest(requestFactory.deleteApplication(appName), monitor);
	}

	public CloudApplication getV1CloudApplication(String appName, IProgressMonitor monitor) throws CoreException {
		return runRequest(requestFactory.getCloudApplication(appName), monitor);
	}

	public List<CloudRoute> getRoutes(String domainName, IProgressMonitor monitor) throws CoreException {
		ClientRequest<List<CloudRoute>> request = requestFactory.getRoutes(domainName);

		CancellableRequestThread<List<CloudRoute>> t = new CancellableRequestThread<List<CloudRoute>>(request, monitor);
		return t.runAndWaitForCompleteOrCancelled();
	}

	public void deleteRoute(List<CloudRoute> routes, IProgressMonitor monitor) throws CoreException {
		runRequest(requestFactory.deleteRoute(routes), monitor);
	}

	public void deleteRoute(String host, String domainName, IProgressMonitor monitor) throws CoreException {
		runRequest(requestFactory.deleteRoute(host, domainName), monitor);
	}

	public CloudOrgsAndSpaces getCloudSpaces(IProgressMonitor monitor) throws CoreException {
		return runRequest(requestFactory.getCloudSpaces(), monitor);
	}

	public boolean reserveRouteIfAvailable(String host, String domainName, IProgressMonitor monitor) {
		ClientRequest<Boolean> request = getClientRequestFactory().reserveRouteIfAvailable(host, domainName);

		CancellableRequestThread<Boolean> t = new CancellableRequestThread<Boolean>(request, monitor);
		Boolean result = t.runAndWaitForCompleteOrCancelled();

		if (result != null) {
			return result;
		}

		return false;
	}

	public CFStreamingLogToken addApplicationLogListener(String appName, CFApplicationLogListener listener) {
		if (appName != null && listener != null) {
			try {
				return runRequest(requestFactory.addApplicationLogListener(appName, listener));
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(NLS.bind(Messages.ERROR_APPLICATION_LOG_LISTENER, appName, e.getMessage()),
						e);
			}
		}
		return null;
	}

	public void createApplication(String appName, Staging staging, int memory, List<String> uris, List<String> services,
			IProgressMonitor monitor) throws CoreException {
		runRequest(requestFactory.createApplication(appName, staging, memory, uris, services), monitor);

	}

	public StartingInfo restartApplication(String deploymentName, String startLabel, IProgressMonitor monitor)
			throws CoreException {
		return runRequest(requestFactory.restartApplication(deploymentName, startLabel), monitor);
	}

	public void uploadApplication(String appName, ApplicationArchive v1ArchiveWrapper,
			UploadStatusCallback uploadStatusCallback, IProgressMonitor monitor) throws CoreException {
		runRequest(requestFactory.uploadApplication(appName, v1ArchiveWrapper, uploadStatusCallback), monitor);
	}

	public void stopApplication(CloudFoundryApplicationModule cloudModule, String stoppingApplicationMessage,
			IProgressMonitor monitor) throws CoreException {
		runRequest(requestFactory.stopApplication(cloudModule, stoppingApplicationMessage), monitor);
	}

	public void updateEnvVarRequest(String appName, List<EnvironmentVariable> variables, IProgressMonitor monitor)
			throws CoreException {
		runRequest(requestFactory.getUpdateEnvVarRequest(appName, variables), monitor);
	}

	public CloudBehaviourOperations getBehaviourOperations() {
		return cloudBehaviourOperations;
	}

	public CFInfo getCloudInfo() throws CoreException {
		// cache the info to avoid frequent network connection to Cloud Foundry.
		if (cachedInfo == null) {
			cachedInfo = new CFInfo(new CloudCredentials(cloudServer.getUsername(), cloudServer.getPassword()),
					cloudServer.getUrl(), cloudServer.getProxyConfiguration(), cloudServer.isSelfSigned());
		}
		return cachedInfo;
	}

	public boolean supportsSsh() {
		try {
			CFInfo info = getCloudInfo();
			return info != null && info.getSshClientId() != null && info.getSshHost() != null
					&& info.getSshHost().getHost() != null && info.getSshHost().getFingerPrint() != null;
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return false;
	}

	protected <T> T runRequest(ClientRequest<T> request, IProgressMonitor monitor) throws CoreException {
		return request.run(monitor);
	}

	protected <T> T runRequest(ClientRequest<T> request) throws CoreException {
		return request.run(new NullProgressMonitor());
	}
}
