/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.eclipse.fileinstall;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;

import com.carrotgarden.eclipse.fileinstall.util.ConfUtil;
import com.carrotgarden.eclipse.fileinstall.util.FileUtil;
import com.carrotgarden.eclipse.fileinstall.util.ProjectUtil;
import com.carrotgarden.eclipse.fileinstall.util.ResourceUtil;

/**
 * Project managed by the plug-in.
 */
public class Project {

	/**
	 * Report project health state.
	 */
	public enum Change {
		/** Problems present. */
		NEGATIVE, //
		/** Problems are resolved. */
		POSITIVE, //
		/** Problems state is the same. */
		UNCHANGED, //
	}

	/**
	 * State change detector.
	 */
	private abstract class ChangeDetector {

		/**
		 * Remember if past is positive.
		 */
		volatile boolean isPositive = false;

		/**
		 * Remember past state value.
		 */
		volatile Change past = Change.UNCHANGED;

		/**
		 * Update once, report change from the past.
		 */
		Change change() {
			final Change next = next();
			if (next == past) {
				return Change.UNCHANGED;
			} else {
				past = next;
				isPositive = (next == Change.POSITIVE);
				if (isPositive) {
					return Change.POSITIVE;
				} else {
					return Change.NEGATIVE;
				}
			}
		}

		/**
		 * Report current state; must be {@link Change#POSITIVE} or
		 * {@link Change#NEGATIVE}.
		 */
		abstract Change next();

	}

	/**
	 * Master project managed by plug-in.
	 */
	public static class Master extends Project {

		/**
		 * Lazy value.
		 */
		private volatile Conf conf;

		public Master(final IProject project) {
			super(project);
		}

		/**
		 * Project plug-in configuration.
		 */
		public Conf conf() {
			if (conf == null) {
				conf = new Conf(ConfUtil.config(project()));
			}
			return conf;
		}

		/**
		 * Create all live workers for a master.
		 */
		public void confCreate() {
			ensureConf();
			final Conf conf = conf();
			final List<String> list = conf.eclipseList();
			for (final String worker : list) {
				if (ResourceUtil.hasProjectOpen(worker)) {
					confCreate(worker);
				}
			}
		}

		/**
		 * Create worker-specific fileinstall.cfg file for the master.
		 */
		public void confCreate(final String worker) {
			ensureConf();
			final Conf conf = conf();

			if (!ResourceUtil.hasProject(worker)) {
				Plugin.logWarn("Project#confCreate: missing project: " + worker);
				return;
			}

			final Map<String, String> variables = //
			Conf.variables(ResourceUtil.workspacePath(), worker,
					ResourceUtil.projectPath(worker));

			final String path = ConfUtil.replace(//
					conf.fileinstallPath(), variables);

			final String tempalte = ConfUtil.replace(//
					conf.fileinstallTemplate(), variables);

			final File file = ProjectUtil.file(project(), path);

			try {
				FileUtil.writeTextFile(file, tempalte);
				Plugin.logInfo("Project#confCreate file: " + file);
			} catch (final Throwable e) {
				Plugin.logErrr("Project#confCreate failure", e);
			}
		}

		/**
		 * Delete all fileinstall.cfg file for the master.
		 */
		public void confDelete() {
			ensureConf();
			final Conf conf = conf();
			final List<String> list = conf.eclipseList();
			for (final String worker : list) {
				confDelete(worker);
			}
		}

		/**
		 * Delete worker-specific fileinstall.cfg file for the master.
		 */
		public void confDelete(final String worker) {

			if (!ResourceUtil.hasProject(worker)) {
				Plugin.logWarn("Project#confDelete: missing project: " + worker);
				return;
			}

			ensureConf();
			final Conf conf = conf();

			final Map<String, String> variables = //
			Conf.variables(ResourceUtil.workspacePath(), worker,
					ResourceUtil.projectPath(worker));

			final String path = ConfUtil.replace(//
					conf.fileinstallPath(), variables);

			final File file = ProjectUtil.file(project(), path);

			file.delete();

			Plugin.logInfo("Project#confDelete file: " + file);
		}

		/**
		 * Ensure configuration is extracted for master project.
		 */
		public void ensureConf() {
			try {
				if (ProjectUtil.hasFile(project(), Conf.PROJ_PATH)) {
					return;
				}
				ProjectUtil.copyFromClasspathIntoProject(Project.class,
						Conf.KLAZ_PATH, project(), Conf.PROJ_PATH);
			} catch (final Throwable e) {
				Plugin.logErrr("Project#ensureConf: failure", e);
			}
		}

	}

	/**
	 * Worker project is a dependency for a master project.
	 */
	public static class Worker extends Project {

		/**
		 * Manifest presence detector.
		 */
		private final ChangeDetector manifest = new ChangeDetector() {
			@Override
			Change next() {
				return ProjectUtil.manifest(project()).//
						exists() ? Change.POSITIVE : Change.NEGATIVE;
			}
		};

		/**
		 * Build health detector.
		 */
		private final ChangeDetector severity = new ChangeDetector() {
			@Override
			Change next() {
				return ProjectUtil.severity(project()) //
				!= IMarker.SEVERITY_ERROR ? Change.POSITIVE : Change.NEGATIVE;
			}
		};

		public Worker(final IProject project) {
			super(project);
		}

		/**
		 * Verify last build result status.
		 */
		public boolean isBuildSuccess() {
			return severity.isPositive;
		}

		/**
		 * Verify last build result status.
		 */
		public boolean isManifestPresent() {
			return manifest.isPositive;
		}

		/**
		 * Update once, report manifest presence.
		 */
		public Change manifestChange() {
			return manifest.change();
		}

		/**
		 * Update once, report severity change.
		 */
		public Change severityChange() {
			return severity.change();
		}

	}

	/**
	 * Underlying resource project.
	 */
	private final IProject project;

	protected Project(final IProject project) {
		this.project = project;
	}

	/**
	 * Project name.
	 */
	public String name() {
		return project.getName();
	}

	/**
	 * Resource project.
	 */
	public IProject project() {
		return project;
	}

	@Override
	public String toString() {
		return project.toString();
	}

}
