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

package org.eclipse.draw3d.graphics3d.lwjgl.graphics;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw3d.geometry.IMatrix4f;
import org.eclipse.draw3d.geometry.Math3D;
import org.eclipse.draw3d.geometry.Matrix4f;
import org.eclipse.draw3d.geometry.Matrix4fImpl;
import org.eclipse.draw3d.geometry.Vector3fImpl;
import org.eclipse.draw3d.graphics3d.lwjgl.font.LwjglFont;
import org.eclipse.draw3d.graphics3d.lwjgl.font.LwjglFontManager;
import org.eclipse.draw3d.util.BufferUtils;
import org.eclipse.draw3d.util.ColorConverter;
import org.eclipse.draw3d.util.ImageConverter.ConversionSpecs;
import org.eclipse.draw3d.util.converter.BufferInfo;
import org.eclipse.draw3d.util.converter.ImageConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.LineAttributes;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.widgets.Display;
import org.lwjgl.opengl.GL11;

/**
 * Allows drawing to a texture object directly using OpenGL. The following
 * OpenGL state is expected:
 * <ul>
 * <li>{@link GL11#GL_TEXTURE_2D} enabled</li>
 * <li>{@link GL11#GL_BLEND} enabled</li>
 * <li>{@link GL11#glBlendFunc(int, int)} with arguments
 * {@link GL11#GL_SRC_ALPHA}, {@link GL11#ONE_MINUS_SRC_ALPHA}</li>
 * <li>GL shade model {@link GL11#GL_FLAT}</li>
 * <li>{@link GL11#GL_DEPTH_TEST} disabled</li>
 * <li>{@link GL11#GL_CULL_FACE} disabled</li>
 * <li>{@link GL11#GL_LINE_SMOOTH} enabled and {@link GL11#GL_LINE_SMOOTH_HINT}
 * set to {@link GL11#GL_NICEST}</li>
 * <li>viewport and projection matrix must be set up for orthographic 2D
 * projection like this:<br>
 * <code>gluOrtho2D(0, width, height, 0);<br>
 glViewport(0, 0, width, height);<br>glTranslatef(LwjglGraphics.RASTER_OFFSET, LwjglGraphics.RASTER_OFFSET, 0);</code>
 * .<br>
 * That last call is needed to achieve exact rasterization, if it is left out,
 * not only will rasterization be incorrect, but clipping planes will not work
 * either.</li>
 * </ul>
 * The following matrices and attribute groups may be modified by this class and
 * should be saved:
 * <ul>
 * <li>{@link GL11#GL_LIGHTING_BIT}</li>
 * <li>{@link GL11#GL_CURRENT_BIT}</li>
 * <li>{@link GL11#GL_TRANSFORM_BIT}</li>
 * <li>{@link GL11#GL_LINE_BIT}</li>
 * <li>{@link GL11#GL_POLYGON_BIT}</li>
 * <li>{@link GL11#GL_TEXTURE_BIT}</li>
 * </ul>
 * <h3>Limitations</h3> The following functions are not supported and will throw
 * an exception:
 * <ul>
 * <li>{@link Graphics#drawPath(org.eclipse.swt.graphics.Path)}</li>
 * <li>{@link Graphics#drawText(String, org.eclipse.draw2d.geometry.Point, int)}
 * </li>
 * <li>{@link Graphics#drawText(String, int, int, int)}</li>
 * <li>
 * {@link Graphics#drawTextLayout(org.eclipse.swt.graphics.TextLayout, int, int)}
 * </li>
 * <li>
 * {@link Graphics#drawTextLayout(org.eclipse.swt.graphics.TextLayout, int, int, int, int, Color, Color)}
 * </li>
 * <li>{@link Graphics#fillPath(org.eclipse.swt.graphics.Path)}</li>
 * <li>{@link Graphics#setClip(org.eclipse.swt.graphics.Path)}</li>
 * </ul>
 * The following functions are implemented, but will be ignored:
 * <ul>
 * <li>{@link Graphics#setAdvanced(boolean)} (GEF 3.5)</li>
 * <li>{@link Graphics#setAntialias(int)}</li>
 * <li>{@link Graphics#setBackgroundPattern(Pattern)}</li>
 * <li>{@link Graphics#setInterpolation(int)}</li>
 * <li>{@link Graphics#setFillRule(int)}</li>
 * <li>{@link Graphics#setLineCap(int)}</li>
 * <li>{@link Graphics#setLineJoin(int)}</li>
 * <li>{@link Graphics#setTextAntialias(int)}</li>
 * </ul>
 * Even if a setter is ignored, its corresponding getter will return the value
 * that was set before, with support for {@link #popState()} and
 * {@link #restoreState()}.
 * 
 * @author Kristian Duske
 * @version $Revision$
 * @since 08.03.2008
 * @todo revisit the method of calculating the number of segments to draw for an
 *       arc, since it will work for small arcs, but it will draw too many
 *       segments for long arcs, resulting in a performance penalty
 * @todo implement the unsupported methods
 */
public class LwjglGraphics extends Graphics {

	/**
	 * A graphics state that forwards requests for not set values to its parent
	 * state.
	 * 
	 * @author Kristian Duske
	 * @version $Revision$
	 * @since 08.03.2008
	 */
	private class GraphicsState {

		private Boolean m_advanced;

		private Integer m_alpha;

		private Integer m_antialias;

		private Color m_backgroundColor;

		private Pattern m_backgroundPattern;

		private Rectangle m_clip;

		private Integer m_fillRule;

		private Font m_font;

		private Color m_foregroundColor;

		private Pattern m_foregroundPattern;

		private Integer m_interpolation;

		private Integer m_lineCap;

		private int[] m_lineDash;

		private int m_lineDashLength;

		private Integer m_lineJoin;

		private Float m_lineMiterLimit;

		private Integer m_lineStyle;

		private Float m_lineWidth;

		private final GraphicsState m_parentState;

		private Integer m_textAntialias;

		private Matrix4fImpl m_transformation;

		private Boolean m_xorMode;

		/**
		 * Creates a new chained graphics state with the given parent.
		 * 
		 * @param i_parentState
		 *            the parent of this state
		 */
		public GraphicsState(GraphicsState i_parentState) {

			m_parentState = i_parentState;
		}

		/**
		 * Intersects the current clipping rectangle with the given rectangle.
		 * 
		 * @param i_clip
		 *            the rectangle to intersect with
		 */
		public void clipRect(Rectangle i_clip) {

			if (i_clip == null)
				return;

			Rectangle currentClip = getClip();
			if (currentClip != null) {
				if (m_clip == null) {
					m_clip = new Rectangle(currentClip);
				}
				m_clip.intersect(i_clip);
			} else {
				m_clip = new Rectangle(i_clip);
			}
		}

		/**
		 * Indicates whether the graphics system is in advanced graphics mode.
		 * 
		 * @return <code>true</code> if the graphics subsystem is in advanced
		 *         graphics mode and <code>false</code> otherwise
		 */
		public boolean getAdvanced() {

			if (m_advanced != null)
				return m_advanced;

			return m_parentState.getAdvanced();
		}

		/**
		 * Returns the alpha value of this graphics state.
		 * 
		 * @return the alpha value of this graphics state
		 */
		public int getAlpha() {

			if (m_alpha != null)
				return m_alpha;

			return m_parentState.getAlpha();
		}

		/**
		 * Returns the antialias value for this graphics state.
		 * 
		 * @return the antialias value for this graphics state.
		 */
		public int getAntialias() {

			if (m_antialias != null)
				return m_antialias;

			return m_parentState.getAntialias();
		}

		/**
		 * Returns the background color of this graphics state.
		 * 
		 * @return the background color
		 */
		public Color getBackgroundColor() {

			if (m_backgroundColor != null)
				return m_backgroundColor;

			return m_parentState.getBackgroundColor();
		}

		/**
		 * Returns the background pattern of this graphics state.
		 * 
		 * @return the background pattern
		 */
		public Pattern getBackgroundPattern() {

			if (m_backgroundPattern != null)
				return m_backgroundPattern;

			if (m_parentState != null)
				return m_parentState.getBackgroundPattern();

			return null;
		}

