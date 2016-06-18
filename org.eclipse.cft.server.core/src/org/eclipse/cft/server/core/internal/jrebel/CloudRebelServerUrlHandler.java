/*******************************************************************************
 * Copyright (c) 2016 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.core.internal.jrebel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.jrebel.ReflectionHandler.ReflectionErrorHandler;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.osgi.framework.Bundle;

public class CloudRebelServerUrlHandler implements CFRebelServerUrlHandler {

	@Override
	public boolean updateUrls(CloudFoundryServer cloudServer, int eventType, IModule module, List<String> oldUrls,
			List<String> currentUrls, IProgressMonitor monitor) {

		ReflectionHandler reflectionHandler = JRebelIntegrationUtility.createReflectionHandler();
		return new ServerUrlReflectionHandler(module, cloudServer, oldUrls, currentUrls, reflectionHandler)
				.doUrlUpdate();
	}

	public class ServerUrlReflectionHandler implements ReflectionErrorHandler {
		private final IModule module;

		private final CloudFoundryServer cloudServer;

		private final List<String> oldUrls;

		private final List<String> currentUrls;

		private final ReflectionHandler reflectionHandler;

		public ServerUrlReflectionHandler(IModule module, CloudFoundryServer cloudServer, List<String> oldUrls,
				List<String> currentUrls, ReflectionHandler reflectionHandler) {
			Assert.isNotNull(reflectionHandler,
					"JRebel Cloud Foundry Tools Integration reflection handler cannot be null"); //$NON-NLS-1$
			this.module = module;
			this.cloudServer = cloudServer;
			this.oldUrls = oldUrls;
			this.currentUrls = currentUrls;
			this.reflectionHandler = reflectionHandler;
			this.reflectionHandler.addErrorHandler(this);
		}

		private boolean doReflectionUpdateUrls(Bundle bundle, boolean updated, Class<?> providerClass)
				throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
			Method getRemotingProject = reflectionHandler.getRemotingProject(providerClass);

			if (getRemotingProject == null) {
				return false;
			}

			getRemotingProject.setAccessible(true);

			IProject project = module.getProject();
			Object remoteProjectObj = getRemotingProject.invoke(null, project);

			if (JRebelIntegrationUtility.isRemotingProject(remoteProjectObj)) {

				Class<?> integrationClass = reflectionHandler.getJRebelIntegration(bundle);

				if (integrationClass == null) {
					return false;
				}

				updated = removeUrls(updated, integrationClass);
				updated = addUrls(updated, integrationClass);
			}

			if (!updated) {
				CFRebelConsoleUtil.printToConsole(module, cloudServer,
						Messages.CFRebelServerIntegration_NO_URL_UPDATES_PERFORMED);
			}

			return updated;
		}

		private boolean addUrls(boolean updated, Class<?> integrationClass) {
			if (currentUrls != null && !currentUrls.isEmpty()) {

				Method addServerUrlsMethod = reflectionHandler.getAddServerUrlMethod(integrationClass);

				if (addServerUrlsMethod != null) {

					Map<String, URI> serverUrls = new HashMap<String, URI>();
					for (String url : currentUrls) {
						if (url != null) {
							URI uri = asUri(url);
							if (uri != null) {
								serverUrls.put(uri.toString(), uri);
							}
						}
					}

					if (reflectionHandler.addServerUrl(addServerUrlsMethod, serverUrls)) {
						updated = true;
						CFRebelConsoleUtil.printToConsole(module, cloudServer,
								NLS.bind(Messages.CFRebelServerIntegration_UPDATED_URL, currentUrls));
					}
				}
			}
			return updated;
		}

		private boolean removeUrls(boolean updated, Class<?> integrationClass) {
			if (oldUrls != null && !oldUrls.isEmpty()) {
				Collection<String> toRemove = toRemove(oldUrls, currentUrls);

				if (!toRemove.isEmpty()) {
					Method removeServerUrlsMethod = reflectionHandler.getRemoveServerUrlMethod(integrationClass);

					if (removeServerUrlsMethod != null) {

						List<URI> urisToRemove = new ArrayList<URI>();
						for (String url : toRemove) {
							if (url != null) {
								URI uri = asUri(url);
								if (uri != null) {
									urisToRemove.add(uri);
								}
							}
						}

						if (reflectionHandler.removeServerUrl(removeServerUrlsMethod, urisToRemove)) {
							CFRebelConsoleUtil.printToConsole(module, cloudServer,
									NLS.bind(Messages.CFRebelServerIntegration_REMOVED_URL, toRemove));
							updated = true;
						}
					}
				}
			}
			return updated;
		}

		protected Collection<String> toRemove(List<String> old, List<String> current) {
			Set<String> toRemove = new HashSet<String>();
			if (old != null) {
				if (current == null) {
					toRemove.addAll(old);
				}
				else {
					for (String url : old) {
						if (!current.contains(url)) {
							toRemove.add(url);
						}
					}
				}
			}
			return toRemove;
		}

		public boolean doUrlUpdate() {
			Bundle bundle = JRebelIntegrationUtility.getJRebelBundle();
			boolean updated = false;
			Throwable error = null;

			if (bundle != null) {
				updated = true;
				try {

					Class<?> providerClass = reflectionHandler.getRebelRemotingProvider(bundle);

					if (providerClass != null) {
						updated = doReflectionUpdateUrls(bundle, updated, providerClass);
					}
				}
				catch (SecurityException e) {
					error = e;
				}
				catch (NoSuchMethodException e) {
					error = e;
				}
				catch (IllegalAccessException e) {
					error = e;
				}
				catch (InvocationTargetException e) {
					error = e;
				}
				catch (IllegalArgumentException e) {
					error = e;
				}

			}

			if (error != null) {
				CFRebelConsoleUtil.printErrorToConsole(module, cloudServer, error.getMessage());
				CloudFoundryPlugin.logError(error.getMessage(), error);
			}
			return updated;
		}

		protected URI asUri(String url) {
			URI uri = null;
			try {
				uri = new URI(url);
			}
			catch (URISyntaxException e) {
				CloudFoundryPlugin.logError(e);
			}
			return uri;
		}

		@Override
		public void errorLoading(String memberId, String containerId, Throwable t) {
			String errorMessage = NLS.bind(Messages.CFRebelServerIntegration_ERROR_INCOMPATIBLE_JREBEL, memberId,
					containerId);
			CFRebelConsoleUtil.printErrorToConsole(module, cloudServer, errorMessage);
			CloudFoundryPlugin.logError(errorMessage, t);
		}
	}

}
