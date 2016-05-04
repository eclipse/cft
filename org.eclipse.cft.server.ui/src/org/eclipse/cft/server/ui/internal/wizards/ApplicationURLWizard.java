/*******************************************************************************
 * Copyright (c) 2013, 2016 Pivotal Software, Inc. 
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

import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudApplicationURL;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.ui.internal.CloudApplicationUrlPart;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.cft.server.ui.internal.CFUiUtil;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.PartChangeEvent;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * Allows the editing or addition of an application URL based on an existing
 * list of Cloud domains.
 * 
 */
public class ApplicationURLWizard extends Wizard implements IReservedURLTracker {

	private final CloudFoundryServer cloudServer;

	private final String initialUrl;

	private String editedUrl;

	private ApplicationURLWizardPage urlPage;

	private static final String title = Messages.ApplicationURLWizard_TITLE_MOD_APP_URL;

	// Keep track of reserved URLs in the wizard and not in individual wizard pages
	private List<CloudApplicationURL> reservedUrls;

	public ApplicationURLWizard(CloudFoundryServer cloudServer, String initialUrl) {
		this.cloudServer = cloudServer;
		this.initialUrl = initialUrl;
		setWindowTitle(title);
		setNeedsProgressMonitor(true);
		this.reservedUrls = new ArrayList<CloudApplicationURL>();
	}

	@Override
	public boolean performFinish() {
		CFUiUtil.cleanupReservedRoutes(this, cloudServer, reservedUrls, editedUrl);
		return true;
	}

	@Override
	public boolean performCancel() {
		CFUiUtil.cleanupReservedRoutes(this, cloudServer, reservedUrls, null);
		return super.performCancel();
	}

	@Override
	public void addPages() {
		String serverTypeId = cloudServer.getServer().getServerType().getId();

		ImageDescriptor imageDescriptor = CloudFoundryImages.getWizardBanner(serverTypeId);
		// Use the cached version if possible.
		ApplicationUrlLookupService urlLookup = ApplicationUrlLookupService.getCurrentLookup(cloudServer);
		urlPage = createPage(imageDescriptor, urlLookup);
		urlPage.setWizard(this);
		addPage(urlPage);
	}

	public String getUrl() {
		return editedUrl;
	}

	protected ApplicationURLWizardPage createPage(ImageDescriptor imageDescriptor, ApplicationUrlLookupService urlLookup) {
		CloudApplicationUrlPart urlPart = new CloudApplicationUrlPart(urlLookup);
		ApplicationURLWizardPage applicationURLWizardPage = new ApplicationURLWizardPage(imageDescriptor, urlLookup, urlPart);
		urlPart.setPage(applicationURLWizardPage);
		return applicationURLWizardPage;
	}

	class ApplicationURLWizardPage extends AbstractURLWizardPage {

		private final CloudApplicationUrlPart urlPart;

		protected ApplicationURLWizardPage(ImageDescriptor titleImage, ApplicationUrlLookupService urlLookup,
				CloudApplicationUrlPart urlPart) {
			super(Messages.ApplicationURLWizard_TEXT_PAGE, title, titleImage, urlLookup);
			setDescription(Messages.ApplicationURLWizard_TEXT_MOD_APP_URL);
			this.urlPart = urlPart;
		}

		protected void performWhenPageVisible() {

			// Refresh the application URL (since the URL host tends to the the
			// application name, if the application name has changed
			// make sure it gets updated in the UI. Also fetch the list of
			// domains
			// ONCE per session.
			// Run all URL refresh and fetch in the same UI Job to ensure that
			// domain updates occur first before the UI is refreshed.
			if (!refreshedDomains) {
				refreshApplicationUrlDomains();
			}
		}

		public void createControl(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout());
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(composite);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(composite);

			urlPart.createPart(composite);
			urlPart.addPartChangeListener(this);

			setControl(composite);
		}

		@Override
		protected void domainsRefreshed() {
			urlPart.refreshDomains();
			urlPart.setUrl(initialUrl);
		}

		public void handleChange(PartChangeEvent event) {
			if (event.getSource() == CloudUIEvent.APPLICATION_URL_CHANGED) {
				editedUrl = event.getData() instanceof String ? (String) event.getData() : null;
			}

			super.handleChange(event);
		}
	}
	
	// Implement IReservedURLTracker

	@Override
	public void addToReserved(CloudApplicationURL appUrl, boolean isUrlCreatedByWizard) {
		reservedUrls.add(appUrl);
	}
	
	@Override
	public void removeFromReserved(CloudApplicationURL appUrl) {
		if (reservedUrls.contains(appUrl)) {
			reservedUrls.remove(appUrl);
		}
	}
	
	@Override
	public boolean isReserved(CloudApplicationURL appUrl) {
		return reservedUrls.contains(appUrl);
	}

	@Override
	public HostnameValidationResult validateURL(CloudApplicationURL appUrl) {
		return CFUiUtil.validateHostname(appUrl, cloudServer, getContainer());
	}

}
