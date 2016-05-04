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

import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.ui.internal.CloudServerSpacesDelegate;
import org.eclipse.cft.server.ui.internal.CFUiUtil;
import org.eclipse.cft.server.ui.internal.ICoreRunnable;
import org.eclipse.cft.server.ui.internal.ServerDescriptor;
import org.eclipse.cft.server.ui.internal.ServerHandler;
import org.eclipse.cft.server.ui.internal.ServerHandlerCallback;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IServerWorkingCopy;

/**
 * Creates a new server instance from a selected space in an organization and
 * spaces viewer in the wizard. It only creates the server instance if another
 * server instance to that space does not exist.
 */
public class OrgsAndSpacesWizard extends Wizard {

	private final CloudFoundryServer cloudServer;

	private CloneServerPage cloudSpacePage;

	public OrgsAndSpacesWizard(CloudFoundryServer server) {
		this.cloudServer = server;
		setWindowTitle(server.getServer().getName());
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {

		cloudSpacePage = new CloneServerPage(cloudServer);
		cloudSpacePage.setWizard(this);
		addPage(cloudSpacePage);
	}

	@Override
	public boolean performFinish() {

		final CloudSpace selectedSpace = cloudSpacePage.getSelectedCloudSpace();

		// Only create a new space, if it doesnt match the existing space
		if (selectedSpace != null
				&& !CloudServerSpacesDelegate.matchesSpace(selectedSpace, cloudServer.getCloudFoundrySpace())) {

			String serverName = cloudSpacePage.getServerName();
			final ServerDescriptor descriptor = ServerDescriptor.getServerDescriptor(cloudServer, serverName);

			if (descriptor == null) {
				CloudFoundryPlugin.logError(NLS.bind(Messages.ERROR_NO_CLOUD_SERVER_DESCRIPTOR,
						cloudServer.getServerId()));
				return false;
			}
			else {
				final String password = cloudServer.getPassword();
				final String userName = cloudServer.getUsername();
				final String url = cloudServer.getUrl();
				final boolean selfSignedCert = cloudServer.getSelfSignedCertificate();

				CFUiUtil.runForked(new ICoreRunnable() {
					public void run(final IProgressMonitor monitor) throws CoreException {

						ServerHandler serverHandler = new ServerHandler(descriptor);

						serverHandler.createServer(monitor, ServerHandler.NEVER_OVERWRITE, new ServerHandlerCallback() {

							@Override
							public void configureServer(IServerWorkingCopy wc) throws CoreException {
								CloudFoundryServer cloudServer = (CloudFoundryServer) wc.loadAdapter(
										CloudFoundryServer.class, null);

								if (cloudServer != null) {
									cloudServer.setPassword(password);
									cloudServer.setUsername(userName);
									cloudServer.setUrl(url);
									cloudServer.setSpace(selectedSpace);
									cloudServer.setSelfSignedCertificate(selfSignedCert);
									cloudServer.saveConfiguration(monitor);
								}
							}
						});
					}
				}, this);
			}
		}
		return true;
	}
}
