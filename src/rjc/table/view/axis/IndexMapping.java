/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
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
  private int[] m_dataIndices; // array of data indices mapped from view indices
  private int   m_mappedCount; // count of indices with explicit mappings stored

  /**************************************** constructor ******************************************/
  /**
   * Creates a new empty index mapping.
   */
  public IndexMapping()
  {
    // initialise with empty mapping
    reset();
  }

  /******************************************** reset ********************************************/
  /**
   * Resets all stored mappings, clearing to identity mapping.
   */
  public void reset()
  {
    // reset to empty state with no stored mappings
    m_dataIndices = new int[0];
    m_mappedCount = 0;
  }

  /****************************************** getDataIndex ***************************************/
  /**
   * Gets the data index for a given view index (fast direct lookup).
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
    if ( viewIndex < m_mappedCount )
      return m_dataIndices[viewIndex];

    return viewIndex;
  }

  /**************************************** getViewIndex *****************************************/
  /**
   * Finds the view index for a given data index (slow as needs to search).
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
    return TableAxis.INVALID;
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

  /***************************************** setMapping ******************************************/
  /**
   * Sets a new mapping from view-indices to data-indices based on provided mapping array.
   * <p>
   * The newMapping array contains data indices for non-hidden view indices only, in order.
   * This method updates the internal mapping to reflect the new ordering while leaving
   * hidden indices unchanged.
   * 
   * @param newMapping array of data indices for non-hidden view indices
   * @param size IndexSize object to determine hidden/non-hidden status
   * @param max maximum view index to consider for mapping
   */
  public void setMapping( int[] newMapping, IndexSize size, int max )
  {
    // ensure capacity for all view indices up to max
    ensureCapacity( max );

    // update mapping for non-hidden view indices from new mapping (used to apply new sorting)
    int count = 0;
    for ( int viewIndex = 0; viewIndex < max; viewIndex++ )
    {
      int oldDataIndex = getDataIndex( viewIndex );
      if ( size.getSize( oldDataIndex ) > 0 )
      {
        int newDataIndex = newMapping[count++];
        m_dataIndices[viewIndex] = newDataIndex;
      }
    }

    // update mapped count to reflect new maximum mapped index
    if ( max > m_mappedCount )
      m_mappedCount = max;
    trimIdentityTail();
  }

  /************************************** trimIdentityTail ***************************************/
  /**
  * Reduces the mapped count to exclude trailing entries that match identity mapping (value equals index).
  * <p>
  * This optimises storage by not considering unnecessary mappings where the view index equals the
  * data index. For example, if the mapping ends with [..., 5→5, 6→6, 7→7], the mapped count
  * is reduced to exclude these trailing entries since they provide no information beyond the 
  * identity mapping. The array itself is not modified; only m_mappedCount is adjusted.
  * 
  * @return true if the mapped count was reduced
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

  /***************************************** moveIndices *****************************************/
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
   * @param insertAtViewIndex the position where moved indices should be inserted
   * @return the minimum index affected by the reordering (useful for cache invalidation)
   */
  public int moveIndices( int[] sortedSourceIndexes, int insertAtViewIndex )
  {
    // ensure capacity for all affected indices
    int maxRequiredIndex = Math.max( insertAtViewIndex, sortedSourceIndexes[sortedSourceIndexes.length - 1] );
    ensureCapacity( maxRequiredIndex + 1 );
    m_mappedCount = maxRequiredIndex + 1;

    // extract data values by removing from highest to lowest index to preserve lower positions during removal
    int[] movedDataIndexes = new int[sortedSourceIndexes.length];
    for ( int i = sortedSourceIndexes.length - 1; i >= 0; i-- )
      movedDataIndexes[i] = remove( sortedSourceIndexes[i] );

    // calculate final insert position accounting for removed elements before target
    int insertPosition = insertAtViewIndex;
    for ( int sourceIndex : sortedSourceIndexes )
      if ( sourceIndex < insertAtViewIndex )
        insertPosition--;
      else
        break;

    // shift existing elements right to create space for moved elements
    int count = movedDataIndexes.length;
    System.arraycopy( m_dataIndices, insertPosition, m_dataIndices, insertPosition + count,
        m_mappedCount - insertPosition );

    // insert extracted data into created gap
    System.arraycopy( movedDataIndexes, 0, m_dataIndices, insertPosition, count );

    // update mapped count to include reinserted elements
    m_mappedCount += count;

    // return earliest affected index for cache invalidation
    return Math.min( insertPosition, sortedSourceIndexes[0] );
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

  /**************************************** deleteMapping ****************************************/
  /**
   * Removes all view entries whose stored data index falls within
   * {@code [dataStart, dataStart+dataCount)}, shifts the remaining view entries down to close
   * the gaps, and decrements every remaining stored data value {@code >= dataStart} by
   * {@code dataCount}.
   * <p>
   * Only the explicitly stored range {@code [0, m_mappedCount)} is scanned; identity-mapped
   * view indexes beyond that range self-heal as the axis count shrinks. Trailing identity
   * entries are intentionally not trimmed here — trimming after a decrement could silently
   * discard entries that must be incremented back on undo.
   * <p>
   * The returned array captures the original {@code {viewIndex, dataIndex}} pair for every
   * removed entry (in ascending view-index order) and is suitable for passing to
   * {@link #insertMapping} on undo.
   *
   * @param dataStart first data index of the deleted run (inclusive)
   * @param dataCount number of consecutive data indexes deleted
   * @return captured pairs {@code int[n][2]} where each row is {@code {viewIndex, dataIndex}},
   *         ordered by ascending original view index
   */
  public int[][] deleteMapping( int dataStart, int dataCount )
  {
    int dataEnd = dataStart + dataCount; // exclusive upper bound of deleted data range

    // first pass: count how many stored view entries fall in the deleted data range
    int removedCount = 0;
    for ( int v = 0; v < m_mappedCount; v++ )
      if ( m_dataIndices[v] >= dataStart && m_dataIndices[v] < dataEnd )
        removedCount++;

    // capture original pairs and build compacted array in a single second pass
    int[][] captured = new int[removedCount][2];
    int capturePos = 0;
    int writePos = 0;
    for ( int v = 0; v < m_mappedCount; v++ )
    {
      int d = m_dataIndices[v];
      if ( d >= dataStart && d < dataEnd )
        // record original view index and data index before removal
        captured[capturePos++] = new int[] { v, d };
      else
        // keep entry, decrementing data value if it lies above the deleted range
        m_dataIndices[writePos++] = d >= dataEnd ? d - dataCount : d;
    }
    m_mappedCount = writePos;
    return captured;
  }

  /**************************************** insertMapping ****************************************/
  /**
   * Reverses a previous {@link #deleteMapping} call by re-inserting the captured view entries.
   * <p>
   * First increments all currently stored data values {@code >= dataStart} by {@code dataCount}
   * to reverse the decrement applied during deletion. Then inserts each captured
   * {@code {viewIndex, dataIndex}} pair back into the stored mapping, shifting existing entries
   * right to make room.
   * <p>
   * Captured pairs must be in ascending view-index order as returned by
   * {@link #deleteMapping}.
   *
   * @param dataStart first data index of the restored run (must match original deletion)
   * @param dataCount number of consecutive data indexes restored
   * @param captured  pairs returned by the corresponding {@link #deleteMapping} call
   */
  public void insertMapping( int dataStart, int dataCount, int[][] captured )
  {
    if ( captured.length == 0 )
      return;

    // increment all stored data values above the restored range (reverse the earlier decrement)
    for ( int v = 0; v < m_mappedCount; v++ )
      if ( m_dataIndices[v] >= dataStart )
        m_dataIndices[v] += dataCount;

    // re-insert captured pairs in ascending view-index order; each insertion shifts the tail
    // right by one so subsequent captured view indexes remain correct
    for ( int[] pair : captured )
    {
      int viewIndex = pair[0];
      int dataIndex = pair[1];
      ensureCapacity( Math.max( viewIndex, m_mappedCount ) + 1 );

      if ( viewIndex < m_mappedCount )
        // shift right to create slot at viewIndex
        System.arraycopy( m_dataIndices, viewIndex, m_dataIndices, viewIndex + 1, m_mappedCount - viewIndex );

      m_dataIndices[viewIndex] = dataIndex;
      m_mappedCount = Math.max( m_mappedCount, viewIndex ) + 1;
    }
    trimIdentityTail();
  }

  /************************************** ensureCapacity *****************************************/
  private void ensureCapacity( int requiredCapacity )
  {
    // early return if current capacity is sufficient
    if ( requiredCapacity <= m_dataIndices.length )
      return;

    // calculate new capacity with 25% growth factor, minimum of required capacity
    int newCapacity = Math.max( requiredCapacity + 4, m_dataIndices.length + ( m_dataIndices.length >> 2 ) );

    // create new array and copy existing mappings
    int[] expandedArray = new int[newCapacity];
    System.arraycopy( m_dataIndices, 0, expandedArray, 0, m_mappedCount );

    // initialise new slots with identity mapping values
    for ( int i = m_mappedCount; i < newCapacity; i++ )
      expandedArray[i] = i;

    m_dataIndices = expandedArray;
  }

}