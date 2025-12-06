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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import rjc.table.HashSetInt;
import rjc.table.signal.IListener;
import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableDouble.ReadOnlyDouble;
import rjc.table.signal.ObservableInteger;
import rjc.table.signal.ObservableInteger.ReadOnlyInteger;

/*************************************************************************************************/
/**************** Component handling axis sizing, pixel caching and zoom logic *******************/
/*************************************************************************************************/

/**
 * Manages axis sizing, pixel coordinate caching, and zoom scaling for table rows or columns.
 * <p>
 * This component handles the calculations required to convert between view-index positions and
 * pixel coordinates in a table axis. It maintains caches to optimise performance when calculating
 * start positions and total dimensions, and automatically invalidates these caches when sizing
 * properties change.
 * <p>
 * The layout supports:
 * <ul>
 * <li>Default sizing with per-index size exceptions</li>
 * <li>Minimum size enforcement</li>
 * <li>Header sizing separate from body cells</li>
 * <li>Zoom factor application for display scaling</li>
 * <li>Hiding and showing individual indices</li>
 * <li>Efficient pixel coordinate to index lookups</li>
 * </ul>
 * <p>
 * Pixel calculations are cached incrementally on demand and invalidated when relevant properties
 * change. The layout listens to the count property and optional zoom property to maintain cache
 * consistency.
 * 
 * @see TableAxis
 * @see IndexMapping
 * @see IndexSize
 * @see PixelCache
 */
public class AxisLayout implements IListener
{
  private final ReadOnlyInteger   m_countProperty;

  private int                     m_defaultSize;
  private int                     m_minimumSize;
  private int                     m_headerSize;
  private ReadOnlyDouble          m_zoomProperty;
  private IndexMapping            m_indexMapping;

  private final IndexSize         m_dataIndexSize    = new IndexSize();
  private final PixelCache        m_startPixelCache  = new PixelCache();
  private final ObservableInteger m_totalPixelsCache = new ObservableInteger( TableAxis.INVALID );

  /*************************************** constructor *******************************************/
  /**
   * Creates a new axis layout component.
   * 
   * @param countProperty
   *          observable count of body cells in this axis
   * @param indexMapping
   *          mapping between view and data indices
   */
  public AxisLayout( ReadOnlyInteger countProperty, IndexMapping indexMapping )
  {
    // store count property and listen for changes
    m_countProperty = countProperty;
    m_countProperty.addListener( this );
    m_indexMapping = indexMapping;
    reset();
  }

  /******************************************* reset *********************************************/
  /**
   * Resets all sizing configuration to defaults and clears all caches.
   */
  public void reset()
  {
    // set default sizing values
    m_defaultSize = 100;
    m_minimumSize = 20;
    m_headerSize = 50;

    // clear internal storage and invalidate caches
    m_dataIndexSize.clear();
    m_startPixelCache.clear();
    m_totalPixelsCache.set( TableAxis.INVALID );
  }

