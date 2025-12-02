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

import java.util.Map;

import rjc.table.HashSetInt;
import rjc.table.Utils;
import rjc.table.signal.ObservableDouble.ReadOnlyDouble;
import rjc.table.signal.ObservableInteger.ReadOnlyInteger;

/*************************************************************************************************/
/********* View-table axis functionality including index mapping, sizing and visibility **********/
/*************************************************************************************************/

public class TableAxis
{
  // standard axis constants
  public static final int       INVALID   = -2;
  public static final int       HEADER    = -1;
  public static final int       FIRSTCELL = 0;
  public static final int       BEFORE    = Integer.MIN_VALUE + 1;
  public static final int       AFTER     = Integer.MAX_VALUE - 1;

  // axis components
  private final ReadOnlyInteger m_countProperty;
  private final IndexMapping    m_mapping;
  private final AxisLayout      m_layout;

  /**************************************** constructor ******************************************/
  public TableAxis( ReadOnlyInteger countProperty )
  {
    // validate count property
    if ( countProperty == null || countProperty.get() < 0 )
      throw new IllegalArgumentException( "Bad body cell count = " + countProperty );

    // initialise components
    m_countProperty = countProperty;
    m_mapping = new IndexMapping();
    m_layout = new AxisLayout( countProperty );
  }

  /******************************************** reset ********************************************/
  public void reset()
  {
    // reset all components
    m_mapping.clear();
    m_layout.reset();
  }

  // ==============================================================================================
  // Base Axis / Count Property Methods
  // ==============================================================================================

  /************************************** getCountProperty ***************************************/
  public ReadOnlyInteger getCountProperty()
  {
    // return count property
    return m_countProperty;
  }

  /****************************************** getCount *******************************************/
  public int getCount()
  {
    // return current count
    return m_countProperty.get();
  }

  /***************************************** getVisible ******************************************/
  public int getVisible( int index )
  {
    // return index if visible, otherwise find nearest visible index
    if ( index < FIRSTCELL )
      return getFirstVisible();
    if ( index >= getCount() )
      return getLastVisible();
    if ( m_layout.isIndexVisible( index ) )
      return index;

    return getNextVisible( index );
  }

  /************************************** getFirstVisible ****************************************/
  public int getFirstVisible()
  {
    // return first visible body cell index
    return getNextVisible( HEADER );
  }

  /*************************************** getLastVisible ****************************************/
  public int getLastVisible()
  {
    // return last visible body cell index
    return getPreviousVisible( getCount() );
  }

  /*************************************** getNextVisible ****************************************/
  public int getNextVisible( int index )
  {
    // return next visible body cell index
    if ( index < HEADER )
      index = HEADER;

    int max = getCount();
    for ( int check = index + 1; check < max; check++ )
      if ( m_layout.isIndexVisible( check ) )
        return check;
    for ( int check = index; check > HEADER; check-- )
      if ( m_layout.isIndexVisible( check ) )
        return check;

    return INVALID;
  }

  /************************************* getPreviousVisible **************************************/
  public int getPreviousVisible( int index )
  {
    // return previous visible body cell index
    int max = getCount();
    if ( index > max )
      index = max;

    for ( int check = index - 1; check > HEADER; check-- )
      if ( m_layout.isIndexVisible( check ) )
        return check;
    for ( int check = index; check < max; check++ )
      if ( m_layout.isIndexVisible( check ) )
        return check;

    return INVALID;
  }

  // ==============================================================================================
  // Index Mapping Methods (Direct access to IndexMapping)
  // ==============================================================================================

  /**************************************** getDataIndex *****************************************/
  public int getDataIndex( int viewIndex )
  {
    // delegate to index mapping with boundary checking against count
    if ( viewIndex < HEADER || viewIndex >= m_countProperty.get() )
      return INVALID;
    if ( viewIndex == HEADER )
      return HEADER;
    return m_mapping.getDataIndex( viewIndex );
  }

  /**************************************** getViewIndex *****************************************/
  public int getViewIndex( int dataIndex )
  {
    // delegate to index mapping with boundary checking against count
    if ( dataIndex < HEADER || dataIndex >= m_countProperty.get() )
      return INVALID;
    if ( dataIndex == HEADER )
      return HEADER;

    return m_mapping.getViewIndex( dataIndex );
  }

