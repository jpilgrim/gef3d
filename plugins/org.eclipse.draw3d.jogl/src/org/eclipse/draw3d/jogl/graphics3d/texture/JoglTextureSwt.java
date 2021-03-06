/*******************************************************************************
 * Copyright (c) 2009-2010 Jens von Pilgrim and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Kristian Duske   - initial API and implementation
 *    Jens von Pilgrim - initial API and implementation
 *    Madhu Samuel	   - Implementation
 ******************************************************************************/
package org.eclipse.draw3d.jogl.graphics3d.texture;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import javax.media.opengl.GL;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw3d.util.Draw3DCache;
import org.eclipse.draw3d.util.converter.BufferInfo;
import org.eclipse.draw3d.util.converter.ImageConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;

/**
 * A texture that uses SWT to draw the image in main memory and then uploads it
 * to a texture object in video memory.
 * 
 * @author Madhu Samuel
 * @version $Revision$
 * @since 27.08.2009
 */
public class JoglTextureSwt extends AbstractJoglTexture {

	private int m_clearAlpha;

	private GC m_gc;

	private int m_glTexture = 0;

	protected SWTGraphics m_graphics;

	private Image m_image;

	private BufferInfo m_info;

	private ByteBuffer m_textureBuffer;

	private final Color m_transparentColor =
		new Color(Display.getCurrent(), 0x99, 0x99, 0x99);

	private boolean m_uploadTexture;

	/**
	 * Creates a new texture with the given dimensions and alpha value.
	 * 
	 * @param i_width the width of the texture
	 * @param i_height the height of the texture
	 */
	public JoglTextureSwt(int i_width, int i_height, GL i_gl) {
		if(i_gl==null) 
			throw new NullPointerException("gl must not be null");
		this.m_gl = i_gl;

		m_info =
			new BufferInfo(i_width, i_height, GL.GL_RGBA,
				GL.GL_UNSIGNED_BYTE, 1);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.graphics3d.lwjgl.texture.LwjglTexture#activate()
	 */
	public void activate() {

		if (m_disposed)
			throw new IllegalStateException("texture is disposed");

		if (!m_valid) {
			deleteGraphics();
			createGraphics();
			m_valid = true;
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.graphics3d.lwjgl.texture.LwjglTexture#clear(org.eclipse.swt.graphics.Color,
	 *      int)
	 */
	public void clear(Color i_color, int i_alpha) {

		if (i_alpha != m_clearAlpha) {
			m_clearAlpha = i_alpha;
			deleteGraphics();
			createGraphics();
		}

		m_graphics.pushState();
		try {
			m_graphics.setBackgroundColor(i_color);
			m_graphics.fillRectangle(0, 0, m_info.getWidth(), m_info
				.getHeight());
		} finally {
			m_graphics.popState();
		}
	}

	private void createGraphics() {

		int width = m_info.getWidth();
		int height = m_info.getHeight();
		int size = width * height;

		byte[] data = new byte[size * 3];

		PaletteData pal = new PaletteData(0xFF0000, 0x00FF00, 0x0000FF);
		ImageData imageData = new ImageData(width, height, 24, pal, 1, data);
		imageData.alphaData = new byte[size];
		Arrays.fill(imageData.alphaData, (byte) m_clearAlpha);

		m_image = new Image(Display.getCurrent(), imageData);

		m_gc = new GC(m_image);
		m_graphics = new SWTGraphics(m_gc);
		m_graphics.setAlpha(0xFF);
	}

	private void createTexture() {
		IntBuffer buffer = Draw3DCache.getIntBuffer(1);
		try {
			buffer.rewind();
			//FIXME gl.glGenTextures(buffer); is modified as below
			m_gl.glGenTextures(1, buffer);
			m_glTexture = buffer.get(0);

			m_gl.glBindTexture(GL.GL_TEXTURE_2D, m_glTexture);
			m_gl.glTexParameteri(GL.GL_TEXTURE_2D,
				GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
			m_gl.glTexParameteri(GL.GL_TEXTURE_2D,
				GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
			m_gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S,
				GL.GL_CLAMP);
			m_gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T,
				GL.GL_CLAMP);

			m_gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA, m_info
				.getWidth(), m_info.getHeight(), 0, GL.GL_RGBA,
				GL.GL_UNSIGNED_BYTE, m_textureBuffer);

			m_gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
		} finally {
			Draw3DCache.returnIntBuffer(buffer);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.graphics3d.lwjgl.texture.LwjglTexture#deactivate()
	 */
	public void deactivate() {

		if (m_disposed)
			throw new IllegalStateException("texture is disposed");

		m_uploadTexture = true;
	}

	private void deleteGraphics() {

		if (m_image != null)
			m_image.dispose();

		if (m_gc != null)
			m_gc.dispose();

		if (m_graphics != null)
			m_graphics.dispose();
	}

	private void deleteTexture() {

		if (m_glTexture > 0) {
			IntBuffer buffer = Draw3DCache.getIntBuffer(1);
			try {
				buffer.rewind();
				buffer.put(m_glTexture);
				//FIXME gl.glDeleteTextures(buffer); is modifed as below - madhu
				m_gl.glDeleteTextures(1,buffer);
			} finally {
				Draw3DCache.returnIntBuffer(buffer);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.graphics3d.lwjgl.texture.LwjglTexture#dispose()
	 */
	public void dispose() {

		if (m_disposed)
			return;

		deleteGraphics();
		deleteTexture();

		m_transparentColor.dispose();
		m_disposed = true;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.graphics3d.lwjgl.texture.LwjglTexture#getGraphics()
	 */
	public Graphics getGraphics() {

		if (m_disposed)
			throw new IllegalStateException("texture is disposed");

		if (!m_valid)
			throw new IllegalStateException("texture not valid");

		if (m_graphics == null)
			throw new IllegalStateException("texture not initialized");

		return m_graphics;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.graphics3d.lwjgl.texture.LwjglTexture#getTextureId()
	 */
	public int getTextureId() {

		if (m_disposed)
			throw new IllegalStateException("texture is disposed");

		if (m_uploadTexture) {

			ImageConverter converter = ImageConverter.getInstance();
			m_textureBuffer =
				converter.imageToBuffer(m_image, m_info, m_textureBuffer, true);

			deleteTexture();
			createTexture();

			m_uploadTexture = false;
		}

		return m_glTexture;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.graphics3d.lwjgl.texture.LwjglTexture#setDimensions(int,
	 *      int)
	 */
	public void setDimensions(int i_width, int i_height) {

		if (m_disposed)
			throw new IllegalStateException("texture is disposed");

		if (i_width <= 0 || i_height <= 0)
			throw new IllegalArgumentException(
				"texture dimensions must not be negative");

		if (m_info.getWidth() != i_width || m_info.getHeight() != i_height) {
			m_valid = false;
			m_info =
				new BufferInfo(i_width, i_height, m_info.getPixelFormat(),
					m_info.getDataType(), m_info.getAlignment());
		}
	}
}
