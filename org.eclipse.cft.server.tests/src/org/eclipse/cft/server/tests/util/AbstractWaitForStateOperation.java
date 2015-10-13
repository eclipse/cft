/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc.
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
package org.eclipse.cft.server.tests.util;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.WaitWithProgressJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Waits until a deployed application reaches an expected state.
 */
public abstract class AbstractWaitForStateOperation extends WaitWithProgressJob {
	private final CloudFoundryServer cloudServer;

	private final CloudFoundryApplicationModule appModule;

	public AbstractWaitForStateOperation(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule) {
		this(cloudServer, appModule, 10, 3000);
	}

	public AbstractWaitForStateOperation(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule,
			int attempts, long sleep) {
		super(attempts, sleep);
		this.cloudServer = cloudServer;
		this.appModule = appModule;
	}

	@Override
	protected boolean internalRunInWait(IProgressMonitor progress) throws CoreException {

		CloudApplication updatedCloudApp = cloudServer.getBehaviour()
				.getCloudApplication(appModule.getDeployedApplicationName(), progress);

		if (updatedCloudApp == null) {
			throw CloudErrorUtil
					.toCoreException("No cloud application found while attempting to check application state."); //$NON-NLS-1$
		}

		return isInState(updatedCloudApp.getState());

	}

	@Override
	protected boolean shouldRetryOnError(Throwable t) {
		// If cloud application cannot be resolved due to any errors, stop any
		// further attempts to check app state.
		return false;
	}

	protected abstract boolean isInState(AppState state);

}