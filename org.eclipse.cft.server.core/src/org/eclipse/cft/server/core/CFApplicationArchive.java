/*******************************************************************************
 * Copyright (c) 2016 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.core;

import org.eclipse.core.runtime.CoreException;

public interface CFApplicationArchive {
	/**
	 *
	 * @return the name of the archive (excluding any path). Cannot be null.
	 */
	String getName();

	/**
	 *
	 * @return a collection of entries contained in the archive
	 */
	Iterable<ArchiveEntry> getEntries();
	
	/**
	 * 
	 * @throws CoreException if failed to close the archive
	 */
	public void close() throws CoreException;
}