  /******************************************* reorder *******************************************/
  public void reorder( HashSetInt toBeMovedIndexes, int insertIndex )
  {
    // convert set to sorted array once for both operations
    var sortedMoved = toBeMovedIndexes.toSortedArray();

    // perform mapping reorder and capture affected index
    int minAffectedIndex = m_mapping.reorderIndices( sortedMoved, insertIndex );

    // update layout components with reorder data
    m_layout.reorderExceptions( sortedMoved, insertIndex );
    m_layout.truncatePixelCaches( minAffectedIndex, 0 );
  }

  /************************************* getIndexMappingHash *************************************/
  public int getIndexMappingHash()
  {
    // delegate to mapping object
    return m_mapping.hashCode();
  }

  // ==============================================================================================
  // Axis Sizing / Layout Methods (Delegated to AxisLayout)
  // ==============================================================================================

  /*************************************** getHeaderPixels ***************************************/
  public int getHeaderPixels()
  {
    // delegate to layout
    return m_layout.getHeaderPixels();
  }

  /*************************************** getDefaultSize ****************************************/
  public int getDefaultSize()
  {
    // delegate to layout
    return m_layout.getDefaultSize();
  }

  /*************************************** setDefaultSize ****************************************/
  public void setDefaultSize( int defaultSize )
  {
    // delegate to layout
    m_layout.setDefaultSize( defaultSize );
  }

  /*************************************** setMinimumSize ****************************************/
  public void setMinimumSize( int minSize )
  {
    // delegate to layout
    m_layout.setMinimumSize( minSize );
  }

  /**************************************** setHeaderSize ****************************************/
  public void setHeaderSize( int headerSize )
  {
    // delegate to layout
    m_layout.setHeaderSize( headerSize );
  }

  /**************************************** setIndexSize *****************************************/
  public void setIndexSize( int viewIndex, int newSize )
  {
    // delegate to layout
    m_layout.setIndexSize( viewIndex, newSize );
  }

  /*************************************** getTotalPixels ****************************************/
  public int getTotalPixels()
  {
    // delegate to layout
    return m_layout.getTotalPixels();
  }

  /*********************************** getTotalPixelsProperty ************************************/
  public ReadOnlyInteger getTotalPixelsProperty()
  {
    // delegate to layout
    return m_layout.getTotalPixelsProperty();
  }

  /*************************************** getIndexPixels ****************************************/
  public int getIndexPixels( int viewIndex )
  {
    // delegate to layout
    return m_layout.getIndexPixels( viewIndex );
  }

  /**************************************** getStartPixel ****************************************/
  public int getStartPixel( int viewIndex, int scrollOffset )
  {
    // delegate to layout
    return m_layout.getStartPixel( viewIndex, scrollOffset );
  }

  /*********************************** getIndexFromCoordinate ************************************/
  public int getIndexFromCoordinate( int coordinate, int scrollOffset )
  {
    // delegate to layout
    return m_layout.getIndexFromCoordinate( coordinate, scrollOffset );
  }

  /************************************** getSizeExceptions **************************************/
  public Map<Integer, Integer> getSizeExceptions()
  {
    // delegate to layout
    return m_layout.getSizeExceptions();
  }

  /************************************* clearSizeExceptions *************************************/
  public void clearSizeExceptions()
  {
    // delegate to layout
    m_layout.clearSizeExceptions();
  }

  /*************************************** clearIndexSize ****************************************/
  public void clearIndexSize( int viewIndex )
  {
    // delegate to layout
    m_layout.clearIndexSize( viewIndex );
  }

  /*************************************** setZoomProperty ***************************************/
  public void setZoomProperty( ReadOnlyDouble zoomProperty )
  {
    // delegate to layout
    m_layout.setZoomProperty( zoomProperty );
  }

  /*************************************** isIndexVisible ****************************************/
  public boolean isIndexVisible( int viewIndex )
  {
    // delegate to layout
    return m_layout.isIndexVisible( viewIndex );
  }

  /***************************************** hideIndexes *****************************************/
  public HashSetInt hideIndexes( HashSetInt viewIndexes )
  {
    // delegate to layout
    return m_layout.hideIndexes( viewIndexes );
  }

  /**************************************** unhideIndexes ****************************************/
  public HashSetInt unhideIndexes( HashSetInt viewIndexes )
  {
    // delegate to layout
    return m_layout.unhideIndexes( viewIndexes );
  }

  /****************************************** unhideAll ******************************************/
  public HashSetInt unhideAll()
  {
    // delegate to layout
    return m_layout.unhideAll();
  }

  /************************************** getHiddenIndexes ***************************************/
  public HashSetInt getHiddenIndexes()
  {
    // delegate to layout
    return m_layout.getHiddenIndexes();
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return as string
    return Utils.name( this ) + "[count=" + getCount() + "]";
  }

}