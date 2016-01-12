/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License�); you may not use this file except in compliance 
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 *  Contributors:
 *     IBM - initial API and implementation - Bug 485697 - Implement 
 *           host name taken check in CF wizards
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.cft.server.core.internal.CloudApplicationURL;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * Validator to check if the hostname is already taken in specified domain
 * 
 */
public class HostnameValidator implements IRunnableWithProgress {

	private CloudApplicationURL appUrl;
	private IStatus status;
	private CloudFoundryServer server;
	// Allow override of the default message for the host name taken problem (for initialization of the wizard)
	private String message = null;

	public HostnameValidator(CloudApplicationURL appUrl, CloudFoundryServer server) {
		this(appUrl, server, null);
	}
	
	public HostnameValidator(CloudApplicationURL appUrl, CloudFoundryServer server, String message) {
		this.appUrl = appUrl;
		this.server = server;
		this.message = message;
		this.status = Status.OK_STATUS;
	}
	
	public IStatus getStatus() {
		return status;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		monitor.beginTask(Messages.CloudApplicationUrlPart_HOST_CHECK_JOB_DISPLAY_INFO, IProgressMonitor.UNKNOWN);
		try {
			server.getBehaviour().checkHostTaken(appUrl.getSubdomain(), appUrl.getDomain(), false, monitor);
		}
		catch (CoreException ce) {
			status = ce.getStatus();
			String errorMessage = ce.getMessage();
			// host name taken is potentially one cause of the CoreException
			// Look for it specifically, and change the error message so it's more user-friendly.
			// Message from java client is: "Client error - The host is taken".
			// Replace it with ours that is more meaningful.
			if (errorMessage != null && errorMessage.contains("Client error - The host is taken")) { // $NON-NLS-N$
				status = new Status(IStatus.ERROR, status.getPlugin(), message != null ? message : Messages.bind(
						Messages.CloudApplicationUrlPart_ERROR_HOSTNAME_TAKEN, appUrl.getUrl()));
			} else {
				// For other errors (like connection time out), give a more user-friendly message than the one provided
				status = new Status(IStatus.ERROR, status.getPlugin(), Messages.CloudApplicationUrlPart_ERROR_UNABLE_TO_CHECK_HOSTNAME);
				// Log the error at least.
				CloudFoundryPlugin.logError(Messages.CloudApplicationUrlPart_ERROR_UNABLE_TO_CHECK_HOSTNAME, ce);
			}
		}
		finally {
			monitor.done();
		}			
	}


}
