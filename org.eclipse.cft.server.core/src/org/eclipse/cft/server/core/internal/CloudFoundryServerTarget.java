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
package org.eclipse.cft.server.core.internal;

import org.eclipse.cft.server.core.internal.client.ClientRequestFactory;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;

/**
 * Internal use only
 * <p/>
 * Contains additional server information that is not defined in server
 * extension points
 *
 */
public abstract class CloudFoundryServerTarget {

	public static final String ALL_SERVERS = "allcloudfoundryservers";

	public static final CloudFoundryServerTarget DEFAULT = new CloudFoundryServerTarget() {

		@Override
		public boolean supportsSsh() {
			return false;
		}

		@Override
		public String getServerUri() {
			return ALL_SERVERS;
		}

		@Override
		public ClientRequestFactory getRequestFactory(CloudFoundryServerBehaviour behaviour) {
			return new ClientRequestFactory(behaviour);
		}
	};

	/**
	 * 
	 * @return server URI if applicable to a specific CF server vendor. Must not
	 * be null.
	 */
	abstract public String getServerUri();

	abstract public boolean supportsSsh();

	/**
	 * 
	 * @param behaviour instance of the server behaviour that requires a request
	 * factory
	 * @return Non-null request factory. All Cloud Foundry based servers use a
	 * client for Cloud requests.
	 */
	abstract public ClientRequestFactory getRequestFactory(CloudFoundryServerBehaviour behaviour);
}
