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
 *     Keith Chong, IBM - Allow module to bypass facet check 
 ********************************************************************************/
package org.eclipse.cft.server.core.internal;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.eclipse.cft.server.core.ICloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.ModuleCache.ServerData;
import org.eclipse.cft.server.core.internal.application.ApplicationRegistry;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.core.internal.client.SelfSignedStore;
import org.eclipse.cft.server.core.internal.spaces.CloudFoundrySpace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.jst.server.core.internal.J2EEUtil;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleType;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.internal.IModuleVisitor;
import org.eclipse.wst.server.core.internal.Server;
import org.eclipse.wst.server.core.internal.ServerPlugin;
import org.eclipse.wst.server.core.internal.ServerPublishInfo;
import org.eclipse.wst.server.core.model.IURLProvider;
import org.eclipse.wst.server.core.model.ServerDelegate;

/**
 * Local representation of a Cloud Foundry server, with API to obtain local
 * {@link IModule} for deployed applications, as well as persist local server
 * information like the server name, or user credentials.
 * <p/>
 * Note that a local cloud foundry server is an instance that may be discarded
 * and created multiple times by the underlying local server framework even
 * while the same server is still connected, typically when server changes are
 * saved (e.g. changing a server name), therefore the server instance should NOT
 * hold state intended to be present during the life span of an Eclipse runtime
 * session. Use an appropriate caching mechanism if application or server state
 * needs to be cached during a runtime session. See {@link ModuleCache}.
 * <p/>
 * In addition, the local server instance delegates to a
 * {@link CloudFoundryServerBehaviour} for ALL CF client calls. Do NOT add
 * client calls in the server instance. These should be added to the
 * {@link CloudFoundryServerBehaviour}.
 * 
 * IMPORTANT NOTE: This class can be referred by the branding extension from
 * adopter so this class should not be moved or renamed to avoid breakage to
 * adopters.
 * @author Christian Dupuis
 * @author Terry Denney
 * @author Leo Dos Santos
 * @author Steffen Pingel
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class CloudFoundryServer extends ServerDelegate implements IURLProvider {

	private static ThreadLocal<Boolean> deleteServicesOnModuleRemove = new ThreadLocal<Boolean>() {
		protected Boolean initialValue() {
			return Boolean.TRUE;
		}
	};
	

	/** Used to retrieve passwords from secure preferences */
	private static final String OLD_PLUGIN_ID = "org.cloudfoundry.ide.eclipse.server.core";
	
	/**
	 * Attribute key for the a unique server ID used to store credentials in the
	 * secure store.
	 */
	private static final String PROP_SERVER_ID = "org.eclipse.cft.serverId"; //$NON-NLS-1$
	private static final String OLD_PROP_SERVER_ID = "org.cloudfoundry.ide.eclipse.serverId"; //$NON-NLS-1$

	/**
	 * Attribute key for the password.
	 */
	private static final String OLD_PROP_PASSWORD_ID = "org.cloudfoundry.ide.eclipse.password";
	/**
	 * Attribute key for the API url.
	 */
	private static final String PROP_URL = "org.eclipse.cft.url"; //$NON-NLS-1$
	private static final String OLD_PROP_URL = "org.cloudfoundry.ide.eclipse.url"; //$NON-NLS-1$

	/**
	 * Attribute key for the username.
	 */
	private static final String PROP_USERNAME_ID = "org.eclipse.cft.username"; //$NON-NLS-1$
	private static final String OLD_PROP_USERNAME_ID = "org.cloudfoundry.ide.eclipse.username"; //$NON-NLS-1$

	private static final String PROP_ORG_ID = "org.eclipse.cft.org"; //$NON-NLS-1$
	private static final String OLD_PROP_ORG_ID = "org.cloudfoundry.ide.eclipse.org"; //$NON-NLS-1$

	private static final String PROP_SPACE_ID = "org.eclipse.cft.space"; //$NON-NLS-1$
	private static final String OLD_PROP_SPACE_ID = "org.cloudfoundry.ide.eclipse.space"; //$NON-NLS-1$

	static final String TUNNEL_SERVICE_COMMANDS_PROPERTY = "org.eclipse.cft.tunnel.service.commands"; //$NON-NLS-1$

	private static final String PROPERTY_DEPLOYMENT_NAME = "deployment_name"; //$NON-NLS-1$

	/**
	 * Attribute key for the token.
	 */
	public static final String PROP_SSO_ID = "org.eclipse.cft.sso"; //$NON-NLS-1$
	
	public static final String PROP_PASSCODE_ID = "org.eclipse.cft.passcode"; //$NON-NLS-1$

	protected void updateState(Server server, CloudFoundryApplicationModule appModule) throws CoreException {
		IModule[] localModule = new IModule[] { appModule.getLocalModule() };
		server.setModuleState(localModule, appModule.getState());
		if (server.getModulePublishState(localModule) == IServer.PUBLISH_STATE_UNKNOWN) {
			if (!server.hasPublishedResourceDelta(localModule)) {
				server.setModulePublishState(localModule, IServer.PUBLISH_STATE_NONE);
			}
			else {
				server.setModulePublishState(localModule, IServer.PUBLISH_STATE_INCREMENTAL);
			}
		}
		// Update the child module states if available.
		updateChildModuleStates(server, localModule);
	}

	private void updateChildModuleStates(Server server, IModule[] parentModule) throws CoreException {
		if (parentModule == null || parentModule.length == 0) {
			return;
		}

		int parentModuleSize = parentModule.length;

		IModule[] childModules = server.getChildModules(parentModule, null);
		if (childModules == null || childModules.length == 0) {
			return;
		}
		for (IModule curChildModule : childModules) {
			IModule[] curFullChildModule = new IModule[parentModuleSize + 1];
			System.arraycopy(parentModule, 0, curFullChildModule, 0, parentModuleSize);
			curFullChildModule[parentModuleSize] = curChildModule;

			if (server.getModulePublishState(curFullChildModule) == IServer.PUBLISH_STATE_UNKNOWN) {
				if (!server.hasPublishedResourceDelta(curFullChildModule)) {
					server.setModulePublishState(curFullChildModule, IServer.PUBLISH_STATE_NONE);
				}
				else {
					server.setModulePublishState(curFullChildModule, IServer.PUBLISH_STATE_INCREMENTAL);
				}
			}
			updateChildModuleStates(server, curFullChildModule);
		}
	}

	private String serverTypeId;
	
	private ServerCredentialsStore credentialsStore;

	private boolean secureStoreDirty;

	private String initialServerId;

	private String password;

	private CloudFoundrySpace cloudSpace;
	
	private String token;

	public CloudFoundryServer() {
	}

	public void updateApplicationModule(CloudFoundryApplicationModule module) {
		if (getData() != null) {
			getData().updateCloudApplicationModule(module);
		}
	}

	/** Convenience method to determine if a module is for a non-faceted project */
	public static boolean isNonfacetedModule(IModule module) {
		boolean isNonfacetedProjResult = false;
		
		// Workaround - Remove the following and use the above commented
		// out code
		ClassLoader classLoader = module.getClass().getClassLoader();
		if (classLoader != null) {
			try {
				Class iModule2 = classLoader.loadClass("org.eclipse.wst.server.core.IModule2"); //$NON-NLS-1$
				if (iModule2 != null) {
					Method getProperty = iModule2.getMethod("getProperty", String.class); //$NON-NLS-1$
					Object o = getProperty.invoke(module, CloudFoundryConstants.PROPERTY_MODULE_NO_FACET);
					if (o instanceof String && ((String) o).equals("true")) { //$NON-NLS-1$
						isNonfacetedProjResult = true;
					}
				}
			}
			catch (Exception e) {
				// If any issues, just go ahead and do the facet check
				// below
			}
		}
		// End of workaround
		
		return isNonfacetedProjResult;
	}
	
	@Override
	public IStatus canModifyModules(IModule[] add, IModule[] remove) {
		if (add != null) {
			int size = add.length;
			for (int i = 0; i < size; i++) {
				IModule module = add[i];
				if (!ApplicationRegistry.isSupportedModule(module)) {
					return new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, 0, NLS.bind(
							Messages.CloudFoundryServer_ERROR_APPTYPE_NOT_SUPPORTED, module.getModuleType().getId()),
							null);
				}

				IStatus status;
				// If the module, in a non-faceted project, has been determined
				// to be deployable to CF (ie. a single zip application
				// archive), then this facet check is unnecessary.
				boolean ignoreFacetCheck = isNonfacetedModule(module);

				if (module.getProject() != null && !ignoreFacetCheck) {
					status = FacetUtil.verifyFacets(module.getProject(), getServer());
					if (status != null && !status.isOK()) {
						return status;
					}
				}
			}
		}
		// if (remove != null) {
		// for (IModule element : remove) {
		// if (element instanceof ApplicationModule) {
		// return new Status(IStatus.ERROR, CloudfoundryPlugin.PLUGIN_ID, 0,
		// "Some modules can not be removed.", null);
		// }
		// }
		// }

		return Status.OK_STATUS;
	}

	public void clearApplications() {
		ServerData data = getData();
		if (data != null) {
			data.clear();
		}
	}

	public IStatus error(String message, Exception e) {
		return new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID, NLS.bind("{0} [{1}]", message, //$NON-NLS-1$
				getServer().getName()), e);
	}

	/**
	 * Fetches the corresponding Cloud Foundry-aware module for the given WST
	 * IModule. The Cloud Foundry-aware module contains additional information
	 * that is specific to Cloud Foundry. It will not create the module if it
	 * does not exist. For most cases where an cloud application module is
	 * expected to already exist, this method is preferable than
	 * {@link #getCloudModule(IModule)}, and avoids possible bugs when an
	 * application is being deleted. See {@link #getCloudModule(IModule)}.
	 * @param module WST local module
	 * @return Cloud module, if it exists, or null.
	 */
	public CloudFoundryApplicationModule getExistingCloudModule(IModule module) {
		if (module instanceof CloudFoundryApplicationModule) {
			return (CloudFoundryApplicationModule) module;
		}

		return getData() != null ? getData().getExistingCloudModule(module) : null;
	}

	private CloudFoundryApplicationModule getOrCreateCloudModule(IModule module) {
		if (module instanceof CloudFoundryApplicationModule) {
			return (CloudFoundryApplicationModule) module;
		}

		return getData() != null ? getData().getOrCreateCloudModule(module) : null;
	}

	/**
	 * Gets an existing Cloud module for the given {@link IModule} or if it
	 * doesn't exist, it will attempt to create it.
	 * <p/>
	 * NOTE: care should be taken when invoking this method. Only invoke in
	 * cases where a cloud module may not yet exist, for example, when
	 * refreshing list of currently deployed applications for the first time, or
	 * deploying an application for the first time. If a cloud module is already
	 * expected to exist for some operation (e.g., modifying properties for an
	 * application that is already deployed, like scaling memory, changing
	 * mapped URLs, binding services etc..) , use
	 * {@link #getExistingCloudModule(IModule)} instead. The reason for this is
	 * to avoid recreating a module that may be in the processing of being
	 * deleted by another operation, but the corresponding WST {@link IModule}
	 * may still be referenced by the local server. Using
	 * {@link #getExistingCloudModule(IModule)} is also preferable for better
	 * error detection, as if a module is expected to exist for an operation,
	 * but it doesn't it may indicate that an error occurred in refreshing the
	 * list of deployed applications.
	 * @param module
	 * @return existing cloud module, or if not yet created, creates and returns
	 * it.
	 */
	public CloudFoundryApplicationModule getCloudModule(IModule module) {
		if (module == null) {
			return null;
		}
		return getOrCreateCloudModule(module);
	}

	/**
	 * Update all Cloud Modules for the list of local server {@link IModule}. If
	 * all modules have been mapped to a Cloud module, {@link IStatus#OK} is
	 * returned. If no modules are present (nothing is deployed),
	 * {@link IStatus#OK} also returned. Otherwise, if there are modules with
	 * missing mapped Cloud Application modules, {@link IStatus#ERROR} is
	 * returned.
	 * @return {@link IStatus#OK} if all local server {@link IModule} have a
	 * Cloud Application module mapping, or list of {@link IModule} in the
	 * server is empty. Otherwise, {@link IStatus#ERROR} returned.
	 */
	public IStatus refreshCloudModules() {
		if (getServerOriginal() == null) {
			return CloudFoundryPlugin.getErrorStatus(NLS.bind(
					Messages.CloudFoundryServer_ERROR_SERVER_ORIGIN_NOT_FOUND, getDeploymentName()));
		}
		IModule[] modules = getServerOriginal().getModules();
		if (modules != null) {
			StringWriter writer = new StringWriter();

			for (IModule module : modules) {
				ICloudFoundryApplicationModule appModule = getCloudModule(module);
				if (appModule == null) {
					writer.append(Messages.CloudFoundryServer_ERROR_FAIL_ON_CFAPP_CREATION);
					writer.append(module.getId());
					writer.append('\n');
				}
			}
			String message = writer.toString();
			if (message.length() > 0) {
				CloudFoundryPlugin.getErrorStatus(message);
			}
		}

		return Status.OK_STATUS;
	}

	/**
	 * Does not refresh the list of application modules. Returns the cached
	 * list, which may be empty.
	 * @return never null. May be empty
	 */
	public Collection<CloudFoundryApplicationModule> getExistingCloudModules() {
		return getData() != null ? getData().getExistingCloudModules()
				: new ArrayList<CloudFoundryApplicationModule>(0);
	}

	public CloudFoundryServerBehaviour getBehaviour() {
		return (CloudFoundryServerBehaviour) getServer().loadAdapter(CloudFoundryServerBehaviour.class, null);
	}

	@Override
	public IModule[] getChildModules(IModule[] module) {
		if (module == null || module.length == 0) {
			return new IModule[0];
		}

		// IModuleType moduleType = module[0].getModuleType();
		//
		// if (module.length == 1 && moduleType != null &&
		// ID_WEB_MODULE.equals(moduleType.getId())) {
		// IWebModule webModule = (IWebModule)
		// module[0].loadAdapter(IWebModule.class, null);
		// if (webModule != null) {
		// IModule[] modules = webModule.getModules();
		// return modules;
		// }
		// }

		IModuleType moduleType = module[module.length - 1].getModuleType();

		if (moduleType != null && CloudFoundryConstants.ID_WEB_MODULE.equals(moduleType.getId())) {
			IWebModule webModule = (IWebModule) module[module.length - 1].loadAdapter(IWebModule.class, null);
			if (webModule != null)
				return webModule.getModules();
		}

		return new IModule[0];
	}

	/**
	 * Returns the cached server data for the server. In some case the data may
	 * be null, if the server has not yet been created but it's available to be
	 * configured (e.g while a new server instance is being created).
	 * @return cached server data. May be null.
	 */
	private ServerData getData() {
		return CloudFoundryPlugin.getModuleCache().getData(getServerOriginal());
	}

	public String getDeploymentName() {
		return getAttribute(PROPERTY_DEPLOYMENT_NAME, ""); //$NON-NLS-1$
	}

	public String getPassword() {
		if (secureStoreDirty) {
			return password;
		}
		String cachedPassword = getData() != null ? getData().getPassword() : null;
		if (cachedPassword != null) {
			return cachedPassword;
		}
		String legacyPassword = getAttribute(OLD_PROP_PASSWORD_ID, (String) null);
		if (legacyPassword != null) {
			return legacyPassword;
		}
		return new ServerCredentialsStore(getServerId()).getPassword();
	}

	public String getToken() {
		if (secureStoreDirty) {
			return token;
		}
		String cachedToken = getData() != null ? getData().getToken() : null;
		if (cachedToken != null) {
			return cachedToken;
		}
		return new ServerCredentialsStore(getServerId()).getToken();
	}

	
	/**
	 * Public for testing.
	 */
	public synchronized ServerCredentialsStore getCredentialsStore() {
		if (credentialsStore == null) {
			if(isNewCFEclipseServer()) {
				credentialsStore = new ServerCredentialsStore(initialServerId);
			} else {
				// If this server saved passwords to the old secure preferences location, then use that.
				credentialsStore = new ServerCredentialsStore(initialServerId, OLD_PLUGIN_ID);
			}
		}
		return credentialsStore;
	}

	@Override
	public IModule[] getRootModules(IModule module) throws CoreException {
		if (ApplicationRegistry.isSupportedModule(module)) {
			IStatus status = canModifyModules(new IModule[] { module }, null);
			if (status == null || !status.isOK()) {
				throw new CoreException(status);
			}
			return new IModule[] { module };
		}

		return J2EEUtil.getWebModules(module, null);
	}

	public CloudFoundryServerRuntime getRuntime() {
		return (CloudFoundryServerRuntime) getServer().getRuntime().loadAdapter(CloudFoundryServerRuntime.class, null);
	}

	/**
	 * This method is API used by CloudFoundry Code.
	 */
	public String getUrl() {
		
		if(isNewCFEclipseServer()) {
			return getAttribute(PROP_URL, (String) null);
		} else {
			return getAttribute(OLD_PROP_URL, (String) null);
		}
	}

	public String getUsername() {
		if(isNewCFEclipseServer()) {
			return getAttribute(PROP_USERNAME_ID, (String) null);			
		} else {
			return getAttribute(OLD_PROP_USERNAME_ID, (String) null);
		}
		
	}

	public String getServerId() {
		if(isNewCFEclipseServer()) {
			return getAttribute(PROP_SERVER_ID, (String) null);	
		} else {
			return getAttribute(OLD_PROP_SERVER_ID, (String) null);	
		}
		
	}
	
	public boolean isSso() {
		return getAttribute(PROP_SSO_ID, false);
	}


	public boolean hasCloudSpace() {
		return getCloudFoundrySpace() != null;
	}

	public CloudFoundrySpace getCloudFoundrySpace() {

		if (cloudSpace == null) {
			String orgName = getOrg();
			String spaceName = getSpace();

			String[] checkValidity = { orgName, spaceName };
			boolean valid = false;
			for (String value : checkValidity) {
				valid = validSpaceValue(value);
				if (!valid) {
					break;
				}
			}
			if (valid) {
				cloudSpace = new CloudFoundrySpace(orgName, spaceName);
			}

		}
		return cloudSpace;
	}

	protected boolean validSpaceValue(String value) {
		return value != null && value.length() > 0;
	}

	public boolean isConnected() {
		return getServer().getServerState() == IServer.STATE_STARTED;
	}

	@Override
	public void modifyModules(final IModule[] add, IModule[] remove, IProgressMonitor monitor) throws CoreException {

		// Log modify modules parameters
		CloudFoundryPlugin.logInfo(convertModifyModulesToString(add, remove));
		
		final CloudFoundryApplicationModule toReplace = remove != null && remove.length > 0 ? getExistingCloudModule(remove[0])
				: null;
		if (toReplace != null && getData().getTaggedForReplace(toReplace) != null) {
			try {
				replace(toReplace, add, monitor);
			}
			finally {
				getData().untagForReplace(toReplace);
			}
		}
		else {
			if (remove != null && remove.length > 0) {
				if (getData() != null) {
					for (IModule module : remove) {
						getData().moduleAdditionCompleted(module);
					}
				}

				try {
					getBehaviour().operations().deleteModules(remove, deleteServicesOnModuleRemove.get()).run(monitor);
				}
				catch (CoreException e) {
					// ignore deletion of applications that didn't exist
					if (!CloudErrorUtil.isNotFoundException(e)) {
						throw e;
					}
				}
			}

			if (add != null && add.length > 0) {

				if (getData() != null) {
					for (IModule module : add) {
						// avoid automatic deletion before module has been
						// deployed
						getData().moduleBeingAdded(module);
					}
				}
			}
		}
	}

	/**
	 * 
	 * Replaces an existing Cloud module with another, without deleting, or
	 * changing start state of the underlying mapped {@link CloudApplication}.
	 * <p/>
	 * One purpose of this replace operation is to map an existing
	 * {@link CloudFoundryApplicationModule} to another workspace project
	 * (indirectly referenced by the target module).
	 * <p/>
	 * The existing module is deleted (but not its associated
	 * {@link CloudApplication}), and the target module, if not already
	 * existing, is created, and mapped to {@link CloudApplication}
	 * <p/>
	 * If no target module is specified to replace the existing module, then the
	 * existing module is deleted and replaced with an external (that is, an
	 * unmapped to a workspace project) module instead.
	 * 
	 * @param existing Cloud module to be replaced
	 * @param target module to replace the original module
	 * @param monitor
	 * @throws CoreException
	 */
	protected void replace(CloudFoundryApplicationModule existingCloudModule, IModule[] targetModule,
			IProgressMonitor monitor) throws CoreException {

		// Since the underlying deployed application in the Cloud is not
		// being deleted or modified,
		// fetch an updated CloudApplication as it will be mapped to the new
		// target module
		final CloudFoundryApplicationModule existing = existingCloudModule;
		final IModule[] target = targetModule;
		final CloudApplication updatedCloudApplication = getBehaviour().getCloudApplication(
				existing.getDeployedApplicationName(), monitor);

		if (updatedCloudApplication == null) {
			throw CloudErrorUtil.toCoreException(NLS.bind(
					Messages.CloudFoundryServer_ERROR_UNABLE_REPLACE_MODULE_NO_CLOUD_APP,
					existing.getDeployedApplicationName()));
		}
		
		// Since Cloud application exists, also fetch the stats as they are needed to correctly compute the module state
		final ApplicationStats stats = getBehaviour().getApplicationStats(existing.getDeployedApplicationName(), monitor);

		CloudFoundryPlugin.getCallback().stopApplicationConsole(existing, this);

		final Server server = (Server) getServer();

		List<IModule> externalModules = new ArrayList<IModule>();

		// Delete the module to replace and ensure publish info and state
		// are
		// set correctly
		IModule[] existingModule = new IModule[] { existing.getLocalModule() };

		int existingModuleState = server.getModuleState(existingModule);

		// Find all external modules, as the external modules may need to be
		// set again in the server, but skip
		// the module to be replaced, as it will be deleted
		// later on
		for (IModule module : server.getModules()) {
			CloudFoundryApplicationModule appM = getExistingCloudModule(module);

			if (appM != null && appM.isExternal()) {
				IModule appMLocalMod = appM.getLocalModule();
				if (!appMLocalMod.getName().equals(existing.getLocalModule().getName())) {
					externalModules.add(appM);
				}
			}
		}

		server.setModulePublishState(existingModule, IServer.PUBLISH_STATE_UNKNOWN);
		server.setModuleState(existingModule, IServer.STATE_UNKNOWN);

		// Remove from the local Cloud cache
		getData().remove(existing);

		final List<IModule[]> remainingModules = new ArrayList<IModule[]>();

		// Remove the publish info of the deleted module
		server.visit(new IModuleVisitor() {
			public boolean visit(IModule[] module) {

				if (module.length > 0 && module[0].getName().equals(existing.getLocalModule().getName())) {
					return false;
				}
				else {
					remainingModules.add(module);
					return true;
				}
			}
		}, monitor);

		ServerPublishInfo info = server.getServerPublishInfo();

		info.removeDeletedModulePublishInfo(server, remainingModules);
		info.save();

		// Now create module for the target that will replace the existing
		// moduel
		if (target != null && target.length > 0) {
			CloudFoundryApplicationModule remappedMod = getCloudModule(target[0]);
			remappedMod.setCloudApplication(updatedCloudApplication);
			if (remappedMod.isExternal()) {
				externalModules.add(remappedMod);
			}

			server.setExternalModules(externalModules.toArray(new IModule[0]));

			updateState(server, remappedMod);

			// Tell webtools the new module has been published
			server.setModulePublishState(new IModule[] { remappedMod.getLocalModule() }, IServer.PUBLISH_STATE_NONE);

			// Restore the module state of the deleted module in the new
			// module
			server.setModuleState(new IModule[] { remappedMod.getLocalModule() }, existingModuleState);
		}
		else {
			// Otherwise create an external module for the application that just
			// got
			// deleted
			server.setExternalModules(externalModules.toArray(new IModule[0]));

			updateModule(updatedCloudApplication, existing.getDeployedApplicationName(), stats, monitor);
		}
	}

	@Override
	public void setDefaults(IProgressMonitor monitor) {
		super.setDefaults(monitor);
		String typeName = CloudFoundryBrandingExtensionPoint.getServerDisplayName(serverTypeId);
		if (typeName == null || typeName.trim().length() == 0) {
			typeName = getServer().getServerType().getName();
		}
		String name = typeName;
		int i = 2;
		while (ServerPlugin.isNameInUse(getServerWorkingCopy().getOriginal(), name)) {
			name = NLS.bind("{0} ({1})", new String[] { typeName, String.valueOf(i) }); //$NON-NLS-1$
			i++;
		}
		
		getServerWorkingCopy().setName(name);
		getServerWorkingCopy().setHost("Cloud"); //$NON-NLS-1$

		setAttribute("auto-publish-setting", 1); //$NON-NLS-1$
	}

	public void setDeploymentName(String name) {
		setAttribute(PROPERTY_DEPLOYMENT_NAME, name);
	}

	public void setPassword(String password) {
		this.secureStoreDirty = true;
		this.password = password;

		// remove password in case an earlier version stored it in server
		// properties
		if (getServerWorkingCopy() != null) {
	
			// We only null the old property here, as the new property is never set.
			getServerWorkingCopy().setAttribute(OLD_PROP_PASSWORD_ID, (String) null);
			
		}
		// in case setUrl() or setPassword() were never called, e.g. for legacy
		// servers
		updateServerId();

		if (getData() != null) {
			getData().setPassword(password);
		}
	}

