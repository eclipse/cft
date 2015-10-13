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

import org.cloudfoundry.client.lib.domain.CloudService;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.TableColumn;

/**
 * @author Terry Denney
 * @author Christian Dupuis
 */
public class ServicesTreeLabelProvider extends LabelProvider implements ITableLabelProvider {

	private final TableViewer viewer;

	public ServicesTreeLabelProvider(TableViewer viewer) {
		this.viewer = viewer;
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof CloudService) {
			return CloudFoundryImages.getImage(CloudFoundryImages.OBJ_SERVICE);
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof CloudService) {
			CloudService service = (CloudService) element;
			return service.getName();
		}
		return super.getText(element);
	}

	public Image getColumnImage(Object element, int columnIndex) {

		TableColumn column = viewer.getTable().getColumn(columnIndex);
		if (column != null && column.getData() instanceof ServiceViewColumn) {

			switch ((ServiceViewColumn) column.getData()) {
			case Name:
				return getImage(element);
			default:
				return getColumnImage((CloudService) element, (ServiceViewColumn) column.getData());
			}
		}

		return null;
	}

	protected Image getColumnImage(CloudService service, ServiceViewColumn column) {
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		String result = null;
		TableColumn column = viewer.getTable().getColumn(columnIndex);
		if (column != null && element instanceof CloudService) {
			CloudService cloudService = (CloudService) element;
			ServiceViewColumn serviceColumn = (ServiceViewColumn) column.getData();

			if (serviceColumn != null) {
				switch (serviceColumn) {
				case Name:
					result = getText(element);
					break;
				case Version:
					result = cloudService.getVersion();
					break;
				case Vendor:
					result = cloudService.getLabel();
					break;
				case Plan:
					result = cloudService.getPlan();
					break;
				case Provider:
					result = cloudService.getProvider();
					break;
				}
			}
		}
		return result;
	}

}
