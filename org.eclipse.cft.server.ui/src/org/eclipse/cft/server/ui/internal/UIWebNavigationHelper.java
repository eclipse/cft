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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.internal.browser.WebBrowserPreference;
import org.eclipse.ui.progress.UIJob;

@SuppressWarnings("restriction")
public class UIWebNavigationHelper {

	private String label;

	private String location;

	public UIWebNavigationHelper(String location, String label) {
		super();
		this.location = location;
		this.label = label;

	}

	public String getLocation() {
		return location;
	}

	public void navigate() {
		UIJob job = new UIJob(label) {

			public IStatus runInUIThread(IProgressMonitor monitor) {
				CFUiUtil.openUrl(location);
				return Status.OK_STATUS;
			}
		};

		job.schedule();
	}

	public void navigateExternal() {
		UIJob job = new UIJob(label) {

			public IStatus runInUIThread(IProgressMonitor monitor) {
				CFUiUtil.openUrl(location, WebBrowserPreference.EXTERNAL);
				return Status.OK_STATUS;
			}
		};

		job.schedule();
	}
}
