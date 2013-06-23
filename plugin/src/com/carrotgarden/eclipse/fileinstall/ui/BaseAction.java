/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.eclipse.fileinstall.ui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.carrotgarden.eclipse.fileinstall.Nature;
import com.carrotgarden.eclipse.fileinstall.Plugin;
import com.carrotgarden.eclipse.fileinstall.util.NatureUtil;

/**
 * Eclipse UI action template.
 */
public abstract class BaseAction implements IObjectActionDelegate {

	/**
	 * Projects selected by user in UI.
	 */
	private final LinkedHashSet<IProject> projectSet = new LinkedHashSet<IProject>();

	/**
	 * For implementations of operations that really don't expect more than one
	 * project this method conveniently returns the first selected project. If
	 * the enablement condition on the menu are set correctly, this should in
	 * fact be the only project.
	 */
	protected IJavaProject getJavaProject() {
		final IProject project = getProject();
		if (project != null) {
			return JavaCore.create(project);
		}
		return null;
	}

	/**
	 * For implementations of operations that really don't expect more than one
	 * project this method conveniently returns the first selected project. If
	 * the enablement condition on the menu are set correctly, this should in
	 * fact be the only project.
	 */
	protected IProject getProject() {
		switch (projectSet.size()) {
		case 1:
			for (final IProject project : projectSet) {
				return project;
			}
		default:
			return null;
		}
	}

	protected List<IProject> getProjectSet() {
		return new ArrayList<IProject>(projectSet);
	}

	protected boolean isNatureEnabled() {
		if (projectSet.isEmpty()) {
			return false;
		}
		for (final IProject project : getProjectSet()) {
			if (!NatureUtil.hasNature(project, Nature.NATURE_ID)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 */
	protected void runUI(final IRunnableWithProgress runnable) {

		final IWorkbenchWindow context = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();

		final ISchedulingRule scheduling = ResourcesPlugin.getWorkspace()
				.getRuleFactory().buildRule();
		try {
			PlatformUI.getWorkbench().getProgressService()
					.runInUI(context, runnable, scheduling);
		} catch (final Exception e) {
			e.printStackTrace();
			// GradleUI.log(e);
			// final String msg = ExceptionUtil.getMessage(e);
			// if (msg != null) {
			// MessageDialog.openError(null, runnable.toString() + " Failed",
			// msg);
			// }
		}
	}

	@Override
	public void selectionChanged(final IAction action,
			final ISelection selection) {

		// Plugin.logInfo("selection = " + selection);

		projectSet.clear();

		if (selection.isEmpty()) {
			return;
		}

		if (selection instanceof IStructuredSelection) {
			final IStructuredSelection structured = (IStructuredSelection) selection;
			for (final Object element : structured.toList()) {
				if (element instanceof IProject) {
					projectSet.add((IProject) element);
				} else if (element instanceof IResource) {
					projectSet.add(((IResource) element).getProject());
				}
			}
			return;
		}

		Plugin.logWarn("Wrong selection=" + selection);

	}

	/**
	 * For implementations of operations that really don't expect more than one
	 * project this method conveniently returns the first selected project. If
	 * the enablement condition on the menu are set correctly, this should in
	 * fact be the only project.
	 */
	// protected GradleProject getGradleProject() {
	// final IProject project = getProject();
	// if (project != null) {
	// return GradleCore.create(project);
	// }
	// return null;
	// }

	@Override
	public void setActivePart(final IAction action,
			final IWorkbenchPart targetPart) {
	}

}
