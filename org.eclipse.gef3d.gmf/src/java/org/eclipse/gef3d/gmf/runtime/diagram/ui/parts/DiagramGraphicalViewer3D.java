/*******************************************************************************
 * Copyright (c) 2009 Kristian Duske and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Kristian Duske - initial API and implementation
 ******************************************************************************/
package org.eclipse.gef3d.gmf.runtime.diagram.ui.parts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.draw2d.ExclusionSearch;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.TreeSearch;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw3d.Draw3DCanvas;
import org.eclipse.draw3d.IFigure3D;
import org.eclipse.draw3d.ISurface;
import org.eclipse.draw3d.LightweightSystem3D;
import org.eclipse.draw3d.geometry.Math3D;
import org.eclipse.draw3d.geometry.Math3DCache;
import org.eclipse.draw3d.geometry.Vector3f;
import org.eclipse.draw3d.picking.Hit;
import org.eclipse.draw3d.picking.Picker;
import org.eclipse.draw3d.util.Draw3DCache;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Handle;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef3d.gmf.runtime.core.service.IProviderAcceptorProvider;
import org.eclipse.gef3d.gmf.runtime.core.service.ProviderAcceptor;
import org.eclipse.gef3d.ui.parts.GraphicalViewer3D;
import org.eclipse.gmf.runtime.common.ui.services.editor.IEditorProvider;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramGraphicalViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * 3D diagram graphical viewer.
 * 
 * @author Kristian Duske
 * @version $Revision$
 * @since Apr 7, 2009
 */