		/**
		 * Returns the clip rectangle of this state.
		 * 
		 * @return the clip rectangle
		 */
		public Rectangle getClip() {

			if (m_clip != null)
				return m_clip;

			return m_parentState.getClip();
		}

		/**
		 * Returns the fill rule of this graphics state.
		 * 
		 * @return the fill rule of this graphics state
		 */
		public int getFillRule() {

			if (m_fillRule != null)
				return m_fillRule;

			return m_parentState.getFillRule();
		}

		/**
		 * Returns the font of this graphics state.
		 * 
		 * @return the font
		 */
		public Font getFont() {

			if (m_font != null)
				return m_font;

			return m_parentState.getFont();
		}

		/**
		 * Returns the foreground color of this graphics state.
		 * 
		 * @return the foreground color
		 */
		public Color getForegroundColor() {

			if (m_foregroundColor != null)
				return m_foregroundColor;

			return m_parentState.getForegroundColor();
		}

		public Pattern getForegroundPattern() {

			if (m_foregroundPattern != null)
				return m_foregroundPattern;

			if (m_parentState != null)
				return m_parentState.getForegroundPattern();

			return null;
		}

		/**
		 * Returns the interpolation setting for this graphics state.
		 * 
		 * @return the interpolation setting
		 */
		public int getInterpolation() {

			if (m_interpolation != null)
				return m_interpolation;

			return m_parentState.getInterpolation();
		}

		/**
		 * Returns the line cap setting for this graphics state
		 * 
		 * @return the line cap setting
		 */
		public int getLineCap() {

			if (m_lineCap != null)
				return m_lineCap;

			return m_parentState.getLineCap();
		}

		/**
		 * Returns the custom line dash pattern.
		 * 
		 * @return the custom line dash pattern
		 */
		public int[] getLineDash() {

			if (m_lineDash != null)
				return m_lineDash;

			if (m_parentState != null)
				return m_parentState.getLineDash();

			return null;
		}

		/**
		 * Returns the total length of the line dash pattern. The total length
		 * of a line dash pattern is the sum of all the elements of the pattern
		 * array.
		 * 
		 * @return the total length of the line dash pattern
		 */
		public int getLineDashLength() {

			if (m_lineDash != null)
				return m_lineDashLength;

			return m_parentState.getLineDashLength();
		}

		public int getLineJoin() {

			if (m_lineJoin != null)
				return m_lineJoin;

			return m_parentState.getLineJoin();
		}

		/**
		 * Returns the line miter limit of this graphics state.
		 * 
		 * @return the line miter limit of this graphics state
		 */
		public float getLineMiterLimit() {

			if (m_lineMiterLimit != null)
				return m_lineMiterLimit;

			return m_parentState.getLineMiterLimit();
		}

		/**
		 * Returns the line style of this graphics state.
		 * 
		 * @return the line style
		 */
		public int getLineStyle() {

			if (m_lineStyle != null)
				return m_lineStyle;

			return m_parentState.getLineStyle();
		}

		/**
		 * Returns the line width of this graphics state.
		 * 
		 * @return the line width
		 */
		public float getLineWidth() {

			if (m_lineWidth != null)
				return m_lineWidth;

			return m_parentState.getLineWidth();
		}

		/**
		 * Returns the parent graphics state.
		 * 
		 * @return the parent graphics state
		 */
		public GraphicsState getParent() {

			return m_parentState;
		}

		/**
		 * Returns the text antialias value of this graphics state.
		 * 
		 * @return the text antialias value
		 */
		public int getTextAntialias() {

			if (m_textAntialias != null)
				return m_textAntialias;

			return m_parentState.getTextAntialias();
		}

		/**
		 * Returns the transformation matrix of this graphics state.
		 * 
		 * @return the transformation matrix
		 */
		public Matrix4f getTransformation() {

			return m_transformation;
		}

		/**
		 * Indicates whether XOR mode drawing is enabled.
		 * 
		 * @return <code>true</code> if XOR drawing mode is enabled or
		 *         <code>false</code> otherwise
		 */
		public boolean getXORMode() {

			if (m_xorMode != null)
				return m_xorMode;

			return m_parentState.getXORMode();
		}

		/**
		 * Rotates the coordinates system by the given angle counterclockwise.
		 * 
		 * @param i_degrees
		 *            the rotation angle in degrees
		 */
		public void rotate(float i_degrees) {

			if (m_transformation == null)
				m_transformation = new Matrix4fImpl();

			// don't use Math3d.rotate() here because when rotating about the z
			// axis, we can use a shorthand:
			double radians = Math.toRadians(i_degrees);
			float sin = (float) Math.sin(radians);
			float cos = (float) Math.cos(radians);

			TMP_M4.setIdentity();
			TMP_M4.a11 = cos;
			TMP_M4.a21 = sin;
			TMP_M4.a22 = cos;
			TMP_M4.a12 = -sin;

			Math3D.mul(TMP_M4, m_transformation, m_transformation);
		}

		/**
		 * Scales the current transformation and corrects the clipping rectangle
		 * by the given factors.
		 * 
		 * @param i_horizontal
		 *            the horizontal scaling factor
		 * @param i_vertical
		 *            the vertical scaling factor
		 */
		public void scale(float i_horizontal, float i_vertical) {

			if (m_transformation == null)
				m_transformation = new Matrix4fImpl();

			TMP_V3.set(i_horizontal, i_vertical, 0);

			Math3D.scale(TMP_V3, m_transformation, m_transformation);
		}

		/**
		 * Specifies whether the graphics system is in advanced graphics mode.
		 * 
		 * @param i_advanced
		 *            <code>true</code> if the graphics system is in advanced
		 *            graphics mode and <code>false</code> otherwise
		 */
		public void setAdvanced(boolean i_advanced) {

			m_advanced = i_advanced;
		}

		/**
		 * Sets the alpha value of this graphics state.
		 * 
		 * @param i_alpha
		 *            the alpha value
		 */
		public void setAlpha(int i_alpha) {

			m_alpha = i_alpha;
		}

		/**
		 * Sets the antialias value for this graphics state.
		 * 
		 * @param i_antialias
		 *            the antialias value
		 */
		public void setAntialias(int i_antialias) {

			m_antialias = i_antialias;
		}

		/**
		 * Sets the background color of this graphics state.
		 * 
		 * @param i_backgroundColor
		 *            the new background color
		 */
		public void setBackgroundColor(Color i_backgroundColor) {

			m_backgroundColor = i_backgroundColor;
		}

		/**
		 * Sets the background pattern of this graphics state.
		 * 
		 * @param i_backgroundPattern
		 *            the background pattern
		 */
		public void setBackgroundPattern(Pattern i_backgroundPattern) {

			m_backgroundPattern = i_backgroundPattern;
		}

		/**
		 * Sets the clip rectangleof this state.
		 * 
		 * @param i_clip
		 *            the clip rectangle
		 */
		public void setClip(Rectangle i_clip) {

			if (i_clip == null) {
				m_clip = null;
				return;
			}

			if (m_clip == null) {
				m_clip = new Rectangle(i_clip);
				return;
			}

			m_clip.setBounds(i_clip);
		}

		/**
		 * Sets the fill rule of this graphics state.
		 * 
		 * @param i_fillRule
		 *            the fill rule of this graphics state
		 */
		public void setFillRule(int i_fillRule) {

			m_fillRule = i_fillRule;
		}

		/**
		 * Sets the font of this graphics state.
		 * 
		 * @param i_font
		 *            the font
		 */
		public void setFont(Font i_font) {

			m_font = i_font;
		}

		/**
		 * Sets the foreground color of this graphics state.
		 * 
		 * @param i_foregroundColor
		 *            the foreground color
		 */
		public void setForegroundColor(Color i_foregroundColor) {

			m_foregroundColor = i_foregroundColor;
		}

		/**
		 * Sets the foreground pattern of this graphics state.
		 * 
		 * @param i_foregroundPattern
		 *            the foreground pattern
		 */
		public void setForegroundPattern(Pattern i_foregroundPattern) {

			m_foregroundPattern = i_foregroundPattern;
		}

