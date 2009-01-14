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

package org.eclipse.gef3d.ext.intermodel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.draw2d.FigureListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.GraphicalEditPart;

/**
 * ConnectedElementAdapter There should really be more documentation here.
 * 
 * @author Jens von Pilgrim
 * @version $Revision$
 * @since 22.01.2008
 */
public class ConnectedElementAdapter  {

	GraphicalEditPart elementEditPart;

	Object intermodelConnector;

	List sourceConnections;

	List targetConnections;
	
	// Observer-Pattern (Subject)
	Collection<IElementAdapterListener> listeners;
	
	

	/**
	 * @param i_elementEditPart
	 * @param i_intermodelConnector
	 */
	public ConnectedElementAdapter(GraphicalEditPart i_elementEditPart,
			Object i_intermodelConnector) {
		
		if (i_elementEditPart == null) // parameter precondition
			throw new NullPointerException("i_elementEditPart must not be null");
		
		
		elementEditPart = i_elementEditPart;
		intermodelConnector = i_intermodelConnector;
		sourceConnections = new ArrayList(2);
		targetConnections = new ArrayList(2);
		listeners = new HashSet<IElementAdapterListener>(); 
			// the edit part listens to this class
		i_elementEditPart.getFigure().addFigureListener(new FigureListener() {

			public void figureMoved(IFigure i_source) {
				fireFigureMoved(i_source);
			}
			
		});

	}

	public void addElementAdapterListener(IElementAdapterListener listener) {
		if (listener == null) // parameter precondition
			throw new NullPointerException("listener must not be null");
		listeners.add(listener);
	}
	
	public void removeElementAdapterListener(IElementAdapterListener listener) {
		if (listener != null) {
			listeners.remove(listener);
		}
	}

	/**
	 * Propagates event to registered listeners.
	 * @param i_source The moved figure, this is the figure displaying the
	 * 	adapted model element.
	 */
	protected void fireFigureMoved(IFigure i_source) {
		for(IElementAdapterListener listener: listeners) {
			listener.figureMoved(i_source);
		}
		
	}

	/**
	 * @return the elementEditPart
	 */
	public GraphicalEditPart getElementEditPart() {
		return elementEditPart;
	}

	/**
	 * May be null
	 * 
	 * @return the intermodelConnector
	 */
	public Object getIntermodelConnector() {
		return intermodelConnector;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer strb = new StringBuffer();
		strb.append("Element's part: ");
		if (elementEditPart != null) {
			strb.append(elementEditPart.toString());
		} else {
			strb.append("ERROR, part is null");
		}
		return strb.toString();
	}

	/**
	 * @return the sourceConnections
	 */
	public List getSourceConnections() {
		return sourceConnections;
	}

	/**
	 * @return the targetConnections
	 */
	public List getTargetConnections() {
		return targetConnections;
	}

	/**
	 * Delegate method
	 * 
	 * @param i_o
	 * @return
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean addSourceConnection(Object i_o) {
		return sourceConnections.add(i_o);
	}

	/**
	 * Delegate method
	 * 
	 * @param i_o
	 * @return
	 * @see java.util.List#remove(java.lang.Object)
	 */
	public boolean removeSourceConnection(Object i_o) {
		return sourceConnections.remove(i_o);
	}

	/**
	 * Delegate method
	 * 
	 * @param i_o
	 * @return
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean addTargetConnection(Object i_o) {
		return targetConnections.add(i_o);
	}

	/**
	 * Delegate method
	 * 
	 * @param i_o
	 * @return
	 * @see java.util.List#remove(java.lang.Object)
	 */
	public boolean removeTargetConnection(Object i_o) {
		return targetConnections.remove(i_o);
	}
	
	

}