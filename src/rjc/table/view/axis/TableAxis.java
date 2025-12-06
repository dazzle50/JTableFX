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

/**
 * Complete axis functionality for table rows or columns, combining index mapping, sizing, and
 * visibility management.
 * <p>
 * A table axis represents either a row or column dimension of a table view. It provides the core
 * functionality needed to:
 * <ul>
 * <li>Map between view positions and underlying data positions (for reordering)</li>
 * <li>Calculate pixel sizes and positions for rendering</li>
 * <li>Manage per-index sizing exceptions and visibility state</li>
 * <li>Handle zoom factors for display scaling</li>
 * <li>Navigate between visible indices (skipping hidden ones)</li>
 * </ul>
 * <p>
 * The axis uses several index types:
 * <ul>
 * <li><b>View index</b>: position in the displayed table (affected by reordering)</li>
 * <li><b>Data index</b>: position in the underlying data model (stable)</li>
 * <li><b>Special values</b>: HEADER (-1), INVALID (-2), BEFORE (Integer.MIN_VALUE+1), AFTER
 * (Integer.MAX_VALUE-1)</li>
 * </ul>
 * <p>
 * Internally, the axis delegates to three specialist components:
 * <ul>
 * <li>{@link IndexMapping}: bidirectional view↔data index mapping</li>
 * <li>{@link AxisLayout}: pixel coordinate calculations and caching</li>
 * <li>{@link IndexSize}: compact storage for per-index sizes and hidden state</li>
 * </ul>
 * 
 * @see IndexMapping
 * @see AxisLayout
 * @see IndexSize
 */
public class TableAxis
{
  // standard axis index constants
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
  /**
   * Creates a new table axis with the specified cell count.
   * 
   * @param countProperty
   *          observable count of body cells in this axis (must be non-null and >= 0)
   * @throws IllegalArgumentException
   *           if countProperty is null or negative
   */
  public TableAxis( ReadOnlyInteger countProperty )
  {
    // validate count property is usable
    if ( countProperty == null || countProperty.get() < 0 )
      throw new IllegalArgumentException( "Bad body cell count = " + countProperty );

    // initialise all components
    m_countProperty = countProperty;
    m_mapping = new IndexMapping();
    m_layout = new AxisLayout( countProperty, m_mapping );
  }

  /******************************************** reset ********************************************/
  /**
   * Resets the axis to default state, clearing all mappings, sizes, and visibility overrides.
   */
  public void reset()
  {
    // reset all components to defaults
    m_mapping.clear();
    m_layout.reset();
  }

  // ==============================================================================================
  // Base Axis / Count Property Methods
  // ==============================================================================================

  /************************************** getCountProperty ***************************************/
  /**
   * Returns the observable count property for this axis.
   * 
   * @return read-only integer property representing body cell count
   */
  public ReadOnlyInteger getCountProperty()
  {
    // return observable count property
    return m_countProperty;
  }

  /****************************************** getCount *******************************************/
  /**
   * Returns the current number of body cells in this axis.
   * 
   * @return body cell count
   */
  public int getCount()
  {
    // return current count value
    return m_countProperty.get();
  }

  /***************************************** getVisible ******************************************/
  /**
   * Returns the specified index if visible, otherwise returns the nearest visible index.
   * 
   * @param viewIndex
   *          view index to check
   * @return viewIndex if visible, otherwise nearest visible index (or INVALID if none visible)
   */
  public int getVisible( int viewIndex )
  {
    // return first/last visible for out-of-bounds indices
    if ( viewIndex < FIRSTCELL )
      return getFirstVisible();
    if ( viewIndex >= getCount() )
      return getLastVisible();

    // return index if already visible
    if ( m_layout.isIndexVisible( viewIndex ) )
      return viewIndex;

    // find next visible index
    return getNextVisible( viewIndex );
  }

  /************************************** getFirstVisible ****************************************/
  /**
   * Returns the first visible body cell view index.
   * 
   * @return first visible index, or INVALID if all hidden
   */
  public int getFirstVisible()
  {
    // search forward from header for first visible
    return getNextVisible( HEADER );
  }

