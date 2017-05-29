/*******************************************************************************
 * Copyright (c) 2013, 2017 Pivotal Software, Inc. and others 
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
 *     IBM - change to use map to store info
 ********************************************************************************/
package org.eclipse.cft.server.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import org.eclipse.cft.server.core.internal.application.ManifestConstants;

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

	private List<String> uris;

	private HashMap<Object, Object> deploymentInfoMap = new HashMap<Object, Object>();

	public ApplicationDeploymentInfo(String appName) {
		setDeploymentName(appName);
	}

	public void setEnvVariables(List<EnvironmentVariable> envVars) {
		List<EnvironmentVariable> curEnvVars = getEnvVariables() ;
		if (curEnvVars == envVars) {
			// Notify Observers only and no need to update the actual value since both objects are the same.
			setChanged();
			notifyObservers(envVars);
		} else {
			// Only clear and add the entries if it is not the same envVars object as the original.
			curEnvVars.clear();
			if (envVars != null) {
				curEnvVars.addAll(envVars);
				// Notify Observers
				setChanged();
				notifyObservers(envVars);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public List<EnvironmentVariable> getEnvVariables() {
		Object curEnvVars = deploymentInfoMap.get(ManifestConstants.ENV_PROP);
		if (curEnvVars == null) {
			// Initialize the variables.
			curEnvVars = new ArrayList<EnvironmentVariable>();
			deploymentInfoMap.put(ManifestConstants.ENV_PROP, curEnvVars);
		}
		return (List<EnvironmentVariable>)curEnvVars;
	}

	public int getInstances() {
		return getIntValue(ManifestConstants.INSTANCES_PROP);
	}

	public void setInstances(int instances) {
		deploymentInfoMap.put(ManifestConstants.INSTANCES_PROP, Integer.valueOf(instances));
	}

	public String getBuildpack() {
		return (String)deploymentInfoMap.get(ManifestConstants.BUILDPACK_PROP);
	}

	public void setBuildpack(String buildpack) {
		setStringValue(ManifestConstants.BUILDPACK_PROP, buildpack);
	}
	
	public String getDeploymentName() {
		return (String)deploymentInfoMap.get(ManifestConstants.NAME_PROP);
	}
	
	public String getCommand() {
		return (String)deploymentInfoMap.get(ManifestConstants.COMMAND_PROP);
	}
	
	public String getHealthCheckType() {
		return (String)deploymentInfoMap.get(ManifestConstants.HEALTH_CHECK_TYPE);
	}
	
	public String getHealthCheckHttpEndpoint() {
		return (String)deploymentInfoMap.get(ManifestConstants.HEALTH_CHECK_HTTP_ENDPOINT);
	}

	public void setDeploymentName(String name) {
		setStringValue(ManifestConstants.NAME_PROP, name);
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

	@SuppressWarnings("unchecked")
	public List<CFServiceInstance> getServices() {
		return (List<CFServiceInstance>)deploymentInfoMap.get(ManifestConstants.SERVICES_PROP);
	}
	
	public boolean hasProperty(String property) {
		return deploymentInfoMap.containsKey(property);
	}

	/**
	 * 
	 * @return never null, may be empty.
	 */
	public List<String> asServiceBindingList() {
		List<String> bindingList = new ArrayList<String>();
		List<CFServiceInstance> services = getServices();

		if (services != null && !services.isEmpty()) {
			for (CFServiceInstance service : services) {
				bindingList.add(service.getName());
			}
		}
		return bindingList;
	}

	public void setServices(List<CFServiceInstance> services) {
		if (services == null) {
			deploymentInfoMap.remove(ManifestConstants.SERVICES_PROP);
		} else {
			deploymentInfoMap.put(ManifestConstants.SERVICES_PROP, services);
		}
	}

	public int getMemory() {
		return getIntValue(ManifestConstants.MEMORY_PROP);
	}

	public void setMemory(int memory) {
		deploymentInfoMap.put(ManifestConstants.MEMORY_PROP, Integer.valueOf(memory));
	}

	public String getArchive() {
		return (String)deploymentInfoMap.get(ManifestConstants.PATH_PROP);
	}

	public void setArchive(String archive) {
		setStringValue(ManifestConstants.PATH_PROP, archive);
	}
	
	public Integer getDiskQuota() {
		return (Integer)deploymentInfoMap.get(ManifestConstants.DISK_QUOTA_PROP);
	}

	public void setDiskQuota(int diskQuota) {
		deploymentInfoMap.put(ManifestConstants.DISK_QUOTA_PROP, Integer.valueOf(diskQuota));
	}
	
	private int getIntValue(String key) {
		Integer curInt = (Integer)deploymentInfoMap.get(key);
		return curInt != null ? curInt.intValue() : 0;		
	}
	
	public String getStack() {
		return (String)deploymentInfoMap.get(ManifestConstants.STACK_PROP);
	}
	
	public void setStack(String curStack) {
		if (curStack == null) {
			deploymentInfoMap.remove(ManifestConstants.STACK_PROP);
		} else {
			deploymentInfoMap.put(ManifestConstants.STACK_PROP, curStack);
		}
	}

	public Integer getTimeout() {
		return (Integer)deploymentInfoMap.get(ManifestConstants.TIMEOUT_PROP);
	}
	
	/**
	 * 
	 * Sets the values of the parameter info, if non-null, into this info. Any
	 * know mutable values (e.g. containers and arrays) are set as copies.
	 */
	@SuppressWarnings("unchecked")
	public void setInfo(ApplicationDeploymentInfo info) {
		if (info == null) {
			return;
		}

		// Preserve all entries in deployment info map even for ones not explicitly tracking by this class.
		this.deploymentInfoMap = (HashMap<Object, Object>)info.deploymentInfoMap.clone();

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
	
	private void setStringValue(String keyStr, String valueStr) {
		if (valueStr == null) {
			deploymentInfoMap.remove(keyStr);
		} else {
			deploymentInfoMap.put(keyStr, valueStr);
		}		
	}

	/**
	 * Copy the deployment info, with any known mutable values set as copies as
	 * well. Therefore, if an info property is a list of values (e.g. list of
	 * bound services), modifying the list in the copy will not affect the list
	 * of values in the original version.
	 * @return non-null copy of this info.
	 */
	@SuppressWarnings("unchecked")
	public ApplicationDeploymentInfo copy() {
		ApplicationDeploymentInfo info = new ApplicationDeploymentInfo(getDeploymentName());
		
		// Preserve all entries in deployment info map even for ones not explicitly tracking by this class.
		info.deploymentInfoMap = (HashMap<Object, Object>)this.deploymentInfoMap.clone();

		if (getServices() != null) {
			info.setServices(new ArrayList<CFServiceInstance>(getServices()));
		}

		if (getUris() != null) {
			info.setUris(new ArrayList<String>(getUris()));
		}

		if (getEnvVariables() != null) {
			ArrayList<EnvironmentVariable> curEnvVaris = new ArrayList<EnvironmentVariable>();
			curEnvVaris.addAll(getEnvVariables());
			info.setEnvVariables(curEnvVaris);
		}

		return info;
	}
	
	/**
	 * Get the list of info that are not explicitly handled or know by the tools.
	 * @return
	 */
	public HashMap<Object, Object> getUnknownInfo() {
		@SuppressWarnings("unchecked")
		HashMap<Object, Object> curDeploymentInfoMap = (HashMap<Object, Object>)this.deploymentInfoMap.clone();
		curDeploymentInfoMap.remove(ManifestConstants.APPLICATIONS_PROP);
		curDeploymentInfoMap.remove(ManifestConstants.BUILDPACK_PROP);
		curDeploymentInfoMap.remove(ManifestConstants.DISK_QUOTA_PROP);
		curDeploymentInfoMap.remove(ManifestConstants.DOMAIN_PROP);
		curDeploymentInfoMap.remove(ManifestConstants.ENV_PROP);
		curDeploymentInfoMap.remove(ManifestConstants.INSTANCES_PROP);
//		curDeploymentInfoMap.remove(ManifestConstants.LABEL_PROP);
		curDeploymentInfoMap.remove(ManifestConstants.MEMORY_PROP);
		curDeploymentInfoMap.remove(ManifestConstants.NAME_PROP);
		curDeploymentInfoMap.remove(ManifestConstants.PATH_PROP);
//		curDeploymentInfoMap.remove(ManifestConstants.PLAN_PROP);
//		curDeploymentInfoMap.remove(ManifestConstants.PROVIDER_PROP);
		curDeploymentInfoMap.remove(ManifestConstants.SERVICES_PROP);
		curDeploymentInfoMap.remove(ManifestConstants.SUB_DOMAIN_PROP);
//		curDeploymentInfoMap.remove(ManifestConstants.VERSION_PROP);

		return curDeploymentInfoMap;
	}
	
	/**
	 * Get the list of info that are not explicitly handled or know by the tools.
	 * @return
	 */
	public void addAllToDeploymentMap(Map<?, ?> curDeploymentInfoMap) {
		this.deploymentInfoMap.putAll(curDeploymentInfoMap);
	}
}
