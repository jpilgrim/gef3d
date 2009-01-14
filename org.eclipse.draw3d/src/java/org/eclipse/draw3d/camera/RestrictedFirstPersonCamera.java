/*******************************************************************************
 * Copyright (c) 2008 Jens von Pilgrim and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Kristian Duske - initial API and implementation
 ******************************************************************************/
package org.eclipse.draw3d.camera;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.draw3d.RenderContext;
import org.eclipse.draw3d.geometry.IVector3f;
import org.eclipse.draw3d.geometry.Math3D;
import org.eclipse.draw3d.geometry.Matrix4fImpl;
import org.eclipse.draw3d.geometry.Vector3f;
import org.eclipse.draw3d.geometry.Vector3fImpl;
import org.eclipse.draw3d.graphics3d.Graphics3D;
import org.eclipse.draw3d.graphics3d.Graphics3DDraw;

/**
 * A camera that implements a first person strategy and that restricts movement
 * so that a sense of up and down is retained.
 * 
 * @author Kristian Duske
 * @version $Revision$
 * @since 19.11.2007
 */
public class RestrictedFirstPersonCamera implements ICamera {

	private static final float HALF_PI = (float) Math.PI / 2;

	@SuppressWarnings("unused")
	private static Logger log = Logger
			.getLogger(RestrictedFirstPersonCamera.class.getName());

	/**
	 * Matrix4f to use in calculations.
	 */
	private final Matrix4fImpl TMP_M = new Matrix4fImpl();

	/**
	 * Vector3f to use in calculations.
	 */
	private final Vector3fImpl TMP_V3 = new Vector3fImpl();

	private final List<ICameraListener> m_listeners = new ArrayList<ICameraListener>(
			3);

	private final Vector3fImpl m_position = new Vector3fImpl();

	private final Vector3fImpl m_right = new Vector3fImpl();

	private final Vector3fImpl m_up = new Vector3fImpl();

	private final Vector3fImpl m_viewDir = new Vector3fImpl();

	private int m_viewportHeight;

	private int m_viewportWidth;

