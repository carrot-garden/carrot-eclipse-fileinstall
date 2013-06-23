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
	 * Report project error state.
	 */
	public enum Change {
		/** Problems introduced. */
		NEGATIVE, //
		/** Problems state is the same. */
		NO_CHANGE, //
		/** Problems resolved. */
		POSITIVE, //
	}

	/**
	 * Last build health check status.
	 */
	private volatile boolean isPositive = false;

	/**
	 * Last build health problem severity.
	 */
	private volatile int pastSeverity = Integer.MIN_VALUE;

	/**
	 * Underlying resource project.
	 */
	private final IProject project;

	public Project(final IProject project) {
		this.project = project;
	}

	/**
	 * Update once, report change.
	 */
	public Change change() {
		final int nextSeverity = ProjectUtil.severity(project);
		if (nextSeverity == pastSeverity) {
			return Change.NO_CHANGE;
		} else {
			pastSeverity = nextSeverity;
			isPositive = (nextSeverity != IMarker.SEVERITY_ERROR);
			if (isPositive) {
				return Change.POSITIVE;
			} else {
				return Change.NEGATIVE;
			}
		}
	}

	/**
	 * Lazy value.
	 */
	private volatile Conf conf;

	/**
	 * Project plug-in configuration.
	 */
	public Conf conf() {
		if (conf == null) {
			conf = new Conf(ConfUtil.config(project));
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
		final Map<String, String> variables = //
		Conf.variables(ResourceUtil.workspacePath(), worker);
		final String path = ConfUtil.replace(//
				conf.fileinstallPath(), variables);
		final String tempalte = ConfUtil.replace(//
				conf.fileinstallTemplate(), variables);
		final File file = ProjectUtil.file(project, path);
		try {
			FileUtil.writeTextFile(file, tempalte);
		} catch (final Throwable e) {
			Plugin.logErr("Create failure.", e);
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
		ensureConf();
		final Conf conf = conf();
		final Map<String, String> variables = //
		Conf.variables(ResourceUtil.workspacePath(), worker);
		final String path = ConfUtil.replace(//
				conf.fileinstallPath(), variables);
		final File file = ProjectUtil.file(project, path);
		file.delete();
	}

	/**
	 * Ensure configuration is extracted for master project.
	 */
	public void ensureConf() {
		try {
			if (ProjectUtil.hasFile(project, Conf.PROJ_PATH)) {
				return;
			}
			ProjectUtil.copyFromClasspathIntoProject(Project.class,
					Conf.KLAZ_PATH, project, Conf.PROJ_PATH);
		} catch (final Throwable e) {
			Plugin.logErr("Ensure failure.", e);
		}
	}

	/**
	 * Verify last build result status.
	 */
	public boolean isPositive() {
		return isPositive;
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
