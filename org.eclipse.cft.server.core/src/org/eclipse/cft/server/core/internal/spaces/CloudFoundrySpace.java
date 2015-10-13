/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal.spaces;

import org.cloudfoundry.client.lib.domain.CloudSpace;

/**
 * Local representation of a Cloud space, used when the cloud space is stored in
 * the WST server. It always contains an org and space name, but it may not
 * always be mapped to an actual client CloudSpace instance (so it may not
 * contain client metadata like GUID).
 * <p/>
 * For example, when start an existing Cloud Foundry WST server instance that
 * has the org and space name stored, a CloudFoundrySpace will be created with
 * this information, but it may not yet be linked to an actual client CloudSpace
 * until a Cloud space lookup is performed by the plugin (for example, when
 * creating a client to connect to the CF remote server).
 * 
 * @see CloudSpace
 */
public class CloudFoundrySpace {

	private CloudSpace space;

	private final String spaceName;

	private final String orgName;

	public CloudFoundrySpace(CloudSpace space) {
		this(space.getOrganization().getName(), space.getName());
		this.space = space;
	}

	public CloudFoundrySpace(String orgName, String spaceName) {
		this.orgName = orgName;
		this.spaceName = spaceName;
	}

	public String getOrgName() {
		return orgName;
	}

	public String getSpaceName() {
		return spaceName;
	}

	public CloudSpace getSpace() {
		return this.space;
	}

}
