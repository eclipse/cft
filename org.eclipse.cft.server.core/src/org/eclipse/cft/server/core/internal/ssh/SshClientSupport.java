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
package org.eclipse.cft.server.core.internal.ssh;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.AuthorizationHeaderProvider;
import org.eclipse.cft.server.core.internal.client.V1ClientSupport;
import org.eclipse.cft.server.core.internal.client.v2.CloudInfoV2;
import org.eclipse.core.runtime.CoreException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * @author Kris De Volder
 */
public class SshClientSupport extends V1ClientSupport {

	private String sshClientId;

	public SshClientSupport(AuthorizationHeaderProvider oauth, CloudInfoV2 cloudInfo, boolean trustSelfSigned,
			HttpProxyConfiguration httpProxyConfiguration) {
		super(oauth, cloudInfo, trustSelfSigned, httpProxyConfiguration);
		this.sshClientId = cloudInfo.getSshClientId();
	}

	public String getSshCode() {
		try {
			URIBuilder builder = new URIBuilder(authorizationUrl + "/oauth/authorize"); //$NON-NLS-1$

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
		return cloudInfo.getSshHost();
	}

	public static SshClientSupport create(final CloudFoundryOperations client, CloudCredentials creds,
			HttpProxyConfiguration proxyConf, boolean selfSigned) {
		AuthorizationHeaderProvider oauth = new AuthorizationHeaderProvider() {
			public String getAuthorizationHeader() {
				OAuth2AccessToken token = client.login();
				return token.getTokenType() + " " + token.getValue();
			}
		};

		CloudInfoV2 cloudInfo = new CloudInfoV2(creds, client.getCloudControllerUrl().toString(), proxyConf,
				selfSigned);

		return new SshClientSupport(oauth, cloudInfo, selfSigned, proxyConf);
	}

	public Session connect(CloudApplication app, CloudFoundryServer cloudServer, int appInstance) throws CoreException {

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
