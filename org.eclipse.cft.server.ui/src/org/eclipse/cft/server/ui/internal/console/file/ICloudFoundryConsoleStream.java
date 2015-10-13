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
package org.eclipse.cft.server.ui.internal.console.file;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * Defines an Eclipse CF console content. Generally, there is one console
 * content instance per file resource being streamed to the CF console, and the
 * responsibility of the console content is to create a stream that fetches
 * content from a CF server for a particular file and sends it as output to the
 * Eclipse CF console output stream.
 */
public interface ICloudFoundryConsoleStream {

	/**
	 * Fetch and write content to the console.
	 * @param monitor
	 * @return content that is written to the console output stream on each call
	 * to the method
	 * @throws CoreException if error occurred writing to output stream. This
	 * may not necessarily close the stream, as some cases may require
	 * re-attempts even with errors. To have the content manager close the
	 * stream, see {@link #isActive()}
	 */
	public String write(IProgressMonitor monitor) throws CoreException;

	/**
	 * Write given content to the console.
	 * @param content
	 * @return
	 * @throws CoreException
	 */
	public String write(String content) throws CoreException;

	/**
	 * Link the console content to an actual Eclipse console output stream.
	 * @param outputStream to the Eclipse console.
	 */
	public void initialiseStream(IOConsoleOutputStream outputStream);

	/**
	 * Permanently close the content stream. A closed content is always
	 * inactive.
	 */
	public void close();

	/**
	 * 
	 * @return true if stream is open and can still stream content. False
	 * otherwise. Console content managers will use this API to determine if
	 * further streaming requests should be made on the content.
	 */
	public boolean isActive();

	/**
	 * 
	 * @return non-null content type. Identifies the console content and may
	 * enable additional management on the content.
	 */
	public IContentType getContentType();
}
