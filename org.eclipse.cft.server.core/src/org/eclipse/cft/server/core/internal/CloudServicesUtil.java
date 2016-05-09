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
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.cft.server.core.CFServiceOffering;
import org.eclipse.cft.server.core.CFServicePlan;

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

	public static List<CFServiceOffering> asServiceOfferings(List<CloudServiceOffering> offerings) {
		List<CFServiceOffering> cfOfferings = new ArrayList<CFServiceOffering>();
		if (offerings != null) {
			for (CloudServiceOffering offering : offerings) {
				CFServiceOffering cfOffering = new CFServiceOffering(offering.getLabel(), offering.getVersion(),
						offering.getDescription(), offering.isActive(), offering.isBindable(), offering.getUrl(),
						offering.getInfoUrl(), offering.getUniqueId(), offering.getExtra(),
						offering.getDocumentationUrl(), offering.getProvider());
				addServiceOfferingPlans(offering, cfOffering);
				cfOfferings.add(cfOffering);
			}
		}

		return cfOfferings;
	}

	private static void addServiceOfferingPlans(CloudServiceOffering offering, CFServiceOffering cfOffering) {
		List<CloudServicePlan> offeringPlans = offering.getCloudServicePlans();
		List<CFServicePlan> cfOfferingPlans = new ArrayList<CFServicePlan>();
		if (offeringPlans != null) {
			for (CloudServicePlan plan : offeringPlans) {
				CFServicePlan cfPlan = new CFServicePlan(plan.getName(), plan.getDescription(), plan.isFree(),
						plan.isPublic(), plan.getExtra(), plan.getUniqueId());
				cfPlan.setServiceOffering(cfOffering);
				cfOfferingPlans.add(cfPlan);
			}
		}
		cfOffering.setServicePlans(cfOfferingPlans);
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
