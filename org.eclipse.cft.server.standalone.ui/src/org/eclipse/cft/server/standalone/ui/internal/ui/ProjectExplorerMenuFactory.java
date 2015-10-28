/*******************************************************************************
 * Copyright (c) 2012, 2014 Pivotal Software, Inc. 
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
package org.eclipse.cft.server.standalone.ui.internal.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cft.server.standalone.core.internal.application.StandaloneFacetHandler;
import org.eclipse.cft.server.standalone.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.actions.AbstractMenuContributionFactory;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.menus.IMenuService;

/**
 * Contributes context menu actions to the Project explorer for Java standalone
 * projects.
 * 
 */
public class ProjectExplorerMenuFactory extends AbstractMenuContributionFactory {

	public ProjectExplorerMenuFactory() {
		super("popup:org.eclipse.ui.projectConfigure?endof=additions", null); //$NON-NLS-1$
	}

	@Override
	protected List<IAction> getActions(IMenuService menuService) {
		Object context = menuService.getCurrentState();
		IProject project = getProjectFromContext(context);
		List<IAction> actions = new ArrayList<IAction>();
		if (project != null) {
			StandaloneFacetHandler handler = new StandaloneFacetHandler(project);
			if (handler.canAddFacet()) {
				actions.add(new ConvertToStandaloneAction(project));
			} else if (handler.hasFacet()) {
				actions.add(new RemoveStandaloneAction(project));
			}

		}
		return actions;
	}

	protected IProject getProjectFromContext(Object context) {
		IProject project = null;
		if (context instanceof IEvaluationContext) {

			Object evalContext = ((IEvaluationContext) context)
					.getDefaultVariable();

			if (evalContext instanceof List<?>) {
				List<?> content = (List<?>) evalContext;
				if (!content.isEmpty()) {
					evalContext = content.get(0);
				}
			}

			if (evalContext instanceof IProject) {
				project = (IProject) evalContext;
			} else if (evalContext instanceof IAdaptable) {
				project = (IProject) ((IAdaptable) evalContext)
						.getAdapter(IProject.class);
			}
		}
		return project;
	}

	public class ConvertToStandaloneAction extends Action {

		protected final IProject project;

		public ConvertToStandaloneAction(IProject project) {
			this.project = project;
			setActionValues();
		}

		protected void setActionValues() {
			setText(Messages.ProjectExplorerMenuFactory_LABEL_CONVERT_TEXT);
			setToolTipText(Messages.ProjectExplorerMenuFactory_LABEL_CONVERT_TOOLTIP);
			setEnabled(true);
		}

		public void run() {
			Job job = new Job(Messages.ProjectExplorerMenuFactory_JOB_ENABLE) {

				protected IStatus run(IProgressMonitor monitor) {

					new StandaloneFacetHandler(project).addFacet(monitor);
					return Status.OK_STATUS;
				}
			};

			job.setSystem(false);
			job.schedule();
		}
	}

	public class RemoveStandaloneAction extends Action {

		protected final IProject project;

		public RemoveStandaloneAction(IProject project) {
			this.project = project;
			setActionValues();
		}

		protected void setActionValues() {
			setText(Messages.ProjectExplorerMenuFactory_LABEL_REMOVE_TEXT);
			setToolTipText(Messages.ProjectExplorerMenuFactory_LABEL_REMOVE_TOOLTIP);
			setEnabled(true);
		}

		public void run() {
			Job job = new Job(Messages.ProjectExplorerMenuFactory_JOB_DISABLE) {

				protected IStatus run(IProgressMonitor monitor) {

					new StandaloneFacetHandler(project).removeFacet();
					return Status.OK_STATUS;
				}
			};

			job.setSystem(false);
			job.schedule();
		}
	}

}
