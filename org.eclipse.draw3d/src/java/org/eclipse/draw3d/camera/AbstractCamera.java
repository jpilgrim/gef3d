/*******************************************************************************
 * Copyright (c) 2009 Jens von Pilgrim and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jens von Pilgrim - initial API and implementation
 *    Kristian Duske - refactoring and cleanup, support for multiple camera 
 *    	implementations
 ******************************************************************************/
package org.eclipse.draw3d.camera;

import java.util.List;
import java.util.Vector;

/**
 * This abstract camera is to be subclassed by concrete cameras.
 * 
 * @author Jens von Pilgrim
 * @author Kristian Duske
 * @version $Revision$
 * @since Apr 10, 2009
 */
public abstract class AbstractCamera implements ICamera {

	private int m_far = 40000;

	private final List<ICameraListener> m_listeners;

	private int m_near = 100;

	/**
	 * @param i_lightweightSystem3D
	 */
	public AbstractCamera() {
		m_listeners = new Vector<ICameraListener>(3);
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

	/**
	 * Notifies all camera listeners that this camera has changed.
	 */
	protected void fireCameraChanged() {
		for (ICameraListener listener : m_listeners)
			listener.cameraChanged();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.camera.ICamera#getFar()
	 */
	public int getFar() {

		return m_far;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.camera.ICamera#getNear()
	 */
	public int getNear() {

		return m_near;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.camera.ICamera#removeCameraListener(org.eclipse.draw3d.camera.ICameraListener)
	 */
	public void removeCameraListener(ICameraListener i_listener) {
		m_listeners.remove(i_listener);
	}

}
