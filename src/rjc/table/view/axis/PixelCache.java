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
/********************* Cache for storing start pixel positions of axis cells *********************/
/*************************************************************************************************/

public class PixelCache
{
  private int[] m_cache = new int[0];
  private int   m_size  = 0;

  /******************************************** clear ********************************************/
  /**
   * Clears the cache, resetting size to zero.
   */
  public void clear()
  {
    // reset size to zero (keep array allocated for reuse)
    m_size = 0;
  }

  /********************************************* size ********************************************/
  /**
   * Returns the number of valid entries in the cache.
   * 
   * @return Current cache size
   */
  public int size()
  {
    // return number of valid entries
    return m_size;
  }

  /********************************************* get *********************************************/
  /**
   * Returns the cached pixel value at the specified index.
   * 
   * @param index Position in cache to retrieve
   * @return Pixel value at index
   * @throws IndexOutOfBoundsException if index is negative or >= size
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
   * Adds a new pixel value to the end of the cache.
   * 
   * @param value Pixel value to add
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
   * Truncates the cache to the specified size if current size is larger.
   * 
   * @param newSize Maximum size for cache
   */
  public void truncate( int newSize )
  {
    // truncate cache if current size is larger
    if ( m_size > newSize )
      m_size = newSize;
  }

  /*************************************** ensureCapacity ****************************************/
  /**
   * Ensures the internal array has at least the specified capacity.
   * 
   * @param minCapacity Minimum required capacity
   */
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
   * Performs binary search to find the index whose cached value is less than or equal to the
   * target value.
   * 
   * @param value Value to search for
   * @return Index of largest cached value <= targetValue, or -1 if all values are greater
   */
  public int getIndex( int value )
  {
    // find position by binary search
    int startPos = 0;
    int endPos = m_size;
    while ( startPos != endPos )
    {
      int midPos = ( endPos + startPos ) / 2;
      if ( m_cache[midPos] <= value )
        startPos = midPos + 1;
      else
        endPos = midPos;
    }
    return startPos - 1;
  }

  /****************************************** isEmpty ********************************************/
  /**
   * Returns true if the cache contains no entries.
   * 
   * @return true if cache is empty
   */
  public boolean isEmpty()
  {
    // check if cache has no entries
    return m_size == 0;
  }

}