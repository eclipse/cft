/*******************************************************************************
 * Copyright (c) 2016, 2017 Pivotal Software, Inc. and others
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

import org.eclipse.cft.server.core.internal.CloudFoundryPlugin;
import org.eclipse.cft.server.core.internal.CloudFoundryServer;
import org.eclipse.cft.server.core.internal.OperationScheduler;
import org.eclipse.cft.server.core.internal.client.CFOperation;
import org.eclipse.cft.server.ui.internal.Messages;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class FormMessageHandler {

	private final int MAX_MESSAGE_DISPLAY_SIZE = 100;

	private CloudFoundryServer cloudServer;

	private ScrolledForm form;

	public FormMessageHandler(CloudFoundryServer cloudServer, ScrolledForm form) {
		this.cloudServer = cloudServer;
		this.form = form;
	}

	protected IStatus getStatusFromRunningOperation() {
		if (cloudServer != null) {
			OperationScheduler scheduler = cloudServer.getBehaviour().getOperationScheduler();
			CFOperation op = scheduler != null ? scheduler.getCurrentOperation() : null;

			if (op != null && op.getOperationName() != null) {
				return CloudFoundryPlugin.getStatus(op.getOperationName(), IStatus.INFO);
			}
		}

		return null;
	}

	/**
	 * Primary API to set messages in the editor. Callers should use this over
	 * {@link #setErrorMessage(String)} as it performs additional checks that
	 * are not done by the WTP framework.
	 * <p/>
	 * MUST be called from UI thread.
	 * @param status to display in the editor. If null, it will be interpreted
	 * as "OK" status, and any existing message will be cleared.
	 */
	public void setMessage(IStatus status) {

		// Running ops have priority when displaying messages to the editor.
		IStatus opStatus = getStatusFromRunningOperation();

		if (opStatus != null) {
			status = opStatus;
		}

		String message = null;

		// Don't show "OK" status
		message = status != null && !Status.OK_STATUS.getMessage().equals(status.getMessage()) ? status.getMessage()
				: null;
		int messageProviderType = EditorMessageTypes.getMessageProviderType(status);

		if (message == null) {
			setMessageInForm(null, messageProviderType);
			return;
		}
		else {
			// First replace all return carriages, or new lines with spaces
			StringBuffer buffer = new StringBuffer(message);
			for (int i = 0; i < buffer.length(); i++) {
				char ch = buffer.charAt(i);
				if (ch == '\r' || ch == '\n') {
					buffer.replace(i, i + 1, " "); //$NON-NLS-1$
				}
			}

			String fullMessage = buffer.toString();
			if (buffer.length() > MAX_MESSAGE_DISPLAY_SIZE) {
				String endingSegment = Messages.CloudFoundryApplicationsEditorPage_TEXT_SEE_ERRORLOG;
				message = buffer.substring(0, MAX_MESSAGE_DISPLAY_SIZE).trim() + endingSegment;
			}
			else {
				message = fullMessage;
			}
			
			setMessageInForm(message, messageProviderType);
		}
	}

	protected void setMessageInForm(String message, int messageProviderType) {
		if (form != null && !form.isDisposed()) {
			form.setMessage(message, messageProviderType);
		}
	}

}
