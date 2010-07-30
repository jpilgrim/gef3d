/*******************************************************************************
 * Copyright (c) 2008 Jens von Pilgrim and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Matthias Thiele - initial API and implementation
 ******************************************************************************/
package org.eclipse.draw3d.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * This class provides methods for often used buffers with native byte order.
 * 
 * @author Matthias Thiele
 * @version $Revision$
 * @since 25.11.2008
 */

public class BufferUtils {

	/**
	 * Creates a byte buffer with the specified number of elements. Elements
	 * will be stored in native byte. The created buffer will be direct.
	 * 
	 * @param size The number of bte elements which shall fit into the created
	 *            buffer.
	 * @return A ByteBuffer of specified size.
	 */
	public static ByteBuffer createByteBuffer(int size) {
		return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
	}

	/**
	 * Creates a double buffer with the specified number of elements. Elements
	 * will be stored in native byte order as the buffer is a view on an
	 * ByteBuffer with native byte order. The created buffer will be direct.
	 * 
	 * @param size The number of double elements which shall fit into the
	 *            created buffer.
	 * @return A DoubleBuffer of specified size.
	 */
	public static DoubleBuffer createDoubleBuffer(int size) {
		return createByteBuffer(size * (Double.SIZE / Byte.SIZE)).asDoubleBuffer();
	}

	/**
	 * Creates a float buffer with the specified number of elements. Elements
	 * will be stored in native byte order as the buffer is a view on an
	 * ByteBuffer with native byte order. The created buffer will be direct.
	 * 
	 * @param size The number of float elements which shall fit into the created
	 *            buffer.
	 * @return A FloatBuffer of specified size.
	 */
	public static FloatBuffer createFloatBuffer(int size) {
		return createByteBuffer(size * (Float.SIZE / Byte.SIZE)).asFloatBuffer();
	}

	/**
	 * Creates an integer buffer with the specified number of elements. Elements
	 * will be stored in native byte order as the buffer is a view on an
	 * ByteBuffer with native byte order. The created buffer will be direct.
	 * 
	 * @param size The number of integer elements which shall fit into the
	 *            created buffer.
	 * @return An IntBuffer of specified size.
	 */
	public static IntBuffer createIntBuffer(int size) {
		return createByteBuffer(size * (Integer.SIZE / Byte.SIZE)).asIntBuffer();
	}

	/**
	 * Puts the given values into the given buffer, limiting the buffer to the
	 * number of values.
	 * 
	 * @param i_buf the buffer
	 * @param i_values the values
	 * @throws NullPointerException if either of the given arguments is
	 *             <code>null</code>
	 * @throws IllegalArgumentException if the given buffer is too small to hold
	 *             the given values
	 */
	public static void put(IntBuffer i_buf, int... i_values) {
		if (i_buf == null)
			throw new NullPointerException("i_buf must not be null");

		if (i_buf.capacity() < i_values.length)
			throw new IllegalArgumentException(i_buf + " is too small");

		i_buf.limit(i_values.length);
		i_buf.rewind();
		i_buf.put(i_values);
		i_buf.rewind();
	}

}
