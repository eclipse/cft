/*******************************************************************************
 * Copyright (c) 2014, 2015 Pivotal Software, Inc. 
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
 *     IBM - Bug 485697 - Implement host name taken check in CF wizards
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.wizards;

import org.eclipse.cft.server.ui.internal.IEventSource;
import org.eclipse.cft.server.ui.internal.Messages;

public class CloudUIEvent implements IEventSource<CloudUIEvent> {
	public static final CloudUIEvent APP_NAME_CHANGE_EVENT = new CloudUIEvent(
			Messages.CloudUIEvent_TEXT_APP_NAME_CHANGE);
	
	public static final CloudUIEvent VALIDATE_HOST_TAKEN_EVENT = new CloudUIEvent(
			Messages.CloudUIEvent_TEXT_VALIDATE_HOST_TAKEN_EVENT);

	public static final CloudUIEvent APPLICATION_URL_CHANGED = new CloudUIEvent(
			Messages.CloudUIEvent_TEXT_APP_URL_CHANGE);

	public static final CloudUIEvent VALIDATE_SUBDOMAIN_EVENT = new CloudUIEvent(
			Messages.CloudUIEvent_TEXT_VALIDATE_SUBDOMAIN_EVENT);

	public static final CloudUIEvent BUILD_PACK_URL = new CloudUIEvent(Messages.CloudUIEvent_TEXT_BUILDPACK);

	public static final CloudUIEvent MEMORY = new CloudUIEvent(Messages.COMMONTXT_MEM);

	private final String name;

	public CloudUIEvent(String name) {
		this.name = name;
	}

	@Override
	public CloudUIEvent getSource() {
		return this;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return getName();
	}

}
