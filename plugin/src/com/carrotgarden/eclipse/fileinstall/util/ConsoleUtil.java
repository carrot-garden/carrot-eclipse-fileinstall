/*******************************************************************************
 * Copyright (c) 2012 VMWare, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * VMWare, Inc. - initial API and implementation
 *******************************************************************************/
package com.carrotgarden.eclipse.fileinstall.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class ConsoleUtil {

	public static class Console {

		public final InputStream in;
		public final OutputStream out;
		public final OutputStream err;

		public Console(final IOConsoleInputStream in,
				final IOConsoleOutputStream out, final IOConsoleOutputStream err) {
			this.in = in;
			this.out = out;
			this.err = err;
		}

		public void close() {
			try {
				out.close();
			} catch (final IOException e) {
			}
			try {
				err.close();
			} catch (final IOException e) {
			}
			try {
				in.close();
			} catch (final IOException e) {
			}
		}
	}

	/**
	 * Maximum number of old consoles to keep open. If this number is exceeded
	 * the oldest console will be removed from the UI.
	 */
	private static final int MAX_SIZE = 5;

	public static Color getOutputColor() {
		return DebugUIPlugin
				.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_OUT_COLOR);
	}

	public static Color getErrorColor() {
		return DebugUIPlugin
				.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_ERR_COLOR);
	}

	public static Color getInputColor() {
		return DebugUIPlugin
				.getPreferenceColor(IDebugPreferenceConstants.CONSOLE_SYS_IN_COLOR);
	}

	private static LinkedList<IOConsole> history = new LinkedList<IOConsole>();

	private static void add(final IOConsole console) {

		IOConsole toClose = null;

		synchronized (history) {
			history.addLast(console);
			if (history.size() > MAX_SIZE) {
				toClose = history.removeFirst();
			}
		}

		if (toClose != null) {
			close(toClose);
		}

		console.activate();

		ConsolePlugin.getDefault().getConsoleManager()
				.addConsoles(new IConsole[] { console });

	}

	private static void close(final IOConsole console) {

		ConsolePlugin.getDefault().getConsoleManager()
				.removeConsoles(new IConsole[] { console });

	}

	public static Console console(final String title) {

		final IOConsole console = new IOConsole(title, null);

		final IOConsoleInputStream in = console.getInputStream();
		final IOConsoleOutputStream out = console.newOutputStream();
		final IOConsoleOutputStream err = console.newOutputStream();

		in.setColor(getInputColor());
		out.setColor(getOutputColor());
		err.setColor(getErrorColor());

		add(console);

		return new Console(in, out, err);
	}

	static final SimpleDateFormat TIME_FORM = //
	new SimpleDateFormat("HH:mm:ss.SSS");

	/**
	 * Eclipse problem severity parser.
	 */
	public enum Severity {
		OK(IStatus.OK), //
		INFO(IStatus.INFO), //
		WARN(IStatus.WARNING), //
		ERROR(IStatus.ERROR), //
		CANCEL(IStatus.CANCEL), //
		UNKNOWN(-1), //
		;
		public final int code;

		private Severity(final int code) {
			this.code = code;
		}

		/**
		 * Eclipse problem severity parser.
		 */
		public static Severity from(final int code) {
			switch (code) {
			case IStatus.OK:
				return OK;
			case IStatus.INFO:
				return INFO;
			case IStatus.WARNING:
				return WARN;
			case IStatus.ERROR:
				return ERROR;
			case IStatus.CANCEL:
				return CANCEL;
			default:
				return UNKNOWN;
			}
		}
	}

	/** time : severity : code : message : cause */
	private static final String TEXT_FORM = "%12s %-4s %-2d %s";

	/**
	 * Render eclipse status as string.
	 */
	public static String render(final IStatus status) {
		return render(TEXT_FORM, status);
	}

	/**
	 * Render eclipse status as string.
	 */
	public static String render(String format, final IStatus status) {

		final String time = TIME_FORM.format(new Date());
		final String severity = Severity.from(status.getSeverity()).name();
		final int code = status.getCode();
		final String message = status.getMessage();
		final Throwable cause = status.getException();

		if (cause != null) {
			format += " [%s]";
		}

		return String.format(format, time, severity, code, message, cause);

	}

}
