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

import rjc.table.HashSetInt;
import rjc.table.signal.ObservableInteger.ReadOnlyInteger;

/*************************************************************************************************/
/**************************** Table view-index mapping to data-index *****************************/
/*************************************************************************************************/

public class AxisMap extends AxisSize
{
  // mapping from view-index to data-index
  private IndexMapping m_dataIndexFromViewIndex = new IndexMapping();

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
    // delegate to index mapping with boundary checking
    return m_dataIndexFromViewIndex.getWithBounds( viewIndex, getCount(), FIRSTCELL, INVALID );
  }

  /**************************************** getViewIndex *****************************************/
  public int getViewIndex( int dataIndex )
  {
    // delegate to index mapping with boundary checking
    return m_dataIndexFromViewIndex.indexOfWithBounds( dataIndex, getCount(), FIRSTCELL, INVALID );
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

    // perform bulk reorder and get minimum affected index
    int minAffectedIndex = m_dataIndexFromViewIndex.bulkReorder( sortedMovedIndexes, insertIndex );

    // update pixel caches and exception mappings for affected range
    truncatePixelCaches( minAffectedIndex, 0 );
    reorderExceptions( sortedMovedIndexes, insertIndex );
  }

  /************************************* getIndexMappingHash *************************************/
  public int getIndexMappingHash()
  {
    // hash-code automatically trims identity mappings
    return m_dataIndexFromViewIndex.hashCode();
  }

}