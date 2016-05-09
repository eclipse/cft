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

/**
 * Describes the application that is to be pushed to a CF server, or already
 * exists in a server.
 * <p/>
 * This is the primary model of an application, and includes among other things,
 * the application's name, memory, instances, buildpack, URLs, and list of bound
 * services. This models the application information as found in Cloud Foundry.
 * 
 * <p/>
 * The info only describes an application, but does not define the contents of
 * an application.
 * 
 * @see CFApplicationArchive for application contents
 */
public class ApplicationDeploymentInfo extends Observable {

	private List<EnvironmentVariable> envVars = new ArrayList<EnvironmentVariable>();

	private int instances;

	private String name;

	private List<String> uris;

	private List<CFServiceInstance> services;

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

	public List<CFServiceInstance> getServices() {
		return services;
	}

	/**
	 * 
	 * @return never null, may be empty.
	 */
	public List<String> asServiceBindingList() {
		List<String> bindingList = new ArrayList<String>();

		if (services != null && !services.isEmpty()) {
			for (CFServiceInstance service : services) {
				bindingList.add(service.getName());
			}
		}
		return bindingList;
	}

	public void setServices(List<CFServiceInstance> services) {
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
			setServices(new ArrayList<CFServiceInstance>(info.getServices()));
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
			info.setServices(new ArrayList<CFServiceInstance>(getServices()));
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
