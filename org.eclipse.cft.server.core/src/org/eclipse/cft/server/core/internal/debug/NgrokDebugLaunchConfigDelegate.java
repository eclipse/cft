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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.ApplicationAction;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.wst.server.core.IModule;

public class NgrokDebugLaunchConfigDelegate extends CloudFoundryDebugDelegate {

	public static final String LAUNCH_CONFIGURATION_ID = "org.eclipse.cft.debug.launchconfig.ngrok"; //$NON-NLS-1$

	private final static Pattern NGROK_OUTPUT_FILE = Pattern.compile(".*ngrok\\.txt"); //$NON-NLS-1$

	@Override
	public String getLaunchConfigurationTypeId() {
		return LAUNCH_CONFIGURATION_ID;
	}

	protected EnvironmentVariable getDebugEnvironment(ApplicationDeploymentInfo info) {
		List<EnvironmentVariable> vars = info.getEnvVariables();
		for (EnvironmentVariable var : vars) {
			if (NgrokDebugProvider.JAVA_OPTS.equals(var.getVariable())) {
				return var;
			}
		}
		return null;
	}

	public boolean configureApp(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer, int instance,
			int debugPort, IProgressMonitor monitor) throws CoreException {

		ApplicationDeploymentInfo info = appModule.getDeploymentInfo();
		List<EnvironmentVariable> vars = info.getEnvVariables();
		EnvironmentVariable javaOpts = getDebugEnvironment(info);

		IModule[] mod = new IModule[] { appModule.getLocalModule() };

		boolean restart = CloudFoundryProperties.isModuleStopped.testProperty(mod, cloudServer);

		if (!NgrokDebugProvider.containsDebugOption(javaOpts)) {
			if (javaOpts == null) {
				javaOpts = new EnvironmentVariable();
				javaOpts.setVariable(NgrokDebugProvider.JAVA_OPTS);
				vars.add(javaOpts);
			}

			String value = javaOpts.getValue();
			String debugOpts = "-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=4000,suspend=n"; //$NON-NLS-1$
			if (value == null) {
				value = debugOpts;
			}
			else {
				value = value + ' ' + debugOpts;
			}

			javaOpts.setValue(value);

			cloudServer.getBehaviour().operations().environmentVariablesUpdate(appModule.getLocalModule(),
					appModule.getDeployedApplicationName(), vars).run(monitor);

			restart = true;

		}

		if (restart) {
			cloudServer.getBehaviour().operations().applicationDeployment(mod, ApplicationAction.START, false)
					.run(monitor);
		}
		return true;

	}

	@Override
	protected DebugConnectionDescriptor getDebugConnectionDescriptor(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, int appInstance, int remoteDebugPort, IProgressMonitor monitor)
					throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

		IFile ngrokFile = NgrokDebugProvider.getFile(appModule.getLocalModule().getProject(), ".profile.d", "ngrok.sh"); //$NON-NLS-1$ //$NON-NLS-2$

		String remoteNgrokOutputFile = null;

		if (ngrokFile != null && ngrokFile.getRawLocation() != null) {
			try {

				Reader reader = new FileReader(new File(ngrokFile.getRawLocation().toString()));
				StringWriter writer = new StringWriter();
				try {
					IOUtils.copy(reader, writer);
				}
				finally {
					reader.close();
				}
				String fileContents = writer.toString();
				if (fileContents != null) {
					String[] segments = fileContents.split(">");
					if (segments.length >= 2) {
						Matcher matcher = NGROK_OUTPUT_FILE.matcher(segments[1]);
						if (matcher.find()) {
							remoteNgrokOutputFile = segments[1].substring(matcher.start(), matcher.end());
							if (remoteNgrokOutputFile != null) {
								remoteNgrokOutputFile = remoteNgrokOutputFile.trim();
							}
						}
					}
				}
			}
			catch (FileNotFoundException e) {
				throw CloudErrorUtil.toCoreException(e);
			}
			catch (IOException e) {
				throw CloudErrorUtil.toCoreException(e);
			}
		}

		if (remoteNgrokOutputFile == null) {
			String errorMessage = "Unable to connect the debugger. Failed to resolve a path to an output ngrok.txt file from the local ngrok.sh file. Please ensure the shell script file exists in your project and is in the project's classpath. Also please ensure that the script includes a command for the ngrok executable running on the Cloud to generate output to an ngrok.txt."; //$NON-NLS-1$
			throw CloudErrorUtil.toCoreException(errorMessage);
		}

		configureApp(appModule, cloudServer, appInstance, remoteDebugPort, subMonitor);

		String fileContent = NgrokDebugProvider.getFileContent(appModule, cloudServer, remoteNgrokOutputFile,
				subMonitor);

		if (fileContent.indexOf("Tunnel established at tcp://ngrok.com:") > -1) { //$NON-NLS-1$
			String pattern = "Tunnel established at tcp://ngrok.com:"; //$NON-NLS-1$
			int start = fileContent.indexOf(pattern);
			String sub = fileContent.substring(start);
			int end = sub.indexOf('\n');
			sub = sub.substring(pattern.length(), end);
			int port = Integer.parseInt(sub.trim());

			DebugConnectionDescriptor descriptor = new DebugConnectionDescriptor("ngrok.com", port); //$NON-NLS-1$

			if (!descriptor.isValid()) {
				throw CloudErrorUtil.toCoreException(
						"Invalid port:" + descriptor.getPort() + " or ngrok server address: " + descriptor.getHost() //$NON-NLS-1$ //$NON-NLS-2$
								+ " parsed from ngrok output file in the Cloud."); //$NON-NLS-1$
			}
			return descriptor;
		}
		else {
			throw CloudErrorUtil.toCoreException(
					"Unable to parse port or ngrok server address from the ngrok output file in the Cloud for " //$NON-NLS-1$
							+ appModule.getDeployedApplicationName()
							+ ". Please verify that ngrok executable is present in the application deployment and running in the Cloud"); //$NON-NLS-1$
		}
	}

}
