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
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.cft.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.cft.server.ui.internal.CFUiUtil;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.UpdatePasswordDialog;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class UpdatePasswordOperation implements ICloudFoundryOperation {

	private final CloudFoundryServer cloudServer;

	public UpdatePasswordOperation(CloudFoundryServer cloudServer) {
		this.cloudServer = cloudServer;
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {

		final String[] updatedPassword = new String[1];

		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				Shell shell = CFUiUtil.getShell();
				if (shell == null || shell.isDisposed()) {
					CloudFoundryPlugin.logError("No shell available to open update password dialogue"); //$NON-NLS-1$
					return;
				}
				final UpdatePasswordDialog dialog = new UpdatePasswordDialog(shell, cloudServer.getUsername(),
						cloudServer.getServer().getId());

				if (dialog.open() == Window.OK) {
					updatedPassword[0] = dialog.getPassword();
				}
			}
		});

		// Perform this outside of UI thread as to not lock it if it is long
		// running
		if (updatedPassword[0] != null) {
			cloudServer.setAndSavePassword(updatedPassword[0]);

			// Once password has been changed, reconnect the server to verify
			// that the password is valid
			final IStatus[] changeStatus = { Status.OK_STATUS };
			try {
				cloudServer.getBehaviour().reconnect(monitor);
			}
			catch (CoreException e) {
				changeStatus[0] = e.getStatus();
			}

			ServerEventHandler.getDefault().firePasswordUpdated(cloudServer, changeStatus[0]);

			Display.getDefault().asyncExec(new Runnable() {

				@Override
				public void run() {
					if (!changeStatus[0].isOK()) {
						if (changeStatus[0].getException() != null) {
							CloudFoundryPlugin.logError(changeStatus[0].getException());
						}
						MessageDialog.openError(
								Display.getDefault().getActiveShell(),
								Messages.UpdatePasswordDialog_ERROR_VERIFY_PW_TITLE,
								NLS.bind(Messages.UpdatePasswordCommand_ERROR_PW_UPDATE_BODY,
										changeStatus[0].getMessage()));
					}
					else {
						MessageDialog.openInformation(Display.getDefault().getActiveShell(),
								Messages.UpdatePasswordCommand_TEXT_PW_UPDATE,
								Messages.UpdatePasswordCommand_TEXT_PW_UPDATE_SUCC);
					}
				}
			});
		}
	}

}
