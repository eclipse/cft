/*******************************************************************************
 * Copyright (c) 2016, 2017 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.core.internal.jrebel;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.core.resources.IProject;
import org.osgi.framework.Bundle;

public class ReflectionHandler {

	private ReflectionErrorHandler errorHandler;

	public ReflectionHandler() {

	}

	public void addErrorHandler(ReflectionErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public Method getRemotingProject(Class<?> providerClass) {
		String methodName = "getRemotingProject";//$NON-NLS-1$
		Throwable t = null;
		Method method = null;
		try {
			method = providerClass.getMethod(methodName, IProject.class);
		}
		catch (NoSuchMethodException e) {
			t = e;
		}
		catch (SecurityException e) {
			t = e;
		}
		if (method == null) {
			handleError(methodName, providerClass.getName(), t);
		}
		return method;
	}

	public Method getAddServerUrlMethod(Class<?> integrationClass) {

		String methodName = "addServer";//$NON-NLS-1$
		Throwable t = null;
		Method method = null;
		try {
			method = integrationClass.getDeclaredMethod(methodName, new Class<?>[] { URI.class, String.class });
		}
		catch (NoSuchMethodException e) {
			t = e;
		}
		catch (SecurityException e) {
			t = e;
		}
		if (method == null) {
			handleError(methodName, integrationClass.getName(), t);
		}
		return method;
	}

	public Method getRemoveServerUrlMethod(Class<?> integrationClass) {
		String methodName = "removeServer";//$NON-NLS-1$
		Throwable t = null;
		Method method = null;
		try {
			method = integrationClass.getDeclaredMethod(methodName, new Class<?>[] { URI.class });
		}
		catch (NoSuchMethodException e) {
			t = e;
		}
		catch (SecurityException e) {
			t = e;
		}
		if (method == null) {
			handleError(methodName, integrationClass.getName(), t);
		}
		return method;
	}

	public Class<?> getRebelRemotingProvider(Bundle bundle) {
		Class<?> providerClass = null;
		String className = "org.zeroturnaround.eclipse.jrebel.remoting.RebelRemotingProvider"; //$NON-NLS-1$
		Throwable t = null;
		try {
			providerClass = bundle.loadClass(className);
		}
		catch (Throwable e) {
			t = e;
		}
		if (providerClass == null) {
			handleError(className, bundle.getSymbolicName(), t);
		}

		return providerClass;
	}

	public Class<?> getJRebelIntegration(Bundle bundle) {
		Class<?> providerClass = null;
		String className = "org.zeroturnaround.eclipse.api.JRebelIntegration"; //$NON-NLS-1$
		Throwable t = null;
		try {
			providerClass = bundle.loadClass(className);
		}
		catch (Throwable e) {
			t = e;
		}

		if (providerClass == null) {
			handleError(className, bundle.getSymbolicName(), t);
		}

		return providerClass;
	}

	protected void handleError(String memberId, String containerId, Throwable t) {
		if (errorHandler != null) {
			errorHandler.errorLoading(memberId, containerId, t);
		}
	}

	public interface ReflectionErrorHandler {

		public void errorLoading(String memberId, String containerId, Throwable t);

	}

	public void addServerUrl(Method addServerUrlsMethod, Map<String, URI> serverUrls, Runnable onSuccess) {
		CloudFoundryPlugin.getCallback().syncRunInUi(() -> addUrl(addServerUrlsMethod, serverUrls, onSuccess));
	}
	
	protected void addUrl(Method addServerUrlsMethod, Map<String, URI> serverUrls, Runnable onSuccess) {
		if (serverUrls != null && !serverUrls.isEmpty()) {
			Throwable t = null;

			try {
				addServerUrlsMethod.setAccessible(true);

				for (Entry<String, URI> entry : serverUrls.entrySet()) {
					addServerUrlsMethod.invoke(null, entry.getValue(), entry.getKey());
				}
				if (onSuccess != null) {
					onSuccess.run();
				}
			}
			catch (Throwable e) {
				t = e;
			}

			if (t != null) {
				handleError(addServerUrlsMethod.getName(), addServerUrlsMethod.getDeclaringClass().getName(), t);
			}
		}
	}
	
	public void removeServerUrl(Method removeServerUrlMethod, List<URI> uris, Runnable onSuccess) {
		CloudFoundryPlugin.getCallback().syncRunInUi(() -> removeUrl(removeServerUrlMethod, uris, onSuccess));
	}
	
	protected void removeUrl(Method removeServerUrlMethod, List<URI> uris, Runnable onSuccess) {
		if (uris != null && !uris.isEmpty()) {
			Throwable t = null;

			try {
				removeServerUrlMethod.setAccessible(true);
				for (URI uri : uris) {
					removeServerUrlMethod.invoke(null, uri);
				}
				if (onSuccess != null) {
					onSuccess.run();
				}
			}
			catch (Throwable e) {
				t = e;
			}

			if (t != null) {
				handleError(removeServerUrlMethod.getName(), removeServerUrlMethod.getDeclaringClass().getName(), t);
			}
		}
	}
}
