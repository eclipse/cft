/*******************************************************************************
 * Copyright (c) 2012, 2016 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.ui.internal.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.jface.viewers.IStructuredSelection;

public class ServicesHandler {

	private List<String> services;

	private final List<CFServiceInstance> cloudServices;

	public ServicesHandler(IStructuredSelection selection) {
		Object[] objects = selection.toArray();
		cloudServices = new ArrayList<CFServiceInstance>();

		for (Object obj : objects) {
			if (obj instanceof CFServiceInstance) {
				cloudServices.add((CFServiceInstance) obj);
			}

		}
	}

	public List<String> getServiceNames() {

		if (services == null) {
			services = new ArrayList<String>();

			for (CFServiceInstance service : cloudServices) {
				services.add(service.getName());
			}
		}

		return services;
	}

	public String toString() {
		StringBuilder serviceNames = new StringBuilder();
		for (String service : getServiceNames()) {
			if (serviceNames.length() > 0) {
				serviceNames.append(", "); //$NON-NLS-1$
			}
			serviceNames.append(service);
		}
		return serviceNames.toString();
	}
}
