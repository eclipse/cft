/**
 * Copied from cloudfoundry-client-lib-1.1.2:
 * 
 * CloudControllerClientImpl
 * 
 * Original license:
 * 
 * Copyright 2009-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.cft.server.core.internal.client.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.cloudfoundry.client.lib.util.JsonUtil;
import org.springframework.http.HttpStatus;

/**
 * Work-around for limitations in v1 cloudfoundry-client-lib (1.1.4 and
 * earlier), primarily private methods that cannot be overridden.
 * <p/>
 * This is only used until v2 of the client is adopted by CFT
 *
 */
public class ExternalClientV1 extends CfClientSideCart {

	private CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

	public ExternalClientV1(CloudFoundryOperations client, CloudSpace sessionSpace, CloudInfoV2 cloudInfo,
			boolean trustSelfSigned, HttpProxyConfiguration httpProxyConfiguration) {
		super(client, sessionSpace, cloudInfo, trustSelfSigned, httpProxyConfiguration);
	}

	/**
	 * Returns a Cloud application without running instances information.
	 * @param appName
	 * @return non-null Cloud Application
	 * @throws CloudFoundryException if error occurs or application does not
	 * exist anymore
	 */
	public CloudApplication getApplicationNoRunningInstances(String appName) {
		Map<String, Object> resource = findApplicationResource(appName, true);
		if (resource == null) {
			throw new CloudFoundryException(HttpStatus.NOT_FOUND, "Not Found", //$NON-NLS-1$
					"Application not found"); //$NON-NLS-1$
		}
		return mapCloudApplicationNoRunningInstances(resource);
	}

	/**
	 * Returns all Cloud applications in the space without running instances
	 * information.
	 * @param appName
	 * @return non-null list of applications.
	 * @throws CloudFoundryException if error occurs
	 */
	public List<CloudApplication> getApplicationsNoRunningInstances() {
		List<CloudApplication> apps = new ArrayList<CloudApplication>();
	
		if (sessionSpace != null) {
			Map<String, Object> urlVars = new HashMap<String, Object>();
			String urlPath = "/v2"; //$NON-NLS-1$
			urlVars.put("space", sessionSpace.getMeta().getGuid()); //$NON-NLS-1$
			urlPath = urlPath + "/spaces/{space}"; //$NON-NLS-1$
			urlPath = urlPath + "/apps?inline-relations-depth=1"; //$NON-NLS-1$
			List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
			for (Map<String, Object> resource : resourceList) {
				processApplicationResource(resource, true);
				apps.add(mapCloudApplicationNoRunningInstances(resource));
			}
		}
	
		return apps;
	}

	public void stopApplication(String appName) {
		CloudApplication app = getApplicationNoRunningInstances(appName);
		if (app.getState() != CloudApplication.AppState.STOPPED) {
			HashMap<String, Object> appRequest = new HashMap<String, Object>();
			appRequest.put("state", CloudApplication.AppState.STOPPED); //$NON-NLS-1$
			restTemplate.put(getUrl("/v2/apps/{guid}"), appRequest, app.getMeta().getGuid()); //$NON-NLS-1$
		}
	}

	/*
	 * 
	 * Helper methods
	 * 
	 * 
	 */

	protected CloudApplication mapCloudApplicationNoRunningInstances(Map<String, Object> resource) {
		CloudApplication cloudApp = null;
		if (resource != null) {
			cloudApp = resourceMapper.mapResource(resource, CloudApplication.class);
			cloudApp.setUris(findApplicationUris(cloudApp.getMeta().getGuid()));
		}
		return cloudApp;
	}

	protected Map<String, Object> findApplicationResource(String appName, boolean fetchServiceInfo) {
		Map<String, Object> urlVars = new HashMap<String, Object>();
		String urlPath = "/v2"; //$NON-NLS-1$
		if (sessionSpace != null) {
			urlVars.put("space", sessionSpace.getMeta().getGuid()); //$NON-NLS-1$
			urlPath = urlPath + "/spaces/{space}"; //$NON-NLS-1$
		}
		urlVars.put("q", "name:" + appName); //$NON-NLS-1$ //$NON-NLS-2$
		urlPath = urlPath + "/apps?inline-relations-depth=1&q={q}"; //$NON-NLS-1$

		List<Map<String, Object>> allResources = getAllResources(urlPath, urlVars);
		if (!allResources.isEmpty()) {
			return processApplicationResource(allResources.get(0), fetchServiceInfo);
		}
		return null;
	}

