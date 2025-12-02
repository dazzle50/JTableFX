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

import java.util.HashMap;
import java.util.Map;

import rjc.table.HashSetInt;

/*************************************************************************************************/
/***************** Compact storage for axis index nominal sizes and hidden state *****************/
/*************************************************************************************************/

public class IndexSize
{
  // constant indicating default size should be used
  public static final short DEFAULT = Short.MAX_VALUE;

  // short array storing size exceptions and hidden state
  // DEFAULT = use default size
  // negative value = hidden (absolute value is size)
  private short[]           m_sizes = new short[0];

  /**************************************** constructor ******************************************/
  public IndexSize()
  {
    // empty constructor
  }

  /******************************************** clear ********************************************/
  public void clear()
  {
    // reset to empty array
    m_sizes = new short[0];
  }

  /*************************************** ensureCapacity ****************************************/
  private void ensureCapacity( int index )
  {
    // expand array if needed to accommodate index
    if ( index >= m_sizes.length )
    {
      // calculate new capacity with growth factor of 1.25x minimum
      int newCapacity = Math.max( index + 4, m_sizes.length + ( m_sizes.length >> 2 ) + 4 );
      short[] newSizes = new short[newCapacity];
      System.arraycopy( m_sizes, 0, newSizes, 0, m_sizes.length );
      // initialise new elements to default
      for ( int i = m_sizes.length; i < newSizes.length; i++ )
        newSizes[i] = DEFAULT;
      m_sizes = newSizes;
    }
  }

  /****************************************** setSize ********************************************/
  /**
   * Sets the size for the specified index.
   * 
   * @param index
   *          The cell index
   * @param size
   *          The size value (use DEFAULT for default)
   */
  public void setSize( int index, short size )
  {
    // ensure array is large enough
    ensureCapacity( index );

    // preserve hidden state if currently hidden
    if ( m_sizes[index] < 0 )
      m_sizes[index] = (short) -size;
    else
      m_sizes[index] = size;
  }

  /****************************************** getSize ********************************************/
  /**
   * Gets the size for the specified index, negative if hidden.
   * 
   * @param index
   *          The cell index
   * @return The size value (DEFAULT if default)
   */
  public short getSize( int index )
  {
    // return default if index beyond array bounds
    if ( index >= m_sizes.length )
      return DEFAULT;

    // return size, negative if hidden
    return m_sizes[index];
  }

  /***************************************** resetSizeToDefault *******************************************/
  /**
   * Clears the size exception for the specified index, reverting to default.
   * 
   * @param index
   *          The cell index
   * @return true if the size was changed
   */
  public boolean resetSizeToDefault( int index )
  {
    // return false if index beyond array bounds or already default
    if ( index >= m_sizes.length || m_sizes[index] == DEFAULT )
      return false;

    // preserve hidden state when clearing size
    if ( m_sizes[index] < 0 )
      m_sizes[index] = (short) -DEFAULT;
    else
      m_sizes[index] = DEFAULT;

    return true;
  }

  /************************************** enforceMinimumSize *************************************/
  /**
   * Ensures all size exceptions meet the specified minimum size.
   * 
   * @param minSize
   *          The minimum size to enforce
   */
  public void enforceMinimumSize( short minSize )
  {
    // iterate through all stored sizes and enforce minimum
    for ( int i = 0; i < m_sizes.length; i++ )
    {
      short size = m_sizes[i];
      short absSize = size < 0 ? (short) -size : size;
      if ( absSize != DEFAULT && absSize < minSize )
      {
        // preserve hidden state whilst enforcing minimum
        if ( size < 0 )
          m_sizes[i] = (short) -minSize;
        else
          m_sizes[i] = minSize;
      }
    }
  }

  /******************************************* hide **********************************************/
  /**
   * Hides the specified index.
   * 
   * @param index
   *          The cell index
   * @return true if the hidden state was changed
   */
  public boolean hide( int index )
  {
    // ensure array is large enough
    ensureCapacity( index );

    // return false if already hidden
    if ( m_sizes[index] < 0 )
      return false;

    // make negative to indicate hidden
    m_sizes[index] = (short) -m_sizes[index];
    return true;
  }

  /****************************************** unhide *********************************************/
  /**
   * Unhides the specified index.
   * 
   * @param index
   *          The cell index
   * @return true if the hidden state was changed
   */
  public boolean unhide( int index )
  {
    // return false if index beyond array bounds or not hidden
    if ( index >= m_sizes.length || m_sizes[index] >= 0 )
      return false;

    // make positive to indicate visible
    m_sizes[index] = (short) -m_sizes[index];
    return true;
  }

  /***************************************** unhideAll *******************************************/
  /**
   * Unhides all hidden indexes.
   * 
   * @return Set of indexes that were unhidden
   */
  public HashSetInt unhideAll()
  {
    // find all hidden indexes and unhide them
    var shown = new HashSetInt();
    for ( int i = 0; i < m_sizes.length; i++ )
      if ( m_sizes[i] < 0 )
      {
        m_sizes[i] = (short) -m_sizes[i];
        shown.add( i );
      }

    return shown.isEmpty() ? null : shown;
  }

  /************************************** getHiddenIndexes ***************************************/
  /**
   * Returns a set containing all hidden cell indexes.
   * 
   * @return HashSetInt containing all hidden cell indexes
   */
  public HashSetInt getHiddenIndexes()
  {
    // collect all hidden indexes
    var hidden = new HashSetInt();
    for ( int i = 0; i < m_sizes.length; i++ )
      if ( m_sizes[i] < 0 )
        hidden.add( i );

    return hidden;
  }