		/**
		 * Sets the interpolation setting for this graphics state.
		 * 
		 * @param i_interpolation
		 *            the interpolation setting
		 */
		public void setInterpolation(int i_interpolation) {

			m_interpolation = i_interpolation;
		}

		/**
		 * Sets the line cap value of this graphics state.
		 * 
		 * @param i_lineCap
		 *            the line cap value
		 */
		public void setLineCap(int i_lineCap) {

			m_lineCap = i_lineCap;
		}

		/**
		 * Sets the custom line dash pattern.
		 * 
		 * @param i_lineDash
		 *            the custom line dash pattern
		 */
		public void setLineDash(int[] i_lineDash) {

			m_lineDash = i_lineDash;
			m_lineDashLength = 0;

			if (m_lineDash != null)
				for (int i = 0; i < m_lineDash.length; i++)
					m_lineDashLength += m_lineDash[i];
		}

		/**
		 * Sets the line join value of this graphics state.
		 * 
		 * @param i_lineJoin
		 *            the line join value
		 */
		public void setLineJoin(int i_lineJoin) {

			m_lineJoin = i_lineJoin;
		}

		/**
		 * Sets the line miter limit of this graphics state.
		 * 
		 * @param i_lineMiterLimit
		 *            the new line miter limit
		 */
		public void setLineMiterLimit(float i_lineMiterLimit) {

			m_lineMiterLimit = i_lineMiterLimit;
		}

		/**
		 * Sets the line style of this graphics state.
		 * 
		 * @param i_lineStyle
		 *            the line style
		 */
		public void setLineStyle(int i_lineStyle) {

			m_lineStyle = i_lineStyle;
		}

		/**
		 * Sets the line width of this graphics state.
		 * 
		 * @param i_lineWidth
		 *            the line width
		 */
		public void setLineWidth(float i_lineWidth) {

			m_lineWidth = i_lineWidth;
		}

		/**
		 * Sets the text antialias value of this graphics state.
		 * 
		 * @param i_textAntialias
		 *            the text antialias value
		 */
		public void setTextAntialias(int i_textAntialias) {

			m_textAntialias = i_textAntialias;
		}

		/**
		 * Sets the current transformation.
		 * 
		 * @param i_transformation
		 *            the current transformation
		 */
		public void setTransformation(IMatrix4f i_transformation) {

			if (m_transformation == null)
				m_transformation = new Matrix4fImpl(i_transformation);
			else
				m_transformation.set(i_transformation);
		}

		/**
		 * Specifies whether XOR mode drawing is enabled.
		 * 
		 * @param i_xorMode
		 *            <code>true</code> if XOR mode should be enabled or
		 *            <code>false</code> otherwise
		 */
		public void setXORMode(boolean i_xorMode) {

			m_xorMode = i_xorMode;
		}

		/**
		 * Shears the current transformation and corrects the clipping rectangle
		 * by the given amounts.
		 * 
		 * @param i_horz
		 *            the horizontal shearing amount
		 * @param i_vert
		 *            the vertical shearing amount
		 */
		public void shear(float i_horz, float i_vert) {

			if (m_transformation == null)
				m_transformation = new Matrix4fImpl();

			TMP_M4.setIdentity();
			TMP_M4.a21 = i_horz;
			TMP_M4.a12 = i_vert;

			Math3D.mul(TMP_M4, m_transformation, m_transformation);
		}

		/**
		 * Translates the current transformation and corrects the clipping
		 * rectangle by the given amounts.
		 * 
		 * @param i_dX
		 *            the X translation
		 * @param i_dY
		 *            the Y translation
		 */
		public void translate(float i_dX, float i_dY) {

			if (m_transformation == null)
				m_transformation = new Matrix4fImpl();

			TMP_V3.set(i_dX, i_dY, 0);
			Math3D.translate(TMP_V3, m_transformation, m_transformation);
		}
	}

	private enum LastColor {

		/**
		 * The last OpenGL color set by this graphics was the background color.
		 */
		BACKGROUND,

		/**
		 * The last OpenGL color set by this graphics was the foreground color.
		 */
		FOREGROUND,

		/**
		 * The last OpenGL color set by this graphics is unknown.
		 */
		UNKNOWN
	}

	private static final double ARC_LENGTH_FACTOR = 4 * Math.PI * Math.PI;

	/**
	 * Decrease this factor to get a larger number of segments and vice versa.
	 */
	private static final double ARC_SEGMENTS_FACTOR = 5 * Math.PI;

	private static final FloatBuffer BUF_F16 = BufferUtils
			.createFloatBuffer(16);

	/**
	 * This value is used as the base for clipping plane equations.
	 */
	private static final double CLIP_BASE = 1000000d;

	private static final DoubleBuffer DBL_BUF = BufferUtils
			.createDoubleBuffer(4);

	private static final int LINE_STYLES = SWT.LINE_SOLID | SWT.LINE_DASH
			| SWT.LINE_DOT | SWT.LINE_DASHDOT | SWT.LINE_DASHDOTDOT;

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(LwjglGraphics.class
			.getName());

	/**
	 * Offset value needed for correct rasterization.
	 */
	public static final float RASTER_OFFSET = 0.375f;

	private static final float[] TMP_F4 = new float[4];

	private static final Matrix4fImpl TMP_M4 = new Matrix4fImpl();

	private static final Vector3fImpl TMP_V3 = new Vector3fImpl();

	private boolean m_clippingEnabled = true;

	private Rectangle m_currentClip;

	private LwjglLinePattern m_currentLinePattern;

	private GraphicsState m_defaultState;

	private boolean m_disposed = false;

	private boolean m_disposeFonts = false;

	private LwjglFontManager m_fontManager;

	private int m_height;

	private LastColor m_lastColor = LastColor.UNKNOWN;

	private Map<Object, LwjglLinePattern> m_linePatterns = new HashMap<Object, LwjglLinePattern>();

	private GraphicsState m_state;

	private int m_width;

	/**
	 * Creates a new OpenGL graphics object with the given width and height;
	 * 
	 * @param i_width
	 *            the width of this graphics object
	 * @param i_height
	 *            the height of this graphics object
	 * @param i_fontManager
	 *            to the font manager to use
	 */
	public LwjglGraphics(int i_width, int i_height,
			LwjglFontManager i_fontManager) {

		setDimensions(i_width, i_height);
		initDefaultGraphicsState();

		glSetClip();
		glSetLineWidth();
		glSetLineStyle();
		glSetXORMode();

		setFontManager(i_fontManager);
	}

