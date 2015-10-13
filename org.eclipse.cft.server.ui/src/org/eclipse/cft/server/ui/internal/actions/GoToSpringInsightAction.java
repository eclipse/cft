/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
import org.eclipse.cft.server.ui.internal.CloudFoundryURLNavigation;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IWorkbenchPart;

public class GoToSpringInsightAction extends AbstractCloudFoundryServerAction {

	
	protected void serverSelectionChanged(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule, IAction action) {
		if (CloudFoundryURLNavigation.canEnableCloudFoundryNavigation(cloudServer)) {
			action.setEnabled(true);
			return;
		}
		action.setEnabled(false);
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// Nothing
	}

	@Override
	void doRun(CloudFoundryServer cloudServer, CloudFoundryApplicationModule appModule, IAction action) {
		CloudFoundryURLNavigation.INSIGHT_URL.navigate();
	}

}
