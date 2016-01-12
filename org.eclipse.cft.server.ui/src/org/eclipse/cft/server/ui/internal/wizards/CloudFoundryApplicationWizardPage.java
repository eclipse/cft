/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudApplicationURL;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.ModuleCache;
import org.eclipse.cft.server.core.internal.ModuleCache.ServerData;
import org.eclipse.cft.server.core.internal.ValueValidationUtil;
import org.eclipse.cft.server.core.internal.application.ManifestParser;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.cft.server.ui.internal.CloudUiUtil;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.PartChangeEvent;
import org.eclipse.cft.server.ui.internal.UIPart;
import org.eclipse.cft.server.ui.internal.WizardPartChangeEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * 
 * First page in the application deployment wizard that prompts the user for an
 * application name.
 * 
 */
public class CloudFoundryApplicationWizardPage extends PartsWizardPage {

	private Pattern VALID_CHARS = Pattern.compile("[A-Za-z\\$_0-9\\-]+"); //$NON-NLS-1$

	protected static final String DEFAULT_DESCRIPTION = Messages.CloudFoundryApplicationWizardPage_TEXT_SET_APP_DETAIL;

	private String appName;

	private String buildpack;

	private Text buildpackText;

	private String serverTypeId;

	protected final CloudFoundryServer server;

	protected final CloudFoundryApplicationModule module;

	protected String filePath;

	protected final ApplicationWizardDescriptor descriptor;

	// Preserving the old constructor to avoid API breakage
	public CloudFoundryApplicationWizardPage(CloudFoundryServer server, CloudFoundryDeploymentWizardPage deploymentPage,
			CloudFoundryApplicationModule module, ApplicationWizardDescriptor descriptor) {
		super(Messages.CloudFoundryApplicationWizardPage_TEXT_DEPLOY_WIZ, null, null);
		this.server = server;
		// Simply ignore the deploymentPage, this is preserved to avoid API
		// breakage
		this.module = module;
		this.descriptor = descriptor;
		this.serverTypeId = module.getServerTypeId();
	}

	public CloudFoundryApplicationWizardPage(CloudFoundryServer server, CloudFoundryApplicationModule module,
			ApplicationWizardDescriptor descriptor) {
		super(Messages.CloudFoundryApplicationWizardPage_TEXT_DEPLOY_WIZ, null, null);
		this.server = server;
		this.module = module;
		this.descriptor = descriptor;
		this.serverTypeId = module.getServerTypeId();
	}

	protected void init() {
		appName = descriptor.getDeploymentInfo().getDeploymentName();
		buildpack = descriptor.getDeploymentInfo().getStaging() != null
				? descriptor.getDeploymentInfo().getStaging().getBuildpackUrl() : null;
	}

	protected CloudFoundryApplicationWizard getApplicationWizard() {
		return (CloudFoundryApplicationWizard) getWizard();
	}

	public void createControl(Composite parent) {
		setTitle(Messages.CloudFoundryApplicationWizardPage_TITLE_APP_DETAIL);
		setDescription(DEFAULT_DESCRIPTION);
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(serverTypeId);
		if (banner != null) {
			setImageDescriptor(banner);
		}

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createContents(composite);

		setControl(composite);

	}

	protected Composite createContents(Composite parent) {
		Composite composite = preCreateContents(parent);
		createManifestSection(composite);
		return composite;
	}

