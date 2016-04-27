/*******************************************************************************
 * Copyright (c) 2013, 2016 Pivotal Software, Inc. and others 
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
package org.eclipse.cft.server.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.eclipse.cft.server.core.internal.application.EnvironmentVariable;

/**
 * Describes the application that is to be pushed to a CF server, or already
 * exists in a server.
 * <p/>
 * This is the primary model of an application's metadata, and includes the
 * application's name, staging, URIs, and list of bound services.Note that
 * properties that are NOT part of an application deployment manifest (e.g. that
 * are transient and only applicable when an operation is being performed on the
 * application, like selecting its deployment mode) should not be defined here).
 */
public class ApplicationDeploymentInfo extends Observable {


	private List<EnvironmentVariable> envVars = new ArrayList<EnvironmentVariable>();

	private int instances;

	private String name;

	private List<String> uris;

	private List<CloudService> services;

	private int memory;

	private String archive;

	private String buildpack;

	public ApplicationDeploymentInfo(String appName) {
		setDeploymentName(appName);
	}

	public void setEnvVariables(List<EnvironmentVariable> envVars) {
		this.envVars.clear();
		if (envVars != null) {
			this.envVars.addAll(envVars);
			// Notify Observers
			setChanged();
			notifyObservers(envVars);
		}
	}

	public List<EnvironmentVariable> getEnvVariables() {
		return new ArrayList<EnvironmentVariable>(envVars);
	}

	public int getInstances() {
		return instances;
	}

	public void setInstances(int instances) {
		this.instances = instances;
	}

	public String getBuildpack() {
		return buildpack;
	}

	public void setBuildpack(String buildpack) {
		this.buildpack = buildpack;
	}

	public String getDeploymentName() {
		return name;
	}

	public void setDeploymentName(String name) {
		this.name = name;
		// Notify Observers
		setChanged();
		notifyObservers(name);
	}

	public void setUris(List<String> uris) {
		this.uris = uris;
	}

	public List<String> getUris() {
		return uris;
	}

	public List<CloudService> getServices() {
		return services;
	}

	/**
	 * 
	 * @return never null, may be empty.
	 */
	public List<String> asServiceBindingList() {
		List<String> bindingList = new ArrayList<String>();

		if (services != null && !services.isEmpty()) {
			for (CloudService service : services) {
				bindingList.add(service.getName());
			}
		}
		return bindingList;
	}

	public void setServices(List<CloudService> services) {
		this.services = services;
	}

	public int getMemory() {
		return memory;
	}

	public void setMemory(int memory) {
		this.memory = memory;
	}

	public String getArchive() {
		return this.archive;
	}

	public void setArchive(String archive) {
		this.archive = archive;
	}

	/**
	 * 
	 * Sets the values of the parameter info, if non-null, into this info. Any
	 * know mutable values (e.g. containers and arrays) are set as copies.
	 */
	public void setInfo(ApplicationDeploymentInfo info) {
		if (info == null) {
			return;
		}
		setDeploymentName(info.getDeploymentName());
		setMemory(info.getMemory());
		setBuildpack(info.getBuildpack());
		setInstances(info.getInstances());
		setArchive(info.getArchive());

		if (info.getServices() != null) {
			setServices(new ArrayList<CloudService>(info.getServices()));
		}
		else {
			setServices(null);
		}

		if (info.getUris() != null) {
			setUris(new ArrayList<String>(info.getUris()));
		}
		else {
			setUris(null);
		}

		if (info.getEnvVariables() != null) {
			setEnvVariables(new ArrayList<EnvironmentVariable>(info.getEnvVariables()));
		}
		else {
			setEnvVariables(null);
		}
	}

	/**
	 * Copy the deployment info, with any known mutable values set as copies as
	 * well. Therefore, if an info property is a list of values (e.g. list of
	 * bound services), modifying the list in the copy will not affect the list
	 * of values in the original version.
	 * @return non-null copy of this info.
	 */
	public ApplicationDeploymentInfo copy() {
		ApplicationDeploymentInfo info = new ApplicationDeploymentInfo(getDeploymentName());

		info.setMemory(getMemory());
		info.setBuildpack(getBuildpack());
		info.setInstances(getInstances());
		info.setArchive(getArchive());

		if (getServices() != null) {
			info.setServices(new ArrayList<CloudService>(getServices()));
		}

		if (getUris() != null) {
			info.setUris(new ArrayList<String>(getUris()));
		}

		if (getEnvVariables() != null) {
			info.setEnvVariables(new ArrayList<EnvironmentVariable>(getEnvVariables()));
		}

		return info;
	}
}
