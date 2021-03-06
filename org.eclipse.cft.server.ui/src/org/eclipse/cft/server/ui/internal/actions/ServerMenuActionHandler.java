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
import org.eclipse.jface.action.IAction;
import org.eclipse.wst.server.core.IServer;

public class ServerMenuActionHandler extends MenuActionHandler<IServer> {

	protected ServerMenuActionHandler() {
		super(IServer.class);
	}

	@Override
	protected List<IAction> getActionsFromSelection(IServer server) {
		CloudFoundryServer cloudFoundryServer = (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
		if (cloudFoundryServer == null || server.getServerState() != IServer.STATE_STARTED) {
			return Collections.emptyList();
		}
		List<IAction> actions = new ArrayList<IAction>();

		return actions;
	}

}