  /******************************************* slot **********************************************/
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
      m_totalPixelsCache.set( TableAxis.INVALID );
      m_startPixelCache.truncate( newCount );
    }
    // handle zoom property change
    else if ( sender == m_zoomProperty )
    {
      // invalidate all pixel caches as zoom affects all calculations
      m_startPixelCache.clear();
      m_totalPixelsCache.set( TableAxis.INVALID );
    }
  }

  /************************************** getHeaderPixels ****************************************/
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

  /*************************************** getDefaultSize ****************************************/
  /**
   * Returns the nominal default size for body cells (before zoom is applied).
   * 
   * @return default size in nominal pixels
   */
  public int getDefaultSize()
  {
    // return nominal default size
    return m_defaultSize;
  }

  /*************************************** setDefaultSize ****************************************/
  /**
   * Sets the default size for body cells, enforcing minimum size constraint.
   * 
   * @param defaultSize
   *          new default size (minimum 1, enforced to be >= minimum size)
   * @throws IllegalArgumentException
   *           if defaultSize is less than 1
   */
  public void setDefaultSize( int defaultSize )
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
      m_totalPixelsCache.set( TableAxis.INVALID );
      m_startPixelCache.clear();
      m_defaultSize = defaultSize;
    }
  }

  /*************************************** setMinimumSize ****************************************/
  /**
   * Sets the minimum allowed size for all indices, retroactively enforcing it on existing size
   * exceptions.
   * 
   * @param minSize
   *          new minimum size (must be >= 0)
   * @throws IllegalArgumentException
   *           if minSize is negative
   */
  public void setMinimumSize( int minSize )
  {
    // validate input
    if ( minSize < 0 )
      throw new IllegalArgumentException( "Minimum size must be at least zero " + minSize );

    // update if changed
    if ( m_minimumSize != minSize )
    {
      // ensure default size respects new minimum
      if ( m_defaultSize < minSize )
        setDefaultSize( minSize );

      // enforce new minimum on all existing size exceptions
      if ( minSize > m_minimumSize )
        m_dataIndexSize.applyMinimumSize( (short) minSize );

      // invalidate caches as sizes may have changed
      m_totalPixelsCache.set( TableAxis.INVALID );
      m_startPixelCache.clear();
      m_minimumSize = minSize;
    }
  }

  /**************************************** setHeaderSize ****************************************/
  /**
   * Sets the header size in nominal pixels (before zoom is applied).
   * 
   * @param headerSize
   *          new header size (0 to 65535)
   * @throws IllegalArgumentException
   *           if headerSize is outside valid range
   */
  public void setHeaderSize( int headerSize )
  {
    // validate input range
    if ( headerSize < 0 || headerSize >= 65536 )
      throw new IllegalArgumentException( "Header size must be at least zero " + headerSize );

    // update if changed
    if ( m_headerSize != headerSize )
    {
      // efficiently update total pixels if currently valid
      if ( getTotalPixels() != TableAxis.INVALID )
        m_totalPixelsCache.set( getTotalPixels() - getHeaderPixels() + zoom( headerSize ) );

      // invalidate start pixel cache as header affects all positions
      m_startPixelCache.clear();
      m_headerSize = headerSize;
    }
  }

  /**************************************** setIndexSize *****************************************/
  /**
   * Sets a specific size for an individual index, overriding the default size.
   * 
   * @param viewIndex
   *          view index to set size for (must be valid body cell index)
   * @param newSize
   *          new size in nominal pixels (enforced to be >= minimum size)
   * @throws IndexOutOfBoundsException
   *           if viewIndex is outside valid range
   */
  public void setIndexSize( int viewIndex, int newSize )
  {
    // validate index is within valid body cell range
    if ( viewIndex < TableAxis.FIRSTCELL || viewIndex >= m_countProperty.get() )
      throw new IndexOutOfBoundsException( "Index=" + viewIndex + " but count=" + m_countProperty.get() );

    // enforce minimum size constraint
    if ( newSize < m_minimumSize )
      newSize = m_minimumSize;

    // calculate current zoomed size for comparison
    int dataIndex = m_indexMapping.getDataIndex( viewIndex );
    short oldNominal = m_dataIndexSize.getSize( dataIndex );
    int zoomOld = 0;
    if ( oldNominal > 0 )
      zoomOld = oldNominal == IndexSize.DEFAULT ? zoom( m_defaultSize ) : zoom( oldNominal );

    // store new nominal size
    m_dataIndexSize.setSize( dataIndex, (short) newSize );

    // update caches only if zoomed pixel size actually changed
    int zoomNew = zoom( newSize );
    if ( zoomNew != zoomOld )
      truncatePixelCaches( viewIndex, zoomNew - zoomOld );
  }

  /*************************************** getTotalPixels ****************************************/
  /**
   * Returns the total pixel height/width of this axis including header and all body cells.
   * 
   * @return total pixels, or INVALID if not yet calculated
   */
  public int getTotalPixels()
  {
    // recalculate if cache is invalid
    if ( m_totalPixelsCache.get() == TableAxis.INVALID )
    {
      // sum header pixels plus all body cell pixels
      int pixels = getHeaderPixels() + m_dataIndexSize.calculateTotalPixels( m_countProperty.get(), m_defaultSize,
          m_zoomProperty != null ? m_zoomProperty.get() : 1.0 );
      m_totalPixelsCache.set( pixels );
    }
    return m_totalPixelsCache.get();
  }

  /*********************************** getTotalPixelsProperty ************************************/
  /**
   * Returns read-only observable property for total pixels, enabling change notifications.
   * 
   * @return read-only integer property
   */
  public ReadOnlyInteger getTotalPixelsProperty()
  {
    // return read-only wrapper of cached property
    return m_totalPixelsCache.getReadOnly();
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
    // return zoomed header size
    if ( viewIndex == TableAxis.HEADER )
      return getHeaderPixels();

    // convert to data index and check for hidden or sized state
    int dataIndex = m_indexMapping.getDataIndex( viewIndex );
    short nominalSize = m_dataIndexSize.getSize( dataIndex );
    if ( nominalSize <= 0 )
      return 0; // hidden

    // return zoomed default or exception size
    return nominalSize == IndexSize.DEFAULT ? zoom( m_defaultSize ) : zoom( nominalSize );
  }

  /**************************************** getStartPixel ****************************************/
  /**
   * Returns the starting pixel coordinate of the specified index, accounting for scroll offset.
   * <p>
   * Builds the start pixel cache on demand as indices are accessed. The cache is grown
   * incrementally rather than all at once for efficiency.
   * 
   * @param viewIndex
   *          view index to query (HEADER or body cell index)
   * @param scrollOffset
   *          current scroll position in pixels to subtract from result
   * @return start pixel coordinate relative to visible viewport
   * @throws IndexOutOfBoundsException
   *           if viewIndex < HEADER
   */
  public int getStartPixel( int viewIndex, int scrollOffset )
  {
    // validate and clamp to valid range
    if ( viewIndex < TableAxis.HEADER )
      throw new IndexOutOfBoundsException( "index=" + viewIndex + " but count=" + m_countProperty.get() );
    if ( viewIndex > m_countProperty.get() )
      viewIndex = m_countProperty.get();

    // header always starts at coordinate 0
    if ( viewIndex == TableAxis.HEADER )
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
        start += getIndexPixels( position++ );
        m_startPixelCache.add( start );
      }
    }

    // return cached start position adjusted for scroll
    return m_startPixelCache.get( viewIndex ) - scrollOffset;
  }

  /*********************************** getViewIndexAtPixel ************************************/
  /**
   * Finds which view index contains the specified pixel coordinate.
   * <p>
   * Extends the start pixel cache as needed to locate the correct index via binary search.
   * 
   * @param coordinate
   *          pixel coordinate to query (relative to top/left of viewport)
   * @param scrollOffset
   *          current scroll position in pixels to add to coordinate
   * @return view index at coordinate, or BEFORE/HEADER/AFTER special values
   */
  public int getViewIndexAtPixel( int coordinate, int scrollOffset )
  {
    // check if before visible area
    if ( coordinate < 0 )
      return TableAxis.BEFORE;

    // check if within header area
    if ( coordinate < getHeaderPixels() )
      return TableAxis.HEADER;

    // adjust coordinate for scroll position
    coordinate += scrollOffset;

    // check if after entire table
    if ( coordinate >= getTotalPixels() )
      return TableAxis.AFTER;

    // get current cache end position
    int position = m_startPixelCache.size() - 1;
    int start = position < 0 ? 0 : m_startPixelCache.get( position );

    // extend cache forward if coordinate is beyond cached range
    if ( coordinate > start )
    {
      while ( coordinate > start )
      {
        start += getIndexPixels( position++ );
        m_startPixelCache.add( start );
      }
      // coordinate exactly at boundary returns next index, else previous
      return coordinate == start ? position : position - 1;
    }

    // binary search cached range for coordinate
    return m_startPixelCache.getIndex( coordinate );
  }

  /************************************* truncatePixelCaches *************************************/
  /**
   * Invalidates pixel caches from the specified index onwards, optionally adjusting total pixels.
   * 
   * @param fromViewIndex
   *          view index from which to invalidate caches
   * @param deltaPixels
   *          change in total pixels to apply (0 for full recalculation)
   */
  public void truncatePixelCaches( int fromViewIndex, int deltaPixels )
  {
    // efficiently update total if valid and delta provided
    if ( m_totalPixelsCache.get() != TableAxis.INVALID )
      m_totalPixelsCache.set( m_totalPixelsCache.get() + deltaPixels );

    // invalidate start pixel cache from affected index
    m_startPixelCache.truncate( fromViewIndex );
  }

  /************************************** getSizeExceptions **************************************/
  /**
   * Returns an unmodifiable map of all per-index size exceptions.
   * 
   * @return map of data index to nominal size (excludes default and hidden sizes)
   */
  public Map<Integer, Integer> getSizeExceptions()
  {
    // convert short values to integers for external use
    var exceptions = new HashMap<Integer, Integer>();
    for ( var entry : m_dataIndexSize.getSizeExceptions().entrySet() )
      exceptions.put( entry.getKey(), (int) entry.getValue() );

    return Collections.unmodifiableMap( exceptions );
  }

  /************************************* clearSizeExceptions *************************************/
  /**
   * Removes all per-index size exceptions, reverting all indices to default size (preserves
   * hidden state).
   */
  public void clearSizeExceptions()
  {
    // clear all size exceptions and invalidate all caches
    m_dataIndexSize.clearExceptions();
    m_startPixelCache.clear();
    m_totalPixelsCache.set( TableAxis.INVALID );
  }

  /*************************************** clearIndexSize ****************************************/
  /**
   * Clears the size exception for a specific index, reverting it to default size.
   * 
   * @param viewIndex
   *          view index to clear (must be valid body cell index)
   * @throws IndexOutOfBoundsException
   *           if viewIndex is outside valid range
   */
  public void clearIndexSize( int viewIndex )
  {
    // validate index is within valid body cell range
    if ( viewIndex < TableAxis.FIRSTCELL || viewIndex >= m_countProperty.get() )
      throw new IndexOutOfBoundsException( "cell index=" + viewIndex + " but count=" + m_countProperty.get() );

    // clear size exception and invalidate caches if changed
    int dataIndex = m_indexMapping.getDataIndex( viewIndex );
    if ( m_dataIndexSize.clearIndexSize( dataIndex ) )
    {
      m_totalPixelsCache.set( TableAxis.INVALID );
      m_startPixelCache.clear();
    }
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
    // remove listener from previous property
    if ( m_zoomProperty != null )
      m_zoomProperty.removeListener( this );

    // add listener to new property if not null
    if ( zoomProperty != null )
      zoomProperty.addListener( this );

    // update property reference and invalidate all pixel caches
    m_zoomProperty = zoomProperty;
    m_startPixelCache.clear();
    m_totalPixelsCache.set( TableAxis.INVALID );
  }

  /******************************************** zoom *********************************************/
  /**
   * Applies current zoom factor to a nominal size value.
   * 
   * @param size
   *          nominal size in pixels
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
    // check validity and visibility via data index
    if ( viewIndex < TableAxis.FIRSTCELL || viewIndex >= m_countProperty.get() )
      return false;

    int dataIndex = m_indexMapping.getDataIndex( viewIndex );
    return m_dataIndexSize.getSize( dataIndex ) > 0;
  }

  /*************************************** hideDataIndexes ***************************************/
  /**
   * Hides the specified data indices, making them invisible in the view.
   * 
   * @param dataIndexes
   *          set of data indices to hide
   * @return set of indices that were actually hidden (null if none changed)
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
    m_totalPixelsCache.set( TableAxis.INVALID );
    return hidden;
  }

  /************************************** unhideDataIndexes **************************************/
  /**
   * Unhides the specified data indices, making them visible in the view.
   * 
   * @param dataIndexes
   *          set of data indices to unhide
   * @return set of indices that were actually unhidden (null if none changed)
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
    m_totalPixelsCache.set( TableAxis.INVALID );
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
    m_totalPixelsCache.set( TableAxis.INVALID );
    return shown;
  }

  /************************************ getHiddenDataIndexes *************************************/
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

}