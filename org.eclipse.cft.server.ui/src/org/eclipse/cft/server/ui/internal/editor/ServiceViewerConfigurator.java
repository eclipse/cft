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

import org.eclipse.cft.server.ui.internal.ColumnSortListener;
import org.eclipse.cft.server.ui.internal.editor.ServiceViewColumn.ServiceViewColumnDescriptor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * Configures the services viewer providers, sorters and columns. Must be called
 * before setting the viewer input.
 * 
 */
public class ServiceViewerConfigurator {

	private boolean addAutomaticViewerResizing;

	public ServiceViewerConfigurator() {
		this.addAutomaticViewerResizing = false;

	}

	public ServiceViewerConfigurator enableAutomaticViewerResizing() {
		addAutomaticViewerResizing = true;
		return this;
	}

	/**
	 * This must be called before setting the viewer input
	 * @param tableViewer
	 */
	public void configureViewer(final TableViewer tableViewer) {

		final Table table = tableViewer.getTable();
		table.setHeaderVisible(true);

		int columnIndex = 0;
		ServiceViewColumnDescriptor descriptor = ServiceViewColumn.getServiceViewColumnDescriptor();

		ServiceViewColumn[] columns = descriptor != null ? descriptor.getServiceViewColumn() : null;

		if (columns == null) {
			return;
		}

		String[] columnProperties = new String[columns.length];
		TableColumn sortColumn = null;
		for (ServiceViewColumn column : columns) {
			columnProperties[columnIndex] = column.name();
			TableColumn tableColumn = new TableColumn(table, SWT.NONE, columnIndex++);
			tableColumn.setData(column);
			tableColumn.setText(column.name());
			tableColumn.setWidth(column.getWidth());
			tableColumn.addSelectionListener(new ColumnSortListener(tableViewer));

			if (column == ServiceViewColumn.Name) {
				sortColumn = tableColumn;
			}

		}

		// Add a control listener to resize the columns such that there is no
		// empty space
		// after the last column
		if (addAutomaticViewerResizing) {
			table.getParent().addControlListener(new ControlAdapter() {
				public void controlResized(ControlEvent e) {
					Composite tableComposite = tableViewer.getTable().getParent();
					Rectangle tableCompositeArea = tableComposite.getClientArea();
					int width = tableCompositeArea.width;
					resizeTableColumns(width, table);
				}
			});
		}

		tableViewer.setColumnProperties(columnProperties);

		if (sortColumn != null) {
			table.setSortColumn(sortColumn);
			table.setSortDirection(SWT.UP);
		}
	}

	protected void resizeTableColumns(int tableWidth, Table table) {
		TableColumn[] tableColumns = table.getColumns();

		if (tableColumns.length == 0) {
			return;
		}

		int total = 0;

		// resize only if there is empty space at the end of the table
		for (TableColumn column : tableColumns) {
			total += column.getWidth();
		}

		if (total < tableWidth) {
			// resize the last one
			TableColumn lastColumn = tableColumns[tableColumns.length - 1];
			int newWidth = (tableWidth - total) + lastColumn.getWidth();
			lastColumn.setWidth(newWidth);
		}

	}

}
