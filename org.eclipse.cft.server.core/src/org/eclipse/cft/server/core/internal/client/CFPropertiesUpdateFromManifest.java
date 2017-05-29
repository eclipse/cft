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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cloudfoundry.client.lib.domain.HealthCheckType;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.Staging.StagingBuilder;
import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.StringUtils;
import org.eclipse.cft.server.core.internal.application.ManifestConstants;
import org.eclipse.cft.server.core.internal.application.ManifestParser;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;

public class CFPropertiesUpdateFromManifest {

	private ApplicationDeploymentInfo wc;

	private List<ManifestUpdateCaller> consumers = new ArrayList<>();

	private final ManifestParser parser;

	public CFPropertiesUpdateFromManifest(ManifestParser parser) {
		this.parser = parser;
	}

	protected ApplicationDeploymentInfo getManifestDeploymentInfo() throws CoreException {
		if (this.wc == null) {
			throw CloudErrorUtil
					.toCoreException("Manifest comparator has not been loaded. Please load the comparator."); //$NON-NLS-1$
		}
		return this.wc;
	}

	public void load(IProgressMonitor monitor) throws CoreException {
		this.wc = parser.load(monitor);
	}

	public CFPropertiesUpdateFromManifest memory(int memory, CFConsumer<Integer> consumer) throws CoreException {
		if (getManifestDeploymentInfo().hasProperty(ManifestConstants.MEMORY_PROP)) {
			int manifestVal = getManifestDeploymentInfo().getMemory();
			if (manifestVal != memory) {
				add(ManifestConstants.MEMORY_PROP, manifestVal, memory, consumer);
			}
		}
		return this;
	}

	public CFPropertiesUpdateFromManifest instances(int instances, CFConsumer<Integer> consumer) throws CoreException {
		if (getManifestDeploymentInfo().hasProperty(ManifestConstants.INSTANCES_PROP)) {
			int manifestVal = getManifestDeploymentInfo().getInstances();
			if (manifestVal != instances) {
				add(ManifestConstants.INSTANCES_PROP, manifestVal, instances, consumer);
			}
		}
		return this;
	}

	public CFPropertiesUpdateFromManifest diskQuota(int diskQuota, CFConsumer<Integer> consumer) throws CoreException {
		if (getManifestDeploymentInfo().hasProperty(ManifestConstants.DISK_QUOTA_PROP)) {
			int manifestVal = getManifestDeploymentInfo().getDiskQuota();
			if (manifestVal != diskQuota) {
				add(ManifestConstants.DISK_QUOTA_PROP, manifestVal, diskQuota, consumer);
			}
		}
		return this;
	}

	/**
	 * For v1 only.
	 * @param cloudStaging
	 * @param consumer
	 * @return
	 * @throws CoreException
	 */
	public CFPropertiesUpdateFromManifest v1Staging(Staging cloudStaging, CFConsumer<Staging> consumer)
			throws CoreException {

		String currentHche = cloudStaging.getHealthCheckHttpEndpoint();
		HealthCheckType hct = cloudStaging.getHealthCheckType();
		String currentHct = hct != null ? hct.toString() : null;
		String currentStack = cloudStaging.getStack();

		// Only comparing HCHE, HCT and stack, as these are the only ones
		// currently supported for updating. Preserve the other values
		int currentTimeout = cloudStaging.getHealthCheckTimeout() != null
				? cloudStaging.getHealthCheckTimeout().intValue() : 0;
		String currentBuildpack = cloudStaging.getBuildpackUrl();
		String currentDetectedBuildpack = cloudStaging.getDetectedBuildpack();
		String currentCommand = cloudStaging.getCommand();

		StagingBuilder builder = Staging.builder().healthCheckTimeout(currentTimeout).buildpack(currentBuildpack)
				.detectedBuildpack(currentDetectedBuildpack).command(currentCommand);

		Map<String, String> changedProps = new HashMap<>();

		String manifestHche = getManifestDeploymentInfo().getHealthCheckHttpEndpoint();
		String manifestHct = getManifestDeploymentInfo().getHealthCheckType();
		String manifestStack = getManifestDeploymentInfo().getStack();

		if (getManifestDeploymentInfo().hasProperty(ManifestConstants.HEALTH_CHECK_HTTP_ENDPOINT)
				&& !equals(manifestHche, currentHche)) {
			builder.healthCheckHttpEndpoint(manifestHche);
			changedProps.put(ManifestConstants.HEALTH_CHECK_HTTP_ENDPOINT,
					getComparisonMessage(ManifestConstants.HEALTH_CHECK_HTTP_ENDPOINT, manifestHche, currentHche));
		}
		if (getManifestDeploymentInfo().hasProperty(ManifestConstants.HEALTH_CHECK_TYPE)
				&& !equals(manifestHct, currentHct)) {
			builder.healthCheckType(HealthCheckType.from(manifestHct));
			changedProps.put(ManifestConstants.HEALTH_CHECK_TYPE,
					getComparisonMessage(ManifestConstants.HEALTH_CHECK_TYPE, manifestHct, currentHct));
		}
		if (getManifestDeploymentInfo().hasProperty(ManifestConstants.STACK_PROP)
				&& !equals(manifestStack, currentStack)) {
			builder.stack(manifestStack);
			changedProps.put(ManifestConstants.STACK_PROP,
					getComparisonMessage(ManifestConstants.STACK_PROP, manifestStack, currentStack));
		}

		if (!changedProps.isEmpty()) {
			StringBuilder propNames = new StringBuilder();
			StringBuilder messages = new StringBuilder();
			int size = changedProps.size();
			for (Entry<String, String> entry : changedProps.entrySet()) {
				propNames.append(entry.getKey());
				if (--size > 0) {
					propNames.append(", ");
				}
				messages.append(entry.getValue());
			}
			add(propNames.toString(), messages.toString(), builder.build(), consumer);
		}

		return this;
	}

