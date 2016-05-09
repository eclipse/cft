/*******************************************************************************
 * Copyright (c) 2013, 2016 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.ui.internal.wizards;

import java.util.List;

import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.cft.server.core.internal.ApplicationAction;
import org.eclipse.core.runtime.Assert;

/**
 * 
 * Descriptor that contains all the necessary information to push an application
 * to a Cloud Foundry server, such as the application's name, URL, framework,
 * and runtime
 * <p/>
 * This descriptor is shared by all the pages in the application deployment
 * wizard. Some values are required, and must always be set in order to push the
 * application to the server
 * 
 * IMPORTANT NOTE: This class can be referred by the branding extension from adopter so this class 
 * should not be moved or renamed to avoid breakage to adopters. 
 * 
 */
public class ApplicationWizardDescriptor {

	private final ApplicationDeploymentInfo deploymentInfo;

	private List<CFServiceInstance> createdCloudServices;

	private boolean persistDeploymentInfo;

	private ApplicationAction applicationStartMode;

	public ApplicationWizardDescriptor(ApplicationDeploymentInfo deploymentInfo) {
		Assert.isNotNull(deploymentInfo);

		this.deploymentInfo = deploymentInfo;
	}

	public void setBuildpack(String buildpack) {
		deploymentInfo.setBuildpack(buildpack);
	}

	/**
	 * Optional value. List of services to be created. If a user does not create
	 * services in the Application wizard, return null or an empty list.
	 * @return Optional list of created services, or null/empty list if no
	 * services are to be created
	 */
	public List<CFServiceInstance> getCloudServicesToCreate() {
		return createdCloudServices;
	}

	public void setCloudServicesToCreate(List<CFServiceInstance> createdCloudServices) {
		this.createdCloudServices = createdCloudServices;
	}

	/**
	 * Sets the start mode for the application.
	 */
	public void setApplicationStartMode(ApplicationAction applicationStartMode) {
		this.applicationStartMode = applicationStartMode;
	}

	/**
	 * Get the start mode for the application.
	 */
	public ApplicationAction getApplicationStartMode() {
		return applicationStartMode;
	}

	/**
	 * Its never null. An application wizard descriptor always wraps around an
	 * actual deployment info.
	 * @return non-null deployment info
	 */
	public ApplicationDeploymentInfo getDeploymentInfo() {
		return deploymentInfo;
	}

	/**
	 * 
	 * @param persist true if the deployment descriptor should be persisted in
	 * the app's manifest file. If the manifest file already exists, it will be
	 * overwritten. False otherwise.
	 */
	public void persistDeploymentInfo(boolean persist) {
		this.persistDeploymentInfo = persist;
	}

	/**
	 * 
	 * @return true if the deployment info should be persisted in an app's
	 * manifest. False otherwise.
	 */
	public boolean shouldPersistDeploymentInfo() {
		return persistDeploymentInfo;
	}

}
