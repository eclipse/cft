/*******************************************************************************
 * Copied from Spring Tool Suite. Original license:
 * 
 * Copyright (c) 2015, 2016 Pivotal Software, Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.cft.server.core.internal.client;

import java.io.IOException;
import java.net.URI;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudUtil;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Contains helper methods to allow access to certain components of an existing
 * v1 client (so an existing connection to Cloud Foundry) that cannot be access
 * via legacy v1 API. Examples are {@link RestTemplate}
 *
 * @author Kris De Volder
 */
public class CFClientV1Support {
	protected final AuthorizationHeaderProvider oauth;

	protected final RestTemplate restTemplate;

	private final CFInfo cloudInfo;

	protected final String authorizationUrl;
	
	protected final String tokenUrl;
	
	protected final CloudSpace existingSessionConnection;

	protected final CloudFoundryServer cfServer;
	
	public CFClientV1Support(CloudFoundryOperations cfClient, CloudSpace existingSessionConnection, CFInfo cloudInfo,
			HttpProxyConfiguration httpProxyConfiguration, CloudFoundryServer cfServer, boolean trustSelfSigned) {
		this.cloudInfo = cloudInfo;
		this.oauth = getHeaderProvider(cfClient);
		this.existingSessionConnection = existingSessionConnection;

		this.restTemplate = RestUtils.createRestTemplate(httpProxyConfiguration, trustSelfSigned, true);
		ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
		restTemplate.setRequestFactory(authorize(requestFactory));

		this.tokenUrl = cloudInfo.getTokenUrl();
		
		this.authorizationUrl = cloudInfo.getAuthorizationUrl();
		
		this.cfServer = cfServer;
	}

	/**
	 * 
	 * @return current session space in the connected client. It may be null if
	 * the support does not require access to the existing client session.
	 * However, this does NOT mean that the connected client has no session.
	 * Only means that it is not accessible from the support
	 */
	protected CloudSpace getExistingConnectionSession() {
		return this.existingSessionConnection;
	}

	protected ClientHttpRequestFactory authorize(final ClientHttpRequestFactory delegate) {
		return new ClientHttpRequestFactory() {

			public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
				ClientHttpRequest request = delegate.createRequest(uri, httpMethod);
				request.getHeaders().add("Authorization", oauth.getAuthorizationHeader()); //$NON-NLS-1$
				return request;
			}
		};
	}

	protected String getUrl(String path) {
		return cloudInfo.getCloudControllerUrl() + (path.startsWith("/") //$NON-NLS-1$
				? path : "/" + path); //$NON-NLS-1$
	}

	protected AuthorizationHeaderProvider getHeaderProvider(final CloudFoundryOperations cfClient) {
		AuthorizationHeaderProvider oauth = new AuthorizationHeaderProvider() {
			public String getAuthorizationHeader() {
				OAuth2AccessToken token = cfClient.login();
				
				if(cfServer != null) {
					// In the SSO case, store the token for later use
					try {
						String tokenValue = CloudUtil.getTokenAsJson(token);
						cfServer.setAndSaveToken(tokenValue);
					}
					catch (JsonProcessingException e) {
						CloudFoundryPlugin.logWarning(e.getMessage());
					}					
				}
				
				return token.getTokenType() + " " + token.getValue(); //$NON-NLS-1$
			}
		};
		return oauth;
	}

	protected CFInfo getCloudInfo() {
		return this.cloudInfo;
	}

}
