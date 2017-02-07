/*******************************************************************************
 * Copyright (c) 2012, 2017 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.tests.core;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.cft.server.core.CFServiceOffering;
import org.eclipse.cft.server.core.CFServicePlan;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

public class AbstractCloudFoundryServicesTest extends AbstractAsynchCloudTest {

	protected ICloudFoundryOperation getBindServiceOp(CloudFoundryApplicationModule appModule,
			CFServiceInstance service) throws Exception {
		List<String> servicesToBind = new ArrayList<String>();
		servicesToBind.add(service.getName());

		return serverBehavior.operations().bindServices(appModule, servicesToBind);
	}

	protected ICloudFoundryOperation getUnbindServiceOp(CloudFoundryApplicationModule appModule,
			CFServiceInstance service) throws Exception {
		CloudApplication updatedApplication = getUpdatedApplication(appModule.getDeployedApplicationName());
		List<String> boundServices = updatedApplication.getServices();
		List<String> servicesToUpdate = new ArrayList<String>();

		// Must iterate rather than passing to constructor or using
		// addAll, as some
		// of the entries in existing services may be null
		for (String existingService : boundServices) {
			if (existingService != null) {
				servicesToUpdate.add(existingService);
			}
		}

		if (servicesToUpdate.contains(service.getName())) {
			servicesToUpdate.remove(service.getName());
		}
		return serverBehavior.operations().bindServices(appModule, servicesToUpdate);
	}

	protected void assertServiceBound(String serviceName, CloudApplication application) throws Exception {
		CloudApplication updatedApplication = getUpdatedApplication(application.getName());
		assertNotNull(updatedApplication);
		String foundService = findService(serviceName, updatedApplication);
		assertNotNull(foundService);
	}

	protected void assertServiceNotBound(String serviceName, CloudApplication application) throws Exception {
		CloudApplication updatedApplication = getUpdatedApplication(application.getName());
		assertNotNull(updatedApplication);
		String foundService = findService(serviceName, updatedApplication);
		assertNull(foundService);
	}

	protected String findService(String serviceName, CloudApplication app) {
		List<String> boundServices = app.getServices();
		String foundService = null;
		for (String boundService : boundServices) {
			if (serviceName.equals(boundService)) {
				foundService = boundService;
				break;
			}
		}
		return foundService;
	}

	protected CFServiceInstance getExistingCloudService(String serviceName) throws CoreException {

		List<CFServiceInstance> services = serverBehavior.getServices(new NullProgressMonitor());
		CFServiceInstance foundService = null;
		if (services != null) {
			for (CFServiceInstance service : services) {
				if (serviceName.equals(service.getName())) {
					foundService = service;
					break;
				}
			}
		}
		return foundService;
	}

	protected void assertServiceEquals(CFServiceInstance expectedService, CFServiceInstance actualService)
			throws Exception {
		assertEquals(actualService.getName(), expectedService.getName());
		assertEquals(actualService.getService(), expectedService.getService());
	}

	protected CFServiceOffering getServiceConfiguration(String vendor) throws CoreException {
		List<CFServiceOffering> serviceConfigurations = serverBehavior.getServiceOfferings(new NullProgressMonitor());
		if (serviceConfigurations != null) {
			for (CFServiceOffering serviceConfiguration : serviceConfigurations) {
				if (vendor.equals(serviceConfiguration.getName())) {
					return serviceConfiguration;
				}
			}
		}
		return null;
	}

	protected CFServiceInstance asCFServiceInstance(String name, String plan, String type) throws CoreException {

		CFServiceOffering serviceConfiguration = getServiceConfiguration(type);
		if (serviceConfiguration != null) {

			CFServiceInstance service = new CFServiceInstance(name);
			service.setService(type);
			service.setVersion(serviceConfiguration.getVersion());

			boolean planExists = false;

			List<CFServicePlan> plans = serviceConfiguration.getServicePlans();

			for (CFServicePlan pln : plans) {
				if (plan.equals(pln.getName())) {
					planExists = true;
					break;
				}
			}

			if (!planExists) {
				throw CloudErrorUtil.toCoreException("No plan: " + plan + " found for service :" + type);
			}
			service.setPlan(plan);

			return service;
		}
		return null;
	}
}
