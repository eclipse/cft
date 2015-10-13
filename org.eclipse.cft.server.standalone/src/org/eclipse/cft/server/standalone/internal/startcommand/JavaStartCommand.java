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
package org.eclipse.cft.server.standalone.internal.startcommand;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class JavaStartCommand extends StartCommand {

	public static final String DEFAULT_LIB = "lib"; //$NON-NLS-1$

	public static final IPath DEFAULT_LIB_PATH = Path.EMPTY.append(DEFAULT_LIB);

	public JavaStartCommand() {
		super();
	}

	@Override
	public String getStartCommand() {
		StringWriter writer = new StringWriter();
		writer.append("java"); //$NON-NLS-1$
		if (getArgs() != null) {
			writer.append(" "); //$NON-NLS-1$
			writer.append(getArgs());
		}
		return writer.toString();
	}

	protected String getClassPathOptionArg() {
		StringWriter options = new StringWriter();
		options.append(DEFAULT_LIB);
		options.append("/"); //$NON-NLS-1$
		options.append("*"); //$NON-NLS-1$
		options.append(":"); //$NON-NLS-1$
		options.append("."); //$NON-NLS-1$
		return options.toString();
	}

	public String getArgs() {
		StringWriter options = new StringWriter();
		options.append("$JAVA_OPTS"); //$NON-NLS-1$
		options.append(" "); //$NON-NLS-1$
		options.append("-cp"); //$NON-NLS-1$
		options.append(" "); //$NON-NLS-1$
		options.append(getClassPathOptionArg());
		return options.toString();
	}

	@Override
	public StartCommandType getDefaultStartCommandType() {
		return StartCommandType.Java;
	}

	/**
	 * May be empty, but never null
	 * @return
	 */
	public List<StartCommandType> getStartCommandTypes() {
		return Arrays.asList(new StartCommandType[] { StartCommandType.Java, StartCommandType.Other });
	}

}