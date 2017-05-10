/*******************************************************************************
 * Copyright (c) 2017 Pivotal Software, Inc. and others
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
 *     IBM - initial API and implementation
 ********************************************************************************/
package org.eclipse.cft.server.core.internal.application;

/**
 * Constants related to the deployment manifest file.
 * @author eyuen
 */
public class ManifestConstants {
	
	public static final String APPLICATIONS_PROP = "applications"; //$NON-NLS-1$

	public static final String NAME_PROP = "name"; //$NON-NLS-1$

	public static final String MEMORY_PROP = "memory"; //$NON-NLS-1$

	public static final String INSTANCES_PROP = "instances"; //$NON-NLS-1$

	public static final String SUB_DOMAIN_PROP = "host"; //$NON-NLS-1$

	public static final String DOMAIN_PROP = "domain"; //$NON-NLS-1$

	public static final String SERVICES_PROP = "services"; //$NON-NLS-1$

	public static final String LABEL_PROP = "label"; //$NON-NLS-1$

	public static final String PROVIDER_PROP = "provider"; //$NON-NLS-1$

	public static final String VERSION_PROP = "version"; //$NON-NLS-1$

	public static final String PLAN_PROP = "plan"; //$NON-NLS-1$

	public static final String PATH_PROP = "path"; //$NON-NLS-1$

	public static final String BUILDPACK_PROP = "buildpack"; //$NON-NLS-1$

	public static final String ENV_PROP = "env"; //$NON-NLS-1$

	public static final String DISK_QUOTA_PROP = "disk_quota"; //$NON-NLS-1$

	public static final String STACK_PROP = "stack"; //$NON-NLS-1$
	
	public static final String TIMEOUT_PROP = "timeout"; //$NON-NLS-1$

	public static final String COMMAND_PROP = "command"; //$NON-NLS-1$
	
	public static final String INHERIT_PROP = "inherit"; //$NON-NLS-1$
}
