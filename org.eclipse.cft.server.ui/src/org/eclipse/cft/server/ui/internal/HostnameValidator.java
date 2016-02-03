/*******************************************************************************
 * Copyright (c) 2015, 2016 Pivotal Software Inc. and IBM Corporation 
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
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudRoute;
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

	private final CloudApplicationURL appUrl;
	private final CloudFoundryServer server;
	
	private IStatus status;

	// Allow override of the default message for the host name taken problem (for initialization of the wizard)
	private String message = null;
	
	/** True if a route was created during validation; a route will not be created if it already exists. */
	private boolean routeCreated = false;

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
		
		boolean isRouteOurs = false; // Whether we (space/org) own the route (true if either in use or not in use)
		boolean isInUse = false; // In use by another application or user
		
		try {
			
			List<CloudRoute> routes = server.getBehaviour().getRoutes(appUrl.getDomain(), monitor);
			
			for(CloudRoute cr : routes) {
				// If we own the route...
				if(cr.getHost().equalsIgnoreCase(appUrl.getSubdomain())) {
					isRouteOurs = true;
					isInUse = cr.inUse();
					
					if(!isInUse)  {
						// We own it, but it's not in use by us, so we are ok.
						status = Status.OK_STATUS;
						this.routeCreated = false;
						return;
					}

					
					break;
				}
			}
						
			if(!isRouteOurs) {
				// If we couldn't find the route in the route list, so attempt to reserve it
				boolean routeReserved = server.getBehaviour().reserveRouteIfAvailable(appUrl.getSubdomain(), appUrl.getDomain(), monitor);
				
				if(routeReserved) {
					// We successfully reserved a route that we did not previously own
					this.routeCreated = true;
					isInUse = false;
				} else {
					// We could not reserve the route as some other user owns it
					isInUse = true;
					this.routeCreated = false;
				}				
			}
			
			if(isInUse) {
				// We found it in the route list (and it was in use), or we couldn't reserve it, so it is taken.
				status = new Status(IStatus.ERROR, status.getPlugin(), message != null ? message : Messages.bind(
						Messages.CloudApplicationUrlPart_ERROR_HOSTNAME_TAKEN, appUrl.getUrl()));				
			}
			
		} catch (CoreException ce) {
			// For other errors (like connection time out), give a more user-friendly message than the one provided
			status = new Status(IStatus.ERROR, status.getPlugin(), Messages.CloudApplicationUrlPart_ERROR_UNABLE_TO_CHECK_HOSTNAME);
			// Log the error at least.
			CloudFoundryPlugin.logError(Messages.CloudApplicationUrlPart_ERROR_UNABLE_TO_CHECK_HOSTNAME, ce);
		} finally {
			monitor.done();
		}			
	}

	/** Returns true if a route was created during validation; a route will not 
	 * be created if it already exists. */
	public boolean isRouteCreated() {
		return routeCreated;
	}
	
}
