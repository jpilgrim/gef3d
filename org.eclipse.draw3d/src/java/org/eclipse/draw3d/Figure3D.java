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
package org.eclipse.draw3d;

import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureListener;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.TreeSearch;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.geometry.Translatable;
import org.eclipse.draw3d.geometry.IBoundingBox;
import org.eclipse.draw3d.geometry.IHost3D;
import org.eclipse.draw3d.geometry.IMatrix4f;
import org.eclipse.draw3d.geometry.IParaxialBoundingBox;
import org.eclipse.draw3d.geometry.IVector3f;
import org.eclipse.draw3d.geometry.Position3D;
import org.eclipse.draw3d.geometry.Position3DUtil;
import org.eclipse.draw3d.geometry.Transformable;
import org.eclipse.draw3d.geometry.IPosition3D.MatrixState;
import org.eclipse.draw3d.geometry.IPosition3D.PositionHint;
import org.eclipse.draw3d.geometryext.SyncedVector3f;
import org.eclipse.draw3d.geometryext.SynchronizedPosition3DImpl;
import org.eclipse.draw3d.picking.Query;
import org.eclipse.swt.graphics.Font;

/**
 * 3D version of GEF's Figure. This class extends Figure and can be used instead
 * of a 2D figure in 2D and 3D editors.
 * 
 * @author Jens von Pilgrim
 * @version $Revision$
 * @since 08.11.2007
 */
public class Figure3D extends Figure implements IFigure3D {

	/**
	 * Logger for this class
	 */
	protected static final Logger log =
		Logger.getLogger(Figure3D.class.getName());

	/**
	 * The texture needs to be invalidated every time a child is moved so that
	 * the changes are drawn on the screen.
	 */
	private FigureListener childMovedListener = new FigureListener() {

		public void figureMoved(IFigure i_source) {

			repaint2DComponents = true;
		}
	};

	/**
	 * The connection layer for his figure's 2D children.
	 */
	protected ConnectionLayer connectionLayer = null;

	/**
	 * This figure's friend.
	 */
	protected Figure3DFriend friend;

	/**
	 * This figure's helper.
	 */
	protected Figure3DHelper helper;

	/**
	 * The alpha value of this figure.
	 * 
	 * @see IFigure3D#setAlpha(byte)
	 */
	protected int m_alpha = 255;

	private IParaxialBoundingBox m_paraxialBounds;

	SynchronizedPosition3DImpl position3D;

	/**
	 * The preferred 3D size of this figure. The preferred 3D size is
	 * synchronized with the preferred 2D size. preferredSize object is created
	 * lazily if it has not been created before in
	 * {@link #setPreferredSize3D(IVector3f)} and {@link #getPreferredSize3D()}.
	 * Thus, if you are using this member in subclasses directly, remember that
	 * it might be null (and will be in most cases, since it is only used by
	 * some special layout managers).
	 */
	protected SyncedVector3f preferredSize3D;

	/**
	 * In
	 */
	protected boolean repaint2DComponents = true;

	/**
	 * Boolean semaphore used by {@link #syncSize()} and {@link #syncSize3D()}
	 * to avoid infinite loop.
	 */
	protected boolean updatingBounds = false;

	/**
	 * Creates and initializes a new 3D figure.
	 */
	public Figure3D() {

		position3D = new SynchronizedPosition3DImpl(this);
		friend = new Figure3DFriend(this) {

			@Override
			public Font getLocalFont() {

				return Figure3D.this.getLocalFont();
			}

			@Override
			public boolean is2DContentDirty() {

				return repaint2DComponents;
			}
		};

		helper = new Figure3DHelper(friend);
	}

