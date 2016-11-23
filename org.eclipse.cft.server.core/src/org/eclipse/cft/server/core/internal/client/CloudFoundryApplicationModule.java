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
package org.eclipse.cft.server.core.internal.client;

import java.util.List;

import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.AppState;
import org.cloudfoundry.client.lib.domain.InstanceState;
import org.cloudfoundry.client.lib.domain.InstanceStats;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.eclipse.cft.server.core.AbstractApplicationDelegate;
import org.eclipse.cft.server.core.ApplicationDeploymentInfo;
import org.eclipse.cft.server.core.ICloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudUtil;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.cft.server.core.internal.application.ApplicationRegistry;
import org.eclipse.cft.server.core.internal.application.ApplicationRunState;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.internal.ExternalModule;

/**
 * 
 * Representation of an application that either already exists in a Cloud
 * Foundry server, or is about to be pushed to a server, and therefore may wrap
 * around both {@link CloudApplication} and {@link IModule}.
 * <p/>
 * IMPORTANT NOTE: This is NOT a stateless handle to the application in the
 * Cloud target and does NOT provide up-to-date information from the Cloud, but
 * rather a local model representation of a possible Cloud application that, for
 * example, can be used in cache-based mechanisms. Therefore two different Cloud
 * modules that reference the same Cloud application may not be the same.
 * Additional external update or synchronising mechanisms are required such that
 * up-to-date modules are provided to components that request them. Also, Cloud
 * modules may NOT necessarily model an existing Cloud application. They may
 * also used to indicate possible Cloud applications that do not yet, or no
 * longer, exist in a Cloud target.
 * <p/>
 * The Cloud module contains additional Cloud Foundry information like a
 * deployment information ( {@link ApplicationDeploymentInfo} ), application
 * stats, instances, and staging that is not available from the local WST
 * {@link IModule}. Likewise, it contains information that is not available
 * through the {@link CloudApplication}: for example, if the application is
 * linked to an existing IProject, this information can be accessed through the
 * wrapped IModule.
 * 
 * <p/>
 * Important points to note:
 * <p/>
 * 1. A Cloud module can be external, meaning that it has not been associated
 * with any other known module type (e.g jst.web). This may typically be the
 * case of existing applications in a Cloud whose link to a workspace project
 * cannot be determined, or whose module type cannot be resolved from
 * information provided purely from the Cloud.
 * <p/>
 * 2. A Cloud module can also be linked to a local workspace project via the
 * wrapped {@link IModule}. To obtain the associated {@link IProject} for a
 * Cloud module, call {@link #getLocalModule()} and fetch the IProject from the
 * local module API. External modules may not have an associated
 * {@link IProject}.
 * <p/>
 * 3. A "local module" obtained {@link #getLocalModule()} is the wrapped WST
 * module. For external modules the "local module" may be the same as the
 * external module, since external modules do NOT have a resolved wrapped
 * IModule of other types (like jst.web). Therefore, for external modules the
 * local module itself may in fact be a {@link CloudFoundryApplicationModule}
 * type.
 * <p/>
 * 4. Cloud modules that wrap around a {@link CloudApplication}, obtained via
 * {@link #getApplication()}, are considered to exist in a Cloud server. They
 * may not necessarily be considered "deployed" from the {@link IServer} point
 * of view as publish state of the module in the {@link IServer} is also a
 * factor. However, in general, to check if the module exists in the Cloud
 * target, a non-null check on {@link #getApplication()} is sufficient to
 * determine its existence, keeping in mind that Cloud modules are not handles,
 * and the {@link CloudApplication} obtained from the module may be out of date.
 * <p/>
 * 5. The actual Cloud application name of this Cloud-aware module may differ
 * from the underlying wrapped {@link IModule}. For example, when there is a
 * link from a Cloud application to a local IProject whose names are different.
 * To obtain the local WST module name, use {@link #getName()} or get it through
 * {@link #getLocalModule()}, although the latter may be null if no IModule
 * mapping has been created and linked by the framework.
 * <p/>
 * To obtain the deployed application name, use
 * {@link #getDeployedApplicationName()}.
 * <p/>
 * The Cloud module may be shared in a multi-threaded environment therefore when
 * adding new API care should be taken if access needs to be synchronized.
 * <p/>
 * The app module also contains a deployment information (
 * {@link ApplicationDeploymentInfo} ), which describes deployment properties of
 * the application (e.g., URLs, memory settings, etc..), as well as services
 * that are bound, or will be bound, to the application.
 * <p/>
 * If the application already exists in the Cloud target (i.e. has a
 * corresponding {@link CloudApplication}), the deployment information is kept
 * in synch any time the module mapping to a {@link CloudApplication} is
 * changed.
 * 
 * IMPORTANT NOTE: This class can be referred by the branding extension from
 * adopter so this class should not be moved or renamed to avoid breakage to
 * adopters.
 * 
 * @author Nieraj Singh
 * @author Christian Dupuis
 * @author Terry Denney
 * @author Leo Dos Santos
 * @author Steffen Pingel
 */
