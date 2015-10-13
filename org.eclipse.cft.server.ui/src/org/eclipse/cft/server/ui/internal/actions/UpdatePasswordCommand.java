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
 *     Keith Chong, IBM - Support more general branded server type IDs via org.eclipse.ui.menus
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.actions;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class UpdatePasswordCommand extends BaseCommandHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		initializeSelection(event);
		final CloudFoundryServer cfServer = (CloudFoundryServer) selectedServer.loadAdapter(CloudFoundryServer.class,
				null);

		Job job = new Job(Messages.UpdatePasswordCommand_TEXT_PW_UPDATE) {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					new UpdatePasswordOperation(cfServer).run(monitor);
				}
				catch (CoreException e) {
					CloudFoundryPlugin.logError(e);
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		return null;
	}
}
