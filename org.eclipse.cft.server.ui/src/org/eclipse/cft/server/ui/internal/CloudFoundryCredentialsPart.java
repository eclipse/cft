/*******************************************************************************
 * Copyright (c) 2012, 2016 Pivotal Software, Inc. and others
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
 *     Keith Chong, IBM - Modify Sign-up so it's more brand-friendly
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal;

import org.eclipse.cft.server.core.internal.CloudFoundryBrandingExtensionPoint;
import org.eclipse.cft.server.core.internal.CloudFoundryConstants;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.ValidationEvents;
import org.eclipse.cft.server.core.internal.client.CloudFoundryClientFactory;
import org.eclipse.cft.server.ui.internal.editor.CloudUrlWidget;
import org.eclipse.cft.server.ui.internal.wizards.RegisterAccountWizard;
import org.eclipse.cft.server.ui.internal.wizards.WizardHandleContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.browser.WebBrowserPreference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.wst.server.ui.wizard.IWizardHandle;

/**
 * @author Andy Clement
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Steffen Pingel
 * @author Terry Denney
 * @author Nieraj Singh
 */
@SuppressWarnings("restriction")
public class CloudFoundryCredentialsPart extends UIPart implements IPartChangeListener {

	private CloudFoundryServer cfServer;

	private Text emailText;

	private TabFolder folder;

	private Text passwordText;

	private String serverTypeId;

	private String service;

	private CloudUrlWidget urlWidget;

	private Button validateButton;

	private Button registerAccountButton;

	private Button cfSignupButton;

	private IRunnableContext runnableContext;

	private Button sso;

	private PageBook pageBook;

	private Link prompt;

	private Label passcodeLabel;

	private Text passcodeText;

	public CloudFoundryCredentialsPart(CloudFoundryServer cfServer, WizardPage wizardPage) {
		this(cfServer);

		if (wizardPage != null) {
			wizardPage.setTitle(NLS.bind(Messages.CloudFoundryCredentialsPart_TEXT_CREDENTIAL_WIZ_TITLE, service));
			wizardPage.setDescription(Messages.SERVER_WIZARD_VALIDATOR_CLICK_TO_VALIDATE);
			ImageDescriptor banner = CloudFoundryImages.getWizardBanner(serverTypeId);
			if (banner != null) {
				wizardPage.setImageDescriptor(banner);
			}
			runnableContext = wizardPage.getWizard() != null && wizardPage.getWizard().getContainer() != null ? wizardPage
					.getWizard().getContainer() : null;
		}
	}

	public CloudFoundryCredentialsPart(CloudFoundryServer cfServer, final WizardHandleContext context) {
		this(cfServer);
		IWizardHandle wizardHandle = context.getWizardHandle();
		if (wizardHandle != null) {
			wizardHandle.setTitle(NLS.bind(Messages.CloudFoundryCredentialsPart_TEXT_CREDENTIAL_WIZ_TITLE, service));
			wizardHandle.setDescription(Messages.SERVER_WIZARD_VALIDATOR_CLICK_TO_VALIDATE);
			ImageDescriptor banner = CloudFoundryImages.getWizardBanner(serverTypeId);
			if (banner != null) {
				wizardHandle.setImageDescriptor(banner);
			}

			runnableContext = context.getRunnableContext();
		}
	}

	public CloudFoundryCredentialsPart(CloudFoundryServer cfServer) {

		this.cfServer = cfServer;
		this.serverTypeId = cfServer.getServer().getServerType().getId();
		this.service = CloudFoundryBrandingExtensionPoint.getServiceName(serverTypeId);

		runnableContext = PlatformUI.getWorkbench().getProgressService();
	}

