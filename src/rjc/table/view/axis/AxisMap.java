/**************************************************************************
 *  Copyright (C) 2024 by Richard Crook                                   *
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
import java.util.Set;

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

  /******************************************* reorder *******************************************/
  public int reorder( Set<Integer> toBeMovedIndexes, int insertIndex )
  {
    // reorder mapping between view-indexes and data-indexes
    var beforeHash = getIndexMappingHash();
    var movedSorted = new ArrayList<Integer>( toBeMovedIndexes );
    movedSorted.sort( null );

    // ensure data-view mapping array is big enough
    int neededSize = Math.max( insertIndex, movedSorted.getLast() );
    while ( m_dataIndexFromViewIndex.size() <= neededSize )
      m_dataIndexFromViewIndex.add( m_dataIndexFromViewIndex.size() );

    // remove the indexes to be moved from mapping (highest to lowest to preserve position)
    var movedList = new ArrayList<Integer>( movedSorted.size() );
    for ( int i = movedSorted.size(); i-- > 0; )
      movedList.add( m_dataIndexFromViewIndex.remove( (int) movedSorted.get( i ) ) );

    // adjust insert-index to take account of removed entries
    int oldInsert = insertIndex;
    for ( int index : movedSorted )
      if ( index < oldInsert )
        insertIndex--;
      else
        break;

    // re-insert moved indexes back into mapping at adjusted insert position and in correct order
    m_dataIndexFromViewIndex.addAll( insertIndex, movedList.reversed() );

    // compare mapping hash to see if changed from before reorder, if no change return INVALID
    if ( getIndexMappingHash() == beforeHash )
      return INVALID;

    // truncate pixel cache in case sizes also moved
    int min = Math.min( insertIndex, movedSorted.getFirst() );
    truncatePixelCaches( min, 0 );
    reorderExceptions( movedSorted, insertIndex );

    // return start index of reordered
    return insertIndex;
  }

  /************************************* getIndexMappingHash *************************************/
  private int getIndexMappingHash()
  {
    // remove any unneeded mapping, and return hash-code of resulting array
    int index = m_dataIndexFromViewIndex.size() - 1;
    while ( index >= 0 && m_dataIndexFromViewIndex.get( index ) == index )
      m_dataIndexFromViewIndex.remove( index-- );

    return m_dataIndexFromViewIndex.hashCode();
  }

}
