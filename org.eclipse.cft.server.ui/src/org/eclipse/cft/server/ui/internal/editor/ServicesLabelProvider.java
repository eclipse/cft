/*******************************************************************************
 * Copyright (c) 2012, 2016 Pivotal Software, Inc. and others
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

import static org.eclipse.cft.server.ui.internal.editor.ApplicationInstanceServiceColumn.Name;
import static org.eclipse.cft.server.ui.internal.editor.ApplicationInstanceServiceColumn.Plan;
import static org.eclipse.cft.server.ui.internal.editor.ApplicationInstanceServiceColumn.Service;
import static org.eclipse.cft.server.ui.internal.editor.ApplicationInstanceServiceColumn.Version;

import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for CloudService
 * @author Terry Denney
 * @author Christian Dupuis
 */
public class ServicesLabelProvider extends LabelProvider implements ITableLabelProvider {

	public ServicesLabelProvider() {

	}

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	public ApplicationInstanceServiceColumn[] getServiceViewColumn() {
		return new ApplicationInstanceServiceColumn[] { Name, Service, Plan, Version };
	}

	public String getColumnText(Object element, int columnIndex) {
		if (element instanceof CFServiceInstance) {
			CFServiceInstance service = (CFServiceInstance) element;
			switch (columnIndex) {
			case 0:
				return service.getName();
			case 1:
				return service.getService();
			case 2:
				return service.getPlan();
			case 3:
				return service.getVersion();
			}
		}
		return null;
	}

}
