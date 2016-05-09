/**
 * Copied and derived from cloudfoundry-client-lib-1.1.2:
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
package org.eclipse.cft.server.core.internal.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.InstanceStats;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.cloudfoundry.client.lib.util.JsonUtil;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.client.diego.CFInfo;
import org.eclipse.core.runtime.CoreException;
import org.springframework.http.HttpStatus;

/**
 * Work-around for limitations in v1 cloudfoundry-client-lib (1.1.4 and
 * earlier), including methods in the client that cannot be overridden.
 * <p/>
 * This is only used until v2 of the client is adopted by CFT. It should not be
 * used outside of CFT framework as it will be deprecated.
 *
 */
public class AdditionalV1Operations extends CFClientV1Support {

	private CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

	public AdditionalV1Operations(CloudFoundryOperations client, CloudSpace sessionSpace, CFInfo cloudInfo,
			HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSigned) {
		super(client, sessionSpace, cloudInfo, httpProxyConfiguration, trustSelfSigned);
	}

	/**
	 * A relatively fast way to fetch all applications in the active session
	 * Cloud space, that contains basic information about each apps.
	 * <p/>
	 * Information that may be MISSING from the list for each app: service
	 * bindings, mapped URLs, and app instances.
	 * @return list of apps with basic information
	 * @throws CoreException
	 */
	public List<CloudApplication> getBasicApplications() {
		List<CloudApplication> apps = new ArrayList<CloudApplication>();

		if (getExistingConnectionSession() != null) {
			Map<String, Object> urlVars = new HashMap<String, Object>();
			String urlPath = "/v2";
			urlVars.put("space", getExistingConnectionSession().getMeta().getGuid());
			urlPath = urlPath + "/spaces/{space}";
			urlPath = urlPath + "/apps?inline-relations-depth=1";
			List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
			for (Map<String, Object> resource : resourceList) {
				CloudApplication app = mapBasicCloudApplication(resource);
				if (app != null) {
					apps.add(app);
				}
			}
		}
		return apps;
	}

	/**
	 * A relatively fast way to fetch an application in the active session Cloud
	 * space, that contains basic information.
	 * <p/>
	 * Information that may be MISSING from the list for each app: service
	 * bindings, mapped URLs, and app instances.
	 * @return list of apps with basic information
	 * @throws CoreException
	 */
	public CloudApplication getBasicApplication(String appName) {
		return mapBasicCloudApplication(appName);
	}

	/**
	 * Alternate, and potentially faster, way of stopping an application that
	 * differs from the v1 client, which does not require fetching additional
	 * application information as part of stopping an app
	 * @param appName
	 */
	public void stopApplication(String appName) {
		CloudApplication app = mapBasicCloudApplication(appName);
		if (app.getState() != CloudApplication.AppState.STOPPED) {
			HashMap<String, Object> appRequest = new HashMap<String, Object>();
			appRequest.put("state", CloudApplication.AppState.STOPPED); //$NON-NLS-1$
			restTemplate.put(getUrl("/v2/apps/{guid}"), appRequest, app.getMeta().getGuid()); //$NON-NLS-1$
		}
	}

	/**
	 * A complete application includes all the information available for an
	 * application, including service bindings, mapped URLs, and app instances.
	 * <p/>
	 * This may be a longer running operation due to possibility of multiple
	 * requests sent to CF for the different informations
	 * @param application complete application if it exists.
	 * @return non-null application.
	 * @throws may throw 404 Error if app no longer exists
	 */
	public CFV1Application getCompleteApplication(CloudApplication application) {

		CloudApplication updatedApp = getCompleteApplicationExceptInstances(application.getName());
		ApplicationStats stats = null;

		try {
			stats = doGetApplicationStats(application.getMeta().getGuid(), application.getState());
		}
		catch (Throwable e) {
			if (CloudErrorUtil.is503Error(e)) {
				// Unable to fetch stats at this time. Ignore
			}
		}

		return new CFV1Application(stats, updatedApp);

	}

	//
	// HELPER METHODS
	//

