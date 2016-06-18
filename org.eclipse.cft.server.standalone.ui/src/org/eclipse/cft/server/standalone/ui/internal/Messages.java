/*******************************************************************************
 * Copyright (c) 2014, 2016 Pivotal Software, Inc. 
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

package org.eclipse.cft.server.standalone.ui.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = CloudFoundryJavaStandaloneUIPlugin.PLUGIN_ID + ".internal.Messages"; //$NON-NLS-1$

	public static String JavaCloudFoundryArchiver_REPACKAGING_SPRING_BOOT_APP;

	public static String JavaCloudFoundryArchiver_PACKAGING_APPLICATION_COMPLETED;

	public static String JavaCloudFoundryArchiver_PACKAGING_APPLICATION;

	public static String JavaCloudFoundryArchiver_PACKAGING_MAIN_TYPE;

	public static String JavaCloudFoundryArchiver_FOUND_ARCHIVE_FROM_MANIFEST;

	public static String JavaCloudFoundryArchiver_REFRESHING_PROJECT;

	public static String JavaCloudFoundryArchiver_ERROR_ARCHIVER_NOT_INITIALIZED;

	public static String JavaCloudFoundryArchiver_ERROR_CREATE_CF_ARCHIVE;

	public static String JavaCloudFoundryArchiver_ERROR_CREATE_PACKAGED_FILE;

	public static String JavaCloudFoundryArchiver_ERROR_CREATE_TEMP_DIR;

	public static String JavaCloudFoundryArchiver_ERROR_JAVA_APP_PACKAGE;

	public static String JavaCloudFoundryArchiver_ERROR_NO_JAVA_PROJ_RESOLVED;

	public static String JavaCloudFoundryArchiver_ERROR_NO_MAIN;

	public static String JavaCloudFoundryArchiver_ERROR_NO_MAIN_CLASS_IN_MANIFEST;

	public static String JavaCloudFoundryArchiver_ERROR_MANIFEST_NOT_ACCESSIBLE;

	public static String JavaCloudFoundryArchiver_ERROR_FAILED_READ_MANIFEST;

	public static String JavaCloudFoundryArchiver_ERROR_NO_PACKAGE_FRAG_ROOTS;

	public static String JavaCloudFoundryArchiver_ERROR_NO_PACKAGED_FILE_CREATED;

	public static String JavaCloudFoundryArchiver_ERROR_REPACKAGE_SPRING;

	public static String JavaTypeUIAdapter_JOB_JAVA_ASSIST;

	public static String ProjectExplorerMenuFactory_JOB_DISABLE;

	public static String ProjectExplorerMenuFactory_JOB_ENABLE;

	public static String ProjectExplorerMenuFactory_LABEL_CONVERT_TEXT;
	public static String ProjectExplorerMenuFactory_LABEL_CONVERT_TOOLTIP;
	public static String ProjectExplorerMenuFactory_LABEL_REMOVE_TEXT;
	public static String ProjectExplorerMenuFactory_LABEL_REMOVE_TOOLTIP;

	public static String SelectMainTypeWizardPage_LABEL;

	public static String SelectMainTypeWizardPage_TITLE;

	public static String SelectMainTypeWizardPage_NO_SHELL;

	public static String SelectMainTypeWizardPage_WIZARD_DESCRIPTION;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
