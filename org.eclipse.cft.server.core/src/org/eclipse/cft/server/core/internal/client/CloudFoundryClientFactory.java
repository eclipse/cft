/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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
import java.net.URISyntaxException;
import java.net.URL;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

/**
 * Create Cloud Foundry clients, including clients that are UAA aware.Note that
 * client/operation API should always be called within a specific Request
 * wrapper, unless performing standalone operations like validating credentials
 * or getting a list of organisations and spaces. Request wrappers do various
 * operations prior to invoking client API, including automatic client login and
 * proxy setting handling.
 * 
 * @see org.eclipse.cft.server.core.internal.client.ClientRequest
 * 
 * 
 */
public class CloudFoundryClientFactory {

	private static CloudFoundryClientFactory sessionFactory = null;

	public static CloudFoundryClientFactory getDefault() {
		if (sessionFactory == null) {
			sessionFactory = new CloudFoundryClientFactory();
		}
		return sessionFactory;
	}

	public CloudFoundryOperations getCloudFoundryOperations(CloudCredentials credentials, URL url, boolean selfSigned) {
		return getCloudFoundryOperations(credentials, url, null, selfSigned);
	}

	public CloudFoundryOperations getCloudFoundryOperations(CloudCredentials credentials, URL url, CloudSpace session,
			boolean selfSigned) {

		// Proxies are always updated on each client call by the
		// CloudFoundryServerBehaviour Request as well as the client login
		// handler
		// therefore it is not critical to set the proxy in the client on
		// client
		// creation

		HttpProxyConfiguration proxyConfiguration = getProxy(url);
		return session != null ? new CloudFoundryClient(credentials, url, session, selfSigned)
				: new CloudFoundryClient(credentials, url, proxyConfiguration, selfSigned);
	}

	public CloudFoundryOperations getCloudFoundryOperations(CloudCredentials credentials, URL url, String orgName,
			String spaceName, boolean selfsigned) {

		// Proxies are always updated on each client call by the
		// CloudFoundryServerBehaviour Request as well as the client login
		// handler
		// therefore it is not critical to set the proxy in the client on
		// client
		// creation
		HttpProxyConfiguration proxyConfiguration = getProxy(url);
		return new CloudFoundryClient(credentials, url, orgName, spaceName, proxyConfiguration, selfsigned);
	}

	public CloudFoundryOperations getCloudFoundryOperations(String cloudControllerUrl) throws MalformedURLException {
		return getCloudFoundryOperations(cloudControllerUrl, false);
	}

	public CloudFoundryOperations getCloudFoundryOperations(String cloudControllerUrl, boolean selfSigned)
			throws MalformedURLException {
		URL url = new URL(cloudControllerUrl);
		// Proxies are always updated on each client call by the
		// CloudFoundryServerBehaviour Request as well as the client login
		// handler
		// therefore it is not critical to set the proxy in the client on client
		// creation
		HttpProxyConfiguration proxyConfiguration = getProxy(url);
		return new CloudFoundryClient(url, proxyConfiguration, selfSigned);
	}

	protected static CloudCredentials getCredentials(String userName, String password) {
		return new CloudCredentials(userName, password);
	}

	public static HttpProxyConfiguration getProxy(URL url) {
		if (url == null) {
			return null;
		}
		// In certain cases, the activator would have stopped and the plugin may
		// no longer be available. Usually onl happens on shutdown.
		CloudFoundryPlugin plugin = CloudFoundryPlugin.getDefault();
		if (plugin != null) {
			IProxyService proxyService = plugin.getProxyService();
			if (proxyService != null) {
				try {
					IProxyData[] selectedProxies = proxyService.select(url.toURI());

					// No proxy configured or not found
					if (selectedProxies == null || selectedProxies.length == 0) {
						return null;
					}

					IProxyData data = selectedProxies[0];
					int proxyPort = data.getPort();
					String proxyHost = data.getHost();
					String user = data.getUserId();
					String password = data.getPassword();
					return proxyHost != null ? new HttpProxyConfiguration(proxyHost, proxyPort,
							data.isRequiresAuthentication(), user, password) : null;
				}
				catch (URISyntaxException e) {
					// invalid url (protocol, ...) => proxy will be null
				}
			}
		}
		return null;
	}
}