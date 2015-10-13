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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class RemoveServicesFromApplicationAction extends ModifyServicesForApplicationAction {

	private final List<String> services;

	public RemoveServicesFromApplicationAction(IStructuredSelection selection,
			CloudFoundryApplicationModule application, CloudFoundryServerBehaviour serverBehaviour,
			CloudFoundryApplicationsEditorPage editorPage) {
		super(application, serverBehaviour, editorPage);

		setText(Messages.RemoveServicesFromApplicationAction_TEXT_UNBIND_FROM_APP);
		setImageDescriptor(CloudFoundryImages.REMOVE);

		services = getServiceNames(selection);
	}

	@Override
	public String getJobName() {
		return "Unbinding services"; //$NON-NLS-1$
	}

	@Override
	public List<String> getServicesToAdd() {
		return new ArrayList<String>();
	}

	@Override
	public List<String> getServicesToRemove() {
		return services;
	}
}