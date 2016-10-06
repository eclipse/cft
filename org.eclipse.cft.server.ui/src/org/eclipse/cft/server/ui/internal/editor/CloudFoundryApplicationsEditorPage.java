/*******************************************************************************
 * Copyright (c) 2012, 2016 Pivotal Software, Inc. and others
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
package org.eclipse.cft.server.ui.internal.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.cft.server.core.CFServiceInstance;
import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.CloudServerEvent;
import org.eclipse.cft.server.core.internal.CloudServerListener;
import org.eclipse.cft.server.core.internal.ServerEventHandler;
import org.eclipse.cft.server.core.internal.client.ServicesUpdatedEvent;
import org.eclipse.cft.server.ui.internal.CloudFoundryImages;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.cft.server.ui.internal.actions.EditorAction.EditorCloudEvent;
import org.eclipse.cft.server.ui.internal.actions.EditorAction.RefreshArea;
import org.eclipse.cft.server.ui.internal.actions.RefreshEditorAction;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.ui.editor.ServerEditorPart;

/**
 * @author Terry Denney
 * @author Leo Dos Santos
 * @author Steffen Pingel
 * @author Christian Dupuis
 */
public class CloudFoundryApplicationsEditorPage extends ServerEditorPart {

	private CloudFoundryServer cloudServer;

	private ApplicationMasterDetailsBlock masterDetailsBlock;

	private ManagedForm mform;

	private ServerListener serverListener;

	private final List<CloudServerListener> cloudServerListeners = new ArrayList<CloudServerListener>();

	private List<CFServiceInstance> services;

	private ScrolledForm sform;

	private int[] applicationMemoryChoices;

	private UIJob refreshJob;

	private UpdateEditorOperation currentRefreshOp;

	private FormMessageHandler formMessageHandler;

	@Override
	public void createPartControl(Composite parent) {

		mform = new ManagedForm(parent);
		FormToolkit toolkit = getFormToolkit(parent.getDisplay());

		sform = mform.getForm();
		sform.getForm().setText(Messages.COMMONTXT_APPLICATIONS);
		toolkit.decorateFormHeading(sform.getForm());

		cloudServer = (CloudFoundryServer) getServer().getOriginal().loadAdapter(CloudFoundryServer.class, null);

		masterDetailsBlock = new ApplicationMasterDetailsBlock(this, cloudServer);
		masterDetailsBlock.createContent(mform);

		sform.getForm().setImage(CloudFoundryImages.getImage(CloudFoundryImages.OBJ_APPLICATION));
		refresh(RefreshArea.MASTER);

		serverListener = new ServerListener();
		addCloudServerListener(serverListener);
		getServer().getOriginal().addServerListener(serverListener);
	}

	/**
	 * The message handler for the underlying form in this page. May be null if
	 * it hasn't yet been initialised (for example, the form is not yet created)
	 */
	public FormMessageHandler getFormMessageHandler() {
		if (this.formMessageHandler == null && cloudServer != null && sform != null) {
			this.formMessageHandler = new FormMessageHandler(cloudServer, sform);
		}
		return this.formMessageHandler;
	}

	/**
	 * 
	 * @param listener to be notified of Cloud server and application changes.
	 * The editor manages the lifecycle of the listener once it is added,
	 * including removing it when the editor is disposed.
	 */
	public void addCloudServerListener(CloudServerListener listener) {
		if (listener != null && !cloudServerListeners.contains(listener)) {
			ServerEventHandler.getDefault().addServerListener(listener);
			cloudServerListeners.add(listener);
		}
	}

	@Override
	public void dispose() {
		for (CloudServerListener listener : cloudServerListeners) {
			ServerEventHandler.getDefault().removeServerListener(listener);
		}

		getServer().getOriginal().removeServerListener(serverListener);

		if (mform != null) {
			mform.dispose();
			mform = null;
		}
		super.dispose();
	};

	public CloudFoundryServer getCloudServer() {
		return cloudServer;
	}

	public ApplicationMasterDetailsBlock getMasterDetailsBlock() {
		return masterDetailsBlock;
	}

	public List<CFServiceInstance> getServices() {
		return services;
	}

	public int[] getApplicationMemoryChoices() {
		return applicationMemoryChoices;
	}

	public boolean isDisposed() {
		return sform.isDisposed();
	}

	public void reflow() {
		mform.getForm().reflow(true);
	}

	public void refresh(RefreshArea area) {
		RefreshEditorAction.getRefreshAction(this, area).run();
	}

