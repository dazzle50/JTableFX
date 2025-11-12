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
/**************** Efficient sparse mapping from view indices to data indices *********************/
/*************************************************************************************************/

public class IndexMapping
{
  private int[] m_mapping; // sparse array: only stores reordered indices
  private int   m_size;    // logical size (may exceed array length)

  /**************************************** constructor ******************************************/
  public IndexMapping()
  {
    // initialize with empty mapping
    m_mapping = new int[16];
    m_size = 0;
  }

  /******************************************** clear ********************************************/
  /**
   * Clears all stored mappings, resetting to identity mapping.
   */
  public void clear()
  {
    // reset to empty state
    m_size = 0;
  }

  /********************************************* size ********************************************/
  /**
   * Returns the logical size of the mapping.
   * 
   * @return the number of indices that can be retrieved
   */
  public int size()
  {
    // return logical size
    return m_size;
  }

  /****************************************** isEmpty ********************************************/
  /**
   * Returns whether the mapping is empty.
   * 
   * @return true if no mappings are stored
   */
  public boolean isEmpty()
  {
    // check if size is zero
    return m_size == 0;
  }

  /********************************************* get *********************************************/
  /**
   * Gets the data index for a given view index.
   * 
   * @param viewIndex the view index to look up
   * @return the corresponding data index, or viewIndex if not explicitly mapped
   */
  public int get( int viewIndex )
  {
    // return mapped value if within stored range, otherwise identity mapping
    if ( viewIndex < m_mapping.length )
      return m_mapping[viewIndex];

    return viewIndex;
  }

  /********************************************* add *********************************************/
  /**
   * Appends a data index to the end of the mapping.
   * 
   * @param dataIndex the data index to append
   */
  public void add( int dataIndex )
  {
    // ensure capacity and store value
    ensureCapacity( m_size + 1 );
    m_mapping[m_size++] = dataIndex;
  }

  /********************************************* add *********************************************/
  /**
   * Inserts a data index at the specified position.
   * 
   * @param position the position to insert at
   * @param dataIndex the data index to insert
   */
  public void add( int position, int dataIndex )
  {
    // ensure capacity for insertion
    ensureCapacity( m_size + 1 );

    // shift elements right to make space
    System.arraycopy( m_mapping, position, m_mapping, position + 1, m_size - position );

    // insert new value and increment size
    m_mapping[position] = dataIndex;
    m_size++;
  }

  /******************************************* remove ********************************************/
  /**
   * Removes and returns the data index at the specified position.
   * 
   * @param position the position to remove from
   * @return the data index that was removed
   */
  public int remove( int position )
  {
    // get value to return
    int removed = m_mapping[position];

    // shift elements left to fill gap
    System.arraycopy( m_mapping, position + 1, m_mapping, position, m_size - position - 1 );

    // decrement size
    m_size--;

    return removed;
  }

  /***************************************** indexOf *********************************************/
  /**
   * Finds the view index for a given data index within the mapping size.
   * 
   * @param dataIndex the data index to search for
   * @param maxIndex the maximum view index to return (typically count boundary)
   * @return the view index, or -1 if not found in stored mappings
   */
  public int indexOf( int dataIndex, int maxIndex )
  {
    // search stored mappings for data index up to size limit
    int searchLimit = Math.min( m_size, maxIndex );
    for ( int i = 0; i < searchLimit; i++ )
      if ( m_mapping[i] == dataIndex )
        return i;

    // not found in stored mappings
    return -1;
  }

  /***************************************** indexOf *********************************************/
  /**
   * Finds the view index for a given data index.
   * 
   * @param dataIndex the data index to search for
   * @return the view index, or -1 if not found in stored mappings
   */
  public int indexOf( int dataIndex )
  {
    // search all stored mappings for data index
    return indexOf( dataIndex, m_size );
  }

  /****************************************** hashCode *******************************************/
  /**
   * Returns a hash code for this mapping based on trimmed values.
   * Automatically trims identity mappings before computing hash.
   * 
   * @return hash code value
   */
  @Override
  public int hashCode()
  {
    // trim identity mappings first
    trimToIdentity();

    // compute hash from remaining stored values
    int hash = 1;
    for ( int i = 0; i < m_size; i++ )
      hash = 31 * hash + m_mapping[i];

    return hash;
  }

  /************************************** trimToIdentity *****************************************/
  /**
   * Removes trailing entries that match identity mapping (value equals index).
   * This optimizes storage by not storing unnecessary mappings.
   * 
   * @return true if any entries were trimmed
   */
  public boolean trimToIdentity()
  {
    // track original size
    int originalSize = m_size;

    // remove trailing entries where mapping[i] == i
    while ( m_size > 0 && m_mapping[m_size - 1] == m_size - 1 )
      m_size--;

    // return whether any trimming occurred
    return m_size < originalSize;
  }

  /***************************************** isIdentity ******************************************/
  /**
   * Checks if this mapping represents an identity mapping (no reordering).
   * 
   * @return true if all stored mappings match identity (index == value)
   */
  public boolean isIdentity()
  {
    // check if all stored values equal their indices
    for ( int i = 0; i < m_size; i++ )
      if ( m_mapping[i] != i )
        return false;

    return true;
  }

