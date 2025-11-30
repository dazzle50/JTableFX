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

/*************************************************************************************************/
/********************** Efficient mapping from view indices to data indices **********************/
/*************************************************************************************************/

/**
 * Provides efficient bidirectional mapping between view indices and data indices for table axes.
 * <p>
 * This class maintains a mapping where view indices (positions in the displayed table) map to data
 * indices (positions in the underlying data model). The mapping uses a dynamically-sized array that
 * only stores explicit mappings up to the highest reordered index. The mapped count (m_mappedCount)
 * tracks how many mappings are actively stored, whilst the array length may be larger for future
 * growth. When a view index exceeds the stored range, an identity mapping is assumed (view index
 * equals data index), which optimises memory usage for tables where only some rows/columns have been
 * reordered.
 * <p>
 * The mapping supports:
 * <ul>
 * <li>Bulk reordering operations for efficient row/column drag-and-drop</li>
 * <li>Bidirectional lookups (view→data and data→view)</li>
 * <li>Automatic trimming of trailing identity mappings to minimise memory</li>
 * </ul>
 * <p>
 * The internal array grows dynamically with a 25% growth factor to balance memory efficiency
 * with performance. New entries are initialised to identity mapping values.
 * 
 * @see TableAxis
 * @see AxisLayout
 */
public class IndexMapping
{
  private int[]           m_dataIndices;                  // array of data indices mapped from view indices
  private int             m_mappedCount;                  // count of indices with explicit mappings stored

  public static final int FIRSTCELL = TableAxis.FIRSTCELL;
  public static final int INVALID   = TableAxis.INVALID;

  /**************************************** constructor ******************************************/
  /**
   * Creates a new empty index mapping with minimal initial capacity.
   */
  public IndexMapping()
  {
    // initialise with empty mapping
    m_dataIndices = new int[8];
    m_mappedCount = 0;
  }

  /******************************************** clear ********************************************/
  /**
   * Clears all stored mappings, resetting to identity mapping.
   */
  public void clear()
  {
    // reset to empty state
    m_mappedCount = 0;
  }

  /****************************************** isEmpty ********************************************/
  /**
   * Returns whether the mapping is empty.
   * 
   * @return true if no mappings are stored
   */
  public boolean isEmpty()
  {
    // check if mapped count is zero
    return m_mappedCount == 0;
  }

  /****************************************** getDataIndex ***************************************/
  /**
   * Gets the data index for a given view index.
   * <p>
   * If no explicit mapping exists for the specified view index, the identity mapping is assumed
   * and the view index itself is returned. This optimises memory usage for unmapped indices.
   * 
   * @param viewIndex the view index to look up
   * @return the corresponding data index, or viewIndex if not explicitly mapped
   */
  public int getDataIndex( int viewIndex )
  {
    // return mapped value if within stored range, otherwise identity mapping
    if ( viewIndex < m_dataIndices.length )
      return m_dataIndices[viewIndex];

    return viewIndex;
  }

  /******************************************* append ********************************************/
  /**
   * Appends a data index to the end of the mapping.
   * <p>
   * This extends the logical size of the mapping by one and stores the specified data index at
   * the new position. The internal array is automatically expanded if necessary.
   * 
   * @param dataIndex the data index to append
   */
  public void append( int dataIndex )
  {
    // ensure capacity and store value
    ensureCapacity( m_mappedCount + 1 );
    m_dataIndices[m_mappedCount++] = dataIndex;
  }

  /******************************************* remove ********************************************/
  /**
   * Removes and returns the data index at the specified position.
   * <p>
   * All mappings after the removal position are shifted left by one to fill the gap, and the
   * logical size is decremented.
   * 
   * @param position the position to remove from
   * @return the data index that was removed
   */
  public int remove( int position )
  {
    // get value to return
    int removed = m_dataIndices[position];

    // shift elements left to fill gap
    System.arraycopy( m_dataIndices, position + 1, m_dataIndices, position, m_mappedCount - position - 1 );

    // decrement mapped count
    m_mappedCount--;

    return removed;
  }

