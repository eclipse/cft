/*******************************************************************************
 * Copyright (c) 2012, 2017 Pivotal Software, Inc. and others
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
import java.util.Collection;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.cft.server.core.internal.ApplicationAction;
import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudUtil;
import org.eclipse.cft.server.core.internal.ModuleCache;
import org.eclipse.cft.server.core.internal.ModuleCache.ServerData;
import org.eclipse.cft.server.core.internal.StringUtils;
import org.eclipse.cft.server.core.internal.application.ApplicationRegistry;
import org.eclipse.cft.server.core.internal.application.ManifestParser;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.ui.internal.CloudApplicationUrlPart;
import org.eclipse.cft.server.ui.internal.CloudFoundryBrandingUIExtensionPoint;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.cft.server.ui.internal.ICoreRunnable;
import org.eclipse.cft.server.ui.internal.IEventSource;
import org.eclipse.cft.server.ui.internal.Logger;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.PartChangeEvent;
import org.eclipse.cft.server.ui.internal.UIPart;
import org.eclipse.cft.server.ui.internal.WizardPartChangeEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author Christian Dupuis
 * @author Leo Dos Santos
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Nieraj Singh
 */
public class CloudFoundryDeploymentWizardPage extends AbstractURLWizardPage implements Observer{

	protected final String serverTypeId;

	protected final CloudFoundryServer server;

	protected Composite runDebugOptions;

	protected Button regularStartOnDeploymentButton;

	protected CloudFoundryApplicationWizard wizard;

	protected final CloudFoundryApplicationModule module;

	protected final ApplicationWizardDescriptor descriptor;

	protected CloudApplicationUrlPart urlPart;

	private MemoryPart memoryPart;
	
	private Combo stackCombo;

	private static final String DEFAULT_MEMORY = CloudUtil.DEFAULT_MEMORY + ""; //$NON-NLS-1$

	private ApplicationWizardDelegate wizardDelegate;

	public CloudFoundryDeploymentWizardPage(CloudFoundryServer server, CloudFoundryApplicationModule module,
			ApplicationWizardDescriptor descriptor, ApplicationUrlLookupService urlLookup,
			ApplicationWizardDelegate wizardDelegate) {
		super(Messages.CloudFoundryDeploymentWizardPage_TEXT_DEPLOYMENT, null, null);
		this.server = server;
		this.module = module;
		this.descriptor = descriptor;
		this.serverTypeId = module.getServerTypeId();
		// Create the part before area is created as it be invoked by the page's
		// event handler before the page is visible.
		urlPart = createUrlPart(urlLookup);
		urlPart.setPage(this);
		urlPart.addPartChangeListener(this);
		this.wizardDelegate = wizardDelegate;
		
		descriptor.getDeploymentInfo().addObserver(this);
	}

	/**
	 * Perform some action like refreshing values in the UI. This is only called
	 * after the page is visible.
	 */
	protected void performWhenPageVisible() {

		refreshMemoryOptions();
		
		refreshStack();

		// Check that the current subdomain is in conflict with an existing app.
		ModuleCache moduleCache = CloudFoundryPlugin.getModuleCache();
		ServerData data = moduleCache.getData(server.getServerOriginal());
		Collection<CloudFoundryApplicationModule> applications = data.getExistingCloudModules();
		boolean duplicate = false;

		String currentSubDomain = urlPart.getCurrentSubDomain();
		for (CloudFoundryApplicationModule application : applications) {
			if (application != module && application.getDeployedApplicationName().equalsIgnoreCase(currentSubDomain)) {
				duplicate = true;
				break;
			}
		}

		if (duplicate) {
			IStatus status = CloudFoundryPlugin.getErrorStatus(Messages.CloudFoundryApplicationWizardPage_ERROR_SUBDOMAIN_CONFLICT);
			// We want to show that the host name from the manifest is already in conflict with an already existing application.
			// Piggy back on the part status change event handling design, as though the subdomain part got changed manually
			partStatus.put(CloudUIEvent.VALIDATE_SUBDOMAIN_EVENT, status);
			update(true, status);			
		} else {
			boolean hasManifest = new ManifestParser(module, server).hasManifest();
			// Only do if manifest exists.   Non-manifest cases, the name counter will increment and will have a valid/free hostname
			if (hasManifest) {
				urlPart.doInitialValidate();
			}
		}
	}

	protected void refreshMemoryOptions() {
		memoryPart.refreshMemoryOptions();
	}

