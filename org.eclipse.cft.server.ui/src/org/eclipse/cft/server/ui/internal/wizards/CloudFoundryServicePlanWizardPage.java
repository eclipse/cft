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
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CFServiceInstance;
import org.eclipse.cft.server.core.internal.client.CFServiceOffering;
import org.eclipse.cft.server.core.internal.client.CFServicePlan;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.Observables;
import org.eclipse.core.databinding.observable.map.WritableMap;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.MultiValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.statushandlers.StatusManager;

public class CloudFoundryServicePlanWizardPage extends WizardPage {

	protected DataBindingContext bindingContext;

	private final CloudFoundryServer cloudServer;

	private List<CFServiceOffering> serviceOfferings;

	protected WritableMap map;

	private Text nameText;

	/**
	 * The data model.
	 */
	protected CFServiceInstance service;

	private Composite planDetailsComposite;

	protected Group planGroup;

	private PageBook pageBook;

	private WritableValue planObservable = new WritableValue();

	private Combo typeCombo;

	private Pattern VALID_CHARS = Pattern.compile("[A-Za-z\\$_0-9\\-]+"); //$NON-NLS-1$

	protected CloudFoundryServicePlanWizardPage(CloudFoundryServer cloudServer) {
		super(Messages.CloudFoundryServicePlanWizardPage_TEXT_SERVICE_PAGE);
		this.cloudServer = cloudServer;
		setTitle(Messages.CloudFoundryServicePlanWizardPage_TITLE_SERVICE_CONFIG);
		setDescription(Messages.CloudFoundryServicePlanWizardPage_TEXT_FINISH_ADD);
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
		this.service = createService();
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(10, LayoutConstants.getSpacing().y).applyTo(composite);

		Label label = new Label(composite, SWT.NONE);
		label.setText(Messages.COMMONTXT_NAME_WITH_COLON);

		nameText = new Text(composite, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(nameText);
		nameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				service.setName(nameText.getText());
			}
		});

		bindingContext = new DataBindingContext();
		map = new WritableMap();

		WizardPageSupport.create(this, bindingContext);

		bindingContext.bindValue(SWTObservables.observeText(nameText, SWT.Modify),
				Observables.observeMapEntry(map, "name"), //$NON-NLS-1$
				new UpdateValueStrategy().setAfterConvertValidator(new StringValidator()), null);

		label = new Label(composite, SWT.NONE);
		label.setText(Messages.CloudFoundryServicePlanWizardPage_LABEL_TYPE);

		typeCombo = new Combo(composite, SWT.READ_ONLY | SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(typeCombo);
		typeCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				int index = typeCombo.getSelectionIndex();
				if (index != -1) {
					CFServiceOffering configuration = serviceOfferings.get(index);
					setCloudService(service, configuration);
				}
				refreshPlan();
			}
		});

		bindingContext.bindValue(SWTObservables.observeSelection(typeCombo), Observables.observeMapEntry(map, "type"), //$NON-NLS-1$
				new UpdateValueStrategy().setAfterConvertValidator(new ComboValidator(Messages.CloudFoundryServicePlanWizardPage_TEXT_SELECT_TYPE)), null);

		pageBook = new PageBook(composite, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).span(2, 1).applyTo(pageBook);

		planGroup = new Group(pageBook, SWT.BORDER);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(planGroup);
		planGroup.setLayout(new GridLayout());
		planGroup.setVisible(false);
		planGroup.setText(getPlanLabel());

		MultiValidator validator = new MultiValidator() {
			protected IStatus validate() {
				// access plan value to bind validator
				if (planObservable.getValue() == null) {
					return ValidationStatus.cancel(getValidationErrorMessage());
				}
				return ValidationStatus.ok();
			}
		};
		bindingContext.addValidationStatusProvider(validator);

		Dialog.applyDialogFont(composite);
		setControl(composite);
	}

	public CFServiceInstance getService() {
		return service;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && serviceOfferings == null) {
			// delay until dialog is actually visible
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					if (!getControl().isDisposed()) {
						refresh();
					}
				}
			});
		}
	}

	protected boolean supportsSpaces() {
		return cloudServer != null && cloudServer.hasCloudSpace();
	}

	protected void refresh() {
		if (updateConfiguration()) {
			typeCombo.removeAll();
			for (CFServiceOffering offering : serviceOfferings) {
				String label = offering.getName() != null ? offering.getName() + " - " //$NON-NLS-1$
						+ offering.getDescription() : offering.getDescription();
				typeCombo.add(label);
			}
			refreshPlan();
		}
	}

	protected void refreshPlan() {
		int index = typeCombo.getSelectionIndex();
		if (index == -1) {
			pageBook.setVisible(false);
			planGroup.setVisible(false);

			// re-validate
			planObservable.setValue(null);
		}
		else {
			pageBook.setVisible(true);

			for (Control control : planGroup.getChildren()) {
				control.dispose();
			}
			CFServiceOffering configuration = serviceOfferings.get(index);
			List<CFServicePlan> servicePlans = getPlans(configuration);

			if (servicePlans.size() > 1) {
				pageBook.showPage(planGroup);
				planGroup.setVisible(true);

				Button defaultPlanControl = null;

				for (CFServicePlan plan : servicePlans) {

					String planLabelText = plan.getName();

					Button planButton = new Button(planGroup, SWT.RADIO);

					if (defaultPlanControl == null) {
						defaultPlanControl = planButton;
					}

					planButton.setText(planLabelText);
					planButton.setData(plan);
					planButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent event) {
							Button button = (Button) event.widget;
							if (button.getSelection()) {
								CFServicePlan plan = (CFServicePlan) button.getData();
								setPlan(plan);

							}
						}
					});
				}

				planDetailsComposite = new Composite(planGroup, SWT.NONE);
				GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 10, 0).numColumns(2)
						.applyTo(planDetailsComposite);

				// Set a default plan, if one exists
				if (defaultPlanControl != null) {
					defaultPlanControl.setSelection(true);
					CFServicePlan plan = (CFServicePlan) defaultPlanControl.getData();
					setPlan(plan);

				}
			}
			else if (servicePlans.size() == 1) {
				planGroup.setVisible(false);
				CFServicePlan plan = servicePlans.get(0);
				setPlan(plan);
			}
			else {
				pageBook.setVisible(false);
			}
		}
		((Composite) getControl()).layout(true, true);

	}

	protected void setPlan(CFServicePlan plan) {
		getService().setPlan(plan.getName());
		// re-validate
		planObservable.setValue(plan);
	}

	protected boolean updateConfiguration() {
		try {
			getContainer().run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						serviceOfferings = cloudServer.getBehaviour().getServiceOfferings(monitor);
						Collections.sort(serviceOfferings, new Comparator<CFServiceOffering>() {
							public int compare(CFServiceOffering o1, CFServiceOffering o2) {
								return o1.getDescription().compareTo(o2.getDescription());
							}
						});
						sortServicePlans(serviceOfferings);

					}
					catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
					catch (OperationCanceledException e) {
						throw new InterruptedException();
					}
					finally {
						monitor.done();
					}
				}
			});
			return true;
		}
		catch (InvocationTargetException e) {
			IStatus status = cloudServer.error(
					NLS.bind(Messages.CloudFoundryServicePlanWizardPage_ERROR_CONFIG_RETRIVE, e.getCause().getMessage()), e);
			StatusManager.getManager().handle(status, StatusManager.LOG);
			setMessage(status.getMessage(), IMessageProvider.ERROR);
		}
		catch (InterruptedException e) {
			// ignore
		}
		return false;
	}

	protected class ComboValidator implements IValidator {

		private final String message;

		public ComboValidator(String message) {
			this.message = message;
		}

		public IStatus validate(Object value) {
			if (value instanceof String && ((String) value).length() > 0) {
				return Status.OK_STATUS;
			}
			return ValidationStatus.cancel(message);
		}

	}

	protected class StringValidator implements IValidator {

		public IStatus validate(Object value) {
			if (value instanceof String) {
				if (((String) value).length() == 0) {
					return ValidationStatus.cancel(Messages.CloudFoundryServicePlanWizardPage_TEXT_ENTER_NAME);
				}
				Matcher matcher = VALID_CHARS.matcher((String) value);
				if (!matcher.matches()) {
					return ValidationStatus.error(Messages.CloudFoundryServicePlanWizardPage_ERROR_INVALID_CHAR);
				}
			}
			return Status.OK_STATUS;
		}

	}

	protected void sortServicePlans(List<CFServiceOffering> configurations) {

		for (CFServiceOffering offering : configurations) {
			Collections.sort(offering.getServicePlans(), new Comparator<CFServicePlan>() {
				public int compare(CFServicePlan o1, CFServicePlan o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
		}
	}

	protected List<CFServicePlan> getPlans(CFServiceOffering offering) {
		List<CFServicePlan> plans = new ArrayList<CFServicePlan>();

		List<CFServicePlan> cloudPlans = offering.getServicePlans();

		if (cloudPlans != null) {
			for (CFServicePlan plan : cloudPlans) {
				plans.add(plan);
			}
		}
		return plans;

	}

	protected String getValidationErrorMessage() {
		return Messages.CloudFoundryServicePlanWizardPage_ERROR_SELECT_PLAN;
	}

	protected String getPlanLabel() {
		return Messages.CloudFoundryServicePlanWizardPage_LABEL_PLAN;
	}

	protected void setCloudService(CFServiceInstance service, CFServiceOffering offering) {

		service.setVersion(offering.getVersion());
		service.setService(offering.getName());

	}

	protected CFServiceInstance createService() {
		CFServiceInstance service = new CFServiceInstance(""); //$NON-NLS-1$
		return service;
	}

}
