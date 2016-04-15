/*******************************************************************************
 * Copied from Spring Tool Suite. Original license:
 * 
 * Copyright (c) 2015, 2016 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.cft.server.core.internal.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.cloudfoundry.client.lib.util.JsonUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.v2.CloudInfoV2;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

public class BuildpackSupport extends V1ClientSupport {

	public BuildpackSupport(AuthorizationHeaderProvider oauth, CloudInfoV2 cloudInfo, boolean trustSelfSigned,
			HttpProxyConfiguration httpProxyConfiguration) {
		super(oauth, cloudInfo, trustSelfSigned, httpProxyConfiguration);
	}

	public List<String> getBuildpacks() {
		List<String> buildpacks = new ArrayList<String>();
		String json = restTemplate.getForObject(url("/v2/buildpacks"), String.class); //$NON-NLS-1$
		if (json != null) {
			Map<String, Object> resource = JsonUtil.convertJsonToMap(json);
			if (resource != null) {
				List<Map<String, Object>> newResources = (List<Map<String, Object>>) resource.get("resources"); //$NON-NLS-1$
				if (newResources != null) {
					for (Map<String, Object> res : newResources) {
						String name = CloudEntityResourceMapper.getEntityAttribute(res, "name", String.class); //$NON-NLS-1$
						if (name != null) {
							buildpacks.add(name);
						}
					}
				}
			}
		}
		return buildpacks;
	}

	public static BuildpackSupport create(CloudFoundryServer cloudServer, CloudFoundryOperations client, IProgressMonitor monitor)
			throws CoreException {

		String userName = cloudServer.getUsername();
		String password = cloudServer.getPassword();
		boolean selfSigned = cloudServer.getSelfSignedCertificate();
		final CloudFoundryOperations cfClient = client;
		HttpProxyConfiguration proxyConf = null;

		AuthorizationHeaderProvider oauth = new AuthorizationHeaderProvider() {
			public String getAuthorizationHeader() {
				OAuth2AccessToken token = cfClient.login();
				return token.getTokenType() + " " + token.getValue();
			}
		};

		CloudInfoV2 cloudInfo = new CloudInfoV2(new CloudCredentials(userName, password),
				client.getCloudControllerUrl().toString(), proxyConf, selfSigned);

		return new BuildpackSupport(oauth, cloudInfo, selfSigned, proxyConf);
	}

}