	private void checkDisposed() {

		if (m_disposed)
			throw new IllegalStateException("graphics is disposed");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#clipRect(org.eclipse.draw2d.geometry.Rectangle)
	 */
	@Override
	public void clipRect(Rectangle i_clipRect) {

		checkDisposed();

		m_state.clipRect(i_clipRect);
		glSetClip();
	}

	/**
	 * Disables clipping.
	 */
	public void disableClipping() {

		if (m_clippingEnabled) {
			GL11.glDisable(GL11.GL_CLIP_PLANE0);
			GL11.glDisable(GL11.GL_CLIP_PLANE1);
			GL11.glDisable(GL11.GL_CLIP_PLANE2);
			GL11.glDisable(GL11.GL_CLIP_PLANE3);
			m_clippingEnabled = false;
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#dispose()
	 */
	@Override
	public void dispose() {

		checkDisposed();

		if (m_disposeFonts)
			m_fontManager.dispose();

		for (LwjglLinePattern linePattern : m_linePatterns.values())
			linePattern.dispose();

		m_fontManager = null;
		m_linePatterns = null;

		m_disposed = true;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#drawArc(int, int, int, int, int, int)
	 */
	@Override
	public void drawArc(int i_x, int i_y, int i_w, int i_h, int i_offset,
			int i_length) {

		checkDisposed();

		glSetForegroundColor();

		if (m_state.getLineStyle() == SWT.LINE_CUSTOM) {
			m_currentLinePattern.activate();
			try {
				GL11.glBegin(GL11.GL_LINE_STRIP);
				glDrawTexturedArc(i_x, i_y, i_w, i_h, i_offset, i_length);
				GL11.glEnd();
			} finally {
				m_currentLinePattern.deactivate();
			}
		} else {
			GL11.glBegin(GL11.GL_LINE_STRIP);
			glDrawArc(i_x, i_y, i_w, i_h, i_offset, i_length);
			GL11.glEnd();
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#drawFocus(int, int, int, int)
	 */
	@Override
	public void drawFocus(int i_x, int i_y, int i_w, int i_h) {

		checkDisposed();

		pushState();
		try {
			Device dev = Display.getCurrent();
			Color c = dev.getSystemColor(SWT.COLOR_LIST_SELECTION);
			setForegroundColor(c);
			setLineStyle(SWT.LINE_SOLID);
			setLineWidth(2);
			drawRectangle(i_x, i_y, i_w, i_h);
		} finally {
			popState();
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#drawImage(org.eclipse.swt.graphics.Image,
	 *      int, int)
	 */
	@Override
	public void drawImage(Image i_srcImage, int i_x, int i_y) {

		checkDisposed();

		org.eclipse.swt.graphics.Rectangle bounds = i_srcImage.getBounds();
		int x1 = bounds.x;
		int y1 = bounds.y;
		int x2 = i_x;
		int y2 = i_y;
		int w = bounds.width;
		int h = bounds.height;

		drawImage(i_srcImage, x1, y1, w, h, x2, y2, w, h);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#drawImage(org.eclipse.swt.graphics.Image,
	 *      int, int, int, int, int, int, int, int)
	 */
	@Override
	public void drawImage(Image i_srcImage, int i_x1, int i_y1, int i_w1,
			int i_h1, int i_x2, int i_y2, int i_w2, int i_h2) {

		checkDisposed();

		ConversionSpecs specs = new ConversionSpecs();
		specs.foregroundAlpha = 255;
		specs.textureWidth = i_w1;
		specs.textureHeight = i_h1;
		specs.clip = new org.eclipse.swt.graphics.Rectangle(i_x1, i_y1, i_w1,
				i_h1);

		BufferInfo info = new BufferInfo(m_width, m_height, GL11.GL_RGBA,
				GL11.GL_UNSIGNED_BYTE, 1);

		ImageConverter converter = ImageConverter.getInstance();
		ByteBuffer buffer = converter.imageToBuffer(i_srcImage, info, null,
				false);

		IntBuffer nameBuffer = IntBuffer.allocate(1);
		GL11.glGenTextures(nameBuffer);
		int textureId = nameBuffer.get(0);
		try {

			GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, i_w1, i_h1,
					0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

			GL11.glPushAttrib(GL11.GL_TEXTURE_BIT);
			try {
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
						GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
						GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
						GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
				GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE,
						GL11.GL_REPLACE);

				GL11.glBegin(GL11.GL_QUADS);
				GL11.glTexCoord2f(0, 0);
				GL11.glVertex2i(i_x2, i_y2);

				GL11.glTexCoord2f(1, 0);
				GL11.glVertex2i(i_x2 + i_w2, i_y2);

				GL11.glTexCoord2f(1, 1);
				GL11.glVertex2i(i_x2 + i_w2, i_y2 + i_h2);

				GL11.glTexCoord2f(0, 1);
				GL11.glVertex2i(i_x2, i_y2 + i_h2);
				GL11.glEnd();
			} finally {
				GL11.glPopAttrib();
			}
		} finally {
			GL11.glDeleteTextures(nameBuffer);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#drawLine(int, int, int, int)
	 */
	@Override
	public void drawLine(int i_x1, int i_y1, int i_x2, int i_y2) {

		checkDisposed();

		glSetForegroundColor();

		if (m_state.getLineStyle() == SWT.LINE_CUSTOM) {
			m_currentLinePattern.activate();
			try {
				double s = m_currentLinePattern.getS(i_x1, i_y1, i_x2, i_y2);

				GL11.glBegin(GL11.GL_LINES);
				GL11.glTexCoord1f(0);
				GL11.glVertex2i(i_x1, i_y1);
				GL11.glTexCoord1d(s);
				GL11.glVertex2i(i_x2, i_y2);
				GL11.glEnd();
			} finally {
				m_currentLinePattern.deactivate();
			}
		} else {
			GL11.glBegin(GL11.GL_LINES);
			GL11.glVertex2i(i_x1, i_y1);
			GL11.glVertex2i(i_x2, i_y2);
			GL11.glEnd();
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#drawOval(int, int, int, int)
	 */
	@Override
	public void drawOval(int i_x, int i_y, int i_w, int i_h) {

		drawArc(i_x, i_y, i_w, i_h, 0, 360);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#drawPolygon(org.eclipse.draw2d.geometry.PointList)
	 */
	@Override
	public void drawPolygon(PointList i_points) {

		checkDisposed();

		glSetForegroundColor();
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);

		if (m_state.getLineStyle() == SWT.LINE_CUSTOM) {
			m_currentLinePattern.activate();
			try {
				GL11.glBegin(GL11.GL_LINE_LOOP);
				glDrawTexturedPointList(i_points);
				GL11.glEnd();
			} finally {
				m_currentLinePattern.deactivate();
			}
		} else {
			GL11.glBegin(GL11.GL_LINE_LOOP);
			glDrawPointList(i_points);
			GL11.glEnd();
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#drawPolyline(org.eclipse.draw2d.geometry.PointList)
	 */
	@Override
	public void drawPolyline(PointList i_points) {

		checkDisposed();

		if (i_points.size() < 2)
			return;

		glSetForegroundColor();

		if (m_state.getLineStyle() == SWT.LINE_CUSTOM) {
			m_currentLinePattern.activate();
			try {
				GL11.glBegin(GL11.GL_LINE_STRIP);
				glDrawTexturedPointList(i_points);
				GL11.glEnd();
			} finally {
				m_currentLinePattern.deactivate();
			}
		} else {
			GL11.glBegin(GL11.GL_LINE_STRIP);
			glDrawPointList(i_points);
			GL11.glEnd();
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#drawRectangle(int, int, int, int)
	 */
	@Override
	public void drawRectangle(int i_x, int i_y, int i_width, int i_height) {

		checkDisposed();

		glSetForegroundColor();
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);

		if (m_state.getLineStyle() == SWT.LINE_CUSTOM) {
			m_currentLinePattern.activate();
			try {
				GL11.glBegin(GL11.GL_LINE_LOOP);
				glDrawTexturedRectangle(i_x, i_y, i_width, i_height);
				GL11.glEnd();
			} finally {
				m_currentLinePattern.deactivate();
			}
		} else {
			GL11.glBegin(GL11.GL_LINE_LOOP);
			glDrawRectangle(i_x, i_y, i_width, i_height);
			GL11.glEnd();
		}

		/*-
		 * // draw the final pixel
		 * // TODO: handle this properly when the line is textured
		 * Point lastPoint = Point.SINGLETON;
		 * i_points.getPoint(lastPoint, i_points.size() - 1);
		 * glBegin(GL_POINTS);
		 * glVertex2i(lastPoint.x, lastPoint.y);
		 * glEnd();
		 */
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#drawRoundRectangle(Rectangle, int, int)
	 */
	@Override
	public void drawRoundRectangle(Rectangle i_r, int i_arcWidth,
			int i_arcHeight) {

		checkDisposed();

		glSetForegroundColor();
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);

		if (m_state.getLineStyle() == SWT.LINE_CUSTOM) {
			m_currentLinePattern.activate();
			try {
				GL11.glBegin(GL11.GL_LINE_LOOP);
				glDrawTexturedRoundRectangle(i_r, i_arcWidth, i_arcHeight);
				GL11.glEnd();
			} finally {
				m_currentLinePattern.deactivate();
			}
		} else {
			GL11.glBegin(GL11.GL_LINE_LOOP);
			glDrawRoundRectangle(i_r, i_arcWidth, i_arcHeight);
			GL11.glEnd();
		}

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#drawString(java.lang.String, int, int)
	 */
	@Override
	public void drawString(String i_s, int i_x, int i_y) {

		checkDisposed();

		glSetForegroundColor();
		LwjglFont glFont = glGetFont();
		glFont.renderString(i_s, i_x, i_y, false);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#drawText(java.lang.String, int, int)
	 */
	@Override
	public void drawText(String i_s, int i_x, int i_y) {

		checkDisposed();

		glSetForegroundColor();
		LwjglFont glFont = glGetFont();
		glFont.renderString(i_s, i_x, i_y, true);
	}

	/**
	 * Enables clipping.
	 */
	public void enableClipping() {

		if (!m_clippingEnabled) {
			glSetClip();
			m_clippingEnabled = true;
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#fillArc(int, int, int, int, int, int)
	 */
	@Override
	public void fillArc(int i_x, int i_y, int i_w, int i_h, int i_offset,
			int i_length) {

		checkDisposed();

		glSetBackgroundColor();
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

		GL11.glBegin(GL11.GL_POLYGON);
		GL11.glVertex2f(i_x + (float) i_w / 2, i_y + (float) i_h / 2);
		glDrawArc(i_x, i_y, i_w, i_h, i_offset, i_length);
		GL11.glEnd();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#fillGradient(int, int, int, int,
	 *      boolean)
	 */
	@Override
	public void fillGradient(int i_x, int i_y, int i_w, int i_h,
			boolean i_vertical) {

		checkDisposed();

		throw new UnsupportedOperationException(
				"fillGradient(int, int, int, int, boolean)");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#fillOval(int, int, int, int)
	 */
	@Override
	public void fillOval(int i_x, int i_y, int i_w, int i_h) {

		fillArc(i_x, i_y, i_w, i_h, 0, 360);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#fillPolygon(org.eclipse.draw2d.geometry.PointList)
	 */
	@Override
	public void fillPolygon(PointList i_points) {

		checkDisposed();

		glSetBackgroundColor();
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

		GL11.glBegin(GL11.GL_POLYGON);
		glDrawPointList(i_points);
		GL11.glEnd();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#fillRectangle(int, int, int, int)
	 */
	@Override
	public void fillRectangle(int i_x, int i_y, int i_width, int i_height) {

		checkDisposed();

		glSetBackgroundColor();
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

		GL11.glBegin(GL11.GL_QUADS);
		glDrawRectangle(i_x, i_y, i_width, i_height);
		GL11.glEnd();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#fillRoundRectangle(org.eclipse.draw2d.geometry.Rectangle,
	 *      int, int)
	 */
	@Override
	public void fillRoundRectangle(Rectangle i_r, int i_arcWidth,
			int i_arcHeight) {

		checkDisposed();

		glSetBackgroundColor();
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);

		GL11.glBegin(GL11.GL_POLYGON);
		glDrawRoundRectangle(i_r, i_arcWidth, i_arcHeight);
		GL11.glEnd();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#fillString(java.lang.String, int, int)
	 */
	@Override
	public void fillString(String i_s, int i_x, int i_y) {

		checkDisposed();

		LwjglFont glFont = glGetFont();
		org.eclipse.swt.graphics.Point extent = glFont.getExtent(i_s, false);

		fillRectangle(i_x, i_y, extent.x, extent.y);
		drawString(i_s, i_x, i_y);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#fillText(java.lang.String, int, int)
	 */
	@Override
	public void fillText(String i_s, int i_x, int i_y) {

		checkDisposed();

		LwjglFont glFont = glGetFont();
		org.eclipse.swt.graphics.Point extent = glFont.getExtent(i_s, true);

		fillRectangle(i_x, i_y, extent.x, extent.y);
		drawText(i_s, i_x, i_y);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getAdvanced()
	 */
	@Override
	public boolean getAdvanced() {

		checkDisposed();

		return m_state.getAdvanced();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getAlpha()
	 */
	@Override
	public int getAlpha() {

		checkDisposed();

		return m_state.getAlpha();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getAntialias()
	 */
	@Override
	public int getAntialias() {

		checkDisposed();

		return m_state.getAntialias();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getBackgroundColor()
	 */
	@Override
	public Color getBackgroundColor() {

		checkDisposed();

		return m_state.getBackgroundColor();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getClip(org.eclipse.draw2d.geometry.Rectangle)
	 */
	@Override
	public Rectangle getClip(Rectangle i_rect) {

		checkDisposed();

		Rectangle clip = m_state.getClip();
		i_rect.setBounds(clip);

		return i_rect;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getFillRule()
	 */
	@Override
	public int getFillRule() {

		checkDisposed();

		return m_state.getFillRule();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getFont()
	 */
	@Override
	public Font getFont() {

		checkDisposed();

		return m_state.getFont();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getFontMetrics()
	 */
	@Override
	public FontMetrics getFontMetrics() {

		checkDisposed();

		LwjglFont glFont = glGetFont();
		return glFont.getFontMetrics();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getForegroundColor()
	 */
	@Override
	public Color getForegroundColor() {

		checkDisposed();

		return m_state.getForegroundColor();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getInterpolation()
	 */
	@Override
	public int getInterpolation() {

		checkDisposed();

		return m_state.getInterpolation();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getLineAttributes()
	 */
	@Override
	public LineAttributes getLineAttributes() {

		checkDisposed();

		LineAttributes attrs = new LineAttributes(m_state.getLineWidth());
		attrs.cap = m_state.getLineCap();
		attrs.dashOffset = 0;
		attrs.join = m_state.getLineJoin();
		attrs.miterLimit = m_state.getLineMiterLimit();
		attrs.style = m_state.getLineStyle();

		int[] lineDash = m_state.getLineDash();
		if (lineDash == null)
			attrs.dash = null;
		else {
			attrs.dash = new float[lineDash.length];
			for (int i = 0; i < lineDash.length; i++)
				attrs.dash[i] = lineDash[i];
		}

		return attrs;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getLineCap()
	 */
	@Override
	public int getLineCap() {

		checkDisposed();

		return m_state.getLineCap();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getLineJoin()
	 */
	@Override
	public int getLineJoin() {

		checkDisposed();

		return m_state.getLineJoin();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getLineMiterLimit()
	 */
	@Override
	public float getLineMiterLimit() {

		checkDisposed();

		return m_state.getLineMiterLimit();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getLineStyle()
	 */
	@Override
	public int getLineStyle() {

		checkDisposed();

		return m_state.getLineStyle();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getLineWidth()
	 */
	@Override
	public int getLineWidth() {

		checkDisposed();

		return Math.round(m_state.getLineWidth());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getLineWidthFloat()
	 */
	@Override
	public float getLineWidthFloat() {

		checkDisposed();

		return m_state.getLineWidth();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getTextAntialias()
	 */
	@Override
	public int getTextAntialias() {

		checkDisposed();

		return m_state.getTextAntialias();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#getXORMode()
	 */
	@Override
	public boolean getXORMode() {

		checkDisposed();

		return m_state.getXORMode();
	}

	private void glDrawArc(int i_x, int i_y, int i_w, int i_h, int i_offset,
			int i_length) {

		double start = Math.toRadians(i_offset);
		double length = Math.toRadians(i_length);

		double xFactor = i_w / 2;
		double yFactor = i_h / 2;

		double avgRadius = (xFactor + yFactor) / 2;
		double arcLength = ARC_LENGTH_FACTOR * avgRadius / length;

		double inc = ARC_SEGMENTS_FACTOR / arcLength;

		double xOffset = i_x + i_w / 2;
		double yOffset = i_y + i_h / 2;

		for (double a = start; a < start + length; a += inc) {
			double x = xOffset + Math.cos(a) * xFactor;
			double y = yOffset - Math.sin(a) * yFactor;
			GL11.glVertex2d(x, y);
		}

		double x = xOffset + Math.cos(start + length) * xFactor;
		double y = yOffset - Math.sin(start + length) * yFactor;
		GL11.glVertex2d(x, y);
	}

	private void glDrawPointList(PointList i_points) {

		int[] vertices = i_points.toIntArray();
		for (int i = 0; i < vertices.length; i = i + 2) {
			int x = vertices[i];
			int y = vertices[i + 1];

			GL11.glVertex2i(x, y);
		}
	}

	private void glDrawRectangle(int i_x, int i_y, int i_width, int i_height) {

		int x1 = i_x;
		int y1 = i_y + i_height;
		int x2 = i_x + i_width;
		int y2 = i_y;

		GL11.glVertex2i(x1, y1);
		GL11.glVertex2i(x1, y2);
		GL11.glVertex2i(x2, y2);
		GL11.glVertex2i(x2, y1);
	}

	private void glDrawRoundRectangle(Rectangle i_r, int i_arcWidth,
			int i_arcHeight) {

		int x1 = i_r.x;
		int y1 = i_r.y;
		int x2 = x1 + i_r.width;
		int y2 = y1 + i_r.height;
		int w = i_arcWidth;
		int h = i_arcHeight;
		int w2 = w / 2;
		int h2 = h / 2;

		// left
		GL11.glVertex2i(x1, y1 + h2);
		GL11.glVertex2i(x1, y2 - h2);

		// bottom left corner
		glDrawArc(x1, y2 - h, w, h, 180, 90);

		// bottom
		GL11.glVertex2i(x1 + w2, y2);
		GL11.glVertex2i(x2 - w2, y2);

		// bottom right corner
		glDrawArc(x2 - w, y2 - h, w, h, 270, 90);

		// right
		GL11.glVertex2i(x2, y2 - h2);
		GL11.glVertex2i(x2, y1 + h2);

		// top right corner
		glDrawArc(x2 - w, y1, w, h, 0, 90);

		// top
		GL11.glVertex2i(x2 - w2, y1);
		GL11.glVertex2i(x1 + w2, y1);

		// top left corner
		glDrawArc(x1, y1, w, h, 90, 90);
	}

	private void glDrawTexturedArc(int i_x, int i_y, int i_w, int i_h,
			int i_offset, int i_length) {

		glDrawTexturedArc(i_x, i_y, i_w, i_h, i_offset, i_length, 0);
	}

	private double glDrawTexturedArc(int i_x, int i_y, int i_w, int i_h,
			int i_offset, int i_length, double i_s) {

		double start = Math.toRadians(i_offset);
		double length = Math.toRadians(i_length);

		double xFactor = i_w / 2;
		double yFactor = i_h / 2;

		double avgRadius = (xFactor + yFactor) / 2;
		double arcLength = ARC_LENGTH_FACTOR * avgRadius / length;

		double inc = ARC_SEGMENTS_FACTOR / arcLength;

		double xOffset = i_x + i_w / 2;
		double yOffset = i_y + i_h / 2;

		double lastX = xOffset + Math.cos(start) * xFactor;
		double lastY = yOffset - Math.sin(start) * yFactor;

		double s = i_s;
		GL11.glTexCoord1d(s);
		GL11.glVertex2d(lastX, lastY);

		for (double a = start + inc; a < start + length; a += inc) {
			double x = xOffset + Math.cos(a) * xFactor;
			double y = yOffset - Math.sin(a) * yFactor;
			s += m_currentLinePattern.getS(lastX, lastY, x, y);

			GL11.glTexCoord1d(s);
			GL11.glVertex2d(x, y);

			lastX = x;
			lastY = y;
		}

		double x = xOffset + Math.cos(start + length) * xFactor;
		double y = yOffset - Math.sin(start + length) * yFactor;
		s += m_currentLinePattern.getS(lastX, lastY, x, y);

		GL11.glTexCoord1d(s);
		GL11.glVertex2d(x, y);

		return s;
	}

	private void glDrawTexturedPointList(PointList i_points) {

		int[] vertices = i_points.toIntArray();

		if (vertices.length > 1) {
			int lastX = vertices[0];
			int lastY = vertices[1];

			double s = 0;

			GL11.glTexCoord1d(s);
			GL11.glVertex2i(lastX, lastY);

			for (int i = 2; i < vertices.length; i = i + 2) {
				int x = vertices[i];
				int y = vertices[i + 1];
				s += m_currentLinePattern.getS(lastX, lastY, x, y);

				GL11.glTexCoord1d(s);
				GL11.glVertex2i(x, y);

				lastX = x;
				lastY = y;
			}
		}
	}

	private void glDrawTexturedRectangle(int i_x, int i_y, int i_width,
			int i_height) {

		int x1 = i_x;
		int y1 = i_y + i_height;
		int x2 = i_x + i_width;
		int y2 = i_y;

		double s = 0;
		GL11.glTexCoord1d(s);
		GL11.glVertex2i(x1, y1);

		s += m_currentLinePattern.getS(x1, y1, x1, y2);
		GL11.glTexCoord1d(s);
		GL11.glVertex2i(x1, y2);

		s += m_currentLinePattern.getS(x1, y2, x2, y2);
		GL11.glTexCoord1d(s);
		GL11.glVertex2i(x2, y2);

		s += m_currentLinePattern.getS(x2, y2, x2, y1);
		GL11.glTexCoord1d(s);
		GL11.glVertex2i(x2, y1);
	}

	private void glDrawTexturedRoundRectangle(Rectangle i_r, int i_arcWidth,
			int i_arcHeight) {

		int x1 = i_r.x;
		int y1 = i_r.y;
		int x2 = x1 + i_r.width;
		int y2 = y1 + i_r.height;
		int w = i_arcWidth;
		int h = i_arcHeight;
		double w2 = (double) w / 2;
		double h2 = (double) h / 2;

		// left
		double s = 0;
		GL11.glTexCoord1d(s);
		GL11.glVertex2i(x1, y1 + h / 2);
		s += m_currentLinePattern.getS(x1, y1 + h2, x1, y2 - h2);
		GL11.glTexCoord1d(s);
		GL11.glVertex2i(x1, y2 - h / 2);

		// bottom left corner
		s = glDrawTexturedArc(x1, y2 - h, w, h, 180, 90, s);

		// bottom
		GL11.glVertex2d(x1 + w2, y2);
		s += m_currentLinePattern.getS(x1 + w2, y2, x2 - w2, y2);
		GL11.glTexCoord1d(s);
		GL11.glVertex2d(x2 - w2, y2);

		// bottom right corner
		s = glDrawTexturedArc(x2 - w, y2 - h, w, h, 270, 90, s);

		// right
		GL11.glVertex2d(x2, y2 - h2);
		s += m_currentLinePattern.getS(x2, y2 - h2, x2, y1 + h2);
		GL11.glTexCoord1d(s);
		GL11.glVertex2d(x2, y1 + h2);

		// top right corner
		s = glDrawTexturedArc(x2 - w, y1, w, h, 0, 90, s);

		// top
		GL11.glVertex2d(x2 - w2, y1);
		s += m_currentLinePattern.getS(x2 - w2, y1, x1 + w2, y1);
		GL11.glTexCoord1d(s);
		GL11.glVertex2d(x1 + w2, y1);

		// top left corner
		glDrawTexturedArc(x1, y1, w, h, 90, 90, s);
	}

	private LwjglFont glGetFont() {

		if (m_fontManager == null) {
			m_fontManager = new LwjglFontManager();
			m_disposeFonts = true;
		}

		Font font = m_state.getFont();
		return m_fontManager.getFont(font, (char) 32, (char) 127);
	}

	private void glRestoreState(GraphicsState i_state) {

		Matrix4f transformation = i_state.getTransformation();
		if (transformation != null) {

			Matrix4f inverse = Math3D.invert(transformation, TMP_M4);
			BUF_F16.rewind();
			inverse.toBufferRowMajor(BUF_F16);
			BUF_F16.rewind();

			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			GL11.glMultMatrix(BUF_F16);
		}

		if (i_state.getClip() == null) {
			if (m_state.getClip() != null)
				glSetClip();
		} else {
			if (!i_state.getClip().equals(m_state.getClip()))
				glSetClip();
		}

		if (i_state.getLineWidth() != m_state.getLineWidth())
			glSetLineWidth();

		if (i_state.getLineStyle() != m_state.getLineStyle()
				|| (i_state.getLineStyle() == SWT.LINE_CUSTOM
						&& m_state.getLineStyle() == SWT.LINE_CUSTOM && !Arrays
						.equals(i_state.getLineDash(), m_state.getLineDash())))
			glSetLineStyle();

		if (i_state.getXORMode() != m_state.getXORMode())
			glSetXORMode();
		
		if (i_state.getAntialias() != m_state.getAntialias())
			glSetAntialias();

		m_lastColor = LastColor.UNKNOWN;
	}

	private void glSetAntialias() {

		if (m_state.getAntialias() == SWT.ON) {
			GL11.glEnable(GL11.GL_LINE_SMOOTH);
			GL11.glEnable(GL11.GL_POINT_SMOOTH);
			GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
		} else {
			GL11.glDisable(GL11.GL_LINE_SMOOTH);
			GL11.glDisable(GL11.GL_POINT_SMOOTH);
			GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
		}
	}

	private void glSetBackgroundColor() {

		if (m_lastColor == LastColor.BACKGROUND)
			return;

		Color backgroundColor = m_state.getBackgroundColor();
		int alpha = m_state.getAlpha();

		ColorConverter.toFloatArray(backgroundColor, alpha, TMP_F4);
		GL11.glColor4f(TMP_F4[0], TMP_F4[1], TMP_F4[2], TMP_F4[3]);

		m_lastColor = LastColor.BACKGROUND;
	}

	private void glSetClip() {

		if (!m_clippingEnabled)
			return;

		Rectangle clip = m_state.getClip();
		if (!clip.equals(m_currentClip)) {
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
			GL11.glPushMatrix();
			try {
				// undo raster offset
				GL11.glTranslatef(-RASTER_OFFSET, -RASTER_OFFSET, 0);

				// left plane
				if (clip.x > 0) {
					double a = CLIP_BASE / clip.x;
					DBL_BUF.put(0, a);
					DBL_BUF.put(1, 0);
					DBL_BUF.put(2, 0);
					DBL_BUF.put(3, -CLIP_BASE);
					GL11.glEnable(GL11.GL_CLIP_PLANE0);
					GL11.glClipPlane(GL11.GL_CLIP_PLANE0, DBL_BUF);
				} else {
					GL11.glDisable(GL11.GL_CLIP_PLANE0);
				}

				// top plane
				if (clip.y > 0) {
					double b = CLIP_BASE / clip.y;
					DBL_BUF.put(0, 0);
					DBL_BUF.put(1, b);
					DBL_BUF.put(2, 0);
					DBL_BUF.put(3, -CLIP_BASE);
					GL11.glEnable(GL11.GL_CLIP_PLANE1);
					GL11.glClipPlane(GL11.GL_CLIP_PLANE1, DBL_BUF);
				} else {
					GL11.glDisable(GL11.GL_CLIP_PLANE1);
				}

				// right plane
				if (clip.x + clip.width < m_width - 1) {
					double a = -CLIP_BASE / (clip.x + clip.width);
					DBL_BUF.put(0, a);
					DBL_BUF.put(1, 0);
					DBL_BUF.put(2, 0);
					DBL_BUF.put(3, CLIP_BASE);
					GL11.glEnable(GL11.GL_CLIP_PLANE2);
					GL11.glClipPlane(GL11.GL_CLIP_PLANE2, DBL_BUF);
				} else {
					GL11.glDisable(GL11.GL_CLIP_PLANE2);
				}

				// bottom plane
				if (clip.y + clip.height < m_height - 1) {
					double b = -CLIP_BASE / (clip.y + clip.height);
					DBL_BUF.put(0, 0);
					DBL_BUF.put(1, b);
					DBL_BUF.put(2, 0);
					DBL_BUF.put(3, CLIP_BASE);
					GL11.glEnable(GL11.GL_CLIP_PLANE3);
					GL11.glClipPlane(GL11.GL_CLIP_PLANE3, DBL_BUF);
				} else {
					GL11.glDisable(GL11.GL_CLIP_PLANE3);
				}

				m_currentClip.setBounds(clip);
			} finally {
				GL11.glPopMatrix();
			}
		}
	}

	private void glSetForegroundColor() {

		if (m_lastColor == LastColor.FOREGROUND)
			return;

		Color foregroundColor = m_state.getForegroundColor();
		int alpha = m_state.getAlpha();

		ColorConverter.toFloatArray(foregroundColor, alpha, TMP_F4);
		GL11.glColor4f(TMP_F4[0], TMP_F4[1], TMP_F4[2], TMP_F4[3]);

		m_lastColor = LastColor.FOREGROUND;
	}

	private void glSetLineStyle() {

		int lineStyle = m_state.getLineStyle();
		switch (lineStyle) {
		case SWT.LINE_SOLID:
			m_currentLinePattern = null;
			GL11.glDisable(GL11.GL_LINE_STIPPLE);
			break;
		case SWT.LINE_DASH:
			m_currentLinePattern = null;
			GL11.glLineStipple(3, (short) 0x3F3F);
			GL11.glEnable(GL11.GL_LINE_STIPPLE);
			break;
		case SWT.LINE_DOT:
			m_currentLinePattern = null;
			GL11.glLineStipple(3, (short) 0x3333);
			GL11.glEnable(GL11.GL_LINE_STIPPLE);
			break;
		case SWT.LINE_DASHDOT:
			m_currentLinePattern = null;
			GL11.glLineStipple(3, (short) 0x6F6F);
			GL11.glEnable(GL11.GL_LINE_STIPPLE);
			break;
		case SWT.LINE_DASHDOTDOT:
			m_currentLinePattern = null;
			GL11.glLineStipple(3, (short) 0x5757);
			GL11.glEnable(GL11.GL_LINE_STIPPLE);
			break;
		case SWT.LINE_CUSTOM:
			GL11.glDisable(GL11.GL_LINE_STIPPLE);

			int[] dashPattern = m_state.getLineDash();
			Object key = LwjglLinePattern.getKey(dashPattern);
			m_currentLinePattern = m_linePatterns.get(key);
			if (m_currentLinePattern == null) {
				m_currentLinePattern = new LwjglLinePattern(dashPattern);
				m_linePatterns.put(key, m_currentLinePattern);
			}
			break;
		default:
			throw new IllegalArgumentException("unknown line style: "
					+ lineStyle);
		}
	}

	private void glSetLineWidth() {

		float lineWidth = m_state.getLineWidth();
		GL11.glLineWidth(lineWidth);
	}

	private void glSetXORMode() {

		if (m_state.getXORMode()) {
			GL11.glEnable(GL11.GL_COLOR_LOGIC_OP);
			GL11.glLogicOp(GL11.GL_XOR);
		} else {
			GL11.glDisable(GL11.GL_COLOR_LOGIC_OP);
		}
	}

	private void initDefaultGraphicsState() {

		Display display = Display.getDefault();

		Color foregroundColor = display.getSystemColor(SWT.COLOR_BLACK);
		Color backgroundColor = display.getSystemColor(SWT.COLOR_WHITE);

		m_defaultState = new GraphicsState(null);

		m_defaultState.setAdvanced(true);

		m_defaultState.setForegroundColor(foregroundColor);
		m_defaultState.setBackgroundColor(backgroundColor);
		m_defaultState.setAlpha(255);

		m_defaultState.setLineWidth(1);
		m_defaultState.setLineStyle(SWT.LINE_SOLID);
		m_defaultState.setLineCap(SWT.CAP_FLAT);
		m_defaultState.setLineJoin(SWT.JOIN_MITER);
		m_defaultState.setLineMiterLimit(11);

		m_defaultState.setFont(display.getSystemFont());

		m_currentClip = new Rectangle(0, 0, m_width, m_height);
		m_defaultState.setClip(m_currentClip);

		m_defaultState.setTransformation(Matrix4f.IDENTITY);
		m_defaultState.setXORMode(false);

		m_defaultState.setAntialias(SWT.ON);
		m_defaultState.setInterpolation(SWT.NONE);
		m_defaultState.setTextAntialias(SWT.ON);

		m_defaultState.setFillRule(SWT.FILL_WINDING);

		m_state = new GraphicsState(m_defaultState);
	}

	/**
	 * Indicates whether clipping is enabled.
	 * 
	 * @return <code>true</code> if clipping is enabled and <code>false</code>
	 *         otherwise
	 */
	public boolean isClippingEnabled() {

		return m_clippingEnabled;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#popState()
	 */
	@Override
	public void popState() {

		if (m_state.getParent() == m_defaultState)
			throw new EmptyStackException();

		GraphicsState currentState = m_state;
		m_state = m_state.getParent();
		glRestoreState(currentState);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#pushState()
	 */
	@Override
	public void pushState() {

		m_state = new GraphicsState(m_state);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#restoreState()
	 */
	@Override
	public void restoreState() {

		GraphicsState currentState = m_state;
		m_state = new GraphicsState(m_state.getParent());
		glRestoreState(currentState);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#rotate(float)
	 */
	@Override
	public void rotate(float i_degrees) {

		checkDisposed();

		m_state.rotate(i_degrees);
		GL11.glRotatef(i_degrees, 0, 0, 1);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#scale(double)
	 */
	@Override
	public void scale(double i_amount) {

		scale((float) i_amount, (float) i_amount);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#scale(float, float)
	 */
	@Override
	public void scale(float i_horizontal, float i_vertical) {

		checkDisposed();

		if (i_horizontal == 0 && i_vertical == 0)
			return;

		m_state.scale(i_horizontal, i_vertical);
		GL11.glScalef(i_horizontal, i_vertical, 0);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setAdvanced(boolean)
	 */
	@Override
	public void setAdvanced(boolean i_advanced) {

		checkDisposed();

		m_state.setAdvanced(i_advanced);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setAlpha(int)
	 */
	@Override
	public void setAlpha(int i_alpha) {

		checkDisposed();

		m_state.setAlpha(i_alpha);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setAntialias(int)
	 */
	@Override
	public void setAntialias(int i_antialias) {

		checkDisposed();

		m_state.setAntialias(i_antialias);
		glSetAntialias();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setBackgroundColor(org.eclipse.swt.graphics.Color)
	 */
	@Override
	public void setBackgroundColor(Color i_backgroundColor) {

		checkDisposed();

		m_state.setBackgroundColor(i_backgroundColor);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setBackgroundPattern(org.eclipse.swt.graphics.Pattern)
	 */
	@Override
	public void setBackgroundPattern(Pattern i_backgroundPattern) {

		checkDisposed();

		m_state.setBackgroundPattern(i_backgroundPattern);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setClip(org.eclipse.draw2d.geometry.Rectangle)
	 */
	@Override
	public void setClip(Rectangle i_clip) {

		checkDisposed();

		if (i_clip == null) {
			if (m_state.getClip() == null)
				return;
		} else {
			if (i_clip.equals(m_state.getClip()))
				return;
		}

		m_state.setClip(i_clip);
		glSetClip();
	}

	/**
	 * Sets the dimensions of this graphics object.
	 * 
	 * @param i_width
	 *            the new width
	 * @param i_height
	 *            the new height
	 */
	public void setDimensions(int i_width, int i_height) {

		checkDisposed();

		m_width = i_width;
		m_height = i_height;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setFillRule(int)
	 */
	@Override
	public void setFillRule(int i_fillRule) {

		checkDisposed();

		m_state.setFillRule(i_fillRule);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setFont(org.eclipse.swt.graphics.Font)
	 */
	@Override
	public void setFont(Font i_font) {

		checkDisposed();

		m_state.setFont(i_font);
	}

	/**
	 * Sets the font manager to be used by this graphics.
	 * 
	 * @param i_fontManager
	 *            the font manager
	 * @throws NullPointerException
	 *             if the given font manager is <code>null</code>
	 */
	public void setFontManager(LwjglFontManager i_fontManager) {

		if (i_fontManager == null)
			throw new NullPointerException("i_fontManager must not be null");

		if (m_fontManager != null && m_disposeFonts)
			m_fontManager.dispose();

		m_fontManager = i_fontManager;
		m_disposeFonts = false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setForegroundColor(org.eclipse.swt.graphics.Color)
	 */
	@Override
	public void setForegroundColor(Color i_foregroundColor) {

		checkDisposed();

		m_state.setForegroundColor(i_foregroundColor);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setInterpolation(int)
	 */
	@Override
	public void setInterpolation(int i_interpolation) {

		checkDisposed();

		m_state.setInterpolation(i_interpolation);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setLineAttributes(org.eclipse.swt.graphics.LineAttributes)
	 */
	@Override
	public void setLineAttributes(LineAttributes i_attributes) {

		checkDisposed();

		if (i_attributes == null)
			return;

		setLineCap(i_attributes.cap);
		setLineJoin(i_attributes.join);
		setLineMiterLimit(i_attributes.miterLimit);
		setLineStyle(i_attributes.style);
		setLineWidthFloat(i_attributes.width);
		setLineDash(i_attributes.dash);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setLineCap(int)
	 */
	@Override
	public void setLineCap(int i_cap) {

		checkDisposed();

		m_state.setLineCap(i_cap);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setLineDash(float[])
	 */
	@Override
	public void setLineDash(float[] i_dash) {

		checkDisposed();

		if (i_dash == null)
			setLineDash((int[]) null);
		else {
			int[] newDash = new int[i_dash.length];
			for (int i = 0; i < i_dash.length; i++)
				newDash[i] = (int) i_dash[i];
			setLineDash(newDash);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setLineDash(int[])
	 */
	@Override
	public void setLineDash(int[] i_dash) {

		if (i_dash != null) {
			if (m_state.getLineStyle() == SWT.LINE_CUSTOM
					&& Arrays.equals(i_dash, m_state.getLineDash()))
				return;

			m_state.setLineStyle(SWT.LINE_CUSTOM);
		} else {
			if (m_state.getLineStyle() != SWT.LINE_CUSTOM
					&& m_state.getLineDash() == null)
				return;

			m_state.setLineStyle(SWT.LINE_SOLID);
		}

		m_state.setLineDash(i_dash);
		glSetLineStyle();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setLineJoin(int)
	 */
	@Override
	public void setLineJoin(int i_lineJoin) {

		checkDisposed();

		m_state.setLineJoin(i_lineJoin);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setLineMiterLimit(float)
	 */
	@Override
	public void setLineMiterLimit(float i_lineMiterLimit) {

		checkDisposed();

		m_state.setLineMiterLimit(i_lineMiterLimit);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setLineStyle(int)
	 */
	@Override
	public void setLineStyle(int i_lineStyle) {

		checkDisposed();

		if ((i_lineStyle & LINE_STYLES) == 0)
			throw new IllegalArgumentException("unknown line style: "
					+ i_lineStyle);

		if (m_state.getLineStyle() == i_lineStyle)
			return;

		m_state.setLineStyle(i_lineStyle);
		glSetLineStyle();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setLineWidth(int)
	 */
	@Override
	public void setLineWidth(int i_lineWidth) {

		checkDisposed();

		if (m_state.getLineWidth() == i_lineWidth)
			return;

		m_state.setLineWidth(i_lineWidth);
		glSetLineWidth();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setLineWidthFloat(float)
	 */
	@Override
	public void setLineWidthFloat(float i_lineWidth) {

		checkDisposed();

		m_state.setLineWidth(i_lineWidth);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setTextAntialias(int)
	 */
	@Override
	public void setTextAntialias(int i_textAntialias) {

		checkDisposed();

		m_state.setTextAntialias(i_textAntialias);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#setXORMode(boolean)
	 */
	@Override
	public void setXORMode(boolean i_enabled) {

		checkDisposed();

		if (m_state.getXORMode() == i_enabled)
			return;

		m_state.setXORMode(i_enabled);
		glSetXORMode();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#shear(float, float)
	 */
	@Override
	public void shear(float i_horz, float i_vert) {

		checkDisposed();

		if (i_horz == 0 && i_vert == 0)
			return;

		m_state.shear(i_horz, i_vert);
		TMP_M4.setIdentity();
		TMP_M4.a21 = i_horz;
		TMP_M4.a12 = i_vert;

		BUF_F16.rewind();
		TMP_M4.toBufferRowMajor(BUF_F16);
		BUF_F16.rewind();

		GL11.glMultMatrix(BUF_F16);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#translate(float, float)
	 */
	@Override
	public void translate(float i_dx, float i_dy) {

		checkDisposed();

		if (i_dx == 0 && i_dy == 0)
			return;

		m_state.translate(i_dx, i_dy);
		GL11.glTranslatef(i_dx, i_dy, 0);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Graphics#translate(int, int)
	 */
	@Override
	public void translate(int i_dX, int i_dY) {

		translate((float) i_dX, (float) i_dY);
	}
}
