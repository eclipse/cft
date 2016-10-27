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

import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * [Bug 480364] - Fetching applications from Diego-enabled targets results in
 * 503 Server errors if application is starting. Retry the operation when 503 is
 * encountered.
 */
abstract public class ApplicationRequest<T> extends V1ClientRequest<T> {

	public ApplicationRequest(CloudFoundryServer cloudServer, CloudServerCFClient client, String label) {
		super(cloudServer, client, label);
	}

	@Override
	protected long getRetryInterval(Throwable exception, IProgressMonitor monitor) throws CoreException {
		if (CloudErrorUtil.is503Error(exception)) {
			return 2000;
		}
		return super.getRetryInterval(exception, monitor);
	}

	@Override
	protected long getRetryTimeout() {
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