  /**************************************** getViewIndex *****************************************/
  /**
   * Finds the view index for a given data index within a search limit.
   * <p>
   * This searches the stored mappings up to the specified limit. If the data index is not found
   * in stored mappings but falls within the valid range, the identity mapping is assumed and the
   * data index itself is returned. This optimises lookups for unmapped indices.
   * 
   * @param dataIndex the data index to search for
   * @param searchLimit the maximum view index to search (typically the count boundary)
   * @return the view index, or INVALID if not found in stored mappings and outside valid range
   */
  public int getViewIndex( int dataIndex, int searchLimit )
  {
    // search stored mappings for data index up to mapped count limit
    int limit = Math.min( m_mappedCount, searchLimit );

    // early exit if data index is beyond what we could possibly have stored
    if ( dataIndex >= limit && dataIndex < searchLimit )
      return dataIndex; // identity mapping

    for ( int i = 0; i < limit; i++ )
      if ( m_dataIndices[i] == dataIndex )
        return i;

    // not found in stored mappings
    return INVALID;
  }

  /**************************************** getViewIndex *****************************************/
  /**
   * Finds the view index for a given data index.
   * <p>
   * This searches all stored mappings. Unlike the two-parameter variant, this does not assume
   * identity mapping for values beyond the stored range.
   * 
   * @param dataIndex the data index to search for
   * @return the view index, or INVALID if not found in stored mappings
   */
  public int getViewIndex( int dataIndex )
  {
    // search all stored mappings for data index
    return getViewIndex( dataIndex, m_mappedCount );
  }

  /****************************************** hashCode *******************************************/
  /**
   * Returns a hash code for this mapping based on stored values.
   * <p>
   * Automatically trims trailing identity mappings before computing the hash to ensure mappings
   * that are functionally equivalent produce the same hash code.
   * 
   * @return hash code value
   */
  @Override
  public int hashCode()
  {
    // trim identity mappings first
    trimIdentityTail();

    // compute hash from remaining stored values
    int hash = 1;
    for ( int i = 0; i < m_mappedCount; i++ )
      hash = 31 * hash + m_dataIndices[i];

    return hash;
  }

  /************************************** trimIdentityTail ***************************************/
  /**
   * Removes trailing entries that match identity mapping (value equals index).
   * <p>
   * This optimises storage by not storing unnecessary mappings where the view index equals the
   * data index. For example, if the mapping ends with [..., 5→5, 6→6, 7→7], these trailing
   * entries are removed since they provide no information beyond the identity mapping.
   * 
   * @return true if any entries were trimmed
   */
  public boolean trimIdentityTail()
  {
    // track original mapped count
    int originalCount = m_mappedCount;

    // remove trailing entries where mapping[i] == i
    while ( m_mappedCount > 0 && m_dataIndices[m_mappedCount - 1] == m_mappedCount - 1 )
      m_mappedCount--;

    // return whether any trimming occurred
    return m_mappedCount < originalCount;
  }

