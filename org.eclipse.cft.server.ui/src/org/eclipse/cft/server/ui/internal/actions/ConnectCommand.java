/*******************************************************************************
 * Copyright (c) 2014, 2016 Pivotal Software, Inc. and others 
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

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.ui.internal.CloudFoundryServerUiPlugin;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.wizards.ConnectSsoServerDialog;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.server.core.IServer;

public class ConnectCommand extends BaseCommandHandler {
	private static final Object dialogLock = new Object();
	
	// SSO Only - Is the SSO connection dialog currently open on the screen; synchronize on dialog lock to read/write.
	private static boolean isDialogOpen = false;
	
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// Always init first
		initializeSelection(event);
		
		return openConnectDialogInJob(selectedServer);
	}
	
	private static Object openConnectDialogInJob(IServer selectedServer) {
		
		final CloudFoundryServer cloudServer = (CloudFoundryServer) selectedServer.loadAdapter(CloudFoundryServer.class, null);
		Job connectJob = new Job(Messages.ConnectCommand_JOB_CONN_SERVER) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					openConnectDialog(cloudServer, monitor);
				}
				catch (OperationCanceledException e) {
					return Status.CANCEL_STATUS;
				}
//				catch (CoreException e) {
////					Trace.trace(Trace.STRING_SEVERE, "Error calling connect() ", e);
//					return new Status(IStatus.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID,
//							NLS.bind(Messages.ConnectCommand_ERROR_CONNECT, e.getMessage()));
//				}
				return Status.OK_STATUS;
			}
		};
		connectJob.schedule();

		return null;
	}
	
	public static boolean openConnectDialog(final CloudFoundryServer cloudServer, IProgressMonitor monitor) {
		final AtomicBoolean result = new AtomicBoolean(false);
		
		if (!cloudServer.isSso()/* || cloudServer.getToken() != null*/) {
			try {
				cloudServer.getBehaviour().connect(monitor);
				result.set(true);
			}
			catch (Exception e) {
				CloudFoundryServerUiPlugin.logError(e);
				result.set(false);
			}
		}
		if (cloudServer.isSso() && !cloudServer.isConnected()) {
				
			Display.getDefault().syncExec(new Runnable() {
				
				@Override
				public void run() {
					
					synchronized(dialogLock) {
						if(isDialogOpen) {
							// We only want instance of the dialog at a time. 
							return;
						} else {
							isDialogOpen = true;
						}
					}
					
					try {
						
						Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
						ConnectSsoServerDialog dialog = new ConnectSsoServerDialog(shell, cloudServer);
						dialog.open();
						
						result.set(dialog.isConnectionSuccess());
						
					} finally {
						
						synchronized(dialogLock) {
							isDialogOpen = false;
						}
					}
				}
			});
			
		}
	
		
		return result.get();
	}
	
}
