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
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.actions;

import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.cft.server.ui.internal.editor.ServicesHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;

/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class DeleteServicesAction extends EditorAction {

	private final CloudFoundryServerBehaviour serverBehaviour;

	private final ServicesHandler servicesHandler;

	public DeleteServicesAction(IStructuredSelection selection, CloudFoundryServerBehaviour serverBehaviour,
			CloudFoundryApplicationsEditorPage editorPage) {
		super(editorPage, RefreshArea.ALL);
		this.serverBehaviour = serverBehaviour;

		setText(Messages.DeleteServicesAction_TEXT_DELETE);
		setImageDescriptor(CloudFoundryImages.REMOVE);

		servicesHandler = new ServicesHandler(selection);
	}

	@Override
	public String getJobName() {
		return "Deleting services"; //$NON-NLS-1$
	}

	@Override
	public ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {
		return serverBehaviour.operations().deleteServices(servicesHandler.getServiceNames());
	}

	@Override
	public void run() {

		boolean confirm = MessageDialog.openConfirm(getEditorPage().getSite().getShell(),
				Messages.DeleteServicesAction_TEXT_DELETE_SERVICE,
				NLS.bind(Messages.DeleteServicesAction_TEXT_DELETE_CONFIRMATION, servicesHandler.toString()));
		if (confirm) {
			super.run();
		}
	}

}