  /************************************** getSizeExceptions **************************************/
  /**
   * Returns a map of all size exceptions (excluding default sizes).
   * 
   * @return Map of index to size for all non-default sizes
   */
  public Map<Integer, Short> getSizeExceptions()
  {
    // collect all non-default size exceptions
    var exceptions = new HashMap<Integer, Short>();
    for ( int i = 0; i < m_sizes.length; i++ )
    {
      short size = m_sizes[i];
      short absSize = size < 0 ? (short) -size : size;
      if ( absSize != DEFAULT )
        exceptions.put( i, absSize );
    }

    return exceptions;
  }

  /*************************************** clearExceptions ***************************************/
  /**
   * Clears all size exceptions while preserving hidden state.
   */
  public void clearExceptions()
  {
    // reset all sizes to default while preserving hidden state
    for ( int i = 0; i < m_sizes.length; i++ )
      if ( m_sizes[i] < 0 )
        m_sizes[i] = (short) -DEFAULT;
      else
        m_sizes[i] = DEFAULT;
  }

  /****************************************** truncate *******************************************/
  /**
   * Truncates the internal array to the specified size, removing data beyond the new size.
   * 
   * @param newSize
   *          The new maximum index + 1
   */
  public void truncate( int newSize )
  {
    // only truncate if new size is smaller than current
    if ( newSize < m_sizes.length )
    {
      short[] newSizes = new short[newSize];
      System.arraycopy( m_sizes, 0, newSizes, 0, newSize );
      m_sizes = newSizes;
    }
  }

  /**************************************** reorderIndexes ***************************************/
  /**
   * Reorders the size and hidden data based on moved indexes.
   * 
   * @param movedSorted
   *          Sorted array of source indexes being moved
   * @param insertIndex
   *          Target position for insertion
   */
  public void reorderIndexes( int[] movedSorted, int insertIndex )
  {
    // calculate adjusted insert position
    int adjustedTarget = insertIndex;
    for ( int sourceIndex : movedSorted )
    {
      if ( sourceIndex < insertIndex )
        adjustedTarget--;
      else
        break;
    }

    // find maximum index we need to handle
    int maxIndex = Math.max( m_sizes.length - 1, adjustedTarget + movedSorted.length - 1 );
    if ( maxIndex < 0 )
      return;

    // create new array for reordered data
    short[] newSizes = new short[maxIndex + 1];
    for ( int i = 0; i < newSizes.length; i++ )
      newSizes[i] = DEFAULT;

    // copy moved indexes to their new positions
    for ( int i = 0; i < movedSorted.length; i++ )
    {
      int sourceIndex = movedSorted[i];
      if ( sourceIndex < m_sizes.length )
        newSizes[adjustedTarget + i] = m_sizes[sourceIndex];
    }

    // copy non-moved indexes to their adjusted positions
    for ( int sourceIndex = 0; sourceIndex < m_sizes.length; sourceIndex++ )
    {
      // skip if this index was moved
      if ( java.util.Arrays.binarySearch( movedSorted, sourceIndex ) >= 0 )
        continue;

      // calculate adjusted target position
      int targetIndex = adjustedIndex( sourceIndex, adjustedTarget, movedSorted );
      if ( targetIndex <= maxIndex )
        newSizes[targetIndex] = m_sizes[sourceIndex];
    }

    m_sizes = newSizes;
  }

  /**************************************** adjustedIndex ****************************************/
  private int adjustedIndex( int exceptionIndex, int insertIndex, int[] movedSorted )
  {
    // count of moved before exception index
    int before = 0;
    while ( before < movedSorted.length && movedSorted[before] < exceptionIndex )
      before++;

    if ( exceptionIndex < insertIndex + before )
      return exceptionIndex - before;

    return exceptionIndex - before + movedSorted.length;
  }

  /************************************* calculateTotalPixels ************************************/
  /**
   * Calculates the total pixels for all body cells based on default size, non-default, and hidden state.
   * 
   * @param count
   *          Total number of indexes
   * @param defaultSize
   *          Default size for indexes without exceptions
   * @param zoom
   *          Zoom factor for pixel scaling
   * @return Total pixels for all visible body cells
   */
  public int calculateTotalPixels( int count, int defaultSize, double zoom )
  {
    // start with count of all indexes using default size
    int visibleDefaultCount = count;
    int exceptionPixels = 0;

    // when no zoom factor, pixel size equals nominal size
    if ( Math.abs( zoom - 1.0 ) < 0.001 )
    {
      // process all stored sizes in single pass
      for ( int i = 0; i < m_sizes.length && i < count; i++ )
      {
        short size = m_sizes[i];

        if ( size == DEFAULT )
          continue; // uses default, already counted

        if ( size < 0 )
          visibleDefaultCount--; // hidden
        else
        {
          // visible with non-default size
          visibleDefaultCount--;
          exceptionPixels += size;
        }
      }

      return exceptionPixels + visibleDefaultCount * defaultSize;
    }

    // when zoom, need to apply zoom factor to sizes
    for ( int i = 0; i < m_sizes.length && i < count; i++ )
    {
      short size = m_sizes[i];

      if ( size == DEFAULT )
        continue; // uses default, already counted

      if ( size < 0 )
        visibleDefaultCount--; // hidden
      else
      {
        // visible with non-default size
        visibleDefaultCount--;
        exceptionPixels += (int) ( size * zoom );
      }
    }

    return exceptionPixels + visibleDefaultCount * (int) ( defaultSize * zoom );
  }

}