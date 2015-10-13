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
package org.eclipse.cft.server.tests.core;

import java.util.UUID;

import org.eclipse.cft.server.core.internal.ServerCredentialsStore;

import junit.framework.TestCase;

/**
 * @author Steffen Pingel
 */
public class ServerCredentialsStoreTest extends TestCase {

	private ServerCredentialsStore store;

	public void testFlush() {
		store.setUsername("foo");
		store.setPassword("bar");
		String newServerId = serverId();
		store.flush(newServerId);
		assertEquals("foo", store.getUsername());
		assertEquals("bar", store.getPassword());

		store.flush(newServerId);
		assertEquals("foo", store.getUsername());
		assertEquals("bar", store.getPassword());
	}

	public void testFlushNull() {
		store.flush("serverId");
		assertEquals(null, store.getPassword());
		assertEquals(null, store.getUsername());
	}

	public void testGetSetPassword() throws Exception {
		assertNull(store.getPassword());
		store.setPassword("pass");
		assertEquals("pass", store.getPassword());

		store.flush(serverId());
		assertEquals("pass", store.getPassword());
	}

	public void testGetSetUsername() throws Exception {
		assertNull(store.getUsername());
		store.setUsername("user");
		assertEquals("user", store.getUsername());

		String newServerId = serverId();
		store.flush(newServerId);
		assertEquals("user", store.getUsername());

		store.setUsername("");
		assertEquals("", store.getUsername());

		store.flush(newServerId);
		assertEquals("", store.getUsername());
	}

	public void testPersistFlush() {
		store.setUsername("user");
		assertEquals("user", store.getUsername());

		store.flush(store.getServerId());
		ServerCredentialsStore store2 = new ServerCredentialsStore(store.getServerId());
		assertEquals("user", store2.getUsername());
	}

	public void testPersistFlushNewId() {
		String oldServerId = store.getServerId();
		store.setUsername("user");
		assertEquals("user", store.getUsername());

		String newServerId = serverId();
		store.flush(newServerId);
		assertEquals(newServerId, store.getServerId());

		ServerCredentialsStore newStore = new ServerCredentialsStore(oldServerId);
		assertEquals(null, newStore.getUsername());

		newStore = new ServerCredentialsStore(newServerId);
		assertEquals("user", newStore.getUsername());
	}

	public void testPersistNoFlush() {
		store.setUsername("user");
		assertEquals("user", store.getUsername());

		ServerCredentialsStore store2 = new ServerCredentialsStore(store.getServerId());
		assertEquals(null, store2.getUsername());
	}

	public void testSetPasswordNull() throws Exception {
		store.setPassword("user");
		assertEquals("user", store.getPassword());

		store.setPassword(null);
		store.flush(store.getServerId());
		assertEquals(null, store.getPassword());
	}

	public void testSetUsernameNull() throws Exception {
		store.setUsername("user");
		assertEquals("user", store.getUsername());

		store.setUsername(null);
		store.flush(store.getServerId());
		assertEquals(null, store.getUsername());
	}

	private String serverId() {
		return ServerCredentialsStoreTest.class.getName() + ":" + UUID.randomUUID().toString();
	}

	@Override
	protected void setUp() throws Exception {
		store = new ServerCredentialsStore(serverId());
	}

}
