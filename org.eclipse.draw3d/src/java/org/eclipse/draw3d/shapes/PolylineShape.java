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
package org.eclipse.draw3d.shapes;

import java.util.List;

import org.eclipse.draw3d.RenderContext;
import org.eclipse.draw3d.geometry.IVector3f;
import org.eclipse.draw3d.geometry.Math3D;
import org.eclipse.draw3d.geometry.Math3DCache;
import org.eclipse.draw3d.geometry.Position3D;
import org.eclipse.draw3d.geometry.Vector3f;
import org.eclipse.draw3d.geometry.Math3D.Side;
import org.eclipse.draw3d.geometryext.Plane;
import org.eclipse.draw3d.graphics3d.Graphics3D;
import org.eclipse.draw3d.graphics3d.Graphics3DDraw;
import org.eclipse.draw3d.picking.Query;
import org.eclipse.draw3d.util.ColorConverter;
import org.eclipse.swt.graphics.Color;

/**
 * A polyline shape can be used to render polylines.
 * 
 * @author Kristian Duske
 * @version $Revision$
 * @since 03.04.2008
 */
public class PolylineShape implements Shape {

	private static final float ACCURACY = 10f;

	private static final String KEY_HORIZONTAL_BORDER = "horizontal border";

	private static final String KEY_VERTICAL_BORDER = "vertical border";

	private static final String KEY_VISIBLE_BORDER = "visible border";

	private final float[] m_color = new float[] { 0, 0, 0, 1 };

