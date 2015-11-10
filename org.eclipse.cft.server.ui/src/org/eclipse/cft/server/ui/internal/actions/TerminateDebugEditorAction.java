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

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.cft.server.core.internal.debug.ApplicationDebugLauncher;
import org.eclipse.cft.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class TerminateDebugEditorAction extends DebugApplicationEditorAction {

	public TerminateDebugEditorAction(CloudFoundryApplicationsEditorPage editorPage,
			CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer, int appInstance,
			ApplicationDebugLauncher launcher) {
		super(editorPage, appModule, cloudServer, appInstance, launcher);
	}

	public ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {
		return new ICloudFoundryOperation() {

			public void run(IProgressMonitor monitor) throws CoreException {
				launcher.terminateLaunch(appModule, cloudServer, appInstance);
			}
		};
	}

	@Override
	protected String getOperationLabel() {
		return "Terminating debugger connection"; //$NON-NLS-1$
	}
}
