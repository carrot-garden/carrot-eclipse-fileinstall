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

import com.carrotgarden.eclipse.fileinstall.Conf.Check;
import com.carrotgarden.eclipse.fileinstall.Project.Change;
import com.carrotgarden.eclipse.fileinstall.util.ConfUtil;
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
	 * Mater projects: projects with plug-in nature.
	 */
	private final ConcurrentMap<String, Project.Master> //
	masterMap = new ConcurrentHashMap<String, Project.Master>();

	/**
	 * Project change listener.
	 */
	private final IResourceChangeListener projectListener = new IResourceChangeListener() {
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
	private final IResourceDeltaVisitor projectVisitor = new IResourceDeltaVisitor() {

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
	private final ConcurrentMap<String, Project.Worker> //
	workerMap = new ConcurrentHashMap<String, Project.Worker>();

	/**
	 * Handle worker build start.
	 * 
	 * @see Builder
	 */
	public void builderAboutToBuild(final IJavaProject java) {
		final String name = java.getProject().getName();
		final Project.Worker worker = workerMap.get(name);
		if (worker == null) {
			return;
		}
		doBuildInitiate(worker);
	}

	/**
	 * Handle worker build finish.
	 * 
	 * @see Builder
	 */
	public void builderBuildFinished(final IJavaProject java) {
		final String name = java.getProject().getName();
		final Project.Worker worker = workerMap.get(name);
		if (worker == null) {
			return;
		}
		doBuildTerminate(worker);
	}

	/**
	 * Handle worker clean start.
	 * 
	 * @see Builder
	 */
	public void builderCleanStarting(final IJavaProject java) {
		final String name = java.getProject().getName();
		final Project.Worker worker = workerMap.get(name);
		if (worker == null) {
			return;
		}
	}

	/**
	 * Collect check poll from masters.
	 */
	private Conf.Check check() {

		final ConfUtil.CheckCount total = new ConfUtil.CheckCount();

		for (final Project.Master master : masterMap.values()) {
			final Check check = master.conf().check();
			if (check.eclipseCheckIsMasterDeployPresent()) {
				total.eclipseCheckIsMasterDeployPresent++;
			}
			if (check.eclipseCheckIsMasterLaunchRunning()) {
				total.eclipseCheckIsMasterLaunchRunning++;
			}
			if (check.eclipseCheckIsWorkerBuildSuccess()) {
				total.eclipseCheckIsWorkerBuildSuccess++;
			}
			if (check.eclipseCheckIsWorkerManifestPresent()) {
				total.eclipseCheckIsWorkerManifestPresent++;
			}
		}

		return total;
	}

	/**
	 * Run worker pre-build.
	 */
	private void doBuildInitiate(final Project.Worker worker) {

		final String message = "Manager#doBuildInitiate: worker: " + worker;
		Plugin.logOK(message);

		workerDeactivate(worker.name());

	}

	/**
	 * Run worker post-build.
	 */
	private void doBuildTerminate(final Project.Worker worker) {

		final String message = "Manager#doBuildTerminate: worker: " + worker;
		Plugin.logInfo(message);

		final String name = worker.name();

		worker.manifestChange();
		worker.severityChange();

		final Check check = check();

		int countPositive = 0;
		int countNegative = 0;

		if (check.eclipseCheckIsWorkerBuildSuccess()) {
			if (worker.isBuildSuccess()) {
				countPositive++;
				Plugin.logOK("Manager#doBuildTerminate: build success: "
						+ worker);
			} else {
				Plugin.logOK("Manager#doBuildTerminate: build failure: "
						+ worker);
				countNegative++;
			}
		}

		if (check.eclipseCheckIsWorkerManifestPresent()) {
			if (worker.isManifestPresent()) {
				countPositive++;
				Plugin.logOK("Manager#doBuildTerminate: manifest present: "
						+ worker);
			} else {
				Plugin.logOK("Manager#doBuildTerminate: manifest missing: "
						+ worker);
				countNegative++;
			}
		}

		if (countPositive == 0 && countNegative == 0) {
			Plugin.logOK("Manager#doBuildTerminate: change ignored: " + worker);
			workerActivate(name);
			return;
		}

		if (countNegative > 0) {
			Plugin.logOK("Manager#doBuildTerminate: change negative: " + worker);
			workerDeactivate(name);
			return;
		}

		if (countPositive > 0) {
			Plugin.logOK("Manager#doBuildTerminate: change positive: " + worker);
			workerActivate(name);
			return;
		}

	}

	/**
	 * Handle master/worker activate.
	 */
	public void handleCreate(final IProject project) {
		JobUtil.schedule(new EclipseRunnable("Manager handle activate.") {
			@Override
			public void doit(final IProgressMonitor monitor)
					throws CoreException {

				final String message = "Manager#handleCreate " + project;

				monitor.beginTask(message, 2);
				Plugin.logInfo(message);

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
		JobUtil.schedule(new EclipseRunnable("Manager handle deactivate.") {
			@Override
			public void doit(final IProgressMonitor monitor)
					throws CoreException {

				final String message = "Manager#handleDelete " + project;

				monitor.beginTask(message, 2);
				Plugin.logInfo(message);

				masterDelete(project);
				monitor.worked(1);

				workerDelete(project);
				monitor.worked(1);

				monitor.done();
			}
		});
	}

	public boolean hasMaster(final IProject project) {
		return masterMap.containsKey(project.getName());
	}

	public boolean hasWorker(final IJavaProject java) {
		return workerMap.containsKey(java.getProject().getName());
	}

	/**
	 * Verify if worker is required by known master.
	 */
	private boolean isWorkerRequired(final String workerName) {
		for (final Project.Master master : masterMap.values()) {
			final List<String> eclipseList = master.conf().eclipseList();
			if (eclipseList.contains(workerName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Create master project.
	 */
	private void masterCreate(final IProject project) {
		final String name = project.getName();
		if (NatureUtil.hasPluginNature(project)) {

			Project.Master master = masterMap.get(name);
			if (master == null) {
				masterMap.putIfAbsent(name, new Project.Master(project));
				master = masterMap.get(name);
				Plugin.logInfo("Manager#masterCreate: new: " + master);
			} else {
				Plugin.logInfo("Manager#masterCreate: old: " + master);
				return;
			}

			/** Create master fileinstall.cfg. */
			master.confCreate();

			/** Update present worker projects. */
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
	private void masterDelete(final IProject project) {
		final String name = project.getName();
		final Project.Master master = masterMap.remove(name);
		if (master != null) {
			master.confDelete();
			Plugin.logInfo("Manager#masterDelete: " + master);
		}
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
			if (NatureUtil.hasPluginNature(project)) {
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
			if (NatureUtil.hasPluginNature(project)) {
				handleDelete(project);
			}
		}

		masterMap.clear();
		workerMap.clear();

	}

	/**
	 * Add worker to matching masters, report if any match.
	 */
	private boolean workerActivate(final String workerName) {
		int count = 0;
		for (final Project.Master master : masterMap.values()) {
			final List<String> eclipseList = master.conf().eclipseList();
			if (eclipseList.contains(workerName)) {
				master.confCreate(workerName);
				count++;
			}
		}
		Plugin.logOK("Manager#workerActivate " + count + " / " + workerName);
		return count > 0;
	}

	/**
	 * Create worker project.
	 */
	private void workerCreate(final IProject project) {
		final String name = project.getName();
		if (NatureUtil.hasJavaNature(project) && isWorkerRequired(name)) {

			Project.Worker worker = workerMap.get(name);
			if (worker == null) {
				workerMap.putIfAbsent(name, new Project.Worker(project));
				worker = workerMap.get(name);
				Plugin.logInfo("Manager#workerCreate: new: " + worker);
			} else {
				Plugin.logInfo("Manager#workerCreate: old: " + worker);
			}

			doBuildTerminate(worker);

		}
	}

	/**
	 * Remove worker from matching masters, report if any match.
	 */
	private boolean workerDeactivate(final String workerName) {
		int count = 0;
		for (final Project.Master master : masterMap.values()) {
			final List<String> eclipseList = master.conf().eclipseList();
			if (eclipseList.contains(workerName)) {
				master.confDelete(workerName);
				count++;
			}
		}
		Plugin.logOK("Manager#workerDeactivate " + count + " / " + workerName);
		return count > 0;
	}

	/**
	 * Delete worker project.
	 */
	private void workerDelete(final IProject project) {
		final String name = project.getName();
		final Project worker = workerMap.remove(name);
		if (worker != null) {
			workerDeactivate(name);
			Plugin.logInfo("Manager#workerDelete " + worker);
		}
	}

	/**
	 * Process worker update.
	 */
	@SuppressWarnings("unused")
	private void workerUpdate(final Project.Worker worker) {

		final String name = worker.name();

		final Change manifest = worker.manifestChange();
		final Change severity = worker.severityChange();

		if (manifest == Change.UNCHANGED && severity == Change.UNCHANGED) {
			Plugin.logOK("Manager#workerUpdate: unchanged: " + worker);
			return;
		}

		final Check check = check();

		int countPositive = 0;
		int countNegative = 0;

		if (check.eclipseCheckIsWorkerBuildSuccess()) {
			switch (severity) {
			case POSITIVE:
				countPositive++;
				Plugin.logOK("Manager#workerUpdate: build success: " + worker);
				break;
			case NEGATIVE:
				Plugin.logOK("Manager#workerUpdate: build failure: " + worker);
				countNegative++;
				break;
			}
		}

		if (check.eclipseCheckIsWorkerManifestPresent()) {
			switch (manifest) {
			case POSITIVE:
				Plugin.logOK("Manager#workerUpdate: manifest present: "
						+ worker);
				countPositive++;
				break;
			case NEGATIVE:
				Plugin.logOK("Manager#workerUpdate: manifest missing: "
						+ worker);
				countNegative++;
				break;
			}
		}

		if (countPositive == 0 && countNegative == 0) {
			Plugin.logOK("Manager#workerUpdate: change ignored: " + worker);
			return;
		}

		if (countNegative > 0) {
			Plugin.logOK("Manager#workerUpdate: change negative: " + worker);
			workerDeactivate(name);
			return;
		}

		if (countPositive > 0) {
			Plugin.logOK("Manager#workerUpdate: change positive: " + worker);
			workerActivate(name);
			return;
		}

	}

}
