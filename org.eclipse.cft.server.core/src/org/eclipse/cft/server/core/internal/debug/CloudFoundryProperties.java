/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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
 *     Keith Chong, IBM - Add additional properties for enabling commands based on org.eclipse.ui.menus
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.debug;

import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerUtil;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.core.resources.IProject;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * Defines properties used to test various aspects of debugging an application.
 * Each property should implement an property testing method. The property name
 * must exactly match the defined property name in the property tester extension
 * point.
 * 
 * @author Nieraj Singh
 * 
 */
public enum CloudFoundryProperties {

	isServerSupported {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
			if (cloudFoundryServer != null) {
				return CloudServerUtil.isCloudFoundryServer(cloudFoundryServer.getServer());
			}
			return false;
		}
	},

	isServerStarted {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
			if (cloudFoundryServer != null) {
				IServer server = cloudFoundryServer.getServer();
				if (server != null) {
					return (server.getServerState() == IServer.STATE_STARTED);
				}
			}
			return false;
		}
	},

	isServerStopped {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
			if (cloudFoundryServer != null) {
				IServer server = cloudFoundryServer.getServer();
				if (server != null) {
					return (server.getServerState() == IServer.STATE_STOPPED);
				}
			}
			return false;
		}
	},

	isModuleStopped {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {

			return getDeployedAppState(modules, cloudFoundryServer.getServer()) == IServer.STATE_STOPPED;
		}
	},

	isModuleStarted {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
			return getDeployedAppState(modules, cloudFoundryServer.getServer()) == IServer.STATE_STARTED;
		}
	},

	isCloudModuleStarted {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
			return isModuleStarted.testProperty(modules, cloudFoundryServer);
		}
	},

	isCloudModuleLocal {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
			if (modules != null && modules.length > 0) {

				// Selection is limited to one module
				CloudFoundryApplicationModule cloudModule = cloudFoundryServer.getExistingCloudModule(modules[0]);
				if (cloudModule != null) {
					return !cloudModule.isExternal();
				}
			}
			return false;
		}
	},

	/**
	 * Determines is the module's workspace project is accessible. True if it
	 * is. False, if the associated workspace project is inaccessible or cannot
	 * be resolved
	 */
	isModuleProjectAccessible {
		public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {

			if (modules == null || modules.length == 0) {
				return false;
			}

			IProject project = modules[0].getProject();
			return project != null && project.isAccessible();

		}
	};

	// Should be overridden by each property
	public boolean testProperty(IModule[] modules, CloudFoundryServer cloudFoundryServer) {
		return false;
	}

	protected static int getDeployedAppState(IModule[] modules, IServer server) {
		return modules != null && modules.length > 0 ? server.getModuleState(modules) : -1;
	}
}