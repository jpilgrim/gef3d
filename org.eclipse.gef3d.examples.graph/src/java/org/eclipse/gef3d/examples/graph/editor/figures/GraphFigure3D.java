/*******************************************************************************
 * Copyright (c) 2008 Jens von Pilgrim and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jens von Pilgrim - initial API and implementation
 ******************************************************************************/
package org.eclipse.gef3d.examples.graph.editor.figures;

import java.util.logging.Logger;

import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw3d.FreeformLayer3D;
import org.eclipse.draw3d.RenderContext;
import org.eclipse.draw3d.SurfaceLayout;
import org.eclipse.draw3d.TransparentObject;
import org.eclipse.draw3d.camera.ICamera;
import org.eclipse.draw3d.geometry.IVector3f;
import org.eclipse.draw3d.geometry.Vector3fImpl;
import org.eclipse.draw3d.shapes.CuboidFigureShape;
import org.eclipse.draw3d.shapes.Shape;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

/**
 * GraphFigure3D There should really be more documentation here.
 * 
 * @author Jens von Pilgrim
 * @version $Revision$
 * @since 21.11.2007
 */
public class GraphFigure3D extends FreeformLayer3D implements TransparentObject {
	/**
	 * Logger for this class
	 */
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(GraphFigure3D.class
			.getName());

	private static final Vector3fImpl TMP_V3 = new Vector3fImpl();

	private Shape m_shape = new CuboidFigureShape(this);

	/**
	 * 
	 */
	public GraphFigure3D() {
		SurfaceLayout.setDelegate(this, new FreeformLayout());

		setLocation3D(IVector3f.NULLVEC3f);
		// f.setSize3D(new Vector3f(1300, 900, 60));
		// if (((Graph)getModel()).getVerteces().size()>50) {
		// f.setSize3D(new Vector3f(1500, 1300, 60));
		// } else {
		// f.setSize3D(new Vector3f(400, 300, 60));
		// }

		Vector3fImpl size = new Vector3fImpl(getPosition3D().getSize3D());
		size.setZ(150);
		getPosition3D().setSize3D(size);
		// was: bounds3D.setDepth(150);

		float rotX = (float) Math.toRadians(30);
		float rotY = (float) Math.toRadians(0);
		float rotZ = (float) Math.toRadians(0);

		// Rotation is disabled, leads to an infinite loop in conjunction
		// with handles.
		// f.setRotation3D(new Vector3f(rotX, rotY, rotZ));

		Color bgColor = new Color(Display.getCurrent(), 0xFF, 0xFF, 0xFF);
		setBackgroundColor(bgColor);
		setAlpha((byte) 0x44);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.TransparentObject#getTransparencyDepth()
	 */
	public float getTransparencyDepth(RenderContext renderContext) {

		ICamera camera = renderContext.getCamera();

		getBounds3D().getCenter(TMP_V3);
		return camera.getDistance(TMP_V3);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.Figure3D#render()
	 */
	@Override
	public void render(RenderContext renderContext) {
		renderContext.addTransparentObject(this);
	}

	

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.TransparentObject#renderTransparent(org.eclipse.draw3d.RenderContext)
	 */
	public void renderTransparent(RenderContext renderContext) {
		m_shape.render(renderContext);

	}

}