/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others
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
 *     IBM Corporation - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.ui;

import java.util.regex.Pattern;

import org.eclipse.cft.server.core.AbstractAppStateTracker;
import org.eclipse.cft.server.core.ICloudFoundryApplicationModule;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.client.CloudFoundryApplicationModule;
import org.eclipse.cft.server.ui.internal.Logger;
import org.eclipse.cft.server.ui.internal.console.ConsoleManagerRegistry;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * A general implementation class of the org.eclipse.cft.server.core.internal.AbstractAppStateTracker
 * that provides easy implementation to track console output to decide whether the app is started or not. In
 * most of the cases, the adopter only need to implement the getAppStartedPattern() method.  In more complex cases, 
 * the adopter can override the createPatternMatchListener() to provide their own pattern match listener.
 * 
 * @author Elson Yuen
 */
public abstract class AbstractConsoleMonitorAppStateTracker extends AbstractAppStateTracker {
	private ConsolePatternMatchListener consoleMonitor;
	
	/**
	 * Tracks text appended to the console and notifies listeners in terms of whole
	 * lines.
	 */
	public class ConsolePatternMatchListener implements IPatternMatchListener {

	    private String appName;
		private int appState = IServer.STATE_STARTING;
	    
	    public ConsolePatternMatchListener(String curAppName) {
	    	appName = curAppName;
	    }

		/* (non-Javadoc)
		 * @see org.eclipse.ui.console.IPatternMatchListenerDelegate#connect(org.eclipse.ui.console.TextConsole)
		 */
		public void connect(TextConsole console) {
			// Do nothing
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.ui.console.IPatternMatchListener#disconnect()
		 */
		public synchronized void disconnect() {
			// Do nothing
	    }

	    /* (non-Javadoc)
	     * @see org.eclipse.ui.console.IPatternMatchListener#matchFound(org.eclipse.ui.console.PatternMatchEvent)
	     */
	    public void matchFound(PatternMatchEvent event) {
	    	if (Logger.INFO) {
	    		Logger.println(Logger.INFO_LEVEL, this, "matchFound", "Application start detected: " + appName); //$NON-NLS-1$ //$NON-NLS-2$
	    	}
	    	appState = IServer.STATE_STARTED;
	    }
	    
	    /* (non-Javadoc)
	     * @see org.eclipse.ui.console.IPatternMatchListener#getPattern()
	     */
	    public String getPattern() {
	        return getAppStartedPattern();
	    }

	    /* (non-Javadoc)
	     * @see org.eclipse.ui.console.IPatternMatchListener#getCompilerFlags()
	     */
	    public int getCompilerFlags() {
	        return Pattern.CASE_INSENSITIVE;
	    }

	    /* (non-Javadoc)
	     * @see org.eclipse.ui.console.IPatternMatchListener#getLineQualifier()
	     */
	    public String getLineQualifier() {
	        return "\\n|\\r"; //$NON-NLS-1$
	    }
	    
	    protected int getApplicationState() {
	    	return appState;
	    }
	}
	
	protected ConsolePatternMatchListener createPatternMatchListener(IModule module) {
		return new ConsolePatternMatchListener(module.getName());
	}
	
	/**
	 * Find the message console that corresponds to the server and a given module. If there are multiple instances
	 * of the application, only the first one will get returned.
	 * @param server the server for that console
	 * @param module the app for that console
	 * @return the message console. Null if no corresponding console is found.
	 */
	protected MessageConsole findCloudFoundryConsole(IServer server, IModule module) {
		CloudFoundryServer cfServer = (CloudFoundryServer)server.getAdapter(CloudFoundryServer.class);
		CloudFoundryApplicationModule appModule = cfServer.getExistingCloudModule(module);
		return ConsoleManagerRegistry.getConsoleManager(cfServer).findCloudFoundryConsole(server, appModule);
	}
	
	@Override
	public int getApplicationState(ICloudFoundryApplicationModule appModule) {
		if (Logger.DETAILS) {
			 Logger.println(Logger.DETAILS_LEVEL, this, "getApplicationState", "Waiting for app to start: " + ((IModule)appModule).getName() + ", state=" + consoleMonitor.getApplicationState()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return consoleMonitor.getApplicationState();
	}

	/**
	 * Get the regex pattern that defines that pattern string that matches console messages on each line to 
	 * decide if the application is started.
	 * @return regex pattern for the match pattern.
	 */
    protected abstract String getAppStartedPattern();

	@Override
	public void startTracking(IModule module) {
		if (server == null || module == null) {
			return;
		}

		MessageConsole console = findCloudFoundryConsole(server, module);
		if (console != null) {
			if (Logger.INFO) {
				 Logger.println(Logger.INFO_LEVEL, this, "isApplicationStarted", "Start app state tracking: " + module.getName()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			consoleMonitor = createPatternMatchListener(module);
			console.addPatternMatchListener(consoleMonitor);
		}
	}

	@Override
	public void stopTracking(IModule module) {
		if (server == null || consoleMonitor == null || module == null) {
			return;
		}
		if (Logger.INFO) {
			 Logger.println(Logger.INFO_LEVEL, this, "stopTracking", "Stop app state tracking: " + module.getName()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		MessageConsole console = findCloudFoundryConsole(server, module);
		if (console != null) {
			console.removePatternMatchListener(consoleMonitor);
		}
	}

}
