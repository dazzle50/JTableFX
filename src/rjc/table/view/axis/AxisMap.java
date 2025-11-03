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

import java.util.ArrayList;

import rjc.table.HashSetInt;
import rjc.table.signal.ObservableInteger.ReadOnlyInteger;

/*************************************************************************************************/
/**************************** Table view-index mapping to data-index *****************************/
/*************************************************************************************************/

public class AxisMap extends AxisSize
{
  // array mapping from view-index to data-index
  private ArrayList<Integer> m_dataIndexFromViewIndex = new ArrayList<>();

  /**************************************** constructor ******************************************/
  public AxisMap( ReadOnlyInteger countProperty )
  {
    // pass count property to super class
    super( countProperty );
  }

  /******************************************** reset ********************************************/
  @Override
  public void reset()
  {
    // clear all axis view to data mapping, and call super reset
    m_dataIndexFromViewIndex.clear();
    super.reset();
  }

  /**************************************** getDataIndex *****************************************/
  public int getDataIndex( int viewIndex )
  {
    // return the data-model index from the table-view index
    if ( viewIndex >= FIRSTCELL && viewIndex < m_dataIndexFromViewIndex.size() )
      return m_dataIndexFromViewIndex.get( viewIndex );

    // if not in mapping but within count, then return view index as not re-ordered
    if ( viewIndex >= INVALID && viewIndex < getCount() )
      return viewIndex;

    // view index is out of bounds so return invalid
    return INVALID;
  }

  /**************************************** getViewIndex *****************************************/
  public int getViewIndex( int dataIndex )
  {
    // return the table-view index from the data-model index
    if ( dataIndex >= FIRSTCELL && dataIndex < m_dataIndexFromViewIndex.size() )
      return m_dataIndexFromViewIndex.indexOf( dataIndex );

    // if not in mapping but within count, then return data index as not re-ordered
    if ( dataIndex >= INVALID && dataIndex < getCount() )
      return dataIndex;

    // data index is out of bounds so return invalid
    return INVALID;
  }

  /******************************************* reorder *******************************************/
  /**
   * Reorders the view-to-data index mapping by moving specified indexes to a new position.
   * 
   * @param toBeMovedIndexes the set of view indexes to move
   * @param insertIndex the position where moved indexes should be inserted
   */
  public void reorder( HashSetInt toBeMovedIndexes, int insertIndex )
  {
    // convert set to sorted array for processing in order
    int[] sortedMovedIndexes = toBeMovedIndexes.toSortedArray();
    
    // ensure mapping array is large enough for highest affected index
    int maxRequiredIndex = Math.max( insertIndex, sortedMovedIndexes[sortedMovedIndexes.length - 1] );
    while ( m_dataIndexFromViewIndex.size() <= maxRequiredIndex )
      m_dataIndexFromViewIndex.add( m_dataIndexFromViewIndex.size() );

    // extract data indexes from positions being moved (reverse order preserves positions)
    int[] extractedDataIndexes = new int[sortedMovedIndexes.length];
    for ( int i = sortedMovedIndexes.length - 1; i >= 0; i-- )
      extractedDataIndexes[i] = m_dataIndexFromViewIndex.remove( sortedMovedIndexes[i] );

    // calculate insert position adjusted for removed entries before it
    int adjustedInsertIndex = insertIndex;
    for ( int movedIndex : sortedMovedIndexes )
      if ( movedIndex < insertIndex )
        adjustedInsertIndex--;
      else
        break; // sorted array so no more will be less than insertIndex

    // reinsert extracted data indexes at adjusted position (reverse order)
    for ( int i = extractedDataIndexes.length - 1; i >= 0; i-- )
      m_dataIndexFromViewIndex.add( adjustedInsertIndex, extractedDataIndexes[i] );

    // update pixel caches and exception mappings for affected range
    int minAffectedIndex = Math.min( adjustedInsertIndex, sortedMovedIndexes[0] );
    truncatePixelCaches( minAffectedIndex, 0 );
    reorderExceptions( sortedMovedIndexes, insertIndex );
  }

  /************************************* getIndexMappingHash *************************************/
  public int getIndexMappingHash()
  {
    // remove any unneeded mapping, and return hash-code of resulting array
    int index = m_dataIndexFromViewIndex.size() - 1;
    while ( index >= 0 && m_dataIndexFromViewIndex.get( index ) == index )
      m_dataIndexFromViewIndex.remove( index-- );

    return m_dataIndexFromViewIndex.hashCode();
  }

}
