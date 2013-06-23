/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.eclipse.fileinstall;

import java.io.PrintWriter;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.carrotgarden.eclipse.fileinstall.util.ConsoleUtil;
import com.carrotgarden.eclipse.fileinstall.util.ConsoleUtil.Console;

/**
 * Plug-in for eclipse + fileinstall integration.
 */
public class Plugin extends AbstractUIPlugin implements IStartup {

	/**
	 * Plug-in singleton instance.
	 */
	private static volatile Plugin PLUGIN;

	/**
	 * Permanent plugin GUID.
	 */
	public static final String PLUGIN_ID = "com.carrotgarden.eclipse.fileinstall.plugin";

	/**
	 * Plug-in singleton instance.
	 */
	public static Plugin instance() {
		return PLUGIN;
	}

	/**
	 * Log eclipse status.
	 */
	public static void log(final IStatus status) {
		final Plugin instance = instance();
		if (instance == null) {
			System.err.println(status);
		} else {
			instance.getLog().log(status);
		}
	}

	/**
	 * Log eclipse error.
	 */
	public static void logErrr(final String message, final Throwable exception) {
		final IStatus status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0,
				message, exception);
		log(status);
	}

	/**
	 * Log eclipse information.
	 */
	public static void logInfo(final String message) {
		final IStatus status = new Status(IStatus.INFO, Plugin.PLUGIN_ID, 0,
				message, null);
		log(status);
	}

	/**
	 * Log eclipse debugging.
	 */
	public static void logOK(final String message) {
		final IStatus status = new Status(IStatus.OK, Plugin.PLUGIN_ID, 0,
				message, null);
		log(status);
	}

	/**
	 * Log eclipse warning.
	 */
	public static void logWarn(final String message) {
		final IStatus status = new Status(IStatus.WARNING, Plugin.PLUGIN_ID, 0,
				message, null);
		log(status);
	}

	private volatile BundleContext context;

	/**
	 * Plug-in console.
	 */
	private final Console logConsole = ConsoleUtil.console("fileinstall");

	/**
	 * Plug-in console listener.
	 */
	private final ILogListener logListener = new ILogListener() {
		@Override
		public void logging(final IStatus status, final String plugin) {
			logWrier.println(ConsoleUtil.render(status));
			logWrier.flush();
		}
	};

	/**
	 * Plug-in console writer.
	 */
	private final PrintWriter logWrier = new PrintWriter(logConsole.out);

	/**
	 * Plug-in business logic.
	 */
	private final Manager manager = new Manager();

	/**
	 * Required public constructor.
	 */
	public Plugin() {
	}

	/**
	 * OSGI context.
	 */
	public BundleContext context() {
		return context;
	}

	@Override
	public void earlyStartup() {
		/** Plug-in starts after eclipse UI. */
	}

	/**
	 * Plug-in business logic.
	 */
	public Manager manager() {
		return manager;
	}

	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		this.context = context;
		PLUGIN = this;
		PLUGIN.getLog().addLogListener(logListener);
		logInfo("plugin start");
		manager.start();
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		manager.stop();
		logInfo("plugin stop");
		PLUGIN.getLog().removeLogListener(logListener);
		super.stop(context);
		PLUGIN = null;
		this.context = null;
	}

}
