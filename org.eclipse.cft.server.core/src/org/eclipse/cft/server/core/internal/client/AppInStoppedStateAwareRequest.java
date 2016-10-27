/*******************************************************************************
 * Copyright (c) 2015, 2016 Pivotal Software, Inc. and others 
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

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * 
 * Reattempts the operation if a app in stopped state error is encountered.
 * 
 */
abstract class AppInStoppedStateAwareRequest<T> extends V1ClientRequest<T> {

	public AppInStoppedStateAwareRequest(CloudFoundryServer cloudServer, CloudServerCFClient client, String label) {
		super(cloudServer, client, label);
	}

	@Override
	protected long getRetryInterval(Throwable exception, IProgressMonitor monitor) throws CoreException {

		if (exception instanceof CoreException) {
			exception = ((CoreException) exception).getCause();
		}

		if (exception instanceof CloudFoundryException
				&& CloudErrorUtil.isAppStoppedStateError((CloudFoundryException) exception)) {
			return CloudOperationsConstants.ONE_SECOND_INTERVAL;
		}
		return -1;
	}
}