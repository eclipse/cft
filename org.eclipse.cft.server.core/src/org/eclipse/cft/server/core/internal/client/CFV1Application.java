/*******************************************************************************
 * Copyright (c) 2016 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.core.internal.client;

import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.wst.server.core.IModule;

/**
 * A lower-level, internal wrapper around an application on Cloud Foundry, as
 * represented by the v1 CF Java client. This lower-level model of a CF
 * application is an aggregate of different components that define an CF
 * application, for example the actual application as well as application
 * instances (i.e. stats).
 * <p/>
 * This is a wrapper only used to address shortcomings of v1 CF client
 * {@link CloudApplication} type.
 * <p/>
 * Should not be used outside of the CFT framework as this is a transient
 * definition that will be removed when v1 support is removed from CFT. CFT
 * framework primarily works on {@link IModule} and related
 * {@link CloudFoundryApplicationModule}. Users should access an application on
 * CF using either of these module types instead.
 *
 */
public class CFV1Application {

	private final ApplicationStats stats;

	private final CloudApplication application;

	public CFV1Application(ApplicationStats stats, CloudApplication application) {
		this.stats = stats;
		this.application = application;
	}

	/**
	 * 
	 * @return {@link ApplicationStats} if available. This may be null if the
	 * application is stopped or not fully running (e.g starting)
	 */
	public ApplicationStats getStats() {
		return stats;
	}

	/**
	 * @return non-null {@link CloudApplication}
	 */
	public CloudApplication getApplication() {
		return application;
	}

}
