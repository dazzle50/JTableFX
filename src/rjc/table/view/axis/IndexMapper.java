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
/**************** Component handling view-index to data-index mapping logic **********************/
/*************************************************************************************************/

public class IndexMapper
{
  private final ReadOnlyInteger m_countProperty;
  private final IndexMapping    m_mapping = new IndexMapping();

  /*************************************** constructor *******************************************/
  public IndexMapper( ReadOnlyInteger countProperty )
  {
    // store reference to count property for boundary checking
    m_countProperty = countProperty;
  }

  /******************************************* reset *********************************************/
  /**
   * Resets the mapping to identity state.
   */
  public void reset()
  {
    // clear underlying mapping
    m_mapping.clear();
  }

  /*************************************** getDataIndex ******************************************/
  /**
   * Gets the data index for a given view index.
   */
  public int getDataIndex( int viewIndex )
  {
    // delegate to index mapping with boundary checking against count
    return m_mapping.getWithBounds( viewIndex, m_countProperty.get() );
  }

  /*************************************** getViewIndex ******************************************/
  /**
   * Gets the view index for a given data index.
   */
  public int getViewIndex( int dataIndex )
  {
    // delegate to index mapping with boundary checking against count
    return m_mapping.indexOfWithBounds( dataIndex, m_countProperty.get() );
  }

  /****************************************** reorder ********************************************/
  /**
   * Reorders the mapping and returns the minimum affected index.
   */
  public int reorder( HashSetInt toBeMovedIndexes, int insertIndex )
  {
    // convert set to sorted array
    var sortedMovedIndexes = toBeMovedIndexes.toSortedArray();

    // perform bulk reorder and return minimum affected index for cache invalidation
    return m_mapping.reorderIndices( sortedMovedIndexes, insertIndex );
  }

  /************************************ getIndexMappingHash **************************************/
  /**
   * Returns hash code of the current mapping state.
   */
  public int getIndexMappingHash()
  {
    // delegate to mapping object
    return m_mapping.hashCode();
  }
}