  /*************************************** getLastVisible ****************************************/
  /**
   * Returns the last visible body cell view index.
   * 
   * @return last visible index, or INVALID if all hidden
   */
  public int getLastVisible()
  {
    // search backward from end for last visible
    return getPreviousVisible( getCount() );
  }

  /*************************************** getNextVisible ****************************************/
  /**
   * Finds the next visible body cell index after the specified position.
   * 
   * @param viewIndex
   *          starting view index (exclusive search)
   * @return next visible index, or INVALID if none found
   */
  public int getNextVisible( int viewIndex )
  {
    // clamp to valid starting position
    if ( viewIndex < HEADER )
      viewIndex = HEADER;

    int max = getCount();

    // search forward from viewIndex + 1
    for ( int candidate = viewIndex + 1; candidate < max; candidate++ )
      if ( m_layout.isIndexVisible( candidate ) )
        return candidate;

    // if nothing found forward, search backward from viewIndex
    for ( int candidate = viewIndex; candidate > HEADER; candidate-- )
      if ( m_layout.isIndexVisible( candidate ) )
        return candidate;

    return INVALID;
  }

  /************************************* getPreviousVisible **************************************/
  /**
   * Finds the previous visible body cell index before the specified position.
   * 
   * @param viewIndex
   *          starting view index (exclusive search)
   * @return previous visible index, or INVALID if none found
   */
  public int getPreviousVisible( int viewIndex )
  {
    // clamp to valid starting position
    int max = getCount();
    if ( viewIndex > max )
      viewIndex = max;

    // search backward from viewIndex - 1
    for ( int candidate = viewIndex - 1; candidate > HEADER; candidate-- )
      if ( m_layout.isIndexVisible( candidate ) )
        return candidate;

    // if nothing found backward, search forward from viewIndex
    for ( int candidate = viewIndex; candidate < max; candidate++ )
      if ( m_layout.isIndexVisible( candidate ) )
        return candidate;

    return INVALID;
  }

  // ==============================================================================================
  // Index Mapping Methods (Direct access to IndexMapping)
  // ==============================================================================================

  /**************************************** getDataIndex *****************************************/
  /**
   * Converts a view index to its corresponding data index.
   * 
   * @param viewIndex
   *          view index to convert
   * @return corresponding data index, or INVALID if out of bounds
   */
  public int getDataIndex( int viewIndex )
  {
    // validate bounds and handle special header case
    if ( viewIndex < HEADER || viewIndex >= m_countProperty.get() )
      return INVALID;
    if ( viewIndex == HEADER )
      return HEADER;

    // delegate to mapping component
    return m_mapping.getDataIndex( viewIndex );
  }

  /**************************************** getViewIndex *****************************************/
  /**
   * Converts a data index to its corresponding view index.
   * 
   * @param dataIndex
   *          data index to convert
   * @return corresponding view index, or INVALID if not found or out of bounds
   */
  public int getViewIndex( int dataIndex )
  {
    // validate bounds and handle special header case
    if ( dataIndex < HEADER || dataIndex >= m_countProperty.get() )
      return INVALID;
    if ( dataIndex == HEADER )
      return HEADER;

    // delegate to mapping component
    return m_mapping.getViewIndex( dataIndex );
  }

  /******************************************* reorder *******************************************/
  /**
   * Reorders indices by moving a set of indices to a new position.
   * <p>
   * This is typically used when the user drags rows or columns to reorder them.
   * 
   * @param toBeMovedIndexes
   *          set of view indices to move
   * @param insertIndex
   *          target position for insertion
   */
  public void reorder( HashSetInt toBeMovedIndexes, int insertIndex )
  {
    // convert set to sorted array once for both operations
    var sortedMoved = toBeMovedIndexes.toSortedArray();

    // update index mapping and get earliest affected position
    int minAffectedIndex = m_mapping.moveIndices( sortedMoved, insertIndex );

    // invalidate pixel caches from affected position
    m_layout.truncatePixelCaches( minAffectedIndex, 0 );
  }

