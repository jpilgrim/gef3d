/*******************************************************************************
 * Copyright (c) 2009 Jens von Pilgrim and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Kristian Duske - initial API and implementation
 ******************************************************************************/
package org.eclipse.draw3d.shapes;

import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.draw3d.DisplayListManager;
import org.eclipse.draw3d.RenderContext;
import org.eclipse.draw3d.geometry.IPosition3D;
import org.eclipse.draw3d.geometry.IVector3f;
import org.eclipse.draw3d.geometry.Math3D;
import org.eclipse.draw3d.geometry.Position3DImpl;
import org.eclipse.draw3d.geometry.Vector3f;
import org.eclipse.draw3d.geometry.Vector3fImpl;
import org.eclipse.draw3d.graphics3d.Graphics3D;
import org.eclipse.draw3d.graphics3d.Graphics3DDraw;
import org.eclipse.draw3d.picking.Query;
import org.eclipse.draw3d.util.ColorConverter;
import org.eclipse.draw3d.util.Draw3DCache;
import org.eclipse.swt.graphics.Color;

/**
 * A shape that renders a sphere.
 * 
 * @todo Reuse more vertices for the stripes - currently we create a lot of
 *       duplicates.
 * @todo Dreate entire stripes, not just a single wedge.
 * @todo Due to the approximate nature of the triangulation,
 *       {@link #getDistance(Query)} will only return exact results when the ray
 *       hits a vertex. It would be more precise to intersect with the
 *       individual triangles of the sphere for low precision values.
 * @author Kristian Duske
 * @version $Revision$
 * @since 05.08.2009
 */
public class SphereShape extends PositionableShape {

	/**
	 * A key to store a display list for a sphere in the display list manager.
	 * 
	 * @author Kristian Duske
	 * @version $Revision$
	 * @since 05.06.2009
	 */
	private class SphereKey {

		private int m_hashCode;

		/**
		 * Creates a new key with the given values.
		 * 
		 * @param i_precision the precision of the sphere
		 * @param i_outline <code>true</code> if this key is for the display
		 *            list that draws the outline and <code>false</code> if it
		 *            is for the display list that fills the cylinder
		 */
		public SphereKey(int i_precision, boolean i_outline) {

			m_hashCode = 17;
			m_hashCode = 37 * m_hashCode + new Integer(i_precision).hashCode();
			m_hashCode = 37 * m_hashCode + new Boolean(i_outline).hashCode();
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object i_obj) {

			if (i_obj == null)
				return false;

			if (this == i_obj)
				return true;

			if (!(i_obj instanceof SphereKey))
				return false;

			return m_hashCode == i_obj.hashCode();
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {

			return m_hashCode;
		}
	}

	private static final IVector3f CENTER = new Vector3fImpl(0.5f, 0.5f, 0.5f);

	private static final float RADIUS = 0.5f;

	private static final float RADIUS_SQUARED = RADIUS * RADIUS;

	/**
	 * Contains a matrix that rotates 90 degress about the Z axis.
	 */
	private static final IPosition3D ROTATE_Z90;

	/**
	 * Caches the stripes across multiple instances since they never change.
	 */
	private static final Map<Integer, SphereTriangle[][]> STRIPE_CACHE =
		new WeakHashMap<Integer, SphereTriangle[][]>();

	static {
		Position3DImpl pos = new Position3DImpl();
		pos.setSize3D(new Vector3fImpl(1, 1, 1));
		pos.setRotation3D(new Vector3fImpl(0, 0, (float) Math.PI / 2));

		ROTATE_Z90 = pos;
	}

	private boolean m_fill = true;

	private float[] m_fillColor = new float[] { 1, 1, 1, 1 };

	private SphereKey m_fillKey;

	private boolean m_outline = true;

	private float[] m_outlineColor = new float[] { 0, 0, 0, 1 };

	private SphereKey m_outlineKey;

	private int m_precision;

	private SphereTriangle[][] m_stripes;

