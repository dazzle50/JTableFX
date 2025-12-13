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

/**
 * Compact storage for axis data-index nominal sizes and hidden state using signed short values.
 * <p>
 * This class provides memory-efficient storage for per-index sizing information in table axes.
 * Rather than storing size information for full axis, it uses array that grows dynamically as
 * needed to cover last non-defaulted sized entry.
 * Each entry is stored as a short (2 bytes) rather than an int (4 bytes) to minimise memory usage.
 * <p>
 * The storage scheme uses signed short values with special semantics:
 * <ul>
 * <li>{@link #DEFAULT} (Short.MAX_VALUE) - index uses default size</li>
 * <li>Positive value - index has explicit nominal size and is visible</li>
 * <li>Negative value - index is hidden (absolute value is the size)</li>
 * </ul>
 * <p>
 * This encoding allows both size and visibility to be stored in a single value, reducing memory
 * overhead and simplifying operations that need to preserve hidden state whilst changing sizes.
 * 
 * @see AxisLayout
 */
public class IndexSize
{
  // constant indicating index should use default size
  public static final short DEFAULT = Short.MAX_VALUE;

  // compact storage: positive = visible size, negative = hidden (abs is size), DEFAULT = use default
  private short[] m_sizes;

  /**************************************** constructor ******************************************/
  /**
   * Creates with no stored size or visibility information. 
   */
  public IndexSize()
  {
    // initialise with no stored values
    reset();
  }

  /******************************************** reset ********************************************/
  /**
   * Clears all stored size and visibility information, resetting to empty state.
   */
  public void reset()
  {
    // reset to empty array
    m_sizes = new short[0];
  }

  /*************************************** ensureCapacity ****************************************/
  /**
   * Expands internal array capacity to accommodate the specified index if needed.
   * 
   * @param requiredCapacity
   *          minimum required index capacity
   */
  private void ensureCapacity( int requiredCapacity )
  {
    // expand array if index exceeds current capacity
    if ( requiredCapacity >= m_sizes.length )
    {
      // calculate new capacity with 25% growth factor
      int newCapacity = Math.max( requiredCapacity + 4, m_sizes.length + ( m_sizes.length >> 2 ) );
      short[] newSizes = new short[newCapacity];
      System.arraycopy( m_sizes, 0, newSizes, 0, m_sizes.length );
      // initialise new slots to default
      for ( int i = m_sizes.length; i < newSizes.length; i++ )
        newSizes[i] = DEFAULT;
      m_sizes = newSizes;
    }
  }

  /*************************************** setNominalSize ****************************************/
  /**
   * Sets the nominal size for the specified index, preserving hidden state if already hidden.
   * 
   * @param dataIndex
   *          the cell index (data index, not view index)
   * @param size
   *          the size value (use DEFAULT for default size)
   */
  public void setNominalSize( int dataIndex, short size )
  {
    // expand array capacity if needed
    ensureCapacity( dataIndex );

    // preserve hidden state by negating size if currently hidden
    if ( m_sizes[dataIndex] < 0 )
      m_sizes[dataIndex] = (short) -size;
    else
      m_sizes[dataIndex] = size;
  }

  /*************************************** getNominalSize ****************************************/
  /**
   * Gets the nominal size (DEFAULT if default, negative if hidden) for the specified index.
   * 
   * @param dataIndex
   *          the cell index (data index, not view index)
   * @return the size value (DEFAULT if default, negative if hidden)
   */
  public short getNominalSize( int dataIndex )
  {
    // return default if index beyond stored range
    if ( dataIndex >= m_sizes.length )
      return DEFAULT;

    // return stored value (negative indicates hidden)
    return m_sizes[dataIndex];
  }

  /*************************************** clearIndexSize ****************************************/
  /**
   * Clears the size exception for the specified index, reverting to default whilst preserving
   * hidden state.
   * 
   * @param dataIndex
   *          the cell index to clear
   * @return true if the size was changed, false if already default or out of bounds
   */
  public boolean resetNominalSize( int dataIndex )
  {
    // no change needed if beyond array or already default
    if ( dataIndex >= m_sizes.length || m_sizes[dataIndex] == DEFAULT || m_sizes[dataIndex] == -DEFAULT )
      return false;

    // revert to default whilst preserving hidden state
    if ( m_sizes[dataIndex] < 0 )
      m_sizes[dataIndex] = (short) -DEFAULT;
    else
      m_sizes[dataIndex] = DEFAULT;

    return true;
  }

