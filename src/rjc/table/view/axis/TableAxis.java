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
import rjc.table.signal.IListener;
import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableDouble.ReadOnlyDouble;
import rjc.table.signal.ObservableInteger;
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
 * <li>Map between view positions and underlying data positions (for reordering).</li>
 * <li>Calculate pixel sizes and positions for rendering.</li>
 * <li>Manage per-index sizing exceptions and visibility state.</li>
 * <li>Handle zoom factors for display scaling.</li>
 * <li>Navigate between visible indices (skipping hidden ones).</li>
 * </ul>
 * <p>
 * The axis uses several index types:
 * <ul>
 * <li><b>View index</b>: position in the displayed table (affected by reordering).</li>
 * <li><b>Data index</b>: position in the underlying data model (stable).</li>
 * <li><b>Special values</b>: HEADER (-1), INVALID (-2), BEFORE (Integer.MIN_VALUE + 1), AFTER
 * (Integer.MAX_VALUE - 1).</li>
 * </ul>
 * <p>
 * Assumptions: This class assumes single-threaded access (not thread-safe) and that the provided
 * countProperty signals changes correctly. It also assumes zoom factors are positive non-zero
 * and that data indices remain stable unless explicitly reordered.
 *
 * @see IndexMapping
 * @see IndexSize
 * @see PixelCache
 */
public class TableAxis implements IListener
{
  // standard axis index constants
  public static final int         INVALID   = -2;
  public static final int         HEADER    = -1;
  public static final int         FIRSTCELL = 0;
  public static final int         BEFORE    = Integer.MIN_VALUE + 1;
  public static final int         AFTER     = Integer.MAX_VALUE - 1;

  // axis components
  private final ReadOnlyInteger   m_countProperty;
  private final IndexMapping      m_viewDatamapping;
  private final IndexSize         m_dataIndexSize;
  private final PixelCache        m_startPixelCache;
  private final ObservableInteger m_totalPixelsCache;

  // sizing configuration
  private int                     m_defaultSize;
  private int                     m_minimumSize;
  private int                     m_headerSize;
  private ReadOnlyDouble          m_zoomProperty;

  /***************************************** constructor *****************************************/
  /**
   * Creates a new table axis with the specified cell count.
   *
   * @param countProperty
   *          observable count of body cells in this axis (must be non-null and >= 0).
   * @throws IllegalArgumentException
   *          if countProperty is null or negative.
   */
  public TableAxis( ReadOnlyInteger countProperty )
  {
    // validate count property is usable
    if ( countProperty == null || countProperty.get() < 0 )
      throw new IllegalArgumentException( "Bad body cell count = " + countProperty );

    // initialise components
    m_countProperty = countProperty;
    m_viewDatamapping = new IndexMapping();
    m_dataIndexSize = new IndexSize();
    m_startPixelCache = new PixelCache();
    m_totalPixelsCache = new ObservableInteger( INVALID );

    // listen for count changes
    m_countProperty.addListener( this );

    // apply default settings
    reset();
  }

  /******************************************** reset ********************************************/
  /**
   * Resets the axis to default state, clearing all mappings, sizes, and visibility overrides.
   */
  public void reset()
  {
    // reset mapping and sizing components
    m_viewDatamapping.reset();
    m_dataIndexSize.reset();
    m_startPixelCache.clear();
    m_totalPixelsCache.set( INVALID );

    // set default sizing values
    m_defaultSize = 100;
    m_minimumSize = 20;
    m_headerSize = 50;
  }

