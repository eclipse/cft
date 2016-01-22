/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation 
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
 ********************************************************************************/

package org.eclipse.cft.server.ui.internal.wizards;

import org.eclipse.core.runtime.IStatus;

/** Returned by IReservedURLTracker.validateURL(...) to indicate whether validation succeeded, and whether or not
 * a route was created. */
public class HostnameValidationResult {
	
	/** Status of the validation */
	private final IStatus status;

	/**
	 * Whether or not the route was created during the validation. It will not
	 * be created if it was already reserved in the org/space.
	 */
	private final boolean isRouteCreated;

	public HostnameValidationResult(IStatus status, boolean isRouteCreated) {
		this.status = status;
		this.isRouteCreated = isRouteCreated;
	}

	public IStatus getStatus() {
		return status;
	}

	public boolean isRouteCreated() {
		return isRouteCreated;
	}
}
