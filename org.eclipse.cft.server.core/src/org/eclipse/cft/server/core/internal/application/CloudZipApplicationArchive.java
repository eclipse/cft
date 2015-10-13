/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal.application;

import java.io.IOException;
import java.util.zip.ZipFile;

import org.cloudfoundry.client.lib.archive.ZipApplicationArchive;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.core.runtime.CoreException;

public class CloudZipApplicationArchive extends ZipApplicationArchive implements
		CloudApplicationArchive {

	protected final ZipFile zipFile;

	public CloudZipApplicationArchive(ZipFile zipFile) {
		super(zipFile);
		this.zipFile = zipFile;
	}

	@Override
	public void close() throws CoreException {
		try {
			if (zipFile != null) {
				zipFile.close();
			}
		} catch (IOException e) {
			throw CloudErrorUtil.toCoreException(e);
		}
	}
}
