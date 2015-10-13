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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cft.server.core.internal.log.LogContentType;

/**
 * Loads console stream contributors for different console content types (e.g.
 * HTTP Tracing, application log, standard out)
 *
 */
public class ConsoleStreamRegistry {

	private static ConsoleStreamRegistry current;

	private List<ConsoleStreamProvider> providers;

	public static ConsoleStreamRegistry getInstance() {
		if (current == null) {
			current = new ConsoleStreamRegistry();
		}
		return current;
	}

	public ConsoleStream getStream(LogContentType type) {
		if (type == null) {
			return null;
		}

		if (providers == null) {
			providers = loadProviders();
		}

		ConsoleStreamProvider supportedProvider = null;
		int i = 0;
		while (supportedProvider == null && i < providers.size()) {
			ConsoleStreamProvider provider = providers.get(i);
			LogContentType[] supportedTypes = provider.getSupportedTypes();
			for (LogContentType tp : supportedTypes) {
				if (tp.equals(type)) {
					supportedProvider = provider;
					break;
				}
			}
			i++;
		}

		return supportedProvider != null ? supportedProvider.getStream(type) : null;
	}

	public List<ConsoleStreamProvider> loadProviders() {
		List<ConsoleStreamProvider> providers = new ArrayList<ConsoleStreamProvider>();
		providers.add(new StdStreamProvider());
		providers.add(new TraceStreamProvider());
		providers.add(new ApplicationLogStreamProvider());
		return providers;
	}

}