  /*************************************** reorderIndices ****************************************/
  /**
   * Efficiently reorders multiple indices in a single operation.
   * <p>
   * This method handles the complex mapping updates required when multiple rows or columns are
   * moved together (e.g., during drag-and-drop reordering). The source indices are removed from
   * their current positions and reinserted at the target position, with all other mappings
   * adjusted accordingly.
   * <p>
   * The operation is optimised to perform a single bulk shift for insertion rather than repeated
   * single insertions.
   * * @param sortedSourceIndexes sorted array of view indices to move (must be in ascending order)
   * @param targetIndex the position where moved indices should be inserted
   * @return the minimum index affected by the reordering (useful for cache invalidation)
   */
  public int reorderIndices( int[] sortedSourceIndexes, int targetIndex )
  {
    // ensure mapping array covers all affected indices
    int maxRequiredIndex = Math.max( targetIndex, sortedSourceIndexes[sortedSourceIndexes.length - 1] );
    while ( m_mappedCount <= maxRequiredIndex )
      append( m_mappedCount );

    // extract data indices in reverse to preserve positions
    int[] extractedData = new int[sortedSourceIndexes.length];
    for ( int i = sortedSourceIndexes.length - 1; i >= 0; i-- )
      extractedData[i] = remove( sortedSourceIndexes[i] );

    // calculate adjusted insert position
    int adjustedTarget = targetIndex;
    for ( int sourceIndex : sortedSourceIndexes )
      if ( sourceIndex < targetIndex )
        adjustedTarget--;
      else
        break;

    // insert extracted data back at adjusted position
    int count = extractedData.length;

    // ensure capacity to add items back
    ensureCapacity( m_mappedCount + count );

    // shift existing elements right to make space
    System.arraycopy( m_dataIndices, adjustedTarget, m_dataIndices, adjustedTarget + count,
        m_mappedCount - adjustedTarget );

    // copy extracted data into the gap
    System.arraycopy( extractedData, 0, m_dataIndices, adjustedTarget, count );

    // update mapped count
    m_mappedCount += count;

    // return minimum affected index for cache invalidation
    return Math.min( adjustedTarget, sortedSourceIndexes[0] );
  }

  /*********************************** getDataIndexWithBounds ************************************/
  /**
   * Gets the data index with boundary checking against count.
   * <p>
   * This variant provides additional safety by checking that both the view index and resulting
   * data index are within valid bounds. This is used by {@link TableAxis#getDataIndex} to ensure
   * returned indices are always valid.
   * 
   * @param viewIndex the view index to look up
   * @param maxValidIndex the maximum valid index (exclusive, typically the table count)
   * @return the data index, viewIndex if not mapped and within bounds, or INVALID if out of bounds
   */
  public int getDataIndexWithBounds( int viewIndex, int maxValidIndex )
  {
    // check if within stored mappings
    if ( viewIndex >= FIRSTCELL && viewIndex < m_mappedCount )
      return m_dataIndices[viewIndex];

    // check if within valid range but not explicitly mapped
    if ( viewIndex >= INVALID && viewIndex < maxValidIndex )
      return viewIndex;

    // out of bounds
    return INVALID;
  }

  /*********************************** getViewIndexWithBounds ************************************/
  /**
   * Finds the view index with boundary checking against count.
   * <p>
   * This variant provides additional safety by checking that both the data index and resulting
   * view index are within valid bounds. This is used by {@link TableAxis#getViewIndex} to ensure
   * returned indices are always valid.
   * 
   * @param dataIndex the data index to search for
   * @param count the maximum valid index (exclusive, typically the table count)
   * @return the view index, dataIndex if not mapped and within bounds, or INVALID if out of bounds
   */
  public int getViewIndexWithBounds( int dataIndex, int count )
  {
    // check if within stored mappings
    if ( dataIndex >= FIRSTCELL && dataIndex < m_mappedCount )
    {
      int viewIndex = getViewIndex( dataIndex, m_mappedCount );
      if ( viewIndex >= 0 )
        return viewIndex;
    }

    // check if within valid range but not explicitly mapped
    if ( dataIndex >= INVALID && dataIndex < count )
      return dataIndex;

    // out of bounds
    return INVALID;
  }

  /************************************** ensureCapacity *****************************************/
  private void ensureCapacity( int requiredCapacity )
  {
    // return early if capacity is already sufficient
    if ( requiredCapacity <= m_dataIndices.length )
      return;

    // grow by 25% or to required capacity, whichever is larger
    int newCapacity = Math.max( requiredCapacity + 4, m_dataIndices.length + ( m_dataIndices.length >> 2 ) + 4 );

    // allocate new array with identity mapping initialisation
    int[] newDataIndices = new int[newCapacity];
    System.arraycopy( m_dataIndices, 0, newDataIndices, 0, m_mappedCount );
    for ( int i = m_mappedCount; i < newCapacity; i++ )
      newDataIndices[i] = i;

    m_dataIndices = newDataIndices;
  }

}