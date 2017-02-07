/*******************************************************************************
 * Copyright (c) 2017 Pivotal Software, Inc. and others
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

import org.eclipse.cft.server.tests.util.HarnessPropertiesBuilder.HarnessCFService;

/**
 * CFT test harness properties that allows testing CFT against a CF target
 *
 */
public interface HarnessProperties {

	String getUsername();

	String getPassword();

	String getApiUrl();

	String getOrg();

	String getSpace();

	boolean skipSslValidation();

	String getBuildpack();

	String getSuccessLoadedMessage();

	HarnessCFService serviceToCreate();

}