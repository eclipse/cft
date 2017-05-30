/*******************************************************************************
 * Copyright (c) 2012, 2017 Pivotal Software, Inc. and others 
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

import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.eclipse.cft.server.core.internal.client.CFCloudCredentials;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.V1CloudCredentials;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;

public class CloudServerUtil {

	private CloudServerUtil() {
		// util class
	}

	/**
	 * Returns list of cloud foundry server instances. May be emtpy, but not
	 * null.
	 * @return returns a non-null list of cloud foundry server instances. May be
	 * empty.
	 */
	public static List<CloudFoundryServer> getCloudServers() {
		IServer[] servers = ServerCore.getServers();
		Set<CloudFoundryServer> matchedServers = new HashSet<CloudFoundryServer>();

		if (servers != null) {
			for (IServer server : servers) {
				CloudFoundryServer cfServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
				if (cfServer != null) {
					matchedServers.add(cfServer);
				}
			}
		}

		return new ArrayList<CloudFoundryServer>(matchedServers);

	}

	/**
	 * 
	 * @param serverID unique ID of the server. This should not just be the name
	 * of the server, but the full id (e.g. for V2, it should include the space
	 * name)
	 * @return CloudFoundry server that corresponds to the given ID, or null if
	 * not found
	 */
	public static CloudFoundryServer getCloudServer(String serverID) {
		IServer[] servers = ServerCore.getServers();
		if (servers == null) {
			return null;
		}
		CloudFoundryServer cfServer = null;

		for (IServer server : servers) {
			cfServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);

			if (cfServer != null && cfServer.getServerId().equals(serverID)) {
				break;
			}
		}
		return cfServer;
	}

	/**
	 * 
	 * @param server that associates to a {@link CloudFoundryServer}
	 * @return non-null {@link CloudFoundryServer}
	 * @throws CoreException if server is not a {@link CloudFoundryServer} or
	 * error occurred while resolving Cloud server
	 */
	public static CloudFoundryServer getCloudServer(IServer server) throws CoreException {
		CloudFoundryServer cfServer = (CloudFoundryServer) server.getAdapter(CloudFoundryServer.class);
		if (cfServer == null) {
			throw CloudErrorUtil.toCoreException(
					NLS.bind(Messages.CloudServerUtil_NOT_CLOUD_SERVER_ERROR, server.getName(), server.getId()));
		}
		return cfServer;
	}

	/**
	 * Check if the server is a Cloud Foundry-based server
	 * @param server
	 * @return true if it is a Cloud Foundry server
	 */
	public static boolean isCloudFoundryServer(IServer server) {
		if (server != null) {
			return isCloudFoundryServerType(server.getServerType());
		}
		return false;
	}

	/**
	 * 
	 * @param module
	 * @param server
	 * @return cloud module for the given IModule in the IServer, or null if not
	 * found
	 */
	public static CloudFoundryApplicationModule getCloudModule(IModule module, IServer server) {

		if (module == null) {
			return null;
		}
		try {
			CloudFoundryServer cloudServer = CloudServerUtil.getCloudServer(server);
			CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);
			return appModule;
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return null;
	}

	/**
	 * Returns non-null, existing cloud module for the given IModule. 
	 * @param module
	 * @param server
	 * @return existing non-null Cloud module
	 * @throws CoreException if module does not exist or failed to resolve
	 */
	public static CloudFoundryApplicationModule getExistingCloudModule(IModule module, IServer server)
			throws CoreException {
		CloudFoundryApplicationModule appMod = getCloudModule(module, server);
		if (appMod == null) {
		    throw CloudErrorUtil.toCoreException(NLS.bind(Messages.CloudServerUtil_NO_CLOUD_MODULE_FOUND,
					module.getName(), server.getId()));
		}
		return appMod;
	}

	/**
	 * Check if the server type is a Cloud Foundry-based server type
	 * @param serverType
	 * @return true if it is a Cloud Foundry server type
	 */
	public static boolean isCloudFoundryServerType(IServerType serverType) {
		if (serverType != null) {
			String serverId = serverType.getId();
			return CloudFoundryBrandingExtensionPoint.getServerTypeIds().contains(serverId);
		}
		return false;
	}

	public static CFCloudCredentials getCredentials(CloudFoundryServer cloudServer) throws CoreException {
		CloudCredentials v1Credentials = null;
		if (cloudServer.isSso()) {
			v1Credentials = CloudUtil.createSsoCredentials(cloudServer.getPasscode(), cloudServer.getToken());
		}
		else {
			String userName = cloudServer.getUsername();
			String password = cloudServer.getPassword();
			v1Credentials = new CloudCredentials(userName, password);
		}
		return new V1CloudCredentials(v1Credentials);
	}

	public static IProxyData getProxy(URL url) {
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

					return selectedProxies[0];
				}
				catch (URISyntaxException e) {
					// invalid url (protocol, ...) => proxy will be null
				}
			}
		}
		return null;
	}
}
