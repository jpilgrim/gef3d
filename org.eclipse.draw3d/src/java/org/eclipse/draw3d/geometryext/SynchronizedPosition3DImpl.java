/*******************************************************************************
 * Copyright (c) 2009 Jens von Pilgrim and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jens von Pilgrim - initial API and implementation
 ******************************************************************************/
package org.eclipse.draw3d.geometryext;

import java.util.EnumSet;

import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw3d.geometry.AbstractPosition3D;
import org.eclipse.draw3d.geometry.IBoundingBox;
import org.eclipse.draw3d.geometry.IHost3D;
import org.eclipse.draw3d.geometry.IMatrix4f;
import org.eclipse.draw3d.geometry.IVector3f;
import org.eclipse.draw3d.geometry.Math3D;
import org.eclipse.draw3d.geometry.Matrix4f;
import org.eclipse.draw3d.geometry.Matrix4fImpl;
import org.eclipse.draw3d.geometry.Position3D;
import org.eclipse.draw3d.geometry.Vector3f;
import org.eclipse.draw3d.geometry.Vector3fImpl;
import org.eclipse.draw3d.geometry.IPosition3D.MatrixState;
import org.eclipse.draw3d.geometry.IPosition3D.PositionHint;

/**
 * Mutable implementation of {@link Position3D}, based on a synchronized
 * bounds object. That is the properties of this 3D position object is
 * synchronized via {@link SyncedBounds3D} with a 2D object.
 * 
 * @author Jens von Pilgrim
 * @version $Revision$
 * @since Jan 21, 2009
 */
public class SynchronizedPosition3DImpl extends AbstractPosition3D {
	
	private SyncHost3D host;

	private SyncedBounds3D bounds3D;

	/**
	 * @param i_syncHost, must not be null here!
	 */
	public SynchronizedPosition3DImpl(SyncHost3D i_syncHost) {
		if (i_syncHost == null) // parameter precondition
			throw new NullPointerException("i_syncHost must not be null");

		host = i_syncHost;
		bounds3D = new SyncedBounds3D();
		bounds3D.setDepth(1);
		rotation = new Vector3fImpl(0, 0, 0);
		updatingBounds = false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.IPosition3D#getHost()
	 */
	public IHost3D getHost() {
		return host;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.IPosition3D#getBounds3D()
	 */
	public IBoundingBox getBounds3D() {
		return bounds3D.getBoundingBox(host.getBounds());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.IPosition3D#getLocation3D()
	 */
	public IVector3f getLocation3D() {
		return bounds3D.getLocation3D(host.getBounds());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.Position3D#setLocation3D(org.eclipse.draw3d.geometry.IVector3f)
	 */
	public void setLocation3D(IVector3f i_point) {
		if (i_point == null) // parameter precondition
			throw new NullPointerException("i_point must not be null");

		if (getLocation3D().equals(i_point))
			return;

		Vector3fImpl delta = new Vector3fImpl();
		Math3D.sub(i_point, getLocation3D(), delta);

		Rectangle newBounds = bounds3D.setBounds3D(i_point, getSize3D());

		if (delta.x != 0 || delta.y != 0)
			host.setBounds(newBounds);

		invalidateMatrices();

		firePositionChanged(PositionHint.location, delta);

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.IPosition3D#getSize3D()
	 */
	public IVector3f getSize3D() {
		return bounds3D.getSize3D(host.getBounds());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.Position3D#setSize3D(org.eclipse.draw3d.geometry.IVector3f)
	 */
	public void setSize3D(IVector3f i_size) {
		if (i_size == null) // parameter precondition
			throw new NullPointerException("i_size must not be null");
//		if (i_size.getX() < 0 || i_size.getY() < 0 || i_size.getZ() < 0) // parameter
//			// precondition
//			throw new IllegalArgumentException(
//					"no value of given vector must be less 0, , was " + i_size);

		IVector3f size3D = getSize3D();

		if (size3D.equals(i_size))
			return;

		Vector3fImpl delta = new Vector3fImpl();
		Math3D.sub(i_size, size3D, delta);

		Rectangle newBounds = bounds3D.setBounds3D(getLocation3D(), i_size);
		if (delta.x != 0 || delta.y != 0)
			host.setBounds(newBounds);

		invalidateMatrices();

		firePositionChanged(PositionHint.size, delta);
	}

}