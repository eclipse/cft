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

import java.util.List;

/**
 * Defines a Standalone start command for a given runtime type. A start command
 * may be defined by multiple start command types. For example, a Java start
 * command may defined a java application start command "java ..." or a script
 * file.
 * <p/>
 * If defining multiple start command definitions, a default start command type
 * can also be specified.
 */
public abstract class StartCommand {

	/**
	 * The start command in the form that it would be used to start the
	 * application.
	 */
	abstract public String getStartCommand();

	abstract public StartCommandType getDefaultStartCommandType();

	abstract public List<StartCommandType> getStartCommandTypes();

	abstract public String getArgs();

}
