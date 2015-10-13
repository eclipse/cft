/*******************************************************************************
 * Copyright (c) 2013, 2014 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal;

import org.eclipse.core.runtime.Platform;

public class PlatformUtil {

	private static String os;

	/**
	 * 
	 * @return OS as defined by Platform
	 * @see Platform
	 */
	public static String getOS() {

		if (os == null) {

			os = Platform.getOS();

			if (os != Platform.OS_MACOSX && os != Platform.OS_LINUX) {
				String osName = System.getProperty("os.name").toLowerCase(); //$NON-NLS-1$
				if (osName != null && osName.startsWith("windows")) { //$NON-NLS-1$
					os = Platform.OS_WIN32;
				}
			}

		}

		return os;
	}

}