@SuppressWarnings("restriction")
public class CloudFoundryApplicationModule extends ExternalModule implements ICloudFoundryApplicationModule {

	public static String APPLICATION_STATE_DEPLOYABLE = Messages.CloudFoundryApplicationModule_STATE_DEPLOYABLE;

	public static String APPLICATION_STATE_DEPLOYED = Messages.CloudFoundryApplicationModule_STATE_DEPLOYED;

	public static String APPLICATION_STATE_UPLOADING = Messages.CloudFoundryApplicationModule_STATE_UPLOADING;

	public static String DEPLOYMENT_STATE_LAUNCHED = Messages.CloudFoundryApplicationModule_STATE_LAUNCHED;

	public static String DEPLOYMENT_STATE_LAUNCHING = Messages.CloudFoundryApplicationModule_STATE_LAUNCHING;

	public static String DEPLOYMENT_STATE_STARTING_SERVICES = Messages.CloudFoundryApplicationModule_STATE_STARTING_SERVICES;

	public static String DEPLOYMENT_STATE_STOPPED = Messages.CloudFoundryApplicationModule_STATE_STOPPED;

	public static String DEPLOYMENT_STATE_STOPPING = Messages.CloudFoundryApplicationModule_STATE_STOPPING;

	public static String DEPLOYMENT_STATE_WAITING_TO_LAUNCH = Messages.CloudFoundryApplicationModule_STATE_WAITING_TO_LAUNCH;

	private static final String MODULE_ID = "org.eclipse.cft.server.core.CloudFoundryApplicationModule"; //$NON-NLS-1$

	private static final String MODULE_VERSION = "1.0"; //$NON-NLS-1$

	private CloudApplication application;

	private String deployedAppName;

	private ApplicationStats applicationStats;

	private InstancesInfo instancesInfo;

	/*
	 * Created lazily, and its creation is an indicator of a valid module. For example, module would be valid 
	 * only when the actual existing Cloud application is set in the module (i.e., association established between the two), or the info is completed
	 * externally and saved through a deployment info working copy (e.g., during initial deployment)
	 */
	private ApplicationDeploymentInfo deploymentInfo;

	private StartingInfo startingInfo;

	/** This moduleId indicates that 'this' should be returned when localModule is called. This occurs when
	 * a 'null' IModule parameter is passed to one of the constructors. */
	private final static String CFAM_MODULE_ID = "org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule";
	
	private String localModuleId;

	private final IServer server;

	private IStatus validationStatus;

	/**
	 * Creates a cloud module that has a corresponding local module. This should
	 * be used if there is an accessible workspace project for the deployed app
	 * (the presence of an IModule would indicate a possible accessible
	 * workspace resource for the application).
	 * @param module local module from the WST server. Must not be null.
	 * @param deployedApplicationName name of the deployed application. It may
	 * not match the local workspace project name, as users are allowed to
	 * specify a different deployment name when pushing an application. Must not
	 * be null
	 * @param server. Must not be null.
	 */
	public CloudFoundryApplicationModule(IModule module, String deployedApplicationName, IServer server) {
		this(module, deployedApplicationName, module.getName(), server);
	}

	/**
	 * Creates an external cloud module (a cloud module that corresponds to a
	 * deployed application with no accessible workspace project).
	 * @param deployedApplicationName. Must not be null.
	 * @param server. Must not be null.
	 */
	public CloudFoundryApplicationModule(String deployedApplicationName, IServer server) {
		this(null, deployedApplicationName, deployedApplicationName, server);
	}

