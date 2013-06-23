/**
 * Copyright (C) 2010-2013 Andrei Pozolotin <Andrei.Pozolotin@gmail.com>
 *
 * All rights reserved. Licensed under the OSI BSD License.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package com.carrotgarden.eclipse.fileinstall.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;

import com.carrotgarden.eclipse.fileinstall.Plugin;

/**
 * Eclipse events helper.
 */
public class EventUtil {

	/**
	 * Verify {@link IResourceChangeEvent} {@link IResourceDelta#getFlags()}
	 */
	public static boolean hasFlag(final IResourceChangeEvent event,
			final int flag) {
		if (event == null) {
			return false;
		}
		return hasFlag(event.getDelta(), flag);
	}

	/**
	 * Verify {@link IResourceDelta#getFlags()}
	 */
	public static boolean hasFlag(final IResourceDelta delta, final int flag) {
		if (delta == null) {
			return false;
		}
		return (delta.getFlags() & flag) > 0;
	}

	/**
	 * Verify {@link IResourceChangeEvent} {@link IResourceDelta#getKind()}
	 */
	public static boolean hasKind(final IResourceChangeEvent event,
			final int kind) {
		if (event == null) {
			return false;
		}
		return hasKind(event.getDelta(), kind);
	}

	/**
	 * Verify {@link IResourceDelta#getKind()}
	 */
	public static boolean hasKind(final IResourceDelta delta, final int kind) {
		if (delta == null) {
			return false;
		}
		return (delta.getKind() & kind) > 0;
	}

	/**
	 * Verify {@link IResourceChangeEvent#getType()}
	 */
	public static boolean hasType(final IResourceChangeEvent event,
			final int type) {
		if (event == null) {
			return false;
		}
		return (event.getType() & type) > 0;
	}

	public static String render(final IResourceChangeEvent event) {

		if (event == null) {
			return "null";
		}

		final StringBuilder text = new StringBuilder();

		text.append("type=");
		switch (event.getType()) {
		case IResourceChangeEvent.POST_BUILD:
			text.append("POST_BUILD");
			break;
		case IResourceChangeEvent.POST_CHANGE:
			text.append("POST_CHANGE");
			break;
		case IResourceChangeEvent.PRE_BUILD:
			text.append("PRE_BUILD");
			break;
		case IResourceChangeEvent.PRE_CLOSE:
			text.append("PRE_CLOSE");
			break;
		case IResourceChangeEvent.PRE_DELETE:
			text.append("PRE_DELETE");
			break;
		case IResourceChangeEvent.PRE_REFRESH:
			text.append("PRE_REFRESH");
			break;
		default:
			text.append("UNKNOWN(");
			text.append(event.getType());
			text.append(")");
			break;
		}
		text.append(" ");

		text.append("delta=");
		final IResourceDelta delta = event.getDelta();
		if (delta == null) {

			text.append("null");
			text.append(" ");

		} else {

			text.append(delta);
			text.append(" ");

			text.append("flag=");
			switch (delta.getFlags()) {
			case IResourceDelta.CONTENT:
				text.append("CONTENT");
				break;
			case IResourceDelta.MOVED_FROM:
				text.append("MOVED_FROM");
				break;
			case IResourceDelta.MOVED_TO:
				text.append("MOVED_TO");
				break;
			case IResourceDelta.COPIED_FROM:
				text.append("COPIED_FROM");
				break;
			case IResourceDelta.OPEN:
				text.append("OPEN");
				break;
			case IResourceDelta.TYPE:
				text.append("TYPE");
				break;
			case IResourceDelta.SYNC:
				text.append("SYNC");
				break;
			case IResourceDelta.MARKERS:
				text.append("MARKERS");
				break;
			case IResourceDelta.REPLACED:
				text.append("REPLACED");
				break;
			case IResourceDelta.DESCRIPTION:
				text.append("DESCRIPTION");
				break;
			case IResourceDelta.ENCODING:
				text.append("ENCODING");
				break;
			case IResourceDelta.LOCAL_CHANGED:
				text.append("LOCAL_CHANGED");
				break;
			case IResourceDelta.DERIVED_CHANGED:
				text.append("DERIVED_CHANGED");
				break;
			default:
				text.append("UNKNOWN(");
				text.append(delta.getFlags());
				text.append(")");
				break;
			}
			text.append(" ");

			text.append("kind=");
			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				text.append("ADDED");
				break;
			case IResourceDelta.REMOVED:
				text.append("REMOVED");
				break;
			case IResourceDelta.CHANGED:
				text.append("CHANGED");
				break;
			case IResourceDelta.ADDED_PHANTOM:
				text.append("ADDED_PHANTOM");
				break;
			case IResourceDelta.REMOVED_PHANTOM:
				text.append("REMOVED_PHANTOM");
				break;
			default:
				text.append("UNKNOWN(");
				text.append(delta.getKind());
				text.append(")");
				break;
			}
			text.append(" ");

		}

		text.append("resource=");
		final IResource resource;
		if (delta != null && delta.getResource() != null) {
			resource = delta.getResource();
		} else if (event != null && event.getResource() != null) {
			resource = event.getResource();
		} else {
			resource = null;
		}
		text.append(resource);
		text.append(" ");

		if (event.getDelta() != null) {
		}

		try {
			if (delta != null) {
				delta.accept(new IResourceDeltaVisitor() {
					@Override
					public boolean visit(final IResourceDelta delta)
							throws CoreException {
						Plugin.logInfo("found flag=" + delta.getFlags());
						Plugin.logInfo("found kind=" + delta.getKind());
						final IResource resource = delta.getResource();
						if (delta.getKind() == IResourceDelta.CHANGED) {
							if (resource instanceof IWorkspaceRoot) {
								Plugin.logInfo("found root=" + resource);
								return true;
							}
							if (resource instanceof IProject) {
								Plugin.logInfo("found project=" + resource);
								return false;
							}
						}
						return false;
					}
				});
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return text.toString();

	}

	/**
	 * Accept event delta visitor.
	 */
	public static void accept(final IResourceChangeEvent event,
			final IResourceDeltaVisitor visitor) {
		if (event == null) {
			Plugin.logWarn("event == null");
			return;
		}
		if (event.getDelta() == null) {
			Plugin.logWarn("event.getDelta() == null");
			return;
		}
		if (visitor == null) {
			Plugin.logWarn("visitor == null");
			return;
		}
		try {
			event.getDelta().accept(visitor);
		} catch (final Throwable e) {
			Plugin.logErr("Accept failure.", e);
		}
	}

}
