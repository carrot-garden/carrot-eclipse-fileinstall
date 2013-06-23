/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.eclipse.fileinstall.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;

import com.carrotgarden.eclipse.fileinstall.Plugin;
import com.carrotgarden.eclipse.fileinstall.util.EclipseRunnable;
import com.carrotgarden.eclipse.fileinstall.util.JobUtil;

/**
 * @see plugin.xml
 */
public class ProjectUpdateAction extends BaseAction {

	public ProjectUpdateAction() {
	}

	@Override
	public void run(final IAction action) {

		final IProject project = getProject();

		JobUtil.schedule(new EclipseRunnable("Project update.") {
			@Override
			public void doit(final IProgressMonitor monitor)
					throws CoreException {

				monitor.beginTask("Project update " + project, 2);

				try {

					// 1. disable
					Plugin.instance().manager().handleDelete(project);
					monitor.worked(1);

					Thread.sleep(500);

					// 2. enable
					Plugin.instance().manager().handleCreate(project);
					monitor.worked(1);

				} catch (final Throwable e) {

					Plugin.logErr("Project update failure.", e);

				} finally {
					monitor.done();
				}

			}
		});

	}
}
