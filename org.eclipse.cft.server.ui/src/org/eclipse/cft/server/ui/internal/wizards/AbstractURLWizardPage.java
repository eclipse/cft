/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.ui.internal.wizards;

import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.ui.internal.ICoreRunnable;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;

/**
 * 
 * This page handles the lookup of domains for application URLs. It fetches the
 * list of domains once per session, when the controls are made visible.
 */
public abstract class AbstractURLWizardPage extends PartsWizardPage {

	protected AbstractURLWizardPage(String pageName, String title, ImageDescriptor titleImage,
			ApplicationUrlLookupService urlLookup) {
		super(pageName, title, titleImage);
		this.urlLookup = urlLookup;
	}

	protected AbstractURLWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		this(pageName, title, titleImage, null);
	}

	protected boolean refreshedDomains = false;

	private ApplicationUrlLookupService urlLookup;

	protected ApplicationUrlLookupService getApplicationUrlLookup() {
		return urlLookup;
	}

	protected void refreshApplicationUrlDomains() {

		final ApplicationUrlLookupService urlLookup = getApplicationUrlLookup();
		if (urlLookup == null) {
			update(true,
					CloudFoundryPlugin
							.getStatus(
									Messages.AbstractURLWizardPage_ERROR_NO_URL_HANDLER,
									IStatus.ERROR));
			return;
		}

		final String operationLabel = Messages.AbstractURLWizardPage_LABEL_FETCHING_DOMAIN;
		ICoreRunnable runnable = new ICoreRunnable() {
			public void run(IProgressMonitor coreRunnerMonitor) throws CoreException {
				SubMonitor subProgress = SubMonitor.convert(coreRunnerMonitor, operationLabel, 100);
				try {
					urlLookup.refreshDomains(subProgress);
					refreshedDomains = true;
					// Must launch this again in the UI thread AFTER
					// the refresh occurs.
					Display.getDefault().syncExec(new Runnable() {

						public void run() {
							// Clear any info in the dialogue
							setMessage(null);

							domainsRefreshed();
						}

					});
				}
				finally {
					subProgress.done();
				}
			}
		};
		runAsynchWithWizardProgress(runnable, operationLabel);
	}

	/**
	 * UI callback after the domains have been successfully refreshed.
	 */
	abstract protected void domainsRefreshed();

}
