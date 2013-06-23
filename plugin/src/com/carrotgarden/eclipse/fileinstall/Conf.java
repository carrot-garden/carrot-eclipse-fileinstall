/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.eclipse.fileinstall;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.carrotgarden.eclipse.fileinstall.util.ConfUtil;
import com.typesafe.config.Config;

/**
 * Plug-in configuration settings.
 */
public class Conf {

	/**
	 * Worker project validity checks.
	 */
	public interface Check {

		/** # verify target deploy folder is present */
		public boolean eclipseCheckIsMasterDeployPresent();

		/** # verify target project launch config is running */
		public boolean eclipseCheckIsMasterLaunchRunning();

		/** # verify project is compiled w/o errors */
		public boolean eclipseCheckIsWorkerBuildSuccess();

		/** # verify META-INF/MANIFEST.MF is present in target/classes */
		public boolean eclipseCheckIsWorkerManifestPresent();

	}

	private class CheckImpl implements Check {

		@Override
		public boolean eclipseCheckIsMasterDeployPresent() {
			return config.getBoolean("eclipse.check.is-master-deploy-present");
		}

		@Override
		public boolean eclipseCheckIsMasterLaunchRunning() {
			return config.getBoolean("eclipse.check.is-master-launch-running");
		}

		@Override
		public boolean eclipseCheckIsWorkerBuildSuccess() {
			return config.getBoolean("eclipse.check.is-worker-build-success");
		}

		@Override
		public boolean eclipseCheckIsWorkerManifestPresent() {
			return config
					.getBoolean("eclipse.check.is-worker-manifest-present");
		}

	}

	/** Configuration file delivered on class path. */
	public static final String KLAZ_FILE = "eclipse-fileinstall.conf";
	/** Configuration file delivered on class path. */
	public static final String KLAZ_PATH = "/resources/" + KLAZ_FILE;

	/** Configuration file extracted on project path. */
	public static final String PROJ_FILE = "eclipse-fileinstall.conf";
	/** Configuration file extracted on project path. */
	public static final String PROJ_PATH = "/" + PROJ_FILE;

	/** Configuration variable substitution. */
	public static final String VAR_PROJECT_NAME = "@{eclipse-project-name}";
	/** Configuration variable substitution. */
	public static final String VAR_PROJECT_PATH = "@{eclipse-project-path}";
	/** Configuration variable substitution. */
	public static final String VAR_WORKSPACE = "@{eclipse-workspace}";

	/**
	 * Build variable substitution.
	 */
	public static Map<String, String> variables(final String workspace,
			final String name, final String path) {
		final Map<String, String> map = new HashMap<String, String>();
		map.put(VAR_WORKSPACE, workspace);
		map.put(VAR_PROJECT_NAME, name);
		map.put(VAR_PROJECT_PATH, path);
		return map;
	}

	private final Check check = new CheckImpl();

	private final Config config;

	public Conf(final Config config) {
		this.config = config;
	}

	public Check check() {
		return check;
	}

	/** # list of monitored dependency projects */
	public List<String> eclipseList() {
		return config.getStringList("eclipse.list");
	}

	/** # configuration file name pattern */
	public String fileinstallFile() {
		return config.getString("fileinstall.file");
	}

	/** # configuration deploy folder */
	public String fileinstallFolder() {
		return config.getString("fileinstall.folder");
	}

	public String fileinstallPath() {
		return fileinstallFolder() + "/" + fileinstallFile();
	}

	/** # prototype configuration */
	public String fileinstallTemplate() {
		return ConfUtil.flatFile(config.getConfig("fileinstall.template"));
	}

}
