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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.eclipse.cft.server.core.ArchiveEntry;
import org.eclipse.cft.server.core.CFApplicationArchive;

public class ApplicationUtil {

	public static ApplicationArchive asV1ApplicationArchive(CFApplicationArchive cfArchive) {
		if (cfArchive != null) {
			return new V1ApplicationArchiveAdapter(cfArchive);
		}
		return null;
	}

	static class V1ApplicationArchiveAdapter implements ApplicationArchive {

		private final CFApplicationArchive cfArchive;

		public V1ApplicationArchiveAdapter(CFApplicationArchive cfArchive) {
			this.cfArchive = cfArchive;
		}

		@Override
		public String getFilename() {
			return cfArchive.getName();
		}

		@Override
		public Iterable<Entry> getEntries() {
			Iterable<ArchiveEntry> cfEntries = cfArchive.getEntries();
			List<ApplicationArchive.Entry> legacyEntries = new ArrayList<ApplicationArchive.Entry>();
			if (cfEntries != null) {
				for (ArchiveEntry entry : cfEntries) {
					legacyEntries.add(new V1ArchiveEntry(entry));
				}
			}
			return legacyEntries;
		}
	}

	static class V1ArchiveEntry implements ApplicationArchive.Entry {

		private final ArchiveEntry cfEntry;

		public V1ArchiveEntry(ArchiveEntry cfEntry) {
			this.cfEntry = cfEntry;
		}

		@Override
		public boolean isDirectory() {
			return cfEntry.isDirectory();
		}

		@Override
		public String getName() {
			return cfEntry.getName();
		}

		@Override
		public long getSize() {
			return cfEntry.getSize();
		}

		@Override
		public byte[] getSha1Digest() {
			return cfEntry.getSha1Digest();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return cfEntry.getInputStream();
		}
	}
}
