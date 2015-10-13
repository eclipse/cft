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
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.actions;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;

public class UnmapProjectEditorAction extends Action {
	private final CloudFoundryApplicationsEditorPage editorPage;

	private final IModule module;

	public UnmapProjectEditorAction(CloudFoundryApplicationsEditorPage editorPage, IModule module) {

		setText(Messages.UnmapProjectEditorAction_ACTION_LABEL);

		this.editorPage = editorPage;
		this.module = module;
	}

	@Override
	public void run() {
		final CloudFoundryServer cloudServer = editorPage.getCloudServer();
		final CloudFoundryApplicationModule appModule = cloudServer.getExistingCloudModule(module);
		if (appModule != null && editorPage.getSite() != null && editorPage.getSite().getShell() != null) {

			Job job = new Job(NLS.bind(Messages.REMOVE_PROJECT_MAPPING, appModule.getDeployedApplicationName())) {
				protected IStatus run(IProgressMonitor monitor) {
					try {
						new UnmapProjectOperation(appModule, editorPage.getCloudServer()).run(monitor);
					}
					catch (CoreException e) {
						CloudFoundryPlugin.logError(e);
						return Status.CANCEL_STATUS;
					}
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}
	}
}
