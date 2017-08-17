/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others 
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
 *     IBM Corporation - initial implementation
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.client;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.cft.server.core.ISshClientSupport;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/** 
 * A simple thread-safe connection pool for SSH sessions used by the DiegoRequestFactory.getFile(..) method. 
 *  
 **/
public class FileSshSessionConnPool {

	/** Synchronize when accessing, key and value are thread-safe */
	private final HashMap<MapKey /** CloudApplication+Instance Index*/, MapValue> sessionMap = new HashMap<>();
	
	private final CloudFoundryServerBehaviour behaviour;
	
	/** Synchronize on access */
	private final AtomicInteger numberOfActiveConnections = new AtomicInteger();
	
	private final Object supportLock = new Object();
	/** Synchronize on supportLock before accessing; we only ever request a new sshClientSupport from a single thread. */
	private ISshClientSupport sshClientSupport;

	private final long MAX_ACTIVE_CONNECTIONS = 5;

	/** Maximum length of time that we try to establish an SSH connection before giving up. */
	private final long MAX_CONNECTION_ATTEMPT_TIME_IN_NANOS = TimeUnit.NANOSECONDS.convert(120, TimeUnit.SECONDS); 

	public FileSshSessionConnPool(CloudFoundryServerBehaviour behaviour) {
		this.behaviour = behaviour;
	}
	
		
	/** Thread-safe; processes the file request, and returns the file/dir contents (if possible, otherwise a CoreException is thrown.) */
	public String processSshSessionRequest(CloudApplication app, int instanceIndex, final String path, final boolean isDir, IProgressMonitor monitor) throws CoreException {
		
		MapKey key = new MapKey(app, instanceIndex);

		MapValue value;
		synchronized(sessionMap) {
			value = sessionMap.get(key);
		}

		if(value == null) {
			value = new MapValue();
			synchronized(sessionMap) {
				sessionMap.put(key, value);
			}
		} 
		
		return runWithSession(key, value, monitor, path, isDir);
		
	}
	
	/** Process the request with an available SSH session, otherwise establish a new one (if not above max connections limit).*/
	private String runWithSession(MapKey key, MapValue value, IProgressMonitor monitor, final String path, final boolean isDir) throws CoreException {
		
		String fileResult = null; // This value should not be read unless requestProcessed is true.
		boolean requestProcessed = false;
		
		Exception lastExceptionThrown = null;
		
		Session session = null;
	
		long expireTimeInNanos = System.nanoTime() + MAX_CONNECTION_ATTEMPT_TIME_IN_NANOS;
		
		while(!requestProcessed && System.nanoTime() < expireTimeInNanos) {
			
			session = value.acquireSessionIfAvailable().orElse(null);
				
			if(session != null) {
				// If there is already a connection in the pool, then use it

				RequestResult result = runWithSessionInner(value, session, path, isDir);
				
				requestProcessed = result.isRequestProcessed();
				fileResult = result.getResult();
				lastExceptionThrown = result.getLastExceptionThrown().orElse(null);
				
			} else {
				
				// If there is not already a connection in the pool, then establish a new connection
				boolean areWeOverConnLimit = false;
				synchronized(numberOfActiveConnections) {
					if(numberOfActiveConnections.get() >= MAX_ACTIVE_CONNECTIONS) {
						areWeOverConnLimit = true;
					} else {
						areWeOverConnLimit = false;
						numberOfActiveConnections.incrementAndGet();
					}
				}
				
				if(!areWeOverConnLimit) {
				
					try {
						synchronized (supportLock) {
							if(sshClientSupport == null) {
								sshClientSupport = behaviour.getSshClientSupport(monitor);
							}
						}
						
						session = sshClientSupport.connect(key.getApp().getName(), key.getIndex(), behaviour.getCloudFoundryServer().getServer(), monitor);
						
						RequestResult result = runWithSessionInner(value, session, path, isDir);
						requestProcessed = result.isRequestProcessed();
						fileResult = result.getResult();
						lastExceptionThrown = result.getLastExceptionThrown().orElse(null);
												
					} catch (CoreException e) {
						/* ignore, this is thrown by getSshClientSupport; we will try to establish the connection again after a short delay. */
						lastExceptionThrown = e;
					}
					
				} else {
					/** We have too many active connections, so just wait for one to finish. */
				}
			}
			
			if(!requestProcessed) {
								
				// Wait between failures.
				try { Thread.sleep(1000); } catch (InterruptedException e) { throw new RuntimeException(e); }
				
			}
			
		}
		
		if(!requestProcessed) {
			//  If the request was never successsfully processed, throw a status that includes the most recent thrown exception
			throw new CoreException(CloudFoundryPlugin.getErrorStatus(Messages.SshFileSessionPool_UNABLE_TO_ESTABLISH_CONNECTION, lastExceptionThrown));
		}
		
		return fileResult;
		
	}
	
