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
package org.eclipse.cft.server.ui.internal.console;

import org.eclipse.cft.server.core.internal.log.LogContentType;
import org.eclipse.swt.SWT;

public class StdStreamProvider extends ConsoleStreamProvider {

	@Override
	public ConsoleStream getStream(LogContentType type) {
		int swtColour = -1;
		if (StandardLogContentType.STD_ERROR.equals(type)) {
			swtColour = SWT.COLOR_RED;
		}
		else if (StandardLogContentType.STD_OUT.equals(type)) {
			swtColour = SWT.COLOR_DARK_MAGENTA;
		}

		return swtColour > -1 ? new SingleConsoleStream(new UILogConfig(swtColour)) : null;
	}

	@Override
	public LogContentType[] getSupportedTypes() {
		return new LogContentType[] { StandardLogContentType.STD_ERROR, StandardLogContentType.STD_OUT };
	}

}
