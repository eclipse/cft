/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.ui.internal;

import org.eclipse.cft.server.core.internal.ValidationEvents;
import org.eclipse.core.runtime.IStatus;

/**
 * Contains the {@link IStatus} of a validation operation, along with a
 * validation event type indicating an event that either occurred during or
 * after the validation process.
 *
 */
public class ValidationStatus {

	private final IStatus status;

	private final int eventType;

	public ValidationStatus(IStatus status, int eventType) {
		this.status = status;
		this.eventType = eventType;
	}

	public IStatus getStatus() {
		return status;
	}

	/**
	 * @return an event defined in {@link ValidationEvents}
	 */
	public int getEventType() {
		return eventType;
	}

	@Override
	public String toString() {
		return getStatus() != null ? getStatus().toString() : null;
	}
}