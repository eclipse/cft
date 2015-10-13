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
import java.util.Collections;
import java.util.List;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.cft.server.ui.internal.DebugCommand;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.wst.server.ui.IServerModule;

/**
 * Creates Cloud Foundry debug actions based on a given context. Valid context
 * should include a server module and a cloud foundry server as a bare minimum.
 * 
 */
public class DebugMenuActionHandler extends MenuActionHandler<IServerModule> {

	protected DebugMenuActionHandler() {
		super(IServerModule.class);
	}

	public static final String DEBUG_ACTION_ID = "org.eclipse.cft.server.ui.action.debug"; //$NON-NLS-1$

	static class DebugAction extends Action {

		protected final CloudFoundryServer cloudServer;

		protected final CloudFoundryApplicationModule appModule;

		public DebugAction(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule) {
			this.cloudServer = cloudServer;
			this.appModule = appModule;
			setActionValues();
		}

		protected void setActionValues() {
			setText(Messages.DebugMenuActionHandler_TEXT_DEBUG_TOOLTIP);
			setImageDescriptor(CloudFoundryImages.DEBUG);
			setToolTipText(Messages.DebugMenuActionHandler_TEXT_DEBUG_TOOLTIP);
			setEnabled(true);
		}

		public void run() {
			DebugCommand.debug(cloudServer, appModule);
		}

	}

	@Override
	protected List<IAction> getActionsFromSelection(IServerModule serverModule) {
		CloudFoundryServer cloudFoundryServer = (CloudFoundryServer) serverModule.getServer().loadAdapter(
				CloudFoundryServer.class, null);
		if (cloudFoundryServer == null) {
			return Collections.emptyList();
		}

		List<IAction> actions = new ArrayList<IAction>();

		// DebugAction menuAction = new DebugAction( cloudFoundryServer, appM);
		// actions.add(menuAction);
		return actions;
	}
}