public class DiagramGraphicalViewer3D extends DiagramGraphicalViewer implements
		GraphicalViewer3D, IProviderAcceptorProvider {

	/**
	 * Creates this viewer and adds a {@link ProviderAcceptor} to its
	 * properties. This provider selector is also attached as viewer to the
	 * diagram, in order to be accessible via {@link IEditorProvider}, see
	 * {@link IProviderSelector} for details.
	 */
	public DiagramGraphicalViewer3D() {

		this(new ProviderAcceptor(true));

	}

	/**
	 * Creates this viewer and adds the given provider selector to is
	 * properties.
	 * 
	 * @param providerAcceptor
	 */
	public DiagramGraphicalViewer3D(ProviderAcceptor providerAcceptor) {

		setProperty(ProviderAcceptor.PROVIDER_ACCEPTOR_PROPERTY_KEY,
			providerAcceptor);
	}

	/**
	 * Always returns false as no 3D version of
	 * {@link org.eclipse.gmf.runtime.diagram.ui.parts.DiagramGraphicalViewer.LightweightSystemWithUpdateToggle}
	 * is available yet.
	 * 
	 * @todo implement 3D version of LightweightSystemWithUpdateToggle
	 * @see org.eclipse.gmf.runtime.diagram.ui.parts.DiagramGraphicalViewer#areUpdatesDisabled()
	 */
	@Override
	public boolean areUpdatesDisabled() {

		return false;
	}

	/**
	 * Creates a new GL canvas, sets it as this viewer's control and returns it.
	 * 
	 * @param i_composite the parent composite
	 * @return the GL canvas
	 */
	public Control createControl3D(Composite i_composite) {

		GLCanvas canvas =
			Draw3DCanvas.createCanvas(i_composite, SWT.NONE,
				getLightweightSystem3D());

		setControl(canvas);
		return getControl();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.gmf.runtime.diagram.ui.parts.DiagramGraphicalViewer#createLightweightSystem()
	 */
	@Override
	protected LightweightSystem createLightweightSystem() {

		return new LightweightSystem3D();
	}

	/**
	 * Does nothing yet as no 3D version of
	 * {@link org.eclipse.gmf.runtime.diagram.ui.parts.DiagramGraphicalViewer.LightweightSystemWithUpdateToggle}
	 * is available yet.
	 * 
	 * @todo implement 3D version of LightweightSystemWithUpdateToggle
	 * @see org.eclipse.gmf.runtime.diagram.ui.parts.DiagramGraphicalViewer#enableUpdates(boolean)
	 */
	@Override
	public void enableUpdates(boolean i_enable) {

	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method was copied and subsequently modified.
	 * </p>
	 * 
	 * @author hudsonr (original implementation)
	 * @author Kristian Duske
	 * @see org.eclipse.gef.ui.parts.GraphicalViewerImpl#findHandleAt(org.eclipse.draw2d.geometry.Point)
	 */
	@Override
	public Handle findHandleAt(Point i_mLocation) {

		LayerManager layermanager =
			(LayerManager) getEditPartRegistry().get(LayerManager.ID);

		if (layermanager == null)
			return null;

		Vector3f eye = Draw3DCache.getVector3f();
		Vector3f wLocation = Draw3DCache.getVector3f();
		Vector3f direction = Draw3DCache.getVector3f();
		Point sLocation = Draw3DCache.getPoint();
		try {
			List<IFigure> ignore = new ArrayList<IFigure>(3);
			ignore.add(layermanager.getLayer(LayerConstants.PRIMARY_LAYER));
			ignore.add(layermanager.getLayer(LayerConstants.CONNECTION_LAYER));
			ignore.add(layermanager.getLayer(LayerConstants.FEEDBACK_LAYER));
			TreeSearch search = new ExclusionSearch(ignore);

			LightweightSystem3D lws = getLightweightSystem3D();
			Picker picker = lws.getPicker();

			Hit hit = picker.getHit(i_mLocation.x, i_mLocation.y, search);
			if (hit == null)
				return null;

			IFigure3D figure3D = hit.getFigure();
			if (figure3D instanceof Handle)
				return (Handle) figure3D;

			// keep searching on the surface
			lws.getCamera().getPosition(eye);
			lws.getCamera().unProject(i_mLocation.x, i_mLocation.y, 0, null,
				wLocation);

			ISurface surface = figure3D.getSurface();

			Math3D.getRayDirection(eye, wLocation, direction);
			surface.getSurfaceLocation2D(eye, direction, sLocation);

			IFigure figure2D =
				figure3D.findFigureAt(sLocation.x, sLocation.y, search);

			if (figure2D instanceof Handle)
				return (Handle) figure2D;

			return null;
		} finally {
			Draw3DCache.returnVector3f(eye);
			Draw3DCache.returnVector3f(wLocation);
			Draw3DCache.returnVector3f(direction);
			Draw3DCache.returnPoint(sLocation);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method was copied and subsequently modified.
	 * </p>
	 * 
	 * @author hudsonr (original implementation)
	 * @author Kristian Duske
	 * @see org.eclipse.gef.ui.parts.GraphicalViewerImpl#findObjectAtExcluding(org.eclipse.draw2d.geometry.Point,
	 *      java.util.Collection, org.eclipse.gef.EditPartViewer.Conditional)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public EditPart findObjectAtExcluding(Point i_mLocation,
		Collection i_exclude, final Conditional i_condition) {

		class ConditionalTreeSearch extends ExclusionSearch {

			ConditionalTreeSearch(Collection coll) {

				super(coll);
			}

			@Override
			public boolean accept(IFigure i_figure) {

				IFigure current = i_figure;
				EditPart editpart = null;
				while (editpart == null && current != null) {
					editpart = (EditPart) getVisualPartMap().get(current);
					current = current.getParent();
				}

				return editpart != null
					&& (i_condition == null || i_condition.evaluate(editpart));
			}
		}

		Vector3f rayStart = Math3DCache.getVector3f();
		Vector3f rayDirection = Math3DCache.getVector3f();
		try {

			LightweightSystem3D lws = getLightweightSystem3D();
			Picker picker = lws.getPicker();

			TreeSearch search = new ConditionalTreeSearch(i_exclude);
			Hit hit = picker.getHit(i_mLocation.x, i_mLocation.y, search);

			EditPart part = null;
			if (hit != null) {
				IFigure3D figure3D = hit.getFigure();
				IFigure figure2D =
					figure3D.findFigureAt(i_mLocation.x, i_mLocation.y, search);

				while (part == null && figure2D != null) {
					part = (EditPart) getVisualPartMap().get(figure2D);
					figure2D = figure2D.getParent();
				}
			}

			if (part == null)
				return getContents();

			return part;
		} finally {
			Math3DCache.returnVector3f(rayStart);
			Math3DCache.returnVector3f(rayDirection);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.gef.ui.parts.ScrollingGraphicalViewer#getFigureCanvas()
	 */
	@Override
	protected FigureCanvas getFigureCanvas() {

		return null;
	}

	/**
	 * Returns the 3D lightweight system if there is one.
	 * 
	 * @return the 3D lightweight system or <code>null</code> if the current
	 *         lightweight system is not 3D
	 * @see org.eclipse.gef3d.ui.parts.GraphicalViewer3D#getLightweightSystem3D()
	 */
	public LightweightSystem3D getLightweightSystem3D() {

		LightweightSystem lws = getLightweightSystem();
		if (!(lws instanceof LightweightSystem3D))
			return null;

		return (LightweightSystem3D) lws;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.gef3d.gmf.runtime.core.service.IProviderAcceptorProvider#getProviderAcceptor()
	 */
	public ProviderAcceptor getProviderAcceptor() {

		return (ProviderAcceptor) getProperty(ProviderAcceptor.PROVIDER_ACCEPTOR_PROPERTY_KEY);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.gef.ui.parts.ScrollingGraphicalViewer#reveal(org.eclipse.gef.EditPart)
	 */
	@Override
	public void reveal(EditPart i_part) {

		// TODO: implement this properly
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.gef.ui.parts.AbstractEditPartViewer#setContents(java.lang.Object)
	 */
	@Override
	public void setContents(Object contents) {

		if (contents instanceof EObject) {
			EObject eobj = (EObject) contents;
			ProviderAcceptor providerSelector = getProviderAcceptor();
			eobj.eAdapters().add(providerSelector);

		}
		super.setContents(contents);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.gef.ui.parts.ScrollingGraphicalViewer#setRootFigure(org.eclipse.draw2d.IFigure)
	 */
	@Override
	protected void setRootFigure(IFigure i_figure) {

		super.setRootFigure(i_figure);
		getLightweightSystem().setContents(i_figure);
	}

}
