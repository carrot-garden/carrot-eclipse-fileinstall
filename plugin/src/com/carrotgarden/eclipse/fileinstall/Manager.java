/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.eclipse.fileinstall;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.carrotgarden.eclipse.fileinstall.util.EclipseRunnable;
import com.carrotgarden.eclipse.fileinstall.util.EventUtil;
import com.carrotgarden.eclipse.fileinstall.util.JobUtil;
import com.carrotgarden.eclipse.fileinstall.util.NatureUtil;
import com.carrotgarden.eclipse.fileinstall.util.ResourceUtil;

/**
 * Plug-in business logic.
 */
public class Manager {

	/**
	 * Worker project update mode.
	 */
	enum Mode {
		ENABLE, //
		DISABLE, //
	}

	/**
	 * Mater projects: projects with plug-in nature.
	 */
	final ConcurrentMap<String, Project> masterMap = new ConcurrentHashMap<String, Project>();

	/**
	 * Project change listener.
	 */
	final IResourceChangeListener projectListener = new IResourceChangeListener() {
		@Override
		public void resourceChanged(final IResourceChangeEvent event) {

			/** Handle project close/delete. */
			if (EventUtil.hasType(event, IResourceChangeEvent.PRE_CLOSE
					| IResourceChangeEvent.PRE_DELETE)) {
				final IResource resource = event.getResource();
				if (resource instanceof IProject) {
					handleDelete((IProject) resource);
				}
				return;
			}

			/** Handle project create/open. */
			if (EventUtil.hasType(event, IResourceChangeEvent.POST_CHANGE)) {
				EventUtil.accept(event, projectVisitor);
				return;
			}

		}
	};

	/**
	 * Project post-change visitor.
	 */
	final IResourceDeltaVisitor projectVisitor = new IResourceDeltaVisitor() {

		@Override
		public boolean visit(final IResourceDelta delta) throws CoreException {

			final IResource resource = delta.getResource();

			/** Looking for existing. */
			if (EventUtil.hasKind(delta, IResourceDelta.CHANGED
					| IResourceDelta.ADDED | IResourceDelta.REMOVED)) {

				/** Visit workspace. */
				if (resource instanceof IWorkspaceRoot) {
					return true;
				}

				/** Visit project. */
				if (resource instanceof IProject) {
					final IProject project = (IProject) resource;
					/** Looking for open. */
					if (EventUtil.hasFlag(delta, IResourceDelta.OPEN)) {
						if (project.isOpen()) {
							handleCreate(project);
						}
					}
					return false;
				}

			}

			/** Visit other. */
			return false;

		}

	};

	/**
	 * Worker projects: dependency for master projects.
	 */
	final ConcurrentMap<String, Project> workerMap = new ConcurrentHashMap<String, Project>();

	/**
	 * Handle worker build result.
	 * 
	 * @see Builder
	 */
	public void buildFinished(final IJavaProject java) {
		final String name = java.getProject().getName();
		final Project worker = workerMap.get(name);
		if (worker == null) {
			return;
		}
		switch (worker.change()) {
		case POSITIVE:
			handleUpdate(worker.project(), Mode.ENABLE);
			break;
		case NEGATIVE:
			handleUpdate(worker.project(), Mode.DISABLE);
			break;
		}
	}

	public boolean containsWorker(final IJavaProject java) {
		return workerMap.containsKey(java.getProject().getName());
	}

	public boolean contanisMaster(final IProject project) {
		return masterMap.containsKey(project.getName());
	}

	/**
	 * Handle master/worker activate.
	 */
	public void handleCreate(final IProject project) {
		JobUtil.schedule(new EclipseRunnable("Handle activate.") {
			@Override
			public void doit(final IProgressMonitor monitor)
					throws CoreException {

				monitor.beginTask("Activate " + project, 2);
				Plugin.logInfo("activate = " + project);

				masterCreate(project);
				monitor.worked(1);

				workerCreate(project);
				monitor.worked(1);

				monitor.done();
			}

		});
	}

	/**
	 * Handle master/worker deactivate.
	 */
	public void handleDelete(final IProject project) {
		JobUtil.schedule(new EclipseRunnable("Handle deactivate.") {
			@Override
			public void doit(final IProgressMonitor monitor)
					throws CoreException {

				monitor.beginTask("Deactivate " + project, 2);
				Plugin.logInfo("deactivate = " + project);

				masterDelete(project);
				monitor.worked(1);

				workerDelete(project);
				monitor.worked(1);

				monitor.done();
			}
		});
	}

