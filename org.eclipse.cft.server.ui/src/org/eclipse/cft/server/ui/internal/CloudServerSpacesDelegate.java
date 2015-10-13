/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
 *     
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal;

import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.spaces.CloudFoundrySpace;
import org.eclipse.cft.server.core.internal.spaces.CloudSpacesDescriptor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.operation.IRunnableContext;

/**
 * Handles cloud space descriptor updates and also sets a cloud space in a given
 * cloud server, including a default cloud space.
 * <p/>
 * Note that cloud server changes are not saved. It is up to the invoker to
 * decide when to save changes to the cloud server.
 *
 */
public class CloudServerSpacesDelegate extends CloudSpacesDelegate {

	public CloudServerSpacesDelegate(CloudFoundryServer cloudServer) {
		super(cloudServer);
	}

	protected CloudSpacesDescriptor internalUpdateDescriptor(String urlText, String userName, String password,
			boolean selfSigned, IRunnableContext context) throws CoreException {
		CloudSpacesDescriptor spacesDescriptor = super.internalUpdateDescriptor(urlText, userName, password,
				selfSigned, context);
		internalDescriptorChanged();

		return spacesDescriptor;

	}

	/**
	 * Invoked if the descriptor containing list of orgs and spaces has changed.
	 * If available, a default space will be set in the server
	 */
	protected void internalDescriptorChanged() throws CoreException {
		// Set a default space, if one is available
		if (getCurrentSpacesDescriptor() != null) {
			CloudSpace defaultCloudSpace = getSpaceWithNoServerInstance();
			setSelectedSpace(defaultCloudSpace);
		}
		else {

			// clear the selected space if there is no available spaces
			// descriptor
			setSelectedSpace(null);
		}
	}

	public void setSelectedSpace(CloudSpace selectedCloudSpace) {
		if (hasSpaceChanged(selectedCloudSpace)) {
			// Only set space if a change has occurred. 
			getCloudServer().setSpace(selectedCloudSpace);
		}

	}

	protected boolean hasSpaceChanged(CloudSpace selectedCloudSpace) {
		CloudFoundrySpace existingSpace = getCloudServer().getCloudFoundrySpace();
		return !matchesSpace(selectedCloudSpace, existingSpace);
	}

	@Override
	public boolean hasSpace() {
		return getCloudServer().hasCloudSpace();
	}

	public CloudSpace getCurrentCloudSpace() {
		return getCloudServer().getCloudFoundrySpace() != null ? getCloudServer().getCloudFoundrySpace().getSpace()
				: null;
	}

}
