/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.eclipse.fileinstall;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.compiler.CompilationParticipant;

/**
 * Java build listener.
 */
public class Builder extends CompilationParticipant {

	@Override
	public boolean isActive(final IJavaProject project) {
		final Plugin instance = Plugin.instance();
		if (instance == null) {
			return false;
		}
		/** React only to registered worker projects. */
		return instance.manager().hasWorker(project);
	}

	@Override
	public void buildFinished(final IJavaProject project) {
		final Plugin instance = Plugin.instance();
		if (instance == null) {
			return;
		}
		/** Notify registered worker projects. */
		instance.manager().buildFinished(project);
	}

}
