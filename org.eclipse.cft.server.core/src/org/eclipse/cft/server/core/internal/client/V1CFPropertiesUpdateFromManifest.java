/*******************************************************************************
 * Copyright (c) 2017 Pivotal Software, Inc. and others 
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
package org.eclipse.cft.server.core.internal.client;

import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;
import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.application.ManifestParser;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class V1CFPropertiesUpdateFromManifest {

	public static List<String> v1UpdateFromManifest(CloudFoundryOperations client, CloudFoundryServer cloudServer,
			CloudFoundryApplicationModule appModule, CloudApplication currentApplication, IProgressMonitor progMon)
			throws CoreException {
		String appName = currentApplication.getName();
		ManifestParser parser = new ManifestParser(appModule, cloudServer);
		CFPropertiesUpdateFromManifest updater = new CFPropertiesUpdateFromManifest(parser);
		if (parser.hasManifest()) {
			ApplicationDeploymentInfo wc = updater.load(progMon);
			if (wc != null) {
				Staging staging = currentApplication.getStaging();
				if (staging == null) {
					staging = new Staging();
				}
				return updater
						.memory(currentApplication.getMemory(),
								(memory, monitor) -> client.updateApplicationMemory(appName, memory))
						.instances(currentApplication.getInstances(),
								(instances, monitor) -> client.updateApplicationInstances(appName, instances))
						.diskQuota(currentApplication.getDiskQuota(),
								(diskQuota, monitor) -> client.updateApplicationDiskQuota(appName, diskQuota))
						.v1Staging(staging, (stg, monitor) -> client.updateApplicationStaging(appName, stg))
						.update(progMon);
			}
		}
		return null;
	}
}