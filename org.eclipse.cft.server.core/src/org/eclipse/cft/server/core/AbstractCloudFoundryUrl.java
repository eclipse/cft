/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.core;

import java.util.List;

/**
 * Extend this class to represent a dynamic CloudFoundry Url, useful to provide
 * branding extensions to the CF framework.
 */
public abstract class AbstractCloudFoundryUrl {
	/**
	 * CloudFoundry Url Name.
	 */
	protected String name;

	/**
	 * CloudFoundry server Url.
	 */
	protected String url;

	/**
	 * CloudFoundry server Sign up Url 
	 */
	protected String signUpUrl;
	
	/**
	 * Constructor that requests the two mandatory parameters for a 
	 * Cloud server Url and the optional ones (list of wildcards and sign up url)
	 * @param name Cloud server Url identifier (name) 
	 * @param url Url to connect to this Cloud Server
	 * @param wildcards list of placeholders that are supposed to be provided by the final user. This is
	 * an optional parameter, so if this Url is not going to be customized by the user (i.e. it is
	 * the same Url for every user), then provide this parameter as null.
	 * Consider that the wildcard list is used to alter the initial Url, so when you provide
	 * a Url like <b>http://sample.com/samplewildcard</b> and the list of wildcards contains a single
	 * element <b>samplewilcard</b>, the resulting Url will have the following form: <b>http://sample.com/{wildcard}</b>
	 * so when you invoke {@link #getUrl()}, you will get <b>http://sample.com/{wildcard}</b> as result.
	 * If you provide this parameter as <b>null</b>, the original Url will remain unaltered.
	 * @param signUpUrl a Url that can be used to sign up for a new account in this CloudFoundry server.
	 * This is an optional parameter, so if no public Url is defined to enable new user sign-up
	 * you should pass this parameter as null.
	 */
	public AbstractCloudFoundryUrl (String name, String url, List <Wildcard> wildcards, String signUpUrl) {
		this.name = name;
		this.url = url;
		this.signUpUrl = signUpUrl;
		if (wildcards != null) {
			for (Wildcard wildcard : wildcards) {
				String wildcardName = wildcard.getName();
				if (wildcardName != null && !wildcardName.isEmpty()) {
					this.url = this.url.replaceAll(wildcardName, "{" + wildcardName + "}"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}	
		}
	}
	
	/**
	 * Provides the Url display name that will be shown to the
	 * user in the UI.
	 * 
	 * This is a required attribute (i.e. should not return null)
	 * 
	 * @return a String representing the Url name.
	 */
	public String getName () {
		return name;
	}
	
	/**
	 * Provides the actual Url that the user will need
	 * to connect to the cloud server.
	 * 
	 * This is a required attribute (i.e. should not return null)
	 * 
	 * @return a valid CloudFoundry server Url.
	 */
	public String getUrl () {
		return url;
	}
	
	/**
	 * Provides the sign up Url for the CloudFoundry server, where
	 * a customer can navigate and register for an account on this 
	 * server.
	 * 
	 * This is an optional attribute, hence it is valid that signUpUrl
	 * is null, but if the current server has a valid sign up
	 * Url, return that value here.
	 * 
	 * @return A valid sign up Url where the user will pointed out to
	 * create a new account for this CloudFoundry server. Default is null
	 * (no sign up page), but if there is one, this method must provide it. 
	 */
	public String getSignUpUrl () {
		return signUpUrl;
	}
	
	/**
	 * Returns a flag indicating whether this is a user defined url or not
	 * 
	 * @return true if this is a Url defined by the user, false otherwise.
	 * Default is false.
	 */
	public boolean getUserDefined() {
		return false;
	}

	/**
	 * Returns a flag indicating whether this server uses self-signed certificate
	 * 
	 * @return true if this server uses self-signed certificates, false otherwise.
	 * Default is false.
	 */
	public boolean getSelfSigned() {
		return false;
	}
	
	/**
	 * Wildcard represent a "variable" (placeholder) that is going to be 
	 * replaced by user input (through UI)
	 */
	public static class Wildcard {
		/**
		 * The wildcard name (only required attribute)
		 */
		private String name;
		
		/**
		 * Constructs a Wildcard given its name
		 * 
		 * @param name The Wildcard name
		 */
		public Wildcard (String name) {
			this.name = name;
		}
		
		/**
		 * Returns this Wildcard name
		 * 
		 * @return the Wildcard name
		 */
		public String getName () {
			return name;
		}
	}
}
