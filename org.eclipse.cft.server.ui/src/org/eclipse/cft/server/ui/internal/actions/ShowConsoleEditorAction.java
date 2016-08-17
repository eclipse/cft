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
package org.eclipse.cft.server.ui.internal.actions;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;

/**
 * @author Steffen Pingel
 */
public class ShowConsoleEditorAction extends Action {

	private final CloudFoundryServer server;

	private final CloudFoundryApplicationModule appModule;

	private final int instanceIndex;

	public ShowConsoleEditorAction(CloudFoundryServer server, CloudFoundryApplicationModule appModule,
			int instanceIndex) {
		this.server = server;
		this.appModule = appModule;
		this.instanceIndex = instanceIndex;
		setText(Messages.ShowConsoleEditorAction_TEXT_SHOW_CONSOLE);
	}

	public ShowConsoleEditorAction(CloudFoundryServer server, CloudFoundryApplicationModule appModule) {
		this(server, appModule, 0);
	}

	@Override
	public void run() {
		Job job = new Job(Messages.SHOWING_CONSOLE) {

			@Override
			public IStatus run(IProgressMonitor monitor) {
				if (CloudFoundryPlugin.getCallback() != null) {
					CloudFoundryPlugin.getCallback().stopApplicationConsole(appModule, server);

					CloudFoundryPlugin.getCallback().printToConsole(server, appModule, Messages.SHOWING_CONSOLE, true,
							false);

					CloudFoundryPlugin.getCallback().showCloudFoundryLogs(server, appModule, instanceIndex, monitor);
					return Status.OK_STATUS;
				}
				else {
					return CloudFoundryPlugin.getErrorStatus(
							"Internal Error: No Cloud Foundry console callback available. Unable to refresh console contents."); //$NON-NLS-1$
				}
			}

		};
		job.setSystem(true);
		job.setPriority(Job.INTERACTIVE);
		job.schedule();

	}

}
