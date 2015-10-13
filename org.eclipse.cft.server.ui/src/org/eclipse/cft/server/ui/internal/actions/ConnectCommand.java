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

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.ui.internal.CloudFoundryServerUiPlugin;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

public class ConnectCommand extends BaseCommandHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Always init first
		initializeSelection(event);
		
		final CloudFoundryServer cloudServer = (CloudFoundryServer) selectedServer.loadAdapter(CloudFoundryServer.class, null);
		Job connectJob = new Job(Messages.ConnectCommand_JOB_CONN_SERVER) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					cloudServer.getBehaviour().connect(monitor);
				}
				catch (OperationCanceledException e) {
					return Status.CANCEL_STATUS;
				}
				catch (CoreException e) {
//					Trace.trace(Trace.STRING_SEVERE, "Error calling connect() ", e);
					return new Status(IStatus.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID,
							NLS.bind(Messages.ConnectCommand_ERROR_CONNECT, e.getMessage()));
				}
				return Status.OK_STATUS;
			}
		};
		connectJob.schedule();

		return null;
	}
}
