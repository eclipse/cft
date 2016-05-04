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

import java.io.IOException;
import java.io.InputStream;

/**
 * A single entry contained within an {@link CFApplicationArchive}. Entries are
 * used to represent both files and directories.
 */
public interface ArchiveEntry {

	/**
	 * @return true if the entry is a directory.
	 */
	boolean isDirectory();

	/**
	 *
	 * @return the name of the entry including a path. Should not start with '/'
	 */
	String getName();

	/**
	 *
	 * @return the size of the entry, or 0 if it is a directory
	 */
	long getSize();

	/**
	 *
	 * @return SHA1 digest, or null if it is a directory
	 */
	byte[] getSha1Digest();

	/**
	 *
	 * @return the file contents if entry is a file. Null if it is a directory.
	 * Caller must close the stream.
	 * @throws IOException
	 */
	InputStream getInputStream() throws IOException;
}