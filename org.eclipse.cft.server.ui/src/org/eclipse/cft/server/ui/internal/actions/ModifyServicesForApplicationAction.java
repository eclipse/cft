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
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.client.CFServiceInstance;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.core.internal.client.DeploymentInfoWorkingCopy;
import org.eclipse.cft.server.core.internal.client.ICloudFoundryOperation;
import org.eclipse.cft.server.ui.internal.editor.CloudFoundryApplicationsEditorPage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Action for modifying list of services for an application. Subclasses are
 * responsible for defining the list of services to add or to remove.
 * @author Terry Denney
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public abstract class ModifyServicesForApplicationAction extends EditorAction {

	private CloudFoundryApplicationModule appModule;

	public ModifyServicesForApplicationAction(CloudFoundryApplicationModule appModule,
			CloudFoundryServerBehaviour serverBehaviour, CloudFoundryApplicationsEditorPage editorPage) {
		super(editorPage, RefreshArea.DETAIL);

		this.appModule = appModule;
	}

	abstract public List<String> getServicesToAdd();

	abstract public List<String> getServicesToRemove();

	protected void setApplicationModule(CloudFoundryApplicationModule appModule) {
		this.appModule = appModule;
	}

	@Override
	protected ICloudFoundryOperation getOperation(IProgressMonitor monitor) throws CoreException {
		List<String> existingServices = null;

		final List<String> updatedServices = new ArrayList<String>();

		DeploymentInfoWorkingCopy workingCopy = appModule.resolveDeploymentInfoWorkingCopy(monitor);

		// Check the deployment information to see if it has an existing list of
		// bound services.
		existingServices = workingCopy.asServiceBindingList();

		// Must iterate rather than passing to constructor or using
		// addAll, as some
		// of the entries in existing services may be null.
		if (existingServices != null) {
			for (String existingService : existingServices) {
				if (existingService != null) {
					updatedServices.add(existingService);
				}
			}
		}

		// This leads to duplicate services, as a user could drop an existing
		// service already
		// added to an application
		boolean serviceChanges = false;
		List<String> servicesToAdd = getServicesToAdd();
		for (String serviceToAdd : servicesToAdd) {
			if (!updatedServices.contains(serviceToAdd)) {
				updatedServices.add(serviceToAdd);
				serviceChanges = true;
			}
		}

		serviceChanges |= updatedServices.removeAll(getServicesToRemove());

		if (serviceChanges) {
			return new ModifyServicesForApplicationActionOperation(updatedServices, workingCopy);
		}
		
		return null;
	}

	@Override
	protected boolean shouldLogException(CoreException e) {
		return !CloudErrorUtil.isNotFoundException(e);
	}

	public static List<String> getServiceNames(IStructuredSelection selection) {
		Object[] objects = selection.toArray();
		List<String> services = new ArrayList<String>();

		for (Object object : objects) {
			if (object instanceof CFServiceInstance) {
				services.add(((CFServiceInstance) object).getName());
			}
		}
		return services;
	}

	public static List<CFServiceInstance> getServices(IStructuredSelection selection) {
		Object[] objects = selection.toArray();
		List<CFServiceInstance> services = new ArrayList<CFServiceInstance>();

		for (Object object : objects) {
			if (object instanceof CFServiceInstance) {
				services.add(((CFServiceInstance) object));
			}
		}
		return services;
	}

	/** Attempt to bind the services to the application; if it succeeds, then update the services list. */
	private class ModifyServicesForApplicationActionOperation implements ICloudFoundryOperation {

		/** Copy of the updated deployment info, which will be saved on success.*/
		final DeploymentInfoWorkingCopy workingCopy;
		
		/** List of services to add to the application*/
		final List<String> updatedServices;
		
		public ModifyServicesForApplicationActionOperation(List<String> updatedServices, DeploymentInfoWorkingCopy workingCopy) {
			this.updatedServices = updatedServices;
			this.workingCopy = workingCopy;
		}
		

		@Override
		public void run(IProgressMonitor monitor) throws CoreException {
			// Save the changes even if an app is not deployed
			List<CFServiceInstance> boundServices = new ArrayList<CFServiceInstance>();
			for (String serName : updatedServices) {
				boundServices.add(new CFServiceInstance(serName));
			}

			if (appModule.getApplication() != null) {
				// update services right away, if app is already deployed
				ICloudFoundryOperation bindOp = getBehaviour().operations().bindServices(appModule, updatedServices);
				bindOp.run(monitor); // This will throw an exception on bind failure.
			}
			// Save the workingCopy after the bind operation, otherwise the WC may contain a service that failed to bind.
			workingCopy.setServices(boundServices);
			workingCopy.save();
		}
	}
}


