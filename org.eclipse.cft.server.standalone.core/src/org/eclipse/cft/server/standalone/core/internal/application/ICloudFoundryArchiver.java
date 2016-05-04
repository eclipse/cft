/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others. 
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
package org.eclipse.cft.server.standalone.core.internal.application;

import org.eclipse.cft.server.core.CFApplicationArchive;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

public interface ICloudFoundryArchiver {

	public void initialize(IModule module, IServer server) throws CoreException;

	public CFApplicationArchive getApplicationArchive(IProgressMonitor monitor) throws CoreException;

}
