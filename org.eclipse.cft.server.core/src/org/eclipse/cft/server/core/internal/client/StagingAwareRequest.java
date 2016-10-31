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

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.NotFinishedStagingException;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;

/**
 * 
 * Request that is aware of potential staging related errors and may attempt
 * the request again on certain types of staging errors like Staging Not
 * Finished errors.
 * <p/>
 * Because the set of client operations wrapped around this Request may be
 * attempted again on certain types of errors, it's best to keep the set of
 * client operations as minimal as possible, to avoid performing client
 * operations again that had no errors.
 * 
 * <p/>
 * Note that this should only be used around certain types of operations
 * performed on a app that is already started, like fetching the staging
 * logs, or app instances stats, as re-attempts on these operations due to
 * staging related errors (e.g. staging not finished yet) is permissable.
 * 
 * <p/>
 * However, operations not related an application being in a running state
 * (e.g. creating a service, getting list of all apps), should not use this
 * request.
 */
abstract public class StagingAwareRequest<T> extends BehaviourRequest<T> {

	public StagingAwareRequest(String label, CloudFoundryServerBehaviour behaviour) {
		super(label, behaviour);
	}

	protected long waitOnErrorInterval(Throwable exception, SubMonitor monitor) throws CoreException {

		if (exception instanceof CoreException) {
			exception = ((CoreException) exception).getCause();
		}

		if (exception instanceof NotFinishedStagingException) {
			return CloudOperationsConstants.ONE_SECOND_INTERVAL * 2;
		}
		else if (exception instanceof CloudFoundryException
				&& CloudErrorUtil.isAppStoppedStateError((CloudFoundryException) exception)) {
			return CloudOperationsConstants.ONE_SECOND_INTERVAL;
		}
		return -1;
	}

	protected abstract T doRun(CloudFoundryOperations client, SubMonitor progress) throws CoreException;

}