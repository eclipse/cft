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
package org.eclipse.cft.server.core.internal.application;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.cft.server.core.ArchiveEntry;
import org.eclipse.cft.server.core.CFApplicationArchive;
import org.eclipse.cft.server.core.internal.CloudErrorUtil;
import org.eclipse.core.runtime.CoreException;

public class ZipArchive implements CFApplicationArchive {

	private ZipFile zipFile;

	private List<ArchiveEntry> entries;

	private String name;

	public ZipArchive(ZipFile zipFile) {
		this.zipFile = zipFile;
		this.name = new File(zipFile.getName()).getName();
	}

	private List<ArchiveEntry> getEntries(ZipFile zipFile) {
		List<ArchiveEntry> entries = new ArrayList<ArchiveEntry>();
		Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		while (zipEntries.hasMoreElements()) {
			entries.add(new ZipArchiveEntry(zipEntries.nextElement()));
		}
		return Collections.unmodifiableList(entries);
	}

	public Iterable<ArchiveEntry> getEntries() {
		if (entries == null) {
			this.entries = getEntries(zipFile);
		}
		return entries;
	}

	public String getName() {
		return name;
	}

	public void close() throws CoreException {
		try {
			if (zipFile != null) {
				zipFile.close();
			}
		}
		catch (IOException e) {
			throw CloudErrorUtil.toCoreException(e);
		}
	}

	class ZipArchiveEntry extends AbstractArchiveEntry {

		private ZipEntry entry;

		public ZipArchiveEntry(ZipEntry entry) {
			this.entry = entry;
		}

		public boolean isDirectory() {
			return entry.isDirectory();
		}

		public String getName() {
			return entry.getName();
		}

		public long getSize() {
			return entry.getSize();
		}

		public InputStream getInputStream() throws IOException {
			if (isDirectory()) {
				return null;
			}
			return zipFile.getInputStream(entry);
		}
	}

}
