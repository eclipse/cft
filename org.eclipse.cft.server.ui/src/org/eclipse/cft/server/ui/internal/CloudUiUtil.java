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
 *     IBM - Switching to use the more generic AbstractCloudFoundryUrl
 *     		instead concrete CloudServerURL, deprecating non-recommended methods
 *          Bug 485697 - Implement host name taken check in CF wizards
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.eclipse.cft.server.core.AbstractCloudFoundryUrl;
import org.eclipse.cft.server.core.internal.ApplicationUrlLookupService;
import org.eclipse.cft.server.core.internal.CloudApplicationURL;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryBrandingExtensionPoint;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudUtil;
import org.eclipse.cft.server.core.internal.CloudFoundryBrandingExtensionPoint.CloudServerURL;
import org.eclipse.cft.server.core.internal.client.CloudFoundryClientFactory;
import org.eclipse.cft.server.core.internal.client.CloudFoundryServerBehaviour;
import org.eclipse.cft.server.core.internal.client.DeploymentInfoWorkingCopy;
import org.eclipse.cft.server.core.internal.spaces.CloudOrgsAndSpaces;
import org.eclipse.cft.server.ui.internal.wizards.HostnameValidationResult;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.internal.browser.WebBrowserPreference;
import org.eclipse.ui.internal.browser.WorkbenchBrowserSupport;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;
import org.springframework.web.client.ResourceAccessException;

/**
 * @author Steffen Pingel
 * @author Christian Dupuis
 * @author Terry Denney
 */
@SuppressWarnings("restriction")
public class CloudUiUtil {

	public static final String SERVERS_VIEW_ID = "org.eclipse.wst.server.ui.ServersView"; //$NON-NLS-1$

	public static String ATTR_USER_DEFINED_URLS = "org.eclipse.cft.server.user.defined.urls"; //$NON-NLS-1$

