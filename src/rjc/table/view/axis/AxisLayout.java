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

public class AxisLayout implements IListener
{
  private final ReadOnlyInteger   m_countProperty;

  private int                     m_defaultSize;
  private int                     m_minimumSize;
  private int                     m_headerSize;
  private ReadOnlyDouble          m_zoomProperty;

  private final IndexSize         m_indexSize        = new IndexSize();
  private final PixelCache        m_startPixelCache  = new PixelCache();
  private final ObservableInteger m_totalPixelsCache = new ObservableInteger( TableAxis.INVALID );

  /*************************************** constructor *******************************************/
  public AxisLayout( ReadOnlyInteger countProperty )
  {
    // store count property and listen for changes
    m_countProperty = countProperty;
    m_countProperty.addListener( this );
    reset();
  }

  /******************************************* reset *********************************************/
  /**
   * Resets all sizing configuration to defaults.
   */
  public void reset()
  {
    // set default sizing values
    m_defaultSize = 100;
    m_minimumSize = 20;
    m_headerSize = 50;

    // clear internal storage and caches
    m_indexSize.clear();
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

      // truncate exceptions if count reduced
      if ( newCount < oldCount )
        m_indexSize.truncateSize( newCount );

      // invalidate total pixels and start pixel cache
      m_totalPixelsCache.set( TableAxis.INVALID );
      m_startPixelCache.truncate( newCount );
    }
    // handle zoom property change
    else if ( sender == m_zoomProperty )
    {
      m_startPixelCache.clear();
      m_totalPixelsCache.set( TableAxis.INVALID );
    }
  }

  /************************************** getHeaderPixels ****************************************/
  public int getHeaderPixels()
  {
    // return zoomed header size
    return zoom( m_headerSize );
  }

  /*************************************** getDefaultSize ****************************************/
  public int getDefaultSize()
  {
    // return nominal default size
    return m_defaultSize;
  }

  /*************************************** setDefaultSize ****************************************/
  public void setDefaultSize( int defaultSize )
  {
    // enforce minimum size
    if ( defaultSize < m_minimumSize )
      defaultSize = m_minimumSize;

    // update if changed
    if ( m_defaultSize != defaultSize )
    {
      if ( defaultSize < 1 )
        throw new IllegalArgumentException( "Default size must be at least one " + defaultSize );

      m_totalPixelsCache.set( TableAxis.INVALID );
      m_startPixelCache.clear();
      m_defaultSize = defaultSize;
    }
  }

  /*************************************** setMinimumSize ****************************************/
  public void setMinimumSize( int minSize )
  {
    // validate input
    if ( minSize < 0 )
      throw new IllegalArgumentException( "Minimum size must be at least zero " + minSize );

    // update if changed
    if ( m_minimumSize != minSize )
    {
      if ( m_defaultSize < minSize )
        setDefaultSize( minSize );

      // enforce new minimum on existing exceptions
      if ( minSize > m_minimumSize )
        m_indexSize.enforceMinimumSize( (short) minSize );

      m_totalPixelsCache.set( TableAxis.INVALID );
      m_startPixelCache.clear();
      m_minimumSize = minSize;
    }
  }

  /**************************************** setHeaderSize ****************************************/
  public void setHeaderSize( int headerSize )
  {
    // validate input
    if ( headerSize < 0 || headerSize >= 65536 )
      throw new IllegalArgumentException( "Header size must be at least zero " + headerSize );

    // update if changed
    if ( m_headerSize != headerSize )
    {
      // efficiently update total pixels if valid
      if ( getTotalPixels() != TableAxis.INVALID )
        m_totalPixelsCache.set( getTotalPixels() - getHeaderPixels() + zoom( headerSize ) );

      m_startPixelCache.clear();
      m_headerSize = headerSize;
    }
  }

  /**************************************** setIndexSize *****************************************/
  public void setIndexSize( int viewIndex, int newSize )
  {
    // validate index
    if ( viewIndex < TableAxis.FIRSTCELL || viewIndex >= m_countProperty.get() )
      throw new IndexOutOfBoundsException( "Index=" + viewIndex + " but count=" + m_countProperty.get() );

    // enforce minimum
    if ( newSize < m_minimumSize )
      newSize = m_minimumSize;

    // calculate old zoom size for delta check
    short oldNominal = m_indexSize.getSize( viewIndex );
    int zoomOld = 0;
    if ( oldNominal > 0 )
      zoomOld = oldNominal == IndexSize.DEFAULT ? zoom( m_defaultSize ) : zoom( oldNominal );

    // set new size
    m_indexSize.setSize( viewIndex, (short) newSize );

    // update caches if pixel size changed
    int zoomNew = zoom( newSize );
    if ( zoomNew != zoomOld )
      truncatePixelCaches( viewIndex, zoomNew - zoomOld );
  }

  /*************************************** getTotalPixels ****************************************/
  public int getTotalPixels()
  {
    // recalculate if invalid
    if ( m_totalPixelsCache.get() == TableAxis.INVALID )
    {
      int pixels = getHeaderPixels() + m_indexSize.calculateTotalPixels( m_countProperty.get(), m_defaultSize,
          m_zoomProperty != null ? m_zoomProperty.get() : 1.0 );
      m_totalPixelsCache.set( pixels );
    }
    return m_totalPixelsCache.get();
  }

  /*********************************** getTotalPixelsProperty ************************************/
  public ReadOnlyInteger getTotalPixelsProperty()
  {
    // return read only property
    return m_totalPixelsCache.getReadOnly();
  }

  /*************************************** getIndexPixels ****************************************/
  public int getIndexPixels( int viewIndex )
  {
    // return header size
    if ( viewIndex == TableAxis.HEADER )
      return getHeaderPixels();

    // check hidden or visible size
    short nominalSize = m_indexSize.getSize( viewIndex );
    if ( nominalSize <= 0 )
      return 0;

    return nominalSize == IndexSize.DEFAULT ? zoom( m_defaultSize ) : zoom( nominalSize );
  }

  /**************************************** getStartPixel ****************************************/
  public int getStartPixel( int viewIndex, int scrollOffset )
  {
    // validate and clamp index
    if ( viewIndex < TableAxis.HEADER )
      throw new IndexOutOfBoundsException( "index=" + viewIndex + " but count=" + m_countProperty.get() );
    if ( viewIndex > m_countProperty.get() )
      viewIndex = m_countProperty.get();

    // header starts at 0
    if ( viewIndex == TableAxis.HEADER )
      return 0;

    // fill cache if needed
    if ( viewIndex >= m_startPixelCache.size() )
    {
      if ( m_startPixelCache.isEmpty() )
        m_startPixelCache.add( getHeaderPixels() );

      int position = m_startPixelCache.size() - 1;
      int start = m_startPixelCache.get( position );
      while ( viewIndex > position )
      {
        start += getIndexPixels( position++ );
        m_startPixelCache.add( start );
      }
    }

    return m_startPixelCache.get( viewIndex ) - scrollOffset;
  }

  /*********************************** getIndexFromCoordinate ************************************/
  public int getIndexFromCoordinate( int coordinate, int scrollOffset )
  {
    // check bounds before header
    if ( coordinate < 0 )
      return TableAxis.BEFORE;

    // check bounds within header
    if ( coordinate < getHeaderPixels() )
      return TableAxis.HEADER;

    coordinate += scrollOffset;

    // check bounds after table
    if ( coordinate >= getTotalPixels() )
      return TableAxis.AFTER;

    // extend cache if needed
    int position = m_startPixelCache.size() - 1;
    int start = position < 0 ? 0 : m_startPixelCache.get( position );

    if ( coordinate > start )
    {
      while ( coordinate > start )
      {
        start += getIndexPixels( position++ );
        m_startPixelCache.add( start );
      }
      return coordinate == start ? position : position - 1;
    }

    return m_startPixelCache.getIndex( coordinate );
  }

  /************************************* truncatePixelCaches *************************************/
  public void truncatePixelCaches( int newCacheSize, int deltaPixels )
  {
    // update total pixels if valid
    if ( m_totalPixelsCache.get() != TableAxis.INVALID )
      m_totalPixelsCache.set( m_totalPixelsCache.get() + deltaPixels );

    // truncate start pixel cache
    m_startPixelCache.truncate( newCacheSize );
  }

  /************************************** getSizeExceptions **************************************/
  public Map<Integer, Integer> getSizeExceptions()
  {
    // create map of exceptions
    var exceptions = new HashMap<Integer, Integer>();
    for ( var entry : m_indexSize.getSizeExceptions().entrySet() )
      exceptions.put( entry.getKey(), (int) entry.getValue() );

    return Collections.unmodifiableMap( exceptions );
  }

  /************************************* clearSizeExceptions *************************************/
  public void clearSizeExceptions()
  {
    // clear exceptions and invalidate caches
    m_indexSize.clearExceptions();
    m_startPixelCache.clear();
    m_totalPixelsCache.set( TableAxis.INVALID );
  }

  /*************************************** clearIndexSize ****************************************/
  public void clearIndexSize( int viewIndex )
  {
    // validate index
    if ( viewIndex < TableAxis.FIRSTCELL || viewIndex >= m_countProperty.get() )
      throw new IndexOutOfBoundsException( "cell index=" + viewIndex + " but count=" + m_countProperty.get() );

    // clear specific size and invalidate if changed
    if ( m_indexSize.clearSize( viewIndex ) )
    {
      m_totalPixelsCache.set( TableAxis.INVALID );
      m_startPixelCache.clear();
    }
  }

  /************************************** reorderExceptions **************************************/
  public void reorderExceptions( int[] movedSorted, int insertIndex )
  {
    // forward reorder to index size storage
    m_indexSize.reorderIndexes( movedSorted, insertIndex );
  }

  /*************************************** setZoomProperty ***************************************/
  public void setZoomProperty( ReadOnlyDouble zoomProperty )
  {
    // swap listeners
    if ( m_zoomProperty != null )
      m_zoomProperty.removeListener( this );

    if ( zoomProperty != null )
      zoomProperty.addListener( this );

    // update property and invalidate caches
    m_zoomProperty = zoomProperty;
    m_startPixelCache.clear();
    m_totalPixelsCache.set( TableAxis.INVALID );
  }

  /******************************************** zoom *********************************************/
  private int zoom( int size )
  {
    // apply zoom factor if present
    if ( m_zoomProperty == null )
      return size;

    return (int) ( size * m_zoomProperty.get() );
  }

  /*************************************** isIndexVisible ****************************************/
  public boolean isIndexVisible( int viewIndex )
  {
    // check index validity and visibility
    return viewIndex >= TableAxis.FIRSTCELL && viewIndex < m_countProperty.get()
        && m_indexSize.getSize( viewIndex ) > 0;
  }

  /***************************************** hideIndexes *****************************************/
  public HashSetInt hideIndexes( HashSetInt viewIndexes )
  {
    // hide indexes and track changes
    var hidden = new HashSetInt();
    for ( int index : viewIndexes.toArray() )
      if ( m_indexSize.hide( index ) )
        hidden.add( index );

    if ( !hidden.isEmpty() )
    {
      m_startPixelCache.clear();
      m_totalPixelsCache.set( TableAxis.INVALID );
      return hidden;
    }
    return null;
  }

  /**************************************** unhideIndexes ****************************************/
  public HashSetInt unhideIndexes( HashSetInt viewIndexes )
  {
    // unhide indexes and track changes
    var shown = new HashSetInt();
    for ( int index : viewIndexes.toArray() )
      if ( m_indexSize.unhide( index ) )
        shown.add( index );

    if ( !shown.isEmpty() )
    {
      m_startPixelCache.clear();
      m_totalPixelsCache.set( TableAxis.INVALID );
      return shown;
    }
    return null;
  }

  /****************************************** unhideAll ******************************************/
  public HashSetInt unhideAll()
  {
    // unhide all and invalidate if changed
    var shown = m_indexSize.unhideAll();

    if ( shown != null )
    {
      m_startPixelCache.clear();
      m_totalPixelsCache.set( TableAxis.INVALID );
    }

    return shown;
  }

  /************************************** getHiddenIndexes ***************************************/
  public HashSetInt getHiddenIndexes()
  {
    // return set of hidden indexes
    return m_indexSize.getHiddenIndexes();
  }
}