	public Control createPart(Composite parent) {

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		folder = new TabFolder(composite, SWT.NONE);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		folder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateUI(false);
			}
		});

		try {
			createExistingUserComposite(folder);
			updateUI(false);
		}
		catch (Throwable e1) {
			CloudFoundryPlugin.logError(e1);
		}

		return composite;

	}

	public void setServer(CloudFoundryServer server) {
		this.cfServer = server;
	}

	private void createExistingUserComposite(TabFolder folder) throws CoreException {
		Composite composite = new Composite(folder, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		sso = new Button(composite, SWT.CHECK);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		sso.setLayoutData(gd);
		sso.setText(Messages.SSO_SERVER);
		sso.setSelection(cfServer.isSso());
		
		pageBook = new PageBook(composite, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		pageBook.setLayoutData(gd);
		
		final Control emailPasswordControl = createEmailPasswordControl(pageBook);
		final Control passcodeControl = createPasscodeControl(pageBook);

		showPage(emailPasswordControl, passcodeControl);
		sso.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				cfServer.setSso(sso.getSelection());
				if(sso.getSelection()) {
					updatePromptTextWithSsoUrl();
				}
				showPage(emailPasswordControl, passcodeControl);
				updateUI(false);
			}
			
		});
		urlWidget = new CloudUrlWidget(cfServer) {

			@Override
			protected void setUpdatedSelectionInServer() {
				super.setUpdatedSelectionInServer();
				if(sso.getSelection()) {
					updatePromptTextWithSsoUrl();
				}
				updateUI(false);
			}

		};

		urlWidget.createControls(composite, runnableContext);

		String url = urlWidget.getURLSelection();
		if (url != null) {
			cfServer.setUrl(CFUiUtil.getUrlFromDisplayText(url));
		}

		final Composite validateComposite = new Composite(composite, SWT.NONE);
		validateComposite.setLayout(new GridLayout(3, false));
		validateComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		validateButton = new Button(validateComposite, SWT.PUSH);
		validateButton.setText(Messages.CloudFoundryCredentialsPart_TEXT_VALIDATE_BUTTON);
		validateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {

				updateUI(true);

			}
		});

		registerAccountButton = new Button(validateComposite, SWT.PUSH);
		registerAccountButton.setText(Messages.CloudFoundryCredentialsPart_TEXT_REGISTER_BUTTON);
		registerAccountButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				RegisterAccountWizard wizard = new RegisterAccountWizard(cfServer);
				WizardDialog dialog = new WizardDialog(validateComposite.getShell(), wizard);
				if (dialog.open() == Window.OK) {
					if (wizard.getEmail() != null) {
						emailText.setText(wizard.getEmail());
					}
					if (wizard.getPassword() != null) {
						passwordText.setText(wizard.getPassword());
					}
				}
			}
		});

		cfSignupButton = new Button(validateComposite, SWT.PUSH);
		cfSignupButton.setText(CloudFoundryConstants.PUBLIC_CF_SERVER_SIGNUP_LABEL);
		cfSignupButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {
				String signupURL = CloudFoundryBrandingExtensionPoint.getSignupURL(serverTypeId, cfServer.getUrl());
				if (signupURL != null) {
					CloudFoundryURLNavigation nav = new CloudFoundryURLNavigation(signupURL);
					nav.navigateExternal();
				}
			}
		});

		TabItem item = new TabItem(folder, SWT.NONE);
		item.setText(Messages.COMMONTXT_ACCOUNT_INFO);
		item.setControl(composite);
	}

	private void showPage(Control emailPasswordControl, Control passcodeControl) {
		if (sso.getSelection()) {
			pageBook.showPage(passcodeControl);
		} else {
			pageBook.showPage(emailPasswordControl);
		}
	}

	private Control createPasscodeControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		GridData gd;
		prompt = new Link(composite, SWT.LEFT | SWT.WRAP);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalSpan = 2;
		prompt.setLayoutData(gd);
		prompt.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				if (cfServer.getUrl() != null && !cfServer.getUrl().isEmpty()) {
					String url;
					try {
						url = CloudFoundryClientFactory.getSsoUrl(cfServer.getUrl(), cfServer.isSelfSigned());
						if (url != null && !url.isEmpty()) {
							CFUiUtil.openUrl(url, WebBrowserPreference.EXTERNAL);
						}
					}
					catch (Exception e) {
						CloudFoundryServerUiPlugin.logError(e);
					}
				}
			}
		});
		passcodeLabel = new Label(composite, SWT.NONE);
		passcodeLabel.setText(Messages.LABEL_PASSCODE);
		passcodeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		passcodeText = new Text(composite, SWT.BORDER|SWT.PASSWORD);
		passcodeText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		passcodeText.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				cfServer.setPasscode(passcodeText.getText());
				updateUI(false);
			}
		});
		return composite;
	}

	/** Acquire the SSO url on a separate thread, then update the prompt label accordingly*/
	private void updatePromptTextWithSsoUrl() {
		
		CFUiUtil.runOnUIThreadUntilTimeout(Messages.CFCredentialsPart_ACQUIRING_SSO_URL, new Runnable() {

			@Override
			public void run() {
				final String ssoPromptText = CFUiUtil.getPromptText(cfServer);
				
				Display.getDefault().syncExec(new Runnable() {

					@Override
					public void run() {
						prompt.setText(ssoPromptText);
					}
					
				});				

			}
			
		} );
	}
		
	private Control createEmailPasswordControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label emailLabel = new Label(composite, SWT.NONE);
		emailLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		emailLabel.setText(Messages.COMMONTXT_EMAIL_WITH_COLON);

		emailText = new Text(composite, SWT.BORDER);
		emailText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		emailText.setEditable(true);
		emailText.setFocus();
		if (cfServer.getUsername() != null) {
			emailText.setText(cfServer.getUsername());
		}

		emailText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				cfServer.setUsername(emailText.getText());
				updateUI(false);
			}
		});

		Label passwordLabel = new Label(composite, SWT.NONE);
		passwordLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		passwordLabel.setText(Messages.COMMONTXT_PW);

		passwordText = new Text(composite, SWT.PASSWORD | SWT.BORDER);
		passwordText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		passwordText.setEditable(true);
		if (cfServer.getPassword() != null) {
			passwordText.setText(cfServer.getPassword());
		}

		passwordText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				cfServer.setPassword(passwordText.getText());

				updateUI(false);
			}
		});
		return composite;
	}

	/**
	 * 
	 * @param validateCredentials true if credentials should be validated, which
	 * would require a network I/O request sent to the server. False if only
	 * local validation should be performed (e.g. check for malformed URL)
	 */
	public void updateUI(boolean validateAgainstServer) {

		// If validating against a server, it means a user explicitly requested
		// a credentials validation against server.

		int eventType = validateAgainstServer ? ValidationEvents.SERVER_AUTHORISATION
				: ValidationEvents.CREDENTIALS_FILLED;
		notifyChange(new PartChangeEvent(runnableContext, Status.OK_STATUS, this, eventType));
		updateButtons();

	}

	protected void updateButtons() {
		String url = cfServer.getUrl();
		cfSignupButton.setEnabled(!sso.getSelection() && CloudFoundryURLNavigation.canEnableCloudFoundryNavigation(serverTypeId, url));

		registerAccountButton.setEnabled(!sso.getSelection() && CloudFoundryBrandingExtensionPoint.supportsRegistration(serverTypeId, url));

	}

	public void handleChange(PartChangeEvent event) {
		if (event == null) {
			return;
		}
		int type = event.getType();
		boolean valuesFilled = (type == ValidationEvents.VALIDATION || type == ValidationEvents.SELF_SIGNED)
				&& (event.getStatus() != null && event.getStatus().isOK());

		// If the credentials have changed and do not match those used to
		// previously
		// set a space descriptor, clear the space descriptor

		validateButton.setEnabled(valuesFilled && !sso.getSelection());

	}
}
