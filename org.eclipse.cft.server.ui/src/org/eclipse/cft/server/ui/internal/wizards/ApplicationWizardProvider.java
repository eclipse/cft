/*******************************************************************************
 * Copyright (c) 2013, 2016 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.ui.internal.wizards;

import java.util.List;

import org.eclipse.cft.server.core.AbstractApplicationDelegate;
import org.eclipse.cft.server.core.internal.application.FrameworkProvider;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;

/**
 * 
 * Wrapper around the provider that contributes wizard page for a particular
 * application type from the extension point:
 * 
 * <p/>
 * org.eclipse.cft.server.ui.applicationWizard
 * <p/>
 * 
 * The wrapper converts an {@link IApplicationWizardDelegate} as defined in the
 * extension point above into an {@link ApplicationWizardDelegate}, which
 * contains additional API used by the CF plug-in framework, as well as a
 * mapping to the corresponding {@link AbstractApplicationDelegate}.
 * 
 */
public class ApplicationWizardProvider {

	private final InternalApplicationWizardProvider internalProvider;

	private ApplicationWizardDelegate wizardDelegate;

	public ApplicationWizardProvider(IConfigurationElement configuration, String extensionPointID) {
		internalProvider = new InternalApplicationWizardProvider(configuration, extensionPointID);
	}

	public ApplicationWizardDelegate getDelegate(AbstractApplicationDelegate coreDelegate) {
		if (wizardDelegate == null) {
			IApplicationWizardDelegate actualDelegate = internalProvider.getDelegate();
			if (actualDelegate instanceof ApplicationWizardDelegate) {
				wizardDelegate = (ApplicationWizardDelegate) actualDelegate;
				wizardDelegate.setApplicationDelegate(coreDelegate);
			}
			else {
				wizardDelegate = new ApplicationWizardDelegateImp(actualDelegate, coreDelegate);
			}
		}
		return wizardDelegate;
	}

	public String getProviderID() {
		return internalProvider.getProviderID();
	}

	/**
	 * Actual wizard provider that gets loaded from the extension point.
	 * 
	 */
	static class InternalApplicationWizardProvider extends FrameworkProvider<IApplicationWizardDelegate> {
		public InternalApplicationWizardProvider(IConfigurationElement configuration, String extensionPointID) {
			super(configuration, extensionPointID);
		}
	}

	/**
	 * Maps an {@link IApplicationWizardDelegate} to an
	 * {@link AbstractApplicationDelegate}. The mapping is an internal framework
	 * relation, and therefore not part of the
	 * {@link IApplicationWizardDelegate} API.
	 * 
	 */
	static class ApplicationWizardDelegateImp extends ApplicationWizardDelegate {

		private final IApplicationWizardDelegate actualDelegate;

		public ApplicationWizardDelegateImp(IApplicationWizardDelegate actualDelegate, AbstractApplicationDelegate coreDelegate) {
			this.actualDelegate = actualDelegate;
			setApplicationDelegate(coreDelegate);
		}

		public List<IWizardPage> getWizardPages(ApplicationWizardDescriptor descriptor, IServer server,
				IModule module) {
			return actualDelegate.getWizardPages(descriptor, server, module);
		}

	}
}
