/*******************************************************************************
 * Copyright (c) 2016 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.core.internal.client.diego;

import java.util.Map;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.util.CloudUtil;
import org.cloudfoundry.client.lib.util.JsonUtil;
import org.eclipse.cft.server.core.internal.client.RestUtils;
import org.springframework.web.client.RestTemplate;

public class CFInfo {

	protected final RestTemplate restTemplate;

	private String ccUrl;

	private Map<String, Object> infoMap;

	public CFInfo(CloudCredentials creds, String url, HttpProxyConfiguration proxyConf, boolean selfSigned) {
		restTemplate = RestUtils.createRestTemplate(proxyConf, selfSigned, false);
		this.ccUrl = url;
	}

	public String getAuthorizationUrl() {
		return getProp("authorization_endpoint"); //$NON-NLS-1$
	}

	public String getCloudControllerUrl() {
		return this.ccUrl;
	}

	public String getCloudControllerApiVersion() {
		return getProp("api_version"); //$NON-NLS-1$
	}

	public String getProp(String name) {
		Map<String, Object> map = getMap();
		if (map != null) {
			return CloudUtil.parse(String.class, map.get(name));
		}
		return null;
	}

	protected Map<String, Object> getMap() {
		if (infoMap == null) {
			String infoV2Json = restTemplate.getForObject(getUrl("/v2/info"), String.class); //$NON-NLS-1$
			infoMap = JsonUtil.convertJsonToMap(infoV2Json);
		}
		return infoMap;
	}

	private String getUrl(String path) {
		return ccUrl + path;
	}

}