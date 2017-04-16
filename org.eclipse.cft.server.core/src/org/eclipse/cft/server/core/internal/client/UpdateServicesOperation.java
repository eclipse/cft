/*******************************************************************************
 * Copyright (c) 2015, 2017 Pivotal Software, Inc. and others
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

import java.util.List;

import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.springframework.web.client.HttpServerErrorException;

public class UpdateServicesOperation extends CFOperation {

	private final BaseClientRequest<List<CFServiceInstance>> request;

	public UpdateServicesOperation(BaseClientRequest<List<CFServiceInstance>> request,
			CloudFoundryServerBehaviour behaviour) {
		super(behaviour);
		this.request = request;
	}

	@Override
	public String getOperationName() {
		return Messages.UpdateServicesOperation_OPERATION_MESSAGE;
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {
		try {
			List<CFServiceInstance> existingServices = request.run(monitor);
			ServerEventHandler.getDefault().fireServicesUpdated(getBehaviour().getCloudFoundryServer(),
					existingServices);
		}
		catch (CoreException e) {
			// See if it is an HttpServerErrorException (thrown when receiving,
			// for example, 5xx).
			// If so, parse it into readable form. This follows the pattern of
			// CloudErrorUtil.asCoreException(...)
			CoreException rethrow = e;
			if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException hsee = (HttpServerErrorException) e.getCause();

				// Wrap the status inner exception into a new exception to allow the UI to properly display the full error message
				ServiceCreationFailedException scfe = new ServiceCreationFailedException(hsee.getStatusCode()+" "+hsee.getStatusCode().getReasonPhrase(), hsee);
				Status newStatus = new Status(e.getStatus().getSeverity(), e.getStatus().getPlugin(), e.getStatus().getCode(),
						hsee.getMessage(), scfe);

				rethrow = new CoreException(newStatus);
			}

			logNonModuleError(rethrow);
			throw rethrow;
		}
	}
	
	protected boolean shouldLogException(CoreException e) {
		return !CloudErrorUtil.isNotFoundException(e);
	}

	@SuppressWarnings("serial")
	public static class ServiceCreationFailedException extends Exception {

		public ServiceCreationFailedException(String message, Exception cause) {
			super(message, cause);
		}

	}
}
