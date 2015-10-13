/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jst.server.core.RuntimeClasspathProviderDelegate;
import org.eclipse.wst.server.core.IRuntime;
import org.osgi.framework.Bundle;

/**
 * IMPORTANT NOTE: This class can be referred by the branding extension from adopter so this class 
 * should not be moved or renamed to avoid breakage to adopters.
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class CloudFoundryRuntimeClasspathProvider extends RuntimeClasspathProviderDelegate {

	String[] CP_BUNDLES = { "javax.servlet", "javax.servlet.jsp" }; //$NON-NLS-1$ //$NON-NLS-2$

	public IClasspathEntry[] resolveClasspathContainer(IProject project, IRuntime runtime) {
		List<IClasspathEntry> cp = new ArrayList<IClasspathEntry>(2);
		for (String id : CP_BUNDLES) {
			Bundle bundle = Platform.getBundle(id);
			try {
				File file = FileLocator.getBundleFile(bundle);
				Path path = new Path(file.getCanonicalPath());
				cp.add(JavaCore.newLibraryEntry(path, null, null));
			}
			catch (IOException e) {
				// ignore
			}
		}
		return cp.toArray(new IClasspathEntry[0]);
	}

}
