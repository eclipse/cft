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

import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;

/**
 * [Bug 480364] - Fetching applications from Diego-enabled targets results
 * in 503 Server errors if application is starting. Retry the operation when
 * 503 is encountered.
 */
abstract class ApplicationRequest<T> extends BehaviourRequest<T> {

	public ApplicationRequest(String label, CloudFoundryServerBehaviour behaviour) {
		super(label, behaviour);
	}

	@Override
	protected long waitOnErrorInterval(Throwable exception, SubMonitor monitor) throws CoreException {
		if (CloudErrorUtil.is503Error(exception)) {
			return 2000;
		}
		return super.waitOnErrorInterval(exception, monitor);
	}

	@Override
	protected long getTotalTimeWait() {
		return CloudOperationsConstants.DEFAULT_INTERVAL;
	}

	@Override
	protected CoreException getErrorOnLastFailedAttempt(Throwable error) {

		if (CloudErrorUtil.is503Error(error)) {
			return CloudErrorUtil.asCoreException(get503Error(error), error, true);
		}
		return super.getErrorOnLastFailedAttempt(error);
	}

	protected abstract String get503Error(Throwable rce);
}