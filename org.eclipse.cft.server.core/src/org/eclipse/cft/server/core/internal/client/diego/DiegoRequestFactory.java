/*******************************************************************************
 * Copyright (c) 2015, 2016 Pivotal Software, Inc. 
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

import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.client.ClientRequest;
import org.eclipse.cft.server.core.internal.client.ClientRequestFactory;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.CloudServerCFClient;
import org.eclipse.cft.server.core.internal.client.V1ClientRequest;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

/**
 * TODO: No longer used or necessary. Remove
 */
public class DiegoRequestFactory extends ClientRequestFactory {

	public DiegoRequestFactory(CloudFoundryServer cloudServer, CloudServerCFClient cfClient) throws CoreException{
		super(cloudServer, cfClient);
	}

	@Override
	public ClientRequest<CloudApplication> getCloudApplication(final String appName) throws CoreException {

		return new V1ClientRequest<CloudApplication>(cloudServer, cfClient,
				NLS.bind(Messages.CloudFoundryServerBehaviour_GET_APPLICATION, appName)) {
			@Override
			protected CloudApplication runV1Request(CloudFoundryOperations client, IProgressMonitor progress) throws CoreException {
				try {
					return client.getApplication(appName);
				}
				catch (Exception e) {
					// In some cases fetching app stats to retrieve running
					// instances throws 503 due to
					// CF backend error
					if (CloudErrorUtil.is503Error(e)) {
						return cfClient.getAdditionalV1Operations(progress).getBasicApplication(appName);
					}
					else {
						throw e;
					}
				}
			}
		};
	}

	@Override
	public ClientRequest<List<CloudApplication>> getApplications() throws CoreException {

		final String serverId = cloudServer.getServer().getId();

		final String label = NLS.bind(Messages.CloudFoundryServerBehaviour_GET_ALL_APPS, serverId);

		return new V1ClientRequest<List<CloudApplication>>(cloudServer,cfClient, label) {
			@Override
			protected List<CloudApplication> runV1Request(CloudFoundryOperations client, IProgressMonitor progress)
					throws CoreException {

				try {
					return client.getApplications();
				}
				catch (Exception e) {
					// In some cases fetching app stats to retrieve running
					// instances throws 503 due to
					// CF backend error
					if (CloudErrorUtil.is503Error(e)) {
						return cfClient.getAdditionalV1Operations(progress).getBasicApplications();
					}
					else {
						throw e;
					}
				}
			}
		};
	}

	@Override
	public ClientRequest<?> stopApplication(final String message, final CloudFoundryApplicationModule cloudModule) {
		return new V1ClientRequest<Void>(cloudServer, cfClient,message) {
			@Override
			protected Void runV1Request(CloudFoundryOperations client, IProgressMonitor progress) throws CoreException {
				try {
					client.stopApplication(cloudModule.getDeployedApplicationName());
				}
				catch (Exception e) {
					// In some cases fetching app stats to retrieve running
					// instances throws 503 due to
					// CF backend error
					if (CloudErrorUtil.is503Error(e)) {
						cfClient.getAdditionalV1Operations(progress)
								.stopApplication(cloudModule.getDeployedApplicationName());
					}
					else {
						throw e;
					}
				}

				return null;
			}
		};
	}

	
	

}
