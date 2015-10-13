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

import org.eclipse.cft.server.ui.internal.DebugCommand;
import org.eclipse.cft.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class TerminateDebugEditorAction extends DebugApplicationEditorAction {

	public TerminateDebugEditorAction(CloudFoundryApplicationsEditorPage editorPage, DebugCommand debugCommand) {
		super(editorPage, debugCommand);
	}

	@Override
	protected void runDebugOperation(DebugCommand command, IProgressMonitor monitor) throws CoreException {
		command.terminate();
	}

	@Override
	protected String getOperationLabel() {
		return "Terminating debugger connection"; //$NON-NLS-1$
	}
}
