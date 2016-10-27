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
package org.eclipse.cft.server.core.internal.client;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.spaces.CloudFoundrySpace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Manages client creation to Cloud Foundry. Two types of clients are created:
 * <p/>
 * 1. Clients linked to an existing {@link CloudFoundryServer} instance:
 * {@link CloudServerCFClient}
 * <p/>
 * 2. "Standalone" clients that are not linked to {@link CloudFoundryServer}
 * instances, and can be used as "throw-away" clients, for example, to validate
 * credentials or fetch list of org and spaces: {@link CFClient}
 * 
 * <p/>
 * IMPORTANT: This is an INTERNAL experimental CFT framework type. It may
 * undergo changes in the future as CFT transitions from v1 to v2 CF Java
 * client,and may break anyone that registers a client manager if any changes
 * occur to the API
 *
 */
public interface CFClientManager {

	public boolean supports(String serverUrl);

	/**
	 * Creates a non-null {@link CloudServerCFClient} which is associated with
	 * the given cloud server instance.
	 * @param cloudServer
	 * @param credentials
	 * @param cloudFoundrySpace
	 * @return non-null client
	 * @throws CoreException if client failed to be created
	 */
	public CloudServerCFClient createCloudServerClient(CloudFoundryServer cloudServer, CloudCredentials credentials,
			CloudFoundrySpace cloudFoundrySpace, IProgressMonitor monitor) throws CoreException;

	/**
	 * Creates a non-null standalone client that is not associated with any
	 * cloud server
	 * @param url
	 * @param userName
	 * @param password
	 * @param selfSigned
	 * @return non-null client
	 * @throws CoreException if client failed to be created
	 */
	public CFClient createClient(String url, String userName, String password, boolean selfSigned,
			IProgressMonitor monitor) throws CoreException;

	/**
	 * Creates a non-null standalone client to the specified Cloud space that is
	 * not associated with any cloud server
	 * 
	 * @param url
	 * @param cloudCredentials
	 * @param cloudSpace
	 * @param selfSigned
	 * @return non-null client
	 * @throws CoreException if client failed to be created
	 */
	public CFClient createClient(String url, CloudCredentials cloudCredentials, CloudFoundrySpace cloudSpace,
			boolean selfSigned, IProgressMonitor monitor) throws CoreException;

	/**
	 * Creates a non-null standalone client that is not associated with any
	 * cloud server.
	 * @param url
	 * @param cloudCredentials
	 * @param selfSigned
	 * @param monitor
	 * @return non-null client
	 * @throws CoreException if client failed to be created
	 */
	public CFClient createClient(String url, CloudCredentials cloudCredentials, boolean selfSigned,
			IProgressMonitor monitor) throws CoreException;

	/**
	 * Validates the given credentials by establishing a connection to Cloud
	 * Foundry. Throws exception if validation failed or connection not
	 * established.
	 * @param url
	 * @param userName
	 * @param password
	 * @param selfSigned
	 * @param monitor
	 * @throws CoreException if validation failed or connection not established
	 */
	public void validate(String url, String userName, String password, boolean selfSigned, IProgressMonitor monitor)
			throws CoreException;

	/**
	 * Create client for the existing cloud server but with updated passcode.
	 * @param cloudServer
	 * @param passcode
	 * @param monitor
	 * @return
	 * @throws CoreException
	 */
	public CloudServerCFClient createSsoClientWithUpdatedPasscode(CloudFoundryServer cloudServer, String passcode,
			IProgressMonitor monitor) throws CoreException;

	/**
	 * 
	 * @param url
	 * @param cfServer
	 * @param selfSigned
	 * @param passcode
	 * @param tokenValue
	 * @param monitor
	 * @throws CoreException 
	 */
	public void validateSso(String url, CloudFoundryServer cfServer, boolean selfSigned, String passcode,
			String tokenValue, IProgressMonitor monitor) throws CoreException;

}