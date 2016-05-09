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

import org.eclipse.core.runtime.Assert;

/**
 * Represents a cloud service instance.
 * 
 * WARNING: Referenced by public API. Do not move or rename to avoid breaking
 * public API
 * 
 */
public class CFServiceInstance {

	private String name;

	private String version;

	private String service;

	private String plan;

	private boolean isLocal = false;

	public CFServiceInstance(String name) {
		Assert.isNotNull(name);
		setName(name);
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getService() {
		return service;
	}

	public String getPlan() {
		return plan;
	}

	/**
	 * 
	 * @return isLocal true if the service instance is defined locally but not
	 * in the Cloud. False (default) if service exists in CF.
	 */
	public boolean isLocal() {
		return isLocal;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setService(String service) {
		this.service = service;
	}

	public void setPlan(String plan) {
		this.plan = plan;
	}

	/**
	 * 
	 * @param isLocal true if the service instance is defined locally but not in
	 * the Cloud. False (default) if service exists in CF.
	 */
	public void setIsLocal(boolean isLocal) {
		this.isLocal = isLocal;
	}
}
