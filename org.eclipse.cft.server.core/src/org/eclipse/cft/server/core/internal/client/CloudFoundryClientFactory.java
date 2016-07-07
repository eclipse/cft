/*******************************************************************************
 * Copyright (c) 2012, 2016 Pivotal Software, Inc. and others 
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.util.CloudUtil;
import org.cloudfoundry.client.lib.util.JsonUtil;
import org.cloudfoundry.client.lib.util.RestUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

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
	
	private static String getUrl(String url, String path) {
		return url + (path.startsWith("/") ? path : "/" + path);
	}
	
	private static String getJson(RestTemplate restTemplate, String urlString) {
		ClientHttpResponse response = null;
		HttpMethod method = null;
		try {
			method = HttpMethod.GET;
		
			URI url = new UriTemplate(urlString).expand();
			ClientHttpRequest request = restTemplate.getRequestFactory().createRequest(url, method);
			
			List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
			acceptableMediaTypes.add(MediaType.APPLICATION_JSON);
			request.getHeaders().setAccept(acceptableMediaTypes );
			//if (requestCallback != null) {
			//	requestCallback.doWithRequest(request);
			//}
			response = request.execute();
			if (response.getBody() != null) {
				HttpMessageConverterExtractor<String> extractor = new HttpMessageConverterExtractor<String>(String.class, restTemplate.getMessageConverters());
				String data = extractor.extractData(response);
				return data;
			};
		}
		catch (IOException ex) {
			throw new ResourceAccessException("I/O error on " + method.name() +
					" request for \"" + urlString + "\":" + ex.getMessage(), ex);
		}
		finally {
			if (response != null) {
				response.close();
			}
		}
		return null;
	}

	public static String getSsoUrl(String apiurl, boolean trustSelfSignedCerts) throws Exception {
		RestUtil restUtil = new RestUtil();
		HttpProxyConfiguration httpProxyConfiguration = getProxy(new URL(apiurl));
		RestTemplate restTemplate = restUtil.createRestTemplate(httpProxyConfiguration, trustSelfSignedCerts);
		String infoV2Json = restTemplate.getForObject(getUrl(apiurl, "/v2/info"), String.class);
		Map<String, Object> infoV2Map = JsonUtil.convertJsonToMap(infoV2Json);
		String authorizationEndpoint = CloudUtil.parse(String.class, infoV2Map.get("authorization_endpoint"));
		String passcodeJson = getJson(restTemplate, getUrl(authorizationEndpoint, "/login"));
		Map<String, Object> passcodeMap = JsonUtil.convertJsonToMap(passcodeJson);
		Object prompts = passcodeMap.get("prompts");
		if (prompts instanceof Map) {
			@SuppressWarnings("rawtypes")
			Object text = ((Map)prompts).get("passcode");
			if (text instanceof List) {
				@SuppressWarnings("rawtypes")
				List list = (List) text;
				if (list.size() >= 2 && list.get(1) != null) {
					String message = list.get(1).toString();
					String[] elements = message.split(" ");
					for (int i = 0; i < elements.length; i++) {
						String element = elements[i];
						if (element != null && element.startsWith("http")) {
							
							// Strip trailing parentheses
							String url = element;
							while(url.endsWith(")")) {
								url = url.substring(0,  url.length()-1);
							}
							
							return url;
						}
					}
				}
			}
		}
		return null;
	}


}