  /************************************** applyMinimumSize ***************************************/
  /**
   * Ensures all size exceptions meet the specified minimum nominal size, preserving hidden state.
   * 
   * @param minSize
   *          the minimum size to enforce
   */
  public void applyMinimumSize( short minSize )
  {
    // scan all stored sizes and enforce minimum
    for ( int i = 0; i < m_sizes.length; i++ )
    {
      short size = m_sizes[i];
      short absSize = size < 0 ? (short) -size : size;
      // only adjust non-default sizes below minimum
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
   * Hides the specified index by negating its size value.
   * 
   * @param dataIndex
   *          the cell index to hide
   * @return true if hidden state was changed, false if already hidden
   */
  public boolean hide( int dataIndex )
  {
    // expand array capacity if needed
    ensureCapacity( dataIndex );

    // no change if already hidden (negative)
    if ( m_sizes[dataIndex] < 0 )
      return false;

    // negate to mark as hidden
    m_sizes[dataIndex] = (short) -m_sizes[dataIndex];
    return true;
  }

  /****************************************** unhide *********************************************/
  /**
   * Unhides the specified index by making its size value positive.
   * 
   * @param dataIndex
   *          the cell index to unhide
   * @return true if hidden state was changed, false if already visible or out of bounds
   */
  public boolean unhide( int dataIndex )
  {
    // no change if beyond array or already visible (non-negative)
    if ( dataIndex >= m_sizes.length || m_sizes[dataIndex] >= 0 )
      return false;

    // negate to mark as visible
    m_sizes[dataIndex] = (short) -m_sizes[dataIndex];
    return true;
  }

  /***************************************** unhideAll *******************************************/
  /**
   * Unhides all currently hidden indices.
   * 
   * @return set of data-indices that were unhidden (null if none were hidden)
   */
  public HashSetInt unhideAll()
  {
    // find and unhide all negative (hidden) values
    var unhidden = new HashSetInt();
    for ( int i = 0; i < m_sizes.length; i++ )
      if ( m_sizes[i] < 0 )
      {
        m_sizes[i] = (short) -m_sizes[i];
        unhidden.add( i );
      }

    return unhidden.isEmpty() ? null : unhidden;
  }

  /************************************** getHiddenIndexes ***************************************/
  /**
   * Returns a set containing all currently hidden cell indices.
   * 
   * @return set of hidden cell data-indices (empty if none hidden)
   */
  public HashSetInt getHiddenIndexes()
  {
    // collect all indices with negative (hidden) values
    var hidden = new HashSetInt();
    for ( int i = 0; i < m_sizes.length; i++ )
      if ( m_sizes[i] < 0 )
        hidden.add( i );

    return hidden;
  }

  /************************************** getSizeExceptions **************************************/
  /**
   * Returns a map of all size exceptions (excluding DEFAULT values).
   * 
   * @return map of data-index to absolute size for all non-default sizes
   */
  public Map<Integer, Short> getSizeExceptions()
  {
    // collect all non-default sizes (return absolute values)
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
   * Clears all size exceptions whilst preserving hidden state for all indices.
   */
  public void clearExceptions()
  {
    // reset all to DEFAULT whilst preserving hidden state
    for ( int i = 0; i < m_sizes.length; i++ )
      if ( m_sizes[i] < 0 )
        m_sizes[i] = (short) -DEFAULT;
      else
        m_sizes[i] = DEFAULT;
  }

  /****************************************** truncate *******************************************/
  /**
   * Truncates the internal array to the specified size, discarding data beyond the new size.
   * 
   * @param newSize
   *          the new maximum index + 1
   */
  public void truncate( int newSize )
  {
    // only truncate if new size is smaller
    if ( newSize < m_sizes.length )
    {
      short[] newSizes = new short[newSize];
      System.arraycopy( m_sizes, 0, newSizes, 0, newSize );
      m_sizes = newSizes;
    }
  }

  /**************************************** reorderIndexes ***************************************/
  /**
   * Reorders the size and hidden data when indices are moved in the view.
   * <p>
   * This handles the complex case where multiple indices are moved together to a new position,
   * requiring all other indices to shift accordingly.
   * 
   * @param sortedIndexes
   *          sorted array of source data-indices being moved
   * @param insertIndex
   *          target position for insertion in data space
   */
  public void reorderIndexes( int[] sortedIndexes, int insertIndex )
  {
    // calculate actual insert position accounting for removed elements
    int insertPosition = insertIndex;
    for ( int sourceIndex : sortedIndexes )
    {
      if ( sourceIndex < insertIndex )
        insertPosition--;
      else
        break;
    }

    // determine required array size after reordering
    int maxIndex = Math.max( m_sizes.length - 1, insertPosition + sortedIndexes.length - 1 );
    if ( maxIndex < 0 )
      return;

    // create new array with default values
    short[] newSizes = new short[maxIndex + 1];
    for ( int i = 0; i < newSizes.length; i++ )
      newSizes[i] = DEFAULT;

    // copy moved indices to their new positions
    for ( int i = 0; i < sortedIndexes.length; i++ )
    {
      int sourceIndex = sortedIndexes[i];
      if ( sourceIndex < m_sizes.length )
        newSizes[insertPosition + i] = m_sizes[sourceIndex];
    }

    // copy non-moved indices to adjusted positions
    for ( int sourceIndex = 0; sourceIndex < m_sizes.length; sourceIndex++ )
    {
      // skip moved indices
      if ( java.util.Arrays.binarySearch( sortedIndexes, sourceIndex ) >= 0 )
        continue;

      // calculate where this index moves to after reordering
      int targetIndex = adjustedIndex( sourceIndex, insertPosition, sortedIndexes );
      if ( targetIndex <= maxIndex )
        newSizes[targetIndex] = m_sizes[sourceIndex];
    }

    m_sizes = newSizes;
  }

  /**************************************** adjustedIndex ****************************************/
  /**
   * Calculates the new index position for a non-moved index after reordering.
   * 
   * @param originalIndex
   *          the original index position
   * @param insertIndex
   *          where moved indices were inserted
   * @param movedSorted
   *          sorted array of indices that were moved
   * @return adjusted index position after reordering
   */
  private int adjustedIndex( int originalIndex, int insertIndex, int[] movedSorted )
  {
    // count how many moved indices were before this index
    int before = 0;
    while ( before < movedSorted.length && movedSorted[before] < originalIndex )
      before++;

    // adjust position based on insertion point
    if ( originalIndex < insertIndex + before )
      return originalIndex - before;

    return originalIndex - before + movedSorted.length;
  }

  /************************************* calculateTotalPixels ************************************/
  /**
   * Calculates the total pixels for all body cells based on default size, zoom factor, size 
   * exceptions, and hidden state.
   * 
   * @param count
   *          total number of indices in the axis
   * @param defaultSize
   *          default size for indices without exceptions
   * @param zoom
   *          zoom factor for pixel scaling
   * @return total pixels for all visible body cells
   */
  public int calculateTotalPixels( int count, int defaultSize, double zoom )
  {
    // start assuming all indices use default size
    int defaultSizeCount = count;
    int customSizePixels = 0;

    // optimised path when zoom is effectively 1.0
    if ( Math.abs( zoom - 1.0 ) < 0.001 )
    {
      // process stored sizes in single pass without zoom multiplication
      for ( int i = 0; i < m_sizes.length && i < count; i++ )
      {
        short size = m_sizes[i];

        if ( size == DEFAULT )
          continue; // uses default, already counted

        if ( size < 0 )
          defaultSizeCount--; // hidden, exclude from total
        else
        {
          // visible with custom size
          defaultSizeCount--;
          customSizePixels += size;
        }
      }

      return customSizePixels + defaultSizeCount * defaultSize;
    }

    // apply zoom factor to all sizes
    for ( int i = 0; i < m_sizes.length && i < count; i++ )
    {
      short size = m_sizes[i];

      if ( size == DEFAULT )
        continue; // uses default, already counted

      if ( size < 0 )
        defaultSizeCount--; // hidden, exclude from total
      else
      {
        // visible with custom size - apply zoom
        defaultSizeCount--;
        customSizePixels += (int) ( size * zoom );
      }
    }

    return customSizePixels + defaultSizeCount * (int) ( defaultSize * zoom );
  }

}