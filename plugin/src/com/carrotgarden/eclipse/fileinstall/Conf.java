/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.eclipse.fileinstall;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;

/**
 * Plug-in configuration settings.
 */
public class Conf {

	/** Configuration file delivered on class path. */
	public static final String KLAZ_FILE = "eclipse-fileinstall.conf";
	/** Configuration file delivered on class path. */
	public static final String KLAZ_PATH = "/resources/" + KLAZ_FILE;

	/** Configuration file extracted on project path. */
	public static final String PROJ_FILE = "eclipse-fileinstall.conf";
	/** Configuration file extracted on project path. */
	public static final String PROJ_PATH = "/" + PROJ_FILE;

	/** Configuration variable substitution. */
	public static final String VAR_WORKSPACE = "@{eclipse-workspace}";
	/** Configuration variable substitution. */
	public static final String VAR_PROJECT = "@{eclipse-project}";

	/**
	 * Render as annotated properties file.
	 */
	final static ConfigRenderOptions options = ConfigRenderOptions.defaults()
			.setComments(true) //
			.setOriginComments(true) //
			.setFormatted(true) //
			.setJson(false);

	final Config config;

	public Conf(final Config config) {
		this.config = config;
	}

	/** # verify project is compiled w/o errors */
	public boolean eclipseCheckIsBuildSuccess() {
		return config.getBoolean("eclipse.check.is-build-success");
	}

	/** # verify target deploy folder is present */
	public boolean eclipseCheckIsDeployPresent() {
		return config.getBoolean("eclipse.check.is-deploy-present");
	}

	/** # verify target project launch config is running */
	public boolean eclipseCheckIsLaunchRunningPresent() {
		return config.getBoolean("eclipse.check.is-launch-running");
	}

	/** # verify META-INF/MANIFEST.MF is present in target/classes */
	public boolean eclipseCheckIsManifestPresent() {
		return config.getBoolean("eclipse.check.is-manifest-present");
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
		/** Render as flat properties. */
		final Properties props = new Properties();
		final Config conf = config.getConfig("fileinstall.template");
		for (final Entry<String, ConfigValue> entry : conf.entrySet()) {
			props.put(entry.getKey(), entry.getValue().unwrapped().toString());
		}
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			props.store(output, "Auto generated, do not change.");
		} catch (final Throwable e) {
			Plugin.logErr("Store failure.", e);
		}
		return output.toString();
	}

	/**
	 * Build variable substitution.
	 */
	public static Map<String, String> variables(final String workspace,
			final String worker) {
		final Map<String, String> map = new HashMap<String, String>();
		map.put(VAR_PROJECT, worker);
		map.put(VAR_WORKSPACE, workspace);
		return map;
	}

}
