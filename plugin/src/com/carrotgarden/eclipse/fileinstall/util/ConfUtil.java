/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.eclipse.fileinstall.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.core.resources.IProject;

import com.carrotgarden.eclipse.fileinstall.Conf;
import com.carrotgarden.eclipse.fileinstall.Plugin;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;

/**
 * Typesafe config helper.
 */
public class ConfUtil {

	/**
	 * Total check counter.
	 */
	public static class CheckCount implements Conf.Check {

		public volatile int eclipseCheckIsMasterDeployPresent;
		public volatile int eclipseCheckIsMasterLaunchRunning;
		public volatile int eclipseCheckIsWorkerBuildSuccess;
		public volatile int eclipseCheckIsWorkerManifestPresent;

		@Override
		public boolean eclipseCheckIsMasterDeployPresent() {
			return eclipseCheckIsMasterDeployPresent > 0;
		}

		@Override
		public boolean eclipseCheckIsMasterLaunchRunning() {
			return eclipseCheckIsMasterLaunchRunning > 0;
		}

		@Override
		public boolean eclipseCheckIsWorkerBuildSuccess() {
			return eclipseCheckIsWorkerBuildSuccess > 0;
		}

		@Override
		public boolean eclipseCheckIsWorkerManifestPresent() {
			return eclipseCheckIsWorkerManifestPresent > 0;
		}

	}

	/**
	 * Render as annotated properties file.
	 */
	public final static ConfigRenderOptions options = ConfigRenderOptions
			.defaults().setComments(true) //
			.setOriginComments(true) //
			.setFormatted(true) //
			.setJson(false);

	/**
	 * Unresolved master configuration with plug-in fall-back.
	 */
	public static Config config(final IProject master) {
		return configProj(master).withFallback(configPlug());
	}

	public static Config configPlug() {
		return ConfigFactory.parseResources(ConfUtil.class.getClassLoader(),
				Conf.KLAZ_PATH);
	}

	public static Config configProj(final IProject master) {
		final File file = ProjectUtil.file(master, Conf.PROJ_PATH);
		return ConfigFactory.parseFile(file);
	}

	/**
	 * Render config as flat key=value properties text.
	 */
	public static String flatFile(final Config config) {

		final Properties props = new Properties();

		for (final Entry<String, ConfigValue> entry : config.entrySet()) {
			props.put(entry.getKey(), entry.getValue().unwrapped().toString());
		}

		final ByteArrayOutputStream output = new ByteArrayOutputStream();

		try {
			props.store(output, "Auto generated, do not change.");
		} catch (final Throwable e) {
			Plugin.logErrr("ConfUtil#flatFile: failure", e);
		}

		return output.toString();

	}

	public static String replace(String text, final Map<String, String> map) {
		for (final Map.Entry<String, String> entry : map.entrySet()) {
			text = text.replace(entry.getKey(), entry.getValue());
		}
		return text;
	}

}