	private <T> void add(String propertyName, String comparisonMessage, T manifestValue, CFConsumer<T> consumer) {
		consumers.add(wrap(propertyName, comparisonMessage, manifestValue, consumer));
	}

	private <T> void add(String propertyName, T manifestValue, T cloudValue, CFConsumer<T> consumer) {
		consumers.add(wrap(propertyName, getComparisonMessage(propertyName, manifestValue, cloudValue), manifestValue,
				consumer));
	}

	public List<String> update(IProgressMonitor monitor) throws CoreException {
		List<String> changedProps = new ArrayList<>();

		if (!consumers.isEmpty()) {
			if (!CloudFoundryPlugin.getCallback().question(
					Messages.CFPropertiesUpdateFromManifest_TITLE_QUESTION_CONFIRM_UPDATES,
					getMessageAllChangedProps(consumers))) {
				CloudFoundryPlugin.logWarning(NLS.bind(Messages.CFPropertiesUpdateFromManifest_UPDATES_CANCELED,
						getManifestDeploymentInfo().getDeploymentName()));
				return Collections.emptyList();
			}

			for (ManifestUpdateCaller cfCallable : consumers) {
				try {
					cfCallable.call(monitor);
					changedProps.add(cfCallable.getProperty());
				}
				catch (CoreException e) {
					// Log error per call but dont let one error prevent call to
					// the next consumer
					CloudFoundryPlugin.logError(e);
				}
			}
		}
		return changedProps;
	}

	private String getMessageAllChangedProps(List<ManifestUpdateCaller> changedProps) throws CoreException {
		String message = NLS.bind(Messages.CFPropertiesUpdateFromManifest_CHANGES_DETECTED,
				getManifestDeploymentInfo().getDeploymentName()) + "\n\n";
		for (ManifestUpdateCaller consumer : changedProps) {
			String chldMsg = consumer.getMessage();
			if (!StringUtils.isEmpty(chldMsg)) {
				message += chldMsg;
			}
		}
		message += "\n" + Messages.CFPropertiesUpdateFromManifest_CONFIRM_UPDATES; //$NON-NLS-1$
		return message;
	}

	protected <T> ManifestUpdateCaller wrap(String property, String message, T result, CFConsumer<T> consumer) {
		return new ManifestUpdateCaller(property, message) {

			@Override
			public void call(IProgressMonitor monitor) throws CoreException {
				try {
					consumer.accept(result, monitor);
				}
				catch (Throwable e) {
					String error = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
					String message = NLS.bind(Messages.CFPropertiesUpdateFromManifest_ERROR_UPDATING_PROPERTY,
							new String[] { property, getManifestDeploymentInfo().getDeploymentName(), error });
					throw CloudErrorUtil.toCoreException(message, e);
				}
			}
		};
	}

	protected boolean equals(String local, String cf) {
		if (local == null) {
			return cf == null;
		}
		else {
			return local.equals(cf);
		}
	}

	protected <T> String getComparisonMessage(String property, T manifestValue, T cloudValue) {
		String message = property + " -- " + Messages.CFPropertiesUpdateFromManifest_MANIFEST_LABEL //$NON-NLS-1$
				+ ": " + manifestValue //$NON-NLS-1$
				+ ", " + Messages.CFPropertiesUpdateFromManifest_CLOUD_LABEL //$NON-NLS-1$
				+ ": " + cloudValue //$NON-NLS-1$
		        + '\n';
		return message;
	}

	public static abstract class ManifestUpdateCaller implements CFCallable {

		private final String property;

		private final String message;

		public ManifestUpdateCaller(String property, String message) {
			this.property = property;
			this.message = message;
		}

		public String getProperty() {
			return property;
		}

		public String getMessage() {
			return message;
		}

	}
}
