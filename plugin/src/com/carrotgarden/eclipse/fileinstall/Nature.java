/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.eclipse.fileinstall;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * Plugin project natrue.
 */
public class Nature implements IProjectNature {

	/**
	 * Permanent plug-in nature GUID.
	 */
	public static final String NATURE_ID = "com.carrotgarden.eclipse.fileinstall.nature";

	private volatile IProject project;

	@Override
	public void configure() throws CoreException {
		Plugin.logInfo("Nature activate for " + getProject());
	}

	@Override
	public void deconfigure() throws CoreException {
		Plugin.logInfo("Nature deactivate for " + getProject());
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(final IProject project) {
		this.project = project;
	}

}
