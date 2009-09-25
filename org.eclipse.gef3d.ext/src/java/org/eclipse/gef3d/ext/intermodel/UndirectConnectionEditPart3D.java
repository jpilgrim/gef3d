/*******************************************************************************
 * Copyright (c) 2009 Jens von Pilgrim and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jens von Pilgrim - initial API and implementation
 ******************************************************************************/
package org.eclipse.gef3d.ext.intermodel;

import org.eclipse.draw2d.FigureListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef3d.editparts.AbstractConnectionEditPart3D;
import org.eclipse.gef3d.ext.reverselookup.ReverseLookupManager;

/**
 * Edit part for unidirectional connections. In GEF, a connection is only drawn
 * if both, source and target node edit part, support the connection. This is a
 * problem especially in case of inter-diagram-connections, as these connections
 * are often drawn between embedded (formerly) 2D editors, and their edit parts
 * do not now anything about the inter-diagram-connections. This special
 * connection edit part solves that problem by retrieving the source, the target
 * or both edit parts from the model, using a reverse lookup.
 * <p>
 * Subclasses must implement the two abstract methods for retrieving the source
 * and or target model elements from the connection model. One of these methods
 * may return null, if this is the side for which the edit part is known.
 * </p>
 * <p>
 * This class can be used instead of using the connected element adapter
 * pattern.
 * </p>
 * 
 * @todo reconnect not implemented yet!
 * 
 * @author Jens von Pilgrim
 * @version $Revision$
 * @since Sep 25, 2009
 */