	/**
	 * Refresh the stack list from the model. Select the default value that is set from the
	 * descriptor model.
	 */
	protected void refreshStack() {
		if (stackCombo == null || stackCombo.isDisposed()) {
			return;
		}
		
		// Get the list of stack and set the existing stack
		runAsynchWithWizardProgress(new ICoreRunnable() {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {

				try {
					List<String> stackStrLst = server.getBehaviour().getStacks(monitor);
					
					Display.getDefault().syncExec(new Runnable() {

						public void run() {
							String[] stackLst = new String[stackStrLst.size()];
							stackLst = stackStrLst.toArray(stackLst);

							String curStack = stackCombo.getText();
							int curSelectedStackIndex = stackCombo.getSelectionIndex();

							stackCombo.setItems(stackLst);
							
							// Add the default item to the beginning.
							stackCombo.add(Messages.CloudFoundryDeploymentWizardPage_STACK_DEFAULT, 0);
							stackLst = stackCombo.getItems();
							
							if (curSelectedStackIndex < 0) {
								// Initialize with the value from the model if valid.
								String stackValue = descriptor.getDeploymentInfo().getStack();
								if (stackValue != null) {
									curStack = stackValue;
								}
							}
					
							if (stackLst != null && stackLst.length > 0) {
								boolean matched = false;
								if (curStack != null) {
									for (int i=0; i<stackLst.length; i++) {
										if (curStack.equals(stackLst[i])) {
											stackCombo.select(i);
											matched = true;
											break;
										}
									}
								}
								if (!matched || stackCombo.getSelectionIndex() < 0) {
									// Default valid from model is not a valid selection so just choose the default one.
									stackCombo.select(0);
									descriptor.getDeploymentInfo().setStack(null);
								}
							}
						}

					});
				} catch (CoreException e) {
					if (Logger.WARNING) {
						Logger.println(Logger.WARNING_LEVEL, this, "refreshStack", "Failed to retreive the list of stacks.", e);
					}
				}
			}
		}, Messages.CloudFoundryDeploymentWizardPage_UPDATING_STACKS);
	}

	protected Point getRunDebugControlIndentation() {
		return new Point(15, 5);
	}

	protected void setMemory(String memoryVal) {

		int memory = -1;
		try {
			memory = Integer.parseInt(memoryVal);
		}
		catch (NumberFormatException e) {
			// ignore. error is handled below
		}
		IStatus status = Status.OK_STATUS;
		if (memory > 0) {
			descriptor.getDeploymentInfo().setMemory(memory);
		}
		else {
			// Set an invalid memory so next time page opens, it restores a
			// valid value
			descriptor.getDeploymentInfo().setMemory(-1);
			status = CloudFoundryPlugin.getErrorStatus(Messages.ERROR_INVALID_MEMORY);
		}
		handleChange(new PartChangeEvent(memoryVal, status, CloudUIEvent.MEMORY));
	}

	public void createControl(Composite parent) {
		setTitle(Messages.CloudFoundryDeploymentWizardPage_TITLE_LAUNCH_DEPLOY);
		setDescription(Messages.CloudFoundryDeploymentWizardPage_TEXT_DEPLOY_DETAIL);
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(serverTypeId);
		if (banner != null) {
			setImageDescriptor(banner);
		}

		this.wizard = (CloudFoundryApplicationWizard) getWizard();

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createAreas(composite);

		setControl(composite);
	}

	protected void createAreas(Composite parent) {

		Composite topComposite = new Composite(parent, SWT.NONE);
		GridLayout topLayout = new GridLayout(2, false);
		topComposite.setLayout(topLayout);
		topComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createURLArea(topComposite);

		createMemoryArea(topComposite);
		
		createStackArea(topComposite);

		createStartOrDebugOptions(parent);
	}

	protected void createURLArea(Composite parent) {
		urlPart.createPart(parent);
		urlPart.refreshDomains();
		updateApplicationURL();
	}

	protected CloudApplicationUrlPart createUrlPart(ApplicationUrlLookupService urlLookup) {
		return new CloudApplicationUrlPart(urlLookup);
	}

	protected void createMemoryArea(Composite parent) {
		memoryPart = new MemoryPart();
		memoryPart.addPartChangeListener(this);
		memoryPart.createPart(parent);
	}
	
	protected void createStackArea(Composite parent) {
		// Check extension to see if the stack area is supported by the branding extension on 
		// that server type.
		try {
			if (!CloudFoundryBrandingUIExtensionPoint.isShowStackUI(server.getServer().getServerType().getId())) {
				return;
			}
		} catch (Exception e) {
			if (Logger.WARNING) {
				Logger.println(Logger.WARNING_LEVEL, this.getClass(), "createStackArea", "Failed to check isShowStackUI from branding UI extension: " + server);
			}
		}
		Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.COMMONTXT_STACK);
		GridDataFactory.fillDefaults().grab(false, false).align(SWT.FILL, SWT.CENTER).applyTo(label);

		stackCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(stackCombo);
		stackCombo.setEnabled(true);
		stackCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String curStack = stackCombo.getText();
				if (stackCombo.getSelectionIndex() == 0) {
					// Default is selected so clear the stack settings.
					curStack = null;
				}
				descriptor.getDeploymentInfo().setStack(curStack);
			}
		});
	}

	protected void createStartOrDebugOptions(Composite parent) {

		String startLabelText = Messages.CloudFoundryDeploymentWizardPage_LABEL_START_APP;

		regularStartOnDeploymentButton = new Button(parent, SWT.CHECK);
		regularStartOnDeploymentButton.setText(startLabelText);
		ApplicationAction deploymentMode = descriptor.getApplicationStartMode();

		regularStartOnDeploymentButton.setSelection(deploymentMode == ApplicationAction.START);

		GridData buttonData = new GridData(SWT.FILL, SWT.FILL, false, false);

		if (!isServerDebugModeAllowed()) {
			buttonData.horizontalSpan = 2;
			buttonData.verticalIndent = 10;
		}

		regularStartOnDeploymentButton.setLayoutData(buttonData);

		regularStartOnDeploymentButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				boolean start = regularStartOnDeploymentButton.getSelection();
				ApplicationAction deploymentMode = null;

				// TODO: Uncomment when debug support is available once again
				// (post CF
				// 1.5.0)
				// if (isServerDebugModeAllowed()) {
				// // delegate to the run or debug controls to decide which
				// // mode to select
				// makeStartDeploymentControlsVisible(start);
				// if (!start) {
				// deploymentMode = null;
				// }
				// }
				// else {
				// deploymentMode = start ? ApplicationAction.START : null;
				// }

				deploymentMode = start ? ApplicationAction.START : ApplicationAction.STOP;

				descriptor.setApplicationStartMode(deploymentMode);
			}
		});
		// TODO: Uncomment when debug support is available once again (post CF
		// 1.5.0)
		// if (isServerDebugModeAllowed()) {
		// runDebugOptions = new Composite(parent, SWT.NONE);
		//
		// GridLayoutFactory.fillDefaults().margins(getRunDebugControlIndentation()).numColumns(1)
		// .applyTo(runDebugOptions);
		// GridDataFactory.fillDefaults().grab(false,
		// false).applyTo(runDebugOptions);
		//
		// final Button runRadioButton = new Button(runDebugOptions, SWT.RADIO);
		// runRadioButton.setText("Run");
		// runRadioButton.setToolTipText("Run application after deployment");
		// runRadioButton.setSelection(deploymentMode ==
		// ApplicationAction.START);
		//
		// runRadioButton.addSelectionListener(new SelectionAdapter() {
		//
		// public void widgetSelected(SelectionEvent e) {
		// setDeploymentMode(ApplicationAction.START);
		// }
		// });
		//
		// final Button debugRadioButton = new Button(runDebugOptions,
		// SWT.RADIO);
		// debugRadioButton.setText("Debug");
		// debugRadioButton.setToolTipText("Debug application after deployment");
		// debugRadioButton.setSelection(deploymentMode ==
		// ApplicationAction.DEBUG);
		//
		// debugRadioButton.addSelectionListener(new SelectionAdapter() {
		//
		// public void widgetSelected(SelectionEvent e) {
		// setDeploymentMode(ApplicationAction.DEBUG);
		// }
		// });
		//
		// // Hide run or debug selection controls if there is no server
		// // support
		// makeStartDeploymentControlsVisible(true);
		// }

	}

	protected boolean isServerDebugModeAllowed() {
		return false;
	}

	protected void makeStartDeploymentControlsVisible(boolean makeVisible) {
		if (runDebugOptions != null && !runDebugOptions.isDisposed()) {
			GridData data = (GridData) runDebugOptions.getLayoutData();

			// If hiding, exclude from layout as to not take up space when it is
			// made invisible
			GridDataFactory.createFrom(data).exclude(!makeVisible).applyTo(runDebugOptions);

			runDebugOptions.setVisible(makeVisible);

			// Recalculate layout if run debug options are excluded
			runDebugOptions.getParent().layout(true, true);

		}
	}

	/**
	 * Sets the application URL in the deployment descriptor
	 */
	protected void setUrlInDescriptor(String url) {
		// Bug fix: don't set empty url in list of URLs. Ensure it has a non-empty value before setting in list
		// Otherwise the cf Java client may throw exception if it tries to parse a non-empty list that contains an empty URL
		if (!StringUtils.isEmpty(url)) {
			List<String> urls = new ArrayList<String>();
			urls.add(url);
			descriptor.getDeploymentInfo().setUris(urls);
		}
		else {
			descriptor.getDeploymentInfo().setUris(null);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.cft.server.ui.internal.wizards.PartsWizardPage
	 * #handleChange
	 * (org.eclipse.cft.server.ui.internal.PartChangeEvent)
	 */
	public void handleChange(PartChangeEvent event) {
		Object eventData = event.getData();
		IEventSource<?> source = event.getSource();

		// If the event originated from the URL UI, just update the URL in
		// the
		// descriptor. No other UI needs to be updated.
		if (event.getSource() == CloudUIEvent.APPLICATION_URL_CHANGED) {
			String urlVal = eventData instanceof String ? (String) eventData : null;
			setUrlInDescriptor(urlVal);

			IStatus status = event.getStatus();
			// Don't show the error if the application does not require a URL
			// and the URL is empty
			if (StringUtils.isEmpty(urlVal) && !requiresUrl()) {
				status = Status.OK_STATUS;
			}
			event = new WizardPartChangeEvent(eventData, status, event.getSource(), true);

		}
		else if (source == CloudUIEvent.APP_NAME_CHANGE_EVENT) {
			String value = (String) event.getData();
			updateApplicationNameInDescriptor(value);
			// Set the application URL based on the app name.
			updateApplicationURLFromAppName();
		}

		super.handleChange(event);
	}

	protected void updateApplicationNameInDescriptor(String appName) {

		// Do not set empty Strings
		if (StringUtils.isEmpty(appName)) {
			appName = null;
		}

		descriptor.getDeploymentInfo().setDeploymentName(appName);
	}

	protected void updateApplicationURL() {

		List<String> urls = descriptor.getDeploymentInfo().getUris();
		String url = urls != null && !urls.isEmpty() ? urls.get(0) : null;

		// Existing URLs have higher priority than URLs generated from the
		// application name
		if (url != null) {
			urlPart.setUrl(url);
		}
		else {
			updateApplicationURLFromAppName();
		}
	}

	protected void updateApplicationURLFromAppName() {
		if (shouldSetDefaultUrl()) {
			// When the app name changes, the URL also changes, but only for
			// application types that require a URL.
			String appName = descriptor.getDeploymentInfo().getDeploymentName();

			// When transferring the appname to the url, remove any dots.
			appName = appName.replace(".", "");
			
			urlPart.setSubdomain(appName);
		}
	}

	protected boolean requiresUrl() {
		// By default, applications require a URL, unless specified by the
		// delegate
		return wizardDelegate == null || wizardDelegate.getApplicationDelegate() == null
				|| wizardDelegate.getApplicationDelegate().requiresURL();

	}

	protected boolean shouldSetDefaultUrl() {
		return wizardDelegate == null
				|| ApplicationRegistry.shouldSetDefaultUrl(wizardDelegate.getApplicationDelegate(), module);
	}

	class MemoryPart extends UIPart {

		protected Text memory;

		@Override
		public Control createPart(Composite parent) {
			Label label = new Label(parent, SWT.NONE);
			label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			label.setText(Messages.LABEL_MEMORY_LIMIT);

			memory = new Text(parent, SWT.BORDER);
			memory.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
			gd.horizontalSpan = 2;
			memory.setLayoutData(gd);
			memory.addModifyListener(new ModifyListener() {

				public void modifyText(ModifyEvent e) {
					setMemory(memory.getText());
				}
			});
			return parent;
		}

		public void refreshMemoryOptions() {
			if (memory != null && !memory.isDisposed()) {
				int currentMemory = descriptor.getDeploymentInfo().getMemory();
				if (currentMemory <= 0) {
					memory.setText(DEFAULT_MEMORY);
				}
				else {
					memory.setText(currentMemory + ""); //$NON-NLS-1$
				}
			}
		}
	}

	@Override
	protected void domainsRefreshed() {
		urlPart.refreshDomains();
		updateApplicationURL();
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg != null && arg instanceof String ) {
			try {
				updateApplicationURLFromAppName();
			} catch (Exception e) {
				if (Logger.ERROR) {
					Logger.println(Logger.ERROR_LEVEL, this, "update", "Error updating Application name", e); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}		
	} 
	
}
