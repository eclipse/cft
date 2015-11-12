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

import org.cloudfoundry.client.lib.CloudCredentials;
import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.internal.ApplicationAction;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.application.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.core.internal.ssh.SshClientSupport;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

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

			printToConsole(appModule, cloudServer,
					"Setting JAVA_OPTS with debug options. Debug port: " + remoteDebugPort, //$NON-NLS-1$
					false);

			cloudServer.getBehaviour().operations().environmentVariablesUpdate(appModule.getLocalModule(),
					appModule.getDeployedApplicationName(), vars).run(monitor);

			restart = true;

			printToConsole(appModule, cloudServer, "JAVA_OPTS with debug options successfully set", false); //$NON-NLS-1$
			printToConsole(appModule, cloudServer, "JAVA_OPTS: " + javaOpts.getValue(), false); //$NON-NLS-1$
		}

		if (restart) {
			printToConsole(appModule, cloudServer,
					"Restarting application in debug mode - " + appModule.getDeployedApplicationName(), false); //$NON-NLS-1$

			cloudServer.getBehaviour().operations().applicationDeployment(mod, ApplicationAction.START, false)
					.run(monitor);
		}
	}

	protected DebugConnectionDescriptor getSshConnectionDescriptor(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, int appInstance, int remoteDebugPort, IProgressMonitor monitor)
					throws CoreException {

		String url = cloudServer.getUrl();
		String userName = cloudServer.getUsername();
		String password = cloudServer.getPassword();
		boolean selfSigned = cloudServer.getSelfSignedCertificate();

		SshClientSupport ssh = SshClientSupport.create(
				CloudFoundryServerBehaviour.createExternalClientLogin(url, userName, password, selfSigned, monitor),
				new CloudCredentials(userName, password), null, selfSigned);

		JSch jsch = new JSch();

		String user = "cf:" //$NON-NLS-1$
				+ appModule.getApplication().getMeta().getGuid().toString() + "/" + appInstance; //$NON-NLS-1$

		printToConsole(appModule, cloudServer, "Connecting SSH session for user  " //$NON-NLS-1$
				+ user, false);

		String oneTimeCode = null;
		try {
			Session session = jsch.getSession(user, ssh.getSshHost().getHost(), ssh.getSshHost().getPort());

			oneTimeCode = ssh.getSshCode();

			printToConsole(appModule, cloudServer, "Successfully obtained one-time SSH code" //$NON-NLS-1$
					, false);

			session.setPassword(oneTimeCode);
			session.setUserInfo(getUserInfo(oneTimeCode));
			session.setServerAliveInterval(15 * 1000); // Avoid timeouts during
														// debugging
			session.connect();

			printToConsole(appModule, cloudServer, "Successfully connected SSH client using one-time SSH code" //$NON-NLS-1$
					, false);

			int localDebuggerPort = session.setPortForwardingL(0, "localhost", remoteDebugPort); //$NON-NLS-1$

			printToConsole(appModule, cloudServer,
					"Successfully completed port forwarding from remote port: " //$NON-NLS-1$
							+ remoteDebugPort + " to local port: " //$NON-NLS-1$
							+ localDebuggerPort,
					false);

			return new DebugConnectionDescriptor("localhost", localDebuggerPort); //$NON-NLS-1$

		}
		catch (JSchException e) {
			throw CloudErrorUtil.asCoreException("SSH connection error " + e.getMessage() //$NON-NLS-1$
					+ ". One time code: " + oneTimeCode, e, false); //$NON-NLS-1$
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

	@Override
	protected DebugConnectionDescriptor getDebugConnectionDescriptor(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, int appInstance, int remoteDebugPort, IProgressMonitor monitor)
					throws CoreException {
		setEnvironmentVariable(appModule, cloudServer, remoteDebugPort, monitor);

		return getSshConnectionDescriptor(appModule, cloudServer, appInstance, remoteDebugPort, monitor);
	}

}
