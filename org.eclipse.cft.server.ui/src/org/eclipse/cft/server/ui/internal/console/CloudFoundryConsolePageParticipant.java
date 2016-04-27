/*******************************************************************************
 * Copyright (c) 2012, 2016 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.ui.internal.console;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.actions.CloseConsoleAction;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class CloudFoundryConsolePageParticipant implements IConsolePageParticipant {

	public void activated() {
		// ignore
	}

	public void deactivated() {
		// ignore
	}

	public void dispose() {
		// ignore
	}

	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		return null;
	}

	public void init(IPageBookViewPage page, IConsole console) {
		if (isCloudFoundryConsole(console)) {
			CloseConsoleAction closeAction = new CloseConsoleAction(console);
			closeAction.setImageDescriptor(CloudFoundryImages.CLOSE_CONSOLE);
			IToolBarManager manager = page.getSite().getActionBars().getToolBarManager();
			manager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, closeAction);
		}
	}

	protected boolean isCloudFoundryConsole(IConsole console) {
		if (console instanceof MessageConsole) {
			MessageConsole messageConsole = (MessageConsole) console;
			Object cfServerObj = messageConsole.getAttribute(ApplicationLogConsole.ATTRIBUTE_SERVER);
			Object cfAppModuleObj = messageConsole.getAttribute(ApplicationLogConsole.ATTRIBUTE_APP);
			if (cfServerObj instanceof CloudFoundryServer && cfAppModuleObj instanceof CloudFoundryApplicationModule) {
				CloudFoundryServer cfServer = (CloudFoundryServer) cfServerObj;
				CloudFoundryApplicationModule appModule = (CloudFoundryApplicationModule) cfAppModuleObj;

				CloudConsoleManager manager = ConsoleManagerRegistry.getConsoleManager(cfServer.getServer());
				if (manager != null) {
					MessageConsole existingConsole = manager.findCloudFoundryConsole(cfServer.getServer(), appModule);
					return messageConsole == existingConsole;
				}
			}
		}
		return false;
	}
}
