/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others 
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
 *     IBM - Initial contribution.
 ********************************************************************************/

package org.eclipse.cft.server.core.internal;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CFInfoLogger {
	
	private static final String CRLF = System.getProperty("line.separator"); 
	
	/** Synchronize on this before writing; notify when written. */
	private final List<String> messagesToWrite = new ArrayList<String>();

	private final File logFile;

	/** Logging thread - not started until first message of the session. */
	private CFInfoLoggerThread thread;
	
	public CFInfoLogger(File f) throws IOException {
		logFile = f;
	}
	
	public void log(String str) {
		synchronized(messagesToWrite) {
			if(thread == null) {
				thread = new CFInfoLoggerThread(logFile);
				thread.start();				
			}
			
			messagesToWrite.add(str);
			messagesToWrite.notify();
		}
	}
	
	
	// Write and flush on a separate thread, to prevent blocking the logging thread on slow file IO.
	private class CFInfoLoggerThread extends Thread {
		private final SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d yyyy hh:mm:ss aaa"); // Wed Jul 4 2001 12:08:22 PM 
		
		private final File f;
		private boolean threadIsRunning = true;
		
		public CFInfoLoggerThread(File f) {
			
			this.f = f;			
		}
				
		@Override
		public void run() {
			final List<String> localCopy = new ArrayList<String>();

			// Sanity check: don't allow the file to grow larger than 512 KB (note: it should never even get close)
			boolean appendLog = (f.exists() && f.length() > 1024 * 512) ? false : true;

			FileWriter fw = null;
			try {
				
				while(threadIsRunning) {
					
					// Wait for a new message, then copy to local				
					synchronized(messagesToWrite) {
						try {
							while(messagesToWrite.size()  == 0 && threadIsRunning) {
								messagesToWrite.wait();
							}
							
							localCopy.addAll(messagesToWrite);
							messagesToWrite.clear();						
						}
						catch (InterruptedException e) {
							e.printStackTrace();
							threadIsRunning = false;
						}
					}
					
					if(localCopy.size()  > 0) {
						
						if(fw == null) {
							fw = new FileWriter(f, appendLog);
						}
						
						String currTimestamp = sdf.format(new Date());
				
						StringBuilder sb = new StringBuilder();
						
						for(String msg : localCopy) {
								sb.append(currTimestamp);
								sb.append(" - ");
								sb.append(msg);
								sb.append(CRLF);
						}
						
						fw.write(sb.toString());
						fw.flush();
						
						localCopy.clear();
					}						
					
				} // end-while

			}
			catch (IOException e1) {
				// On IOException, log to the console and terminate the thread.
				e1.printStackTrace();
				threadIsRunning = false;
			}
			
		}
		
		@SuppressWarnings("unused")
		public void setThreadIsRunning(boolean threadIsRunning) {
			this.threadIsRunning = threadIsRunning;
		}
		
	}

}
