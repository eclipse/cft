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
 *     IBM - bug fix
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.application;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.cft.server.core.EnvironmentVariable;
import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudApplicationURL;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryProjectUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.Logger;
import org.eclipse.cft.server.core.internal.application.ManifestConstants;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.DeploymentInfoWorkingCopy;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;

public class ManifestParser {

	private final String relativePath;

	private final CloudFoundryApplicationModule appModule;

	private final CloudFoundryServer cloudServer;

	public static final String DEFAULT = "manifest.yml"; //$NON-NLS-1$

	public ManifestParser(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		this(DEFAULT, appModule, cloudServer);
	}

	public ManifestParser(String relativePath, CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer) {
		Assert.isNotNull(relativePath);
		this.relativePath = relativePath;
		this.appModule = appModule;
		this.cloudServer = cloudServer;
	}

	/**
	 * If the application module has an accessible workspace project, return the
	 * manifest file contained in the project. Otherwise return null. The file
	 * itself may not yet exist, but if returned, it at least means the
	 * workspace project does indeed exist.
	 * @return Manifest file in the workspace project for the application, or
	 * null if the project does not exists or is not accessible.
	 */
	private IFile getFile() {
		IProject project = CloudFoundryProjectUtil.getProject(appModule);
		if (project == null) {
			return null;
		}
		IFile resource = project.getFile(relativePath);

		return resource;
	}

	/**
	 * @return true if the application has an accessible manifest file that
	 * exists. False otherwise, even if the application does have a manifest
	 * file. A false in this case would mean the file is not accessible.
	 */
	public boolean hasManifest() {
		IFile file = getFile();
		return file != null && file.exists();
	}

	/**
	 * @return true if the application has an accessible workspace project where
	 * a manifest file can be written too. False otherwise.
	 */
	public boolean canWriteToManifest() {
		return CloudFoundryProjectUtil.getProject(appModule) != null;
	}

