/*******************************************************************************
 * Copyright (c) 2012, 2017 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.tests.util;

import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class CloudFoundryTestUtil {

	public static final int DEFAULT_TEST_APP_MEMORY = 1024;

	/**
	 * @param host
	 * @param proxyType
	 * @return proxy or null if it cannot be resolved
	 */
	public static Proxy getProxy(String host, String proxyType) {
		Proxy foundProxy = null;
		try {

			URI uri = new URI(proxyType, "//" + host, null);
			List<Proxy> proxies = ProxySelector.getDefault().select(uri);

			if (proxies != null) {
				for (Proxy proxy : proxies) {
					if (proxy != Proxy.NO_PROXY) {
						foundProxy = proxy;
						break;
					}
				}
			}

		}
		catch (URISyntaxException e) {
			// No proxy
		}
		return foundProxy;
	}

	public static void waitIntervals(long timePerTick) {
		try {
			Thread.sleep(timePerTick);
		}
		catch (InterruptedException e) {

		}
	}
}
