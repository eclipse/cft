/*******************************************************************************
 * Copyright (c) 2014 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.ui.internal.console.file;

import org.eclipse.swt.SWT;

/**
 * 
 * Local std error content for the Eclipse console. Intention is to write
 * local content to the console using the
 * {@link #write(String, org.eclipse.core.runtime.IProgressMonitor)}
 * <p/>
 * To fetch std error content from a remote server (e.g. a std  log file), use
 * {@link FileConsoleStream} instead.
 */
public class LocalStdErrorConsoleStream extends LocalConsoleStream {

	public LocalStdErrorConsoleStream() {
		super(SWT.COLOR_RED);
	}

	public IContentType getContentType() {
		return StdContentType.STD_ERROR;
	}

}
