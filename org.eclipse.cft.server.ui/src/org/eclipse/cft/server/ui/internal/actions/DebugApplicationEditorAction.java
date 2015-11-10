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

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.cft.server.core.internal.debug.ApplicationDebugLauncher;
import org.eclipse.cft.server.core.internal.debug.CloudFoundryDebugDelegate;
import org.eclipse.cft.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class DebugApplicationEditorAction extends EditorAction {

	protected final CloudFoundryApplicationModule appModule;

	protected final CloudFoundryServer cloudServer;

	protected final int appInstance;

	protected final int remoteDebugPort;

	protected final ApplicationDebugLauncher launcher;

	public DebugApplicationEditorAction(CloudFoundryApplicationsEditorPage editorPage,
			CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer, int appInstance,
			int remoteDebugPort, ApplicationDebugLauncher launcher) {
		super(editorPage, RefreshArea.DETAIL);
		this.appModule = appModule;
		this.cloudServer = cloudServer;
		this.appInstance = appInstance;
		this.launcher = launcher;
		this.remoteDebugPort = remoteDebugPort;
	}

	public DebugApplicationEditorAction(CloudFoundryApplicationsEditorPage editorPage,
			CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer, int appInstance,
			ApplicationDebugLauncher launcher) {
		this(editorPage, appModule, cloudServer, appInstance, CloudFoundryDebugDelegate.DEFAULT_REMOTE_PORT, launcher);
	}

	public ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {
		return new ICloudFoundryOperation() {

			public void run(IProgressMonitor monitor) throws CoreException {
				launcher.launch(appModule, cloudServer, appInstance, remoteDebugPort);
			}
		};
	}

	public String getJobName() {
		return getOperationLabel() + " - " //$NON-NLS-1$
				+ appModule.getDeployedApplicationName();
	}

	protected String getOperationLabel() {
		return "Connecting to debugger"; //$NON-NLS-1$
	}
}
