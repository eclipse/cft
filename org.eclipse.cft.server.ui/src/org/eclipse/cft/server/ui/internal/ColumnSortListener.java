/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class ColumnSortListener extends SelectionAdapter {

	private final TableViewer viewer;

	public ColumnSortListener(TableViewer viewer) {
		this.viewer = viewer;
	}

	public void widgetSelected(SelectionEvent e) {
		if (e.widget instanceof TableColumn) {
			TableColumn selected = (TableColumn) e.widget;
			Table table = viewer.getTable();
			TableColumn current = table.getSortColumn();

			int newDirection = SWT.UP;
			// If selecting a different column, keep the ascending
			// direction as default. Only switch
			// directions if the same column has been selected.
			if (current == selected) {
				newDirection = table.getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP;
			}
			else {
				table.setSortColumn(selected);
			}
			table.setSortDirection(newDirection);
			refresh();
		}
	}

	protected void refresh() {
		viewer.refresh();
	}
}