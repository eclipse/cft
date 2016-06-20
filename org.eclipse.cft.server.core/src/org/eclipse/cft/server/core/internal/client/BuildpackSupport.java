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

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.cloudfoundry.client.lib.util.JsonUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.diego.CFInfo;

public class BuildpackSupport extends CFClientV1Support {

	public BuildpackSupport(CloudFoundryOperations cfClient, CFInfo cloudInfo,
			HttpProxyConfiguration httpProxyConfiguration, CloudFoundryServer cfServer, boolean trustSelfSigned) {
		super(cfClient, /* no session space required */ null, cloudInfo, httpProxyConfiguration, cfServer, trustSelfSigned);
	}

	@SuppressWarnings("unchecked")
	public List<String> getBuildpacks() {
		List<String> buildpacks = new ArrayList<String>();
		String json = restTemplate.getForObject(getUrl("/v2/buildpacks"), String.class); //$NON-NLS-1$
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

	public static BuildpackSupport create(CloudFoundryOperations client, CFInfo cloudInfo,
			HttpProxyConfiguration proxyConf, CloudFoundryServer cfServer, boolean selfSigned) {
		return new BuildpackSupport(client, cloudInfo, proxyConf, cfServer, selfSigned);
	}
}
