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

import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.cft.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Terry Denney
 */
public class UpdateApplicationMemoryAction extends EditorAction {

	private final int memory;

	private final CloudFoundryApplicationModule module;

	public UpdateApplicationMemoryAction(CloudFoundryApplicationsEditorPage editorPage, int memory,
			CloudFoundryApplicationModule module) {
		super(editorPage, RefreshArea.DETAIL);
		this.memory = memory;
		this.module = module;
	}

	@Override
	public String getJobName() {
		return "Updating application memory limit"; //$NON-NLS-1$
	}

	@Override
	protected ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {
		return getBehaviour().operations().memoryUpdate(module, memory);
	}

}
