/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
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
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.jface.action.Action;
import org.eclipse.wst.server.core.IModule;

/**
 * Refreshes a single module selected in the given editor page, as well as its
 * related instances and stats.
 * <p/>
 * No refresh occurs is no module is selected in the editor page.
 */
public class RefreshModuleEditorAction extends Action {

	private final CloudFoundryApplicationsEditorPage editorPage;

	protected RefreshModuleEditorAction(CloudFoundryApplicationsEditorPage editorPage) {
		setImageDescriptor(CloudFoundryImages.REFRESH);
		setText(Messages.RefreshApplicationEditorAction_TEXT_REFRESH);
		this.editorPage = editorPage;
	}

	@Override
	public void run() {
		IModule selectedModule = editorPage.getMasterDetailsBlock().getCurrentModule();
		CloudFoundryServerBehaviour behaviour = editorPage.getCloudServer().getBehaviour();
		behaviour.getRefreshHandler().schedulesRefreshApplication(selectedModule);
	}

}