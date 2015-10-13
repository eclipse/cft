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
package org.eclipse.cft.server.core.internal.client;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public abstract class WaitWithProgressJob extends AbstractWaitWithProgressJob<Boolean> {

	public WaitWithProgressJob(int attempts, long sleepTime) {
		super(attempts, sleepTime);
	}

	@Override
	protected Boolean runInWait(IProgressMonitor monitor) throws CoreException {
		boolean result = internalRunInWait(monitor);
		return new Boolean(result);
	}

	abstract protected boolean internalRunInWait(IProgressMonitor monitor) throws CoreException;

	@Override
	protected boolean isValid(Boolean result) {
		return result != null && result.booleanValue();
	}

}
