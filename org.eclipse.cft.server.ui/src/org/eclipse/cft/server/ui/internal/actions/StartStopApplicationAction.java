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

import org.eclipse.cft.server.core.internal.ApplicationAction;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.cft.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class StartStopApplicationAction extends EditorAction {

	private final ApplicationAction action;

	private final CloudFoundryApplicationModule application;

	private final CloudFoundryServerBehaviour serverBehaviour;

	public StartStopApplicationAction(CloudFoundryApplicationsEditorPage editorPage, ApplicationAction action,
			CloudFoundryApplicationModule application, CloudFoundryServerBehaviour serverBehaviour) {
		super(editorPage, RefreshArea.DETAIL);
		this.action = action;
		this.application = application;
		this.serverBehaviour = serverBehaviour;
	}

	public ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {
		return serverBehaviour.operations().applicationDeployment(application, action);
	}
}
