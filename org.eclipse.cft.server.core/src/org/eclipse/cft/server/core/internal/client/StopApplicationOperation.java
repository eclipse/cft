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
 *  Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.client;

import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.cft.server.core.internal.application.ModuleChangeEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;

@SuppressWarnings("restriction")
class StopApplicationOperation extends AbstractPublishApplicationOperation {

	/**
	 * 
	 */

	protected StopApplicationOperation(CloudFoundryServerBehaviour behaviour, IModule[] modules) {
		super(behaviour, modules);
	}

	@Override
	public String getOperationName() {
		return Messages.StopApplicationOperation_STOPPING_APP;
	}

	@Override
	protected void doApplicationOperation(IProgressMonitor monitor) throws CoreException {
		Server server = (Server) getBehaviour().getServer();

		boolean succeeded = false;
		try {
			server.setModuleState(getModules(), IServer.STATE_STOPPING);

			CloudFoundryServer cloudServer = getBehaviour().getCloudFoundryServer();

			final CloudFoundryApplicationModule cloudModule = cloudServer.getExistingCloudModule(getModule());

			if (cloudModule == null) {
				throw CloudErrorUtil.toCoreException("Unable to stop application as no cloud module found for: " //$NON-NLS-1$
						+ getModules()[0].getName());
			}

			SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

			String stoppingApplicationMessage = NLS.bind(Messages.CONSOLE_STOPPING_APPLICATION,
					cloudModule.getDeployedApplicationName());

			getBehaviour().clearAndPrintlnConsole(cloudModule, stoppingApplicationMessage);

			subMonitor.worked(20);

			getBehaviour().getRequestFactory().stopApplication(stoppingApplicationMessage, cloudModule)
					.run(subMonitor.newChild(20));

			server.setModuleState(getModules(), IServer.STATE_STOPPED);
			succeeded = true;

			ServerEventHandler.getDefault()
					.fireServerEvent(new ModuleChangeEvent(getBehaviour().getCloudFoundryServer(),
							CloudServerEvent.EVENT_APP_STOPPED, cloudModule.getLocalModule(), Status.OK_STATUS));

			// Update the module
			getBehaviour().updateCloudModuleWithInstances(cloudModule.getDeployedApplicationName(),
					subMonitor.newChild(40));

			getBehaviour().printlnToConsole(cloudModule, Messages.CONSOLE_APP_STOPPED);
			CloudFoundryPlugin.getCallback().stopApplicationConsole(cloudModule, cloudServer);
			subMonitor.worked(20);
		}
		finally {
			if (!succeeded) {
				server.setModuleState(getModules(), IServer.STATE_UNKNOWN);
			}
		}

	}

}