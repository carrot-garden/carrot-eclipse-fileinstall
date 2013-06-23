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
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;

import com.carrotgarden.eclipse.fileinstall.Nature;
import com.carrotgarden.eclipse.fileinstall.Plugin;
import com.carrotgarden.eclipse.fileinstall.util.EclipseRunnable;
import com.carrotgarden.eclipse.fileinstall.util.JobUtil;
import com.carrotgarden.eclipse.fileinstall.util.NatureUtil;

/**
 * @see plugin.xml
 */
public class NatureActivateAction extends BaseAction {

	public NatureActivateAction() {
	}

	@Override
	public void run(final IAction action) {

		final IProject project = getProject();

		JobUtil.schedule(new EclipseRunnable("Nature activate.") {
			@Override
			public void doit(final IProgressMonitor monitor)
					throws CoreException {

				monitor.beginTask("Nature activate: " + project, 2);

				try {

					// 1. nature
					NatureUtil.ensure(project, monitor, Nature.NATURE_ID,
							JavaCore.NATURE_ID);
					monitor.worked(1);

					Thread.sleep(500);

					// 2. config
					Plugin.instance().manager().handleCreate(project);
					monitor.worked(1);

				} catch (final Throwable e) {

					Plugin.logErr("Nature activate failure.", e);

				} finally {
					monitor.done();
				}

			}
		});

	}
}