public abstract class UndirectConnectionEditPart3D extends
		AbstractConnectionEditPart3D implements FigureListener {

	EditPart adaptedSourceEditPart;

	EditPart adaptedTargetEditPart;

	/**
	 * This method is to be overridden by subclasses May return null if target
	 * edit part is known, as this method is only called in order to retrieve
	 * the edit part.
	 * 
	 * @return
	 */
	abstract public Object getTargetModel();

	/**
	 * This method is to be overridden by subclasses. May return null if source
	 * edit part is known, as this method is only called in order to retrieve
	 * the edit part.
	 * 
	 * @return
	 */
	abstract public Object getSourceModel();

	/**
	 * Get target edit part, tries to perform a reverse lookup in order to
	 * retrieve the edit part by its model. If target edit part is set, this
	 * method simply returns it as the original overridden method. Otherwise,
	 * {@link #getTargetModel()} is used to perform a reverse lookup for
	 * retrieving the source edit part. If the reverse lookup fails, an
	 * {@link IllegalStateException} is thrown.
	 * 
	 * @see org.eclipse.gef.editparts.AbstractConnectionEditPart#getTarget()
	 * @throws IllegalStateException if reverse lookup fails
	 */
	@Override
	public EditPart getTarget() {
		EditPart part = super.getTarget();
		if (part != null)
			return part;

		if (adaptedTargetEditPart == null) {
			Object targetModel = getTargetModel();
			if (targetModel == null) {
				return null;
			}
			adaptedTargetEditPart =
				getReverseLookupManager().findNotationElementForDomainElement(
					targetModel);
			if (adaptedTargetEditPart == null) {
				throw new IllegalStateException(
					"Target edit part not found by reverse lookup, "
						+ "check reverse lookup configuration.");
			}
			if (adaptedTargetEditPart instanceof GraphicalEditPart)
				((GraphicalEditPart) adaptedTargetEditPart).getFigure()
					.addFigureListener(this);

		}
		return adaptedTargetEditPart;

	}

	/**
	 * Get source edit part, tries to perform a reverse lookup in order to
	 * retrieve the edit part by its model. If source edit part is set, this
	 * method simply returns it as the original overridden method. Otherwise,
	 * {@link #getSourceModel()} is used to perform a reverse lookup for
	 * retrieving the source edit part. If the reverse lookup fails, an
	 * {@link IllegalStateException} is thrown.
	 * 
	 * @see org.eclipse.gef.editparts.AbstractConnectionEditPart#getTarget()
	 * @throws IllegalStateException if reverse lookup fails
	 */
	@Override
	public EditPart getSource() {
		EditPart part = super.getSource();
		if (part != null)
			return part;

		if (adaptedSourceEditPart == null) {
			Object sourceModel = getSourceModel();
			if (sourceModel == null) {
				return null;
			}
			adaptedSourceEditPart =
				getReverseLookupManager().findNotationElementForDomainElement(
					sourceModel);
			if (adaptedSourceEditPart == null) {
				throw new IllegalStateException(
					"Source edit part not found by reverse lookup, "
						+ "check reverse lookup configuration.");
			}
			if (adaptedSourceEditPart instanceof GraphicalEditPart)
				((GraphicalEditPart) adaptedSourceEditPart).getFigure()
					.addFigureListener(this);

		}
		return adaptedSourceEditPart;

	}

	/**
	 * Returns the reverse lookup manager, for details oin how to install this
	 * manager, see {@link ReverseLookupManager}.
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected ReverseLookupManager<EditPart> getReverseLookupManager() {
		ReverseLookupManager<EditPart> reverseLookupManager =
			(ReverseLookupManager<EditPart>) getViewer().getProperty(
				ReverseLookupManager.RLM_ID);
		if (reverseLookupManager == null) {
			throw new NullPointerException(
				"ReverseLookupManager not found in viewer properies, "
					+ "check configuration");
		}
		return reverseLookupManager;
	}

	/**
	 * Refresh the connection edit part visuals, i.e. its figure, if an observed
	 * figure, i.e. target or source figure, has been moved.
	 * 
	 * @see org.eclipse.draw2d.FigureListener#figureMoved(org.eclipse.draw2d.IFigure)
	 */
	public void figureMoved(IFigure i_source) {
		refreshVisuals();
	}

	/**
	 * {@inheritDoc} Removes this edit part from the source and/or target
	 * figures.
	 * 
	 * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#deactivate()
	 */
	@Override
	public void deactivate() {
		super.deactivate();
		if (adaptedSourceEditPart != null
			&& adaptedSourceEditPart instanceof GraphicalEditPart) {
			((GraphicalEditPart) adaptedSourceEditPart).getFigure()
				.removeFigureListener(this);
		}
		if (adaptedTargetEditPart != null
			&& adaptedTargetEditPart instanceof GraphicalEditPart) {
			((GraphicalEditPart) adaptedTargetEditPart).getFigure()
				.removeFigureListener(this);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Calls super method and performs a refresh, if target and source edit part
	 * can be retrieved from the model by a reverse look up.
	 * </p>
	 * 
	 * @see org.eclipse.gef.editparts.AbstractEditPart#setModel(java.lang.Object)
	 */
	@Override
	public void setModel(Object i_model) {
		super.setModel(i_model);
		if (getSource() != null && getTarget() != null) {
			refresh();
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Calls super method and performs a refresh, if target edit part can be
	 * retrieved from the model by a reverse look up.
	 * </p>
	 * 
	 * @see org.eclipse.gef.editparts.AbstractConnectionEditPart#setSource(org.eclipse.gef.EditPart)
	 */
	@Override
	public void setSource(EditPart i_editPart) {
		super.setSource(i_editPart);
		if (i_editPart != null && getTarget() != null) {
			refresh();
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Calls super method and performs a refresh, if source edit part can be
	 * retrieved from the model by a reverse look up.
	 * </p>
	 * 
	 * @see org.eclipse.gef.editparts.AbstractConnectionEditPart#setTarget(org.eclipse.gef.EditPart)
	 */
	@Override
	public void setTarget(EditPart i_editPart) {
		super.setTarget(i_editPart);
		if (i_editPart != null && getSource() != null) {
			refresh();
		}
	}
}