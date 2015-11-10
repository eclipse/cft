/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core.internal.debug;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;

/**
 * Handles debugger termination events.
 * 
 */
public class ConnectToDebuggerListener implements IDebugEventSetListener {

	private Object eventSource;

	private String launchId;

	public ConnectToDebuggerListener(String launchId, Object eventSource) {
		this.eventSource = eventSource;
		this.launchId = launchId;
	}

	public void handleDebugEvents(DebugEvent[] events) {
		if (events != null) {
			int size = events.length;
			for (int i = 0; i < size; i++) {

				if (events[i].getKind() == DebugEvent.TERMINATE) {

					Object source = events[i].getSource();

					// THe event source should be a thread as remote debugging
					// does not launch a separate local process
					// However, multiple threads may be associated with the
					// debug target, so to check if an app
					// is disconnected from the debugger, additional termination
					// checks need to be performed on the debug target
					// itself
					if (source instanceof IThread) {
						IDebugTarget debugTarget = ((IThread) source).getDebugTarget();

						// Be sure to only handle events from the debugger
						// source that generated the termination event. Do not
						// handle
						// any other application that is currently connected to
						// the debugger.
						if (eventSource.equals(debugTarget) && debugTarget.isDisconnected()) {

							DebugPlugin.getDefault().removeDebugEventListener(this);
							ApplicationDebugLauncher.terminateLaunch(launchId);
							return;
						}
					}
				}
			}
		}
	}

}