	protected Map<String, Object> processApplicationResource(Map<String, Object> resource, boolean fetchServiceInfo) {
		if (fetchServiceInfo) {
			fillInEmbeddedResource(resource, "service_bindings", "service_instance"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		fillInEmbeddedResource(resource, "stack"); //$NON-NLS-1$
		return resource;
	}

	@SuppressWarnings("unchecked")
	protected void fillInEmbeddedResource(Map<String, Object> resource, String... resourcePath) {
		if (resourcePath.length == 0) {
			return;
		}
		Map<String, Object> entity = (Map<String, Object>) resource.get("entity"); //$NON-NLS-1$

		String headKey = resourcePath[0];
		String[] tailPath = Arrays.copyOfRange(resourcePath, 1, resourcePath.length);

		if (!entity.containsKey(headKey)) {
			String pathUrl = entity.get(headKey + "_url").toString(); //$NON-NLS-1$
			Object response = restTemplate.getForObject(getUrl(pathUrl), Object.class);
			if (response instanceof Map) {
				Map<String, Object> responseMap = (Map<String, Object>) response;
				if (responseMap.containsKey("resources")) { //$NON-NLS-1$
					response = responseMap.get("resources"); //$NON-NLS-1$
				}
			}
			entity.put(headKey, response);
		}
		Object embeddedResource = entity.get(headKey);

		if (embeddedResource instanceof Map) {
			Map<String, Object> embeddedResourceMap = (Map<String, Object>) embeddedResource;
			// entity = (Map<String, Object>) embeddedResourceMap.get("entity");
			fillInEmbeddedResource(embeddedResourceMap, tailPath);
		}
		else if (embeddedResource instanceof List) {
			List<Object> embeddedResourcesList = (List<Object>) embeddedResource;
			for (Object r : embeddedResourcesList) {
				fillInEmbeddedResource((Map<String, Object>) r, tailPath);
			}
		}
		else {
			// no way to proceed
			return;
		}
	}

	@SuppressWarnings("unchecked")
	protected List<Map<String, Object>> getAllResources(String urlPath, Map<String, Object> urlVars) {
		List<Map<String, Object>> allResources = new ArrayList<Map<String, Object>>();
		String resp;
		if (urlVars != null) {
			resp = restTemplate.getForObject(getUrl(urlPath), String.class, urlVars);
		}
		else {
			resp = restTemplate.getForObject(getUrl(urlPath), String.class);
		}
		Map<String, Object> respMap = JsonUtil.convertJsonToMap(resp);
		List<Map<String, Object>> newResources = (List<Map<String, Object>>) respMap.get("resources"); //$NON-NLS-1$
		if (newResources != null && newResources.size() > 0) {
			allResources.addAll(newResources);
		}
		String nextUrl = (String) respMap.get("next_url"); //$NON-NLS-1$
		while (nextUrl != null && nextUrl.length() > 0) {
			nextUrl = addPageOfResources(nextUrl, allResources);
		}
		return allResources;
	}

	@SuppressWarnings("unchecked")
	protected String addPageOfResources(String nextUrl, List<Map<String, Object>> allResources) {
		String resp = restTemplate.getForObject(getUrl(nextUrl), String.class);
		Map<String, Object> respMap = JsonUtil.convertJsonToMap(resp);
		List<Map<String, Object>> newResources = (List<Map<String, Object>>) respMap.get("resources"); //$NON-NLS-1$
		if (newResources != null && newResources.size() > 0) {
			allResources.addAll(newResources);
		}
		return (String) respMap.get("next_url"); //$NON-NLS-1$
	}

	protected List<String> findApplicationUris(UUID appGuid) {
		String urlPath = "/v2/apps/{app}/routes?inline-relations-depth=1"; //$NON-NLS-1$
		Map<String, Object> urlVars = new HashMap<String, Object>();
		urlVars.put("app", appGuid); //$NON-NLS-1$
		List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
		List<String> uris = new ArrayList<String>();
		for (Map<String, Object> resource : resourceList) {
			Map<String, Object> domainResource = CloudEntityResourceMapper.getEmbeddedResource(resource, "domain"); //$NON-NLS-1$
			String host = CloudEntityResourceMapper.getEntityAttribute(resource, "host", String.class); //$NON-NLS-1$
			String domain = CloudEntityResourceMapper.getEntityAttribute(domainResource, "name", String.class); //$NON-NLS-1$
			if (host != null && host.length() > 0)
				uris.add(host + "." + domain);
			else
				uris.add(domain);
		}
		return uris;
	}
}
