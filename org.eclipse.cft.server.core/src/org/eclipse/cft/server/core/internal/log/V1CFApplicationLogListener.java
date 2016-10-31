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
package org.eclipse.cft.server.core.internal.log;

import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.domain.ApplicationLog;

/**
 * 
 *
 */
public class V1CFApplicationLogListener implements ApplicationLogListener {

	final private CFApplicationLogListener cfListener;

	public V1CFApplicationLogListener(CFApplicationLogListener cfListener) {
		this.cfListener = cfListener;
	}

	@Override
	public void onComplete() {
		cfListener.onComplete();
	}

	@Override
	public void onError(Throwable error) {
		cfListener.onError(error);
	}

	@Override
	public void onMessage(ApplicationLog log) {
		cfListener.onMessage(AppLogUtil.getLogFromV1(log));
	}

}
