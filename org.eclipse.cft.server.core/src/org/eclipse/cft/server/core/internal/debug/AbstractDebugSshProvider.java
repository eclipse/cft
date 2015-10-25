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
package org.eclipse.cft.server.core.internal.debug;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;

public abstract class AbstractDebugSshProvider implements IDebugProvider {

	protected void printToConsole(CloudFoundryApplicationModule appModule, CloudFoundryServer server, String message,
			boolean error) {
		if (appModule != null && server != null) {
			message = "Cloud Foundry SSH Debugging" //$NON-NLS-1$
					+ " - " + message + '\n'; //$NON-NLS-1$
			CloudFoundryPlugin.getCallback().printToConsole(server, appModule, message, false, error);
		}
	}
}
