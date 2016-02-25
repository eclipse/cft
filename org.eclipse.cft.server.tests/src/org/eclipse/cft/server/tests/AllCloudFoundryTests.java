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
package org.eclipse.cft.server.tests;

import org.eclipse.cft.server.tests.core.BehaviourOperationsTest;
import org.eclipse.cft.server.tests.core.CloudFoundryClientConnectionTest;
import org.eclipse.cft.server.tests.core.CloudFoundryProxyTest;
import org.eclipse.cft.server.tests.core.CloudFoundryServerBehaviourTest;
import org.eclipse.cft.server.tests.core.CloudFoundryServerTest;
import org.eclipse.cft.server.tests.core.CloudFoundryServicesTest;
import org.eclipse.cft.server.tests.core.CloudUtilTest;
import org.eclipse.cft.server.tests.core.DeploymentURLTest;
import org.eclipse.cft.server.tests.core.ModuleRefreshTest;
import org.eclipse.cft.server.tests.core.ServerCredentialsStoreTest;
import org.eclipse.cft.server.tests.sts.util.ManagedTestSuite;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Steffen Pingel
 */
public class AllCloudFoundryTests {

	public static final String PLUGIN_ID = "org.eclipse.cft.server.tests";

	public static Test suite() {
		return suite(false);
	}

	public static Test suite(boolean heartbeat) {
		TestSuite suite = new ManagedTestSuite(AllCloudFoundryTests.class.getName());

		suite.addTestSuite(BehaviourOperationsTest.class);
		suite.addTestSuite(ModuleRefreshTest.class);
		suite.addTestSuite(CloudFoundryServerBehaviourTest.class);

		suite.addTestSuite(CloudFoundryProxyTest.class);
		suite.addTestSuite(ServerCredentialsStoreTest.class);
		suite.addTestSuite(CloudFoundryServerTest.class);
		suite.addTestSuite(CloudUtilTest.class);

		suite.addTestSuite(DeploymentURLTest.class);
		suite.addTestSuite(CloudFoundryServicesTest.class);
		suite.addTestSuite(CloudFoundryClientConnectionTest.class);

		return suite;
	}

	public static Test experimentalSuite() {
		TestSuite suite = new ManagedTestSuite(AllCloudFoundryTests.class.getName());
		suite.addTestSuite(CloudFoundryServerBehaviourTest.class);
		return suite;
	}

}