//	public void setToken(String token) {
//		this.secureStoreDirty = true;
//		this.token = token;
//
//		updateServerId();
//
//		if (getData() != null) {
//			getData().setToken(token);
//		}
//	}
	

	public void setSpace(CloudSpace space) {

		secureStoreDirty = true;

		if (space != null) {
			this.cloudSpace = new CloudFoundrySpace(space);
			internalSetOrg(cloudSpace.getOrgName());
			internalSetSpace(cloudSpace.getSpaceName());
		}
		else {
			// Otherwise clear the org and space
			internalSetOrg(null);
			internalSetSpace(null);
			cloudSpace = null;
		}

		updateServerId();
	}

	public void setUrl(String url) {

		if(isNewCFEclipseServer()) {
			setAttribute(PROP_URL, url);			
		} else {
			setAttribute(OLD_PROP_URL, url);
		}

		
		updateServerId();
	}

	public void setUsername(String username) {
		if(isNewCFEclipseServer()) {
			setAttribute(PROP_USERNAME_ID, username);	
		} else {
			setAttribute(OLD_PROP_USERNAME_ID, username);
		}
		
		updateServerId();
	}
	

	protected void internalSetOrg(String org) {
		if(isNewCFEclipseServer()) {
			setAttribute(PROP_ORG_ID, org);	
		} else {
			setAttribute(OLD_PROP_ORG_ID, org);
		}
		
	}

	protected void internalSetSpace(String space) {
		if(isNewCFEclipseServer()) {
			setAttribute(PROP_SPACE_ID, space);			
		} else {
			setAttribute(OLD_PROP_SPACE_ID, space);
		}
		
	}

	protected String getOrg() {
		if(isNewCFEclipseServer()) {
			return getAttribute(PROP_ORG_ID, (String) null);
		} else {
			return getAttribute(OLD_PROP_ORG_ID, (String) null);
		}
	}

	protected String getSpace() {
		if(isNewCFEclipseServer()) {
			return getAttribute(PROP_SPACE_ID, (String) null);
		} else {
			return getAttribute(OLD_PROP_SPACE_ID, (String) null);
		}
	}

	/**
	 * 
	 * @return true if server uses self-signed certificates. False otherwise,
	 * including if server preference can't be resolved.
	 */
	public boolean isSelfSigned() {
		return isSelfSigned(getUrl());
	}

	public void setSelfSigned(boolean isSelfSigned) {
		setSelfSigned(isSelfSigned, getUrl());
	}

	private void updateServerId() {
		StringWriter writer = new StringWriter();
		writer.append(getUsername());
		if (hasCloudSpace()) {
			writer.append('_');
			writer.append(getOrg());
			writer.append('_');
			writer.append(getSpace());
		}
		writer.append('@');
		writer.append(getUrl());

		if(isNewCFEclipseServer())  {
			setAttribute(PROP_SERVER_ID, writer.toString());	
		} else {
			setAttribute(OLD_PROP_SERVER_ID, writer.toString());
		}
		
	}

	@Override
	protected void initialize() {
		super.initialize();
		serverTypeId = getServer().getServerType().getId();
		// legacy in case password was saved by an earlier version
		this.password = getAttribute(OLD_PROP_PASSWORD_ID, (String) null);
		
		this.initialServerId = getServerId(); 
	}
	

	/**
	 * Update the wrapped (WST) ( {@link IModule} ) and corresponding cloud
	 * module ( {@link CloudFoundryApplicationModule} ) such that they are in
	 * synch with the actual deployed applications (represented by
	 * {@link CloudApplication} ). WST modules ( {@link IModule} ) that do not
	 * have a corresponding deployed application ( {@link CloudApplication})
	 * will be removed.
	 * <p/>
	 * This does not update the module state in the Server except for deleted
	 * modules
	 * <p/>
	 * To do a full update of modules including update of module state, use
	 * {@link #updateModules(Map, Map)}
	 * @param deployedApplications
	 * @throws CoreException
	 */
	public void addAndDeleteModules(Map<String, CloudApplication> deployedApplications, Map<String, ApplicationStats> applicationStats) throws CoreException {
		Server server = (Server) getServer();

		final Set<CloudFoundryApplicationModule> allModules = new HashSet<CloudFoundryApplicationModule>();
		List<CloudFoundryApplicationModule> externalModules = new ArrayList<CloudFoundryApplicationModule>();
		final Set<IModule> deletedModules = new HashSet<IModule>();

		synchronized (this) {

			// There are three representations for an application:
			// 1. CloudApplication, which represents an existing application in
			// the
			// Cloud space
			// 2. WST IModule created by the WST framework that is the local
			// representation of the application used by the WST framework
			// 3. CloudFoundryApplicationModule which contains additional
			// Cloud-specific API not found in the WST IModule or
			// CloudApplication
			// and always
			// maps to a WST Module and CloudApplication.

			// This refresh mechanism will make sure all three are updated and
			// synchronised

			// Iterate through the local WST modules, and update them based on
			// which are external (have no accessible workspace resources),
			// which
			// have no corresponding deployed application .
			// Note that some IModules may also be in the process of being
			// deleted. DO NOT recreate cloud application modules for these
			// CHANGE
			for (IModule module : server.getModules()) {
				// Find the corresponding Cloud Foundry application module for
				// the given WST server IModule
				CloudFoundryApplicationModule cloudModule = getCloudModule(module);

				if (cloudModule == null) {
					CloudFoundryPlugin.logError("Unable to find local Cloud Foundry application module for : " //$NON-NLS-1$
							+ module.getName()
							+ ". Try refreshing applications or disconnecting and reconnecting to the server."); //$NON-NLS-1$
					continue;
				}

				// Now process the deployed application, and re-categorise it if
				// necessary (i.e. whether it's external or not)
				CloudApplication actualApplication = deployedApplications.remove(cloudModule
						.getDeployedApplicationName());

				// Update the cloud module mapping to the cloud application,
				// such that the cloud module
				// has the latest cloud application reference.
				cloudModule.setCloudApplication(actualApplication);

				// the modules maps to an existing application
				if (actualApplication != null) {
					if (cloudModule.isExternal()) {
						externalModules.add(cloudModule);
					}
					allModules.add(cloudModule);
				}
				else if (getData() != null && getData().isModuleBeingAdded(module)) {
					// deployment is still in progress
					allModules.add(cloudModule);
				}
				else {
					// the module maps to an application that no longer exists
					deletedModules.add(module);
				}
			}

			// create modules for new applications
			if (getData() != null) {
				for (CloudApplication application : deployedApplications.values()) {
					CloudFoundryApplicationModule appModule = getData().createModule(application);
					externalModules.add(appModule);
					allModules.add(appModule);
				}
			}

			// update state for cloud applications
			server.setExternalModules(externalModules.toArray(new IModule[0]));

			for (IModule module : server.getModules()) {
				CloudFoundryApplicationModule appModule = getExistingCloudModule(module);
				if (appModule != null) {

					// Set app stats before updating module state in server as
					// state may be dependent on the application stats
					ApplicationStats stats = applicationStats.get(appModule.getDeployedApplicationName());
					appModule.setApplicationStats(stats);
				}
			}

			// FIXNS: This seems to trigger an infinite "recursion", since
			// deleteModules(..) delegates to the server behaviour, which then
			// attempts to delete modules in a server instance that is not saved
			// and when server behaviour delete operation is complete, it will
			// trigger a refresh operation which then proceeds to update
			// modules, but since WST still indicates that the module has not
			// been deleted
			// deleteModule size will not be empty, which will again invoke the
			// server behaviour...
			// update state for deleted applications to trigger a refresh
			if (deletedModules.size() > 0) {
				for (IModule module : deletedModules) {
					server.setModuleState(new IModule[] { module }, IServer.STATE_UNKNOWN);
				}
				doDeleteModules(deletedModules);
			}

			if (getData() != null) {
				getData().removeObsoleteModules(allModules);
			}
		}
	}
	
	/**
	 * Updates the module state of all existing Cloud modules in the server
	 * @throws CoreException
	 */
	public void updateModulesState() throws CoreException {
		updateModulesState(null);
	}
	
	/**
	 * Updates all modules in the server except those whose state matches the list of states to skip.
	 */
	public void updateModulesState(int[] skipModulesInThisState) throws CoreException {
		synchronized (this) {
			Server server = (Server) getServer();

			for (IModule module : server.getModules()) {
				CloudFoundryApplicationModule appModule = getExistingCloudModule(module);
				if (appModule != null) {

					boolean update = true;

					if (skipModulesInThisState != null) {
						int currentModState = server.getModuleState(new IModule[] { module });
						for (int state : skipModulesInThisState) {
							if (currentModState == state) {
								update = false;
								break;
							}
						}
					}
					if (update) {
						updateState(server, appModule);
					}
				}
			}
		}
	}
	
	
	/**
	 * Update the local (WST) ( {@link IModule} ) and corresponding cloud module
	 * ( {@link CloudFoundryApplicationModule} ) such that they are in synch
	 * with the actual deployed applications (represented by
	 * {@link CloudApplication} ). Local WST modules ( {@link IModule} ) that do
	 * not have a corresponding deployed application ( {@link CloudApplication})
	 * will be removed.
	 * @param deployedApplications
	 * @throws CoreException
	 */
	public void updateModules(Map<String, CloudApplication> deployedApplications, Map<String, ApplicationStats> applicationStats) throws CoreException {
		addAndDeleteModules(deployedApplications, applicationStats);
		updateModulesState();
	}


	/**
	 * Updates the {@link IModule} and {@link ICloudFoundryApplicationModule}
	 * associated with the given {@link CloudApplication}. If a null
	 * {@link CloudApplication} is specified, the update operation will first
	 * check if the app is not currently being deployed. Otherwise it will
	 * delete any obsolete modules for that application.
	 * <p/>
	 * There are three representations for an application:
	 * <p/>
	 * 1. {@link CloudApplication}, which represents an existing application in
	 * the Cloud space.
	 * <p/>
	 * 2. WST {@link IModule} created by the WST framework that is the local
	 * representation of the application used by the WST framework
	 * <p/>
	 * 3. {@link ICloudFoundryApplicationModule} which contains additional
	 * Cloud-specific API not found in the WST IModule or CloudApplication and
	 * always maps to a WST Module and CloudApplication.
	 * <p/>
	 * This refresh mechanism will make sure all three are updated and in synch
	 * with one another
	 * <p/>
	 * To update the module, either the {@link CloudApplication} or application
	 * name, or both are needed. Otherwise it is not possible to update a module
	 * with both of these two arguments missing, and null is returned
	 * 
	 * @param existingCloudApplication a Cloud application the Cloud space
	 * @param appName name of the application
	 * @param stats instance information about the application. Null if not available
	 * @param monitor
	 * @return updated Cloud module, if it exists, or null, if it no longer
	 * exists and has been deleted both locally in the server instance as well
	 * as the Cloud space
	 * @throws CoreException if failure occurs while attempting to update the
	 * {@link CloudFoundryApplicationModule}
	 */
	public CloudFoundryApplicationModule updateModule(CloudApplication existingCloudApplication, String appName, ApplicationStats stats,
			IProgressMonitor monitor) throws CoreException {

		if (appName == null && existingCloudApplication != null) {
			appName = existingCloudApplication.getName();
		}

		if (existingCloudApplication == null && appName == null) {
			return null;
		}

		Server server = (Server) getServer();

		List<CloudFoundryApplicationModule> externalModules = new ArrayList<CloudFoundryApplicationModule>();

		synchronized (this) {

			IModule wstModule = null;
			CloudFoundryApplicationModule correspondingCloudModule = null;

			// Now check if WST and CloudFoundryApplicationModule exist for
			// that
			// application:
			// 1. Find if a WST module exists for the pushed application.
			// This
			// is necessary if the corresponding Cloud module needs to be
			// created or publish state of the application needs to be
			// updated
			// later on
			// 2. Determine all external modules. This is required keep the
			// list
			// of external modules in the server accurate and correct.
			for (IModule module : server.getModules()) {
				// Find the wst module for the application, if it exists
				CloudFoundryApplicationModule cloudModule = getExistingCloudModule(module);

				if (cloudModule != null) {
					if (cloudModule.getDeployedApplicationName().equals(appName)) {
						wstModule = module;
						correspondingCloudModule = cloudModule;
					}
					// Any other external module should be placed back in
					// the list of external modules. The only module of
					// interest
					// is the one that matches the given appName.
					else if (cloudModule.isExternal()) {
						externalModules.add(cloudModule);
					}
				}
			}

			// Update the module cache, and if necessary create a module if one
			// does not yet exists for CloudApplications
			// that exist
			if (getData() != null) {

				// If the cloudApplication exists, then either the cloud
				// module needs to be created or mapping updated.
				if (existingCloudApplication != null) {
					// if it doesn't exist create it
					if (correspondingCloudModule == null) {
						// Module needs to be created for the existing
						// application
						correspondingCloudModule = getData().createModule(existingCloudApplication);
						externalModules.add(correspondingCloudModule);
					}
					else {
						// If module already exists then just update the mapping
						// and be sure to add it back to list of externals if it
						// previously existed and was external
						if (correspondingCloudModule.isExternal()) {
							externalModules.add(correspondingCloudModule);
						}
						correspondingCloudModule.setCloudApplication(existingCloudApplication);
					}
					
					
					// Update stats BEFORE updating module state in server as this provides
					// information to correctly determine module run state
					if (correspondingCloudModule != null) {
						correspondingCloudModule.setApplicationStats(stats);
					}
				}
				// if cloud application does not exist, first check that it
				// is not currently being deployed. Otherwise
				// delete it, as it means it no longer exists in the Cloud space
				else if (!getData().isModuleBeingAdded(wstModule)) {

					// Remove the WST module from WST server if it is still
					// present
					if (wstModule != null) {
						server.setModuleState(new IModule[] { wstModule }, IServer.STATE_UNKNOWN);
						deleteModule(wstModule);
					}

					// Remove the cloud module from the catch
					if (correspondingCloudModule != null) {
						getData().remove(correspondingCloudModule);
						correspondingCloudModule = null;
					}
				}

			}

			// Must update all external modules, not just the module that
			// changed, otherwise the
			// list of refreshed modules may be inaccurate
			server.setExternalModules(externalModules.toArray(new IModule[0]));
			for (IModule module : server.getModules()) {
				CloudFoundryApplicationModule appModule = getExistingCloudModule(module);
				if (appModule != null) {
					updateState(server, appModule);
				}
			}

			return correspondingCloudModule;

		}
	}

	private void deleteModule(IModule module) {
		List<IModule> modsToDelete = new ArrayList<IModule>();
		modsToDelete.add(module);
		doDeleteModules(modsToDelete);
	}

	public void removeApplication(CloudFoundryApplicationModule cloudModule) {
		if (getData() != null) {
			getData().remove(cloudModule);
		}
	}

	public IServer getServerOriginal() {
		// if a working copy is saved the delegate is replaced so getServer() is
		// not guaranteed to return an original even if the delegate was
		// accessed from an original
		IServer server = getServer();
		if (server instanceof IServerWorkingCopy) {
			return ((IServerWorkingCopy) server).getOriginal();
		}
		return server;
	}

	String getServerAttribute(String key, String defaultValue) {
		return super.getAttribute(key, defaultValue);
	}

	@Override
	public void saveConfiguration(IProgressMonitor monitor) throws CoreException {
		String serverId = getServerId();
		if (secureStoreDirty || (serverId != null && !serverId.equals(initialServerId))) {

			if (getData() != null) {
				getData().updateServerId(initialServerId, serverId);

				// cache password
				getData().setPassword(password);
			}

			// persist password
			ServerCredentialsStore store = getCredentialsStore();
			store.setUsername(getUsername());
			store.setPassword(password);
			store.flush(serverId);

			this.initialServerId = serverId;
			this.secureStoreDirty = false;
		}
		super.saveConfiguration(monitor);
	}

	public void setAndSavePassword(String password) {
		this.password = password;

		// remove password in case an earlier version stored it in server
		// properties
		if (getServerWorkingCopy() != null) {
			// We only null the old property here, as the new property is never set.
			getServerWorkingCopy().setAttribute(OLD_PROP_PASSWORD_ID, (String) null);
		}

		if (getData() != null) {
			getData().setPassword(password);
		}

		String serverId = getServerId();

		// persist password
		ServerCredentialsStore store = getCredentialsStore();
		store.setUsername(getUsername());
		store.setPassword(password);
		store.flush(serverId);
	}

	
	public void setAndSaveToken(String token) {
		this.token = token;

		if (getData() != null) {
			getData().setToken(token);
		}

		String serverId = getServerId();

		// Persist token
		ServerCredentialsStore store = getCredentialsStore();
		store.setToken(token);
		store.flush(serverId);
	}
	
	public IStatus doDeleteModules(final Collection<IModule> deletedModules) {
		IServerWorkingCopy wc = getServer().createWorkingCopy();
		try {
			deleteServicesOnModuleRemove.set(Boolean.FALSE);
			wc.modifyModules(null, deletedModules.toArray(new IModule[deletedModules.size()]), null);
			wc.save(true, null);

			// Note: This is normally performed as part of a server publish
			// operation in WST, but to avoid doing a
			// a full server publish, yet complete a proper module deletion and
			// cache clearing, it is performed here.
			IServer server = getServer();

			if (server instanceof Server) {
				final List<IModule[]> modulesToVisit = new ArrayList<IModule[]>();

				final Server serv = (Server) server;
				serv.visit(new IModuleVisitor() {
					public boolean visit(IModule[] module) {
						if (serv.getModulePublishState(module) == IServer.PUBLISH_STATE_NONE) {
							serv.getServerPublishInfo().fill(module);
						}

						modulesToVisit.add(module);
						return true;
					}
				}, new NullProgressMonitor());

				serv.getServerPublishInfo().removeDeletedModulePublishInfo(serv, modulesToVisit);
				serv.getServerPublishInfo().save();
			}

		}
		catch (CoreException e) {
			// log error to avoid pop-up dialog
			CloudFoundryPlugin
					.getDefault()
					.getLog()
					.log(new Status(IStatus.ERROR, CloudFoundryPlugin.PLUGIN_ID,
							"Unexpected error while updating modules", e)); //$NON-NLS-1$
			return Status.CANCEL_STATUS;
		}
		finally {
			deleteServicesOnModuleRemove.set(Boolean.TRUE);
		}
		return Status.OK_STATUS;
	}

	public void moduleAdditionCompleted(IModule module) {
		synchronized (this) {
			if (getData() != null) {
				getData().moduleAdditionCompleted(module);
			}
		}
	}

	/**
	 * @return Cloud application module, if it exists for the given app name.
	 * Null otherwise.
	 */
	public CloudFoundryApplicationModule getExistingCloudModule(String appName) throws CoreException {

		if (appName == null) {
			return null;
		}
		CloudFoundryApplicationModule appModule = null;
		Collection<CloudFoundryApplicationModule> modules = getExistingCloudModules();
		if (modules != null) {
			for (CloudFoundryApplicationModule module : modules) {
				if (appName.equals(module.getDeployedApplicationName())) {
					appModule = module;
					break;
				}
			}
		}
		return appModule;
	}

	/**
	 * Convenience method to set signed certificate for server URLs that do not
	 * yet have a server instance (e.g. when managing server URLs)
	 * @param isSelfSigned true if server uses self-signed certificate
	 * @param cloudServerURL non-null Cloud Foundry server URL
	 */
	public static void setSelfSigned(boolean isSelfSigned, String cloudServerURL) {
		try {
			new SelfSignedStore(cloudServerURL).setSelfSignedCert(isSelfSigned);
		}
		catch (Throwable e) {
			CloudFoundryPlugin.logError(e);
		}
	}

	public static boolean isSelfSigned(String cloudServerURL) {
		try {
			return new SelfSignedStore(cloudServerURL).isSelfSignedCert();
		}
		catch (Throwable e) {
			CloudFoundryPlugin.logError(e);
		}
		return false;
	}

	public URL getModuleRootURL(final IModule curModule) {
		// Only publish if the server publish state is not synchronized.
		CloudFoundryApplicationModule cloudModule = getCloudModule(curModule);
		if (cloudModule == null) {
			return null;
		}

		// verify that URIs are set, as it may be a standalone application with
		// no URI
		List<String> uris = cloudModule != null && cloudModule.getApplication() != null ? cloudModule.getApplication()
				.getUris() : null;
		if (uris != null && !uris.isEmpty()) {
			try {
				return new URL("http://" + uris.get(0)); //$NON-NLS-1$
			}
			catch (MalformedURLException e) {
				CloudFoundryPlugin.logError(e);
			}
		}
		return null;
	}

	/**
	 * Returns all modules that can be launched via the Open Home Page dialog In
	 * the form an array of IModule[], which represents the structure of modules
	 * @param root The root module Open Home Page is based on
	 * @return modules that can be launched via Open Home Page dialog
	 */
	public IModule[][] getLaunchableModules(IModule root) {
		// For CF servers, default to an array of IModule containing only the
		// root module.
		// This preserves the original behavior of homePageUrl in
		// OpenHomePageCommand
		// by setting the contextRoot to null, and launches the default
		// application entry URL.

		return new IModule[][] { new IModule[] { root } };
	}

	/**
	 * Get the context root of a given module
	 * @param module The module to get context root from
	 * @return The context root of given module
	 */
	public String getLaunchableModuleContextRoot(IModule[] module) {
		// For CF servers, default to null.
		// This preserves the original behavior of homePageUrl in
		// OpenHomePageCommand
		// by setting the contextRoot to null, and launches the default
		// application entry URL.
		return null;
	}
	
	/** Returns true if the server contains the org.eclipse.cf.* properties, thus indicating it was created after the refactor of Eclipse CF plug-in names. This value
	 * may be used to determine which set of server properties to read/write from (eg. either org.eclipse.cf.*, or org.cloudfoundry.ide.eclipse.*) */
	private boolean isNewCFEclipseServer() {
		String oldPropServerIdVal = getAttribute(OLD_PROP_SERVER_ID, (String)null);
		
		if(oldPropServerIdVal != null) {
			return false;
		} else {		
			// If the PROP_SERVER_ID value is not set, it is a new server; if the value is set, it is still a new server.
			return true;
			
		}
	}
	
	public HttpProxyConfiguration getProxyConfiguration() {
		// Default returns nothing as proxy configuration is not yet supported.
		// May be removed and replaced with v2 client equivalent
		return null;
	}
	
	public String getPasscode() {
		return getAttribute(PROP_PASSCODE_ID, "");
	}
	
	public void setPasscode(String passcode) {
		setAttribute(PROP_PASSCODE_ID, passcode);
		updateServerId();
	}

	public void setSso(boolean selection) {
		setAttribute(PROP_SSO_ID, selection);
		updateServerId();
	}
	
	/** Convert an array of modules to a String for debugging.*/
	private static String moduleListToString(IModule[] module) {
		String moduleStr = "{ ";
		if(module != null) {
			for(int x = 0; x < module.length; x++) {
				IModule currModule = module[x];
				
				if(currModule == null) { continue; } 
				
				moduleStr += currModule.getName()+" ["+currModule.getId()+"/"+(currModule.getModuleType() != null ? currModule.getModuleType().getId() : "")  +"]";
				
				if(x+1 < module.length) {
					moduleStr += ", ";
				}
			}
		}
		moduleStr = moduleStr.trim() + "}";
		
		return moduleStr;
	}

	/** Convert a call to modifyModules(...) to a String, for debugging */
	private static String convertModifyModulesToString(IModule[] addModules, IModule[] removeModules) { 
		try {
			return "CloudFoundryServer.modifyModules(...): add: "+moduleListToString(addModules) +" remove: "+moduleListToString(removeModules);
			
		} catch(Exception t) {
			// This method is for logging only; we should not throw exceptions to calling methods under any circumstances.
		}
		
		return "";
	}
	
}
