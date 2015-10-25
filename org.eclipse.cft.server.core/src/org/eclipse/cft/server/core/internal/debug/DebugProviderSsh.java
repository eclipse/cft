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
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryProjectUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.application.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.core.internal.ssh.SshClientSupport;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaProject;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * Performs a connection to a given server and module. Handles network timeouts,
 * including retrying if connections failed.
 */
public class DebugProviderSsh extends AbstractDebugSshProvider {

	private static DebugProviderSsh defaultProvider;

	private static final String JAVA_OPTS = "JAVA_OPTS"; //$NON-NLS-1$

	@Override
	public DebugConnectionDescriptor getDebugConnectionDescriptor(final CloudFoundryApplicationModule appModule,
			final CloudFoundryServer cloudServer, int debugPort, int instance, IProgressMonitor monitor)
					throws CoreException, OperationCanceledException {

		return getSshConnectionDescriptor(appModule, cloudServer, debugPort, instance, monitor);
	}

	protected DebugConnectionDescriptor getSshConnectionDescriptor(final CloudFoundryApplicationModule appModule,
			final CloudFoundryServer cloudServer, int debugPort, int instance, IProgressMonitor monitor)
					throws CoreException, OperationCanceledException {

		String url = cloudServer.getUrl();
		String userName = cloudServer.getUsername();
		String password = cloudServer.getPassword();
		boolean selfSigned = cloudServer.getSelfSignedCertificate();

		SshClientSupport ssh = SshClientSupport.create(
				CloudFoundryServerBehaviour.createExternalClientLogin(url, userName, password, selfSigned, monitor),
				new CloudCredentials(userName, password), null, selfSigned);

		JSch jsch = new JSch();

		String user = "cf:" //$NON-NLS-1$
				+ appModule.getApplication().getMeta().getGuid().toString() + "/" + instance; //$NON-NLS-1$

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
			session.connect();

			printToConsole(appModule, cloudServer, "Successfully connected SSH client using one-time SSH code" //$NON-NLS-1$
					, false);

			int localDebuggerPort = session.setPortForwardingL(0, "localhost", debugPort); //$NON-NLS-1$

			printToConsole(appModule, cloudServer,
					"Successfully completed port forwarding from remote port: " //$NON-NLS-1$
							+ debugPort + " to local port: " //$NON-NLS-1$
							+ localDebuggerPort,
					false);

			return new DebugConnectionDescriptor("localhost", localDebuggerPort); //$NON-NLS-1$

		}
		catch (JSchException e) {
			throw CloudErrorUtil.asCoreException("SSH connection error " + e.getMessage() //$NON-NLS-1$
					+ ". One time code: " + oneTimeCode, e, false); //$NON-NLS-1$
		}
	}

	@Override
	public boolean canLaunch(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer,
			IProgressMonitor monitor) throws CoreException {
		return containsDebugOption(getDebugEnvironment(appModule.getDeploymentInfo()));
	}

	@Override
	public boolean isDebugSupported(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		IJavaProject javaProject = CloudFoundryProjectUtil.getJavaProject(appModule);
		return javaProject != null && javaProject.exists() && isSSHSupported(cloudServer, appModule);

	}

	@Override
	public String getLaunchConfigurationID() {
		return CloudFoundryDebuggingLaunchConfigDelegate.LAUNCH_CONFIGURATION_ID;
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
	public boolean configureApp(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer, int debugPort,
			IProgressMonitor monitor) throws CoreException {

		// Return true if app needs to restart. False otherwise

		ApplicationDeploymentInfo info = appModule.getDeploymentInfo();
		List<EnvironmentVariable> vars = info.getEnvVariables();
		EnvironmentVariable javaOpts = getDebugEnvironment(info);

		if (containsDebugOption(javaOpts)) {
			printToConsole(appModule, cloudServer,
					"JAVA_OPTS with debug options already exists. Remote debug port: " + debugPort, false); //$NON-NLS-1$
			printToConsole(appModule, cloudServer, "JAVA_OPTS: " + javaOpts.getValue(), false); //$NON-NLS-1$

			return false;
		}

		if (javaOpts == null) {
			javaOpts = new EnvironmentVariable();
			javaOpts.setVariable(JAVA_OPTS);
			vars.add(javaOpts);
		}

		String debugOpts = "-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=" + debugPort + ",suspend=n"; //$NON-NLS-1$ //$NON-NLS-2$
		javaOpts.setValue(debugOpts);

		printToConsole(appModule, cloudServer, "Setting JAVA_OPTS with debug options. Debug port: " + debugPort, false); //$NON-NLS-1$

		cloudServer.getBehaviour().operations()
				.environmentVariablesUpdate(appModule.getLocalModule(), appModule.getDeployedApplicationName(), vars)
				.run(monitor);

		printToConsole(appModule, cloudServer, "JAVA_OPTS with debug options successfully set", false); //$NON-NLS-1$
		printToConsole(appModule, cloudServer, "JAVA_OPTS: " + javaOpts.getValue(), false); //$NON-NLS-1$

		return true;

	}

	protected boolean containsDebugOption(EnvironmentVariable var) {
		return var != null && var.getValue() != null && JAVA_OPTS.equals(var.getVariable())
				&& (var.getValue().contains("-Xdebug") || var.getValue().contains("-Xrunjdwp")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static DebugProviderSsh getCurrent(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		if (defaultProvider == null) {
			defaultProvider = new DebugProviderSsh();
		}
		return defaultProvider;
	}

	protected boolean isSSHSupported(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule) {
		// For now enable for all PWS servers
		return cloudServer.getUrl().contains("api.run.pivotal.io"); //$NON-NLS-1$
	}

	protected UserInfo getUserInfo(final String accessToken) {
		return new UserInfo() {

			@Override
			public void showMessage(String arg0) {
				// TODO Auto-generated method stub
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
