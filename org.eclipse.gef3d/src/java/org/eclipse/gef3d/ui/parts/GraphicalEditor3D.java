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
package org.eclipse.gef3d.ui.parts;

import org.eclipse.draw3d.LightweightSystem3D;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.gef3d.preferences.ScenePreferenceListener;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorSite;


/**
 * GraphicalEditor3D, overrides
 * {@link GraphicalEditor#createPartControl(org.eclipse.swt.widgets.Composite).
 * 
 * @author Jens von Pilgrim
 * @version $Revision$
 * @since 16.11.2007
 */
public abstract class GraphicalEditor3D extends GraphicalEditor {

	/**
	 * The preference listener for this editor.
	 */
	protected ScenePreferenceListener sceneListener;

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method calls several helper methods which could be overridden by
	 * subclasses:
	 * <ol>
	 * <li>{@link #doCreateGraphicalViewer()}</li>
	 * <li>{@link #doAttachFPSCounter(GraphicalViewer3D)}</li>
	 * <li>{@link #doRegisterToScene(IScene)}</li>
	 * </ol>
	 * </p>
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#createGraphicalViewer(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createGraphicalViewer(Composite i_parent) {
		GraphicalViewer3D viewer = doCreateGraphicalViewer();

		// 1:1 from GraphicalEditor.createGraphicalViewer(Composite)
		Control control = viewer.createControl(i_parent);
		setGraphicalViewer(viewer);
		configureGraphicalViewer();
		hookGraphicalViewer();
		initializeGraphicalViewer();

		doAttachFPSCounter(viewer);
		control.addDisposeListener(viewer.getLightweightSystem3D());

		if (viewer instanceof IScene) {
			doRegisterToScene((IScene) viewer);
		}
	}
	
	
	/**
	 * Called by {@link #createGraphicalViewer(Composite)} if created viewer is
	 * an instance of {@link IScene}.
	 * 
	 * @param scene
	 */
	protected void doRegisterToScene(IScene scene) {
		sceneListener = new ScenePreferenceListener(scene);
		sceneListener.start();
	}

	/**
	 * Called by {@link #createGraphicalViewer(Composite)} if created viewer is
	 * an instanceof {@link GraphicalViewer3D}.
	 * 
	 * @param viewer3D
	 */
	protected void doAttachFPSCounter(GraphicalViewer3D viewer3D) {
		IEditorSite editorSite = getEditorSite();
		IActionBars actionBars = editorSite.getActionBars();
		IStatusLineManager statusLine = actionBars.getStatusLineManager();

		FpsStatusLineItem fpsCounter = new FpsStatusLineItem();
		LightweightSystem3D lightweightSystem3D =
			viewer3D.getLightweightSystem3D();
		lightweightSystem3D.addRendererListener(fpsCounter);
		statusLine.add(fpsCounter);
	}

	/**
	 * Here, a {@link GraphicalViewer3DImpl} is created instead of a
	 * ScrollingGraphicalViewer.
	 * 
	 * @return
	 */
	protected GraphicalViewer3D doCreateGraphicalViewer() {
		GraphicalViewer3DImpl viewer = new GraphicalViewer3DImpl();
		return viewer;
	}
	
	
	protected void createActionBarContribution() {
		
		
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#dispose()
	 */
	@Override
	public void dispose() {

		if (sceneListener != null)
			sceneListener.stop();

		super.dispose();
	}
}
