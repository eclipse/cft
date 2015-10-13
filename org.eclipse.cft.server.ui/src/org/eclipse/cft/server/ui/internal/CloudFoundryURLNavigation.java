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
 *     Keith Chong, IBM - Modify Sign-up so it's more brand-friendly
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal;

import org.eclipse.cft.server.core.internal.CloudFoundryBrandingExtensionPoint;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.osgi.util.NLS;

public class CloudFoundryURLNavigation extends UIWebNavigationHelper {

	public static final CloudFoundryURLNavigation INSIGHT_URL = new CloudFoundryURLNavigation(
			"http://insight.cloudfoundry.com/"); //$NON-NLS-1$

	public CloudFoundryURLNavigation(String location) {
		super(location, NLS.bind(Messages.CloudFoundryURLNavigation_TEXT_OPEN_LABEL, location));
	}

	public static boolean canEnableCloudFoundryNavigation(CloudFoundryServer server) {
		if (server == null) {
			return false;
		}
		return canEnableCloudFoundryNavigation(server.getServerId(), server.getUrl());
	}

	public static boolean canEnableCloudFoundryNavigation(String serverTypeId, String url) {
		if (serverTypeId == null) {
			return false;
		}
		// If the signupURL attribute is defined in the extension, then it will
		// enable the Signup button
		return CloudFoundryBrandingExtensionPoint.getSignupURL(serverTypeId, url) != null;
	}
}
