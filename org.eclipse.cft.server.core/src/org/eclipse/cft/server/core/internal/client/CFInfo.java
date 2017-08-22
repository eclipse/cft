/*******************************************************************************
 * Copyright (c) 2016, 2017 Pivotal Software, Inc. and others
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

import java.util.Map;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.util.CloudUtil;
import org.cloudfoundry.client.lib.util.JsonUtil;
import org.eclipse.cft.server.core.internal.ssh.SshHost;
import org.osgi.framework.Version;
import org.springframework.web.client.RestTemplate;

public class CFInfo implements CloudInfo {

	protected final RestTemplate restTemplate;

	private String ccUrl;

	private Map<String, Object> infoMap;

	public CFInfo(CloudCredentials creds, String url, HttpProxyConfiguration proxyConf, boolean selfSigned) {
		restTemplate = RestUtils.createRestTemplate(proxyConf, selfSigned, false);
		this.ccUrl = url;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CloudInfo#getAuthorizationUrl()
	 */
	@Override
	public String getAuthorizationUrl() {
		return getProp("authorization_endpoint"); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CloudInfo#getTokenUrl()
	 */
	@Override
	public String getTokenUrl() {
		return getProp("token_endpoint"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CloudInfo#getCloudControllerUrl()
	 */
	@Override
	public String getCloudControllerUrl() {
		return this.ccUrl;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CloudInfo#getDopplerUrl()
	 */
	@Override
	public String getDopplerUrl() {
		return getProp("doppler_logging_endpoint"); //$NON-NLS-1$
	}

	/**
	 * 
	 * @deprecated use {@link #getCCApiVersion()} instead
	 */
	public String getCloudControllerApiVersion() {
		return getProp("api_version"); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CloudInfo#getCCApiVersion()
	 */
	@Override
	public Version getCCApiVersion() {
		String version  = getCloudControllerApiVersion();
		if (version != null) {
			return new Version(version);
		}
		
		return null;
	}

	public String getProp(String name) {
		Map<String, Object> map = getMap();
		if (map != null) {
			return CloudUtil.parse(String.class, map.get(name));
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CloudInfo#getSshClientId()
	 */
	@Override
	public String getSshClientId() {
		return getProp("app_ssh_oauth_client"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cft.server.core.internal.client.CloudInfo#getSshHost()
	 */
	@Override
	public SshHost getSshHost() {
		String fingerPrint = getProp("app_ssh_host_key_fingerprint"); //$NON-NLS-1$
		String host = getProp("app_ssh_endpoint"); //$NON-NLS-1$
		int port = 22; // Default ssh port
		if (host != null) {
			if (host.contains(":")) { //$NON-NLS-1$
				String[] pieces = host.split(":"); //$NON-NLS-1$
				host = pieces[0];
				port = Integer.parseInt(pieces[1]);
			}
		}
		if (host != null || fingerPrint != null) {
			return new SshHost(host, port, fingerPrint);
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