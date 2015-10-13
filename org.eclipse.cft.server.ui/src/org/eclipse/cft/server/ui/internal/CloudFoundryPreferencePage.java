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
package org.eclipse.cft.server.ui.internal;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class CloudFoundryPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public void init(IWorkbench workbench) {
		//
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite mainArea = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(mainArea);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(mainArea);

		Label serviceLabel = new Label(mainArea, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(serviceLabel);
		serviceLabel.setText(Messages.CloudFoundryPreferencePage_TEXT_SELECT_CF_FEATURE);

		return mainArea;
	}

}