	/**
	 * Creates a new sphere with the given precision.
	 * 
	 * @param i_position3D the position of the sphere
	 * @param i_precision the precision of the sphere
	 */
	public SphereShape(IPosition3D i_position3D, int i_precision) {

		super(i_position3D);

		m_stripes = STRIPE_CACHE.get(i_precision);

		if (m_stripes == null) {
			Vector3f a = new Vector3fImpl(CENTER);
			a.setX(a.getX() + RADIUS);

			Vector3f b = new Vector3fImpl(CENTER);
			b.setY(b.getY() + RADIUS);

			Vector3f c = new Vector3fImpl(CENTER);
			c.setZ(c.getZ() + RADIUS);

			Vector3f d = new Vector3fImpl(CENTER);
			d.setZ(d.getZ() - RADIUS);

			m_stripes = new SphereTriangle[2][];
			m_stripes[0] = new SphereTriangle[] { new SphereTriangle(a, b, c) };
			m_stripes[1] = new SphereTriangle[] { new SphereTriangle(a, b, d) };

			for (int i = 1; i <= i_precision; i++) {

				// initialize stripes arrays
				int numStripes = (2 << i); // 2^(p+1)
				SphereTriangle[][] newStripes =
					new SphereTriangle[numStripes][];
				for (int j = 0; j < numStripes / 2; j++) {
					int numTriangles = j * 2 + 1;
					newStripes[j] = new SphereTriangle[numTriangles];
					newStripes[numStripes - j - 1] =
						new SphereTriangle[numTriangles];
				}

				// divide stripes
				for (int j = 0; j < m_stripes.length; j++) {
					SphereTriangle[] stripe = m_stripes[j];
					SphereTriangle[] newUpper = newStripes[j * 2];
					SphereTriangle[] newLower = newStripes[j * 2 + 1];

					int upperIndex = 0;
					int lowerIndex = 0;

					for (int k = 0; k < stripe.length; k++) {
						SphereTriangle triangle = stripe[k];
						SphereTriangle[] subTriangles =
							triangle.divide(CENTER, RADIUS);

						// is the triangle standing on the segment a-b or on
						// the edge c?
						if (triangle.getC().getZ() > triangle.getA().getZ()) {
							newUpper[upperIndex++] = subTriangles[0];
							newLower[lowerIndex++] = subTriangles[1];
							newLower[lowerIndex++] = subTriangles[2];
							newLower[lowerIndex++] = subTriangles[3];
						} else {
							newLower[lowerIndex++] = subTriangles[0];
							newUpper[upperIndex++] = subTriangles[1];
							newUpper[upperIndex++] = subTriangles[2];
							newUpper[upperIndex++] = subTriangles[3];
						}
					}
				}

				m_stripes = newStripes;
			}

			STRIPE_CACHE.put(i_precision, m_stripes);
		}

		m_outlineKey = new SphereKey(m_precision, true);
		m_fillKey = new SphereKey(m_precision, false);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.shapes.PositionableShape#doRender(org.eclipse.draw3d.RenderContext)
	 */
	@Override
	protected void doRender(RenderContext i_renderContext) {

		Graphics3D g3d = i_renderContext.getGraphics3D();
		DisplayListManager displayListManager =
			i_renderContext.getDisplayListManager();

		initDisplayLists(displayListManager, g3d);

		if (m_fill) {
			g3d.glColor4f(m_fillColor[0], m_fillColor[1], m_fillColor[2],
				m_fillColor[3]);
			displayListManager.executeDisplayList(m_fillKey);
		}

		if (m_outline) {
			g3d.glColor4f(m_outlineColor[0], m_outlineColor[1],
				m_outlineColor[2], m_outlineColor[3]);
			displayListManager.executeDisplayList(m_outlineKey);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.shapes.PositionableShape#doGetDistance(org.eclipse.draw3d.picking.Query)
	 */
	@Override
	protected float doGetDistance(Query i_query) {

		// TODO: intersect with individual triangles for low precision

		// vector from ray origin to sphere center
		Vector3f roToC = Draw3DCache.getVector3f();
		// projection of roToC onto the ray
		Vector3f proj = Draw3DCache.getVector3f();
		// vector from origin + proj to sphere center
		Vector3f orth = Draw3DCache.getVector3f();
		try {
			IVector3f rayOrigin = i_query.getRayOrigin();
			IVector3f rayDirection = i_query.getRayDirection();

			// vector from ray origin to center
			Math3D.sub(CENTER, rayOrigin, roToC);

			// project that vector onto the ray direction
			float dot = Math3D.dot(roToC, rayDirection);
			Math3D.scale(dot, rayDirection, proj);

			Math3D.sub(roToC, proj, orth);

			// vector orth is now orthogonal to ray direction and points to
			// center, its length measures the minimum distance between ray and
			// center
			if (orth.lengthSquared() > RADIUS_SQUARED)
				return Float.NaN;

			// compute solutions of ray equation inserted into sphere equation
			float p = -2 * dot;
			float q = roToC.length() - RADIUS_SQUARED;

			float p2 = p / 2;
			float r = p2 * p2 - q; // radicant

			// this should not happen, but let's be cautious
			if (r < 0)
				return Float.NaN;

			if (r == 0)
				return -p2;

			float s = (float) Math.sqrt(r);
			float x1 = -p2 + s;
			float x2 = -p2 - s;

			return Math.min(x1, x2);
		} finally {
			Draw3DCache.returnVector3f(roToC, proj, orth);
		}
	}

	private void initDisplayLists(DisplayListManager i_manager,
		final Graphics3D i_graphics3D) {

		if (m_fill && !i_manager.isDisplayList(m_fillKey)) {
			i_manager.createDisplayList(m_fillKey, new Runnable() {
				public void run() {
					i_graphics3D.glPushMatrix();
					try {
						renderFill(i_graphics3D);
						for (int i = 0; i < 3; i++) {
							i_graphics3D.setPosition(ROTATE_Z90);
							renderFill(i_graphics3D);
						}
					} finally {
						i_graphics3D.glPopMatrix();
					}
				}
			});
		}

		if (m_outline && !i_manager.isDisplayList(m_outlineKey)) {
			i_manager.createDisplayList(m_outlineKey, new Runnable() {
				public void run() {
					i_graphics3D.glPushMatrix();
					try {
						renderOutline(i_graphics3D);
						for (int i = 0; i < 3; i++) {
							i_graphics3D.setPosition(ROTATE_Z90);
							renderOutline(i_graphics3D);
						}
					} finally {
						i_graphics3D.glPopMatrix();
					}
				}
			});
		}
	}

	private void renderFill(Graphics3D i_g3d) {

		i_g3d.glColor4f(0, 0, 1, 0.5f);
		i_g3d.glPolygonMode(Graphics3DDraw.GL_FRONT_AND_BACK,
			Graphics3DDraw.GL_FILL);

		for (int i = 0; i < m_stripes.length; i++) {
			SphereTriangle[] stripe = m_stripes[i];

			if (i < m_stripes.length / 2) {
				i_g3d.glBegin(Graphics3DDraw.GL_TRIANGLE_STRIP);
				for (int j = stripe.length - 1; j >= 0; j -= 2) {
					i_g3d.glVertex3f(stripe[j].getB());
					i_g3d.glVertex3f(stripe[j].getC());
				}
				i_g3d.glVertex3f(stripe[0].getA());
				i_g3d.glEnd();
			} else {
				i_g3d.glBegin(Graphics3DDraw.GL_TRIANGLE_STRIP);
				for (int j = 0; j < stripe.length; j += 2) {
					i_g3d.glVertex3f(stripe[j].getA());
					i_g3d.glVertex3f(stripe[j].getC());
				}
				i_g3d.glVertex3f(stripe[stripe.length - 1].getB());
				i_g3d.glEnd();
			}
		}
	}

	private void renderOutline(Graphics3D i_g3d) {

		i_g3d.glColor4f(1, 0, 0, 0.5f);

		for (int i = 0; i < m_stripes.length; i++) {
			SphereTriangle[] stripe = m_stripes[i];

			// equator and parallels
			if (i < m_stripes.length / 2) {
				i_g3d.glBegin(Graphics3DDraw.GL_LINE_STRIP);
				for (int j = 0; j < stripe.length; j += 2)
					i_g3d.glVertex3f(stripe[j].getA());
				i_g3d.glVertex3f(stripe[stripe.length - 1].getB());
				i_g3d.glEnd();
			} else {
				if (i < m_stripes.length - 1) {
					i_g3d.glBegin(Graphics3DDraw.GL_LINE_STRIP);
					for (int j = 1; j < stripe.length; j += 2)
						i_g3d.glVertex3f(stripe[j].getA());
					i_g3d.glVertex3f(stripe[stripe.length - 2].getB());
					i_g3d.glEnd();
				}
			}

			// zig-zag
			i_g3d.glBegin(Graphics3DDraw.GL_LINE_STRIP);
			i_g3d.glVertex3f(stripe[0].getA());
			for (int j = 0; j < stripe.length; j++)
				i_g3d.glVertex3f(stripe[j].getC());
			i_g3d.glEnd();
		}
	}

	/**
	 * Specifies whether the polygons should be filled.
	 * 
	 * @param i_fill <code>true</code> if the polygons should be filled and
	 *            <code>false</code> otherwise
	 */
	public void setFill(boolean i_fill) {

		m_fill = i_fill;
	}

	/**
	 * Sets the fill color of this cylinder.
	 * 
	 * @param i_color the fill color
	 * @param i_alpha the alpha value
	 */
	public void setFillColor(Color i_color, int i_alpha) {

		ColorConverter.toFloatArray(i_color, i_alpha, m_fillColor);
	}

	/**
	 * Specifies whether an outline should be drawn.
	 * 
	 * @param i_outline <code>true</code> if an outline should be drawn and
	 *            <code>false</code> otherwise
	 */
	public void setOutline(boolean i_outline) {

		m_outline = i_outline;
	}

	/**
	 * Sets the outline color of this cylinder.
	 * 
	 * @param i_color the outline color
	 * @param i_alpha the alpha value
	 */
	public void setOutlineColor(Color i_color, int i_alpha) {

		ColorConverter.toFloatArray(i_color, i_alpha, m_outlineColor);
	}

}
