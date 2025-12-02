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
/****************** Bidirectional mapping between view-indexes and data-indexes ******************/
/*************************************************************************************************/

/**
 * Provides bidirectional mapping between view indices and data indices for table axes.
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
 * <li>Bidirectional lookups (view→data and data→view)</li>
 * <li>Bulk reordering operations for efficient row/column drag-and-drop</li>
 * <li>Trimming of trailing identity mappings to minimise memory</li>
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
    clear();
  }

  /******************************************** clear ********************************************/
  /**
   * Clears all stored mappings, resetting to identity mapping.
   */
  public void clear()
  {
    // reset to empty state with no stored mappings
    m_dataIndices = new int[0];
    m_mappedCount = 0;
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
    // return stored mapping if within range, otherwise assume identity mapping
    if ( viewIndex < m_dataIndices.length )
      return m_dataIndices[viewIndex];

    return viewIndex;
  }

  /**************************************** getViewIndex *****************************************/
  /**
   * Finds the view index for a given data index.
   * <p>
   * For data indices beyond the mapped count, identity mapping is assumed. For indices within
   * the mapped range, the stored mappings are searched.
   * 
   * @param dataIndex the data index to search for
   * @return the view index, or INVALID if not found in stored mappings
   */
  public int getViewIndex( int dataIndex )
  {
    // assume identity mapping for indices beyond stored range
    if ( dataIndex >= m_mappedCount )
      return dataIndex;

    // search stored mappings up to mapped count
    for ( int i = 0; i < m_mappedCount; i++ )
      if ( m_dataIndices[i] == dataIndex )
        return i;

    // not found in stored mappings
    return INVALID;
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
    // trim trailing identity mappings to ensure consistent hashing
    trimIdentityTail();

    // compute hash from active stored values only
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
    // record original count to detect changes
    int originalCount = m_mappedCount;

    // reduce mapped count for trailing identity mappings where data[i] == i
    while ( m_mappedCount > 0 && m_dataIndices[m_mappedCount - 1] == m_mappedCount - 1 )
      m_mappedCount--;

    // return whether mapped count trimming occurred
    return m_mappedCount < originalCount;
  }

  /*************************************** reorderIndices ****************************************/
  /**
   * Efficiently reorders multiple indices in a single operation.
   * <p>
   * This method handles the complex mapping updates required when multiple rows or columns are
   * moved together (e.g. during drag-and-drop reordering). The source indices are removed from
   * their current positions and reinserted at the target position, with all other mappings
   * adjusted accordingly.
   * <p>
   * The operation is optimised to perform a single bulk shift for insertion rather than repeated
   * single insertions.
   * 
   * @param sortedSourceIndexes sorted array of view indices to move (must be in ascending order)
   * @param targetIndex the position where moved indices should be inserted
   * @return the minimum index affected by the reordering (useful for cache invalidation)
   */
  public int reorderIndices( int[] sortedSourceIndexes, int targetIndex )
  {
    // ensure capacity for all affected indices
    int maxRequiredIndex = Math.max( targetIndex, sortedSourceIndexes[sortedSourceIndexes.length - 1] );
    ensureCapacity( maxRequiredIndex + 1 );
    m_mappedCount = maxRequiredIndex + 1;

    // extract data values in reverse order to preserve original positions during removal
    int[] extractedData = new int[sortedSourceIndexes.length];
    for ( int i = sortedSourceIndexes.length - 1; i >= 0; i-- )
      extractedData[i] = remove( sortedSourceIndexes[i] );

    // calculate final insert position accounting for removed elements before target
    int adjustedTarget = targetIndex;
    for ( int sourceIndex : sortedSourceIndexes )
      if ( sourceIndex < targetIndex )
        adjustedTarget--;
      else
        break;

    // shift existing elements right to create space for moved elements
    int count = extractedData.length;
    System.arraycopy( m_dataIndices, adjustedTarget, m_dataIndices, adjustedTarget + count,
        m_mappedCount - adjustedTarget );

    // insert extracted data into created gap
    System.arraycopy( extractedData, 0, m_dataIndices, adjustedTarget, count );

    // update mapped count to include reinserted elements
    m_mappedCount += count;

    // return earliest affected index for cache invalidation
    return Math.min( adjustedTarget, sortedSourceIndexes[0] );
  }

  /******************************************* remove ********************************************/
  private int remove( int position )
  {
    // extract value at position for return
    int removed = m_dataIndices[position];

    // shift subsequent elements left to fill gap
    System.arraycopy( m_dataIndices, position + 1, m_dataIndices, position, m_mappedCount - position - 1 );

    // reduce mapped count by one
    m_mappedCount--;

    return removed;
  }

  /************************************** ensureCapacity *****************************************/
  private void ensureCapacity( int requiredCapacity )
  {
    // early return if current capacity is sufficient
    if ( requiredCapacity <= m_dataIndices.length )
      return;

    // calculate new capacity with 25% growth factor, minimum of required capacity
    int newCapacity = Math.max( requiredCapacity + 4, m_dataIndices.length + ( m_dataIndices.length >> 2 ) + 4 );

    // create new array and copy existing mappings
    int[] newDataIndices = new int[newCapacity];
    System.arraycopy( m_dataIndices, 0, newDataIndices, 0, m_mappedCount );

    // initialise new slots with identity mapping values
    for ( int i = m_mappedCount; i < newCapacity; i++ )
      newDataIndices[i] = i;

    m_dataIndices = newDataIndices;
  }

}