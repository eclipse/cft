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
 *     Elson Yuen, IBM - Improve logic in determining whether a server type is a Cloud Foundry Server
 *     IBM - Moving decoration to a non UI thread to avoid blocking the interface.
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal;

import java.util.List;

import org.eclipse.cft.server.core.AbstractCloudFoundryUrl;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.CloudServerListener;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.spaces.CloudFoundrySpace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.ui.internal.view.servers.ModuleServer;

/**
 * @author Christian Dupuis
 * @author Terry Denney
 * @author Steffen Pingel
 */
@SuppressWarnings("restriction")
public class CloudFoundryDecorator extends LabelProvider implements ILightweightLabelDecorator {

	private final CloudServerListener listener;

	public CloudFoundryDecorator() {
		this.listener = new CloudServerListener() {
			public void serverChanged(final CloudServerEvent event) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						LabelProviderChangedEvent labelEvent = new LabelProviderChangedEvent(CloudFoundryDecorator.this);
						fireLabelProviderChanged(labelEvent);
					}
				});
			}
		};
		ServerEventHandler.getDefault().addServerListener(listener);
	}

	public void decorate(Object element, final IDecoration decoration) {
		if (element instanceof ModuleServer) {
			ModuleServer moduleServer = (ModuleServer) element;
			IServer s = moduleServer.getServer();
			if (s != null && CloudServerUtil.isCloudFoundryServer(s)) {
				IModule[] modules = moduleServer.getModule();
				if (modules != null && modules.length == 1) {
					
                    ModuleDeploymentDecoration<IDecoration> deploymentDecoration = ModuleDeploymentDecoration.getServersViewDecoration();
					CloudFoundryServer server = getCloudFoundryServer(moduleServer.getServer());
					if (server == null || !server.isConnected()) {
						return;

					}
					CloudFoundryApplicationModule module = server.getExistingCloudModule(modules[0]);

					// module may no longer exist
					if (module == null) {
						return;
					}

					deploymentDecoration.decorateText(decoration, module);
					ImageDescriptor image = deploymentDecoration.getImageDecoration(module);
				
					if (image != null) {
						decoration.addOverlay(image, IDecoration.BOTTOM_LEFT);
					}	
				}
			}
		}
		else if (element instanceof Server) {
			Server server = (Server) element;
			if (CloudServerUtil.isCloudFoundryServer(server)) {
				final CloudFoundryServer cfServer = getCloudFoundryServer(server);
				if (cfServer != null && cfServer.getUsername() != null) {
					
					final List<AbstractCloudFoundryUrl> cloudUrls;
					try {
						cloudUrls = CloudServerUIUtil.getAllUrls(cfServer.getBehaviour().getServer()
								.getServerType().getId(), null, false);
					} catch (CoreException e1) {
						CloudFoundryServerUiPlugin.logError(e1);
						return;
					}
					
					// This now runs on a non UI thread, so we need to join this
					// update to a UI thread.
					Display.getDefault().syncExec(new Runnable() {
						
						@Override
						public void run() {
							// decoration.addSuffix(NLS.bind("  [{0}, {1}]",
							// cfServer.getUsername(), cfServer.getUrl()));
							if (cfServer.hasCloudSpace()) {
								CloudFoundrySpace clSpace = cfServer.getCloudFoundrySpace();
								if (clSpace != null) {
									decoration
											.addSuffix(NLS.bind(" - {0} - {1}", clSpace.getOrgName(), clSpace.getSpaceName())); //$NON-NLS-1$
								}
							}
							String url = cfServer.getUrl();
							// decoration.addSuffix(NLS.bind("  {0}",
							// cfServer.getUsername()));
							for (AbstractCloudFoundryUrl cloudUrl : cloudUrls) {
								if (cloudUrl.getUrl().equals(url)) {
									decoration.addSuffix(NLS.bind(" - {0}", cloudUrl.getUrl())); //$NON-NLS-1$
									break;
								}
							}
						}
					});
				}
			}
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		ServerEventHandler.getDefault().removeServerListener(listener);
	}

	private CloudFoundryServer getCloudFoundryServer(IServer server) {
		Object obj = server.getAdapter(CloudFoundryServer.class);
		if (obj instanceof CloudFoundryServer) {
			return (CloudFoundryServer) obj;
		}
		return null;
	}

	// private String getAppStateString(AppState state) {
	// if (state == AppState.STARTED) {
	// return "Started";
	// }
	// if (state == AppState.STOPPED) {
	// return "Stopped";
	// }
	// if (state == AppState.UPDATING) {
	// return "Updating";
	// }
	// return "unknown";
	// }

}
