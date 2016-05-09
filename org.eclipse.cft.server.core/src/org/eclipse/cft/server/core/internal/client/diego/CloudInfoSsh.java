/*******************************************************************************
 * Copied from Spring Tool Suite. Original license:
 * 
 * Copyright (c) 2015 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.cft.server.core.internal.client.diego;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.eclipse.cft.server.core.internal.ssh.SshHost;

/**
 * @author Kris De Volder
 */
public class CloudInfoSsh extends CFInfo {

	public CloudInfoSsh(CloudCredentials creds, String url, HttpProxyConfiguration proxyConf, boolean selfSigned) {
		super(creds, url, proxyConf, selfSigned);
	}

	public String getSshClientId() {
		return getProp("app_ssh_oauth_client"); //$NON-NLS-1$
	}

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
}
