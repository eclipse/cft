/*******************************************************************************
 * Copyright (c) 2012, 2015 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.standalone.core.internal.application;

import java.util.Set;

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryProjectUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.model.ModuleDelegate;
import org.eclipse.wst.server.core.util.ProjectModuleFactoryDelegate;
import org.osgi.framework.Bundle;

/**
 * Required factory to support Java applications in the Eclipse WST-based Cloud
 * Foundry server, including Spring boot applications.
 * 
 */
public class StandAloneModuleFactory extends ProjectModuleFactoryDelegate {

	@Override
	public ModuleDelegate getModuleDelegate(IModule module) {
		return new JavaLauncherModuleDelegate(module.getProject());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.wst.server.core.util.ProjectModuleFactoryDelegate#createModules
	 * (org.eclipse.core.resources.IProject)
	 */
	public IModule[] createModules(IProject project) {
		if (canCreateModule(project)) {
			IModule module = createModule(project.getName(), project.getName(),
					StandaloneFacetHandler.ID_MODULE_STANDALONE,
					StandaloneFacetHandler.ID_JAVA_STANDALONE_APP_VERSION,
					project);
			if (module != null) {
				return new IModule[] { module };
			}
		}
		// Return null if a module was not created. Returning an empty module
		// list will cache the empty list in WTP
		// preventing a new module from being created if the project state
		// changes in the future
		return null;
	}

	protected boolean canCreateModule(IProject project) {
		// Check if it is a Java project that isn't already supported by another
		// framework (Spring, etc..), as those
		// modules are created separately.
		return canHandle(project);
	}

	public static boolean canHandle(IProject project) {
		if (!CloudFoundryProjectUtil.hasNature(project, JavaCore.NATURE_ID)) {
			return false;
		}

		StandaloneFacetHandler handler = new StandaloneFacetHandler(project);

		// If it is Spring boot, and it doesn't have the facet, add it to avoid
		// having users manually add the facet. Auto-configuration of Spring
		// Boot is only
		// enabled if Spring IDE is installed and also if project has no other
		// facets as to avoid
		// corrupting the project and making it undeployable in other WST server
		// types that do not
		// support the Cloud Foundry facet.
		if (isSpringIDEInstalled() && hasNoFacets(project)
				&& CloudFoundryProjectUtil.isSpringBoot(project)) {

			handler.addFacet(new NullProgressMonitor());
		}

		return handler.hasFacet();
	}

	/**
	 * @return true if and only if it is possible to determine that the project
	 *         has no facets. False otherwise
	 */
	private static boolean hasNoFacets(IProject project) {
		// As adding facets can corrupt projects that are deployable to other
		// WST server instances, if facets
		// cannot be resolved, assume the project has facets.
		boolean hasNoFacets = false;
		try {
			IFacetedProject facetedProject = ProjectFacetsManager
					.create(project);
			if (facetedProject != null) {
				Set<IProjectFacetVersion> facets = facetedProject
						.getProjectFacets();
				hasNoFacets = facets == null || facets.isEmpty();
			} else {
				hasNoFacets = true;
			}
		} catch (CoreException e) {
			CloudFoundryPlugin.logError(e);
		}
		return hasNoFacets;
	}

	private static boolean isSpringIDEInstalled() {
		Bundle bundle = null;
		try {
			bundle = Platform.getBundle("org.springframework.ide.eclipse.core"); //$NON-NLS-1$
		} catch (Throwable e) {
			// Ignore. if it can't be resolved, assume not installed to avoid
			// errors in non-STS environments
		}

		return bundle != null;
	}
}
