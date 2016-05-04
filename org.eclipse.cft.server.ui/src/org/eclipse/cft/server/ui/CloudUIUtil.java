/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others
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
 *     IBM Corporation - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.ui;

import java.util.List;

import org.eclipse.cft.server.core.AbstractCloudFoundryUrl;
import org.eclipse.cft.server.ui.internal.CFUiUtil;
import org.eclipse.cft.server.ui.internal.CloudServerUIUtil;
import org.eclipse.core.runtime.CoreException;

/**
 * Wrapper class that exposes as API some internal methods to be used outside of
 * the current plugin.
 */
public class CloudUIUtil {

	/**
	 * Open the given URL on a browser
	 * @param url the URL value to be launched.
	 */
	public static void openUrl(String url) {
		CFUiUtil.openUrl(url);
	}

	/**
	 * Returns the list of all available urls (default and non-default) for the
	 * given server type.
	 * 
	 * @param serverTypeId the server type id to look for in the records.
	 * @return a list of AbstractCloudFoundryUrl containing all known urls for
	 * the current server type id.
	 * @throws CoreException basically if operation is cancelled.
	 */
	public static List<AbstractCloudFoundryUrl> getAllUrls(String serverTypeId) throws CoreException {
		return CloudServerUIUtil.getAllUrls(serverTypeId, null);
	}

	/**
	 * Returns the available default url (if there is one) for the given server
	 * type.
	 * 
	 * @param serverTypeId the server type id to look for in the records.
	 * @return an AbstractCloudFoundryUrl representing the default url or null
	 * if there isn't one.
	 * @throws CoreException basically if operation is cancelled.
	 */
	public static AbstractCloudFoundryUrl getDefaultUrl(String serverTypeId) throws CoreException {
		return CloudServerUIUtil.getDefaultUrl(serverTypeId, null);
	}

	/**
	 * Returns the list of all non-default Urls (provided by the framework or
	 * user-defined) for the given server type.
	 * 
	 * @param serverTypeId the server type id to look for in the records.
	 * @return a list of AbstractCloudFoundryUrl containing all known
	 * non-default Urls.
	 * @throws CoreException basically if operation is cancelled.
	 */
	public static List<AbstractCloudFoundryUrl> getUrls(String serverTypeId) throws CoreException {
		return CloudServerUIUtil.getUrls(serverTypeId, null);
	}
}
