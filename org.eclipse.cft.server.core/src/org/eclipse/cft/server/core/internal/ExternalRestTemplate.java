/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
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

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.rest.CloudControllerResponseErrorHandler;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * Rest template for calls to Cloud Foundry that are typically external to the
 * Cloud client (for example, directly sending requests to an application
 * deployed in a Cloud space via the application URL that does not require Cloud
 * Controller endpoints). For Cloud operations that go through the Cloud
 * Controller, this should NOT be used. Instead, use API in
 * {@link CloudFoundryServerBehaviour} which indirectly calls the underlying
 * {@link CloudFoundryOperations}
 */
public class ExternalRestTemplate extends RestTemplate {

	public ExternalRestTemplate() {
		createRestTemplate();
	}

	protected ClientHttpRequestFactory createRequestFactory() {
		HttpClientBuilder httpClientBuilder = HttpClients.custom().useSystemProperties();
		HttpClient httpClient = httpClientBuilder.build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

		return requestFactory;
	}

	protected void createRestTemplate() {
		setRequestFactory(createRequestFactory());
		setErrorHandler(new CloudControllerResponseErrorHandler());
		setMessageConverters(getHttpMessageConverters());
	}

	protected List<HttpMessageConverter<?>> getHttpMessageConverters() {
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
		messageConverters.add(new ByteArrayHttpMessageConverter());
		messageConverters.add(new StringHttpMessageConverter());
		messageConverters.add(new ResourceHttpMessageConverter());
		messageConverters.add(new MappingJackson2HttpMessageConverter());
		return messageConverters;
	}
}
