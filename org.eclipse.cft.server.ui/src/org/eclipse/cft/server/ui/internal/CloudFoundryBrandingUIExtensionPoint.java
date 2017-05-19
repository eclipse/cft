/*******************************************************************************
 * Copyright (c) 2015,2017 IBM Corporation and others 
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
 *     IBM Corp. - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.ui.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cft.server.core.internal.CloudFoundryBrandingExtensionPoint;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

public class CloudFoundryBrandingUIExtensionPoint extends CloudFoundryBrandingExtensionPoint {

	public static String ATTR_NAME = "name"; //$NON-NLS-1$

	public static String ATTR_SERVER_TYPE_ID = "serverTypeId"; //$NON-NLS-1$

	public static String ATTR_WIZ_BAN = "wizardBanner"; //$NON-NLS-1$

	public static String ATTR_IS_SHOW_STACK_UI = "isShowStackUI"; //$NON-NLS-1$

	public static String POINT_ID = "org.eclipse.cft.server.ui.brandingUI"; //$NON-NLS-1$

	private static Map<String, IConfigurationElement> brandingUIDefinitions = new HashMap<String, IConfigurationElement>();
	
	private static List<String> brandingUIServerTypeIds = new ArrayList<String>();

	private static boolean read;

	private static void readBrandingUIDefinitions() {
		IExtensionPoint brandingUIExtPoint = Platform.getExtensionRegistry().getExtensionPoint(POINT_ID);
		if (brandingUIExtPoint != null) {
			
			// Ensure core branding is initialized first
			CloudFoundryBrandingExtensionPoint.readBrandingDefinitions();
			
			brandingUIServerTypeIds.clear();
			for (IExtension extension : brandingUIExtPoint.getExtensions()) {
				for (IConfigurationElement config : extension.getConfigurationElements()) {
					String serverId = config.getAttribute(ATTR_SERVER_TYPE_ID);
					String name = config.getAttribute(ATTR_NAME);
					if (serverId != null && serverId.trim().length() > 0 && name != null && name.trim().length() > 0) {
						brandingUIDefinitions.put(serverId, config);
						brandingUIServerTypeIds.add(serverId);
					}
				}
			}
			read = true;
		}
	}

	public static List<String> getServerTypeIds() {
		if (!read) {
			readBrandingUIDefinitions();
		}

		return brandingUIServerTypeIds;
	}

	public static String getServiceName(String serverTypeId) {
		if (!read) {
			readBrandingUIDefinitions();
		}
		IConfigurationElement config = brandingUIDefinitions.get(serverTypeId);
		if (config != null) {
			return config.getAttribute(ATTR_NAME);
		}
		return null;
	}

	public static String getWizardBannerPath(String serverTypeId) {
		if (!read) {
			readBrandingUIDefinitions();
		}
		IConfigurationElement config = brandingUIDefinitions.get(serverTypeId);
		if (config != null) {
			return config.getAttribute(ATTR_WIZ_BAN);
		}
		return null;
	}

	public static boolean isShowStackUI(String serverTypeId) {
		if (!read) {
			readBrandingUIDefinitions();
		}
		IConfigurationElement config = brandingUIDefinitions.get(serverTypeId);
		if (config != null) {
			String curIsShowStackUIStr = config.getAttribute(ATTR_IS_SHOW_STACK_UI);
			// Default value is true.
			return curIsShowStackUIStr == null ? true : Boolean.valueOf(config.getAttribute(ATTR_IS_SHOW_STACK_UI));
		}
		return true;
	}
}