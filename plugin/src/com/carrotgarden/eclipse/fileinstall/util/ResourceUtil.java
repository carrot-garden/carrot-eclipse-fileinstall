/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.eclipse.fileinstall.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * Eclipse resource helper.
 */
public class ResourceUtil {

	/**
	 * Workspace path.
	 */
	public static String workspacePath() {
		return workspaceRoot().getLocation().toFile().getAbsolutePath();
	}

	public static IWorkspaceRoot workspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/**
	 * Project handle.
	 */
	public static IProject project(final String name) {
		return workspaceRoot().getProject(name);
	}

	/**
	 * Verify if project exists.
	 */
	public static boolean hasProject(final String name) {
		return project(name).exists();
	}

	/**
	 * Verify if project open.
	 */
	public static boolean hasProjectOpen(final String name) {
		final IProject project = project(name);
		return project.exists() && project.isOpen();
	}

}
