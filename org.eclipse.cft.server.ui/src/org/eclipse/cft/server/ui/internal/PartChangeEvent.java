/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
 * Event fired by a component, like a UI part, when state in that component has
 * changed.
 */
public class PartChangeEvent {

	private final IStatus status;

	private final IEventSource<?> source;

	private final Object data;

	private final int type;

	public PartChangeEvent(Object data, IStatus status, IEventSource<?> source, int type) {
		this.source = source;
		this.status = status;
		this.data = data;
		this.type = type;
	}

	public PartChangeEvent(Object data, IStatus status, IEventSource<?> source) {
		this(data, status, source, ValidationEvents.EVENT_NONE);
	}

	public IEventSource<?> getSource() {
		return source;
	}

	public IStatus getStatus() {
		return status;
	}

	public int getType() {
		return type;
	}

	public Object getData() {
		return data;
	}

	@Override
	public String toString() {
		return "PartChangeEvent [status=" + status + ", source=" + source + ", type=" + type + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

}