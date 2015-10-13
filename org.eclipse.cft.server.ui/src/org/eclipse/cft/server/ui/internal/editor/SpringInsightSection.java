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

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.ui.internal.CloudFoundryURLNavigation;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;


public class SpringInsightSection extends ServerEditorSection {

	public void createSection(Composite parent) {

		// Don't create section if CF server is not the Pivotal CF server
		if (!CloudFoundryURLNavigation.canEnableCloudFoundryNavigation(getCloudFoundryServer())) {
			return;
		}
		super.createSection(parent);

		FormToolkit toolkit = getFormToolkit(parent.getDisplay());

		Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR);
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		section.setText(Messages.SpringInsightSection_TEXT_SPRING_INSIGHT);
		section.setExpanded(false);

		Composite composite = toolkit.createComposite(section);
		section.setClient(composite);

		GridLayoutFactory.fillDefaults().numColumns(1).margins(10, 5).applyTo(composite);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);

		new GoToSpringLinkWidget(composite, toolkit).createControl();

	}

	protected CloudFoundryServer getCloudFoundryServer() {
		if (server != null) {
			return (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
		}
		return null;
	}

}