  /******************************************** slot *********************************************/
  @Override
  public void slot( ISignal sender, Object... msg )
  {
    // handle count property change
    if ( sender == m_countProperty )
    {
      int oldCount = (int) msg[0];
      int newCount = m_countProperty.get();

      // remove size data beyond new count if reduced
      if ( newCount < oldCount )
        m_dataIndexSize.truncate( newCount );

      // invalidate caches affected by count change
      m_totalPixelsCache.set( INVALID );
      m_startPixelCache.truncate( newCount );
    }
    // handle zoom property change
    else if ( sender == m_zoomProperty )
    {
      // invalidate all pixel caches as zoom affects all calculations
      m_startPixelCache.clear();
      m_totalPixelsCache.set( INVALID );
    }
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

  /****************************************** isVisible ******************************************/
  /**
   * Checks if the specified view index is currently visible (not hidden).
   *
   * @param viewIndex  view index to check
   * @return true if visible, false if hidden or out of bounds
   */
  public boolean isVisible( int viewIndex )
  {
    // check validity and visibility via data index
    if ( viewIndex < FIRSTCELL || viewIndex >= getCount() )
      return false;

    int dataIndex = m_viewDatamapping.getDataIndex( viewIndex );
    return m_dataIndexSize.getNominalSize( dataIndex ) > 0;
  }

  /***************************************** getVisible ******************************************/
  /**
   * Returns the specified index if visible, otherwise returns the nearest visible index.
   * 
   * @param viewIndex  view index to check
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
    if ( isVisible( viewIndex ) )
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
   * @param viewIndex  starting view index (exclusive search)
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
      if ( isVisible( candidate ) )
        return candidate;

    // if nothing found forward, search backward from viewIndex
    for ( int candidate = viewIndex; candidate > HEADER; candidate-- )
      if ( isVisible( candidate ) )
        return candidate;

    return INVALID;
  }

  /************************************* getPreviousVisible **************************************/
  /**
   * Finds the previous visible body cell index before the specified position.
   * 
   * @param viewIndex  starting view index (exclusive search)
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
      if ( isVisible( candidate ) )
        return candidate;

    // if nothing found backward, search forward from viewIndex
    for ( int candidate = viewIndex; candidate < max; candidate++ )
      if ( isVisible( candidate ) )
        return candidate;

    return INVALID;
  }

  // ==============================================================================================
  // Index Mapping Methods
  // ==============================================================================================

  /**************************************** getDataIndex *****************************************/
  /**
   * Converts a view index to its corresponding data index (fast direct lookup).
   * 
   * @param viewIndex  view index to convert
   * @return corresponding data index, or INVALID if out of bounds
   */
  public int getDataIndex( int viewIndex )
  {
    // validate bounds and handle special header case
    if ( viewIndex < HEADER || viewIndex >= getCount() )
      return INVALID;
    if ( viewIndex == HEADER )
      return HEADER;

    // delegate to mapping component
    return m_viewDatamapping.getDataIndex( viewIndex );
  }

  /**************************************** getViewIndex *****************************************/
  /**
   * Converts a data index to its corresponding view index (slow as needs to search).
   * 
   * @param dataIndex  data index to convert
   * @return corresponding view index, or INVALID if not found or out of bounds
   */
  public int getViewIndex( int dataIndex )
  {
    // validate bounds and handle special header case
    if ( dataIndex < HEADER || dataIndex >= getCount() )
      return INVALID;
    if ( dataIndex == HEADER )
      return HEADER;

    // delegate to mapping component
    return m_viewDatamapping.getViewIndex( dataIndex );
  }

  /***************************************** reorderView *****************************************/
  /**
   * Reorders view-indices by moving a set of indices to a new position in the view-to-data mapping.
   * <p>
   * This is typically used when the user drags rows or columns to reorder them.
   *
   * @param viewIndicesToRelocate
   *    set of view indices to relocate
   * @param insertIndex
   *    target position (view index) for insertion
   */
  public void reorderView( HashSetInt viewIndicesToRelocate, int insertIndex )
  {
    // convert set to a sorted array of view indices once
    int[] sortedMovedViewIndices = viewIndicesToRelocate.toSortedArray();

    // update index mapping and get lowest affected view index
    int lowestAffectedViewIndex = m_viewDatamapping.moveIndices( sortedMovedViewIndices, insertIndex );

    // invalidate pixel caches from affected position
    truncatePixelCaches( lowestAffectedViewIndex, 0 );
  }

  /***************************************** reorderData *****************************************/
  /**
   * Reorders data-indices by moving a set of index sizes to a new position.
   * <p>
   * This is typically used when the user drags rows or columns to reorder them.
   * 
   * @param dataIndicesToRelocate
   *    set of view indices to relocate
   * @param insertIndex
   *    target position (view index) for insertion
   */
  public void reorderData( HashSetInt dataIndicesToRelocate, int insertIndex )
  {
    // TODO
    throw new UnsupportedOperationException( "TableAxis.reorderData() not yet implemented" );
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
    return m_viewDatamapping.hashCode();
  }

  // ==============================================================================================
  // Axis Sizing / Layout Methods
  // ==============================================================================================

  /*************************************** getHeaderPixels ***************************************/
  /**
   * Returns the header size in pixels after applying zoom factor.
   * 
   * @return zoomed header size in pixels
   */
  public int getHeaderPixels()
  {
    // apply zoom to nominal header size
    return zoom( m_headerSize );
  }

  /************************************ getDefaultNominalSize ************************************/
  /**
   * Returns the nominal default size for body cells (before zoom is applied).
   * 
   * @return default size in nominal pixels
   */
  public int getDefaultNominalSize()
  {
    // return nominal default size
    return m_defaultSize;
  }

  /************************************ setDefaultNominalSize ************************************/
  /**
   * Sets the nominal default size for body cells, enforcing minimum size constraint.
   * 
   * @param defaultSize  new default size (minimum 1, enforced to be >= minimum size)
   */
  public void setDefaultNominalSize( int defaultSize )
  {
    // enforce minimum size constraint
    if ( defaultSize < m_minimumSize )
      defaultSize = m_minimumSize;

    // update if changed
    if ( m_defaultSize != defaultSize )
    {
      if ( defaultSize < 1 )
        throw new IllegalArgumentException( "Default size must be at least one " + defaultSize );

      // invalidate caches as default size affects most calculations
      m_totalPixelsCache.set( INVALID );
      m_startPixelCache.clear();
      m_defaultSize = defaultSize;
    }
  }

  /************************************ setMinimumNominalSize ************************************/
  /**
   * Sets the nominal minimum allowed size for all indices, retroactively enforcing it.
   * 
   * @param minSize  new minimum size (must be >= 0)
   */
  public void setMinimumNominalSize( int minSize )
  {
    // validate input
    if ( minSize < 0 )
      throw new IllegalArgumentException( "Minimum size must be at least zero " + minSize );

    // update if changed
    if ( m_minimumSize != minSize )
    {
      // ensure default size respects new minimum
      if ( m_defaultSize < minSize )
        setDefaultNominalSize( minSize );

      // enforce new minimum on all existing size exceptions
      if ( minSize > m_minimumSize )
        m_dataIndexSize.applyMinimumSize( (short) minSize );

      // invalidate caches as sizes may have changed
      m_totalPixelsCache.set( INVALID );
      m_startPixelCache.clear();
      m_minimumSize = minSize;
    }
  }

  /************************************ setHeaderNominalSize *************************************/
  /**
   * Sets the nominal header size in nominal pixels (before zoom is applied).
   * 
   * @param headerSize  new header size (0 to 65535)
   */
  public void setHeaderNominalSize( int headerSize )
  {
    // validate input range
    if ( headerSize < 0 || headerSize >= 65536 )
      throw new IllegalArgumentException( "Header size must be at least zero " + headerSize );

    // update if changed
    if ( m_headerSize != headerSize )
    {
      // efficiently update total pixels if currently valid
      if ( getTotalPixels() != INVALID )
        m_totalPixelsCache.set( getTotalPixels() - getHeaderPixels() + zoom( headerSize ) );

      // invalidate start pixel cache as header affects all positions
      m_startPixelCache.clear();
      m_headerSize = headerSize;
    }
  }

  /**************************************** setNominalSize ***************************************/
  /**
   * Sets nominal size for an individual view-index, overriding the default.
   * 
   * @param viewIndex  view index to set size for
   * @param newSize    new size in nominal pixels (enforced to be >= minimum size)
   */
  public void setNominalSize( int viewIndex, int newSize )
  {
    // validate index is within valid body cell range
    if ( viewIndex < FIRSTCELL || viewIndex >= getCount() )
      throw new IndexOutOfBoundsException( "Index=" + viewIndex + " but count=" + getCount() );

    // enforce minimum size constraint
    if ( newSize < m_minimumSize )
      newSize = m_minimumSize;

    // calculate current zoomed size for comparison
    int dataIndex = m_viewDatamapping.getDataIndex( viewIndex );
    short oldNominal = m_dataIndexSize.getNominalSize( dataIndex );
    int zoomOld = 0;
    if ( oldNominal > 0 )
      zoomOld = oldNominal == IndexSize.DEFAULT ? zoom( m_defaultSize ) : zoom( oldNominal );

    // store new nominal size
    m_dataIndexSize.setNominalSize( dataIndex, (short) newSize );

    // update caches only if zoomed pixel size actually changed
    int zoomNew = zoom( newSize );
    if ( zoomNew != zoomOld )
      truncatePixelCaches( viewIndex, zoomNew - zoomOld );
  }

  /*************************************** getTotalPixels ****************************************/
  /**
   * Returns the total pixel height/width of this axis including header and all body cells.
   * 
   * @return total pixels
   */
  public int getTotalPixels()
  {
    // recalculate if cache is invalid
    if ( m_totalPixelsCache.get() == INVALID )
    {
      // sum header pixels plus all body cell pixels
      int pixels = getHeaderPixels() + m_dataIndexSize.calculateTotalPixels( getCount(), m_defaultSize,
          m_zoomProperty != null ? m_zoomProperty.get() : 1.0 );
      m_totalPixelsCache.set( pixels );
    }
    return m_totalPixelsCache.get();
  }

  /*********************************** getTotalPixelsProperty ************************************/
  /**
   * Returns read-only observable property for total pixels.
   * 
   * @return read-only integer property for total pixels
   */
  public ReadOnlyInteger getTotalPixelsProperty()
  {
    // return read-only wrapper of cached property
    return m_totalPixelsCache.getReadOnly();
  }

  /**************************************** getPixelSize *****************************************/
  /**
   * Returns the pixel size of the specified index after applying zoom.
   * 
   * @param viewIndex  view index to query (HEADER or body cell index)
   * @return pixel size (0 if hidden)
   */
  public int getPixelSize( int viewIndex )
  {
    // return zoomed header size
    if ( viewIndex == HEADER )
      return getHeaderPixels();

    // convert to data index and check for hidden or sized state
    int dataIndex = m_viewDatamapping.getDataIndex( viewIndex );
    short nominalSize = m_dataIndexSize.getNominalSize( dataIndex );
    if ( nominalSize <= 0 )
      return 0; // hidden

    // return zoomed default or exception size
    return nominalSize == IndexSize.DEFAULT ? zoom( m_defaultSize ) : zoom( nominalSize );
  }

  /*************************************** getNominalSize ****************************************/
  /**
   * Returns the nominal size of the specified data index (before zoom is applied).
   * 
   * @param dataIndex  data index to query
   * @return nominal size in pixels (DEFAULT if default, negative if hidden) 
   */
  public short getNominalSize( int dataIndex )
  {
    // return nominal size for specified data index
    return m_dataIndexSize.getNominalSize( dataIndex );
  }

  /**************************************** getPixelStart ****************************************/
  /**
   * Returns the starting pixel coordinate of the specified index.
   * 
   * @param viewIndex     view index to query
   * @param scrollOffset  current scroll position in pixels to subtract from result
   * @return start pixel coordinate relative to visible viewport
   */
  public int getPixelStart( int viewIndex, int scrollOffset )
  {
    // validate and clamp to valid range
    if ( viewIndex < HEADER )
      throw new IndexOutOfBoundsException( "index=" + viewIndex + " but count=" + getCount() );
    if ( viewIndex > getCount() )
      viewIndex = getCount();

    // header always starts at coordinate 0
    if ( viewIndex == HEADER )
      return 0;

    // extend cache incrementally if needed up to requested index
    if ( viewIndex >= m_startPixelCache.size() )
    {
      // initialise cache with header size if empty
      if ( m_startPixelCache.isEmpty() )
        m_startPixelCache.add( getHeaderPixels() );

      // grow cache one entry at a time to requested index
      int position = m_startPixelCache.size() - 1;
      int start = m_startPixelCache.get( position );
      while ( viewIndex > position )
      {
        start += getPixelSize( position++ );
        m_startPixelCache.add( start );
      }
    }

    // return cached start position adjusted for scroll
    return m_startPixelCache.get( viewIndex ) - scrollOffset;
  }

  /************************************* getViewIndexAtPixel *************************************/
  /**
   * Finds which view index contains the specified pixel coordinate.
   * 
   * @param coordinate  pixel coordinate to query
   * @param scrollOffset  current scroll position in pixels
   * @return view index at coordinate, or BEFORE/HEADER/AFTER special values
   */
  public int getViewIndexAtPixel( int coordinate, int scrollOffset )
  {
    // check if before visible area
    if ( coordinate < 0 )
      return BEFORE;

    // check if within header area
    if ( coordinate < getHeaderPixels() )
      return HEADER;

    // adjust coordinate for scroll position
    coordinate += scrollOffset;

    // check if after entire table
    if ( coordinate >= getTotalPixels() )
      return AFTER;

    // get current cache end position
    int position = m_startPixelCache.size() - 1;
    int start = position < 0 ? 0 : m_startPixelCache.get( position );

    // extend cache forward if coordinate is beyond cached range
    if ( coordinate > start )
    {
      while ( coordinate > start )
      {
        start += getPixelSize( position++ );
        m_startPixelCache.add( start );
      }
      // coordinate exactly at boundary returns next index, else previous
      return coordinate == start ? position : position - 1;
    }

    // binary search cached range for coordinate
    return m_startPixelCache.getIndex( coordinate );
  }

  /************************************* getSizeExceptions ***************************************/
  /**
   * Returns a map of all per-index size exceptions (excluding default and hidden sizes).
   * 
   * @return map of data index to nominal size (excludes default and hidden sizes)
   */
  public Map<Integer, Short> getSizeExceptions()
  {
    // delegate to internal storage
    return m_dataIndexSize.getSizeExceptions();
  }

  /************************************ clearSizeExceptions **************************************/
  /**
   * Removes all per-index size exceptions, reverting all to default size.
   */
  public void clearSizeExceptions()
  {
    // clear all size exceptions and invalidate all caches
    m_dataIndexSize.clearExceptions();
    m_startPixelCache.clear();
    m_totalPixelsCache.set( INVALID );
  }

  /************************************** clearIndexSize *****************************************/
  /**
   * Clears the size exception for a specific index, reverting it to default.
   * 
   * @param viewIndex   view index to clear
   */
  public void clearIndexSize( int viewIndex )
  {
    // validate index is within valid body cell range
    if ( viewIndex < FIRSTCELL || viewIndex >= getCount() )
      throw new IndexOutOfBoundsException( "cell index=" + viewIndex + " but count=" + getCount() );

    // clear size exception and invalidate caches if changed
    int dataIndex = m_viewDatamapping.getDataIndex( viewIndex );
    if ( m_dataIndexSize.resetNominalSize( dataIndex ) )
    {
      m_totalPixelsCache.set( INVALID );
      m_startPixelCache.clear();
    }
  }

  /*************************************** setZoomProperty ***************************************/
  /**
   * Sets the zoom property to listen to for display scaling changes.
   * 
   * @param zoomProperty  observable zoom factor (null to disable zoom)
   */
  public void setZoomProperty( ReadOnlyDouble zoomProperty )
  {
    // remove listener from previous property
    if ( m_zoomProperty != null )
      m_zoomProperty.removeListener( this );

    // add listener to new property if not null
    if ( zoomProperty != null )
      zoomProperty.addListener( this );

    // update property reference and invalidate all pixel caches
    m_zoomProperty = zoomProperty;
    m_startPixelCache.clear();
    m_totalPixelsCache.set( INVALID );
  }

  /*************************************** hideDataIndexes ***************************************/
  /**
   * Hides the specified data indices, making them invisible in the view.
   * 
   * @param dataIndexes  set of data indices to hide
   * @return set of indices actually hidden (null if none changed)
   */
  public HashSetInt hideDataIndexes( HashSetInt dataIndexes )
  {
    // attempt to hide each index and track which actually changed
    var hidden = new HashSetInt();
    for ( int index : dataIndexes.toArray() )
      if ( m_dataIndexSize.hide( index ) )
        hidden.add( index );

    // return null if no changes occurred
    if ( hidden.isEmpty() )
      return null;

    // invalidate all pixel caches
    m_startPixelCache.clear();
    m_totalPixelsCache.set( INVALID );
    return hidden;
  }

  /************************************** unhideDataIndexes **************************************/
  /**
   * Unhides the specified data indices, making them visible in the view.
   * 
   * @param dataIndexes  set of data indices to unhide
   * @return set of indices actually unhidden (null if none changed)
   */
  public HashSetInt unhideDataIndexes( HashSetInt dataIndexes )
  {
    // attempt to unhide each index and track which actually changed
    var shown = new HashSetInt();
    for ( int index : dataIndexes.toArray() )
      if ( m_dataIndexSize.unhide( index ) )
        shown.add( index );

    // return null if no changes occurred
    if ( shown.isEmpty() )
      return null;

    // invalidate all pixel caches
    m_startPixelCache.clear();
    m_totalPixelsCache.set( INVALID );
    return shown;
  }

  /****************************************** unhideAll ******************************************/
  /**
   * Unhides all currently hidden indices.
   * 
   * @return set of indices that were unhidden (null if none were hidden)
   */
  public HashSetInt unhideAll()
  {
    // delegate to data index size handler
    var shown = m_dataIndexSize.unhideAll();

    // return null if no changes occurred
    if ( shown.isEmpty() )
      return null;

    // invalidate all pixel caches
    m_startPixelCache.clear();
    m_totalPixelsCache.set( INVALID );
    return shown;
  }

  /*********************************** getHiddenDataIndexes **************************************/
  /**
   * Returns the set of all currently hidden data indices.
   * 
   * @return set of hidden data indices (may be empty)
   */
  public HashSetInt getHiddenDataIndexes()
  {
    // delegate to data index size storage
    return m_dataIndexSize.getHiddenIndexes();
  }

  /******************************************** zoom *********************************************/
  /**
   * Applies current zoom factor to a nominal size value.
   * 
   * @param size  nominal size
   * @return zoomed size in pixels (unchanged if no zoom property set)
   */
  private int zoom( int size )
  {
    // return unchanged if no zoom property
    if ( m_zoomProperty == null )
      return size;

    // apply zoom factor and round to integer
    return (int) ( size * m_zoomProperty.get() );
  }

  /************************************* truncatePixelCaches *************************************/
  /**
   * Invalidates pixel caches from the specified index onwards, optionally adjusting total pixels.
   * 
   * @param fromViewIndex  view index from which to invalidate caches
   * @param deltaPixels    change in total pixels to apply (0 for full recalculation)
   */
  private void truncatePixelCaches( int fromViewIndex, int deltaPixels )
  {
    // efficiently update total if valid and delta provided
    if ( m_totalPixelsCache.get() != INVALID )
      m_totalPixelsCache.set( m_totalPixelsCache.get() + deltaPixels );

    // invalidate start pixel cache from affected index
    m_startPixelCache.truncate( fromViewIndex );
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return descriptive string representation
    return Utils.name( this ) + "[count=" + getCount() + "]";
  }

}