	public static IStatus runForked(final ICoreRunnable coreRunner, IWizard wizard) {
		try {
			IRunnableWithProgress runner = new IRunnableWithProgress() {
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						coreRunner.run(monitor);
					}
					catch (Exception e) {
						throw new InvocationTargetException(e);
					}
					finally {
						monitor.done();
					}
				}
			};
			wizard.getContainer().run(true, false, runner);
		}
		catch (InvocationTargetException e) {
			IStatus status;
			if (e.getCause() instanceof CoreException) {
				status = new Status(IStatus.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, NLS.bind(
						Messages.CloudUiUtil_ERROR_FORK_OP_FAILED, e.getCause().getMessage()), e);
			}
			else {
				status = new Status(IStatus.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, NLS.bind(
						Messages.CloudUiUtil_ERROR_FORK_UNEXPECTED, e.getMessage()), e);
			}
			CloudFoundryServerUiPlugin.getDefault().getLog().log(status);
			IWizardPage page = wizard.getContainer().getCurrentPage();
			if (page instanceof DialogPage) {
				((DialogPage) page).setErrorMessage(status.getMessage());
			}
			return status;
		}
		catch (InterruptedException e) {
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}
	
	private static CloudServerURL convertAbstractCloudFoundryUrlToCloudServerURL (AbstractCloudFoundryUrl abstractUrl) {
		CloudServerURL cloudUrl = null;
		if (abstractUrl != null) {
			// Nothing to do, this is already an old CloudServerURL, just return it
			if (abstractUrl instanceof CloudServerURL) {
				cloudUrl = (CloudServerURL)(abstractUrl);
			} else {
				cloudUrl = new CloudServerURL(abstractUrl.getName(), abstractUrl.getUrl(), abstractUrl.getUserDefined(), 
						abstractUrl.getSignUpUrl(), abstractUrl.getSelfSigned()); 
			}
		}
		return cloudUrl;
	}
	
	private static List<CloudServerURL> convertAbstractCloudFoundryUrlListToCloudServerURLList (List <AbstractCloudFoundryUrl> abstractUrls) {
		if (abstractUrls == null)
			return null;
		
		List<CloudServerURL> urls = new ArrayList<CloudFoundryBrandingExtensionPoint.CloudServerURL>();
		for (AbstractCloudFoundryUrl abstractUrl : abstractUrls) {
			if (abstractUrl != null) {
				urls.add (convertAbstractCloudFoundryUrlToCloudServerURL(abstractUrl));	
			}
		}
		return urls;
	}

	/** 
	 * @deprecated use {@link CloudServerUIUtil#getAllUrls(String, IRunnableContext)}
	 */
	public static List<CloudServerURL> getAllUrls(String serverTypeId) {
		try {
			// Switch to new generic utility method, then convert to the expected return type
			return convertAbstractCloudFoundryUrlListToCloudServerURLList(CloudServerUIUtil.getAllUrls(serverTypeId, null));
		} catch (CoreException ex) {
			CloudFoundryServerUiPlugin.logError(ex);
		}
		
		// If an exception shows up, return an empty list (to have backwards compatibility)
		return new ArrayList<CloudServerURL>();
	}

	/** 
	 * @deprecated use {@link CloudServerUIUtil#getDefaultUrl(String, IRunnableContext)}
	 */
	public static CloudServerURL getDefaultUrl(String serverTypeId) {		
		CloudServerURL url = null;
		try {
			// Switch to new generic utility method, then convert to the expected return type
			AbstractCloudFoundryUrl abstractUrl = CloudServerUIUtil.getDefaultUrl(serverTypeId, null);
			if (abstractUrl != null) {
				url = convertAbstractCloudFoundryUrlToCloudServerURL(abstractUrl);
			} 
		} catch (CoreException ex) {
			CloudFoundryServerUiPlugin.logError(ex);
		}
		
		return url;
	}

	/** 
	 * @deprecated use {@link CloudServerUIUtil#getUrls(String, IRunnableContext)}
	 */
	public static List<CloudServerURL> getUrls(String serverTypeId) {
		try {
			// Switch to new generic utility method, then convert to the expected return type
			return convertAbstractCloudFoundryUrlListToCloudServerURLList(CloudServerUIUtil.getUrls(serverTypeId, null));
		} catch (CoreException ex) {
			CloudFoundryServerUiPlugin.logError(ex);
		}
		
		// If an exception shows up, return an empty list (to have backwards compatibility)
		return new ArrayList<CloudServerURL>();
	}

	/**
	 * @deprecated use {@link CloudServerUIUtil#getUserDefinedUrls(String))}
	 */
	public static List<CloudServerURL> getUserDefinedUrls(String serverTypeId) {
		// Switch to new generic utility method, then convert to the expected return type
		return convertAbstractCloudFoundryUrlListToCloudServerURLList(CloudServerUIUtil.getUserDefinedUrls(serverTypeId));
	}

	/**
	 * @deprecated user {@link CloudServerUIUtil#storeUserDefinedUrls(String, List)}
	 */
	public static void storeUserDefinedUrls(String serverTypeId, List<CloudServerURL> urls) {
		if (urls == null)
			return;
		
		List <AbstractCloudFoundryUrl> abstractUrls = new ArrayList <AbstractCloudFoundryUrl> ();
		for (CloudServerURL cloudUrl : urls) {
			abstractUrls.add(cloudUrl);
		}
		
		// Use the new correct method
		CloudServerUIUtil.storeUserDefinedUrls(serverTypeId, abstractUrls);
	}

	/**
	 * Validates the given credentials. Throws {@link CoreException} if error
	 * occurred during validation.
	 * @param userName
	 * @param password
	 * @param urlText
	 * @param displayURL
	 * @param selfSigned true if its a server using self-signed certificate. If
	 * this information is not known, set this to false
	 * @param context
	 * 
	 * @throws CoreException if validation failed and error type cannot be
	 * determined
	 * @throws OperationCanceledException if validation is cancelled.
	 */
	public static void validateCredentials(final CloudFoundryServer cfServer, final String userName, final String password, final String urlText,
			final boolean displayURL, final boolean selfSigned, IRunnableContext context) throws CoreException,
			OperationCanceledException {

		try {
			ICoreRunnable coreRunner = new ICoreRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					String url = urlText;
					if (displayURL) {
						url = getUrlFromDisplayText(urlText);
					}
					CloudFoundryServerBehaviour.validate(cfServer, url, userName, password, selfSigned, false, null, null, monitor);
				}
			};
			if (context != null) {
				runForked(coreRunner, context);
			}
			else {
				runForked(coreRunner);
			}
		}
		catch (CoreException ce) {
			if (ce.getCause() instanceof ResourceAccessException
					&& ce.getCause().getCause() instanceof javax.net.ssl.SSLPeerUnverifiedException) {
				// Self-signed error. Re-throw as it will involve a client
				// change
				throw CloudErrorUtil.toCoreException(ce.getCause().getCause());
			}
			else {
				throw ce;
			}
		}
	}

	/*
	 * Validates the given SSO credentials. Throws {@link CoreException} if error
	 * occurred during validation.
	 */
	public static void validateSsoCredentials(final CloudFoundryServer cfServer, final String urlText,
			final boolean displayURL, final boolean selfSigned, IRunnableContext context, 
			final String passcode, final String tokenValue) throws CoreException,
			OperationCanceledException {

		try {
			ICoreRunnable coreRunner = new ICoreRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					String url = urlText;
					if (displayURL) {
						url = getUrlFromDisplayText(urlText);
					}
					CloudFoundryServerBehaviour.validate(cfServer, url, null, null, selfSigned, true, passcode, tokenValue, monitor);
				}
			};
			if (context != null) {
				runForked(coreRunner, context);
			}
			else {
				runForked(coreRunner);
			}
		}
		catch (CoreException ce) {
			if (ce.getCause() instanceof ResourceAccessException
					&& ce.getCause().getCause() instanceof javax.net.ssl.SSLPeerUnverifiedException) {
				// Self-signed error. Re-throw as it will involve a client
				// change
				throw CloudErrorUtil.toCoreException(ce.getCause().getCause());
			}
			else {
				throw ce;
			}
		}
	}

	/**
	 * Runnable context can be null. If so, default Eclipse progress service
	 * will be used as a runnable context. Display URL should be true if the
	 * display URL is passed. If so, and attempt will be made to parse the
	 * actual URL.
	 * 
	 * @param userName must not be null
	 * @param password must not be null
	 * @param urlText must not be null. Can be either display or actual URL
	 * @param displayURL true if URL is display URL
	 * @param selfSigned true if connecting to a self-signing server. False otherwise
	 * @param context may be optional
	 * @return spaces descriptor, or null if it couldn't be determined
	 * @throws CoreException
	 */
	public static CloudOrgsAndSpaces getCloudSpaces(final CloudFoundryServer cfServer, final String userName, final String password, final String urlText,
			final boolean displayURL, final boolean selfSigned, IRunnableContext context, final boolean sso, final String passcode, final String tokenValue) throws CoreException {

		try {
			final CloudOrgsAndSpaces[] supportsSpaces = new CloudOrgsAndSpaces[1];
			ICoreRunnable coreRunner = new ICoreRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					String url = urlText;
					if (displayURL) {
						url = getUrlFromDisplayText(urlText);
					}
					if (sso) {
						CloudCredentials credentials = CloudUtil.createSsoCredentials(passcode, tokenValue);
						if (credentials == null) {
							credentials = new CloudCredentials(passcode);
						}
						supportsSpaces[0] = CloudFoundryServerBehaviour.getCloudSpacesExternalClient(cfServer, credentials , url, selfSigned, sso, passcode, tokenValue, monitor);
					} else {
						supportsSpaces[0] = CloudFoundryServerBehaviour.getCloudSpacesExternalClient(cfServer, new CloudCredentials(userName, password), url, selfSigned, monitor);
					}
				}
			};
			if (context != null) {
				runForked(coreRunner, context);
			}
			else {
				runForked(coreRunner);
			}

			return supportsSpaces[0];
		}
		catch (OperationCanceledException e) {
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(e));
		}

	}

	public static String getUrlFromDisplayText(String displayText) {
		String url = displayText;
		if (url != null) {
			int pos = url.lastIndexOf(" - "); //$NON-NLS-1$
			if (pos >= 0) {
				return url.substring(pos + 3);
			}
		}

		return url;
	}

	public static String getDisplayTextFromUrl(String url, String serverTypeId) {
		try {
			List<AbstractCloudFoundryUrl> cloudUrls = CloudServerUIUtil.getAllUrls(serverTypeId, null);
			for (AbstractCloudFoundryUrl cloudUrl : cloudUrls) {
				if (cloudUrl.getUrl().equals(url)) {
					return cloudUrl.getName() + " - " + url; //$NON-NLS-1$
				}
			}
		} catch (CoreException ex) {
			CloudFoundryServerUiPlugin.logError(ex);
		}
		return url;
	}

	public static void runForked(final ICoreRunnable coreRunner) throws OperationCanceledException, CoreException {
		runForked(coreRunner, PlatformUI.getWorkbench().getProgressService());
	}

	public static void runForked(final ICoreRunnable coreRunner, IRunnableContext progressService)
			throws OperationCanceledException, CoreException {
		try {
			IRunnableWithProgress runner = new IRunnableWithProgress() {
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
					try {
						coreRunner.run(monitor);
					}
					catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
					finally {
						monitor.done();
					}
				}

			};
			progressService.run(true, true, runner);
		}
		catch (InvocationTargetException e) {
			if (e.getCause() instanceof CoreException) {
				throw (CoreException) e.getCause();
			}
			else {
				CloudFoundryServerUiPlugin
						.getDefault()
						.getLog()
						.log(new Status(IStatus.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, "Unexpected exception", e)); //$NON-NLS-1$
			}
		}
		catch (InterruptedException e) {
			throw new OperationCanceledException();
		}
	}

	public static void openUrl(String location) {
		openUrl(location, WebBrowserPreference.getBrowserChoice());
	}

	public static void openUrl(String location, int browserChoice) {
		try {
			URL url = null;
			if (location != null) {
				url = new URL(location);
			}
			if (browserChoice == WebBrowserPreference.EXTERNAL) {
				try {
					IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
					support.getExternalBrowser().openURL(url);
				}
				catch (Exception e) {
				}
			}
			else {
				IWebBrowser browser;
				int flags;
				if (WorkbenchBrowserSupport.getInstance().isInternalWebBrowserAvailable()) {
					flags = IWorkbenchBrowserSupport.AS_EDITOR | IWorkbenchBrowserSupport.LOCATION_BAR
							| IWorkbenchBrowserSupport.NAVIGATION_BAR;
				}
				else {
					flags = IWorkbenchBrowserSupport.AS_EXTERNAL | IWorkbenchBrowserSupport.LOCATION_BAR
							| IWorkbenchBrowserSupport.NAVIGATION_BAR;
				}

				String generatedId = "org.eclipse.mylyn.web.browser-" + Calendar.getInstance().getTimeInMillis(); //$NON-NLS-1$
				browser = WorkbenchBrowserSupport.getInstance().createBrowser(flags, generatedId, null, null);
				browser.openURL(url);
			}
		}
		catch (PartInitException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(),
					Messages.CloudUiUtil_ERROR_OPEN_BROWSER_FAIL_TITLE, Messages.CloudUiUtil_ERROR_OPEN_BROWSER_BODY);
		}
		catch (MalformedURLException e) {
			if (location == null || location.trim().equals("")) { //$NON-NLS-1$
				MessageDialog.openInformation(Display.getDefault().getActiveShell(),
						Messages.CloudUiUtil_ERROR_OPEN_BROWSER_FAIL_TITLE,
						NLS.bind(Messages.CloudUiUtil_ERROR_EMPTY_URL_BODY, location));
			}
			else {
				MessageDialog.openInformation(Display.getDefault().getActiveShell(),
						Messages.CloudUiUtil_ERROR_OPEN_BROWSER_FAIL_TITLE,
						NLS.bind(Messages.CloudUiUtil_ERROR_MALFORM_URL_BODY, location));
			}
		}

	}

	/**
	 * Prompts user to define a value for the wildcard in the cloud URL, then
	 * return the new URL
	 * 
	 * @param cloudUrl
	 * @param allCloudUrls
	 * @param shell
	 * @return new URL, null if no wildcard appears in cloudUrl or if user
	 * cancels out of defining a new value
	 * @deprecated use {@link CloudServerUIUtil#getWildcardUrl(AbstractCloudFoundryUrl, List, Shell)} instead.
	 */
	public static CloudServerURL getWildcardUrl(CloudServerURL cloudUrl, List<CloudServerURL> allCloudUrls, Shell shell) {
		// Switch to new generic utility method, then convert to the expected return type
		ArrayList <AbstractCloudFoundryUrl> allCloudFoundryUrls = new ArrayList<AbstractCloudFoundryUrl>();
		if (allCloudUrls != null) {
			for (CloudServerURL _cloudUrl : allCloudUrls) {
				allCloudFoundryUrls.add (_cloudUrl);
			}
		}
		
		AbstractCloudFoundryUrl returnUrl = CloudServerUIUtil.getWildcardUrl(cloudUrl, allCloudFoundryUrls, shell);
		if (returnUrl != null) {
			return new CloudServerURL(returnUrl.getName(), returnUrl.getUrl(), true, returnUrl.getSelfSigned());
		}
		
		return null;
	}

	/**
	 * If the Servers view is available and it contains a selection, the
	 * corresponding structured selection is returned. In any other case,
	 * including the Servers view being unavailable, either because it is not
	 * installed or it is closed, null is returned.
	 * @return structured selection in the Servers view, if the Servers view is
	 * open and available, or null otherwise
	 */
	public static IStructuredSelection getServersViewSelection() {

		IViewRegistry registry = PlatformUI.getWorkbench().getViewRegistry();
		String serversViewID = SERVERS_VIEW_ID;

		// fast check to verify that the servers View is available.
		IViewDescriptor serversViewDescriptor = registry.find(serversViewID);
		if (serversViewDescriptor != null) {

			// Granular null checks required as any of the workbench components
			// may not be available at some given point in time (e.g., during
			// start/shutdown)
			IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

			if (activeWorkbenchWindow != null) {

				IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();

				if (activePage != null) {
					IViewReference[] references = activePage.getViewReferences();

					if (references != null) {
						IViewPart serversViewPart = null;
						for (IViewReference reference : references) {
							if (serversViewID.equals(reference.getId())) {
								serversViewPart = reference.getView(true);
								break;
							}
						}

						if (serversViewPart != null) {

							IViewSite viewSite = serversViewPart.getViewSite();
							if (viewSite != null) {
								ISelectionProvider selectionProvider = viewSite.getSelectionProvider();
								if (selectionProvider != null) {
									ISelection selection = selectionProvider.getSelection();
									if (selection instanceof IStructuredSelection) {
										return (IStructuredSelection) selection;
									}
								}
							}
						}
					}
				}
			}

		}
		return null;
	}

	/**
	 * Returns the current shell or null.
	 * @return
	 */
	public static Shell getShell() {
		return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
	}

	
	public static UniqueSubdomain getUniqueSubdomain(String url, CloudFoundryServer server, IProgressMonitor monitor) throws CoreException {
			if (url == null) return null; // Incorrect usage. Provide a non-null string
			
			ApplicationUrlLookupService lookup = ApplicationUrlLookupService.getCurrentLookup(server);
			CloudApplicationURL cloudUrl = null;
			boolean isUniqueURL = true;
			int intSuffix = 1;
			
			UniqueSubdomain result = null;
			
			if(monitor == null) {
				// Monitor is optional, so use a NullProgressMonitor.
				monitor = new NullProgressMonitor();
			}
			
			List<CloudRoute> routes = null;
				
			do {

				try {
					// It does NOT check if the URL is taken already, even if valid.					
					cloudUrl = lookup.getCloudApplicationURL(url);
				} catch(CoreException e) {
					// if error occurred, eg. url is null, then don't check for host taken and simply return
					break;
				}
				
				if(cloudUrl == null) {
					// if error occurred, eg. url is null, then don't check for host taken and simply return
					break;
				}
													
				if(routes == null) {
					routes = server.getBehaviour().getRoutes(cloudUrl.getDomain(), monitor);
				}
				
				boolean isFound = false; // is in route list?
				boolean isRouteReservedAndUnused = false; // is route reserve and unused?
				boolean isRouteCreated = false; // did we create the route in reserveRoute?
				
				// First check the existing cloud routes
				for(CloudRoute cr : routes) {
					// If we own the route...
					if(cr.getHost().equalsIgnoreCase(cloudUrl.getSubdomain())) {
						isFound = true;
						isRouteCreated = false;
						
						if(!cr.inUse()) {
							isRouteReservedAndUnused = true;
						} else {
							isRouteReservedAndUnused = false;								
						}
						
						break;
					}
				}
				
				if(!isFound) {
					// If the route was not found in the getRoutes(...) list, then attempt to reserve it
					
					isRouteReservedAndUnused = server.getBehaviour().reserveRouteIfAvailable(cloudUrl.getSubdomain(), cloudUrl.getDomain(), monitor);
					isRouteCreated = isRouteReservedAndUnused;
				}

				if(isRouteReservedAndUnused) {
					result = new UniqueSubdomain();
					result.setRouteCreated(isRouteCreated);
					result.setCloudUrl(cloudUrl);
					
					// break or just set boolean to true
					isUniqueURL = true;
					
				} else {
					// The route is taken (as determined either by checking the route list, or by attempting to reserve)
					
					isUniqueURL = false;
					StringBuilder sb = new StringBuilder(url);
					String subdomain = cloudUrl.getSubdomain();
					// Get the last integers in the subdomain
					Pattern p = Pattern.compile("(\\d+)$");  //$NON-NLS-N$
					Matcher m = p.matcher(subdomain);
					// If it ends with a number, then simply increment it by one to get the new candidate subdomain name
					if (m.find()) {
						// Examples: subdomain could be MyApp1 or MyApp99 or MyApp2015
						String intSuffixString = m.group(1); // The number can be any length
						intSuffix = Integer.parseInt(intSuffixString);
						int beginning = subdomain.indexOf(intSuffixString);
						int length = intSuffixString.length();
						// Examples: MyApp1 to MyApp2;  MyApp99 to MyApp100; MyApp2015 to MyApp2016
						// Increment intSuffix first
						url = sb.replace(beginning, beginning+length, Integer.toString(++intSuffix)).toString();
					} else { // Otherwise, simply append 1 to the end of the subdomain
						// Example: subdomain = MyApp --> MyApp1
						url = sb.insert(sb.indexOf(subdomain) + subdomain.length(), "1").toString();  
					}						
				}
					
			} while (!isUniqueURL && intSuffix < Integer.MAX_VALUE - 1 && !monitor.isCanceled()); // Support suffix up to the number 2^31 - 1
			
			return result;
		}
		
		/**
		 * Run the host name validator in the context of the wizard container
		 * 
		 * @param appUrl
		 * @param server
		 * @param monitor
		 * @param message
		 * @return
		 */
		public static HostnameValidationResult validateHostname(CloudApplicationURL appUrl, CloudFoundryServer server, IWizardContainer container) {
			return validateHostname(appUrl, server, container, null);
		}

		/**
		 * Run the host name validator in the context of the wizard container and provide a custom error message
		 * @param appUrl
		 * @param server
		 * @param container
		 * @param message - override with custom error message
		 * @return
		 */
		public static HostnameValidationResult validateHostname(CloudApplicationURL appUrl, CloudFoundryServer server, IWizardContainer container, String message) {
			HostnameValidator val = message == null ? new HostnameValidator(appUrl, server) : new HostnameValidator(appUrl, server, message);
			try {
				container.run(true,  true,  val);
			}
			catch (Exception e) {
				CloudFoundryPlugin.logWarning("Hostname taken validation was not completed. " + e.getMessage()); //$NON-NLS-1$

				IStatus status = new Status(IStatus.ERROR, CloudFoundryServerUiPlugin.PLUGIN_ID, Messages.CloudApplicationUrlPart_ERROR_UNABLE_TO_CHECK_HOSTNAME);				
				return new HostnameValidationResult(status, val.isRouteCreated());
			}
			
			return new HostnameValidationResult(val.getStatus(), val.isRouteCreated());
		}
		
		/**
		 * Clean up reserved URLs except the URL to keep (urlToKeep) from the context of a wizard.  If urlToKeep is null, then all
		 * reserved URLs will be removed.
		 * 
		 * @param wizard - the wizard
		 * @param server - the CloudFoundryServer
		 * @param reservedUrls - list of CloudApplicationURL that have been reserved
		 * @param urlToKeep - The URL to keep from the list of reservedUrls
		 */
		public static void cleanupReservedRoutes(IWizard wizard, final CloudFoundryServer server, List<CloudApplicationURL> reservedUrls, String urlToKeep) {
			for (CloudApplicationURL cloudURL : reservedUrls) {
				// Don't remove the one that is needed
				if (urlToKeep != null && urlToKeep.equals(cloudURL.getUrl())) {
					continue;
				}
				deleteRoute(wizard, server, cloudURL);
			}
			reservedUrls.clear();
		}

		/**
		 * Clean up reserved URLs except those specified in the deployment info, from the context of a wizard
		 * 
		 * @param workingCopy
		 * @param wizard - the wizard
		 * @param server - the CloudFoundryServer
		 * @param reservedUrls - the list of all reserved route URLS
		 */
		public static void cleanupReservedRoutesIfNotNeeded(DeploymentInfoWorkingCopy workingCopy, IWizard wizard, final CloudFoundryServer server, List<CloudApplicationURL> reservedUrls) {
			List<String> urls = workingCopy.getUris();
			if (urls == null) {
				urls = new ArrayList<String>();
			}
			// Clean up unused routes that were reserved and no longer needed
			for (CloudApplicationURL cloudURL : reservedUrls) {
				boolean isNeeded = false;

				for (String url : urls) {
					if (url.equals(cloudURL.getUrl())) {
						isNeeded = true;
						break;
					}
				}
				if (!isNeeded) {
					deleteRoute(wizard, server, cloudURL);
				}
			}
			reservedUrls.clear();
		}
		
		private static void deleteRoute(final IWizard wizard, final CloudFoundryServer server, final CloudApplicationURL fCloudURL) {
			try {
				wizard.getContainer().run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							server.getBehaviour().deleteRoute(fCloudURL.getSubdomain(), fCloudURL.getDomain(), monitor);
						}
						catch (CoreException e) {
							CloudFoundryPlugin.logError(e);
						}
						finally {
							monitor.done();
						}
					}
				});
			} catch (InterruptedException e) {
				CloudFoundryPlugin.logWarning("The following route was not deleted: " + fCloudURL.getUrl());  //$NON-NLS-N$					
			} catch (InvocationTargetException e) {
				CloudFoundryPlugin.logWarning("The following route was not deleted: " + fCloudURL.getUrl());  //$NON-NLS-N$
			}
		}
		

	/** Returned by the getUniqueSubdomain(...) method; if successful it contains the unique cloud subdomain url,
	 * and whether or not the route needed to be created (a route does not need to be created if it already exists) */
	public static class UniqueSubdomain {
		private CloudApplicationURL cloudUrl = null;
		private boolean routeCreated = false;
	
		
		public CloudApplicationURL getCloudUrl() {
			return cloudUrl;
		}
		
		public boolean isRouteCreated() {
			return routeCreated;
		}
		
		public void setCloudUrl(CloudApplicationURL cloudUrl) {
			this.cloudUrl = cloudUrl;
		}
		
		public void setRouteCreated(boolean routeCreated) {
			this.routeCreated = routeCreated;
		}
		
	}
	
	public static String getPromptText(CloudFoundryServer cfServer) {
		String ssoUrl = "";
		String href = null;
		if (cfServer.getUrl() != null && !cfServer.getUrl().isEmpty()) {
			try {
				href = CloudFoundryClientFactory.getSsoUrl(cfServer.getUrl(), cfServer.getSelfSignedCertificate());
				if (href != null && !href.isEmpty()) {
					ssoUrl = Messages.bind(Messages.PASSCODE_PROMPT2, href);
				} else {
					ssoUrl = Messages.PASSCODE_IS_NOT_SUPPORTED;
				}
			}
			catch (Exception e1) {
				CloudFoundryServerUiPlugin.logError(e1);
			}
		}
		return ssoUrl;
	}

}
