/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
 *  https://github.com/dazzle50/JTableFX                                  *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.table.view.axis;

import java.util.Arrays;

/*************************************************************************************************/
/**************** Efficient cache for storing start pixel positions of axis cells ****************/
/*************************************************************************************************/

/**
 * Provides an efficient cache for storing the starting pixel coordinates of axis cells (rows or
 * columns). The cache maintains a monotonically increasing sequence of pixel positions, where each
 * entry represents the starting pixel coordinate of a cell at a given index.
 * <p>
 * This cache is used to optimise coordinate-to-cell index lookups and cell position calculations
 * in table views. Rather than recalculating pixel positions from scratch each time, previously
 * calculated positions are stored and reused. The cache is built incrementally on-demand as cells
 * are accessed or rendered.
 * <p>
 * The cache automatically manages its capacity, growing as needed with a 25% growth factor to
 * balance memory usage and reallocations. It supports efficient binary search via
 * {@link #getIndex(int)} to quickly determine which cell corresponds to a given pixel coordinate.
 * <p>
 * Cache invalidation occurs when axis properties change (e.g., cell sizes, zoom level, or cell
 * visibility), at which point the cache can be cleared or truncated as appropriate.
 * 
 * @see AxisLayout
 */
public class PixelCache
{
  private int[] m_cache = new int[0];
  private int   m_size  = 0;

  /******************************************** clear ********************************************/
  /**
   * Clears the cache, resetting the size to zero whilst retaining the allocated array capacity
   * for potential reuse. This is more efficient than creating a new array when the cache will be
   * repopulated.
   */
  public void clear()
  {
    // reset size to zero (keep array allocated for reuse)
    m_size = 0;
  }

  /********************************************* size ********************************************/
  /**
   * Returns the number of valid entries currently stored in the cache.
   * 
   * @return The current cache size (number of valid entries)
   */
  public int size()
  {
    // return number of valid entries
    return m_size;
  }

  /********************************************* get *********************************************/
  /**
   * Returns the cached pixel value at the specified index position.
   * 
   * @param index
   *          The position in the cache to retrieve (must be >= 0 and < size)
   * @return The pixel value stored at the specified index
   * @throws IndexOutOfBoundsException
   *           if index is negative or >= size
   */
  public int get( int index )
  {
    // check index is valid
    if ( index < 0 || index >= m_size )
      throw new IndexOutOfBoundsException( "index=" + index + " size=" + m_size );

    // return cached value
    return m_cache[index];
  }

  /********************************************* add *********************************************/
  /**
   * Adds a new pixel value to the end of the cache, automatically expanding the internal array
   * capacity if necessary.
   * 
   * @param value
   *          The pixel value to append to the cache
   */
  public void add( int value )
  {
    // ensure array has capacity for one more entry
    ensureCapacity( m_size + 1 );

    // add value and increment size
    m_cache[m_size++] = value;
  }

  /****************************************** truncate *******************************************/
  /**
   * Truncates the cache to the specified size if the current size exceeds it. If the current size
   * is already less than or equal to the new size, no action is taken. This is useful when cells
   * are removed or resized, invalidating later cache entries.
   * 
   * @param newSize
   *          The maximum size for the cache
   */
  public void truncate( int newSize )
  {
    // truncate cache if current size is larger
    if ( m_size > newSize )
      m_size = newSize;
  }

  /*************************************** ensureCapacity ****************************************/
  private void ensureCapacity( int minCapacity )
  {
    // ensure array has at least the minimum capacity
    if ( minCapacity > m_cache.length )
    {
      // grow by 25% or to minimum capacity, whichever is larger
      int newCapacity = Math.max( minCapacity, m_cache.length + ( m_cache.length >> 2 ) + 5 );
      m_cache = Arrays.copyOf( m_cache, newCapacity );
    }
  }

  /****************************************** getIndex *******************************************/
  /**
   * Performs a binary search to find the largest index whose cached pixel value is less than or
   * equal to the specified target value. This is used to determine which cell corresponds to a
   * given pixel coordinate.
   * 
   * @param value
   *          The pixel value to search for
   * @return The index of the largest cached value <= value, or -1 if all cached values are greater
   *         than the target
   */
  public int getIndex( int value )
  {
    // binary search returns (-(insertion point) - 1) if not found
    int result = Arrays.binarySearch( m_cache, 0, m_size, value );

    // if exact match or insertion point, adjust to get largest index <= value
    return result >= 0 ? result : -result - 2;
  }

  /****************************************** isEmpty ********************************************/
  /**
   * Returns true if the cache contains no valid entries.
   * 
   * @return true if the cache is empty (size == 0), false otherwise
   */
  public boolean isEmpty()
  {
    // check if cache has no entries
    return m_size == 0;
  }

}