  /************************************* getIndexMappingHash *************************************/
  /**
   * Returns a hash code representing the current index mapping state.
   * <p>
   * This can be used to detect when reordering has occurred.
   * 
   * @return hash code of index mapping
   */
  public int getIndexMappingHash()
  {
    // delegate to mapping component
    return m_mapping.hashCode();
  }

  // ==============================================================================================
  // Axis Sizing / Layout Methods (Delegated to AxisLayout)
  // ==============================================================================================

  /*************************************** getHeaderPixels ***************************************/
  /**
   * Returns the header size in pixels after applying zoom factor.
   * 
   * @return zoomed header size in pixels
   */
  public int getHeaderPixels()
  {
    // delegate to layout component
    return m_layout.getHeaderPixels();
  }

  /*************************************** getDefaultSize ****************************************/
  /**
   * Returns the nominal default size for body cells (before zoom is applied).
   * 
   * @return default size in nominal pixels
   */
  public int getDefaultSize()
  {
    // delegate to layout component
    return m_layout.getDefaultSize();
  }

  /*************************************** setDefaultSize ****************************************/
  /**
   * Sets the default size for body cells, enforcing minimum size constraint.
   * 
   * @param defaultSize
   *          new default size (minimum 1, enforced to be >= minimum size)
   */
  public void setDefaultSize( int defaultSize )
  {
    // delegate to layout component
    m_layout.setDefaultSize( defaultSize );
  }

  /*************************************** setMinimumSize ****************************************/
  /**
   * Sets the minimum allowed size for all indices, retroactively enforcing it.
   * 
   * @param minSize
   *          new minimum size (must be >= 0)
   */
  public void setMinimumSize( int minSize )
  {
    // delegate to layout component
    m_layout.setMinimumSize( minSize );
  }

  /**************************************** setHeaderSize ****************************************/
  /**
   * Sets the header size in nominal pixels (before zoom is applied).
   * 
   * @param headerSize
   *          new header size (0 to 65535)
   */
  public void setHeaderSize( int headerSize )
  {
    // delegate to layout component
    m_layout.setHeaderSize( headerSize );
  }

  /**************************************** setIndexSize *****************************************/
  /**
   * Sets a specific size for an individual index, overriding the default.
   * 
   * @param viewIndex
   *          view index to set size for
   * @param newSize
   *          new size in nominal pixels (enforced to be >= minimum size)
   */
  public void setIndexSize( int viewIndex, int newSize )
  {
    // delegate to layout component
    m_layout.setIndexSize( viewIndex, newSize );
  }

  /*************************************** getTotalPixels ****************************************/
  /**
   * Returns the total pixel height/width of this axis including header and all body cells.
   * 
   * @return total pixels
   */
  public int getTotalPixels()
  {
    // delegate to layout component
    return m_layout.getTotalPixels();
  }

  /*********************************** getTotalPixelsProperty ************************************/
  /**
   * Returns read-only observable property for total pixels.
   * 
   * @return read-only integer property for total pixels
   */
  public ReadOnlyInteger getTotalPixelsProperty()
  {
    // delegate to layout component
    return m_layout.getTotalPixelsProperty();
  }

  /*************************************** getIndexPixels ****************************************/
  /**
   * Returns the pixel size of the specified index after applying zoom.
   * 
   * @param viewIndex
   *          view index to query (HEADER or body cell index)
   * @return pixel size (0 if hidden)
   */
  public int getIndexPixels( int viewIndex )
  {
    // delegate to layout component
    return m_layout.getIndexPixels( viewIndex );
  }

  /**************************************** getStartPixel ****************************************/
  /**
   * Returns the starting pixel coordinate of the specified index.
   * 
   * @param viewIndex
   *          view index to query
   * @param scrollOffset
   *          current scroll position in pixels to subtract from result
   * @return start pixel coordinate relative to visible viewport
   */
  public int getStartPixel( int viewIndex, int scrollOffset )
  {
    // delegate to layout component
    return m_layout.getStartPixel( viewIndex, scrollOffset );
  }

