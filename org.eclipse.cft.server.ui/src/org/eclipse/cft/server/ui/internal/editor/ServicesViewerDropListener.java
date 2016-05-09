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
package org.eclipse.cft.server.ui.internal.editor;

import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.ui.internal.actions.AddServicesToApplicationAction;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;


/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class ServicesViewerDropListener extends ViewerDropAdapter {

//	private CloudApplication application;

	private final CloudFoundryApplicationsEditorPage editorPage;

	private CloudFoundryApplicationModule appModule;

	private final CloudFoundryServerBehaviour serverBehaviour;

	protected ServicesViewerDropListener(Viewer viewer, CloudFoundryServerBehaviour serverBehaviour,
			CloudFoundryApplicationsEditorPage editorPage) {
		super(viewer);
		this.serverBehaviour = serverBehaviour;
		this.editorPage = editorPage;
	}

	@Override
	public void dragEnter(DropTargetEvent event) {
		if (event.detail == DND.DROP_DEFAULT || event.detail == DND.DROP_NONE) {
			event.detail = DND.DROP_COPY;
		}
		super.dragEnter(event);
	}

	@Override
	public boolean performDrop(Object data) {
		IStructuredSelection selection = (IStructuredSelection) data;
		new AddServicesToApplicationAction(selection, appModule, serverBehaviour, editorPage).run();

		return true;
	}

	public void setModule(CloudFoundryApplicationModule module) {
		this.appModule = module;
		
//		if (module == null) {
//			this.application = null;
//		} else {
//			this.application = module.getApplication();
//		}
	}

	@Override
	public boolean validateDrop(Object target, int operation, TransferData type) {
		overrideOperation(DND.DROP_COPY);
//		if (application == null)
//			return false;

		if (operation == DND.DROP_COPY || operation == DND.DROP_DEFAULT) {
			if (LocalSelectionTransfer.getTransfer().isSupportedType(type)) {
				IStructuredSelection selection = (IStructuredSelection) LocalSelectionTransfer.getTransfer()
						.getSelection();
				Object[] objects = selection.toArray();
				for (Object obj : objects) {
					if (obj instanceof CFServiceInstance) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
