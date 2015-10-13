/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal.client;

import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.internal.ApplicationAction;

/**
 * Local deployment configuration for an application that is specific to Eclipse
 * only. It is NOT the same as {@link ApplicationDeploymentInfo}, which
 * represents the deployment information of the app in the Cloud Foundry server
 * (like memory scaling, URLs, etc..). The deployment configuration is not meant
 * to be persisted, or the information sent to the Cloud Foundry Server.
 * <p/>
 * One deployment configuration is the start mode for the application.
 */
public class DeploymentConfiguration {

	private final ApplicationAction startMode;

	public DeploymentConfiguration(ApplicationAction startMode) {
		this.startMode = startMode;
	}

	/**
	 * @return the start mode of the application. Use
	 * {@link ApplicationAction#STOP} if the application should be deployed but
	 * not started.
	 */
	public ApplicationAction getApplicationStartMode() {
		return startMode;
	}

}
