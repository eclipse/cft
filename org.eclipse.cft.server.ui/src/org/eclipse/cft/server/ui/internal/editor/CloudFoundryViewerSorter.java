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

import org.cloudfoundry.client.lib.domain.InstanceStats;
import org.eclipse.cft.server.core.internal.client.CFServiceInstance;
import org.eclipse.cft.server.ui.internal.editor.AppStatsContentProvider.InstanceStatsAndInfo;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.wst.server.core.IModule;


/**
 * @author Terry Denney
 * @author Christian Dupuis
 */
public class CloudFoundryViewerSorter extends ViewerSorter {

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		if (e1 instanceof InstanceStatsAndInfo && e1 instanceof InstanceStatsAndInfo) {
			InstanceStats stats1 = ((InstanceStatsAndInfo) e1).getStats();
			InstanceStats stats2 = ((InstanceStatsAndInfo) e2).getStats();
			return stats1.getId().compareTo(stats2.getId());
		}
		if (e1 instanceof CFServiceInstance && e2 instanceof CFServiceInstance) {
			CFServiceInstance service1 = (CFServiceInstance) e1;
			CFServiceInstance service2 = (CFServiceInstance) e2;
			return service1.getName().compareTo(service2.getName());
		}
		if (e1 instanceof IModule && e2 instanceof IModule) {
			IModule m1 = (IModule) e1;
			IModule m2 = (IModule) e2;
			return m1.getName().compareTo(m2.getName());
		}
		return super.compare(viewer, e1, e2);
	}

}
