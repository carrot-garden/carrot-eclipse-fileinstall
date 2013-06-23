/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.eclipse.fileinstall.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.carrotgarden.eclipse.fileinstall.Plugin;

/**
 * Eclipse project helper.
 */
public class ProjectUtil {

	/**
	 * Extract class path resource to the file system.
	 */
	public static void copyFromClasspathIntoProject(
	//
			final Class<?> klaz, //
			final String sourcePath, //
			final IProject project, //
			final String targetPath //
	) throws Exception {

		final InputStream input = klaz.getResourceAsStream(sourcePath);

		final File target = file(project, targetPath);

		if (!target.exists()) {
			target.getParentFile().mkdirs();
		}

		final OutputStream output = new FileOutputStream(target);

		IOUtils.copy(input, output);

		input.close();
		output.close();

	}

	/**
	 * File located inside the project.
	 */
	public static File file(//
			final IProject project, //
			final String targetPath //
	) {
		return project.getFile(targetPath).getLocation().toFile();
	}

	/**
	 * Verify project has a file.
	 */
	public static boolean hasFile(//
			final IProject project, //
			final String targetPath //
	) {
		return file(project, targetPath).exists();
	}

	/**
	 * Report project problem severity.
	 */
	public static int severity(final IProject project) {
		try {
			final int severity = project.findMaxProblemSeverity(
					IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			// Plugin.logInfo("ProjectUtil#severity = " + severity + " @ "
			// + project);
			return severity;
		} catch (final Throwable e) {
			Plugin.logErrr("ProjectUtil#severity failure", e);
			return IMarker.SEVERITY_ERROR;
		}

	}

	/**
	 * Manifest file name.
	 */
	public static final String MANIFEST_FILE = //
	"MANIFEST.MF";

	/**
	 * Manifest location on class path.
	 */
	public static final String MANIFEST_PATH = //
	"META-INF" + "/" + MANIFEST_FILE;

	/**
	 * Location of build manifest in the maven project.
	 */
	public static final String MANIFEST_MAVEN = //
	"target/classes" + "/" + MANIFEST_PATH;

	/**
	 * Report project manifest file; can be missing.
	 */
	public static File manifest(final IProject project) {
		final File file = manifestDiscover(project);
		Plugin.logOK("ProjectUtil#manifest: file: " + file);
		return file;
	}

	/**
	 * Discover project manifest from multiple locations.
	 */
	private static File manifestDiscover(final IProject project) {
		if (NatureUtil.hasJavaNature(project)) {
			try {
				final IJavaProject java = JavaCore.create(project);
				final IPath output = java.getOutputLocation().makeRelativeTo(
						project.getFullPath());
				final IFile manifest = project.getFolder(output).getFile(
						MANIFEST_PATH);
				return manifest.getLocation().toFile();
			} catch (final Throwable e) {
				Plugin.logErrr("ProjectUtil#manifest: failure", e);
				return manifestDefault(project);
			}
		}
		return manifestDefault(project);
	}

	/**
	 * Defautl manifest location.
	 */
	private static File manifestDefault(final IProject project) {
		return file(project, MANIFEST_MAVEN);
	}

}
