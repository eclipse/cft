/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others
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
package org.eclipse.cft.server.core.internal;

import java.util.Date;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;

public class Logger implements DebugOptionsListener {

	public void optionsChanged(DebugOptions options) {

		Logger.ERROR = options.getBooleanOption(CloudFoundryPlugin.PLUGIN_ID + Logger.ERROR_LEVEL, false);
		Logger.WARNING = options.getBooleanOption(CloudFoundryPlugin.PLUGIN_ID + Logger.WARNING_LEVEL, false);
		Logger.INFO = options.getBooleanOption(CloudFoundryPlugin.PLUGIN_ID + Logger.INFO_LEVEL, false);
		Logger.DETAILS = options.getBooleanOption(CloudFoundryPlugin.PLUGIN_ID + Logger.DETAILS_LEVEL, false);
	}

	/**
	 * Trace a specific message. Remember to always wrap this call in an if statement and check for the corresponding
	 * enablement flag.
	 * 
	 * @param level
	 *            The tracing level.
	 * @param curClass
	 *            The class being traced.
	 * @param methodName
	 *            The method being traced.
	 * @param msgStr
	 *            The trace string
	 */
	public final static void println(final String level, @SuppressWarnings("rawtypes") Class curClass, final String methodName, final String msgStr) {

		Logger.print(level, curClass, methodName, msgStr, null);
	}

	/**
	 * Trace a specific message and {@link Throwable}. Remember to always wrap this call in an if statement and check
	 * for the corresponding enablement flag.
	 * 
	 * @param level
	 *            The tracing level.
	 * @param curClass
	 *            The class being traced.
	 * @param methodName
	 *            The method being traced.
	 * @param msgStr
	 *            The trace string
	 * @param t
	 *            The {@link Throwable} to print as part of the tracing.
	 */
	public final static void println(final String level, @SuppressWarnings("rawtypes") Class curClass, final String methodName, final String msgStr,
			final Throwable t) {

		Logger.print(level, curClass, methodName, msgStr, t);
	}

	/**
	 * Trace a specific message. Remember to always wrap this call in an if statement and check for the corresponding
	 * enablement flag.
	 * 
	 * @param level
	 *            The tracing level.
	 * @param obj
	 *            The {@link Object} being traced.
	 * @param methodName
	 *            The method being traced.
	 * @param msgStr
	 *            The trace string
	 */
	public final static void println(final String level, final Object obj, final String methodName, final String msgStr) {

		Class<?> objClass = (obj != null) ? obj.getClass() : null;
		Logger.print(level, objClass, methodName, msgStr, null);
	}

	/**
	 * Trace a specific message and {@link Throwable}. Remember to always wrap this call in an if statement and check
	 * for the corresponding enablement flag.
	 * 
	 * @param level
	 *            The tracing level.
	 * @param obj
	 *            The {@link Object} being traced.
	 * @param methodName
	 *            The method being traced.
	 * @param msgStr
	 *            The trace string
	 * @param t
	 *            The {@link Throwable} to print as part of the tracing.
	 */
	public final static void println(final String level, final Object obj, final String methodName,
			final String msgStr, final Throwable t) {

		Class<?> objClass = (obj != null) ? obj.getClass() : null;
		Logger.print(level, objClass, methodName, msgStr, t);
	}

	private final static void print(final String level, final Class<?> clazz, final String methodName,
			final String msgStr, final Throwable t) {

		final StringBuffer printStrBuf = new StringBuffer();
		printStrBuf.append(new Date());
		printStrBuf.append(" "); //$NON-NLS-1$
		printStrBuf.append(level);
		printStrBuf.append(" "); //$NON-NLS-1$
		if (clazz != null) {
			printStrBuf.append(clazz.getName());
		}
		if (methodName != null) {
			printStrBuf.append("."); //$NON-NLS-1$
			printStrBuf.append(methodName);
			printStrBuf.append(": "); //$NON-NLS-1$
		}
		if (msgStr != null) {
			printStrBuf.append(msgStr);
		}

		// write the output to the System.out stream
		System.out.println(printStrBuf.toString());
		if (t != null) {
			System.out.print(level + " " + t); //$NON-NLS-1$
			t.printStackTrace(System.out);
		}
	}

	// tracing enablement flags
	public static boolean ERROR = false;

	public static boolean WARNING = false;

	public static boolean INFO = false;

	public static boolean DETAILS = false;

	// tracing levels
	public final static String ERROR_LEVEL = "/debug/error"; //$NON-NLS-1$

	public final static String WARNING_LEVEL = "/debug/warning"; //$NON-NLS-1$

	public final static String INFO_LEVEL = "/debug/info"; //$NON-NLS-1$

	public final static String DETAILS_LEVEL = "/debug/details"; //$NON-NLS-1$

}