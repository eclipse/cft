/*******************************************************************************
 * Copyright (c) 2014, 2017 Pivotal Software, Inc. and others
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.wst.server.core.IModule;

/**
 * An operation performed in a {@link CloudFoundryServerBehaviour} target Cloud
 * space on modules.
 */
@SuppressWarnings({ "deprecation" })
public abstract class ModulesOperation extends CFOperation {

	private final IModule[] modules;

	public ModulesOperation(CloudFoundryServerBehaviour behaviour, IModule[] modules) {
		super(behaviour);
		this.modules = modules;
	}

	public ModulesOperation(CloudFoundryServerBehaviour behaviour, IModule module) {
		super(behaviour);
		this.modules = new IModule[] {module};
	}

	/**
	 * Run the operation on a module that has been verified to exist
	 * @param monitor
	 * @throws CoreException
	 */
	abstract protected void runOnVerifiedModule(IProgressMonitor monitor) throws CoreException;

	public final void run(IProgressMonitor monitor) throws CoreException {
		// The above single argument run(...) method is used by operations whose
		// monitor did not originate from a ServerDelegate publish job.
		run(monitor, -1);
	}

	/**
	 * Execute the specified operation on the current thread.
	 * @param monitor
	 * @param monitorParentWorkedSize If the monitor has already had beginTask
	 * called, as is the case when this is called from publishModule(...), then
	 * we need to convert it to something our SubMonitor can use.
	 * @throws CoreException
	 */
	public final void run(IProgressMonitor monitor, int monitorParentWorkedSize) throws CoreException {

		// monitorParentWorkedSize will be > 0 if the 'monitor' argument
		// originated from ServerDelegate.
		if (monitorParentWorkedSize > -1) {
			monitor = convertAndConsumeParentSubProgressMonitor(monitor, monitorParentWorkedSize);
		}

		if (clearFirstModuleStatus()) {
			CloudFoundryApplicationModule appModule = getCloudModule(getFirstModule());
			if (appModule != null) {
				// Clear the module status to remove any obsolete errors
				appModule.setStatus(null);
			}
		}
	
		try {
			runOnVerifiedModule(monitor);
		}
		catch (CoreException e) {
			CloudFoundryApplicationModule appModule = getCloudModule(getFirstModule());
			// always check that the CFAM is present before setting any new status. E.g
			// if an operation deletes a module, then the CFAM will not be present
			if (appModule != null) {
				appModule.setAndLogErrorStatus(e, getOperationName());
			} else {
				logNonModuleError(e);
			}
			throw e;
		}
	}

	/**
	 * 
	 * @return true if module status should be cleared before running the operation. False otherwise
	 */
	protected boolean clearFirstModuleStatus() {
		return true;
	}
	
	protected IModule[] getModules() {
		return modules;
	}

	public IModule getFirstModule() {
		return modules[0];
	}

	private static IProgressMonitor convertAndConsumeParentSubProgressMonitor(IProgressMonitor monitor,
			int parentWorkedSize) {
		// The parentWorked argument matches the value in
		// ServerBehaviourDelegate.publishModule(...).
		// ServerBehaviourDelegate.publishModule() passes us a monitor upon
		// which beginTask(1000) has
		// already been called.

		// Since this operation (ApplicationOperation and child classes) fully
		// contains all the required work,
		// we consume all of it as a new monitor, to be used by SubMonitor in
		// child code.
		return new SubProgressMonitor(monitor, parentWorkedSize);
	}
}
