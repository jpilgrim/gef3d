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
package org.eclipse.draw3d;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.TreeSearch;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw3d.geometry.IVector3f;
import org.eclipse.draw3d.geometry.Vector3f;

/**
 * A surface is a 2D plane that belongs to a figure and has 2D children of that
 * figure projected on it. It has a 3D coordinate system with its origin in the
 * upper left corner and the Z axis oriented so that points with a positive Z
 * component are behind the surface.
 * 
 * @author Kristian Duske
 * @version $Revision$
 * @since 08.07.2009
 */
public interface ISurface {

    /**
     * Returns the child figure at the given surface coordinates or
     * <code>null</code> if no such figure exists. If the given tree search
     * structure is not <code>null</code>, only such figures that satisfy the
     * tree search conditions will be considered.
     * 
     * @param i_sx
     *            the surface X coordinate
     * @param i_sy
     *            the surface Y coordinate
     * @param i_search
     *            the tree search structure
     * @return the figure at the given surface coordinates or <code>null</code>
     *         if no such figure exists
     */
    public IFigure findFigureAt(int i_sx, int i_sy, TreeSearch i_search);

    /**
     * Returns the host figure of this surface.
     * 
     * @return the host figure
     */
    public IFigure3D getHost();

    /**
     * Returns the 2D surface coordinates of a point specified in world
     * coordinates.
     * 
     * @param i_wx
     *            the world X coordinate
     * @param i_wy
     *            the world Y coordinate
     * @param i_wz
     *            the world Z coordinate
     * @param io_result
     *            the result point, if <code>null</code>, a new point will be
     *            created
     * @return the 2D surface coordinates of the given point
     */
    public Point getSurfaceLocation2D(float i_wx, float i_wy, float i_wz,
            Point io_result);

    /**
     * 
     * Returns the 2D surface coordinates of a point specified by the
     * intersection of a ray and the surface.
     * 
     * @param i_rayStart
     *            the start point of the ray
     * @param i_rayPoint
     *            the a point on the ray other than the start point
     * @param io_result
     *            the result point, if <code>null</code>, a new point will be
     *            returned
     * 
     * @return the 2D surface coordinates of the point of intersection or
     *         <code>null</code> if the given ray does not intersect with this
     *         surface
     */
    public Point getSurfaceLocation2D(IVector3f i_rayStart,
            IVector3f i_rayPoint, Point io_result);

    /**
     * Returns the 2D surface coordinates of a point specified in world
     * coordinates.
     * 
     * @param i_world
     *            the world coordinates of the point
     * @param io_result
     *            the result point, if <code>null</code>, a new point will be
     *            created
     * @return the 2D surface coordinates of the given point
     */
    public Point getSurfaceLocation2D(IVector3f i_world, Point io_result);

    /**
     * Returns the 3D surface coordinates of a point specified in world
     * coordinates.
     * 
     * @param i_wx
     *            the world X coordinate
     * @param i_wy
     *            the world Y coordinate
     * @param i_wz
     *            the world Z coordinate
     * @param io_result
     *            the result vector, if <code>null</code>, a new vector will be
     *            created
     * @return the 3D surface coordinates of the given point
     */
    public Vector3f getSurfaceLocation3D(float i_wx, float i_wy, float i_wz,
            Vector3f io_result);

    /**
     * Returns the 3D surface coordinates of a point given in 3D surface
     * coordinates in reference to a given surface. This can be used to convert
     * from surface coordinates relative to a given surface to surface
     * coordinates relative to this surface.
     * 
     * @param i_reference
     *            the reference surface
     * @param i_surface
     *            the 3D surface coordinates relative to the given reference
     *            surface
     * @param io_result
     *            the result vector, if <code>null</code>, a new vector will be
     *            returned
     * @return the 3D surface coordinates relative to this surface
     */
    public Vector3f getSurfaceLocation3D(ISurface i_reference,
            Vector3f i_surface, Vector3f io_result);

    /**
     * Returns the 3D surface coordinates of a point specified by the
     * intersection of a ray and the surface.
     * 
     * @param i_rayStart
     *            the start point of the ray
     * @param i_rayPoint
     *            a pointon the ray other than the start point
     * @param io_result
     *            the result vector, if <code>null</code>, a new vector will be
     *            returned
     * 
     * @return the 3D surface coordinates of the point of intersection or
     *         <code>null</code> if the given ray does not intersect with this
     *         surface
     */
    public Vector3f getSurfaceLocation3D(IVector3f i_rayStart,
            IVector3f i_rayPoint, Vector3f io_result);

    /**
     * Returns the 3D surface coordinates of a point specified in world
     * coordinates.
     * 
     * @param i_world
     *            the world coordinates of the point
     * @param io_result
     *            the result vector, if <code>null</code>, a new vector will be
     *            created
     * @return the 3D surface coordinates of the given point
     */
    public Vector3f getSurfaceLocation3D(IVector3f i_world, Vector3f io_result);

    /**
     * Returns the world dimensions of the given dimension specified in 2D
     * surface coordinates.
     * 
     * @param i_surface
     *            the 2D surface coordinates of the dimension
     * @param io_result
     *            the result vector, if <code>null</code>, a new vector will be
     *            created
     * @return the result vector
     */
    public Vector3f getWorldDimension(Dimension i_surface, Vector3f io_result);

    /**
     * Returns the world coordinates of the given point specified in 3D surface
     * coordinates.
     * 
     * @param i_sx
     *            the surface X coordinate
     * @param i_sy
     *            the surface Y coordinate
     * @param i_sz
     *            the surface Z coordinate
     * @param io_result
     *            the result vector, if <code>null</code>, a new vector will be
     *            created
     * @return the result vector
     */
    public Vector3f getWorldLocation(float i_sx, float i_sy, float i_sz,
            Vector3f io_result);

    /**
     * Returns the world coordinates of the given point specified in 3D surface
     * coordinates.
     * 
     * @param i_surface
     *            the 3D surface coordinates of the point
     * @param io_result
     *            the result vector, if <code>null</code>, a new one will be
     *            created
     * @return the result vector
     */
    public Vector3f getWorldLocation(IVector3f i_surface, Vector3f io_result);

    /**
     * Returns the world coordinates of the given point specified in 2D surface
     * coordinates.
     * 
     * @param i_surface
     *            the 2D surface coordinates of the point
     * @param io_result
     *            the result vector, if <code>null</code>, a new one will be
     *            created
     * @return the result vector
     */
    public Vector3f getWorldLocation(Point i_surface, Vector3f io_result);
}
