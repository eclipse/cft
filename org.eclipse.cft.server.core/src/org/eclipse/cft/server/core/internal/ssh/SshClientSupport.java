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
package org.eclipse.cft.server.core.internal.ssh;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.core.ISshClientSupport;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.client.CFClientV1Support;
import org.eclipse.cft.server.core.internal.client.CFInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * @author Kris De Volder
 */
public class SshClientSupport extends CFClientV1Support implements ISshClientSupport {

	private String sshClientId;

	public SshClientSupport(CloudFoundryOperations cfClient, CFInfo cloudInfo,
			HttpProxyConfiguration httpProxyConfiguration, CloudFoundryServer server, boolean trustSelfSigned) {
		super(cfClient, /* no session space required */ null, cloudInfo, httpProxyConfiguration, server,
				trustSelfSigned);
		this.sshClientId = cloudInfo.getSshClientId();
	}

	@Override
	public String getSshCode() {
		try {
			URIBuilder builder = new URIBuilder(tokenUrl + "/oauth/authorize"); //$NON-NLS-1$

			builder.addParameter("response_type" //$NON-NLS-1$
					, "code"); //$NON-NLS-1$
			builder.addParameter("grant_type", //$NON-NLS-1$
					"authorization_code"); //$NON-NLS-1$
			builder.addParameter("client_id", sshClientId); //$NON-NLS-1$

			URI url = new URI(builder.toString());

			ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
			HttpStatus statusCode = response.getStatusCode();
			if (statusCode != HttpStatus.FOUND) {
				throw new CloudFoundryException(statusCode);
			}

			String loc = response.getHeaders().getFirst("Location"); //$NON-NLS-1$
			if (loc == null) {
				throw new CloudOperationException("No 'Location' header in redirect response"); //$NON-NLS-1$
			}
			List<NameValuePair> qparams = URLEncodedUtils.parse(new URI(loc), "utf8"); //$NON-NLS-1$
			for (NameValuePair pair : qparams) {
				String name = pair.getName();
				if (name.equals("code")) { //$NON-NLS-1$
					return pair.getValue();
				}
			}
			throw new CloudOperationException("No 'code' param in redirect Location: " + loc); //$NON-NLS-1$
		}
		catch (URISyntaxException e) {
			throw new CloudOperationException(e);
		}
	}

	public SshHost getSshHost() {
		return getCloudInfo().getSshHost();
	}

	public static ISshClientSupport create(final CloudFoundryOperations client, CFInfo cloudInfo,
			HttpProxyConfiguration proxyConf, CloudFoundryServer cfServer, boolean selfSigned) {
		return new SshClientSupport(client, cloudInfo, proxyConf, cfServer, selfSigned);
	}

	@Override
	public Session connect(String appName, int appInstance, IServer server, IProgressMonitor monitor)
			throws CoreException {

		CloudFoundryServer cloudServer = CloudServerUtil.getCloudServer(server);
		CloudApplication app = cloudServer.getBehaviour().getCloudApplication(appName, monitor);

		if (app == null) {
			throw CloudErrorUtil.toCoreException(NLS.bind(Messages.SshClientSupport_NO_CLOUD_APP, appName));
		}

		JSch jsch = new JSch();

		String user = "cf:" //$NON-NLS-1$
				+ app.getMeta().getGuid().toString() + "/" + appInstance; //$NON-NLS-1$

		String oneTimeCode = null;
		try {
			Session session = jsch.getSession(user, getSshHost().getHost(), getSshHost().getPort());

			oneTimeCode = getSshCode();

			session.setPassword(oneTimeCode);
			session.setUserInfo(getUserInfo(oneTimeCode));
			session.setServerAliveInterval(15 * 1000); // Avoid timeouts
			session.connect();

			return session;

		}
		catch (JSchException e) {
			throw CloudErrorUtil.asCoreException("SSH connection error " + e.getMessage() //$NON-NLS-1$
					, e, false);
		}
	}

	protected UserInfo getUserInfo(final String accessToken) {
		return new UserInfo() {

			@Override
			public void showMessage(String arg0) {
			}

			@Override
			public boolean promptYesNo(String arg0) {
				return true;
			}

			@Override
			public boolean promptPassword(String arg0) {
				return true;
			}

			@Override
			public boolean promptPassphrase(String arg0) {
				return false;
			}

			@Override
			public String getPassword() {
				return accessToken;
			}

			@Override
			public String getPassphrase() {
				return null;
			}
		};
	}

}