	/**
	 * 
	 * @param applicationName name of application to lookup in the manifest
	 * file.
	 * @param propertyName String value property to retrieve from manifest for
	 * given application entry.
	 * @return Value of property, or null if not found, or entry for application
	 * in manifest does not exist.
	 */
	public String getApplicationProperty(String applicationName, String propertyName) {
		try {
			Map<?, ?> map = getApplication(applicationName);
			if (map != null) {
				return getStringValue(map, propertyName);
			}
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return null;
	}

	/**
	 * 
	 * @param containerMap
	 * @param propertyName
	 * @return map of values for the given property name, or null if it cannot
	 * be resolved
	 */
	protected Map<?, ?> getContainingPropertiesMap(Map<?, ?> containerMap, String propertyName) {
		if (containerMap == null || propertyName == null) {
			return null;
		}
		Object yamlElementObj = containerMap.get(propertyName);

		if (yamlElementObj instanceof Map<?, ?>) {
			return (Map<Object, Object>) yamlElementObj;
		}
		else {
			return null;
		}
	}

	protected String getStringValue(Map<?, ?> containingMap, String propertyName) {

		if (containingMap == null) {
			return null;
		}

		Object valObj = containingMap.get(propertyName);

		if (valObj instanceof String) {
			return (String) valObj;
		}
		return null;
	}

	protected Integer getIntegerValue(Map<?, ?> containingMap, String propertyName) {

		if (containingMap == null) {
			return null;
		}

		Object valObj = containingMap.get(propertyName);

		if (valObj instanceof Integer) {
			return (Integer) valObj;
		}
		return null;
	}

	protected Map<?, ?> getApplication(String applicationName) throws CoreException {
		Map<Object, Object> results = parseManifestFromFile();

		if (results == null) {
			return null;
		}

		Object applicationsObj = results.get(ManifestConstants.APPLICATIONS_PROP);
		if (!(applicationsObj instanceof List<?>)) {
			throw CloudErrorUtil.toCoreException("Expected a top-level list of applications in: " //$NON-NLS-1$
					+ relativePath
					+ ". Unable to continue parsing manifest values. No manifest values will be loaded into the application deployment info."); //$NON-NLS-1$
		}

		List<?> applicationsList = (List<?>) applicationsObj;

		// Use only the first application entry
		if (applicationsList.isEmpty()) {
			return null;
		}

		Map<?, ?> application = null;
		String errorMessage = null;
		// If no application name specified, get the first one.
		if (applicationName == null) {
			Object mapObj = applicationsList.get(0);
			application = (mapObj instanceof Map<?, ?>) ? (Map<?, ?>) mapObj : null;
			if (application == null) {
				errorMessage = "Expected a map of application properties in: " //$NON-NLS-1$
						+ relativePath
						+ ". Unable to continue parsing manifest values. No manifest values will be loaded into the application deployment info."; //$NON-NLS-1$

			}
		}
		else {
			for (Object mapObj : applicationsList) {
				if (mapObj instanceof Map<?, ?>) {
					application = (Map<?, ?>) mapObj;
					String appName = getStringValue(application, ManifestConstants.NAME_PROP);
					if (applicationName.equals(appName)) {
						break;
					}
					else {
						application = null;
					}
				}
			}
		}
		
		if (getStringValue(application, ManifestConstants.INHERIT_PROP) != null) {
			Logger.println(Logger.WARNING_LEVEL, this, "load", //$NON-NLS-1$
					"Manifest file entry inherit is not support. The inherit entry will be ignored during deployment: " + relativePath); //$NON-NLS-1$
		}

		if (errorMessage != null) {
			throw CloudErrorUtil.toCoreException(errorMessage);
		}

		return application;
	}

	/**
	 * 
	 * @return Deployment copy if a manifest file was successfully loaded into
	 * an app's deployment info working copy. Note that the copy is NOT saved.
	 * Null if there is no content to load into the app's deployment info
	 * working copy.
	 * @throws CoreException if error occurred while loading an existing
	 * manifest file.
	 */
	public DeploymentInfoWorkingCopy load(IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		subMonitor.beginTask(NLS.bind(Messages.ManifestParser_READING, appModule.getDeployedApplicationName()), 6);
		DeploymentInfoWorkingCopy workingCopy;
		try {
			workingCopy = appModule.resolveDeploymentInfoWorkingCopy(subMonitor);

			Map<?, ?> application = getApplication(null);

			subMonitor.worked(1);
			if (application == null) {
				return null;
			}
			
			HashMap<Object,Object> applicationClone = new HashMap<Object,Object>();
			applicationClone.putAll(application);
			// Remove the parameters that will be explicitly read and interpreted below.
			applicationClone.remove(ManifestConstants.MEMORY_PROP);
			applicationClone.remove(ManifestConstants.DISK_QUOTA_PROP);
			applicationClone.remove(ManifestConstants.SUB_DOMAIN_PROP);
			applicationClone.remove(ManifestConstants.DOMAIN_PROP);
			applicationClone.remove(ManifestConstants.ENV_PROP);
			applicationClone.remove(ManifestConstants.SERVICES_PROP);
			
			// Put all parameters to map
			workingCopy.addAllToDeploymentMap(applicationClone);

			// NOTE: When reading from manifest, the manifest may be INCOMPLETE,
			// therefore do not automatically
			// set all properties in the deployment info. Check if the value of
			// the
			// property is actually set before set value
			// in the info
			String appName = getStringValue(application, ManifestConstants.NAME_PROP);

			subMonitor.worked(1);
			if (appName == null) {
				CloudFoundryPlugin.logError(Messages.ManifestParser_NO_APP_NAME);
			}
			readMemory(application, workingCopy);
			subMonitor.worked(1);

			readDiskQuota(application, workingCopy);
			subMonitor.worked(1);

			readApplicationURL(application, workingCopy, appName, monitor);
			subMonitor.worked(1);

			readEnvars(workingCopy, application);
			subMonitor.worked(1);

			readServices(workingCopy, application);
			subMonitor.worked(1);
		}
		finally {
			subMonitor.done();
		}

		return workingCopy;
	}

	protected void readEnvars(DeploymentInfoWorkingCopy workingCopy, Map<?, ?> applications) {
		Map<?, ?> propertiesMap = getContainingPropertiesMap(applications, ManifestConstants.ENV_PROP);

		if (propertiesMap == null) {
			return;
		}

		List<EnvironmentVariable> variables = new ArrayList<EnvironmentVariable>();
		for (Entry<?, ?> entry : propertiesMap.entrySet()) {
			if ((entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
				String varName = (String) entry.getKey();
				String varValue = (String) entry.getValue();
				if (varName != null && varValue != null) {
					EnvironmentVariable variable = new EnvironmentVariable();
					variable.setVariable(varName);
					variable.setValue(varValue);
					variables.add(variable);
				}
			}
		}
		workingCopy.setEnvVariables(variables);
	}

	protected void readServices(DeploymentInfoWorkingCopy workingCopy, Map<?, ?> applications) {
		Map<?, ?> services = getContainingPropertiesMap(applications, ManifestConstants.SERVICES_PROP);
		// Backward compatibility with old manifest pre-1.8.2 where services
		// were maps
		if (services != null) {
			Map<String, CFServiceInstance> servicesToBind = new LinkedHashMap<String, CFServiceInstance>();

			for (Entry<?, ?> entry : services.entrySet()) {
				Object serviceNameObj = entry.getKey();
				if (serviceNameObj instanceof String) {
					String serviceName = (String) serviceNameObj;
					if (!servicesToBind.containsKey(serviceName)) {
						CFServiceInstance service = new CFServiceInstance(serviceName);
						servicesToBind.put(serviceName, service);

						Object servicePropertiesObj = entry.getValue();
						if (servicePropertiesObj instanceof Map<?, ?>) {
							Map<?, ?> serviceProperties = (Map<?, ?>) servicePropertiesObj;
							String label = getStringValue(serviceProperties, ManifestConstants.LABEL_PROP);
							if (label != null) {
								service.setService(label);
							}
							String version = getStringValue(serviceProperties, ManifestConstants.VERSION_PROP);
							if (version != null) {
								service.setVersion(version);
							}
							String plan = getStringValue(serviceProperties, ManifestConstants.PLAN_PROP);
							if (plan != null) {
								service.setPlan(plan);
							}
						}
					}
				}
			}

			workingCopy.setServices(new ArrayList<CFServiceInstance>(servicesToBind.values()));
		}
		else {
			Object yamlElementObj = applications.get(ManifestConstants.SERVICES_PROP);
			if (yamlElementObj instanceof List<?>) {
				List<?> servListFromYaml = (List<?>) yamlElementObj;
				Set<String> addedService = new HashSet<String>();
				List<CFServiceInstance> cloudServices = new ArrayList<CFServiceInstance>();
				for (Object servNameObj : servListFromYaml) {
					if (servNameObj instanceof String && !addedService.contains(servNameObj)) {
						String serviceName = (String) servNameObj;
						addedService.add(serviceName);
						cloudServices.add(new CFServiceInstance(serviceName));
					}
				}
				workingCopy.setServices(cloudServices);
			}

		}
	}

	protected void readApplicationURL(Map<?, ?> application, DeploymentInfoWorkingCopy workingCopy, String appName,
			IProgressMonitor monitor) {
		String subdomain = getStringValue(application, ManifestConstants.SUB_DOMAIN_PROP);
		String domain = getStringValue(application, ManifestConstants.DOMAIN_PROP);

		// A URL can only be constructed from the manifest if either a domain or
		// a subdomain is specified. If neither is specified, but the app name
		// is, the app name is used as the subdomain.Otherwise the
		// deployment process will generate a URL from the app name, but it is
		// not necessary to specify that URL here.
		if (subdomain == null && domain == null && appName == null) {
			return;
		}
		CloudApplicationURL cloudURL = null;

		try {
			ApplicationUrlLookupService urlLookup = ApplicationUrlLookupService.update(cloudServer, monitor);

			if (subdomain == null) {
				subdomain = appName;
			}

			if (domain == null) {
				// If no domain is specified get a URL with a default domain
				cloudURL = urlLookup.getDefaultApplicationURL(subdomain);
			}
			else {
				// retain the URL even if it fails validation as it may contain
				// partial information that can still be displayed
				// to a user
				cloudURL = new CloudApplicationURL(subdomain, domain);
				cloudURL = urlLookup.validateCloudApplicationUrl(cloudURL);
			}
		}
		catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}

		if (cloudURL != null) {
			List<String> urls = Arrays.asList(cloudURL.getUrl());
			workingCopy.setUris(urls);
		}
	}

	protected void readMemory(Map<?, ?> application, DeploymentInfoWorkingCopy workingCopy) {
		Integer memoryVal = getIntegerValue(application, ManifestConstants.MEMORY_PROP);

		// If not in Integer form, try String as the memory may end in with a
		// 'G' or 'M'
		if (memoryVal == null) {
			String memoryStringVal = getStringValue(application, ManifestConstants.MEMORY_PROP);
			if (memoryStringVal != null && memoryStringVal.length() > 0) {

				char memoryIndicator[] = { 'M', 'G', 'm', 'g' };
				int gIndex = -1;

				for (char indicator : memoryIndicator) {
					gIndex = memoryStringVal.indexOf(indicator);
					if (gIndex >= 0) {
						break;
					}
				}

				// There has to be a number before the 'G' or 'M', if 'G' or 'M'
				// is used, or its not a valid
				// memory
				if (gIndex > 0) {
					memoryStringVal = memoryStringVal.substring(0, gIndex);
				}
				else if (gIndex == 0) {
					CloudFoundryPlugin.logError("Failed to read memory value in manifest file: " + relativePath //$NON-NLS-1$
							+ " for: " + appModule.getDeployedApplicationName() + ". Invalid memory: " //$NON-NLS-1$ //$NON-NLS-2$
							+ memoryStringVal);
				}

				try {
					memoryVal = Integer.valueOf(memoryStringVal);
				}
				catch (NumberFormatException e) {
					// Log an error but do not stop the parsing
					CloudFoundryPlugin.logError("Failed to parse memory from manifest file: " + relativePath + " for: " //$NON-NLS-1$ //$NON-NLS-2$
							+ appModule.getDeployedApplicationName() + " due to: " + e.getMessage()); //$NON-NLS-1$
				}
			}
		}

		if (memoryVal != null) {
			int actualMemory = -1;
			switch (memoryVal.intValue()) {
			case 1:
				actualMemory = 1024;
				break;
			case 2:
				actualMemory = 2048;
				break;
			default:
				actualMemory = memoryVal.intValue();
				break;
			}
			if (actualMemory > 0) {
				workingCopy.setMemory(actualMemory);
			}
		}
	}

	protected void readDiskQuota(Map<?, ?> application, DeploymentInfoWorkingCopy workingCopy) {
		Integer diskQuotaVal = getIntegerValue(application, ManifestConstants.DISK_QUOTA_PROP);

		// If not in Integer form, try String as the memory may end in with a
		// 'G' or 'M'
		if (diskQuotaVal == null) {
			String diskQuotaStringVal = getStringValue(application, ManifestConstants.DISK_QUOTA_PROP);
			if (diskQuotaStringVal != null && diskQuotaStringVal.length() > 0) {

				char diskQuotaIndicator[] = { 'M', 'G', 'm', 'g' };
				int gIndex = -1;

				for (char indicator : diskQuotaIndicator) {
					gIndex = diskQuotaStringVal.indexOf(indicator);
					if (gIndex >= 0) {
						break;
					}
				}

				// There has to be a number before the 'G' or 'M', if 'G' or 'M'
				// is used, or its not a valid
				// memory
				if (gIndex > 0) {
					diskQuotaStringVal = diskQuotaStringVal.substring(0, gIndex);
				}
				else if (gIndex == 0) {
					CloudFoundryPlugin.logError("Failed to disk quota value in manifest file: " + relativePath //$NON-NLS-1$
							+ " for: " + appModule.getDeployedApplicationName() + ". Invalid disk quota: " //$NON-NLS-1$ //$NON-NLS-2$
							+ diskQuotaStringVal);
				}

				try {
					diskQuotaVal = Integer.valueOf(diskQuotaStringVal);
				}
				catch (NumberFormatException e) {
					// Log an error but do not stop the parsing
					CloudFoundryPlugin.logError("Failed to disk quota memory from manifest file: " + relativePath + " for: " //$NON-NLS-1$ //$NON-NLS-2$
							+ appModule.getDeployedApplicationName() + " due to: " + e.getMessage()); //$NON-NLS-1$
				}
			}
		}

		if (diskQuotaVal != null) {
			int actualDiskQuota = -1;
			switch (diskQuotaVal.intValue()) {
			case 1:
				actualDiskQuota = 1024;
				break;
			case 2:
				actualDiskQuota = 2048;
				break;
			default:
				actualDiskQuota = diskQuotaVal.intValue();
				break;
			}
			if (actualDiskQuota > 0) {
				workingCopy.setDiskQuota(actualDiskQuota);
			}
		}
	}

	/**
	 * 
	 * @return map of parsed manifest file, if the file exists. If the file does
	 * not exist, return null.
	 * @throws CoreException if manifest file exists, but error occurred that
	 * prevents a map to be generated.
	 */
	protected Map<Object, Object> parseManifestFromFile() throws CoreException {

		InputStream inputStream = null;
		IFile resource = getFile();

		if (resource != null && resource.exists()) {
			inputStream = resource.getContents();
		}

		if (inputStream != null) {
			Yaml yaml = new Yaml();

			try {

				Object results = yaml.load(inputStream);

				if (results instanceof Map<?, ?>) {
					return (Map<Object, Object>) results;
				}
				else {
					throw CloudErrorUtil.toCoreException("Expected a map of values for manifest file: " + relativePath //$NON-NLS-1$
							+ ". Unable to load manifest content.  Actual results: " + results); //$NON-NLS-1$
				}

			}
			finally {
				try {
					inputStream.close();
				}
				catch (IOException e) {
					// Ignore
				}
			}

		}
		return null;
	}

	/**
	 * Writes the app's current deployment info into a manifest file in the
	 * app's related workspace project. If the workspace project is not
	 * accessible, false is returned. If the manifest file does not exist in the
	 * app's workspace project, one will be created. If manifest file failed to
	 * create, exception is thrown. Returns true if the manifest file was
	 * successfully written. If so, the project is also refreshed.
	 * @return true if deployment info for the cloud module was written to
	 * manifest file. False if there was no content to write to the manifest
	 * file.
	 * <p/>
	 * WHne writing content to manifest, if the application has a previous
	 * deployment information, and the app name changed, meaning the current
	 * name of the application does not match the one in the old deployment
	 * information, the old entry in the manifest will be updated to the new
	 * name, rather than creating a new entry with the new name.
	 * <p/>
	 * If both an existing entry with the current application name, as well as
	 * an old entry with the old application name, exist, the old entry has
	 * higher priority, as the old entry truly represents the application that
	 * is being edited.
	 * @param monitor progress monitor
	 * @param previousInfo Previous deployment information pertaining to the
	 * give application module. May be null if there is no previous information
	 * available.
	 * @throws CoreException if error occurred while writing to a Manifest file.
	 */
	public boolean write(IProgressMonitor monitor, ApplicationDeploymentInfo previousInfo) throws CoreException {

		ApplicationDeploymentInfo deploymentInfo = appModule.getDeploymentInfo();

		if (deploymentInfo == null) {
			return false;
		}

		SubMonitor subProgress = SubMonitor.convert(monitor);
		subProgress.beginTask(NLS.bind(Messages.ManifestParser_WRITING, appModule.getDeployedApplicationName()), 5);

		try {

			// Fetch the previous name, in case the app name was changed. This
			// will
			// allow the old
			// entry to be replaced by the new one, since application entries
			// are
			// looked up by application name.
			String previousName = previousInfo != null ? previousInfo.getDeploymentName() : null;

			String appName = deploymentInfo.getDeploymentName();

			Map<Object, Object> deploymentInfoYaml = null;
			try {
				deploymentInfoYaml = parseManifestFromFile();
			} catch (CoreException e) {
				// Failed to load the original manifest file. Continue to try to write the new content.
				CloudFoundryPlugin.logWarning(e.getMessage());
			}

			subProgress.worked(1);

			if (deploymentInfoYaml == null) {
				deploymentInfoYaml = new LinkedHashMap<Object, Object>();
			}

			Object applicationsObj = deploymentInfoYaml.get(ManifestConstants.APPLICATIONS_PROP);
			List<Map<Object, Object>> applicationsList = null;
			if (applicationsObj == null) {
				applicationsList = new ArrayList<Map<Object, Object>>();
				deploymentInfoYaml.put(ManifestConstants.APPLICATIONS_PROP, applicationsList);
			}
			else if (applicationsObj instanceof List<?>) {
				applicationsList = (List<Map<Object, Object>>) applicationsObj;
			}
			else {
				throw CloudErrorUtil.toCoreException("Expected a top-level list of applications in: " + relativePath //$NON-NLS-1$
						+ ". Unable to continue writing manifest values."); //$NON-NLS-1$
			}

			Map<Object, Object> applicationWithSameName = null;

			Map<Object, Object> oldApplication = null;

			// Each application listing should be a map. Find both an entry with
			// the
			// same name as the application name
			// As well as an entry with an older name of the application, in
			// case
			// the application has changed.
			for (Object appMap : applicationsList) {
				if (appMap instanceof Map<?, ?>) {
					Map<Object, Object> properties = (Map<Object, Object>) appMap;
					String name = getStringValue(properties, ManifestConstants.NAME_PROP);
					if (appName.equals(name)) {
						applicationWithSameName = properties;
					}
					else if (previousName != null && previousName.equals(name)) {
						oldApplication = properties;
					}
				}
			}

			// The order of priority in terms of replacing an existing entry is
			// : 1.
			// old application entry that
			// has been changed will get replaced 2. existing entry with same
			// name
			// as app will now get replaced2.
			Map<Object, Object> application = oldApplication != null ? oldApplication : applicationWithSameName;

			if (application == null) {
				application = new LinkedHashMap<Object, Object>();
				applicationsList.add(application);
			}

			application.put(ManifestConstants.NAME_PROP, appName);

			String memory = getMemoryAsString(deploymentInfo.getMemory());
			if (memory != null) {
				application.put(ManifestConstants.MEMORY_PROP, memory);
			}

			Integer diskQuotaInt = deploymentInfo.getDiskQuota();
			if (diskQuotaInt != null) {
				String diskQuota = getMemoryAsString(diskQuotaInt);
				if (diskQuota != null) {
					application.put(ManifestConstants.DISK_QUOTA_PROP, diskQuota);
				}
			}

			int instances = deploymentInfo.getInstances();
			if (instances > 0) {
				application.put(ManifestConstants.INSTANCES_PROP, instances);
			}

			List<String> urls = deploymentInfo.getUris();
			if (urls != null && !urls.isEmpty()) {
				// Persist only the first URL
				String url = urls.get(0);

				ApplicationUrlLookupService lookup = ApplicationUrlLookupService.getCurrentLookup(cloudServer);
				CloudApplicationURL cloudUrl = lookup.getCloudApplicationURL(url);
				String subdomain = cloudUrl.getSubdomain();
				String domain = cloudUrl.getDomain();

				if (subdomain != null) {
					application.put(ManifestConstants.SUB_DOMAIN_PROP, subdomain);
				}

				if (domain != null) {
					application.put(ManifestConstants.DOMAIN_PROP, domain);
				}
			}
			else {
				// If URL is not present, remove any exiting ones
				application.remove(ManifestConstants.SUB_DOMAIN_PROP);
				application.remove(ManifestConstants.DOMAIN_PROP);
			}

			List<EnvironmentVariable> envvars = deploymentInfo.getEnvVariables();

			if (envvars != null && !envvars.isEmpty()) {
				Map<Object, Object> varMap = new LinkedHashMap<Object, Object>();

				// Clear the list of environment variables first.
				application.put(ManifestConstants.ENV_PROP, varMap);
				for (EnvironmentVariable var : envvars) {
					varMap.put(var.getVariable(), var.getValue());
				}
			}
			else {
				// Avoid writing empty list or keeping obsolete env vars
				// therefore always remove the property if no en vars are
				// present
				application.remove(ManifestConstants.ENV_PROP);
			}

			String buildpack = deploymentInfo.getBuildpack();
			
			// Only overwrite the buildpack URL if it can be resolved
			// Otherwise retain any old value from before
			if (buildpack != null) {
				application.put(ManifestConstants.BUILDPACK_PROP, buildpack);
			}

			// Only overwrite the archive path if present, but do not
			// clear the property if it is not present as archive
			// paths are local and the value may not be present if doing a
			// refresh from the server
			String archiveURL = deploymentInfo.getArchive();
			if (archiveURL != null) {
				application.put(ManifestConstants.PATH_PROP, archiveURL);
			}

			// Regardless if there are services or not, always clear list of
			// services in the manifest. The list of
			// services in the
			// deployment info has to match the content in the manifest.

			List<CFServiceInstance> servicesToBind = deploymentInfo.getServices();
			if (servicesToBind != null && !servicesToBind.isEmpty()) {

				List<String> services = new ArrayList<String>();
				application.put(ManifestConstants.SERVICES_PROP, services);

				for (CFServiceInstance service : servicesToBind) {
					String serviceName = service.getName();
					if (serviceName != null && !services.contains(serviceName)) {
						services.add(serviceName);
					}
				}
			}
			else {
				application.remove(ManifestConstants.SERVICES_PROP);
			}
			
			// Copy the rest of the non-special handled ones.
			HashMap<Object, Object> curUnknownInfo = deploymentInfo.getUnknownInfo();
			application.putAll(curUnknownInfo);
			
			// Remove the stack value if it does not exist to handle the default stack settings.
			if (!curUnknownInfo.containsKey(ManifestConstants.STACK_PROP)) {
				application.remove(ManifestConstants.STACK_PROP);
			}

			subProgress.worked(1);

			if (deploymentInfoYaml.isEmpty()) {
				return false;
			}

			DumperOptions options = new DumperOptions();
			options.setExplicitStart(true);
			options.setCanonical(false);
			options.setPrettyFlow(true);
			options.setDefaultFlowStyle(FlowStyle.BLOCK);
			Yaml yaml = new Yaml(options);
			String manifestValue = yaml.dump(deploymentInfoYaml);

			subProgress.worked(1);
			if (manifestValue == null) {
				throw CloudErrorUtil.toCoreException("Manifest map for " + appModule.getDeployedApplicationName() //$NON-NLS-1$
						+ " contained values but yaml parser failed to serialise the map. : " + deploymentInfoYaml); //$NON-NLS-1$
			}

			IFile file = getFile();

			if (file != null) {
				ByteArrayInputStream bais = new ByteArrayInputStream(manifestValue.getBytes());

				if (file.exists()) {
					file.delete(true, subProgress);
				} 
				
				file.create(bais, true, subProgress);

				subProgress.worked(1);
				refreshProject(monitor);
				subProgress.worked(1);
				return true;
			}
			else {
				throw CloudErrorUtil.toCoreException("No changes could be written to: " + relativePath //$NON-NLS-1$
						+ ". Unable to write changes to the application's manifest file for: " //$NON-NLS-1$
						+ appModule.getDeployedApplicationName());
			}

		}
		finally {
			subProgress.done();
		}
	}

	protected String getMemoryAsString(int memory) {
		if (memory < 1) {
			return null;
		}
		return memory + "M"; //$NON-NLS-1$
	}

	protected void refreshProject(IProgressMonitor monitor) throws CoreException {

		IProject project = CloudFoundryProjectUtil.getProject(appModule);
		if (project != null && project.isAccessible()) {
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
	}

}
