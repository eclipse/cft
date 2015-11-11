/*******************************************************************************
 * Copied from Spring Tool Suite. Original license:
 * 
 * Copyright (c) 2015 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.cft.server.core.internal.client;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;

/**
 * 
 * Reattempts the operation if a app in stopped state error is encountered.
 * 
 */
abstract class AppInStoppedStateAwareRequest<T> extends BehaviourRequest<T> {

	public AppInStoppedStateAwareRequest(String label, CloudFoundryServerBehaviour behaviour) {
		super(label, behaviour);
	}

	protected long waitOnErrorInterval(Throwable exception, SubMonitor monitor) throws CoreException {

		if (exception instanceof CoreException) {
			exception = ((CoreException) exception).getCause();
		}

		if (exception instanceof CloudFoundryException
				&& CloudErrorUtil.isAppStoppedStateError((CloudFoundryException) exception)) {
			return CloudOperationsConstants.ONE_SECOND_INTERVAL;
		}
		return -1;
	}

	protected abstract T doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException;

}