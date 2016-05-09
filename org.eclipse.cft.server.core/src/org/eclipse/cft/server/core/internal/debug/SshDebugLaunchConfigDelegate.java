/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal.debug;

import java.util.List;

import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.internal.ApplicationAction;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.application.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.diego.CFInfo;
import org.eclipse.cft.server.core.internal.client.diego.CloudInfoSsh;
import org.eclipse.cft.server.core.internal.ssh.SshClientSupport;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SshDebugLaunchConfigDelegate extends CloudFoundryDebugDelegate {

	private static final String JAVA_OPTS = "JAVA_OPTS"; //$NON-NLS-1$

	public static final String LAUNCH_CONFIGURATION_ID = "org.eclipse.cft.debug.launchconfig.ssh"; //$NON-NLS-1$

	@Override
	public String getLaunchConfigurationTypeId() {
		return LAUNCH_CONFIGURATION_ID;
	}

	protected boolean containsDebugOption(EnvironmentVariable var) {
		return var != null && var.getValue() != null && JAVA_OPTS.equals(var.getVariable())
				&& (var.getValue().contains("-Xdebug") || var.getValue().contains("-Xrunjdwp")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected void setEnvironmentVariable(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			int remoteDebugPort, IProgressMonitor monitor) throws CoreException {

		ApplicationDeploymentInfo info = appModule.getDeploymentInfo();
		List<EnvironmentVariable> vars = info.getEnvVariables();
		EnvironmentVariable javaOpts = getDebugEnvironment(info);

		IModule[] mod = new IModule[] { appModule.getLocalModule() };

		boolean restart = CloudFoundryProperties.isModuleStopped.testProperty(mod, cloudServer);

		if (!containsDebugOption(javaOpts)) {

			if (javaOpts == null) {
				javaOpts = new EnvironmentVariable();
				javaOpts.setVariable(JAVA_OPTS);
				vars.add(javaOpts);
			}

			String debugOpts = "-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=" + remoteDebugPort //$NON-NLS-1$
					+ ",suspend=n"; //$NON-NLS-1$
			javaOpts.setValue(debugOpts);

			cloudServer.getBehaviour().operations().environmentVariablesUpdate(appModule.getLocalModule(),
					appModule.getDeployedApplicationName(), vars).run(monitor);

			restart = true;
		}

		if (restart) {
			printToConsole(appModule, cloudServer, NLS.bind(Messages.SshDebugLaunchConfigDelegate_RESTARTING_APP,
					appModule.getDeployedApplicationName()), false);

			cloudServer.getBehaviour().operations().applicationDeployment(mod, ApplicationAction.START, false)
					.run(monitor);
		}
	}

	protected DebugConnectionDescriptor getSshConnectionDescriptor(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, int appInstance, int remoteDebugPort, IProgressMonitor monitor)
			throws CoreException {

		CFInfo cloudInfo = cloudServer.getBehaviour().getCloudInfo();
		if (cloudInfo instanceof CloudInfoSsh) {
			SshClientSupport ssh = SshClientSupport.create(cloudServer.getBehaviour().getClient(monitor),
					(CloudInfoSsh) cloudInfo, cloudServer.getProxyConfiguration(),
					cloudServer.isSelfSigned());

			try {
				printToConsole(appModule, cloudServer,
						NLS.bind(Messages.SshDebugLaunchConfigDelegate_CONNECTING_FOR_USER,
								appModule.getDeployedApplicationName()),
						false);

				Session session = ssh.connect(appModule.getApplication(), cloudServer, appInstance);

				printToConsole(appModule, cloudServer,
						NLS.bind(Messages.SshDebugLaunchConfigDelegate_CONNECTION_SUCCESSFUL,
								appModule.getDeployedApplicationName()),
						false);

				int localDebuggerPort = session.setPortForwardingL(0, "localhost", remoteDebugPort); //$NON-NLS-1$

				printToConsole(appModule, cloudServer,
						NLS.bind(Messages.SshDebugLaunchConfigDelegate_PORT_FORWARDING_SUCCESSFUL, remoteDebugPort,
								localDebuggerPort),
						false);

				return new DebugConnectionDescriptor("localhost", localDebuggerPort); //$NON-NLS-1$

			}
			catch (JSchException e) {
				throw CloudErrorUtil.toCoreException("SSH connection error " + e.getMessage());//$NON-NLS-1$
			}
		}
		else {
			throw CloudErrorUtil.toCoreException(
					"Unable to resolve SSH connection information from the Cloud Foundry target. Please ensure SSH is supported.");//$NON-NLS-1$
		}

	}

	protected EnvironmentVariable getDebugEnvironment(ApplicationDeploymentInfo info) {
		List<EnvironmentVariable> vars = info.getEnvVariables();
		for (EnvironmentVariable var : vars) {
			if (JAVA_OPTS.equals(var.getVariable())) {
				return var;
			}
		}
		return null;
	}

	@Override
	protected DebugConnectionDescriptor getDebugConnectionDescriptor(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, int appInstance, int remoteDebugPort, IProgressMonitor monitor)
			throws CoreException {
		setEnvironmentVariable(appModule, cloudServer, remoteDebugPort, monitor);

		return getSshConnectionDescriptor(appModule, cloudServer, appInstance, remoteDebugPort, monitor);
	}

	@Override
	protected void printToConsole(CloudFoundryApplicationModule appModule, CloudFoundryServer server, String message,
			boolean error) {
		super.printToConsole(appModule, server, NLS.bind(Messages.Ssh_CONSOLE_MESSAGE, message), error);
	}

}
