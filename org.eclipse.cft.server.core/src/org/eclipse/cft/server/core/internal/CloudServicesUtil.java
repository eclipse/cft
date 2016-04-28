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
package org.eclipse.cft.server.core.internal;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.eclipse.cft.server.core.internal.client.CFServiceInstance;

public class CloudServicesUtil {

	public static CFServiceInstance asServiceInstance(CloudService cloudService) {
		CFServiceInstance instance = new CFServiceInstance(cloudService.getName());
		instance.setPlan(cloudService.getPlan());
		instance.setService(cloudService.getLabel());
		instance.setVersion(cloudService.getVersion());

		return instance;
	}

	public static List<CFServiceInstance> asServiceInstances(List<CloudService> cloudServices) {
		List<CFServiceInstance> serviceInstances = new ArrayList<CFServiceInstance>();
		if (cloudServices != null) {
			for (CloudService service : cloudServices) {
				serviceInstances.add(asServiceInstance(service));
			}
		}

		return serviceInstances;
	}

	public static CloudService asLegacyV1Service(CFServiceInstance serviceInstance) {
		CloudService service = new CloudService();

		CloudEntity.Meta meta = CloudEntity.Meta.defaultMeta();
		service.setMeta(meta);
		service.setLabel(serviceInstance.getService());
		service.setName(serviceInstance.getName());
		service.setPlan(serviceInstance.getPlan());
		service.setVersion(serviceInstance.getVersion());

		return service;
	}
}
