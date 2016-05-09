/*******************************************************************************
 * Copyright (c) 2015, 2016 Pivotal Software, Inc. and others
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;

public class CloudFoundryTargetManager {

	// Ordered list of targets with highest priority first
	List<CloudFoundryServerTarget> targets = new ArrayList<CloudFoundryServerTarget>();

	public CloudFoundryTargetManager() {

	}

	/**
	 * Targets are priorities in the order that they are added, with first added
	 * having highest priority
	 * @param target
	 */
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
	public synchronized CloudFoundryServerTarget getTarget(CloudFoundryServer cloudServer) throws CoreException {
		// Fetch by server URL first
		CloudFoundryServerTarget cftarget = null;
		for (CloudFoundryServerTarget target : targets) {
			if (target.supports(cloudServer.getServer())) {
				cftarget = target;
				break;
			}
		}

		if (cftarget == null) {
			throw CloudErrorUtil.toCoreException(NLS.bind(Messages.CloudFoundryTargetManager_NO_TARGET_DEFINITION_FOUND,
					cloudServer.getServer().getId()));
		}
		return cftarget;
	}
}
