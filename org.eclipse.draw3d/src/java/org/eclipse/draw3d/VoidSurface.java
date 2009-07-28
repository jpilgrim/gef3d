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
import org.eclipse.draw2d.UpdateManager;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw3d.camera.ICamera;
import org.eclipse.draw3d.camera.ICameraListener;
import org.eclipse.draw3d.geometry.Math3D;
import org.eclipse.draw3d.geometry.Vector3f;
import org.eclipse.draw3d.picking.ColorPicker;

/**
 * The void surface is used by the root figure. The void surface is always
 * oriented perpendicular to the viewing direction at a fixed distance.
 * 
 * @author Kristian Duske
 * @version $Revision$
 * @since 14.07.2009
 */
public class VoidSurface extends AbstractSurface implements ISceneListener {

    private ICameraListener m_cameraListener = new ICameraListener() {

        public void cameraChanged() {

            coordinateSystemChanged();
        }
    };

    private float m_depth;

    private IFigure3D m_host;

    private IScene m_scene;

    /**
     * Creates a new void surface for the given scene. The given depth value
     * specifies the distance of the surface from the camera.
     * 
     * @param i_host
     *            the host figure of this surface
     * @param i_scene
     *            the scene
     * @param i_depth
     *            the depth value of the surface
     */
    public VoidSurface(IFigure3D i_host, IScene i_scene, float i_depth) {

        if (i_host == null)
            throw new NullPointerException("i_host must not be null");

        if (i_scene == null)
            throw new NullPointerException("i_scene must not be null");

        m_host = i_host;
        m_scene = i_scene;
        m_depth = i_depth;

        m_scene.getCamera().addCameraListener(m_cameraListener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.draw3d.ISceneListener#cameraChanged(org.eclipse.draw3d.camera.ICamera,
     *      org.eclipse.draw3d.camera.ICamera)
     */
    public void cameraChanged(ICamera i_oldCamera, ICamera i_newCamera) {

        i_oldCamera.removeCameraListener(m_cameraListener);
        i_newCamera.addCameraListener(m_cameraListener);

        coordinateSystemChanged();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.draw3d.ISurface#findFigureAt(int, int,
     *      org.eclipse.draw2d.TreeSearch)
     */
    public IFigure findFigureAt(int i_sx, int i_sy, TreeSearch i_search) {

        UpdateManager updateManager = m_scene.getUpdateManager();
        if (!(updateManager instanceof PickingUpdateManager3D))
            return null;

        Vector3f w = Math3D.getVector3f();
        Vector3f rayStart = Math3D.getVector3f();
        Vector3f rayDirection = Math3D.getVector3f();
        try {

            // input coordinates are surface coordinates, convert them into
            // mouse coordinates
            getWorldLocation(i_sx, i_sy, 0, w);
            Point m = m_scene.getCamera().project(w.getX(), w.getY(), w.getZ(),
                null);

            ColorPicker picker = ((PickingUpdateManager3D) updateManager).getPicker();
            IFigure3D hit = picker.getFigure3D(m.x, m.y);

            if (hit == null)
                return null;

            m_scene.getCamera().getPosition(rayStart);
            
            Math3D.sub(w, rayStart, rayDirection);
            Math3D.normalise(rayDirection, rayDirection);
            
            ISurface surface = hit.getSurface();
            Point s = surface.getSurfaceLocation2D(rayStart, rayDirection, null);

            return hit.getSurface().findFigureAt(s.x, s.y, i_search);
        } finally {
            Math3D.returnVector3f(w);
            Math3D.returnVector3f(rayStart);
            Math3D.returnVector3f(rayDirection);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.draw3d.ISurface#getHost()
     */
    public IFigure2DHost3D getHost() {

        return m_host;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.draw3d.AbstractSurface#getOrigin(org.eclipse.draw3d.geometry.Vector3f)
     */
    @Override
    protected Vector3f getOrigin(Vector3f io_result) {

        ICamera camera = m_scene.getCamera();
        return camera.unProject(0, 0, m_depth, null, io_result);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.draw3d.AbstractSurface#getXAxis(org.eclipse.draw3d.geometry.Vector3f)
     */
    @Override
    protected Vector3f getXAxis(Vector3f io_result) {

        ICamera camera = m_scene.getCamera();
        return camera.getRightVector(io_result);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.draw3d.AbstractSurface#getYAxis(org.eclipse.draw3d.geometry.Vector3f)
     */
    @Override
    protected Vector3f getYAxis(Vector3f io_result) {

        ICamera camera = m_scene.getCamera();
        Vector3f yAxis = camera.getUpVector(io_result);

        yAxis.scale(-1);
        return yAxis;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.draw3d.AbstractSurface#getZAxis(org.eclipse.draw3d.geometry.Vector3f)
     */
    @Override
    protected Vector3f getZAxis(Vector3f io_result) {

        ICamera camera = m_scene.getCamera();
        Vector3f zAxis = camera.getViewDirection(io_result);

        Math3D.normalise(zAxis, zAxis);
        return zAxis;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.draw3d.ISceneListener#renderPassFinished(org.eclipse.draw3d.RenderContext)
     */
    public void renderPassFinished(RenderContext i_renderContext) {

        // nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.draw3d.ISceneListener#renderPassStarted(org.eclipse.draw3d.RenderContext)
     */
    public void renderPassStarted(RenderContext i_renderContext) {

        // nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        StringBuilder b = new StringBuilder();

        b.append("Void surface with origin ");
        b.append(getOrigin(null));
        b.append(" and depth " + m_depth);

        return b.toString();
    }
}