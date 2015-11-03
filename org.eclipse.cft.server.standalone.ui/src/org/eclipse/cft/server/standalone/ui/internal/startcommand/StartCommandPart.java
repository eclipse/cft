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
package org.eclipse.cft.server.standalone.ui.internal.startcommand;

import org.eclipse.cft.server.ui.internal.UIPart;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Base part that allows a UI part to be defined to set a Java start command
 * 
 */
public abstract class StartCommandPart extends UIPart {
	private final Composite parent;

	private Control composite;

	protected StartCommandPart(Composite parent) {
		this.parent = parent;
	}

	public Control getComposite() {
		if (composite == null) {
			composite = createPart(parent);
		}
		return composite;
	}

	/**
	 * Tells the part to update the start command from current values of in the
	 * UI control and notify listeners with the revised start command
	 */
	abstract public void updateStartCommand();

}