	protected CloudFoundryApplicationModule(IModule module, String deployedApplicationName, String localName,
			IServer server) {
		super(localName, localName, MODULE_ID, MODULE_VERSION, null);
		Assert.isNotNull(deployedApplicationName);
		Assert.isNotNull(localName);
		Assert.isNotNull(server);
		
		this.localModuleId = (module != null) ? module.getId() : CFAM_MODULE_ID;
		
		this.server = server;
		setDeployedApplicationName(deployedApplicationName);
		CloudFoundryPlugin.trace("Created ApplicationModule " + deployedApplicationName + " for module " + module); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * A mapping to a cloud application representing a deployed application. A
	 * non-null cloud application means that the application is already deployed
	 * and exists in the CF server.
	 * <p/>
	 * If cloud application is null, it means the application module has not yet
	 * been deployed, or there was an error mapping the local application module
	 * with the actual deployed application (e.g. a connection error when trying
	 * to refresh the list of currently deployed applications).
	 * @return the actual cloud application obtained from the CF client library
	 * indicating a deployed application. It may be null.
	 */
	public CloudApplication getApplication() {
		return application;
	}

	/**
	 * The deployed application name. This may be different from the IModule
	 * name which is typically the project name (if accessible). Therefore to
	 * get the name of the actual app, always use this API. To get the local
	 * module name use {@link #getName()}, which matches the local workspace
	 * project, or get it through the IModule itself {@link #getLocalModule()}.
	 * @see IModule#getName()
	 * @see #getLocalModule()
	 */
	public synchronized String getDeployedApplicationName() {
		return deployedAppName;
	}

	public ApplicationStats getApplicationStats() {
		return applicationStats;
	}

	public StartingInfo getStartingInfo() {
		return startingInfo;
	}

	public void setStartingInfo(StartingInfo startingInfo) {
		this.startingInfo = startingInfo;
	}

	public InstancesInfo getInstancesInfo() {
		return instancesInfo;
	}

	public synchronized int getInstanceCount() {
		if (application != null) {
			return application.getInstances();
		}
		return 0;
	}

	/**
	 * Returns a copy of the application's deployment info describing deployment
	 * properties for the application like the application's memory settings,
	 * mapped URLs and bound services.
	 * <p/>
	 * Changes to the copy will have no effect. To make changes to the
	 * deployment information, request a working copy, and save it. See
	 * {@link #getDeploymentInfoWorkingCopy()}
	 * 
	 * <p/>
	 * If null, it means that the application is not currently deployed in the
	 * server, or the plugin has not yet determined if the application is
	 * deployed.
	 * <p/>
	 * If not null, it does NOT necessarily mean the application is deployed, as
	 * the application may be in the process of being deployed and will
	 * therefore have a deployment information.
	 * @return a copy of the application's deployment information. Changes to
	 * the copy will have no effect.
	 */
	public synchronized ApplicationDeploymentInfo getDeploymentInfo() {
		return deploymentInfo != null ? deploymentInfo.copy() : null;
	}

	/**
	 * Creates a working copy of the current deployment information. If the
	 * application does not have a current deployment information, a working
	 * copy will be generated from the app's deployment default values. A new
	 * copy is always returned. No changes take effect in the app modules'
	 * deployment info unless the working copy is saved.
	 * <p/>
	 * @return a new working copy with either existing deployment information,
	 * or default deployment information, if an deployment information does not
	 * exist.
	 */
	public synchronized DeploymentInfoWorkingCopy resolveDeploymentInfoWorkingCopy(IProgressMonitor monitor)
			throws CoreException {
		DeploymentInfoWorkingCopy wc = new ModuleDeploymentInfoWorkingCopy(this);
		wc.fill(monitor);
		return wc;
	}

	
	/**
	 * 
	 * @see AbstractApplicationDelegate#validateDeploymentInfo(ApplicationDeploymentInfo)
	 * @return OK status if deployment information is complete and valid. Returns an error status
	 * if invalid (i.e. it is missing some information).
	 * 
	 */
	public synchronized IStatus validateDeploymentInfo() {
		// Bug: 507637 . CloudFoundryApplicationModule may be out of sync with the underlying server. 
		// Note that missing deployment information at the time of validation means the server is out of sync. All CFAM that
		// are successfully created should have a valid deployment information, regardless of whether the actual application
		// is published or not.
		if (deploymentInfo == null) {
			String message = NLS.bind(Messages.CloudFoundryApplicationModule_SERVER_OUT_OF_SYNC, server.getId());
			IStatus status = CloudFoundryPlugin.getErrorStatus(message);
			setStatus(status);
			return status;
		}
		else {
			AbstractApplicationDelegate delegate = ApplicationRegistry.getApplicationDelegate(getLocalModule());
			IStatus status = delegate != null ? delegate.validateDeploymentInfo(deploymentInfo)
					: CloudUtil.basicValidateDeploymentInfo(deploymentInfo);

			if (status == null) {
				status = Status.OK_STATUS;
			}
			if (!status.isOK()) {
				status = CloudFoundryPlugin.getErrorStatus(NLS.bind(Messages.ERROR_APP_DEPLOYMENT_VALIDATION_ERROR,
						getDeployedApplicationName(), status.getMessage()));
			}
			setStatus(status);
			return status;
		}
	}

	/**
	 * 
	 * Returns the local WST module mapping. If present (not null), it most
	 * likely means that there is an accessible Eclipse workspace project for
	 * the application. If null, it means the application is external, which
	 * indicates that it is deployed in a CF server but does not have an
	 * accessible workspace project.
	 * 
	 * @return local WST module. May be null if the application is external.
	 */
	public IModule getLocalModule() {
		
		if(localModuleId == null) {
			return null;
		}
		
		if(localModuleId == CFAM_MODULE_ID) {
			return this;
		}
		
		IModule newModule = null;
		
		IModule[] lm = server.getModules();
		if(lm != null) {
			for(IModule im : lm) {
				if(im.getId().equals(localModuleId)) {
					newModule = im;
					break;
				}
			}
		}

		if(newModule == null) {
			/** The local module is no longer available (for example, it is no longer linked to cloud module) */
			return this;
		}
		
		return newModule;
	
	}
	
	public int getPublishState() {
		// if (isExternal()) {
		return IServer.PUBLISH_STATE_NONE;
		// }
		// return IServer.PUBLISH_STATE_UNKNOWN;
	}

	public String getServerTypeId() {
		return server.getServerType().getId();
	}

	/**
	 * Compute the running state of the application in the Cloud. This may not
	 * match {@link IServer#getModuleState(IModule[])}. The module state in the
	 * server is a cached state, and may be managed by external components like
	 * deployment or restart operations. In turn, these operations may rely on
	 * the computed application state as determined by this method to set the
	 * appropriate module state in the server.
	 * 
	 * @return One of the following state of the application on the Cloud
	 * target: {@link IServer#STATE_STARTED}, {@link IServer#STATE_STOPPED},
	 * {@link IServer#STATE_UNKNOWN}
	 * @deprecated use {@link #getRunState()} instead
	 * 
	 */
	public synchronized int getState() {
		return getCloudState(getApplication(), this.applicationStats);
	}

	/**
	 * 
	 * @return run state of the module. If application is starting, started, or stopped, it will return the following, respectively:
	 * {@link IServer#STATE_STARTING}, {@link IServer#STATE_STARTED}, {@link IServer#STATE_STOPPED}. Otherwise it will return
	 * {@link IServer#STATE_UNKNOWN}
	 */
	public synchronized ApplicationRunState getRunState() {
		// CF does not accurately track "starting" state of an application, therefore rely
		// on the state of the module in the local WTP server to determine if it is in "starting" state
		if (getLocalModule() != null) {
			int startingState = getStateInServer();
			if (startingState == IServer.STATE_STARTING) {
				return ApplicationRunState.getRunState(startingState);
			}
		}
		// If app is not in starting mode, compute the run state based on the actual cloud app state in CF:
		return ApplicationRunState.getRunState(getCloudState(getApplication(), this.applicationStats));
	}

	/**
	 * 
	 * @return module state in the IServer.
	 */
	public int getStateInServer() {
		return server.getModuleState(new IModule[] { getLocalModule() });
	}

	/**
	 * @return One of the following state of the application on the Cloud
	 * target: {@link IServer#STATE_STARTED}, {@link IServer#STATE_STOPPED},
	 * {@link IServer#STATE_UNKNOWN}
	 */
	public static int getCloudState(CloudApplication cloudApp, ApplicationStats applicationStats) {
		// Fetch the running state of the first instance that is running.

		if (applicationStats != null) {
			List<InstanceStats> records = applicationStats.getRecords();

			if (records != null) {
				for (InstanceStats stats : records) {
					if (stats != null && stats.getState() == InstanceState.RUNNING) {
						return IServer.STATE_STARTED;
					}
				}
			}
		}

		// If the app desired state is stopped (so it may indicate that a
		// request to stop the app has been made)
		// consider the app to be stopped
		if (cloudApp != null && cloudApp.getState() == AppState.STOPPED) {
			return IServer.STATE_STOPPED;
		}

		return IServer.STATE_UNKNOWN;
	}

	public boolean isExternal() {
		return localModuleId == CFAM_MODULE_ID;
	}

	public synchronized void setError(CoreException error) {
		this.validationStatus = error != null ? error.getStatus() : null;
	}

	public synchronized void setStatus(IStatus status) {
		if (status == null || status.isOK()) {
			this.validationStatus = null;
		}
		else {
			this.validationStatus = status;
		}
	}

	public synchronized IStatus getStatus() {
		return this.validationStatus;
	}

	public synchronized void setApplicationStats(ApplicationStats applicationStats) {
		this.applicationStats = applicationStats;
	}

	public synchronized void setInstancesInfo(InstancesInfo instancesInfo) {
		this.instancesInfo = instancesInfo;
	}

	/**
	 * Maps the application module to an actual deployed application in a CF
	 * server. It replaces any existing deployment info with one generated from
	 * the cloud application. The existing deployment descriptor remains
	 * unchanged if removing the cloud application mapping (i.e. setting to
	 * null)
	 * 
	 * @param cloudApplication the actual deployed application in a CF server.
	 * @throws CoreException if failure occurred while setting a cloud
	 * application, or the deployment info is currently being modified by some
	 * other component.
	 */
	public synchronized void setCloudApplication(CloudApplication cloudApplication) {
		this.application = cloudApplication;

		if (application != null) {
			// Update the deployment info so that it reflects the actual
			// deployed
			// application. Note that Eclipse-specific properties are retained
			// from
			// existing deployment infos.
			// Only the actual deployed app properties (e.g. name, services,
			// URLs)
			// are updated from the cloud application
			ApplicationDeploymentInfo cloudApplicationInfo = resolveDeployedApplicationInformation();
			if (cloudApplicationInfo != null) {
				internalSetDeploymentInfo(cloudApplicationInfo);
			}
		}
	}

	/**
	 * 
	 * @return true if application exists in the Cloud server AND running state
	 * is known (stopped, started, starting)
	 */
	public synchronized boolean isDeployed() {
		// WARNING: if refactoring, beware to retain this behaviour as deployed
		// means existing in CF AND having a KNOWN app state
		// Many components rely on the application to be in KNOWN app state in
		// addition to existing in CF therefore take care
		// when modifying this implementation
		return exists() && getState() != IServer.STATE_UNKNOWN;
	}

	/**
	 * @return true if the application exists in CF. False otherwise. No
	 * information is provided if the application is synchronised (published).
	 */
	public synchronized boolean exists() {
		return getApplication() != null;
	}

	/**
	 * Sets a deployment information for the application. Note that if the
	 * application is already deployed (i.e. a {@link CloudApplication} mapping
	 * exists for this module), this will overwrite the deployment information
	 * for the {@link CloudApplication}.
	 * @param lastDeploymentInfo the latest deployment of the application. IF
	 * the application name in the latest deployment has changed, the current
	 * module name will also be updated. If setting null (e.g. application is
	 * being deleted), the current module name will remain unchanged.
	 */
	private void internalSetDeploymentInfo(ApplicationDeploymentInfo deploymentInfo) {
		this.deploymentInfo = deploymentInfo;
		// Note that last Deployment info may be null (e.g. when deleting an
		// application). Only update the appliation ID if setting a new last
		// deployment info, since
		// the module should match the application properties listed in the
		// latest deployment, including any app name changes.
		if (deploymentInfo != null && deploymentInfo.getDeploymentName() != null) {
			setDeployedApplicationName(deploymentInfo.getDeploymentName());
		}
	}

	/*
	 * 
	 * Internal helper methods. Non-synchronized
	 */

	/**
	 * Resolve deployment information from values in the corresponding deployed
	 * application ( {@link CloudApplication} ). If the application is not yet
	 * deployed (i.e., cloud application is null), null is returned.
	 * 
	 * @param appModule application currently deployed in CF server
	 * @param cloudServer server where app is deployed
	 * @return a new copy of the deployment info for the deployed app, or null
	 * if the cloud application is null
	 */
	protected ApplicationDeploymentInfo resolveDeployedApplicationInformation() {
		if (application == null) {
			return null;
		}

		AbstractApplicationDelegate delegate = ApplicationRegistry.getApplicationDelegate(getLocalModule());
		ApplicationDeploymentInfo info = null;
		CloudFoundryServer cloudServer = getCloudFoundryServer();

		if (delegate != null) {
			try {
				info = delegate.getExistingApplicationDeploymentInfo(this, cloudServer.getServer());
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
		}

		// If no info has been resolved yet, use a default parser
		if (info == null) {
			info = CloudUtil.parseApplicationDeploymentInfo(application);
		}

		return info;
	}

	/**
	 * Application name must not be null. This is the deployed application name.
	 * @param applicationName most not be null
	 */
	protected void setDeployedApplicationName(String applicationName) {
		Assert.isNotNull(applicationName);
		if (!applicationName.equals(this.deployedAppName)) {
			this.deployedAppName = applicationName;
			if (localModuleId != null) {
				CloudFoundryServer cloudServer = getCloudFoundryServer();

				// Since the deployment name changed, update the local module ->
				// deployed module cache in the server
				cloudServer.updateApplicationModule(this);
			}
		}
	}

	/**
	 * Returns a default deployment information, with basic information to
	 * deploy or start/restart an application. It is not guaranteed to be
	 * complete or valid, as in some cases missing information is acceptable
	 * since additional deployment steps may involve prompting for the missing
	 * values.
	 * <p/>
	 * Never null. At the very basic, it will set a simple default deployment
	 * information with just the application name and memory setting.
	 * @return non-null default deployment info. This default information is
	 * also set in the module as the module's current deployment information.
	 */
	protected ApplicationDeploymentInfo getDefaultDeploymentInfo(IProgressMonitor monitor) throws CoreException {

		AbstractApplicationDelegate delegate = ApplicationRegistry.getApplicationDelegate(getLocalModule(),
				getCloudFoundryServer());
		ApplicationDeploymentInfo defaultInfo = null;

		if (delegate != null) {
			try {
				defaultInfo = delegate.getDefaultApplicationDeploymentInfo(this, getCloudFoundryServer().getServer(),
						monitor);
			}
			catch (CoreException e) {
				CloudFoundryPlugin.logError(e);
			}
		}

		if (defaultInfo == null) {
			defaultInfo = createGeneralDefaultInfo();
		}

		return defaultInfo;
	}

	/**
	 * Creates a general deployment info that should be applicable to any
	 * application type. It will have an app name as well as memory setting.
	 * @return Non-null general deployment info with basic information for
	 * application deployment.
	 */
	protected ApplicationDeploymentInfo createGeneralDefaultInfo() {
		ApplicationDeploymentInfo info = new ApplicationDeploymentInfo(getDeployedApplicationName());
		info.setMemory(CloudUtil.DEFAULT_MEMORY);
		return info;
	}

	protected CloudFoundryServer getCloudFoundryServer() {
		return (CloudFoundryServer) server.loadAdapter(CloudFoundryServer.class, null);
	}

	/**
	 * Should not be instantiated outside of a Cloud Module, as it is coupled
	 * with the implementation of the module.
	 */
	protected class ModuleDeploymentInfoWorkingCopy extends DeploymentInfoWorkingCopy {

		protected ModuleDeploymentInfoWorkingCopy(CloudFoundryApplicationModule appModule) {
			super(appModule);
		}

		@Override
		public void save() {
			synchronized (appModule) {

				// Set the working copy as a regular deployment info, as to not
				// keeping
				// a reference to the working copy
				ApplicationDeploymentInfo info = new ApplicationDeploymentInfo(getDeployedApplicationName());
				info.setInfo(this);
				appModule.internalSetDeploymentInfo(info);
			}
		}
	}

}
