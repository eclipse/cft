/*******************************************************************************
 * Copyright (c) 2012, 2016 Pivotal Software, Inc. 
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
 *     IBM - Bug 485697 - Implement host name taken check in CF wizards
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.wizards;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.eclipse.cft.server.core.internal.ApplicationAction;
import org.eclipse.cft.server.core.internal.CloudApplicationURL;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.DeploymentConfiguration;
import org.eclipse.cft.server.core.internal.client.DeploymentInfoWorkingCopy;
import org.eclipse.cft.server.ui.internal.CloudUiUtil;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

/**
 * Prompts a user for application deployment information. Any information set by
 * the user is set in the application module's deployment descriptor.
 * <p/>
 * To avoid setting deployment values in the application module if a user
 * cancels the operation, it is up to the caller to ensure that 1. the
 * application module has a deployment descriptor available to edit and 2. if
 * operation is cancelled, the values in the module are restored.
 */
public class CloudFoundryApplicationWizard extends Wizard implements IReservedURLTracker {

	protected final CloudFoundryApplicationModule module;

	protected final CloudFoundryServer server;

	protected IApplicationWizardDelegate wizardDelegate;

	protected final ApplicationWizardDescriptor applicationDescriptor;

	protected DeploymentInfoWorkingCopy workingCopy;

	// Keep track of reserved URLs in the wizard and not in individual wizard pages and parts
	private List<ReservedURL> reservedUrls;

	/**
	 * @param server must not be null
	 * @param module must not be null.
	 * @param workingCopy a working copy that should be edited by the wizard. If
	 * a user clicks "OK", the working copy will be saved into its corresponding
	 * app module. Must not be null.
	 * @param wizard delegate that provides wizard pages for the application
	 * module. If null, default Java web wizard delegate will be used.
	 */
	public CloudFoundryApplicationWizard(CloudFoundryServer server, CloudFoundryApplicationModule module,
			DeploymentInfoWorkingCopy workingCopy, IApplicationWizardDelegate wizardDelegate) {
		Assert.isNotNull(server);
		Assert.isNotNull(module);
		Assert.isNotNull(workingCopy);
		this.server = server;
		this.module = module;
		this.wizardDelegate = wizardDelegate;

		this.workingCopy = workingCopy;
		applicationDescriptor = new ApplicationWizardDescriptor(this.workingCopy);

		// By default applications are started after being pushed to the server
		applicationDescriptor.setApplicationStartMode(ApplicationAction.START);
		setNeedsProgressMonitor(true);
		setWindowTitle(Messages.CloudFoundryApplicationWizard_TITLE_APP);

		this.reservedUrls = new ArrayList<ReservedURL>();
	}

	@Override
	public void addPages() {

		// if a wizard provider exists, see if it contributes pages to the
		// wizard
		List<IWizardPage> applicationDeploymentPages = null;

		if (wizardDelegate == null) {
			// Use the default Java Web pages
			wizardDelegate = ApplicationWizardRegistry.getDefaultJavaWebWizardDelegate();
		}

		applicationDeploymentPages = wizardDelegate.getWizardPages(applicationDescriptor, server.getServer(), module);

		if (applicationDeploymentPages != null && !applicationDeploymentPages.isEmpty()) {
			for (IWizardPage updatedPage : applicationDeploymentPages) {
				addPage(updatedPage);
			}
		}
		else {

			String moduleID = module != null && module.getModuleType() != null ? module.getModuleType().getId()
					: "Unknown module type."; //$NON-NLS-1$

			CloudFoundryPlugin
					.logError("No application deployment wizard pages found for application type: " //$NON-NLS-1$
							+ moduleID
							+ ". Unable to complete application deployment. Check that the application type is registered in the Cloud Foundry application framework."); //$NON-NLS-1$
		}

	}

	/**
	 * @return newly created services. The services may not necessarily be bound
	 * to the application. To see the actual list of services to be bound,
	 * obtain the deployment descriptor: {@link #getDeploymentDescriptor()}
	 */
	public List<CloudService> getCloudServicesToCreate() {
		return applicationDescriptor.getCloudServicesToCreate();
	}

	public boolean persistManifestChanges() {
		return applicationDescriptor.shouldPersistDeploymentInfo();
	}

	public DeploymentConfiguration getDeploymentConfiguration() {
		if (applicationDescriptor.getApplicationStartMode() != null) {
			return new DeploymentConfiguration(applicationDescriptor.getApplicationStartMode());
		}
		return null;
	}

	/** Return a list of URLs that were created by the wizard, so they may be deleted. */
	private List<CloudApplicationURL> getCreatedUrls() {
		List<CloudApplicationURL> urlsToDelete = new ArrayList<CloudApplicationURL>();
		for(ReservedURL url : reservedUrls) {
			if(url.isRouteCreated() && url.getUrl() != null) {
				urlsToDelete.add(url.getUrl());
			}
		}
		
		return urlsToDelete;
	}
	
	@Override
	public boolean performFinish() {
		workingCopy.save();
		CloudUiUtil.cleanupReservedRoutesIfNotNeeded(workingCopy, this, server, getCreatedUrls());
		return true;
	}

	@Override
	public boolean performCancel() {
		CloudUiUtil.cleanupReservedRoutes(this,  server, getCreatedUrls(), null);
		return super.performCancel();
	}

	// Implement IReservedURLTracker

	public void addToReserved(CloudApplicationURL appUrl, boolean isUrlCreatedByWizard) {
		reservedUrls.add(new ReservedURL(appUrl, isUrlCreatedByWizard));
	}
	
	public void removeFromReserved(CloudApplicationURL appUrl) {
		if (reservedUrls.contains(appUrl)) {
			reservedUrls.remove(appUrl);
		}
	}
	
	public boolean isReserved(CloudApplicationURL appUrl) {
		return reservedUrls.contains(appUrl);
	}

	public HostnameValidationResult validateURL(CloudApplicationURL appUrl) {
		return CloudUiUtil.validateHostname(appUrl, server, getContainer());
	}
	
	
	/** Private inner class */
	private static class ReservedURL {
		private final CloudApplicationURL url;
		
		/** Whether or not the route was created by this wizard; if so, it should be deleted on cancel. */
		private final boolean isRouteCreated;
		
		public ReservedURL(CloudApplicationURL url, boolean isRouteCreated) {
			this.url = url;
			this.isRouteCreated = isRouteCreated;
		}

		public CloudApplicationURL getUrl() {
			return url;
		}
		
		public boolean isRouteCreated() {
			return isRouteCreated;
		}
	}
}
