/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.eclipse.fileinstall.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;

import com.carrotgarden.eclipse.fileinstall.Conf;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Typesafe config helper.
 */
public class ConfUtil {

	/**
	 * Unresolved master configuration with plugin fall-back.
	 */
	public static Config config(final IProject master) {
		return configProj(master).withFallback(configPlug());
	}

	public static Config config(final IProject master, final IProject worker)
			throws Exception {

		final String eclipseWorkspace = master.getWorkspace().getRoot()
				.getLocation().toFile().getAbsolutePath();
		final String eclipseProject = worker.getName();

		final Map<String, String> substitution = new HashMap<String, String>();
		substitution.put(Conf.VAR_WORKSPACE, eclipseWorkspace);
		substitution.put(Conf.VAR_PROJECT, eclipseProject);

		return config(master, substitution);
	}

	public static Config config(final IProject master,
			final Map<String, String> substitution) throws Exception {
		final String configProject = configProject(master, substitution);
		final String configReference = configPlugin(substitution);
		return ConfigFactory.parseString(configProject).withFallback(
				ConfigFactory.parseString(configReference));
	}

	public static Config configProj(final IProject master) {
		final File file = ProjectUtil.file(master, Conf.PROJ_PATH);
		return ConfigFactory.parseFile(file);
	}

	public static String configProject(final IProject master) throws Exception {
		final File file = ProjectUtil.file(master, Conf.PROJ_PATH);
		final String text = FileUtil.readTextFile(file);
		return text;
	}

	public static String configProject(final IProject master,
			final Map<String, String> substitution) throws Exception {
		return replace(configProject(master), substitution);
	}

	public static Config configPlug() {
		return ConfigFactory.parseResources(ConfUtil.class.getClassLoader(),
				Conf.KLAZ_PATH);
	}

	public static String configPlugin() throws Exception {
		final String text = FileUtil.readTextResource(ProjectUtil.class,
				Conf.KLAZ_PATH);
		return text;
	}

	public static String configPlugin(final Map<String, String> substitution)
			throws Exception {
		return replace(configPlugin(), substitution);
	}

	public static String replace(String text, final Map<String, String> map) {
		for (final Map.Entry<String, String> entry : map.entrySet()) {
			text = text.replace(entry.getKey(), entry.getValue());
		}
		return text;
	}

}
