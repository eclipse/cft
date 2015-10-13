/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;



/**
 * @author Terry Denney
 */
public class DeleteServicesWizardPage extends WizardPage {

	private final List<String> services;

	private CheckboxTableViewer viewer;
	
	private Object[] checkedElements;

	public DeleteServicesWizardPage(CloudFoundryServer cloudServer, List<String> services) {
		super(Messages.DeleteServicesWizardPage_TEXT_SERVICE);
		this.services = services;
		setTitle(Messages.DeleteServicesWizardPage_TITLE_DELETE_SERVICE);
		setDescription(Messages.DeleteServicesWizardPage_TEXT_SELECT_SERVICE);
		ImageDescriptor banner = CloudFoundryImages.getWizardBanner(cloudServer.getServer().getServerType().getId());
		if (banner != null) {
			setImageDescriptor(banner);
		}
	}
	
	@Override
	public boolean isPageComplete() {
//		return checkedElements != null && checkedElements.length > 0;
		return true;
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		
		Table table = new Table(container, SWT.CHECK | SWT.BORDER);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		viewer = new CheckboxTableViewer(table);
		
		viewer.addCheckStateListener(new ICheckStateListener() {
			
			public void checkStateChanged(CheckStateChangedEvent event) {
				checkedElements = viewer.getCheckedElements();
				getWizard().getContainer().updateButtons();
			}
		});
		
		LabelProvider labelProvider = new LabelProvider();
		ITreeContentProvider contentProvider = new ITreeContentProvider() {
			
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				// ignore
			}
			
			public void dispose() {
				// ignore
			}
			
			public Object[] getElements(Object inputElement) {
				return services.toArray();
			}
			
			public boolean hasChildren(Object element) {
				return false;
			}
			
			public Object getParent(Object element) {
				return null;
			}
			
			public Object[] getChildren(Object parentElement) {
				return null;
			}
		};
		
		viewer.setLabelProvider(labelProvider);
		viewer.setContentProvider(contentProvider);
		viewer.setInput(services);
		
		setControl(container);
		
		getWizard().getContainer().updateButtons();
	}

	public List<String> getSelectedServices() {
		List<String> result = new ArrayList<String>();
		if (checkedElements != null) {
			for(Object checkedElement: checkedElements) {
				result.add((String) checkedElement);
			}
		}
		return result;
	}

}
