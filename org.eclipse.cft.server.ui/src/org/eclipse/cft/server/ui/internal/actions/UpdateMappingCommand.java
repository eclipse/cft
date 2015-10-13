/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
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
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.actions;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

public abstract class UpdateMappingCommand extends ModuleCommand {

	@Override
	protected void run(CloudFoundryApplicationModule appModule, CloudFoundryServer cloudServer) {
		final ICloudFoundryOperation op = getCloudOperation(appModule, cloudServer);

		if (op != null) {
			Job job = new Job(NLS.bind(Messages.UPDATE_PROJECT_MAPPING, appModule.getDeployedApplicationName())) {
				protected IStatus run(IProgressMonitor monitor) {
					try {
						op.run(monitor);
					}
					catch (CoreException e) {
						CloudFoundryPlugin.logError(e);
						if(e.getStatus() != null) {
							return e.getStatus();
						}
						return Status.CANCEL_STATUS;
					}
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}
		else {
			CloudFoundryPlugin.logError("No operation resolved to run in this action"); //$NON-NLS-1$
		}
	}

	abstract protected ICloudFoundryOperation getCloudOperation(CloudFoundryApplicationModule appModule,
			CloudFoundryServer cloudServer);
}
