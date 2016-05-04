/*******************************************************************************
 * Copyright (c) 2014, 2016 Pivotal Software, Inc. and others
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
import org.eclipse.cft.server.core.internal.log.LogContentType;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * 
 * NOTE: Must stay internal as API may change. Should NOT be extended or used by
 * adopters of CF Eclipse until made public.
 *
 */
public abstract class CloudConsoleManager {

	/**
	 * @param server
	 * @param app
	 * @param instanceIndex
	 * @param show Start console if show is true, otherwise reset and start only
	 * if console was previously created already
	 * @param monitor NOTE: may be removed in the future, when consoles are
	 * purely client callbacks that do not require progress monitors *
	 * 
	 */
	public abstract void startConsole(CloudFoundryServer server, LogContentType type,
			CloudFoundryApplicationModule appModule, int instanceIndex, boolean show, boolean clear,
			IProgressMonitor monitor);

	/**
	 * Find the message console that corresponds to the server and a given
	 * module. If there are multiple instances of the application, only the
	 * first one will get returned.
	 * @param server the server for that console
	 * @param module the app for that console
	 * @return the message console. Null if no corresponding console is found.
	 */
	public abstract MessageConsole findCloudFoundryConsole(IServer server, IModule module);

	public abstract void writeToStandardConsole(String message, CloudFoundryServer server,
			CloudFoundryApplicationModule appModule, int instanceIndex, boolean clear, boolean isError);

	/**
	 * Displays existing log content for the given running application instance.
	 * @param server cloud server
	 * @param appModule running application
	 * @param instanceIndex app index
	 * @param clear true if current app instance console should be cleared.
	 * False otherwise to continue tailing from existing content.
	 * @param monitor NOTE: may be removed in the future, when consoles are
	 * purely client callbacks that do not require progress monitors
	 */
	public abstract void showCloudFoundryLogs(CloudFoundryServer server, CloudFoundryApplicationModule appModule,
			int instanceIndex, boolean clear, IProgressMonitor monitor);

	public abstract void stopConsole(IServer server, CloudFoundryApplicationModule appModule, int instanceIndex);

	public abstract void stopConsoles();

}