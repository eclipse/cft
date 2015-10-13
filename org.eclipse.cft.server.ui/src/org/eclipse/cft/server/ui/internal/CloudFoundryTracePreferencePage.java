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

import org.eclipse.cft.server.core.internal.log.HttpTracer;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page to enable/disable Cloud Foundry HTTP verbose tracing.
 */
public class CloudFoundryTracePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private boolean isTracingEnabled;

	public CloudFoundryTracePreferencePage() {
		setPreferenceStore(CloudFoundryServerUiPlugin.getDefault().getPreferenceStore());
	}

	public void init(IWorkbench workbench) {
		// Do nothing
	}

	@Override
	protected Control createContents(Composite parent) {

		Composite topComposite = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(topComposite);
		GridDataFactory.fillDefaults().grab(true, true);

		final Button enableTracing = new Button(topComposite, SWT.CHECK);
		GridDataFactory.fillDefaults().grab(false, false);
		enableTracing.setText(Messages.LABEL_ENABLE_TRACING);
		enableTracing.setToolTipText(Messages.TOOLTIP_ENABLE_TRACING);

		isTracingEnabled = HttpTracer.getCurrent().isEnabled();

		enableTracing.setSelection(isTracingEnabled);

		enableTracing.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				isTracingEnabled = enableTracing.getSelection();
			}

		});

		return topComposite;
	}

	@Override
	protected void performApply() {
		HttpTracer.getCurrent().enableTracing(isTracingEnabled);
		super.performApply();
	}

	@Override
	public boolean performOk() {
		HttpTracer.getCurrent().enableTracing(isTracingEnabled);
		return super.performOk();
	}
}