	/** Call JSCH to retrieve the file, and handle cleanup if an error occurs */
	private RequestResult runWithSessionInner(MapValue value, Session session, String path, boolean isDir) {

		boolean processed = false; // Whether the user's session request completed w/o error.
		boolean errorOccured = false; // Whether a jsch error occurred at any point.
		
		Exception lastExceptionThrown = null; // For diagnostic purposes only
		
		String result = null;
		try {
			Channel channel = session.openChannel("exec");
			try {
				
				String command = isDir ? "ls -p " + path //$NON-NLS-1$
						// Basic work-around to scp which doesn't appear to work
						// well. Returns empty content for existing files.
						: "cat " + path; //$NON-NLS-1$

				((ChannelExec) channel).setCommand(command);

				result = getContent(channel);

				processed = true;
				
			} finally {
				channel.disconnect();
			}
			
		} catch(Exception e) {
			/* This will be either JSchException or IOException, but every exception should be caught here, to avoid failing to trigger the cleanup logic.
			 * Any exceptions we will safely ignore, and the SSH session will be terminated and not reused. */
			errorOccured = true;
			lastExceptionThrown = e;
		}

		if(errorOccured) {
			try { session.disconnect(); } catch(Exception e2) { /* ignore */ }

			synchronized(numberOfActiveConnections) {
				numberOfActiveConnections.decrementAndGet();
			}
		} else {
			// If an error did not occur, it is safe to return the session to the pool
			value.releaseSession(session);
		}
		
		return new RequestResult(processed, result, lastExceptionThrown);
		
	}

	/** Read JSch Channel into String */
	private static String getContent(Channel channel) throws IOException, JSchException {
		InputStream in = null;
		OutputStream outStream = null;
		in = channel.getInputStream();
		channel.connect();

		try {
			if (in != null) {
	
				ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
				outStream = new BufferedOutputStream(byteArrayOut);
				byte[] buffer = new byte[4096];
				int bytesRead = -1;
	
				while ((bytesRead = in.read(buffer)) != -1) {
					outStream.write(buffer, 0, bytesRead);
				}
				outStream.flush();
				byteArrayOut.flush();
	
				return byteArrayOut.toString();
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (outStream != null) {
				outStream.close();
			}
		}
		return null;
	}

	
	// Inner Classes ----------------------
	
	private static class RequestResult {
		private final boolean requestProcessed;
		private final String userResult; // May be null, if the user returned null.
		
		private final Exception lastExceptionThrown;
		
		public RequestResult(boolean requestProcessed, String userResult, Exception lastExceptionThrown) {
			this.requestProcessed = requestProcessed;
			this.userResult = userResult;
			this.lastExceptionThrown = lastExceptionThrown;
		}
		
		public boolean isRequestProcessed() {
			return requestProcessed;
		}
		
		public String getResult() {
			return userResult;
		}
		
		public Optional<Exception> getLastExceptionThrown() {
			return Optional.ofNullable(lastExceptionThrown);
		}
		
	}
	
	/** Combination of CloudApplication and index to create a single map key; Thread-safe. */
	private static class MapKey {
		private final CloudApplication app;
		private final int index;
		
		public MapKey(CloudApplication app, int index) {
			this.app = app;
			this.index = index;
		}
		
		public CloudApplication getApp() {
			return app;
		}
		
		public int getIndex() {
			return index;
		}
		
		@Override
		public int hashCode() {
			return app.hashCode()+index;
		}
		
		@Override
		public boolean equals(Object o) {
			MapKey other = (MapKey)o;
			if(!other.app.equals(app)) {
				return false;
			}
			return index == other.index;
		}
			
	}
	
	/** List of SSH sessions that are available to be reused; Thread-safe .*/
	private static class MapValue {

		/** Synchronize on access */
		private final List<Session> availableSessions = new ArrayList<Session>();
		
		public MapValue() {
		}
		
		public Optional<Session> acquireSessionIfAvailable() {
			synchronized (availableSessions) {
				if(availableSessions.size() > 0) {
					return Optional.of(availableSessions.remove(0));
				}
			}
			
			return Optional.empty();
		}

		public void releaseSession(Session s) {
			synchronized (availableSessions) {
				availableSessions.add(s);
			}
		}
	}
}
