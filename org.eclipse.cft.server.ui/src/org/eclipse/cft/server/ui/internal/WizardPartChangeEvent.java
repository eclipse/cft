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

public class WizardPartChangeEvent extends PartChangeEvent {

	private final boolean updateWizardButtons;

	public WizardPartChangeEvent(Object data, IStatus status, UIPart source, int type, boolean updateWizardButtons) {
		super(data, status, source, type);
		this.updateWizardButtons = updateWizardButtons;
	}

	public WizardPartChangeEvent(Object data, IStatus status, IEventSource<?> source, boolean updateWizardButtons) {
		super(data, status, source, ValidationEvents.EVENT_NONE);
		this.updateWizardButtons = updateWizardButtons;
	}

	public boolean updateWizardButtons() {
		return updateWizardButtons;
	}

}
