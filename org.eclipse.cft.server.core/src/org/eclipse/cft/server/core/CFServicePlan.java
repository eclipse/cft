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
package org.eclipse.cft.server.core;

/**
 * 
 * WARNING: Referenced by public API. Do not move or rename to avoid breaking
 * public API
 *
 */
public class CFServicePlan {
	private String name;

	private boolean free;

	private boolean publicPlan;

	private String description;

	private String extra;

	private String uniqueId;

	private CFServiceOffering serviceOffering;

	public CFServicePlan(String name, String description, boolean free, boolean publicPlan, String extra,
			String uniqueId) {
		this.name = name;
		this.description = description;
		this.free = free;
		this.publicPlan = publicPlan;
		this.extra = extra;
		this.uniqueId = uniqueId;
	}

	public String getName() {
		return this.name;
	}

	public boolean isFree() {
		return this.free;
	}

	public boolean isPublic() {
		return this.publicPlan;
	}

	public String getDescription() {
		return description;
	}

	public String getExtra() {
		return extra;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public CFServiceOffering getServiceOffering() {
		return serviceOffering;
	}

	public void setServiceOffering(CFServiceOffering serviceOffering) {
		this.serviceOffering = serviceOffering;
	}
}