	private List<IVector3f> m_points;

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.shapes.Shape#getDistance(org.eclipse.draw3d.picking.Query,
	 *      org.eclipse.draw3d.geometry.Position3D)
	 */
	public float getDistance(Query i_query, Position3D i_position) {

		if (m_points.size() < 2)
			return Float.NaN;

		/*
		 * To determine whether a polyline is hit by a ray, we implement the
		 * following method. First, we reduce the problem to line segments by
		 * checking individually the segments of the polyline for intersections
		 * with the ray. To reduce the number of checks, we divide the world
		 * space into five subspaces using three planes. One plane contains the
		 * origin of the picking ray and has the ray direction as its normal.
		 * This plane divides the world space into two subspaces we call "front"
		 * and "behind" for obvious reasons. Furthermore, we subdive the front
		 * and back spaces space "horizontally" and "vertically" using two
		 * planes that are orthogonal to each other and whose line of
		 * intersection is parallel to the ray direction and contains the ray
		 * origin. This is easy to image, just place yourself at the origin of
		 * the ray and look into its direction. The "horizontal" and "vertical"
		 * division planes would be visible as two orthogonal lines which
		 * intersect exactly at the point you are looking at. Be aware that from
		 * your point of view, neither of these planes is usually horizontal or
		 * vertical - we only use these words to differentiate the two planes.
		 * Now that we have subdivided space, we can formulate conditions for
		 * intersecting line segments. Firstly, a segment can only intersect
		 * with the ray if at least one of its vertices is the front space and
		 * if its vertices are contained in opposite sub spaces (for example,
		 * vertex 1 is in the upper left and vertex2 in the lower right
		 * subspace). After we have found such a candidate, we can easily
		 * calculate the point of intersection between the segment and the ray.
		 */

		Plane visBorder = getVisibleBorder(i_query);
		Plane hBorder = getHorizontalBorder(i_query);
		Plane vBorder = getVerticalBorder(i_query);

		IVector3f p1 = m_points.get(0);
		IVector3f p2;

		Side visSide1 = visBorder.getSide(p1);
		Side hSide1 = null, vSide1 = null, visSide2, hSide2, vSide2;

		for (int i = 1; i < m_points.size(); i++) {
			p2 = m_points.get(i);
			visSide2 = visBorder.getSide(p2);

			// is at least one point in the front subspace?
			if (visSide1 != visSide2 || visSide1 == Side.FRONT) {

				if (hSide1 == null)
					hSide1 = hBorder.getSide(p1);
				hSide2 = hBorder.getSide(p2);

				// are the points on different sides or on the horizontal plane?
				if (hSide1 != hSide2 || (hSide1 == null && hSide2 == null)) {

					if (vSide1 == null)
						vSide1 = vBorder.getSide(p1);
					vSide2 = vBorder.getSide(p2);

					// are the points on different sides or on the vertical
					// plane?
					if (vSide1 != vSide2 || (vSide1 == null && vSide2 == null)) {
						Vector3f intersection = Math3DCache.getVector3f();
						Vector3f tmp = Math3DCache.getVector3f();
						try {
							// the two points are in different quadrants or on a
							// plane, so we try and hit the segment
							Plane intersectingBorder = null;
							if ((hSide1 == null && hSide2 == null))
								intersectingBorder = vBorder;
							else
								intersectingBorder = hBorder;

							intersectingBorder.intersectionWithSegment(p1, p2,
								intersection);

							// intersection only if tmp is on the
							// picking ray
							IVector3f rayOrigin = i_query.getRayOrigin();
							IVector3f rayDirection = i_query.getRayDirection();

							Math3D.sub(intersection, rayOrigin, tmp);
							float fx = tmp.getX() / rayDirection.getX();
							float fy = tmp.getY() / rayDirection.getY();
							float fz = tmp.getZ() / rayDirection.getZ();

							if (!Math3D.equals(fx, fy, ACCURACY))
								return Float.NaN;

							if (!Math3D.equals(fx, fz, ACCURACY))
								return Float.NaN;

							if (!Math3D.equals(fy, fz, ACCURACY))
								return Float.NaN;

							return (fx + fy + fz) / 3f;
						} finally {
							Math3DCache.returnVector3f(intersection, tmp);
						}
					}

					// carry the "vertical" side of point 2 to point 1
					vSide1 = vSide2;
				} else {
					vSide1 = null;
				}

				// carry the "horizontal" side of point 2 to point 1
				hSide1 = hSide2;
			} else {
				hSide1 = null;
				vSide1 = null;
			}

			// carry the "visible" side of point 2 to point 1
			visSide1 = visSide2;
			p1 = p2;
		}

		return Float.NaN;
	}

	private Plane getHorizontalBorder(Query i_query) {

		Plane horizontalBorder = (Plane) i_query.get(KEY_HORIZONTAL_BORDER);
		if (horizontalBorder == null) {
			Vector3f normal = Math3DCache.getVector3f();
			try {
				IVector3f rayOrigin = i_query.getRayOrigin();
				IVector3f rayDirection = i_query.getRayDirection();

				Math3D.cross(rayOrigin, rayDirection, normal);
				Math3D.normalise(normal, normal);

				horizontalBorder = new Plane();
				horizontalBorder.set(rayOrigin, normal);

				i_query.set(KEY_HORIZONTAL_BORDER, horizontalBorder);
			} finally {
				Math3DCache.returnVector3f(normal);
			}
		}

		return horizontalBorder;
	}

	/**
	 * @return the points
	 */
	public List<IVector3f> getPoints() {
		return m_points;
	}

	private Plane getVerticalBorder(Query i_query) {

		Plane verticalBorder = (Plane) i_query.get(KEY_VERTICAL_BORDER);
		if (verticalBorder == null) {
			Vector3f hNormal = Math3DCache.getVector3f();
			Vector3f vNormal = Math3DCache.getVector3f();
			try {
				Plane horizontalBorder = getHorizontalBorder(i_query);
				horizontalBorder.getNormal(hNormal);
				IVector3f rayDirection = i_query.getRayDirection();

				Math3D.cross(rayDirection, hNormal, vNormal);

				verticalBorder = new Plane();
				verticalBorder.set(i_query.getRayOrigin(), vNormal);

				i_query.set(KEY_VERTICAL_BORDER, verticalBorder);
			} finally {
				Math3DCache.returnVector3f(hNormal, vNormal);
			}
		}

		return verticalBorder;
	}

	private Plane getVisibleBorder(Query i_query) {

		Plane visibleBorder = (Plane) i_query.get(KEY_VISIBLE_BORDER);
		if (visibleBorder == null) {
			IVector3f rayOrigin = i_query.getRayOrigin();
			IVector3f rayDirection = i_query.getRayDirection();

			visibleBorder = new Plane();
			visibleBorder.set(rayOrigin, rayDirection);

			i_query.set(KEY_VISIBLE_BORDER, visibleBorder);
		}

		return visibleBorder;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.shapes.Shape#render()
	 */
	public void render(RenderContext renderContext) {

		if (m_points == null || m_points.isEmpty())
			return;

		float red = m_color[0];
		float green = m_color[1];
		float blue = m_color[2];
		float alpha = m_color[3];
		Graphics3D g3d = renderContext.getGraphics3D();

		g3d.glColor4f(red, green, blue, alpha);

		g3d.glBegin(Graphics3DDraw.GL_LINE_STRIP);
		for (IVector3f point : m_points)
			g3d.glVertex3f(point.getX(), point.getY(), point.getZ());
		g3d.glEnd();
	}

	/**
	 * Sets the color of this polyline shape.
	 * 
	 * @param i_color the color
	 * @param i_alpha the alpha value
	 * @throws NullPointerException if the given color is <code>null</code>
	 */
	public void setColor(Color i_color, int i_alpha) {

		ColorConverter.toFloatArray(i_color, i_alpha, m_color);
	}

	/**
	 * Sets the color of this polyline shape.
	 * 
	 * @param i_color the color in BBGGRR format
	 * @param i_alpha the alpha value
	 */
	public void setColor(int i_color, int i_alpha) {

		ColorConverter.toFloatArray(i_color, i_alpha, m_color);
	}

	/**
	 * Sets the points of the polyline to render.
	 * 
	 * @param i_points the points
	 * @throws NullPointerException if the given list is <code>null</code>
	 */
	public void setPoints(List<IVector3f> i_points) {

		if (i_points == null)
			throw new NullPointerException("i_points must not be null");

		m_points = i_points;
	}

}
