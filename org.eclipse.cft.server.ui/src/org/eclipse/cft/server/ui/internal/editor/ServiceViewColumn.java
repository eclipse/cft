/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.ui.internal.editor;

public enum ServiceViewColumn {
	Name(150), Version(100), Vendor(100), Tunnel(80), Plan(50), Provider(100);
	private int width;

	private ServiceViewColumn(int width) {
		this.width = width;
	}

	public int getWidth() {
		return width;
	}

	public static ServiceViewColumnDescriptor getServiceViewColumnDescriptor() {
		return new ServiceViewColumnDescriptor();
	}

	public static class ServiceViewColumnDescriptor {

		public ServiceViewColumnDescriptor() {
		}

		public ServiceViewColumn[] getServiceViewColumn() {
			return new ServiceViewColumn[] { Name, Vendor, Provider, Version, Plan };
		}

	}

}