	/**
	 * Handle worker update.
	 */
	void handleUpdate(final IProject project, final Mode mode) {
		JobUtil.schedule(new EclipseRunnable("Handle update.") {
			@Override
			public void doit(final IProgressMonitor monitor)
					throws CoreException {

				monitor.beginTask("Update " + project, 1);
				Plugin.logInfo("update = " + mode + "/" + project);

				final String name = project.getName();

				switch (mode) {
				case ENABLE:
					workerActivate(name);
					break;
				case DISABLE:
					workerDeactivate(name);
					break;
				}

				monitor.done();
			}

		});
	}

	/**
	 * Create master project.
	 */
	void masterCreate(final IProject project) {
		final String name = project.getName();
		if (hasPluginNature(project)) {

			Project master = masterMap.get(name);
			if (master == null) {
				masterMap.putIfAbsent(name, new Project(project));
				master = masterMap.get(name);
				Plugin.logInfo("master create = " + master);
			} else {
				Plugin.logInfo("master exists = " + master);
				return;
			}

			master.confCreate();

			for (final String workerName : master.conf().eclipseList()) {
				final IProject worker = ResourceUtil.project(workerName);
				if (worker.exists()) {
					workerCreate(worker);
				}
			}
		}
	}

	/**
	 * Delete master project.
	 */
	void masterDelete(final IProject project) {
		final String name = project.getName();
		final Project master = masterMap.remove(name);
		if (master != null) {
			master.confDelete();
			Plugin.logInfo("master delete = " + master);
		}
	}

	/**
	 * Verify project has java nature.
	 */
	boolean hasJavaNature(final IProject project) {
		return NatureUtil.hasNature(project, JavaCore.NATURE_ID);
	}

	/**
	 * Verify project has plug-in nature.
	 */
	boolean hasPluginNature(final IProject project) {
		return NatureUtil.hasNature(project, Nature.NATURE_ID);
	}

	/**
	 * Initialize project manager.
	 */
	public void start() {

		masterMap.clear();
		workerMap.clear();

		final IWorkspace workspace = ResourcesPlugin.getWorkspace();

		workspace.addResourceChangeListener(projectListener);

		/**
		 * Scan existing projects.
		 */
		final IProject[] projectList = workspace.getRoot().getProjects();
		for (final IProject project : projectList) {
			if (hasPluginNature(project)) {
				handleCreate(project);
			}
		}

	}

	/**
	 * Terminate project manager.
	 */
	public void stop() {

		final IWorkspace workspace = ResourcesPlugin.getWorkspace();

		workspace.removeResourceChangeListener(projectListener);

		/**
		 * Scan existing projects.
		 */
		final IProject[] projectList = workspace.getRoot().getProjects();
		for (final IProject project : projectList) {
			if (hasPluginNature(project)) {
				handleDelete(project);
			}
		}

		masterMap.clear();
		workerMap.clear();

	}

	/**
	 * Add worker to matching masters, report if any match.
	 */
	boolean workerActivate(final String workerName) {
		int count = 0;
		for (final Project master : masterMap.values()) {
			final List<String> eclipseList = master.conf().eclipseList();
			if (eclipseList.contains(workerName)) {
				master.confCreate(workerName);
				count++;
			}
		}
		Plugin.logInfo("worker activate = " + count + " /" + workerName);
		return count > 0;
	}

	/**
	 * Create worker project.
	 */
	void workerCreate(final IProject project) {
		final String name = project.getName();
		if (hasJavaNature(project) && isWorkerRequired(name)) {

			Project worker = workerMap.get(name);
			if (worker == null) {
				workerMap.putIfAbsent(name, new Project(project));
				worker = workerMap.get(name);
				Plugin.logInfo("worker create = " + worker);
			} else {
				Plugin.logInfo("worker exists = " + worker);
			}

			worker.change();

			if (worker.isPositive()) {
				workerActivate(name);
			} else {
				workerDeactivate(name);
			}

		}
	}

	/**
	 * Remove worker from matching masters, report if any match.
	 */
	boolean workerDeactivate(final String workerName) {
		int count = 0;
		for (final Project master : masterMap.values()) {
			final List<String> eclipseList = master.conf().eclipseList();
			if (eclipseList.contains(workerName)) {
				master.confDelete(workerName);
				count++;
			}
		}
		Plugin.logInfo("worker deactivate = " + count + " /" + workerName);
		return count > 0;
	}

	/**
	 * Delete worker project.
	 */
	void workerDelete(final IProject project) {
		final String name = project.getName();
		final Project worker = workerMap.remove(name);
		if (worker != null) {
			workerDeactivate(name);
			Plugin.logInfo("worker delete = " + worker);
		}
	}

	/**
	 * Verify if worker is required by known master.
	 */
	boolean isWorkerRequired(final String workerName) {
		for (final Project master : masterMap.values()) {
			final List<String> eclipseList = master.conf().eclipseList();
			if (eclipseList.contains(workerName)) {
				return true;
			}
		}
		return false;
	}

}
