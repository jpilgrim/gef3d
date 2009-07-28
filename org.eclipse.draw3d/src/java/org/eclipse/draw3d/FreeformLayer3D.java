/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others,
 * Copyright (c) 2008 Jens von Pilgrim and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation of 2D version
 *    Jens von Pilgrim - initial API and implementation
 ******************************************************************************/
package org.eclipse.draw3d;

import java.util.Iterator;
import java.util.logging.Logger;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureListener;
import org.eclipse.draw2d.FreeformFigure;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw3d.draw2dports.FreeformHelper;

/**
 * 3D version of {@link FreeformLayer}. Besides the information found in GEF's
 * {@link FreeformLayer} documentation
 * ("A Layer that can extend in all 4 directions."), the really important thing
 * about this class is that it's observing its children using a
 * {@link FreeformHelper}. I.e. the helper registers itsself as a
 * {@link FreeformListener} or (if the children is not a {@link FreeformFigure})
 * as a {@link FigureListener}. If a child's extent changed, the helper is
 * notified. Then, {@link #fireExtentChanged()} and {@link #revalidate()} are
 * called by the helper.
 * <p>
 * Internal note: Why is this necessary? Still not clear.
 * </p>
 * 
 * @author IBM Corporation (original 2D version)
 * @author Jens von Pilgrim
 * @version $Revision$
 * @since Feb 11, 2008
 */
public class FreeformLayer3D extends HostFigure3D implements FreeformFigure {
	/**
	 * Logger for this class
	 */
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(FreeformLayer3D.class
			.getName());

	/**
	 * <p>
	 * Copied and modified from {@link FreeformLayer}. This is not the original
	 * FreeformHelper, since GEF's FreeformHelper is package visible only. It's
	 * a 1:1 copy (maybe changed in future versions, so look at
	 * {@link FreeformHelper}'s javadoc. It was also renamed (from helper to
	 * freeformHelper).
	 * </p>
	 */
	private final FreeformHelper freeformHelper = new FreeformHelper(this);

	/**
	 * <p>
	 * Copied (and not modified yet) from
	 * {@link FreeformLayer#add(IFigure, Object, int)}
	 * </p>
	 * 
	 * @see IFigure#add(IFigure, Object, int)
	 */
	@Override
	public void add(IFigure child, Object constraint, int index) {
		super.add(child, constraint, index);
		freeformHelper.hookChild(child);
	}

	/**
	 * <p>
	 * Copied (and not modified yet) from
	 * {@link FreeformLayer#addFreeformListener(FreeformListener)}
	 * </p>
	 * 
	 * @see FreeformFigure#addFreeformListener(FreeformListener)
	 */
	public void addFreeformListener(FreeformListener listener) {
		addListener(FreeformListener.class, listener);
	}

	/**
	 * <p>
	 * Copied (and not modified yet) from
	 * {@link FreeformLayer#fireExtentChanged()}
	 * </p>
	 * 
	 * @see FreeformFigure#fireExtentChanged()
	 */
	public void fireExtentChanged() {
		Iterator iter = getListeners(FreeformListener.class);
		while (iter.hasNext())
			((FreeformListener) iter.next()).notifyFreeformExtentChanged();
	}

	/**
	 * Overrides to do nothing.
	 * <p>
	 * Copied (and not modified yet) from {@link FreeformLayer}
	 * </p>
	 * 
	 * @see Figure#fireMoved()
	 */
	@Override
	protected void fireMoved() {
	}

	/**
	 * <p>
	 * Copied (and not modified yet) from
	 * {@link FreeformLayer#getFreeformExtent()}
	 * </p>
	 * 
	 * @see FreeformFigure#getFreeformExtent()
	 */
	public Rectangle getFreeformExtent() {
		return freeformHelper.getFreeformExtent();
	}

	/**
	 * <p>
	 * Copied (and not modified yet) from
	 * {@link FreeformLayer#primTranslate(int, int)}
	 * </p>
	 * 
	 * @see Figure#primTranslate(int, int)
	 */
	@Override
	public void primTranslate(int dx, int dy) {
		bounds.x += dx;
		bounds.y += dy;
	}

	/**
	 * <p>
	 * Copied (and not modified yet) from {@link FreeformLayer#remove(IFigure)}
	 * </p>
	 * 
	 * @see IFigure#remove(IFigure)
	 */
	@Override
	public void remove(IFigure child) {
		freeformHelper.unhookChild(child);
		super.remove(child);
	}

	/**
	 * <p>
	 * Copied (and not modified yet) from
	 * {@link FreeformLayer#removeFreeformListener(FreeformListener)}
	 * </p>
	 * 
	 * @see FreeformFigure#removeFreeformListener(FreeformListener)
	 */
	public void removeFreeformListener(FreeformListener listener) {
		removeListener(FreeformListener.class, listener);
	}

	/**
	 * <p>
	 * Copied (and not modified yet) from
	 * {@link FreeformLayer#setFreeformBounds(Rectangle)}
	 * </p>
	 * 
	 * @see FreeformFigure#setFreeformBounds(Rectangle)
	 */
	public void setFreeformBounds(Rectangle bounds) {
		freeformHelper.setFreeformBounds(bounds);
	}

}
