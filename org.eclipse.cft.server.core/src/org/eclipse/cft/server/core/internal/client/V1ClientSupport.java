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
package org.eclipse.cft.server.core.internal.client;

import java.io.IOException;
import java.net.URI;

import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.eclipse.cft.server.core.internal.client.v2.CloudInfoV2;
import org.eclipse.cft.server.core.internal.client.v2.RestUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class V1ClientSupport {
	protected final AuthorizationHeaderProvider oauth;

	protected final RestTemplate restTemplate;

	protected final CloudInfoV2 cloudInfo;

	protected final String authorizationUrl;

	public V1ClientSupport(AuthorizationHeaderProvider oauth, CloudInfoV2 cloudInfo, boolean trustSelfSigned,
			HttpProxyConfiguration httpProxyConfiguration) {
		this.cloudInfo = cloudInfo;
		this.oauth = oauth;

		this.restTemplate = RestUtils.createRestTemplate(httpProxyConfiguration, trustSelfSigned, true);
		ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
		restTemplate.setRequestFactory(authorize(requestFactory));

		this.authorizationUrl = cloudInfo.getAuthorizationUrl();

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
	
	protected String url(String path) {
		return cloudInfo.getCloudControllerUrl()+path;
	}
}
