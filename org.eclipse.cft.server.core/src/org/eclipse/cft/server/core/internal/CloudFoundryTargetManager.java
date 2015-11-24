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

public class CloudFoundryTargetManager {

	List<CloudFoundryServerTarget> targets = new ArrayList<CloudFoundryServerTarget>();

	public CloudFoundryTargetManager() {

	}

	public synchronized void addTarget(CloudFoundryServerTarget target) {
		if (target != null && !targets.contains(target)) {
			targets.add(target);
		}
	}

	/**
	 * Obtain additional information about the server target.
	 * @param serverUrl
	 * @return non-null server target. If a specific one for the given server
	 * vendor is not found, a default one is returned
	 */
	public synchronized CloudFoundryServerTarget getTarget(CloudFoundryServer cloudServer) {
		// Fetch by server URL first
		CloudFoundryServerTarget serverTarget = null;
		String serverUrl = cloudServer.getUrl();
		for (CloudFoundryServerTarget target : targets) {
			if (serverUrl.contains(target.getServerUri())) {
				serverTarget = target;
				break;
			}
		}

		// Search by CC API version
		String ccApiVersion = cloudServer.getCloudInfo().getCloudControllerApiVersion();
		if (serverTarget == null && ccApiVersion != null) {
			for (CloudFoundryServerTarget target : targets) {
				if (target.getCCApiVersion() != null
						&& ccApiVersion.compareTo(target.getCCApiVersion()) >= 0) {
					serverTarget = target;
					break;
				}
			}
		}

		if (serverTarget == null) {
			serverTarget = CloudFoundryServerTarget.DEFAULT;
		}
		return serverTarget;
	}
}
