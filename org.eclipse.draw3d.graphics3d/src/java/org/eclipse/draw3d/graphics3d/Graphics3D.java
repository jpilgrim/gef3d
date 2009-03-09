/*******************************************************************************
 * Copyright (c) 2008 Jens von Pilgrim and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Matthias Thiele - initial API and implementation
 *    Kristian Duske - initial API
 *    Jens von Pilgrim - initial API
 ******************************************************************************/

package org.eclipse.draw3d.graphics3d;

import org.eclipse.draw2d.Graphics;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.opengl.GLCanvas;

/**
 * Graphics3D This interface summarizes all properties of a renderer
 * implementation. It extends the drawing interfaces which are thematically
 * split as well as provides access to renderer specific instances which are
 * hidden behind other interface types. A renderer implementation has only to
 * implement this interface and register at the Graphics3DRegistry to become
 * available
 * 
 * @author Matthias Thiele
 * @version $Revision$
 * @since 16.12.2008
 */
public interface Graphics3D extends Graphics3DDraw, Graphics3DUtil {
	
	
	/**
	 * Called by registry after creation of the instance if type is screen.
	 * @param canvas
	 */
	public void setGLCanvas(GLCanvas canvas);
	

	/**
	 * Creates an offscreen buffer for the current rendering implementation.
	 * 
	 * @param i_height The height of the buffer
	 * @param i_width The width of the buffer
	 * @param i_bufferConfig The configuration for the buffer
	 * @return A new offscreen buffer instance
	 */
	public Graphics3DOffscreenBuffers getGraphics3DOffscreenBuffer(
			int i_height, int i_width,
			Graphics3DOffscreenBufferConfig i_bufferConfig);

	/**
	 * Creates a configuration for an offscreen buffer. This object may be used
	 * either to create an offscreen buffer immediately or may be saved to
	 * create several offscreen buffers of the same type.
	 * 
	 * @param i_buffers Determines which buffer possibilities to use.
	 * @param i_args Arguments for the buffer
	 * @return The created offscreen buffer configuration instance.
	 */
	public Graphics3DOffscreenBufferConfig getGraphics3DOffscreenBufferConfig(
			int i_buffers, int... i_args);

	/**
	 * Activates a graphics object which will receive all following draw
	 * operations. If a graphics object was already created for the given key
	 * object (which can e.g. be a figure), it will be re-used.
	 * 
	 * @param i_key Object associated respective to associate with the activated
	 *            graphics objects.
	 * @param i_width Desired width of the graphics object.
	 * @param i_height Desired height of the graphics object.
	 * @param i_alpha Desired alpha value of the graphics object.
	 * @param i_color Desired background color of the graphics object.
	 * @return The activated graphics object.
	 */
	public Graphics activateGraphics2D(Object i_key, int i_width, int i_height,
			int i_alpha, Color i_color);

	/**
	 * Deactivates the active graphics object.
	 */
	public void deactivateGraphics2D();

	/**
	 * Gets an ID for the graphics object associated with the given key object.
	 * 
	 * @param i_key The key object.
	 * @return The ID of the graphics object associated with the key object.
	 * @throws IllegalArgumentException If there is no graphics object
	 *             associated with the key object yet.
	 */
	public int getGraphics2DId(Object i_key);

	/**
	 * Cleans up the ressources, instance is not usable afterwards any more.
	 */
	public void dispose();

	/**
	 * Returns whether there is a graphics object associate with the given key
	 * object.
	 * 
	 * @param i_key The key object.
	 * @return <code>true</code> if there was a graphics object associated to
	 *         the key obect previously or <code>false</code> otherwise
	 */
	public boolean hasGraphics2D(Object i_key);

}