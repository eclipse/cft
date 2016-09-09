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
package org.eclipse.cft.server.ui.internal;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.core.internal.application.ApplicationRunState;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;

/**
 * Decorates the label of a module based on deployment and runstate information
 * of the associated application.
 * </p>
 * This decoration contains common code that can be invoked in different types
 * of {@link ILabelProvider} or any other form of labeling, especially label
 * providers that are distinct from one another(e.g. a label provider for a
 * {@link TableViewer} vs a view that uses decoration management and accepts
 * {@link ILightweightLabelDecorator})
 * @param <T> the decoration that needs to be modified (e.g. a StringBuffer)
 */
public abstract class ModuleDeploymentDecoration<T> {

	public void decorateText(T decoration, CloudFoundryApplicationModule module) {
		CloudApplication application = module != null ? module.getApplication() : null;
	
		if (application != null && module != null) {
			String deployedAppName = application.getName();
			IModule localModule = module.getLocalModule();
			// Only show "Deployed as" when the local module name does not
			// match the deployed app name.
			if (localModule != null && !localModule.getName().equals(deployedAppName)) {
				append(decoration, NLS.bind(Messages.CloudFoundryDecorator_SUFFIX_DEPLOYED_AS, deployedAppName));
			}
			else {
				append(decoration, Messages.CloudFoundryDecorator_SUFFIX_DEPLOYED);
			}
			decorateRunState(decoration, module);
		}
		else {
			append(decoration, Messages.CloudFoundryDecorator_SUFFIX_NOT_DEPLOYED);
		}
	}

	public ImageDescriptor getImageDecoration(CloudFoundryApplicationModule module) {
		if (module != null && module.getStatus() != null && !module.getStatus().isOK()) {
			if (module.getStatus().getSeverity() == IStatus.ERROR) {
				return CloudFoundryImages.OVERLAY_ERROR;
			}
			else if (module.getStatus().getSeverity() == IStatus.WARNING) {
				return CloudFoundryImages.OVERLAY_WARNING;
			}
		}
		return null;
	}

	protected abstract void append(T decoration, String message);

	protected void decorateRunState(T decoration, CloudFoundryApplicationModule appModule) {
		if (appModule != null) {
			ApplicationRunState runState = appModule.getRunState();
			append(decoration, " [" //$NON-NLS-1$
			+ runState.getLabel()
			+ "]"); //$NON-NLS-1$
		}
	}

	/*
	 * 
	 * 
	 * Concrete implementations for different decoration cases.
	 * 
	 * 
	 *
	 */

	private static class ServersViewDecoration extends ModuleDeploymentDecoration<IDecoration> {

		@Override
		protected void append(IDecoration decoration, String message) {
			decoration.addSuffix(message);
		}

		protected void decorateRunState(IDecoration decoration, CloudFoundryApplicationModule appModule) {
			// Do nothing. WTP adds its own runstate decoration.
		}
	}

	private static class ModuleTextDecoration extends ModuleDeploymentDecoration<StringBuffer> {

		@Override
		protected void append(StringBuffer decoration, String message) {
			decoration.append(message);
		}
	}

	/**
	 * 
	 * @return non-null module deployment decorator that decorates Servers view
	 */
	public static ModuleDeploymentDecoration<IDecoration> getServersViewDecoration() {
		return new ServersViewDecoration();
	}

	/**
	 * 
	 * @return non-null module deployment decorator that decorates on a
	 * text-based decoration
	 */
	public static ModuleDeploymentDecoration<StringBuffer> getModuleTextDecoration() {
		return new ModuleTextDecoration();
	}
}