  /*********************************** getViewIndexAtPixel ************************************/
  /**
   * Finds which view index contains the specified pixel coordinate.
   * 
   * @param coordinate
   *          pixel coordinate to query
   * @param scrollOffset
   *          current scroll position in pixels
   * @return view index at coordinate, or BEFORE/HEADER/AFTER special values
   */
  public int getViewIndexAtPixel( int coordinate, int scrollOffset )
  {
    // delegate to layout component
    return m_layout.getViewIndexAtPixel( coordinate, scrollOffset );
  }

  /************************************** getSizeExceptions **************************************/
  /**
   * Returns an unmodifiable map of all per-index size exceptions.
   * 
   * @return map of data index to nominal size
   */
  public Map<Integer, Integer> getSizeExceptions()
  {
    // delegate to layout component
    return m_layout.getSizeExceptions();
  }

  /************************************* clearSizeExceptions *************************************/
  /**
   * Removes all per-index size exceptions, reverting all to default size.
   */
  public void clearSizeExceptions()
  {
    // delegate to layout component
    m_layout.clearSizeExceptions();
  }

  /*************************************** clearIndexSize ****************************************/
  /**
   * Clears the size exception for a specific index, reverting it to default.
   * 
   * @param viewIndex
   *          view index to clear
   */
  public void clearIndexSize( int viewIndex )
  {
    // delegate to layout component
    m_layout.clearIndexSize( viewIndex );
  }

  /*************************************** setZoomProperty ***************************************/
  /**
   * Sets the zoom property to listen to for display scaling changes.
   * 
   * @param zoomProperty
   *          observable zoom factor (null to disable zoom)
   */
  public void setZoomProperty( ReadOnlyDouble zoomProperty )
  {
    // delegate to layout component
    m_layout.setZoomProperty( zoomProperty );
  }

  /*************************************** isIndexVisible ****************************************/
  /**
   * Checks if the specified view index is currently visible (not hidden).
   * 
   * @param viewIndex
   *          view index to check
   * @return true if visible, false if hidden or out of bounds
   */
  public boolean isIndexVisible( int viewIndex )
  {
    // delegate to layout component
    return m_layout.isIndexVisible( viewIndex );
  }

  /***************************************** hideIndexes *****************************************/
  /**
   * Hides the specified data indices, making them invisible in the view.
   * 
   * @param dataIndexes
   *          set of data indices to hide
   * @return set of indices actually hidden (null if none changed)
   */
  public HashSetInt hideIndexes( HashSetInt dataIndexes )
  {
    // delegate to layout component
    return m_layout.hideDataIndexes( dataIndexes );
  }

  /**************************************** unhideIndexes ****************************************/
  /**
   * Unhides the specified data indices, making them visible in the view.
   * 
   * @param dataIndexes
   *          set of data indices to unhide
   * @return set of indices actually unhidden (null if none changed)
   */
  public HashSetInt unhideIndexes( HashSetInt dataIndexes )
  {
    // delegate to layout component
    return m_layout.unhideDataIndexes( dataIndexes );
  }

  /****************************************** unhideAll ******************************************/
  /**
   * Unhides all currently hidden indices.
   * 
   * @return set of indices that were unhidden (null if none were hidden)
   */
  public HashSetInt unhideAll()
  {
    // delegate to layout component
    return m_layout.unhideAll();
  }

  /************************************** getHiddenIndexes ***************************************/
  /**
   * Returns the set of all currently hidden data indices.
   * 
   * @return set of hidden data indices (may be empty)
   */
  public HashSetInt getHiddenDataIndexes()
  {
    // delegate to layout component
    return m_layout.getHiddenDataIndexes();
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return descriptive string representation
    return Utils.name( this ) + "[count=" + getCount() + "]";
  }

}