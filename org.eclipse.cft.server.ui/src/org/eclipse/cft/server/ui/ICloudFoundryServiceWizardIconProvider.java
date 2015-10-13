/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others
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

package org.eclipse.cft.server.ui;

import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Concrete implementations of this class should be thread safe, allowing
 * multiple concurrent calls to its member methods. Returned images will be
 * cached for the lifetime of the wizard dialog, and re-acquired for subsequent
 * wizard invocations.
 * 
 * Ownership of any returned ImageDescriptor are transferred to the calling
 * service wizard. The service wizard will handle image lifecycle/disposal.
 * 
 * If the width or height of the Image/ImageDescriptor smaller than or exceeds 
 * 32 pixels, the image will be resized as close to 32 as possible while 
 * preserving the aspect ratio.
 * 
 */
public interface ICloudFoundryServiceWizardIconProvider {

	/**
	 * This interface method can be implemented by consumers to provide icons
	 * for the services of the add services wizard.
	 * 
	 * Image retrieval and loading is the responsibility of the service wizard;
	 * An image descriptor can contain an HTTP/HTTPS URL and the service wizard
	 * will handle retrieval and display.
	 * 
	 * @param offering The specific service offering for which an icon is being
	 * requested
	 * @param server The specific server for which the request is being made
	 * @return An image descriptor which can be used to create an image for used
	 * by the service wizard.
	 */
	public ImageDescriptor getServiceIcon(CloudServiceOffering offering, CloudFoundryServer server);

	/**
	 * If an error occurs while calling createImage(...) on an ImageDescriptor
	 * returned by the getServiceIcon(...) method above, then this
	 * getDefaultServiceIcon(...) method will be invoked to return a backup
	 * image. For example, if the URL contained in a returned ImageDescriptor is
	 * invalid, this method will be called to supply a default/basic/generic
	 * icon instead, for the service offering.
	 * 
	 * In this scenario, consumers may wish to return a generic icon from a
	 * plugin, the local filesystem, or an alternate URL, otherwise the icon
	 * will appear blank (empty space) in the icon wizard.
	 * 
	 * @param offering The specific service offering for which an icon is being requested
	 * @param server The specific server for which the request is being made
	 * @return An image descriptor which can be used to create an image for used by the service wizard.
	 * 
	 */
	public ImageDescriptor getDefaultServiceIcon(CloudServiceOffering offering, CloudFoundryServer server);

}
