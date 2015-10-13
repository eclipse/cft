/*******************************************************************************
 * Copyright (c) 2014, 2015 Pivotal Software, Inc. 
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
 *     Steven Hung, IBM - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.actions;

import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.ui.internal.Logger;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.cft.server.ui.internal.editor.ServicesHandler;
import org.eclipse.cft.server.ui.internal.wizards.ServiceToApplicationsBindingWizard;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.progress.UIJob;

public class ServiceToApplicationsBindingAction extends Action {

	private final CloudFoundryServerBehaviour serverBehaviour;

	private final ServicesHandler servicesHandler;

	private final CloudFoundryApplicationsEditorPage editorPage;

	public ServiceToApplicationsBindingAction(IStructuredSelection selection,
			CloudFoundryServerBehaviour serverBehaviour, CloudFoundryApplicationsEditorPage editorPage) {

		this.serverBehaviour = serverBehaviour;
		this.editorPage = editorPage;

		setText(Messages.MANAGE_SERVICES_TO_APPLICATIONS_ACTION);
		servicesHandler = new ServicesHandler(selection);
	}

	@Override
	public void run() {
		UIJob uiJob = new UIJob(Messages.MANAGE_SERVICES_TO_APPLICATIONS_TITLE) {
			public IStatus runInUIThread(IProgressMonitor monitor) {
				try {
					if (serverBehaviour != null) {
						ServiceToApplicationsBindingWizard wizard = new ServiceToApplicationsBindingWizard(
								servicesHandler, serverBehaviour.getCloudFoundryServer(), editorPage);
						WizardDialog dialog = new WizardDialog(editorPage.getSite().getShell(), wizard);
						dialog.open();
					}
				}
				catch (CoreException e) {
					if (Logger.ERROR) {
						Logger.println(Logger.ERROR_LEVEL, this, "runInUIThread", "Error launching wizard", e); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}

				return Status.OK_STATUS;
			}

		};
		uiJob.setSystem(true);
		uiJob.setPriority(Job.INTERACTIVE);
		uiJob.schedule();
	}

}
