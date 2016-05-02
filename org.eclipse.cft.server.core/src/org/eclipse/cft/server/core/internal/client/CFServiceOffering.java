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
package org.eclipse.cft.server.core.internal.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes a Cloud service. This service is typically defined in Cloud Foundry
 * and is used to create service instances. See {@link CFServiceInstance}
 *
 */
public class CFServiceOffering {
	private String serviceName;

	private String version;

	private String provider;

	private String description;

	private boolean active;

	private boolean bindable;

	private String url;

	private String infoUrl;

	private String uniqueId;

	private String extra;

	private String docUrl;

	private List<CFServicePlan> servicePlans = new ArrayList<CFServicePlan>();

	public CFServiceOffering(String serviceName, String version, String description, boolean active, boolean bindable,
			String url, String infoUrl, String uniqueId, String extra, String docUrl, String provider) {
		this.serviceName = serviceName;
		this.version = version;
		this.description = description;
		this.active = active;
		this.bindable = bindable;
		this.url = url;
		this.infoUrl = infoUrl;
		this.uniqueId = uniqueId;
		this.extra = extra;
		this.docUrl = docUrl;
		this.provider = provider;
	}

	public String getName() {
		return this.serviceName;
	}

	public String getDescription() {
		return description;
	}

	public String getVersion() {
		return version;
	}

	public boolean isActive() {
		return active;
	}

	public boolean isBindable() {
		return bindable;
	}

	public String getUrl() {
		return url;
	}

	public String getInfoUrl() {
		return infoUrl;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public String getExtra() {
		return extra;
	}

	public String getDocumentationUrl() {
		return docUrl;
	}

	/**
	 * 
	 * @return non-null list of service plans
	 */
	public List<CFServicePlan> getServicePlans() {
		return new ArrayList<CFServicePlan>(servicePlans);
	}
	
	/**
	 * 
	 * @return non-null list of service plans
	 */
	public void setServicePlans(List<CFServicePlan> servicePlans) {
		if (servicePlans == null) {
			servicePlans = Collections.emptyList();
		}
		this.servicePlans = servicePlans;
	}

	public String getProvider() {
		return provider;
	}
}
