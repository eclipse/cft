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
package org.eclipse.cft.server.ui.internal.wizards;

import org.eclipse.cft.server.core.AbstractApplicationDelegate;

/**
 * 
 * This contains a reference to the core-level application delegate, which
 * contains API to push an application to a CF server. 
 */
public abstract class ApplicationWizardDelegate implements IApplicationWizardDelegate {

	private AbstractApplicationDelegate appDelegate;

	public void setApplicationDelegate(AbstractApplicationDelegate appDelegate) {
		this.appDelegate = appDelegate;
	}

	/**
	 * Corresponding core level application delegate that contains API for
	 * pushing an app to a CF server. This may be null, as a wizard delegate may
	 * not be mapped to an app delegate (in the event it uses a default app
	 * delegate from the CF Application framework) .
	 * @return Corresponding Application delegate, if it exists, or null.
	 */
	public AbstractApplicationDelegate getApplicationDelegate() {
		return appDelegate;
	}

}
