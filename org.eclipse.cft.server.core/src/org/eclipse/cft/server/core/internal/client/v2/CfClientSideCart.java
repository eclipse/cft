/*******************************************************************************
 * Copied from Spring Tool Suite. Original license:
 * 
 * Copyright (c) 2015 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.cft.server.core.internal.client.v2;

import java.io.IOException;
import java.net.URI;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.eclipse.cft.server.core.internal.client.AuthorizationHeaderProvider;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

/**
 * @author Kris De Volder
 */
public abstract class CfClientSideCart {

	protected final AuthorizationHeaderProvider oauth;

	protected final RestTemplate restTemplate;

	protected final CloudInfoV2 cloudInfo;

	protected final CloudFoundryOperations client;

	protected final CloudSpace sessionSpace;

	public CfClientSideCart(CloudFoundryOperations client, CloudSpace sessionSpace, CloudInfoV2 cloudInfo,
			boolean trustSelfSigned, HttpProxyConfiguration httpProxyConfiguration) {
		this.cloudInfo = cloudInfo;
		this.client = client;
		this.oauth = authProvider(client);
		this.sessionSpace = sessionSpace;

		this.restTemplate = RestUtils.createRestTemplate(httpProxyConfiguration, trustSelfSigned, true);
		ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
		restTemplate.setRequestFactory(authorize(requestFactory));
	}

	private AuthorizationHeaderProvider authProvider(final CloudFoundryOperations client) {
		return new AuthorizationHeaderProvider() {
			public String getAuthorizationHeader() {
				OAuth2AccessToken token = client.login();
				return token.getTokenType() + " " + token.getValue(); //$NON-NLS-1$
			}
		};
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

}
