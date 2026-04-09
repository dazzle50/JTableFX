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
  private final IndexMapping      m_viewDataMapping;
  private final IndexSize         m_dataNominalSize;
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
    m_viewDataMapping = new IndexMapping();
    m_dataNominalSize = new IndexSize();
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
    m_viewDataMapping.reset();
    m_dataNominalSize.reset();
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
        m_dataNominalSize.truncate( newCount );

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

    int dataIndex = m_viewDataMapping.getDataIndex( viewIndex );
    return m_dataNominalSize.getSize( dataIndex ) > 0;
  }

  /**************************************** getAllVisible ****************************************/
  /**
   * Returns an array of all body cell view indices that are visible.
   * 
   * @return array of all visible view indices
   */
  public int[] getAllVisible()
  {
    // allocate array (worst case: all indices visible)
    int count = getCount();
    int[] visible = new int[count];
    int visibleCount = 0;
    int pixelStart = getPixelStart( 0, 0 );

    // scan through all indices checking for non-zero pixel spans
    for ( int viewIndex = 0; viewIndex < count; viewIndex++ )
    {
      int pixelEnd = getPixelStart( viewIndex + 1, 0 );
      if ( pixelEnd - pixelStart > 0 )
        visible[visibleCount++] = viewIndex;
      pixelStart = pixelEnd;
    }

    // if all visible, return array containing all indices, otherwise trim to actual visible count
    if ( visibleCount == count )
      return visible;
    int[] trimmed = new int[visibleCount];
    System.arraycopy( visible, 0, trimmed, 0, visibleCount );
    return trimmed;
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
    {
      int dataIndex = m_viewDataMapping.getDataIndex( candidate );
      if ( m_dataNominalSize.getSize( dataIndex ) > 0 )
        return candidate;
    }

    // if nothing found forward, search backward from viewIndex
    for ( int candidate = viewIndex; candidate > HEADER; candidate-- )
    {
      int dataIndex = m_viewDataMapping.getDataIndex( candidate );
      if ( m_dataNominalSize.getSize( dataIndex ) > 0 )
        return candidate;
    }

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
    {
      int dataIndex = m_viewDataMapping.getDataIndex( candidate );
      if ( m_dataNominalSize.getSize( dataIndex ) > 0 )
        return candidate;
    }

    // if nothing found backward, search forward from viewIndex
    for ( int candidate = viewIndex; candidate < max; candidate++ )
    {
      int dataIndex = m_viewDataMapping.getDataIndex( candidate );
      if ( m_dataNominalSize.getSize( dataIndex ) > 0 )
        return candidate;
    }

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
    return m_viewDataMapping.getDataIndex( viewIndex );
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
    return m_viewDataMapping.getViewIndex( dataIndex );
  }

  /*************************************** getDataIndexes ****************************************/
  /**
   * Converts a set of view indexes into an array of their corresponding data indexes.
   *
   * @param viewIndexes  set of view indices to convert
   * @return array of corresponding data indices
   */
  public int[] getDataIndexes( HashSetInt viewIndexes )
  {
    // allocate array for mapped results
    int[] dataIndexes = new int[viewIndexes.size()];
    var it = viewIndexes.iterator();

    // map each view index to its underlying data index
    for ( int i = 0; i < dataIndexes.length; i++ )
      dataIndexes[i] = getDataIndex( it.next() );

    return dataIndexes;
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
    int lowestAffectedViewIndex = m_viewDataMapping.moveIndices( sortedMovedViewIndices, insertIndex );

    // invalidate pixel caches from affected position
    truncatePixelCaches( lowestAffectedViewIndex, 0 );
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
    return m_viewDataMapping.hashCode();
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
        m_dataNominalSize.applyMinimumSize( (short) minSize );

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
   * Sets nominal size for an individual data-index, overriding the default.
   * 
   * @param dataIndex  data index to set size for
   * @param newSize    new size in nominal pixels (enforced to be >= minimum size)
   */
  public void setNominalSize( int dataIndex, int newSize )
  {
    // validate index is within valid body cell range
    if ( dataIndex < FIRSTCELL || dataIndex >= getCount() )
      throw new IndexOutOfBoundsException( "Index=" + dataIndex + " but count=" + getCount() );

    // enforce minimum size constraint, being aware negative sizes indicates hidden
    if ( newSize < 0 )
      newSize = -newSize < m_minimumSize ? -m_minimumSize : newSize;
    else
      newSize = newSize < m_minimumSize ? m_minimumSize : newSize;

    // calculate current zoomed size for comparison
    short oldNominal = m_dataNominalSize.getSize( dataIndex );
    int zoomOld = 0;
    if ( oldNominal > 0 )
      zoomOld = oldNominal == IndexSize.DEFAULT ? zoom( m_defaultSize ) : zoom( oldNominal );

    // store new nominal size
    m_dataNominalSize.setSize( dataIndex, (short) newSize );

    // update caches only if zoomed pixel size actually changed
    int zoomNew = newSize < 0 ? 0 : ( newSize == IndexSize.DEFAULT ? zoom( m_defaultSize ) : zoom( newSize ) );

    if ( zoomNew != zoomOld )
    {
      int viewIndex = m_viewDataMapping.getViewIndex( dataIndex );
      truncatePixelCaches( viewIndex, zoomNew - zoomOld );
    }
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
      int pixels = getHeaderPixels() + m_dataNominalSize.calculateTotalPixels( getCount(), m_defaultSize,
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
    int dataIndex = m_viewDataMapping.getDataIndex( viewIndex );
    short nominalSize = m_dataNominalSize.getSize( dataIndex );
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
    return m_dataNominalSize.getSize( dataIndex );
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

  /************************************ getNonDefaultIndexes *************************************/
  /**
   * Returns a map of all cell data-indexes with non-default size or hidden.
   * 
   * @return map of data-index to size (negative if hidden)
   */
  public Map<Integer, Short> getNonDefaultIndexes()
  {
    // delegate to internal storage
    return m_dataNominalSize.getNonDefaultIndexes();
  }

  /*************************************** resetSizeOfAll ****************************************/
  /**
   * Resets sizes of all indices to default size preserving hidden state.
   */
  public void resetSizeOfAll()
  {
    // clear all size exceptions and invalidate all caches
    m_dataNominalSize.resetSizeOfAll();
    m_startPixelCache.clear();
    m_totalPixelsCache.set( INVALID );
  }

  /****************************************** resetSize ******************************************/
  /**
   * Resets sizes of specified index to default size preserving hidden state.
   * 
   * @param dataIndex   data index to clear size for
   */
  public void resetSize( int dataIndex )
  {
    // validate index is within valid body cell range
    if ( dataIndex < FIRSTCELL || dataIndex >= getCount() )
      throw new IndexOutOfBoundsException( "cell index=" + dataIndex + " but count=" + getCount() );

    // clear size exception and invalidate caches if changed
    if ( m_dataNominalSize.resetSize( dataIndex ) )
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

  /******************************************** hide *********************************************/
  /**
   * Hides the specified data indices, making them invisible in the view.
   * 
   * @param dataIndexes  set of data indices to hide
   * @return set of indices actually hidden (null if none changed)
   */
  public HashSetInt hide( HashSetInt dataIndexes )
  {
    // attempt to hide each index and track which actually changed
    var hidden = new HashSetInt();
    for ( int index : dataIndexes.toArray() )
      if ( m_dataNominalSize.hide( index ) )
        hidden.add( index );

    // return null if no changes occurred
    if ( hidden.isEmpty() )
      return null;

    // invalidate all pixel caches
    m_startPixelCache.clear();
    m_totalPixelsCache.set( INVALID );
    return hidden;
  }

  /******************************************* unhide ********************************************/
  /**
   * Unhides the specified data indices, making them visible in the view.
   * 
   * @param dataIndexes  set of data indices to unhide
   * @return set of indices actually unhidden (null if none changed)
   */
  public HashSetInt unhide( HashSetInt dataIndexes )
  {
    // attempt to unhide each index and track which actually changed
    var shown = new HashSetInt();
    for ( int index : dataIndexes.toArray() )
      if ( m_dataNominalSize.unhide( index ) )
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
    var shown = m_dataNominalSize.unhideAll();

    // return null if no changes occurred
    if ( shown.isEmpty() )
      return null;

    // invalidate all pixel caches
    m_startPixelCache.clear();
    m_totalPixelsCache.set( INVALID );
    return shown;
  }

  /*********************************** getHiddenViewIndexes **************************************/
  /**
   * Returns the set of all currently hidden view indices.
   * 
   * @return set of hidden view indices (may be empty)
   */
  public HashSetInt getHiddenViewIndexes()
  {
    // delegate to data index size storage
    var dataIndexes = m_dataNominalSize.getHiddenIndexes();

    // convert data indexes to view indexes
    var viewIndexes = new HashSetInt( dataIndexes.size() );
    var it = dataIndexes.iterator();
    while ( it.hasNext() )
      viewIndexes.add( m_viewDataMapping.getViewIndex( it.next() ) );

    return viewIndexes;
  }

  /***************************************** setMapping ******************************************/
  /**
   * Sets a new view-to-data index mapping for this axis (used when sorting).
   * 
   * @param newMapping  array mapping view indices to data indices
   */
  public void setMapping( int[] newMapping )
  {
    // apply the index mapping to only-visible indices
    m_viewDataMapping.setMapping( newMapping, m_dataNominalSize, getCount() );
    m_startPixelCache.clear();
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

  /**************************************** deleteMapping ****************************************/
  /**
   * Removes all view-to-data mapping entries whose data index falls within the deleted run,
   * shifts remaining entries down, and decrements stored data values above the run.
   * <p>
   * Must be called <em>before</em> the corresponding data deletion, consistent with
   * {@link #deleteSizes}.
   *
   * @param dataStart first data index of the deleted run
   * @param dataCount number of consecutive data indexes deleted
   * @return captured {@code {viewIndex, dataIndex}} pairs for restoration by {@link #insertMapping}
   */
  public int[][] deleteMapping( int dataStart, int dataCount )
  {
    // delegate to mapping component; pixel caches invalidated by deleteSizes caller
    return m_viewDataMapping.deleteMapping( dataStart, dataCount );
  }

  /**************************************** insertMapping ****************************************/
  /**
   * Reverses a previous {@link #deleteMapping} call, restoring all captured view-to-data entries.
   * <p>
   * Must be called <em>after</em> the corresponding data insertion, consistent with
   * {@link #insertSizes}.
   *
   * @param dataStart first data index of the restored run
   * @param dataCount number of consecutive data indexes restored
   * @param captured  pairs returned by the corresponding {@link #deleteMapping} call
   */
  public void insertMapping( int dataStart, int dataCount, int[][] captured )
  {
    // delegate to mapping component; pixel caches invalidated by insertSizes caller
    m_viewDataMapping.insertMapping( dataStart, dataCount, captured );
  }

  /**
   * Adjusts the view-to-data index mapping to account for {@code dataCount} newly inserted
   * data entries starting at {@code dataStart}.
   * <p>
   * Must be called <em>after</em> the corresponding data insertion, consistent with
   * {@link #insertSizes(int, int)}.
   *
   * @param dataStart first data index of the inserted run
   * @param dataCount number of consecutive data indexes inserted
   */
  public void insertMapping( int dataStart, int dataCount )
  {
    // delegate to mapping component; pixel caches are invalidated by insertSizes
    m_viewDataMapping.insertMapping( dataStart, dataCount );
  }

  /***************************************** deleteSizes *****************************************/
  /**
   * Removes the nominal sizes for a contiguous range of data indices, shifting all higher
   * indices down, and returns the captured values for later restoration by {@link #insertSizes}.
   * <p>
   * This must be called <em>before</em> the corresponding data deletion so that the subsequent
   * count-change signal's {@code truncate} call becomes a no-op.
   *
   * @param start  first data index of the range to remove
   * @param count  number of consecutive data indices to remove
   * @return captured size values, suitable for passing to {@link #insertSizes}
   */
  public short[] deleteSizes( int start, int count )
  {
    // remove range and shift tail; full cache invalidation required
    short[] captured = m_dataNominalSize.deleteSizes( start, count );
    m_startPixelCache.clear();
    m_totalPixelsCache.set( INVALID );
    return captured;
  }

  /***************************************** insertSizes *****************************************/
  /**
   * Restores nominal sizes previously captured by {@link #deleteSizes}, inserting them at
   * {@code start} and shifting all higher existing entries upward.
   * <p>
   * Call this <em>after</em> the corresponding data insertion so that the count is already
   * correct before pixel caches are invalidated.
   *
   * @param start  data index at which to insert
   * @param sizes  values previously returned by {@link #deleteSizes}
   */
  public void insertSizes( int start, short[] sizes )
  {
    // insert sizes and shift tail; full cache invalidation required
    m_dataNominalSize.insertSizes( start, sizes );
    m_startPixelCache.clear();
    m_totalPixelsCache.set( INVALID );
  }

  /**
   * Inserts {@code count} default-sized entries at {@code start} in the nominal-size store,
   * shifting all higher entries upward, then invalidates pixel caches.
   * <p>
   * Call this <em>after</em> the corresponding data insertion so that the count-change signal
   * has already fired before caches are invalidated.
   *
   * @param start data index at which to insert
   * @param count number of consecutive default-sized entries to insert
   */
  public void insertSizes( int start, int count )
  {
    // insert default slots, shift tail, then invalidate all pixel caches
    m_dataNominalSize.insertSizes( start, count );
    m_startPixelCache.clear();
    m_totalPixelsCache.set( INVALID );
  }

  /****************************************** swapSizes ******************************************/
  /**
   * Swaps the nominal sizes of two data indices, invalidating pixel caches.
   * Used during reorder operations to keep index sizes aligned with data positions.
   *
   * @param dataIndex1 first data index
   * @param dataIndex2 second data index
   */
  public void swapSizes( int dataIndex1, int dataIndex2 )
  {
    short s1 = m_dataNominalSize.getSize( dataIndex1 );
    short s2 = m_dataNominalSize.getSize( dataIndex2 );
    // skip if identical — avoids redundant cache thrashing during batch reorders
    if ( s1 == s2 )
      return;
    m_dataNominalSize.setSize( dataIndex1, s2 );
    m_dataNominalSize.setSize( dataIndex2, s1 );
    m_startPixelCache.clear();
    m_totalPixelsCache.set( INVALID );
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