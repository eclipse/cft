/*******************************************************************************
 * Copyright (c) 2016 Pivotal Software, Inc. 
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

import java.net.MalformedURLException;
import java.net.URL;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryLoginHandler;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudUtil;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.spaces.CloudFoundrySpace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Internal use only
 * <p/>
 * Contains additional support like a request factory that is not defined in
 * cloud server API.
 * @see ClientRequestFactory
 *
 */
public class V1ClientManager implements CFClientManager {

	public V1ClientManager() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.cft.server.core.internal.CFClientFactory#supports(java.lang.
	 * String)
	 */
	@Override
	public boolean supports(String serverUrl) {
		return serverUrl != null;
	}

	/**
	 * Creates a new client to the specified server URL using the given
	 * credentials. This does NOT connect the client to the server, nor does it
	 * set the client as the session client for the server behaviour. The
	 * session client is set indirectly via {@link #connect(IProgressMonitor)}
	 * @param serverURL server to connect to. Must NOT be null.
	 * @param credentials must not be null.
	 * @param cloudSpace optional. Can be null, as a client can be created
	 * without specifying an org/space (e.g. a client can be created for the
	 * purpose of looking up all the orgs/spaces in a server)
	 * @param selfSigned true if connecting to a server with self signed
	 * certificate. False otherwise
	 * @return non-null client.
	 * @throws CoreException if failed to create client.
	 */
	@Override
	public CFClient createClient(String url, CloudCredentials credentials, CloudFoundrySpace cloudSpace,
			boolean selfSigned, IProgressMonitor monitor) throws CoreException {

		SubMonitor progress = SubMonitor.convert(monitor);
		progress.beginTask(Messages.CONNECTING, IProgressMonitor.UNKNOWN);

		try {
			URL serverUrl = toUrl(url);

			// What is this port used for?
			int port = serverUrl.getPort();
			if (port == -1) {
				port = serverUrl.getDefaultPort();
			}

			// If no cloud space is specified, use appropriate client factory
			// API to create a non-space client
			// NOTE that using a space API with null org and space will result
			// in errors as that API will
			// expect valid org and space values.
			CloudFoundryOperations v1Client = cloudSpace != null
					? createV1Client(serverUrl, credentials, cloudSpace, selfSigned)
					: createV1Client(serverUrl, credentials, selfSigned);
			return new CFClient(v1Client);
		}
		catch (RuntimeException t) {
			throw CloudErrorUtil.checkServerCommunicationError(t);
		}
		finally {
			progress.done();
		}

	}

	/**
	 * Creates a standalone client with no association with a server behaviour.
	 * This is used only for connecting to a Cloud Foundry server for credential
	 * verification. The session client for the server behaviour is created when
	 * the latter is created
	 * @param userName
	 * @param password
	 * @param server url
	 * 
	 * @param selfSigned true if connecting to self-signing server. False
	 * otherwise
	 * @return
	 * @throws CoreException
	 */
	@Override
	public CFClient createClient(String url, String userName, String password, boolean selfSigned,
			IProgressMonitor monitor) throws CoreException {

		if (password == null) {
			password = "";
		}

		return createClient(url, new CloudCredentials(userName, password),
				/* no cloud space */ null, selfSigned, monitor);
	}

	@Override
	public CFClient createClient(String url, CloudCredentials credentials, boolean selfSigned, IProgressMonitor monitor)
			throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.beginTask(Messages.CONNECTING, IProgressMonitor.UNKNOWN);

		try {
			return new CFClient(createV1Client(toUrl(url), credentials, selfSigned));
		}
		catch (RuntimeException t) {
			throw CloudErrorUtil.checkServerCommunicationError(t);
		}
		finally {
			progress.done();
		}
	}

	@Override
	public CloudServerCFClient createSsoClientWithUpdatedPasscode(CloudFoundryServer cloudServer, String passcode,
			IProgressMonitor monitor) throws CoreException {

		SubMonitor progress = SubMonitor.convert(monitor);
		progress.beginTask(Messages.CONNECTING, IProgressMonitor.UNKNOWN);
		try {
			String url = cloudServer.getUrl();
			boolean selfSigned = cloudServer.isSelfSigned();
			CloudFoundrySpace cloudSpace = cloudServer.getCloudFoundrySpace();
			String tokenValue = cloudServer.getToken();
			CloudCredentials credentials = CloudUtil.createSsoCredentials(passcode, tokenValue);
			CloudFoundryOperations v1Client = createV1Client(toUrl(url), credentials, cloudSpace, selfSigned);
			CloudServerCFClient cfClient = new CloudServerCFClient(v1Client, cloudServer);

			CloudFoundryLoginHandler operationsHandler = new CloudFoundryLoginHandler(cfClient, cloudServer);
			int attempts = 5;

			operationsHandler.login(progress, attempts, CloudOperationsConstants.LOGIN_INTERVAL);
			return cfClient;
		}
		catch (RuntimeException t) {
			throw CloudErrorUtil.checkServerCommunicationError(t);
		}
		finally {
			progress.done();
		}
	}

	@Override
	public void validate(String url, String userName, String password, boolean selfSigned, IProgressMonitor monitor)
			throws CoreException {
		createClient(url, userName, password, selfSigned, monitor);
	}

	@Override
	public CloudServerCFClient createCloudServerClient(CloudFoundryServer cloudServer, CloudCredentials credentials,
			CloudFoundrySpace cloudFoundrySpace, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.beginTask(Messages.CONNECTING, IProgressMonitor.UNKNOWN);
		try {

			CloudFoundryOperations v1Client = cloudFoundrySpace != null ? createV1Client(toUrl(cloudServer.getUrl()), credentials, cloudFoundrySpace,
					cloudServer.isSelfSigned()) : createV1Client(toUrl(cloudServer.getUrl()), credentials, cloudServer.isSelfSigned());
			CloudServerCFClient cloudServerCfClient = new CloudServerCFClient(v1Client, cloudServer);

			CloudFoundryLoginHandler operationsHandler = new CloudFoundryLoginHandler(cloudServerCfClient, cloudServer);
			int attempts = 5;

			operationsHandler.login(progress, attempts, CloudOperationsConstants.LOGIN_INTERVAL);

			return cloudServerCfClient;
		}
		catch (RuntimeException t) {
			throw CloudErrorUtil.checkServerCommunicationError(t);
		}
		finally {
			progress.done();
		}
	}

	@Override
	public void validateSso(String url, CloudFoundryServer cloudServer, boolean selfSigned, String passcode,
			String tokenValue, IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor);
		progress.beginTask(Messages.CONNECTING, IProgressMonitor.UNKNOWN);
		try {

			CloudCredentials credentials = CloudUtil.createSsoCredentials(passcode, tokenValue);
			CloudFoundryOperations v1Client = createV1Client(toUrl(url), credentials, selfSigned);
			CFClient cfClient = new CFClient(v1Client);

			CloudFoundryLoginHandler operationsHandler = new CloudFoundryLoginHandler(cfClient, cloudServer);
			int attempts = 5;

			operationsHandler.login(progress, attempts, CloudOperationsConstants.LOGIN_INTERVAL);
		}
		catch (RuntimeException t) {
			throw CloudErrorUtil.checkServerCommunicationError(t);
		}
		finally {
			progress.done();
		}
	}

	/*
	 * 
	 * Helper methods
	 * 
	 */
	protected CloudFoundryOperations createV1Client(URL serverUrl, CloudCredentials credentials, boolean selfSigned) {
		return CloudFoundryPlugin.getCloudFoundryClientFactory().getCloudFoundryOperations(serverUrl, credentials,
				selfSigned);
	}

	protected CloudFoundryOperations createV1Client(URL serverUrl, CloudCredentials credentials,
			CloudFoundrySpace cloudSpace, boolean selfSigned) {
		return CloudFoundryPlugin.getCloudFoundryClientFactory().getCloudFoundryOperations(serverUrl, credentials,
				cloudSpace.getOrgName(), cloudSpace.getSpaceName(), selfSigned);
	}

	protected URL toUrl(String url) throws CoreException {

		try {
			return new URL(url);

		}
		catch (MalformedURLException e) {
			throw new CoreException(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
					"The server url " + url + " is invalid: " + e.getMessage(), e)); //$NON-NLS-1$ //$NON-NLS-2$
		}

	}

	protected URL toUrl(CloudFoundryServer cloudServer) throws CoreException {
		return toUrl(cloudServer.getUrl());
	}
}
