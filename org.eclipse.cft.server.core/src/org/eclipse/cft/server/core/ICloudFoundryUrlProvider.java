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
 * Enforces the structure that a Url Provider must follow
 * so it can be used to provide dynamic Cloud Urls through
 * branding extension.
 * 
 * <b>: IMPORTANT: It is highly recommended for classes that 
 * implement this interface to use a cache mechanism when calculating
 * the urls that will be returned, specially of those obtained
 * dynamically (as for example, from a web service), since the
 * retrieval methods could be invoked multiple times and no cache
 * is implemented at the base level. </b>
 */
public interface ICloudFoundryUrlProvider {
	/**
	 * Provides the default Url for the current
	 * Cloud Foundry server type.
	 * 
	 * This default Url will take precedence over any value
	 * provided through the static plugin.xml.
	 * 
	 * If for some reason, two default Urls are provided for
	 * the same contribution (one dynamically through an 
	 * {@link ICloudFoundryUrlProvider}, then another one
	 * through a static contribution in plugin.xml, the 
	 * static one will be added to the list of non-default
	 * Urls when processing this contribution. 
	 *  
	 * @return an AbstractCloudFoundryUrl or null if no default
	 * Url is given by this provider.
	 */
	public abstract AbstractCloudFoundryUrl getDefaultUrl ();
	
	/**
	 * Provides a list of additional Urls for the current
	 * Cloud Foundry server type.
	 * 
	 * @return a List of (non default) AbstractCloudFoundryUrl
	 */
	public abstract List <AbstractCloudFoundryUrl> getNonDefaultUrls ();
}
