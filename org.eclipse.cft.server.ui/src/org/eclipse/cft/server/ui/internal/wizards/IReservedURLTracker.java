/*******************************************************************************
 * Copyright (c) 2015, 2016 Pivotal Software Inc. and IBM Corporation. 
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance 
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
package org.eclipse.cft.server.ui.internal.wizards;

import org.eclipse.cft.server.core.internal.CloudApplicationURL;

/**
 * Tracker to add/reserve a Cloud route, check if a Cloud route is taken, and delete an
 * existing Cloud route.  This interface is intended to be implemented by CF wizards to track
 * the reserved URLs (multiple URLs can be reserved in one wizard session), and provide the
 * additional host-name taken validation on the deployment URL.
 * 
 * @since CF 1.8.4
 *
 */
public interface IReservedURLTracker {
   	
	/**
	 * Check if the URL has already been reserved
	 * @param appUrl
	 * @return
	 */
	public boolean isReserved(CloudApplicationURL appUrl);
		
	/** Add the URL to the reserved list; you may also specify whether or not the url was created by the wizard. 
	 * If it was not created by the wizard (it is an existing reserved route) then it will not be deleted on wizard cancel. */
	public void addToReserved(CloudApplicationURL appUrl, boolean isUrlCreatedByWizard);
	
	/**
	 * Remove the URL from the reserved list
	 * @param appUrl
	 */
	public void removeFromReserved(CloudApplicationURL appUrl);
	
	/**
	 * Validate the Cloud URL to see if it is taken
	 * @param appUrl
	 * @return
	 */
	 public HostnameValidationResult validateURL(CloudApplicationURL appUrl);
}
