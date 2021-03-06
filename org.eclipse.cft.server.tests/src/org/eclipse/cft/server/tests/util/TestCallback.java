/*******************************************************************************
 * Copyright (c) 2012, 2017 Pivotal Software, Inc.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.cft.server.core.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.ApplicationAction;
import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudApplicationURL;
import org.eclipse.cft.server.core.internal.CloudFoundryCallback;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.DeploymentConfiguration;
import org.eclipse.cft.server.core.internal.client.DeploymentInfoWorkingCopy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

/**
 * @author Steffen Pingel
 */
public class TestCallback extends CloudFoundryCallback {

	private final String appName;

	private final String url;

	private final int memory;

	private final int diskQuota;

	private final boolean startApp;

	private final List<EnvironmentVariable> variables;

	private final List<CFServiceInstance> services;

	private final String buildpack;

	public TestCallback(String appName, int memory, int diskQuota, boolean startApp,
			List<EnvironmentVariable> variables, List<CFServiceInstance> services, String buildpack) {
		this.appName = appName;
		this.url = null;
		this.startApp = startApp;
		this.memory = memory;
		this.diskQuota = diskQuota;
		this.variables = variables;
		this.services = services;
		this.buildpack = buildpack;
	}

	@Override
	public void applicationStarted(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule) {
		// ignore
	}

	@Override
	public void stopApplicationConsole(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {
		// ignore
	}

	@Override
	public void disconnecting(CloudFoundryServer server) {
		// ignore
	}

	@Override
	public void getCredentials(CloudFoundryServer server) {
		throw new OperationCanceledException();
	}

	@Override
	public DeploymentConfiguration prepareForDeployment(CloudFoundryServer server, CloudFoundryApplicationModule module,
			IProgressMonitor monitor) throws CoreException {

		// NOTE:
		// This
		// section
		// here
		// is
		// a
		// substitute
		// for
		// the
		// Application

		DeploymentInfoWorkingCopy copy = module.resolveDeploymentInfoWorkingCopy(monitor);
		copy.setDeploymentName(appName);
		copy.setMemory(memory);
		copy.setDiskQuota(diskQuota);
		if (variables != null) {
			copy.setEnvVariables(variables);
		}
		if (services != null) {
			copy.setServices(services);
		}
		if (buildpack != null) {
			copy.setBuildpack(buildpack);
		}

		if (url != null) {
			copy.setUris(Collections.singletonList(url));
		}
		else {
			// Derive the URL from the app name specified in the test call back.
			// NOTE that although the working copy SHOULD have a default URL
			// generated from a default application name
			// (generally, the project name), since the appname and project name
			// can be different,
			// and such difference would be specified manually by the user in
			// the deployment wizard,
			// be sure to generate a URL from the actual app name specified in
			// this Test call back, to be sure
			// the URL is built off the app name rather than the project name,
			// as the test case may have specified
			// a different app name than the default app name from the project
			// name.

			ApplicationUrlLookupService urlLookup = ApplicationUrlLookupService.update(server, monitor);

			CloudApplicationURL url = urlLookup.getDefaultApplicationURL(copy.getDeploymentName());
			if (url != null) {
				copy.setUris(Arrays.asList(url.getUrl()));
			}
		}

		copy.save();

		ApplicationAction mode = startApp ? ApplicationAction.START : ApplicationAction.STOP;

		return new DeploymentConfiguration(mode);
	}

	@Override
	public void deleteServices(List<String> services, CloudFoundryServer cloudServer) {
		// ignore
	}

	private boolean autoDeployEnabled;

	@Override
	public boolean isAutoDeployEnabled() {
		return autoDeployEnabled;
	}

	public void setAutoDeployEnabled(boolean autoDeployEnabled) {
		this.autoDeployEnabled = autoDeployEnabled;
	}

	@Override
	public void deleteApplication(CloudFoundryApplicationModule cloudModule, CloudFoundryServer cloudServer) {
		// ignore
	}

	@Override
	public void applicationStarting(CloudFoundryServer server, CloudFoundryApplicationModule cloudModule) {
	}

	@Override
	public boolean ssoLoginUserPrompt(CloudFoundryServer cloudServer) {
		return false;
	}

}
