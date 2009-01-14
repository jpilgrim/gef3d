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

package org.eclipse.gef3d.ext.multieditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

/**
 * MultiEditorPartFactory is an extension for Gef3D to enable multiple kind of
 * editors to be shown in a single Gef3D editor. This factory serves as a
 * distributor, sending a creation request to the appropriate "normal" factory
 * attached to this multi part factory. This factory must be used in combination
 * with {@link MultiEditPart}, i.e. the parent edit part of added root contexts
 * must be an instance of MultiEditPart.
 * <p>
 * "Normal" factories are identified by the context or root context, which
 * is the normal behavior in most cases. When initializing an editor, the
 * root context itself, usually the top level container, is also created by
 * a factory. For this case, factories can be prepared using the model element
 * as a key. When the edit part for this root model element was created, the
 * factory is remapped, i.e. it is identified in future calls by the edit part
 * (which is simply the normal behavior).
 * <p>
 * For enabling multiple editors to be combined even if they are displaying
 * the same model or elements within the same edit parts as other editors
 * (which is true for marker model editors or, more important, intermodel 
 * editors), several factories can be mapped to a single root model or 
 * context edit part. The factories mapped to the same key element are weighted.
 * The factory set is then called in the descending order of its weights, and
 * the first part which is created is used. Usually, factories creating 
 * edit parts for 2D editors (which figures are projected on planes) are added
 * using a low weight, e.g. by simply adding them via 
 * {@link #add(EditPart, EditPartFactory)}
 * or {@link #prepare(Object, EditPartFactory)}. Intermodel factories are then
 * added using a high weight via {@link #add(EditPart, EditPartFactory, int)} 
 * with {@link #HIGHEST_PRIORITY}. 
 * 
 * @author Jens von Pilgrim
 * @version $Revision$
 * @since 14.01.2008
 */
public class MultiEditorPartFactory implements EditPartFactory {

	public final static int LOWEST_PRIORITY = Integer.MIN_VALUE;

	public final static int HIGHEST_PRIORITY = Integer.MAX_VALUE;

	private class WeightedFactory implements Comparable<WeightedFactory> {
		int weight;

		EditPartFactory factory;

		/**
		 * @param i_weight
		 * @param i_factory
		 */
		public WeightedFactory(int i_weight, EditPartFactory i_factory) {
			super();
			weight = i_weight;
			factory = i_factory;
		}

		/**
		 * {@inheritDoc} This method does not work as specified in
		 * {@link Object}, thus this class is private. Especially it is not
		 * transitive if weights are equal.
		 * 
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(WeightedFactory i_o) {
			if (i_o.weight == weight) {
				return (i_o.factory == factory) ? 0 : -1;
			}
			return (i_o.weight < weight) ? -1 : 1;
		}

		/** 
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return weight + ": " +factory;
		}
		
		
	}

	private class FactorySet extends TreeSet<WeightedFactory> {

		/**
		 * @param i_context
		 * @param i_model
		 * @return
		 */
		public EditPart createEditPart(EditPart i_context, Object i_model) {
			EditPart part = null;
			for (WeightedFactory wf : this) {
				part = wf.factory.createEditPart(i_context, i_model);
				if (part != null)
					break;
			}
			return part;
		}

		
		
	}

	/**
	 * Logger for this class
	 */
	private static final Logger log = Logger
			.getLogger(MultiEditorPartFactory.class.getName());

	Map<EditPart, FactorySet> m_delegatedFactories;

	Map<Object, FactorySet> m_preparedFactories;

	// EditPartFactory defaultFactory = null;

	/**
	 * 
	 */
	public MultiEditorPartFactory() { // EditPartFactory i_defaultFactory) {
		m_delegatedFactories = new HashMap<EditPart, FactorySet>();
		m_preparedFactories = new HashMap<Object, FactorySet>();
		// defaultFactory = i_defaultFactory;
	}

	/**
	 * Calls {@link #add(EditPart, EditPartFactory, int)} with 
	 * {@link #LOWEST_PRIORITY}. .
	 * 
	 * @param i_rootContext
	 * @param i_factory
	 */
	public void add(EditPart i_rootContext, EditPartFactory i_factory) {
		add(i_rootContext, i_factory, LOWEST_PRIORITY);
	}

	/**
	 * Adds a new "normal" edit part factory which is called whenever a context
	 * edit part in {@link #createEditPart(EditPart, Object)} is (transitive)
	 * child of given root context. The first context edit part added will
	 * become the primary context, i.e. if context is null in
	 * {@link #createEditPart(EditPart, Object)}, this primary context will be
	 * used. The primary context must not be removed! Parent of a root context
	 * must be an instance of {@link MultiEditPart}. This is not checked here
	 * since parent may be null at the time adding the edit part here. This
	 * condition is checked in {@link #createEditPart(EditPart, Object)}.
	 * {@link MultiEditPart}
	 * 
	 * @param i_rootContext, must not be null
	 * @param i_factory, must not be null
	 * @param i_weight priority
	 */
	public void add(EditPart i_rootContext, EditPartFactory i_factory,
			int i_weight) {

		if (i_factory == null) {
			throw new NullPointerException(
					"(Edit Part) Factory must not be null");
		}
		if (i_rootContext == null) {
			throw new NullPointerException(
					"Root context edit part must not be null");
		}

		FactorySet fs = m_delegatedFactories.get(i_rootContext);
		if (fs == null) {
			fs = new FactorySet();
			m_delegatedFactories.put(i_rootContext, fs);
		}
		fs.add(new WeightedFactory(i_weight, i_factory));

	}
	
