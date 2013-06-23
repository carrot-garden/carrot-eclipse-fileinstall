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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;

import com.carrotgarden.eclipse.fileinstall.Plugin;

/**
 * Utility methods to convert exceptions into other types of exceptions, status
 * objects etc.
 * 
 * @author Kris De Volder
 */
public class ExceptionUtil {

	public static CoreException coreException(final int severity,
			final String msg) {
		return coreException(status(severity, msg));
	}

	public static CoreException coreException(final IStatus status) {
		final Throwable e = status.getException();
		if (e == null) {
			return new CoreException(status);
		} else if (e instanceof CoreException) {
			return (CoreException) e;
		}
		return new CoreException(status);
	}

	public static CoreException coreException(final String msg) {
		return coreException(IStatus.ERROR, msg);
	}

	public static CoreException coreException(final Throwable e) {
		if (e instanceof CoreException) {
			return (CoreException) e;
		} else {
			return coreException(status(e));
		}
	}

	public static Throwable getDeepestCause(final Throwable e) {
		Throwable cause = e;
		Throwable parent = e.getCause();
		while (parent != null && parent != e) {
			cause = parent;
			parent = cause.getCause();
		}
		return cause;
	}

	public static String getMessage(final Throwable e) {
		// The message of nested exception is usually more interesting than the
		// one on top.
		final Throwable cause = getDeepestCause(e);
		final String msg = cause.getMessage();
		if (msg != null) {
			return msg;
		}
		return e.getMessage();
	}

	/**
	 * This exception is raised when a project is not properly connected to its
	 * 'root' project, which will result in the inability to fetch a model for
	 * the project.
	 */
	// public static InconsistenProjectHierarchyException
	// inconsistentProjectHierachy(GradleProject project) {
	// return new
	// InconsistenProjectHierarchyException(status("Gradle project hierarchy is inconsistent for '"+project.getDisplayName()+"'"));
	// }

	public static IllegalStateException notImplemented(final String string) {
		return new IllegalStateException("Not implemented: " + string);
	}

	public static IStatus status(final int severity, final String msg) {
		return new Status(severity, Plugin.PLUGIN_ID, msg);
	}

	public static IStatus status(final Throwable e) {
		return status(IStatus.ERROR, e);
	}

	public static IStatus status(final int severity, final Throwable e) {
		if (e instanceof OperationCanceledException
				|| e instanceof InterruptedException) {
			return Status.CANCEL_STATUS;
		}
		if (e instanceof CoreException) {
			final IStatus status = ((CoreException) e).getStatus();
			if (status != null && status.getSeverity() == severity) {
				final Throwable ee = status.getException();
				if (ee != null) {
					return status;
				}
			}
		}
		// if (e instanceof BuildException) {
		// final String msg = getMessage(e);
		// if (msg.contains("Task 'cleanEclipse' not found")
		// || msg.contains("Task 'eclipse' not found")) {
		// e = new Gradle1792BugException(e);
		// }
		// }
		return new Status(severity, Plugin.PLUGIN_ID, getMessage(e), e);
	}

	public static IStatus status(final String msg) {
		return status(IStatus.ERROR, msg);
	}

	public static final IStatus OK_STATUS = status(IStatus.OK, "");

}
