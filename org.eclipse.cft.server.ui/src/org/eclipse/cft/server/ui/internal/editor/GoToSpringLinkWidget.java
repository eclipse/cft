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

import org.eclipse.cft.server.ui.internal.CloudFoundryURLNavigation;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;


public class GoToSpringLinkWidget extends LinkWidget {

	public static final String NAVIGATION_LABEL = Messages.GoToSpringLinkWidget_TEXT_SPRING_INSIGHT;

	public GoToSpringLinkWidget(Composite parent, FormToolkit toolKit) {
		super(parent, NAVIGATION_LABEL, CloudFoundryURLNavigation.INSIGHT_URL.getLocation(), toolKit);
	}

	protected void navigate() {
		CloudFoundryURLNavigation.INSIGHT_URL.navigate();
	}
}
