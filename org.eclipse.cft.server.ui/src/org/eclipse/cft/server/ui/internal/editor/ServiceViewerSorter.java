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

import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TableColumn;

public class ServiceViewerSorter extends CloudFoundryViewerSorter {
	private final TableViewer tableViewer;

	public ServiceViewerSorter(TableViewer tableViewer) {
		this.tableViewer = tableViewer;
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		TableColumn sortColumn = tableViewer.getTable().getSortColumn();
		if (sortColumn != null) {
			ServiceViewColumn serviceColumn = (ServiceViewColumn) sortColumn.getData();
			int result = 0;
			int sortDirection = tableViewer.getTable().getSortDirection();
			if (serviceColumn != null) {
				if (e1 instanceof CFServiceInstance && e2 instanceof CFServiceInstance) {
					CFServiceInstance service1 = (CFServiceInstance) e1;
					CFServiceInstance service2 = (CFServiceInstance) e2;

					switch (serviceColumn) {
					case Name:
						result = super.compare(tableViewer, e1, e2);
						break;
					default:
						result = compare(service1, service2, serviceColumn);
						break;
					}

				}
			}
			return sortDirection == SWT.UP ? result : -result;
		}

		return super.compare(viewer, e1, e2);
	}

	protected int compare(CFServiceInstance service1, CFServiceInstance service2,
			ServiceViewColumn sortColumn) {
		int result = 0;
		switch (sortColumn) {
		case Version:
			result = service1.getVersion() != null ? service1.getVersion().compareTo(service2.getVersion()) : 0;
			break;
		case Service:
			result = service1.getService() != null ? service1.getService().compareTo(service2.getService()) : 0;
			break;
		case Plan:
			result = service1.getPlan() != null ? service1.getPlan().compareTo(service2.getPlan()) : 0;
			break;
		}
		return result;
	}
}