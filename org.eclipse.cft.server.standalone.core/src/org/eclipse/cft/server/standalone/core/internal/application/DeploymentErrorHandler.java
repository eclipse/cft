/*******************************************************************************
 * Copyright (c) 2016 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.standalone.core.internal.application;

import org.eclipse.cft.server.core.internal.CFConsoleHandler;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.standalone.core.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IModule;

public class DeploymentErrorHandler {

	private CFConsoleHandler consoleHandler;
	private CloudFoundryApplicationModule appModule;
	private IModule actualModule;
	private CloudFoundryServer cloudServer;

	public DeploymentErrorHandler(IModule actualModule, CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer, CFConsoleHandler consoleHandler) {
		this.actualModule = actualModule;
		this.appModule = appModule;
		this.cloudServer = cloudServer;
		this.consoleHandler = consoleHandler;
	}

	public void handleApplicationDeploymentFailure(String errorMessage) throws CoreException {
		if (errorMessage == null) {
			errorMessage = Messages.DeploymentErrorHandler_ERROR_CREATE_PACKAGED_FILE;
		}
		errorMessage += " - " //$NON-NLS-1$
				+ appModule.getDeployedApplicationName() + ". Unable to package application for deployment."; //$NON-NLS-1$
		if (consoleHandler != null) {
			consoleHandler.printErrorToConsole(actualModule, cloudServer, errorMessage);
		}
		throw CloudErrorUtil.toCoreException(errorMessage);
	}

	public void handleApplicationDeploymentFailure() throws CoreException {
		handleApplicationDeploymentFailure(null);
	}
}
