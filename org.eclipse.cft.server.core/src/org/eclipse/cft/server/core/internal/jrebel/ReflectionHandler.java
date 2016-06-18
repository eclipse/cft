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
package org.eclipse.cft.server.core.internal.jrebel;

import java.lang.reflect.Method;
import java.net.URI;

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

	public boolean addServerUrl(Method addServerUrlsMethod, URI uri, String serverName) {
		Throwable t = null;
		try {
			addServerUrlsMethod.invoke(null, uri, serverName);
			return true;
		}
		catch (Throwable e) {
			t = e;
		}

		if (t != null) {
			handleError(addServerUrlsMethod.getName(), addServerUrlsMethod.getDeclaringClass().getName(), t);
		}

		return false;
	}

	public boolean removeServerUrl(Method removeServerUrlMethod, URI uri) {
		Throwable t = null;

		try {
			removeServerUrlMethod.invoke(null, uri);
			return true;
		}
		catch (Throwable e) {
			t = e;
		}

		if (t != null) {
			handleError(removeServerUrlMethod.getName(), removeServerUrlMethod.getDeclaringClass().getName(), t);
		}
		return false;
	}

}
