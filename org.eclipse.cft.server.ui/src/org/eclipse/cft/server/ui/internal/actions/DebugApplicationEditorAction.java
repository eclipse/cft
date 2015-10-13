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

import org.eclipse.cft.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.cft.server.ui.internal.DebugCommand;
import org.eclipse.cft.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class DebugApplicationEditorAction extends EditorAction {

	private final DebugCommand debugCommand;

	public DebugApplicationEditorAction(CloudFoundryApplicationsEditorPage editorPage, DebugCommand debugCommand) {
		super(editorPage, RefreshArea.DETAIL);
		this.debugCommand = debugCommand;
	}

	public ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {
		return new ICloudFoundryOperation() {

			public void run(IProgressMonitor monitor) throws CoreException {
				if (debugCommand != null) {
					runDebugOperation(debugCommand, monitor);
				}
			}
		};
	}

	protected void runDebugOperation(DebugCommand command, IProgressMonitor monitor) throws CoreException {
		command.debug(monitor);
	}

	public String getJobName() {
		return getOperationLabel()
				+ " - " + debugCommand.getLaunch().getApplicationModule().getDeployedApplicationName(); //$NON-NLS-1$
	}

	protected String getOperationLabel() {
		return "Connecting to debugger"; //$NON-NLS-1$
	}
}
