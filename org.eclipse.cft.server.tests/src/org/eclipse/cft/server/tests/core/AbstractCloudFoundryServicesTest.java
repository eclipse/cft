/*******************************************************************************
 * Copyright (c) 2012, 2016 Pivotal Software, Inc.
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
import org.cloudfoundry.client.lib.domain.CloudService;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

public class AbstractCloudFoundryServicesTest extends AbstractAsynchCloudTest {

	protected void deleteService(CloudService service) throws CoreException {
		harness.deleteService(service);
	}

	protected CloudService createCloudService(String name, String label, String plan) throws CoreException {
		CloudService toCreate = getCloudServiceToCreate(name, label, plan);

		if (toCreate == null) {
			throw CloudErrorUtil.toCoreException(
					"Unable to create service : " + label + ". Service does not exist in the Cloud space.");
		}

		createService(toCreate);
		return toCreate;
	}

	protected ICloudFoundryOperation getBindServiceOp(CloudFoundryApplicationModule appModule, CloudService service)
			throws Exception {
		List<String> servicesToBind = new ArrayList<String>();
		servicesToBind.add(service.getName());

		return serverBehavior.operations().bindServices(appModule, servicesToBind);
	}

	protected ICloudFoundryOperation getUnbindServiceOp(CloudFoundryApplicationModule appModule, CloudService service)
			throws Exception {
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

	protected void createService(CloudService service) throws CoreException {
		serverBehavior.operations().createServices(new CloudService[] { service }).run(new NullProgressMonitor());
	}

	protected void assertServiceExists(CloudService expectedService) throws Exception {
		String expectedServicename = expectedService.getName();
		CloudService foundService = getCloudService(expectedServicename);
		assertNotNull(foundService);
		assertServiceEquals(expectedService, foundService);
	}

	protected void assertServiceExists(String serviceName) throws Exception {
		CloudService foundService = getCloudService(serviceName);
		assertNotNull(foundService);
	}

	protected CloudService getCloudService(String serviceName) throws CoreException {

		List<CloudService> services = serverBehavior.getServices(new NullProgressMonitor());
		CloudService foundService = null;
		if (services != null) {
			for (CloudService service : services) {
				if (serviceName.equals(service.getName())) {
					foundService = service;
					break;
				}
			}
		}
		return foundService;
	}

	protected void assertServiceNotExist(String expectedServicename) throws Exception {

		CloudService foundService = getCloudService(expectedServicename);

		assertNull(foundService);
	}

	protected void assertServiceEquals(CloudService expectedService, CloudService actualService) throws Exception {
		assertEquals(actualService.getName(), expectedService.getName());
		assertEquals(actualService.getLabel(), expectedService.getLabel());
	}
}
