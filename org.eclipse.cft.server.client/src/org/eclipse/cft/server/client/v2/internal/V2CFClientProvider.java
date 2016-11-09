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
package org.eclipse.cft.server.client.v2.internal;

import org.cloudfoundry.client.CloudFoundryClient;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.ProviderPriority;
import org.eclipse.cft.server.core.internal.client.CFClient;
import org.eclipse.cft.server.core.internal.client.CFClientProvider;
import org.eclipse.cft.server.core.internal.client.CFCloudCredentials;
import org.eclipse.cft.server.core.internal.client.CFInfo;
import org.eclipse.cft.server.core.internal.spaces.CloudFoundrySpace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IServer;
import org.osgi.framework.Version;

public class V2CFClientProvider implements CFClientProvider {
	


	@Override
	public ProviderPriority getPriority() {
		return ProviderPriority.HIGH;
	}

	@Override
	public boolean supports(String serverUrl, CFInfo info) {
		return info != null && info.getDopplerUrl() != null && supportsVersion(info.getCCApiVersion());
	}

	@Override
	public CFClient getClient(IServer cloudServer, CFCloudCredentials credentials, CloudFoundrySpace cloudFoundrySpace,
			IProgressMonitor monitor) throws CoreException {
		// Passcode not supported yet
		if (credentials.isPasscodeSet()) {
			throw CloudErrorUtil.toCoreException("One-time passcode not supported in this version of v2 client for doppler log streaming.");
		}
		CloudFoundryServer cfServer = CloudServerUtil.getCloudServer(cloudServer);
		if (cfServer  != null) {
			return new V2Client(cfServer, credentials, cloudFoundrySpace);
		}
		return null;
	}

	protected boolean supportsVersion(Version ccApiVersion) {
		if (ccApiVersion == null) {
			return false;
		}
		Version supported = getSupportedV2ClientApiVersion();
		return ccApiVersion.compareTo(supported) > 0;
	}

	public Version getSupportedV2ClientApiVersion() {
		return new Version(CloudFoundryClient.SUPPORTED_API_VERSION);
	}

}
