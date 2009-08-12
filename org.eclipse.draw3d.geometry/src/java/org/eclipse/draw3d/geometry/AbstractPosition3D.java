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
package org.eclipse.draw3d.geometry;

import java.util.EnumSet;

/**
 * Abstract implementation of {@link Position3D}, this implementation is the
 * base class for 2D-bounds synchronized and independent implementations.
 * 
 * @author Jens von Pilgrim
 * @version $Revision$
 * @since Jan 21, 2009
 */
public abstract class AbstractPosition3D implements Position3D {

	private transient Matrix4fImpl m_absoluteRotationMatrix;

	private Vector3f m_center = new Vector3fImpl();

	private boolean m_invertible;

	/**
	 * The matrix with which to transform the direction of a ray.
	 * 
	 * @see #transformRay(Vector3f, Vector3f)
	 */
	private transient Matrix4fImpl m_rayDirectionMatrix;

	/**
	 * The matrix with which to transform the origin of a ray.
	 * 
	 * @see #transformRay(Vector3f, Vector3f)
	 */
	private transient Matrix4fImpl m_rayOriginMatrix;

	/**
	 * The rotation angles of this figure.
	 */
	protected Vector3f m_rotationAngles;

	private transient Matrix4fImpl m_rotationLocationMatrix;

	private transient Matrix4fImpl m_transformationMatrix;

	/**
	 * Boolean semaphore used by {@link #syncSize()} and {@link #syncSize3D()}
	 * to avoid infinite loop.
	 */
	protected boolean m_updatingBounds;

	private boolean m_valid;

	/**
	 * Creates and initializes a new instance.
	 */
	public AbstractPosition3D() {

		m_rotationAngles = new Vector3fImpl(); // null vector
		m_transformationMatrix = new Matrix4fImpl(); // identity
		m_rotationLocationMatrix = new Matrix4fImpl(); // identity
		m_rayOriginMatrix = new Matrix4fImpl(); // identity
		m_rayDirectionMatrix = new Matrix4fImpl(); // identiy
		m_absoluteRotationMatrix = new Matrix4fImpl(); // identity
		m_valid = false;
		m_invertible = false;
		m_updatingBounds = false;
	}