	public void selectAndReveal(IModule module) {
		// Refresh the UI immediately with the cached information for the
		// module. Refresh
		masterDetailsBlock.refreshUI(RefreshArea.MASTER);

		TableViewer viewer = masterDetailsBlock.getMasterPart().getApplicationsViewer();
		viewer.setSelection(new StructuredSelection(module));

		// Launch a fresh operation that will update the module. As this is
		// longer
		// running, it will eventually refresh the UI via events
		refresh(RefreshArea.DETAIL);
	}

	@Override
	public void setFocus() {
	}

	public void setServices(List<CFServiceInstance> services) {
		this.services = services;
	}

	public void setApplicationMemoryChoices(int[] applicationMemoryChoices) {
		this.applicationMemoryChoices = applicationMemoryChoices;
	}

	private synchronized void setRefreshOp(UpdateEditorOperation op) {
		this.currentRefreshOp = op;
	}

	private synchronized UpdateEditorOperation getRefreshOp() {
		return this.currentRefreshOp;
	}

	private class ServerListener implements CloudServerListener, IServerListener {
		public void serverChanged(final CloudServerEvent event) {

			if (event.getServer() == null) {
				CloudFoundryPlugin.logError(
						"Internal error: unable to refresh editor. No Cloud server specified in the server event."); // $NON-NLS-1$
				return;
			}
			// Do not refresh if not from the same server
			if (!cloudServer.getServer().getId().equals(event.getServer().getServer().getId())) {
				return;
			}

			// Don't trigger editor refresh on instances update to avoid
			// multiple
			// refreshes as it is performed
			// as part of an application update operation which triggers
			// a separate module refresh event
			if (event.getType() != CloudServerEvent.EVENT_INSTANCES_UPDATED) {
				RefreshArea area = event instanceof EditorCloudEvent ? ((EditorCloudEvent) event).getRefreshArea()
						: RefreshArea.ALL;
				launchRefresh(new UpdateEditorOperation(event, area));
			}
		}

		public void serverChanged(ServerEvent event) {
			// refresh when server is saved, e.g. due to add/remove of modules
			if (event.getKind() == ServerEvent.SERVER_CHANGE) {
				launchRefresh(new UpdateEditorOperation(CloudServerEvent.EVENT_UPDATE_COMPLETED, RefreshArea.ALL,
						event.getStatus()));
			}
		}
	}

	protected void launchRefresh(UpdateEditorOperation refreshOp) {

		setRefreshOp(refreshOp);

		// Only schedule one job per editor page session, in case multiple
		// refresh requests are received, only the one that is currently
		// scheduled should execute
		if (refreshJob == null) {
			refreshJob = new UIJob(Messages.CloudFoundryApplicationsEditorPage_JOB_REFRESH) {

				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {

					UpdateEditorOperation op = getRefreshOp();
					if (op != null) {
						op.run(monitor);
					}
					return Status.OK_STATUS;
				}
			};
		}

		refreshJob.schedule();
	}

	/**
	 * Refresh operation that should only be run in UI thread.
	 *
	 */
	private class UpdateEditorOperation {

		private CloudServerEvent event;

		private final RefreshArea area;

		private final int type;

		private final IStatus status;

		public UpdateEditorOperation(CloudServerEvent event, RefreshArea area) {
			this.event = event;
			this.area = area;
			this.type = event.getType();
			this.status = event.getStatus() != null ? event.getStatus() : Status.OK_STATUS;
		}

		public UpdateEditorOperation(int eventType, RefreshArea area, IStatus status) {
			this.area = area;
			this.type = eventType;
			this.status = status != null ? status : Status.OK_STATUS;
		}

		public void run(IProgressMonitor monitor) {

			if (isDisposed() || mform == null || mform.getForm() == null || mform.getForm().isDisposed()
					|| masterDetailsBlock.getMasterPart().getManagedForm().getForm().isDisposed()) {
				return;
			}

			if (event instanceof ServicesUpdatedEvent && this.type == CloudServerEvent.EVENT_SERVICES_UPDATED
					&& status.getSeverity() != IStatus.ERROR) {
				List<CFServiceInstance> services = ((ServicesUpdatedEvent) event).getServices();
				if (services == null) {
					services = Collections.emptyList();
				}
				setServices(services);
			}

			// Refresh the UI
			masterDetailsBlock.refreshUI(area);
		}
	}

	/**
	 * Primary API to set messages in the editor that are visible to the user.
	 * Callers should use this method instead of
	 * {@link ServerEditorPart#setErrorMessage(String)} as it performs
	 * additional checks on the message.
	 * 
	 * @param status can be null if there are no messages to set. If so, or
	 * status is OK, any current messages in the editor will be cleared.
	 */
	public void setMessage(IStatus status) {
		FormMessageHandler messageHandler = getFormMessageHandler();
		if (messageHandler != null) {
			messageHandler.setMessage(status);
		}
	}

}
