/*******************************************************************************
 * Copyright (c) 2015, 2016 Pivotal Software, Inc.
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
package org.eclipse.cft.server.tests.core;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.tests.util.ModulesRefreshListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * Provides API for testing Cloud operations asynchronously as well as provides
 * a mechanism to test module refresh.
 *
 */
public abstract class AbstractAsynchCloudTest extends AbstractCloudFoundryTest {

	/**
	 * Runs the given operation asynchronously in a separate Job, and waits in
	 * the current thread for the refresh triggered by that operation to
	 * complete. This tests the CF tooling asynch, event-driven refresh module
	 * behaviour. When checking for the refresh to complete, it will also verify
	 * that the expected refresh event type is the event type that was actually
	 * received.
	 * @param runnable
	 * @param expectedAppName if not null, will use a module refresh listener
	 * listening for single module refresh. Otherwise if null listens to all
	 * modules refresh
	 * @param expectedRefreshEventType
	 * @throws CoreException
	 */
	protected void asynchExecuteApplicationOperation(final IRunnableWithProgress runnable, String expectedAppName,
			int expectedEvent) throws Exception {

		ModulesRefreshListener listener = getModulesRefreshListener(expectedAppName, cloudServer, expectedEvent);

		asynchExecuteOperation(runnable);

		assertModuleRefreshedAndDispose(listener, expectedEvent);
	}

	protected void asynchExecuteOperation(final IRunnableWithProgress runnable) {
		Job job = new Job("Running Cloud Operation") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					runnable.run(monitor);
				}
				catch (InvocationTargetException e) {
					e.printStackTrace();
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
				return Status.OK_STATUS;
			}

		};
		job.setPriority(Job.INTERACTIVE);
		job.schedule();
	}

	protected static ModulesRefreshListener getModulesRefreshListener(String appName, CloudFoundryServer cloudServer,
			int expectedEventType) {
		ModulesRefreshListener eventHandler = ModulesRefreshListener.getListener(appName, cloudServer,
				expectedEventType);
		assertFalse(eventHandler.hasBeenRefreshed());
		return eventHandler;
	}

	/**
	 * Waits for module refresh to complete and asserts that the given event was
	 * received. This blocks the thread.
	 * @param refreshHandler
	 * @param expectedEventType
	 * @throws CoreException
	 */
	protected static void assertModuleRefreshedAndDispose(ModulesRefreshListener refreshHandler, int expectedEventType)
			throws CoreException {
		assertTrue(refreshHandler.modulesRefreshed(new NullProgressMonitor()));
		assertTrue(refreshHandler.hasBeenRefreshed());
		assertEquals(expectedEventType, refreshHandler.getMatchedEvent().getType());
		refreshHandler.dispose();
	}

	protected CloudFoundryApplicationModule deployApplicationWithModuleRefresh(String appName, IProject project, boolean startApp,
			String buildpack) throws Exception {

		ModulesRefreshListener listener = ModulesRefreshListener.getListener(appName, cloudServer,
				CloudServerEvent.EVENT_APP_DEPLOYMENT_CHANGED);
		
		CloudFoundryApplicationModule appModule = deployApplication(appName, project, startApp, buildpack);

		assertModuleRefreshedAndDispose(listener, CloudServerEvent.EVENT_APP_DEPLOYMENT_CHANGED);
		return appModule;
	}

}