	/**
	 * Notifies host if present.
	 * 
	 * @param hint
	 * @param delta
	 */
	protected void firePositionChanged(PositionHint hint, IVector3f delta) {
		if (getHost() != null)
			getHost().positionChanged(EnumSet.of(hint), delta);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.IPosition3D#getAbsolute(org.eclipse.draw3d.geometry.Position3D)
	 */
	public Position3D getAbsolute(Position3D o_result) {

		Position3D result = o_result;
		if (result == null)
			result = Position3DUtil.createAbsolutePosition();

		Position3D parentPosition = getParentPosition();
		if (parentPosition == null) {
			result.setLocation3D(getLocation3D());
			result.setSize3D(getSize3D());
			result.setRotation3D(getRotation3D());
		} else {
			Vector3f location = Math3DCache.getVector3f();
			Vector3f rotation = Math3DCache.getVector3f();
			Vector3f halfSize = Math3DCache.getVector3f();
			Vector3f center = Math3DCache.getVector3f();
			try {
				Math3D.scale(1 / 2f, getSize3D(), halfSize);
				Math3D.add(getLocation3D(), halfSize, center);
				IMatrix4f matrix = parentPosition.getRotationLocationMatrix();
				center.transform(matrix);

				Math3D.sub(center, halfSize, location);
				result.setLocation3D(location);

				rotation.set(getRotation3D());
				IHost3D parent = getHost().getParentHost3D();
				while (parent != null) {
					Math3D.add(parent.getPosition3D().getRotation3D(),
						rotation, rotation);
					parent = parent.getParentHost3D();
				}

				result.setRotation3D(rotation);
				result.setSize3D(getSize3D());

				return result;
			} finally {
				Math3DCache
					.returnVector3f(location, rotation, halfSize, center);
			}
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.IPosition3D#getAbsoluteRotationMatrix()
	 */
	public IMatrix4f getAbsoluteRotationMatrix() {

		validate();
		return m_absoluteRotationMatrix;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.IPosition3D#getCenter3D()
	 */
	public IVector3f getCenter3D() {

		validate();
		return m_center;
	}

	private Position3D getParentPosition() {

		IHost3D host = getHost();
		if (host != null) {
			IHost3D parent = host.getParentHost3D();
			if (parent != null)
				return parent.getPosition3D();

		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.IPosition3D#getRelative(org.eclipse.draw3d.geometry.IPosition3D,
	 *      org.eclipse.draw3d.geometry.Position3D)
	 */
	public Position3D getRelative(IPosition3D i_position3D, Position3D o_result) {

		Position3D result = o_result;
		if (result == null)
			result = Position3DUtil.createRelativePosition(getHost());

		Position3D abs = Math3DCache.getPosition3D();
		Vector3f tmp = Math3DCache.getVector3f();
		try {
			getAbsolute(abs);

			Math3D.sub(i_position3D.getLocation3D(), abs.getLocation3D(), tmp);
			result.setLocation3D(tmp);

			Math3D.sub(i_position3D.getRotation3D(), abs.getRotation3D(), tmp);
			result.setRotation3D(tmp);

			result.setSize3D(i_position3D.getSize3D());
			return result;
		} finally {
			Math3DCache.returnPosition3D(abs);
			Math3DCache.returnVector3f(tmp);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.IPosition3D#getRotation3D()
	 */
	public IVector3f getRotation3D() {

		return m_rotationAngles;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.IPosition3D#getRotationLocationMatrix()
	 */
	public IMatrix4f getRotationLocationMatrix() {

		validate();
		return m_rotationLocationMatrix;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.IPosition3D#getTransformationMatrix()
	 */
	public IMatrix4f getTransformationMatrix() {

		validate();
		return m_transformationMatrix;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.Position3D#invalidate()
	 */
	public void invalidate() {

		m_valid = false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.IPosition3D#isValid()
	 */
	public boolean isValid() {

		if (!m_valid)
			return false;

		Position3D parentPosition = getParentPosition();
		if (parentPosition != null)
			if (!parentPosition.isValid())
				return false;

		return true;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.Position3D#setCenter3D(org.eclipse.draw3d.geometry.IVector3f)
	 */
	public void setCenter3D(IVector3f i_center) {

		Vector3f delta = Math3DCache.getVector3f();
		Vector3f location = Math3DCache.getVector3f();
		try {
			Math3D.sub(getCenter3D(), i_center, delta);
			Math3D.sub(getLocation3D(), delta, location);

			setLocation3D(location);
		} finally {
			Math3DCache.returnVector3f(delta, location);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.Position3D#setPosition(org.eclipse.draw3d.geometry.IPosition3D)
	 */
	public void setPosition(IPosition3D i_position3D) {

		Position3D absNew = Math3DCache.getPosition3D();
		Position3D absParent = Math3DCache.getPosition3D();
		Vector3f location = Math3DCache.getVector3f();
		Vector3f rotation = Math3DCache.getVector3f();
		try {
			i_position3D.getAbsolute(absNew);
			location.set(absNew.getLocation3D());
			rotation.set(absNew.getRotation3D());

			Position3D parentPosition = getParentPosition();
			if (parentPosition != null) {
				
				IMatrix4f m = parentPosition.getRotationLocationMatrix();
				IMatrix4f inv = Math3D.invert(m, null);
				
				location.transform(inv);
				
				parentPosition.getAbsolute(absParent);
				Math3D.sub(rotation, absParent.getRotation3D(), rotation);
			}

			setLocation3D(location);
			setRotation3D(rotation);
			setSize3D(absNew.getSize3D());
		} finally {
			Math3DCache.returnPosition3D(absNew, absParent);
			Math3DCache.returnVector3f(location, rotation);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.Position3D#setRotation3D(org.eclipse.draw3d.geometry.IVector3f)
	 */
	public void setRotation3D(IVector3f i_rotation) {
		if (i_rotation == null) // parameter precondition
			throw new NullPointerException("i_rotation must not be null");

		if (m_rotationAngles.equals(i_rotation))
			return;

		Vector3fImpl delta = new Vector3fImpl();
		Math3D.sub(i_rotation, m_rotationAngles, delta);

		m_rotationAngles.set(i_rotation);
		invalidate();

		firePositionChanged(PositionHint.ROTATION, delta);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("l=").append(getLocation3D());
		result.append(", s=").append(getSize3D());
		result.append(", r=").append(getRotation3D());
		result.append(", host=").append(getHost());
		return result.toString();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.IPosition3D#transformRay(org.eclipse.draw3d.geometry.Vector3f,
	 *      org.eclipse.draw3d.geometry.Vector3f)
	 */
	public boolean transformRay(Vector3f i_origin, Vector3f i_direction) {

		validate();

		if (!m_invertible)
			return false;

		i_origin.transform(m_rayOriginMatrix);
		i_direction.transform(m_rayDirectionMatrix);
		return true;
	}

	private void validate() {

		if (isValid())
			return;

		m_center.set(getSize3D());
		m_center.scale(0.5f);
		Math3D.add(getLocation3D(), m_center, m_center);

		Vector3f halfSize = Math3DCache.getVector3f();
		try {
			// calculate the transformation and rotation / location matrices
			// transformations are applied in reverse order
			Position3D parent = getParentPosition();
			if (parent != null) {
				Math3D.translate(parent.getRotationLocationMatrix(),
					getLocation3D(), m_rotationLocationMatrix);
				m_absoluteRotationMatrix
					.set(parent.getAbsoluteRotationMatrix());
			} else {
				Math3D.translate(IMatrix4f.IDENTITY, getLocation3D(),
					m_rotationLocationMatrix);
				m_absoluteRotationMatrix.setIdentity();
			}

			if (!IVector3f.NULLVEC3f.equals(getRotation3D())) {
				Math3D.scale(0.5f, getSize3D(), halfSize);
				Math3D.translate(m_rotationLocationMatrix, halfSize,
					m_rotationLocationMatrix);

				Math3D.rotate(getRotation3D(), m_rotationLocationMatrix,
					m_rotationLocationMatrix);

				Math3D.scale(-0.5f, getSize3D(), halfSize);
				Math3D.translate(m_rotationLocationMatrix, halfSize,
					m_rotationLocationMatrix);

				Math3D.rotate(getRotation3D(), m_absoluteRotationMatrix,
					m_absoluteRotationMatrix);
			}

			m_transformationMatrix.set(m_rotationLocationMatrix);

			Math3D.scale(getSize3D(), m_transformationMatrix,
				m_transformationMatrix);

			// calculate the inverse ray transformation matrices
			if (getSize3D().getX() == 0 || getSize3D().getY() == 0
				|| getSize3D().getZ() == 0) {
				m_invertible = false;
			} else {
				m_invertible = true;
				Math3D.invert(m_transformationMatrix, m_rayOriginMatrix);

				Math3D.scale(getSize3D(), m_absoluteRotationMatrix,
					m_rayDirectionMatrix);
				Math3D.invert(m_rayDirectionMatrix, m_rayDirectionMatrix);
			}

			m_valid = true;
		} finally {
			Math3DCache.returnVector3f(halfSize);
		}
	}
}
