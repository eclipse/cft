/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal.debug;

public class DebugConnectionDescriptor {

	private final String host;

	private final int port;

	private final int timeout;

	public static final int DEFAULT_TIMEOUT = 30 * 1000;

	public DebugConnectionDescriptor(String host, int port) {
		this(host, port, DEFAULT_TIMEOUT);
	}

	public DebugConnectionDescriptor(String ip, int port, int timeout) {
		this.host = ip;
		this.port = port;
		this.timeout = timeout;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public int getTimeout() {
		return timeout;
	}

	public boolean isValid() {
		return host != null && host.length() > 0 && port > 0;
	}

	@Override
	public String toString() {
		return "DebugConnectionDescriptor [ip=" + host + ", port=" + port + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + port;
		result = prime * result + timeout;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DebugConnectionDescriptor other = (DebugConnectionDescriptor) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		}
		else if (!host.equals(other.host))
			return false;
		if (port != other.port)
			return false;
		if (timeout != other.timeout)
			return false;
		return true;
	}

}