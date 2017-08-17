/*******************************************************************************
 * Copyright (c) 2015, 2017 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal.client.diego;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.client.BaseClientRequest;
import org.eclipse.cft.server.core.internal.client.BehaviourRequest;
import org.eclipse.cft.server.core.internal.client.CFInfo;
import org.eclipse.cft.server.core.internal.client.ClientRequestFactory;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.core.internal.client.FileSshSessionConnPool;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;

public class DiegoRequestFactory extends ClientRequestFactory {

	/** Connection pool which reuses SSH sessions (and retries failures) when acquiring files through Diego's SSH API.*/
	private final FileSshSessionConnPool fileSshConnectionPool;

	public DiegoRequestFactory(CloudFoundryServerBehaviour behaviour) {
		super(behaviour);
		 fileSshConnectionPool = new FileSshSessionConnPool(behaviour);
	}

	@Override
	public BaseClientRequest<CloudApplication> getCloudApplication(final String appName) throws CoreException {

		return new BehaviourRequest<CloudApplication>(
				NLS.bind(Messages.CloudFoundryServerBehaviour_GET_APPLICATION, appName), behaviour) {
			@Override
			protected CloudApplication doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				try {
					return client.getApplication(appName);
				}
				catch (Exception e) {
					// In some cases fetching app stats to retrieve running
					// instances throws 503 due to
					// CF backend error
					if (CloudErrorUtil.is503Error(e)) {
						return client.getAdditionalRestOperations().getBasicApplication(appName);
					}
					else {
						throw e;
					}
				}
			}
		};
	}

	@Override
	public BaseClientRequest<List<CloudApplication>> getApplications() throws CoreException {

		final String serverId = behaviour.getCloudFoundryServer().getServer().getId();

		final String label = NLS.bind(Messages.CloudFoundryServerBehaviour_GET_ALL_APPS, serverId);

		return new BehaviourRequest<List<CloudApplication>>(label, behaviour) {
			@Override
			protected List<CloudApplication> doRun(CloudFoundryOperations client, SubMonitor progress)
					throws CoreException {

				try {
					return client.getApplications();
				}
				catch (Exception e) {
					// In some cases fetching app stats to retrieve running
					// instances throws 503 due to
					// CF backend error
					if (CloudErrorUtil.is503Error(e)) {
						return client.getAdditionalRestOperations().getBasicApplications();
					}
					else {
						throw e;
					}
				}
			}
		};
	}

	@Override
	public BaseClientRequest<?> stopApplication(final String message, final CloudFoundryApplicationModule cloudModule) {
		return new BehaviourRequest<Void>(message, behaviour) {
			@Override
			protected Void doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {
				try {
					client.stopApplication(cloudModule.getDeployedApplicationName());
				}
				catch (Exception e) {
					// In some cases fetching app stats to retrieve running
					// instances throws 503 due to
					// CF backend error
					if (CloudErrorUtil.is503Error(e)) {
						client.getAdditionalRestOperations()
								.stopApplicationUsingBasicApp(cloudModule.getDeployedApplicationName());
					}
					else {
						throw e;
					}
				}

				return null;
			}
		};
	}

	@Override
	public BaseClientRequest<String> getFile(final CloudApplication app, final int instanceIndex, final String path,
			final boolean isDir) throws CoreException {

		final CloudFoundryServer cloudServer = behaviour.getCloudFoundryServer();

		// If ssh is not supported, try the default legacy file fetching
		if (!supportsSsh()) {
			return super.getFile(app, instanceIndex, path, isDir);
		}

		String label = NLS.bind(Messages.CloudFoundryServerBehaviour_FETCHING_FILE, path, app.getName());
		return new BehaviourRequest<String>(label, behaviour) {
			@Override
			protected String doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException {

				if (path == null) {
					return null;
				}

				// Returns file/directory contents on success, or throws CoreException on failure.
				return fileSshConnectionPool.processSshSessionRequest(app, instanceIndex, path, isDir, new NullProgressMonitor());				

			}
		};
	}

	@Override
	public CFInfo getCloudInfo() throws CoreException {
		if (cachedInfo == null) {
			CloudFoundryServer cloudServer = behaviour.getCloudFoundryServer();
			cachedInfo = new CFInfo(new CloudCredentials(cloudServer.getUsername(), cloudServer.getPassword()),
					cloudServer.getUrl(), cloudServer.getProxyConfiguration(), cloudServer.isSelfSigned());
		}
		return cachedInfo;
	}

	@Override
	public boolean supportsSsh() {
		try {
			CFInfo info = getCloudInfo();
			return info != null && info.getSshClientId() != null && info.getSshHost() != null
					&& info.getSshHost().getHost() != null && info.getSshHost().getFingerPrint() != null;
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return false;

	}

	protected String getContent(Channel channel) throws CoreException {
		InputStream in = null;
		OutputStream outStream = null;
		try {
			in = channel.getInputStream();
			channel.connect();

			if (in != null) {

				ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
				outStream = new BufferedOutputStream(byteArrayOut);
				byte[] buffer = new byte[4096];
				int bytesRead = -1;

				while ((bytesRead = in.read(buffer)) != -1) {
					outStream.write(buffer, 0, bytesRead);
				}
				outStream.flush();
				byteArrayOut.flush();

				return byteArrayOut.toString();
			}
		}
		catch (IOException e) {
			throw CloudErrorUtil.toCoreException(e);
		}
		catch (JSchException e) {
			throw CloudErrorUtil.toCoreException(e);
		}
		finally {
			channel.disconnect();
			try {
				if (in != null) {
					in.close();
				}
				if (outStream != null) {
					outStream.close();
				}
			}
			catch (IOException e) {
				// Don't prevent any operation from completing if streams don't
				// close. Just log error
				CloudFoundryPlugin.logError(e);
			}
		}
		return null;
	}

}