  /*************************************** hasReordering *****************************************/
  /**
   * Fast check if any reordering has occurred by looking for first non-identity mapping.
   * This is faster than isIdentity() as it returns on first mismatch.
   * 
   * @return true if any index doesn't map to itself
   */
  public boolean hasReordering()
  {
    // early exit on first non-identity mapping
    for ( int i = 0; i < m_size; i++ )
      if ( m_mapping[i] != i )
        return true;

    return false;
  }

  /**************************************** bulkReorder ******************************************/
  /**
   * Efficiently reorders multiple indices in a single operation.
   * This is optimized for the reorder() method in AxisMap.
   * 
   * @param sortedSourceIndexes sorted array of view indices to move (ascending order)
   * @param targetIndex the position where moved indices should be inserted
   * @return the minimum index affected by the reordering
   */
  public int bulkReorder( int[] sortedSourceIndexes, int targetIndex )
  {
    // ensure mapping array covers all affected indices
    int maxRequiredIndex = Math.max( targetIndex, sortedSourceIndexes[sortedSourceIndexes.length - 1] );
    while ( m_size <= maxRequiredIndex )
      add( m_size );

    // extract data indices in reverse to preserve positions
    int[] extractedData = new int[sortedSourceIndexes.length];
    for ( int i = sortedSourceIndexes.length - 1; i >= 0; i-- )
      extractedData[i] = remove( sortedSourceIndexes[i] );

    // calculate adjusted insert position
    int adjustedTarget = targetIndex;
    for ( int sourceIndex : sortedSourceIndexes )
    {
      if ( sourceIndex < targetIndex )
        adjustedTarget--;
      else
        break;
    }

    // reinsert extracted data at adjusted position (reverse order)
    for ( int i = extractedData.length - 1; i >= 0; i-- )
      add( adjustedTarget, extractedData[i] );

    // return minimum affected index for cache invalidation
    return Math.min( adjustedTarget, sortedSourceIndexes[0] );
  }

  /**************************************** getViewRange *****************************************/
  /**
   * Gets a contiguous range of data indices for the specified view range.
   * Useful for bulk operations like rendering visible cells.
   * 
   * @param startViewIndex the starting view index (inclusive)
   * @param endViewIndex the ending view index (exclusive)
   * @param output array to store data indices (must be large enough)
   * @return the output array for chaining
   */
  public int[] getViewRange( int startViewIndex, int endViewIndex, int[] output )
  {
    // fill output array with data indices for view range
    for ( int i = 0; i < endViewIndex - startViewIndex; i++ )
      output[i] = get( startViewIndex + i );

    return output;
  }

  /**************************************** copyToArray ******************************************/
  /**
   * Copies the internal mapping to a new array for external processing.
   * Only copies up to the logical size, not the full capacity.
   * 
   * @return a new array containing the stored mappings
   */
  public int[] copyToArray()
  {
    // create new array with exact size needed
    int[] copy = new int[m_size];

    // copy stored mappings
    System.arraycopy( m_mapping, 0, copy, 0, m_size );

    return copy;
  }

  /*************************************** getWithBounds *****************************************/
  /**
   * Gets the data index with boundary checking against count.
   * Returns INVALID constant if indices are out of bounds.
   * 
   * @param viewIndex the view index to look up
   * @param count the maximum valid index (exclusive)
   * @param FIRSTCELL the first valid cell index constant
   * @param INVALID the invalid index constant
   * @return the data index, viewIndex if not mapped, or INVALID if out of bounds
   */
  public int getWithBounds( int viewIndex, int count, int FIRSTCELL, int INVALID )
  {
    // check if within stored mappings
    if ( viewIndex >= FIRSTCELL && viewIndex < m_size )
      return m_mapping[viewIndex];

    // check if within valid range but not explicitly mapped
    if ( viewIndex >= INVALID && viewIndex < count )
      return viewIndex;

    // out of bounds
    return INVALID;
  }

  /************************************* indexOfWithBounds ***************************************/
  /**
   * Finds the view index with boundary checking against count.
   * Returns INVALID constant if indices are out of bounds.
   * 
   * @param dataIndex the data index to search for
   * @param count the maximum valid index (exclusive)
   * @param FIRSTCELL the first valid cell index constant
   * @param INVALID the invalid index constant
   * @return the view index, dataIndex if not mapped, or INVALID if out of bounds
   */
  public int indexOfWithBounds( int dataIndex, int count, int FIRSTCELL, int INVALID )
  {
    // check if within stored mappings
    if ( dataIndex >= FIRSTCELL && dataIndex < m_size )
    {
      int viewIndex = indexOf( dataIndex, m_size );
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
  private void ensureCapacity( int minCapacity )
  {
    // grow array if needed
    if ( minCapacity > m_mapping.length )
    {
      // calculate new capacity using 1.5x growth factor for better memory efficiency
      // 1.5x balances growth speed vs memory waste better than 2x for sparse data
      int growthCapacity = m_mapping.length + ( m_mapping.length >> 1 );
      int newCapacity = Math.max( growthCapacity, minCapacity );

      // cap maximum growth to avoid huge allocations for very large indices
      int maxGrowth = m_mapping.length + 1024 + ( m_mapping.length >> 5 );
      if ( newCapacity > maxGrowth && minCapacity <= maxGrowth )
        newCapacity = maxGrowth;

      int[] newMapping = new int[newCapacity];

      // copy existing values
      System.arraycopy( m_mapping, 0, newMapping, 0, m_size );

      // Initialise new slots with identity mapping
      for ( int i = m_size; i < newCapacity; i++ )
        newMapping[i] = i;

      m_mapping = newMapping;
    }
  }

}