	@Override
	public void add(IFigure i_figure, Object i_constraint, int i_index) {

		super.add(i_figure, i_constraint, i_index);

		// register as figure listener with 2D children so that we know when
		// they move
		if (!(i_figure instanceof IFigure3D))
			i_figure.addFigureListener(childMovedListener);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Figure#findFigureAt(int, int,
	 *      org.eclipse.draw2d.TreeSearch)
	 * @see Figure3DHelper#findFigureAt(int, int, TreeSearch)
	 */
	@Override
	public IFigure findFigureAt(int i_mx, int i_my, TreeSearch i_search) {

		return helper.findFigureAt(i_mx, i_my, i_search);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Overriden to invalidate position 3D after the figure moved.
	 * {@link #invalidate()} is only called after a resize.
	 * </p>
	 * 
	 * @see org.eclipse.draw2d.Figure#fireFigureMoved()
	 */
	@Override
	protected void fireFigureMoved() {

		position3D.invalidateMatrices();

		IFigure parent = getParent();
		while (parent != null) {
			if (parent instanceof IFigure3D)
				((IFigure3D) parent).invalidateParaxialBounds();

			parent = parent.getParent();
		}

		invalidateParaxialBoundsTree();

		super.fireFigureMoved();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#getAlpha()
	 */
	public int getAlpha() {

		return m_alpha;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#getAncestor3D()
	 */
	public IFigure3D getAncestor3D() {

		return Figure3DHelper.getAncestor3D(getParent());
	}

	// Overriding setBounds instead and update bounds in setSize3D/setLocatoin3D
	//	
	/**
	 * Returns 2D bounds of this figure. If resolution is not disabled, the
	 * bounds are converted from the 3D bounds, simply ignoring the z values of
	 * position and size. Otherwise, the 2D bounds are returned.
	 * 
	 * @see org.eclipse.draw2d.Figure#getBounds()
	 */
	@Override
	public Rectangle getBounds() {

		return super.getBounds();
	}

	/**
	 * {@inheritDoc} Returns bounds, i.e. lower left back corner and size. The
	 * coordinates are parent relative coordinates.
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#getBounds3D()
	 * @deprecated use {@link Position3D#getBounds3D()
	 *             getPosition3D().getBounds3D()}, or simply
	 *             {@link #getPosition3D()}
	 */
	public IBoundingBox getBounds3D() {

		return position3D.getBounds3D();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure2DHost3D#getChildren2D()
	 */
	public List<IFigure> getChildren2D() {

		return helper.getChildren2D();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#getChildren3D()
	 */
	public List<IFigure3D> getChildren3D() {

		return helper.getChildren3D();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure2DHost3D#getConnectionLayer(org.eclipse.draw3d.ConnectionLayerFactory)
	 */
	public ConnectionLayer getConnectionLayer(ConnectionLayerFactory i_clfactory) {

		if (connectionLayer == null && i_clfactory != null) {
			connectionLayer = i_clfactory.createConnectionLayer(this);
			// add(connectionLayer); // or else it doesn't have an update
			// manager
		}
		return connectionLayer;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#getSuccessor3D()
	 */
	public List<IFigure3D> getDescendants3D() {

		return helper.getDescendants3D();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.picking.Pickable#getDistance(org.eclipse.draw3d.picking.Query)
	 */
	public float getDistance(Query i_query) {

		return Float.NaN;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#getLocation3D()
	 * @deprecated use {@link Position3D#getLocation3D()
	 *             getPosition3D().getLocation3D()}
	 */
	public IVector3f getLocation3D() {

		return position3D.getLocation3D();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#getLocationMatrix()
	 * @deprecated use {@link Position3D#getLocationMatrix()
	 *             getPosition3D().getLocationMatrix()}
	 */
	public IMatrix4f getLocationMatrix() {

		return position3D.getLocationMatrix();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#getMatrixState()
	 * @deprecated use {@link Position3D#getMatrixState()
	 *             getPosition3D().getMatrixState()}
	 */
	public MatrixState getMatrixState() {

		return position3D.getMatrixState();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#getModelMatrix()
	 * @deprecated use {@link Position3D#getModelMatrix()
	 *             getPosition3D().getModelMatrix()}
	 */
	public IMatrix4f getModelMatrix() {

		return position3D.getModelMatrix();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#getParaxialBoundingBox()
	 */
	@SuppressWarnings("unchecked")
	public IParaxialBoundingBox getParaxialBoundingBox() {

		if (m_paraxialBounds == null)
			m_paraxialBounds = helper.getParaxialBoundingBox();

		return m_paraxialBounds;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.IHost3D#getParentHost3D()
	 */
	public IHost3D getParentHost3D() {

		return getAncestor3D();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.IHost3D#getPosition3D()
	 */
	public Position3D getPosition3D() {

		return position3D;
	}

	/**
	 * Returns preferred 3D size, this size is synchronized with 2D dimension.
	 * Actually the returned vector is a synchronized version of the 2D object.
	 * {@inheritDoc}
	 * <p>
	 * Internal note: preferredSize object is created lazily if it has not been
	 * created before
	 * </p>
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#getPreferredSize3D()
	 */
	public IVector3f getPreferredSize3D() {

		if (preferredSize3D == null) {
			preferredSize3D = new SyncedVector3f();
		}
		return preferredSize3D.getVector3f(getPreferredSize());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#getRenderContext()
	 */
	public RenderContext getRenderContext() {

		return getAncestor3D().getRenderContext();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#getRotation3D()
	 * @deprecated use {@link Position3D#getRotation3D()
	 *             getPosition3D().getRotation3D()}
	 */
	public IVector3f getRotation3D() {

		return position3D.getRotation3D();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#getScene()
	 */
	public IScene getScene() {

		return Figure3DHelper.getAncestor3D(getParent()).getScene();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#getSize3D()
	 * @deprecated use {@link Position3D#getSize3D()
	 *             getPosition3D().getSize3D()}
	 */
	public IVector3f getSize3D() {

		return position3D.getSize3D();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure2DHost3D#getSurface()
	 */
	public ISurface getSurface() {

		return null;
	}

	/**
	 * Returns always true -- clipping and optimized redraw of 3D figures is
	 * handled differently. There is no way to determine whether a 3D figure
	 * intersects with a 2D rectangle. Since this method is used during redraw,
	 * it must return true in order to get 3D figures painted if they are
	 * children of 2D figures. Possible 2D parents include layers and panes,
	 * which are in fact only structural nodes and not real figures.
	 * 
	 * @todo is this really ok?
	 * @see org.eclipse.draw2d.Figure#intersects(org.eclipse.draw2d.geometry.Rectangle)
	 */
	@Override
	public boolean intersects(Rectangle i_rect) {

		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Overridden to invalidate position.
	 * </p>
	 * 
	 * @see org.eclipse.draw2d.Figure#invalidate()
	 */
	@Override
	public void invalidate() {

		position3D.invalidateMatrices();
		super.invalidate();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#invalidateParaxialBounds()
	 */
	public void invalidateParaxialBounds() {

		m_paraxialBounds = null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#invalidateParaxialBoundsTree()
	 */
	public void invalidateParaxialBoundsTree() {

		helper.invalidateParaxialBoundsTree();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Overridden to mark 2D components invalid, too.
	 * </p>
	 * 
	 * @see org.eclipse.draw2d.Figure#invalidateTree()
	 */
	@Override
	public void invalidateTree() {

		repaint2DComponents = true;
		super.invalidateTree();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method is overridden here because it is neccessary to change the
	 * order of paint operations: The client area must be painted before the
	 * figure itself is painted, because otherwise the texture will not have
	 * been painted yet before it needs to be rendered.
	 * </p>
	 * 
	 * @see org.eclipse.draw2d.Figure#paint(org.eclipse.draw2d.Graphics)
	 */
	@Override
	public void paint(Graphics i_graphics) {

		paintBorder(i_graphics);
		paintClientArea(i_graphics);
		paintFigure(i_graphics);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Figure#paintBorder(org.eclipse.draw2d.Graphics)
	 */
	@Override
	protected void paintBorder(Graphics i_graphics) {

		helper.paintBorder(i_graphics);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Figure#paintChildren(org.eclipse.draw2d.Graphics)
	 */
	@Override
	protected void paintChildren(Graphics i_graphics) {

		helper.paintChildren(i_graphics);
		repaint2DComponents = false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Figure#paintFigure(org.eclipse.draw2d.Graphics)
	 */
	@Override
	protected void paintFigure(Graphics i_graphics) {

		helper.paintFigure(i_graphics);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.geometry.IHost3D#positionChanged(java.util.EnumSet,
	 *      org.eclipse.draw3d.geometry.IVector3f)
	 */
	public void positionChanged(EnumSet<PositionHint> i_hint, IVector3f delta) {

		boolean bFigureMoved = false;

		if (i_hint.contains(PositionHint.SIZE)) { // from old setSize3D method
			if (!(delta.getX() == 0 && delta.getY() == 0)
				&& (delta.getZ() != 0)) {
				invalidate();
				bFigureMoved = true;
			}
		}

		if (i_hint.contains(PositionHint.ROTATION)) { // from old setRotation3D
			bFigureMoved = true;
		}

		if (i_hint.contains(PositionHint.LOCATION)) { // from old setLocation3D
			if (!(delta.getX() == 0 && delta.getY() == 0)
				&& (delta.getZ() != 0)) {
				bFigureMoved = true;
			}
		}

		if (bFigureMoved) {
			fireFigureMoved();
			repaint();
		}

	}

	/**
	 * The default implementation renders nothing.
	 * 
	 * @see org.eclipse.draw3d.Renderable#prerender(org.eclipse.draw3d.RenderContext)
	 */
	public void prerender(RenderContext renderContext) {

		// nothing to do
	}

	@Override
	public void remove(IFigure i_figure) {

		super.remove(i_figure);
		if (!(i_figure instanceof IFigure3D))
			i_figure.removeFigureListener(childMovedListener);
	}

	/**
	 * The default implementation renders nothing.
	 * 
	 * @see org.eclipse.draw3d.Renderable#render(org.eclipse.draw3d.RenderContext)
	 */
	public void render(RenderContext renderContext) {

		// nothing to do
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Figure#revalidate()
	 */
	@Override
	public void revalidate() {

		super.revalidate();
		helper.revalidate();
	}

	/**
	 * @see org.eclipse.draw3d.IFigure3D#setAlpha(int)
	 */
	public void setAlpha(int i_alpha) {

		m_alpha = i_alpha;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#setLocation3D(org.eclipse.draw3d.geometry.IVector3f)
	 * @deprecated use {@link Position3D#setLocation3D(IVector3f)
	 *             getPosition3D().setLocation3D(point3D)}
	 */
	public void setLocation3D(IVector3f i_point) {

		position3D.setLocation3D(i_point);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Internal note: preferredSize object is created lazily if it has not been
	 * created before
	 * </p>
	 * 
	 * @param i_preferredSize3D new preferred size, must not be null and all
	 *            values must not be less 0
	 * @see org.eclipse.draw3d.IFigure3D#setPreferredSize3D(org.eclipse.draw3d.geometry.IVector3f)
	 */
	public void setPreferredSize3D(IVector3f i_preferredSize3D) {

		if (i_preferredSize3D == null) // parameter precondition
			throw new NullPointerException("i_preferredSize3D must not be null");
		if (i_preferredSize3D.getX() < 0 || i_preferredSize3D.getY() < 0
			|| i_preferredSize3D.getZ() < 0) // parameter
			// precondition
			throw new IllegalArgumentException(
				"no value of given vector must be less 0, , was "
					+ i_preferredSize3D);

		if (preferredSize3D == null) {
			preferredSize3D = new SyncedVector3f();
		}
		Dimension size =
			preferredSize3D.setVector3fAsDimension(i_preferredSize3D);
		setPreferredSize(size);

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#setRotation3D(org.eclipse.draw3d.geometry.IVector3f)
	 * @deprecated use {@link Position3D#setRotation3D(IVector3f)
	 *             getPosition3D().setRotation3D(rotation)}
	 */
	public void setRotation3D(IVector3f i_rotation) {

		position3D.setRotation3D(i_rotation);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @param i_size new size, must not be null, no value must be less 0
	 * @see org.eclipse.draw3d.IFigure3D#setSize3D(org.eclipse.draw3d.geometry.IVector3f)
	 * @deprecated use {@link Position3D#setSize3D(IVector3f)
	 *             getPosition3D().setSize3D(size)}
	 */
	public void setSize3D(IVector3f i_size) {

		position3D.setSize3D(i_size);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		StringBuffer strb = new StringBuffer();
		strb.append(this.getClass().getName()).append(" at (");
		strb.append(getLocation3D().getX()).append(",").append(
			getLocation3D().getY()).append(",").append(getLocation3D().getZ())
			.append(")");
		strb.append(", size (");
		strb.append(getSize3D().getX()).append(",").append(getSize3D().getY())
			.append(",").append(getSize3D().getZ()).append(")");
		return strb.toString();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#transformFromParent(org.eclipse.draw3d.geometry.Transformable)
	 */
	public void transformFromParent(Transformable i_transformable) {

		Position3DUtil.transformFromParent(position3D, i_transformable);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#transformToAbsolute(org.eclipse.draw3d.geometry.Transformable)
	 */
	public void transformToAbsolute(Transformable io_transformable) {

		Position3DUtil.transformToAbsolute(position3D, io_transformable);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#transformToParent(org.eclipse.draw3d.geometry.Transformable)
	 */
	public void transformToParent(Transformable io_transformable) {

		Position3DUtil.transformToParent(position3D, io_transformable);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw3d.IFigure3D#transformToRelative(org.eclipse.draw3d.geometry.Transformable)
	 */
	public void transformToRelative(Transformable io_transformable) {

		Position3DUtil.transformToRelative(position3D, io_transformable);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Figure#translateFromParent(org.eclipse.draw2d.geometry.Translatable)
	 */
	@Override
	public void translateFromParent(Translatable i_t) {

		// do nothing
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Figure#translateToParent(org.eclipse.draw2d.geometry.Translatable)
	 */
	@Override
	public void translateToParent(Translatable i_t) {

		// do nothing
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Figure#useLocalCoordinates()
	 */
	@Override
	protected boolean useLocalCoordinates() {

		// otherwise, 2D children get drawn at their absolute positions and are
		// not visible on the texture
		return true;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.draw2d.Figure#validate()
	 */
	@Override
	public void validate() {

		super.validate();
		repaint2DComponents = true;
	}
}
