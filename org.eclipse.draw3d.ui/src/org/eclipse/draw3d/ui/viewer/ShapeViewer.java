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
package org.eclipse.draw3d.ui.viewer;

import org.eclipse.draw3d.IFigure3D;
import org.eclipse.draw3d.ShapeFigure3D;
import org.eclipse.draw3d.geometry.Vector3fImpl;
import org.eclipse.draw3d.shapes.CylinderShape;
import org.eclipse.draw3d.shapes.Shape;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.widgets.Display;

/**
 * ShapeViewer There should really be more documentation here.
 * 
 * @author Kristian Duske
 * @version $Revision$
 * @since 03.06.2009
 */
public class ShapeViewer extends Draw3DViewer {

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.ui.viewer.Draw3DViewer#createContents()
	 */
	@Override
	protected IFigure3D createContents() {

		IFigure3D figure = new ShapeFigure3D() {

			@Override
			protected Shape createShape() {

				// return new SphereShape(12);
				return new CylinderShape(12, 0.4f);
			}
		};

		figure.getPosition3D().setLocation3D(new Vector3fImpl(0, 0, 0));
		figure.getPosition3D().setSize3D(new Vector3fImpl(100, 100, 100));

		Device dev = Display.getCurrent();
		figure.setBackgroundColor(dev.getSystemColor(SWT.COLOR_DARK_BLUE));
		figure.setForegroundColor(dev.getSystemColor(SWT.COLOR_CYAN));

		return figure;
	}
}
