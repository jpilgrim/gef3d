/*******************************************************************************
 * Copyright (c) 2008 Jens von Pilgrim and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jens von Pilgrim - initial API and implementation
 ******************************************************************************/
package org.eclipse.gef3d.requests;

import org.eclipse.draw3d.ISurface;
import org.eclipse.draw3d.geometry.IVector3f;
import org.eclipse.gef.Request;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef3d.tools.DragEditPartsTracker3D;

/**
 * 3D equivalent of {@link ChangeBoundsRequest}, stores additional 3D
 * information to enable 3D movement and change of depth.
 * <p>
 * Note: All 3D locations are in world coordinates. All deltas are computed
 * using the start surface (set in constructor).
 * </p>
 * <h3>Internal notes</h3> This request is created by the
 * {@link DragEditPartsTracker3D}, which itself is to be created in
 * {@link org.eclipse.gef.EditPart#getDragTracker(Request)}
 * 
 * @author Jens von Pilgrim
 * @version $Revision$
 * @since Mar 31, 2008
 */
public class ChangeBounds3DRequest extends ChangeBoundsRequest {

	public static enum Modifier3D {
		NONE, DEPTH, ROTATION
	}

	/**
	 * 3D depth delta.
	 */
	protected IVector3f depthDelta3D = null;

	/**
	 * 3D move delta
	 */
	protected IVector3f moveDepthDelta3D = null;

	/**
	 * 3D location delta
	 */
	protected IVector3f location3D = null;

	/**
	 * 3D rotation delta.
	 */
	protected IVector3f rotationDelta3D = null;

	/**
	 * Modifier indicating change of depth or rotation (or none); set in
	 * constructor.
	 */
	protected Modifier3D modifier3D;

	/**
	 * The surface when the request had been created
	 */
	protected ISurface startSurface;

	/**
	 * @param i_type type of request, passed to super constructor
	 */
	public ChangeBounds3DRequest(Object i_type) {
		super(i_type);
		this.modifier3D = Modifier3D.NONE;
	}

	/**
	 * Returns the size delta as set in {@link #setDepthDelta3D(IVector3f)}.
	 * This is usually the delta 
	 * @return the depthDelta3D
	 */
	public IVector3f getDepthDelta3D() {
		return depthDelta3D;
	}

	/**
	 * @param i_sizeDelta3D the depthDelta3D to set
	 */
	public void setDepthDelta3D(IVector3f i_depthDelta3D) {
		depthDelta3D = i_depthDelta3D;
	}

	/**
	 * @return the moveDepthDelta3D
	 */
	public IVector3f getMoveDepthDelta3D() {
		return moveDepthDelta3D;
	}

	/**
	 * @param i_moveDelta3D the moveDepthDelta3D to set
	 */
	public void setMoveDepthDelta3D(IVector3f i_moveDelta3D) {
		moveDepthDelta3D = i_moveDelta3D;
	}

	/**
	 * @return the location3D
	 */
	public IVector3f getLocation3D() {
		return location3D;
	}

	/**
	 * @param i_location3d the location3D to set
	 */
	public void setLocation3D(IVector3f i_location3d) {
		location3D = i_location3d;
	}

	/**
	 * @return the rotationDelta3D
	 */
	public IVector3f getRotationDelta3D() {
		return rotationDelta3D;
	}

	/**
	 * @param i_rotationDelta3D the rotationDelta3D to set
	 */
	public void setRotationDelta3D(IVector3f i_rotationDelta3D) {
		rotationDelta3D = i_rotationDelta3D;
	}

	/**
	 * @return the modifier3D
	 */
	public Modifier3D getModifier3D() {
		return modifier3D;
	}

	/**
	 * @param i_modifier3d the modifier3D to set
	 */
	public void setModifier3D(Modifier3D i_modifier3d) {
		modifier3D = i_modifier3d;
	}
	
	/**
	 * @param i_startSurface the startSurface to set
	 */
	public void setStartSurface(ISurface i_startSurface) {
		startSurface = i_startSurface;
	}

	/**
	 * @return
	 */
	public ISurface getStartSurface() {
		return startSurface;
	}

	
}
