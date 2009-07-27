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
 *    Kristian Duske - refactoring and optimizations
 ******************************************************************************/
package org.eclipse.gef3d.tools;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw3d.geometry.IVector3f;
import org.eclipse.draw3d.geometry.Vector3f;
import org.eclipse.draw3d.geometry.Vector3fImpl;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.tools.ResizeTracker;
import org.eclipse.gef3d.requests.ChangeBounds3DRequest;

/**
 * Extends original version adding 3D functionality.
 * 
 * @todo SnapToHelper and constraint modification not implemented yet.
 * 
 * @author Randy Hudson (hudsonr) (original 2D version)
 * @author Jens von Pilgrim
 * @author Kristian Duske
 * @version $Revision$
 * @since Mar 31, 2008
 */
public class ResizeTracker3D extends ResizeTracker {
	/**
	 * Logger for this class
	 */
	private static final Logger log =
		Logger.getLogger(ResizeTracker3D.class.getName());

	protected TrackState m_trackState;

	/**
	 * Workaround, this attribute is private in ResizeTracker and mirrored here
	 * (set via constructor).
	 */
	GraphicalEditPart owner;

	/**
	 * @param i_owner
	 * @param i_direction
	 */
	public ResizeTracker3D(GraphicalEditPart i_owner, int i_direction) {

		super(i_owner, i_direction);
		owner = i_owner;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Copied and modified to support 3D, creates a
	 * {@link ChangeBounds3DRequest} instead of a {@link ChangeBoundsRequest}
	 * </p>
	 * 
	 * @see org.eclipse.gef.tools.ResizeTracker#createSourceRequest()
	 */
	@Override
	protected Request createSourceRequest() {

		ChangeBounds3DRequest request;
		request = new ChangeBounds3DRequest(REQ_RESIZE);
		request.setResizeDirection(getResizeDirection());
		return request;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.gef.tools.AbstractTool#getLocation()
	 */
	@Override
	protected Point getLocation() {

		return new Point(getTrackState().getLocation2D());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.gef.tools.AbstractTool#getStartLocation()
	 */
	@Override
	protected Point getStartLocation() {

		return new Point(getTrackState().getStartLocation2D());
	}

	/**
	 * Returns the current dragstate. If the drag has just begun, a new
	 * trackstate will be created.
	 * 
	 * @return
	 */
	protected TrackState getTrackState() {

		Point location = getCurrentInput().getMouseLocation();
		if (m_trackState == null) {
			if (log.isLoggable(Level.FINE))
				log.fine("creating track state");

			m_trackState =
				Tracker3DHelper.getTrackState(location, getCurrentViewer());
		}

		m_trackState.setScreenLocation(getCurrentInput().getMouseLocation());
		return m_trackState;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.gef.tools.SimpleDragTracker#performDrag()
	 */
	@Override
	protected void performDrag() {
		try {
			super.performDrag();
		} finally {
			m_trackState = null;
			Tracker3DHelper.getPicker(getCurrentViewer()).clearIgnored();
		}
		if (log.isLoggable(Level.FINE))
			log.fine("resize finished");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.gef.tools.ResizeTracker#updateSourceRequest()
	 */
	@Override
	protected void updateSourceRequest() {

		if (log.isLoggable(Level.FINE))
			log.fine("updating source request");

		// first, behave just like a 2D tracker
		super.updateSourceRequest();

		Request request = getSourceRequest();
		if (request instanceof ChangeBounds3DRequest) {

			// and here we add 3D features:
			ChangeBounds3DRequest req3D = (ChangeBounds3DRequest) request;

			Vector3f moveDelta3D = getTrackState().getMoveDelta3D();
			IVector3f location3D =
				new Vector3fImpl(getTrackState().getLocation3D());

			Vector3fImpl moveDelta = new Vector3fImpl();
			Vector3fImpl resizeDelta = new Vector3fImpl();

			/* TODO recognize shift */
			/*
			 * if (getCurrentInput().isShiftKeyDown() && owner != null) {
			 * request.setConstrainedResize(true); int origHeight =
			 * owner.getFigure().getBounds().height; int origWidth =
			 * owner.getFigure().getBounds().width; float ratio = 1; if
			 * (origWidth != 0 && origHeight != 0) ratio = ((float) origHeight /
			 * (float) origWidth); if (getResizeDirection() ==
			 * PositionConstants.SOUTH_EAST) { if (d.height > (d.width ratio))
			 * d.width = (int) (d.height / ratio); else d.height = (int)
			 * (d.width ratio); } else if (getResizeDirection() ==
			 * PositionConstants.NORTH_WEST) { if (d.height < (d.width ratio))
			 * d.width = (int) (d.height / ratio); else d.height = (int)
			 * (d.width ratio); } else if (getResizeDirection() ==
			 * PositionConstants.NORTH_EAST) { if (-(d.height) > (d.width
			 * ratio)) d.width = -(int) (d.height / ratio); else d.height =
			 * -(int) (d.width ratio); } else if (getResizeDirection() ==
			 * PositionConstants.SOUTH_WEST) { if (-(d.height) < (d.width
			 * ratio)) d.width = -(int) (d.height / ratio); else d.height =
			 * -(int) (d.width ratio); } } else
			 * request.setConstrainedResize(false);
			 */

			// already set in super call
			// GEF:
			// request.setCenteredResize(getCurrentInput().isModKeyDown(SWT.MOD1));
			// replaced height with y and width with x in the following lines:
			if ((getResizeDirection() & PositionConstants.NORTH) != 0) {
				if (getCurrentInput().isControlKeyDown()) {
					resizeDelta.y -= moveDelta3D.getY();
				}
				moveDelta.y += moveDelta3D.getY();
				resizeDelta.y -= moveDelta3D.getY();
			}
			if ((getResizeDirection() & PositionConstants.SOUTH) != 0) {
				if (getCurrentInput().isControlKeyDown()) {
					moveDelta.y -= moveDelta3D.getY();
					resizeDelta.y += moveDelta3D.getY();
				}
				resizeDelta.y += moveDelta3D.getY();
			}
			if ((getResizeDirection() & PositionConstants.WEST) != 0) {
				if (getCurrentInput().isControlKeyDown()) {
					resizeDelta.x -= moveDelta3D.getX();
				}
				moveDelta.x += moveDelta3D.getX();
				resizeDelta.x -= moveDelta3D.getX();
			}
			if ((getResizeDirection() & PositionConstants.EAST) != 0) {
				if (getCurrentInput().isControlKeyDown()) {
					moveDelta.x -= moveDelta3D.getX();
					resizeDelta.x += moveDelta3D.getX();
				}
				resizeDelta.x += moveDelta3D.getX();
			}

			// call 3D setters (instead of2d)
			req3D.setMoveDelta3D(moveDelta);
			req3D.setSizeDelta3D(resizeDelta);
			req3D.setLocation3D(location3D);

			/*
			 * already set in super call: GEF:
			 * request.setEditParts(getOperationSet()); GEF:
			 * request.getExtendedData().clear();
			 */

			/* TODO handle snapToHelper */
			/*
			 * if (!getCurrentInput().isAltKeyDown() && snapToHelper != null) {
			 * PrecisionRectangle rect = sourceRect.getPreciseCopy();
			 * rect.translate(moveDelta); rect.resize(resizeDelta);
			 * PrecisionRectangle result = new PrecisionRectangle();
			 * snapToHelper.snapRectangle(request, request.getResizeDirection(),
			 * rect, result); if (request.isCenteredResize()) { if
			 * (result.preciseX != 0.0) result.preciseWidth += -result.preciseX;
			 * else if (result.preciseWidth != 0.0) { result.preciseX =
			 * -result.preciseWidth; result.preciseWidth *= 2.0; } if
			 * (result.preciseY != 0.0) result.preciseHeight +=
			 * -result.preciseY; else if (result.preciseHeight != 0.0) {
			 * result.preciseY = -result.preciseHeight; result.preciseHeight *=
			 * 2.0; } result.updateInts(); } PrecisionPoint preciseMove = new
			 * PrecisionPoint(result.x + moveDelta.x, result.y + moveDelta.y);
			 * PrecisionDimension preciseResize = new PrecisionDimension(
			 * result.width + resizeDelta.width, result.height +
			 * resizeDelta.height); request.setMoveDelta(preciseMove);
			 * request.setSizeDelta(preciseResize); }
			 */
		} else {
			log
				.warning("3D coordinates of request not set, wrong type: " + request); //$NON-NLS-1$
		}
	}
}