	protected Composite preCreateContents(Composite parent) {
		// This must be called first as the values are then populate into the UI
		// widgets
		init();
		IStatus hostnameStatus = validateHostname();

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		final UIPart appPart = new AppNamePart();
		// Add the listener first so that it can be notified of changes during
		// the part creation.
		appPart.addPartChangeListener(this);

		appPart.createPart(composite);

		Label nameLabel = new Label(composite, SWT.NONE);
		nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		nameLabel.setText(Messages.CloudFoundryApplicationWizardPage_LABEL_BUILDPACK);

		buildpackText = new Text(composite, SWT.BORDER);

		if (buildpack != null) {
			buildpackText.setText(buildpack);
		}

		buildpackText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		buildpackText.setEditable(true);

		buildpackText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				buildpack = buildpackText.getText().trim();

				validateBuildPack();
			}
		});

		// Show hostname taken error message first over buildpack URL errors
		if (hostnameStatus != null && !hostnameStatus.isOK()) {
			setErrorMessage(hostnameStatus.getMessage());
			handleChange(new PartChangeEvent(appName, hostnameStatus, CloudUIEvent.VALIDATE_HOST_TAKEN_EVENT));
		}

		return composite;
	}

	protected void createManifestSection(Composite composite) {

		Label emptyLabel = new Label(composite, SWT.NONE);
		emptyLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

		final Button saveToManifest = new Button(composite, SWT.CHECK);
		saveToManifest.setText(Messages.CloudFoundryApplicationWizardPage_BUTTON_SAVE_MANIFEST);
		saveToManifest.setToolTipText(Messages.CloudFoundryApplicationWizardPage_TEXT_SAVE_MANIFEST_TOOLTIP);
		saveToManifest.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false, 2, 1));

		saveToManifest.setSelection(false);

		saveToManifest.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				descriptor.persistDeploymentInfo(saveToManifest.getSelection());
			}

		});
	}

	protected void validateBuildPack() {

		IStatus status = Status.OK_STATUS;
		// Clear the previous buildpack anyway, as either a new one valid one
		// will be set
		// or if invalid, it should be cleared from the descriptor. The
		// descriptor should only contain
		// either null or a valid URL.
		descriptor.setBuildpack(null);

		// buildpack URL is optional, so an empty URL is acceptable
		if (!ValueValidationUtil.isEmpty(buildpack)) {
			try {
				// Allow for names and URLs
				URI uriObject = URI.create(buildpack);
				if (uriObject.isAbsolute()) {
					String host = uriObject.getHost();
					if (host == null || host.length() == 0) {
						status = CloudFoundryPlugin.getErrorStatus(Messages.COMMONTXT_ENTER_VALID_URL);
					}
				}
				if (status.isOK()) {
					// Only set valid buildpack URLs
					descriptor.setBuildpack(buildpack);
				}
			}
			catch (IllegalArgumentException e) {
				status = CloudFoundryPlugin.getErrorStatus(Messages.COMMONTXT_ENTER_VALID_URL);
			}
		}

		handleChange(new PartChangeEvent(buildpack, status, CloudUIEvent.BUILD_PACK_URL));

	}

	protected IStatus getUpdateNameStatus() {
		IStatus status = Status.OK_STATUS;
		if (ValueValidationUtil.isEmpty(appName)) {
			status = CloudFoundryPlugin.getStatus(Messages.CloudFoundryApplicationWizardPage_ERROR_MISSING_APPNAME,
					IStatus.ERROR);
		}
		else {
			Matcher matcher = VALID_CHARS.matcher(appName);
			if (!matcher.matches()) {
				status = CloudFoundryPlugin
						.getErrorStatus(Messages.CloudFoundryApplicationWizardPage_ERROR_INVALID_CHAR);
			}
			else {
				ModuleCache moduleCache = CloudFoundryPlugin.getModuleCache();
				ServerData data = moduleCache.getData(server.getServerOriginal());
				Collection<CloudFoundryApplicationModule> applications = data.getExistingCloudModules();
				boolean duplicate = false;

				for (CloudFoundryApplicationModule application : applications) {
					if (application != module && application.getDeployedApplicationName().equals(appName)) {
						duplicate = true;
						break;
					}
				}

				if (duplicate) {
					status = CloudFoundryPlugin
							.getErrorStatus(Messages.CloudFoundryApplicationWizardPage_ERROR_NAME_CONFLICT);
				}
			}
		}

		return status;

	}

	protected class AppNamePart extends UIPart {

		private Text nameText;

		@Override
		public Control createPart(Composite parent) {

			Label nameLabel = new Label(parent, SWT.NONE);
			nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			nameLabel.setText(Messages.COMMONTXT_NAME_WITH_COLON);

			nameText = new Text(parent, SWT.BORDER);
			nameText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			nameText.setEditable(true);

			nameText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					appName = nameText.getText();

					// Utilize the observer to do the notification
					descriptor.getDeploymentInfo().setDeploymentName(appName);
					// If first time initialising, dont update the wizard
					// buttons as the may not be available to update yet
					boolean updateButtons = true;
					// Any subsequent app name changes will clear the error
					notifyChange(new PartChangeEvent(appName, Status.OK_STATUS, CloudUIEvent.VALIDATE_HOST_TAKEN_EVENT));
					notifyChange(new WizardPartChangeEvent(appName, getUpdateNameStatus(),
							CloudUIEvent.APP_NAME_CHANGE_EVENT, updateButtons));
				}
			});

			// Set the app name after adding the listener to fire event
			if (appName != null) {
				nameText.setText(appName);
			}

			return parent;
		}
	}
	
	private IStatus validateHostname() {
		ApplicationDeploymentInfo workingCopy = descriptor.getDeploymentInfo();
		IStatus status = Status.OK_STATUS;
		boolean hasManifest = new ManifestParser(module, server).hasManifest();
		if (workingCopy != null) {
			CloudApplicationURL cloudUrl = null;
			// if there is a valid manifest, we need to check if the saved host and domain is already used
			if (hasManifest) {
				List<String> urls = workingCopy.getUris();
			    String url = urls != null && !urls.isEmpty() ? urls.get(0) : null;
			    if (url != null) {
			    	try {
				    	ApplicationUrlLookupService urllookup = ApplicationUrlLookupService.getCurrentLookup(server);
				    	CloudApplicationURL appUrl = urllookup.getCloudApplicationURL(url);
					
				    	// Message saying that the host name from the manifest is already taken. 
				    	String customMessage = Messages.bind(Messages.CloudFoundryApplicationWizardPage_ERROR_INITIAL_HOSTNAME_TAKEN, appUrl.getSubdomain(), appUrl.getDomain());
				    	// 	Validate it and get the status
				    	status = CloudUiUtil.validateHostname(appUrl, server, getContainer(), customMessage);
					
				    	if (status != null && status.isOK()) {
				    		cloudUrl = appUrl;
				    	}
			    	} catch (Throwable ce) {
	
						CloudFoundryPlugin.logError(ce);
					}
			    }
			} else {
				// IFF there is no manifest, then we will do the hostname validation AND suggest a new unique name
				List<String> urls = workingCopy.getUris();
				String url = urls != null && !urls.isEmpty() ? urls.get(0) : null;
				if (url != null) {
					cloudUrl = CloudUiUtil.getUniqueSubdomain(url, server);
					if (cloudUrl != null) {
						// Set the new deployment name and update the URIs only if it needs to be changed
						if (!cloudUrl.getSubdomain().equals(workingCopy.getDeploymentName())) {
						   workingCopy.setDeploymentName(cloudUrl.getSubdomain());
						   appName = cloudUrl.getSubdomain();
						   List<String>newList = new ArrayList<String>();
						   newList.add(cloudUrl.getUrl());
						   workingCopy.setUris(newList);
						}
					}
				}
				getApplicationWizard().addToReserved(cloudUrl);
			}
		}
		return status;
	}
}
