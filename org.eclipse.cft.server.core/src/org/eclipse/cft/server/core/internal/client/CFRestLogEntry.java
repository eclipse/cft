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

import java.net.URI;

public class CFRestLogEntry {

	private final URI uri;

	private final String status;

	private final String message;

	public CFRestLogEntry(URI uri, String status, String message) {
		super();
		this.uri = uri;
		this.status = status;
		this.message = message;
	}

	public URI getUri() {
		return uri;
	}

	public String getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}


}
