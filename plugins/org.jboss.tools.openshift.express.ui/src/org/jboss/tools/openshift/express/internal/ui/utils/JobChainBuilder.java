/******************************************************************************* 
 * Copyright (c) 2012 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.utils;

import org.eclipse.core.runtime.jobs.Job;


/**
 * @author Andre Dietisheim
 * @deprecated
 */
public class JobChainBuilder extends org.jboss.tools.openshift.express.internal.core.util.JobChainBuilder {
	public JobChainBuilder(Job job) {
		super(job);
	}
}
