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
 *     Keith Chong, IBM - Support more general branded server type IDs via org.eclipse.ui.menus
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.actions;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.ui.internal.CloudUiUtil;
import org.eclipse.cft.server.ui.internal.wizards.OrgsAndSpacesWizard;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.progress.UIJob;

public class CloneServerCommand extends BaseCommandHandler {

	private IWorkbenchPart activePart;
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		initializeSelection(event);
		activePart = HandlerUtil.getActivePart(event);
		
		String error = null;
		CloudFoundryServer cloudServer = selectedServer != null ? (CloudFoundryServer) selectedServer.loadAdapter(
				CloudFoundryServer.class, null) : null;
		if (selectedServer == null) {
			error = "No Cloud Foundry server instance available to run the selected action."; //$NON-NLS-1$
		}

		if (error == null) {
			doRun(cloudServer);
		}
		else {
			CloudFoundryPlugin.logError(error);
		}
		
		return null;
	}
	
	private String getJobName() {
		return "Cloning server to selected space"; //$NON-NLS-1$
	}
	
	public void doRun(final CloudFoundryServer cloudServer) {
		final Shell shell = activePart != null && activePart.getSite() != null ? activePart.getSite().getShell()
				: CloudUiUtil.getShell();

		if (shell != null) {
			UIJob job = new UIJob(getJobName()) {

				public IStatus runInUIThread(IProgressMonitor monitor) {
					OrgsAndSpacesWizard wizard = new OrgsAndSpacesWizard(cloudServer);
					WizardDialog dialog = new WizardDialog(shell, wizard);
					dialog.open();

					return Status.OK_STATUS;
				}
			};
			job.setSystem(true);
			job.schedule();
		}
		else {
			CloudFoundryPlugin.logError("Unable to find an active shell to open the orgs and spaces wizard."); //$NON-NLS-1$
		}

	}


}
