/*******************************************************************************
 * Copyright (c) 2012, 2016 Pivotal Software, Inc. 
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
import org.eclipse.cft.server.ui.internal.actions.EditorAction.RefreshArea;
import org.eclipse.cft.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;

/**
 * Performs a full refresh of all published modules. This may be a long running
 * operation, especially with Cloud spaces with a large list of published
 * applications. In addition, it also updates the instances and stats of any
 * selected module in the editor
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class RefreshEditorAction extends Action {

	private final CloudFoundryApplicationsEditorPage editorPage;

	public RefreshEditorAction(CloudFoundryApplicationsEditorPage editorPage) {

		setImageDescriptor(CloudFoundryImages.REFRESH);
		setText(Messages.RefreshApplicationEditorAction_TEXT_REFRESH);

		this.editorPage = editorPage;
	}

	/**
	 * Returns a refresh editor action appropriate to the area being refreshed.
	 * @param editorPage
	 * @param area to refresh
	 * @return Editor action for the given area. If no area is specified,
	 * returns a general refresh action. Never null.
	 */
	public static Action getRefreshAction(CloudFoundryApplicationsEditorPage editorPage, RefreshArea area) {

		if (area == RefreshArea.DETAIL && editorPage.getMasterDetailsBlock().getCurrentModule() != null) {
			return new RefreshModuleEditorAction(editorPage);
		}
		else {
			return new RefreshEditorAction(editorPage);
		}
	}

	@Override
	public void run() {
		Job j = new Job(Messages.RefreshApplicationEditorAction_TEXT_JOB) {

			@Override
			protected IStatus run(IProgressMonitor arg0) {
				// Initialize the server and initiate server update scheduler
				CloudFoundryServerBehaviour behaviour = editorPage.getCloudServer().getBehaviour();
				behaviour.getOperationsScheduler().updateAll();
				
				return Status.OK_STATUS;
			}
			
		};

		j.setPriority(Job.SHORT);
	
		j.schedule();
	}
}