	/**
	 * Calls {@link #prepare(Object, EditPartFactory, int)} with 
	 * {@link #LOWEST_PRIORITY}
	 * @param model
	 * @param i_factory
	 */
	public void prepare(Object model, EditPartFactory i_factory) {
		prepare(model, i_factory, LOWEST_PRIORITY);
	}

	/**
	 * Prepares a factory for which no edit part was created yet. This is
	 * usually necessary only for the root model elements, i.e. the
	 * modelContainer.
	 * 
	 * @param model
	 * @param i_factory
	 */
	public void prepare(Object model, EditPartFactory i_factory, int i_weight) {
		if (i_factory == null) {
			throw new NullPointerException("Factory must not be null");
		}

		// was model already resolved, i.e. does a part already exists?
		for (EditPart part : m_delegatedFactories.keySet()) {
			if (part.getModel() == model) {
				FactorySet fs = m_delegatedFactories.get(part);
				fs.add(new WeightedFactory(i_weight, i_factory));
				return;
			}
		}

		// else prepare factory
		FactorySet fs = m_preparedFactories.get(model);
		if (fs == null) {
			fs = new FactorySet();
			m_preparedFactories.put(model, fs);
		}
		fs.add(new WeightedFactory(i_weight, i_factory));

		m_preparedFactories.put(model, fs);
	}

	/**
	 * Removes given root context and returns associated factories.
	 * 
	 * @param i_rootContext to be removed
	 * @return associated factory of given root context
	 * @throws IllegalStateException if given root context is primary context
	 */
	public List<EditPartFactory> remove(EditPart i_rootContext) {
		FactorySet fs = m_delegatedFactories.remove(i_rootContext);
		List<EditPartFactory> list = new ArrayList<EditPartFactory>();
		for (WeightedFactory wf : fs) {
			list.add(wf.factory);
		}
		return list;
	}

	/**
	 * Creates a edit part by passing the request to one of the formerly added
	 * factories. If given context is null, the primary context's factory is
	 * used. This mechanism requires that a transitive parent of the given
	 * context (if not null) was formerly added as root context in
	 * {@link #add(EditPart, EditPartFactory)}.
	 * 
	 * @group Overriding methods
	 * @see org.eclipse.gef.EditPartFactory#createEditPart(org.eclipse.gef.EditPart,
	 *      java.lang.Object)
	 */
	public EditPart createEditPart(EditPart i_context, Object i_model) {

		// the multi editor base model, actually a list of the real root models
		if (i_model instanceof MultiEditorModelContainer) {
			// if (log.isLoggable(Level.INFO)) {
			// log.info("create multi edit part"); //$NON-NLS-1$
			// }

			EditPart part = new MultiEditorModelContainerEditPart();
			part.setModel(i_model);
			return part;
		}

		FactorySet fs = null;
		EditPart part = null;
		if (i_context != null) { // the usual way, find by context

			fs = findFactoriesByContext(i_context);

			if (fs != null) {
				part = fs.createEditPart(i_context, i_model);
				// if (log.isLoggable(Level.INFO)) {
				// log.info("create part by context"); //$NON-NLS-1$
				// }
			}

		}

		if (part == null) { // no context or no factory was able to create
			// a part? maybe a factory is prepared
			// for that:
			// this is usually the case with modelContainer, i.e. the real root
			// models
			fs = m_preparedFactories.get(i_model);
			if (fs != null) {
				part = fs.createEditPart(i_context, i_model);
				if (part != null) {
					// ok, now all prepared factories can be moved to
					// the delegate factory list

					m_preparedFactories.remove(i_model);
					FactorySet dfs = m_delegatedFactories.get(part);
					if (dfs == null) {
						m_delegatedFactories.put(part, fs);
					} else {
						dfs.addAll(fs);
					}

					// TODO remove
					// m_preparedFactories.remove(i_model);

					// if (log.isLoggable(Level.INFO)) {
					// log.info("create part by model"); //$NON-NLS-1$
					// }

				}

			}
		}

		if (fs == null) {
			throw new IllegalStateException("No root context or "
					+ "factory found for model " + i_model + "; context "
					+ i_context);
		}

		// if (part == null) {
		// if (log.isLoggable(Level.INFO)) {
		// log.info("no part created"); //$NON-NLS-1$
		// }
		// }

		return part;

	}

	/**
	 * @param i_context
	 * @return FactorySet for given context, may return null if context not
	 *         registered
	 */
	private FactorySet findFactoriesByContext(EditPart i_context) {
		if (i_context == null) {
			return null;
		}
		FactorySet fs = m_delegatedFactories.get(i_context);
		if (fs == null) {
			fs = findFactoriesByContext(i_context.getParent());
		}
		return fs;
	}

}