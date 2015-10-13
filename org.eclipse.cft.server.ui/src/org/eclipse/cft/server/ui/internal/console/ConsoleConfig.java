/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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
import org.eclipse.ui.console.MessageConsole;

/**
 * Contains configuration for a Cloud console, including the associated
 * application published to a Cloud server, the Cloud server itself, and the
 * underlying {@link MessageConsole} where content is to be displayed.
 *
 */
public class ConsoleConfig {

	private final CloudFoundryServer cloudServer;

	private final CloudFoundryApplicationModule appModule;

	private final MessageConsole messageConsole;

	public ConsoleConfig(MessageConsole messageConsole, CloudFoundryServer cloudServer,
			CloudFoundryApplicationModule appModule) {

		this.cloudServer = cloudServer;
		this.appModule = appModule;
		this.messageConsole = messageConsole;
	}

	public CloudFoundryServer getCloudServer() {
		return cloudServer;
	}

	public CloudFoundryApplicationModule getCloudApplicationModule() {
		return appModule;
	}

	public MessageConsole getMessageConsole() {
		return messageConsole;
	}
}
