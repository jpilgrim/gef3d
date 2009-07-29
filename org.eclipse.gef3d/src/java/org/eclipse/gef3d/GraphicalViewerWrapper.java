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
package org.eclipse.gef3d;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw3d.IScene;
import org.eclipse.draw3d.ISurface;
import org.eclipse.draw3d.camera.ICamera;
import org.eclipse.draw3d.geometry.Vector3f;
import org.eclipse.draw3d.picking.ColorPicker;
import org.eclipse.draw3d.util.Cache;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.Handle;

/**
 * GraphicalViewerWrapper There should really be more documentation here.
 * 
 * @author Kristian Duske
 * @version $Revision$
 * @since 28.07.2009
 */
public class GraphicalViewerWrapper extends EditPartViewerWrapper implements
        GraphicalViewer {

    /**
     * Creates a new wrapper that delegates to the given graphical viewer.
     * 
     * @param i_viewer
     *            the graphical viewer to delegate to
     * @param i_scene
     *            the scene
     * 
     * @throws NullPointerException
     *             if any of the given arguments is <code>null</code>
     */
    public GraphicalViewerWrapper(GraphicalViewer i_viewer, IScene i_scene) {

        super(i_viewer, i_scene);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.gef.GraphicalViewer#findHandleAt(org.eclipse.draw2d.geometry.Point)
     */
    public Handle findHandleAt(Point i_surfaceLocation) {

        Point mouseLocation = Cache.getPoint();
        Vector3f worldLocation = Cache.getVector3f();
        try {
            ColorPicker picker = m_scene.getPicker();
            ISurface surface = picker.getCurrentSurface();

            surface.getWorldLocation(i_surfaceLocation, worldLocation);

            ICamera camera = m_scene.getCamera();
            camera.project(worldLocation, mouseLocation);

            return ((GraphicalViewer) m_viewer).findHandleAt(mouseLocation);
        } finally {
            Cache.returnPoint(mouseLocation);
            Cache.returnVector3f(worldLocation);
        }
    }

}