	/**
	 * Creates and initializes a first person camera.
	 */
	public RestrictedFirstPersonCamera() {
		reset();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.camera.ICamera#addCameraListener(org.eclipse.draw3d.camera.ICameraListener)
	 */
	public void addCameraListener(ICameraListener i_listener) {

		if (!m_listeners.contains(i_listener))
			m_listeners.add(i_listener);
	}

	private void fireCameraUpdated() {

		for (ICameraListener listener : m_listeners)
			listener.cameraChanged();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.camera.ICamera#getDistance(org.eclipse.draw3d.geometry.Vector3f)
	 */
	public float getDistance(Vector3f i_point) {

		if (i_point == null)
			throw new NullPointerException("i_point must not be null");

		Math3D.sub(i_point, m_position, TMP_V3);
		return TMP_V3.length();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.camera.ICamera#getPosition(Vector3f)
	 */
	public Vector3f getPosition(Vector3f io_result) {

		Vector3f result = io_result;
		if (result == null)
			result = new Vector3fImpl();

		result.set(m_position);
		return result;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.camera.ICamera#lookAt(org.eclipse.draw3d.geometry.IVector3f,
	 *      org.eclipse.draw3d.geometry.IVector3f)
	 */
	public void lookAt(IVector3f i_position, IVector3f i_upvector) {
		// TODO implement lookAt
		throw new UnsupportedOperationException("lookAt not implemeted yet");

	}

	/**
	 * Moves the camera by the given distances.
	 * 
	 * @param i_dX the distance on the X axis
	 * @param i_dY the distance on the Y axis
	 * @param i_dZ the distance on the Z axis
	 */
	public void moveBy(float i_dForward, float i_dStrafe, float i_dUp) {

		TMP_V3.set(m_viewDir);
		TMP_V3.scale(i_dForward);
		Math3D.add(m_position, TMP_V3, m_position);

		TMP_V3.set(m_right);
		TMP_V3.scale(i_dStrafe);
		Math3D.add(m_position, TMP_V3, m_position);

		TMP_V3.set(m_up);
		TMP_V3.scale(i_dUp);
		Math3D.add(m_position, TMP_V3, m_position);

		fireCameraUpdated();
	}

	public void moveTo(float i_x, float i_y, float i_z) {

		m_position.x = i_x;
		m_position.y = i_y;
		m_position.z = i_z;

		fireCameraUpdated();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.camera.ICamera#orbit(Vector3f, float, float)
	 */
	public void orbit(Vector3f i_center, float i_hAngle, float i_vAngle) {

		// don't allow absolute pitch > 90 degrees
		float currentPitch = HALF_PI - Math3D.angle(m_viewDir, UP_REF);
		boolean allowPitch = Math.abs(currentPitch + i_vAngle) < HALF_PI;

		TMP_M.setIdentity();
		if (i_hAngle != 0)
			Math3D.rotate(i_hAngle, UP_REF, TMP_M, TMP_M);

		if (i_vAngle != 0 && allowPitch)
			Math3D.rotate(i_vAngle, m_right, TMP_M, TMP_M);

		// camera position
		Math3D.sub(m_position, i_center, TMP_V3);
		TMP_V3.transform(TMP_M);
		Math3D.add(TMP_V3, i_center, m_position);

		rotate(0, i_vAngle, i_hAngle);
		fireCameraUpdated();
	}

	private void recalculateVectors() {
		Math3D.cross(m_viewDir, UP_REF, m_right);
		Math3D.normalise(m_right, m_right);

		Math3D.cross(m_right, m_viewDir, m_up);
		Math3D.normalise(m_up, m_up);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.camera.ICamera#removeCameraListener(org.eclipse.draw3d.camera.ICameraListener)
	 */
	public void removeCameraListener(ICameraListener i_listener) {

		m_listeners.remove(i_listener);
	}

	public void render() {

		Graphics3D g3d = RenderContext.getContext().getGraphics3D();
		g3d.glViewport(0, 0, m_viewportWidth, m_viewportHeight);

		g3d.glMatrixMode(Graphics3DDraw.GL_PROJECTION);
		g3d.glLoadIdentity();

		float aspect = (float) m_viewportWidth / (float) m_viewportHeight;
		g3d.gluPerspective(45, aspect, 100, 10000);

		g3d.glMatrixMode(Graphics3DDraw.GL_MODELVIEW);
		g3d.glLoadIdentity();

		float viewX = m_position.x + m_viewDir.x;
		float viewY = m_position.y + m_viewDir.y;
		float viewZ = m_position.z + m_viewDir.z;

		g3d.gluLookAt(m_position.x, m_position.y, m_position.z, viewX, viewY,
				viewZ, m_up.x, m_up.y, m_up.z);
	}

	public void reset() {

		m_position.set(0, 0, -1000);

		m_viewDir.set(0, 0, 1);
		recalculateVectors();

		fireCameraUpdated();
	}

	public void rotate(float i_roll, float i_pitch, float i_yaw) {

		TMP_M.setIdentity();

		// don't allow absolute pitch > 90 degrees
		float currentPitch = HALF_PI - Math3D.angle(m_viewDir, UP_REF);
		boolean allowPitch = Math.abs(currentPitch + i_pitch) < HALF_PI;

		if (i_pitch != 0 && allowPitch)
			Math3D.rotate(i_pitch, m_right, TMP_M, TMP_M);

		if (i_yaw != 0)
			Math3D.rotate(i_yaw, UP_REF, TMP_M, TMP_M);

		m_viewDir.transform(TMP_M);
		recalculateVectors();

		fireCameraUpdated();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.camera.ICamera#setViewport(int, int)
	 */
	public void setViewport(int i_width, int i_height) {

		m_viewportWidth = i_width;
		m_viewportHeight = i_height;

		fireCameraUpdated();
	}
}