	/*
	 * Map application information into a CloudApplication type
	 */

	protected CloudApplication mapBasicCloudApplication(String appName) {
		Map<String, Object> resource = getBasicApplicationResource(appName);
		return mapBasicCloudApplication(resource);
	}

	protected CloudApplication mapBasicCloudApplication(Map<String, Object> resource) {
		CloudApplication cloudApp = null;
		if (resource != null) {
			cloudApp = resourceMapper.mapResource(resource, CloudApplication.class);
		}
		return cloudApp;
	}

	protected Map<String, Object> getBasicApplicationResource(String appName) {
		Map<String, Object> urlVars = new HashMap<String, Object>();
		String urlPath = "/v2"; //$NON-NLS-1$
		CloudSpace sessionSpace = getExistingConnectionSession();

		if (sessionSpace != null) {
			urlVars.put("space", sessionSpace.getMeta().getGuid()); //$NON-NLS-1$
			urlPath = urlPath + "/spaces/{space}"; //$NON-NLS-1$
		}
		urlVars.put("q", "name:" + appName); //$NON-NLS-1$ //$NON-NLS-2$
		urlPath = urlPath + "/apps?inline-relations-depth=1&q={q}"; //$NON-NLS-1$

		List<Map<String, Object>> allResources = getAllResources(urlPath, urlVars);
		if (!allResources.isEmpty()) {
			Map<String, Object> foundResource = allResources.get(0);
			fillApplicationStack(foundResource);
			return foundResource;
		}
		return null;
	}

	/**
	 * Returns a complete Cloud application, including bound services and mapped
	 * URLs, but without running instances information.
	 * @param appName
	 * @return non-null Cloud Application
	 * @throws CloudFoundryException if error occurs or application does not
	 * exist anymore
	 */
	protected CloudApplication getCompleteApplicationExceptInstances(String appName) {
		Map<String, Object> resource = getBasicApplicationResource(appName);
		if (resource == null) {
			throw new CloudFoundryException(HttpStatus.NOT_FOUND, "Not Found", //$NON-NLS-1$
					"Application not found"); //$NON-NLS-1$
		}
		fillApplicationServiceBindings(resource);

		CloudApplication app = mapBasicCloudApplication(resource);
		fillApplicationUris(app);
		return app;
	}

	/*
	 * Fill in additional information for the application
	 */

	protected Map<String, Object> fillApplicationServiceBindings(Map<String, Object> resource) {
		fillInEmbeddedResource(resource, "service_bindings", "service_instance"); //$NON-NLS-1$ //$NON-NLS-2$
		return resource;
	}

	protected Map<String, Object> fillApplicationStack(Map<String, Object> resource) {
		fillInEmbeddedResource(resource, "stack"); //$NON-NLS-1$
		return resource;
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

	protected CloudApplication fillApplicationUris(CloudApplication app) {
		if (app != null) {
			app.setUris(findApplicationUris(app.getMeta().getGuid()));
		}
		return app;
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

	/*
	 * Additional common helper methods
	 */

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

	@SuppressWarnings("unchecked")
	private ApplicationStats doGetApplicationStats(UUID appId, CloudApplication.AppState appState) {
		List<InstanceStats> instanceList = new ArrayList<InstanceStats>();
		if (appState.equals(CloudApplication.AppState.STARTED)) {
			Map<String, Object> respMap = getInstanceInfoForApp(appId, "stats");
			for (String instanceId : respMap.keySet()) {
				InstanceStats instanceStats = new InstanceStats(instanceId,
						(Map<String, Object>) respMap.get(instanceId));
				instanceList.add(instanceStats);
			}
		}
		return new ApplicationStats(instanceList);
	}

	private Map<String, Object> getInstanceInfoForApp(UUID appId, String path) {
		String url = getUrl("/v2/apps/{guid}/" + path);
		Map<String, Object> urlVars = new HashMap<String, Object>();
		urlVars.put("guid", appId);
		String resp = restTemplate.getForObject(url, String.class, urlVars);
		return JsonUtil.convertJsonToMap(resp);
	}

}
