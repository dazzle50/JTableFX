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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import rjc.table.signal.IListener;
import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableDouble.ReadOnlyDouble;
import rjc.table.signal.ObservableInteger;
import rjc.table.signal.ObservableInteger.ReadOnlyInteger;

/*************************************************************************************************/
/****************** Table axis with header & body cell sizing including zooming ******************/
/*************************************************************************************************/

public class AxisSize extends AxisBase implements IListener
{
  // variables defining default & minimum cell size (width or height) equals pixels if zoom is 1.0
  private int                   m_defaultSize;
  private int                   m_minimumSize;
  private int                   m_headerSize;
  private ReadOnlyDouble        m_zoomProperty;

  // exceptions to default size (negative are hidden)
  private Map<Integer, Integer> m_sizeExceptions   = new HashMap<>();
  final static public int       HIDDEN_DEFAULT     = Integer.MIN_VALUE;

  // cached cell index to start pixel coordinate
  private ArrayList<Integer>    m_startPixelCache  = new ArrayList<>();

  // observable integer for cached axis size in pixels (includes header)
  private ObservableInteger     m_totalPixelsCache = new ObservableInteger( INVALID );

  /**************************************** constructor ******************************************/
  public AxisSize( ReadOnlyInteger countProperty )
  {
    // pass count property to super class
    super( countProperty );
  }

  /******************************************** reset ********************************************/
  public void reset()
  {
    // set default axis index sizes
    m_defaultSize = 100;
    m_minimumSize = 20;
    m_headerSize = 50;
    m_sizeExceptions.clear();
    m_startPixelCache.clear();
    m_totalPixelsCache.set( INVALID );
  }

  /******************************************** slot *********************************************/
  @Override
  public void slot( ISignal sender, Object... msg )
  {
    // listen to signals sent to axis
    if ( sender == getCountProperty() )
    {
      // set cached axis size to invalid
      m_totalPixelsCache.set( INVALID );

      // remove any exceptions beyond count
      int oldCount = (int) msg[0];
      int newCount = getCount();
      if ( newCount < oldCount )
        for ( int key : m_sizeExceptions.keySet() )
          if ( key >= newCount )
            m_sizeExceptions.remove( key );

      // truncate cell start cache if new size smaller
      if ( newCount < m_startPixelCache.size() )
        m_startPixelCache.subList( newCount, m_startPixelCache.size() ).clear();
    }

    else if ( sender == m_zoomProperty )
    {
      // zoom value has changed so clear the pixel caches
      m_startPixelCache.clear();
      m_totalPixelsCache.set( INVALID );
    }
  }

  /*************************************** getHeaderPixels ***************************************/
  public int getHeaderPixels()
  {
    // return header cell size in pixels
    return zoom( m_headerSize );
  }

  /*************************************** setDefaultSize ****************************************/
  public void setDefaultSize( int defaultSize )
  {
    // if requested new default size is smaller than minimum, increase new default to minimum
    if ( defaultSize < m_minimumSize )
      defaultSize = m_minimumSize;

    // if different, set default cell size and invalidate the caches
    if ( m_defaultSize != defaultSize )
    {
      // check requested new default size is at least one
      if ( defaultSize < 1 )
        throw new IllegalArgumentException( "Default size must be at least one " + defaultSize );

      m_totalPixelsCache.set( INVALID );
      m_startPixelCache.clear();
      m_defaultSize = defaultSize;
    }
  }

  /*************************************** setMinimumSize ****************************************/
  public void setMinimumSize( int minSize )
  {
    // check requested new minimum size is at least zero
    if ( minSize < 0 )
      throw new IllegalArgumentException( "Minimum size must be at least zero " + minSize );

    // if different, set minimum cell size and invalidate body size
    if ( m_minimumSize != minSize )
    {
      // if default smaller than minimum, increase default to size
      if ( m_defaultSize < minSize )
        setDefaultSize( minSize );

      // if minimum size increasing, check exceptions
      if ( minSize > m_minimumSize )
        for ( var exception : m_sizeExceptions.entrySet() )
        {
          int size = exception.getValue();
          if ( size < 0 && size > -minSize )
            exception.setValue( -minSize );
          else if ( size > 0 && size < minSize )
            exception.setValue( minSize );
        }

      m_totalPixelsCache.set( INVALID );
      m_startPixelCache.clear();
      m_minimumSize = minSize;
    }
  }

  /**************************************** setHeaderSize ****************************************/
  public void setHeaderSize( int headerSize )
  {
    // check new size is valid
    if ( headerSize < 0 || headerSize >= 65536 )
      throw new IllegalArgumentException( "Header size must be at least zero " + headerSize );

    // if new size is different, clear cell position start cache
    if ( m_headerSize != headerSize )
    {
      if ( getTotalPixels() != INVALID )
        m_totalPixelsCache.set( getTotalPixels() - getHeaderPixels() + zoom( headerSize ) );

      m_startPixelCache.clear();
      m_headerSize = headerSize;
    }
  }

  /**************************************** setIndexSize *****************************************/
  public void setIndexSize( int index, int newSize )
  {
    // check cell index is valid
    if ( index < FIRSTCELL || index >= getCount() )
      throw new IndexOutOfBoundsException( "Index=" + index + " but count=" + getCount() );

    // make sure size is not below minimum
    if ( newSize < m_minimumSize )
      newSize = m_minimumSize;

    // create a size exception (even if same as default)
    int oldSize = m_sizeExceptions.getOrDefault( index, m_defaultSize );
    m_sizeExceptions.put( index, newSize );

    // if new size is different, update body size and truncate cell position start cache if needed
    if ( newSize != oldSize )
      truncatePixelCaches( index, zoom( newSize ) - zoom( oldSize ) );
  }

  /*************************************** getTotalPixels ****************************************/
  public int getTotalPixels()
  {
    // return axis total size in pixels (including header)
    if ( m_totalPixelsCache.get() == INVALID )
    {
      // cached size is invalid, so re-calculate
      int defaultCount = getCount() - m_sizeExceptions.size();

      int pixels = getHeaderPixels();
      for ( int size : m_sizeExceptions.values() )
        if ( size > 0 /* not hidden */ )
          pixels += zoom( size );

      m_totalPixelsCache.set( pixels + defaultCount * zoom( m_defaultSize ) );
    }

    return m_totalPixelsCache.get();
  }

  /*********************************** getTotalPixelsProperty ************************************/
  public ReadOnlyInteger getTotalPixelsProperty()
  {
    // return return read-only version of axis total pixels size
    return m_totalPixelsCache.getReadOnly();
  }

  /*************************************** getIndexPixels ****************************************/
  public int getIndexPixels( int index )
  {
    // return header size if that was requested
    if ( index == HEADER )
      return zoom( m_headerSize );

    // return cell size from exception or default
    int size = m_sizeExceptions.getOrDefault( index, m_defaultSize );
    return size > 0 ? zoom( size ) : 0;
  }

  /**************************************** getStartPixel ****************************************/
  public int getStartPixel( int index, int scroll )
  {
    // check index is valid
    if ( index < HEADER )
      throw new IndexOutOfBoundsException( "index=" + index + " but count=" + getCount() );
    if ( index > getCount() )
      index = getCount();

    // if header, return zero
    if ( index == HEADER )
      return 0;

    // if cell index is beyond cache, extend cache
    if ( index >= m_startPixelCache.size() )
    {
      // index zero starts after header
      if ( m_startPixelCache.isEmpty() )
        m_startPixelCache.add( getHeaderPixels() );

      int position = m_startPixelCache.size() - 1;
      int start = m_startPixelCache.get( position );
      while ( index > position )
      {
        start += getIndexPixels( position++ );
        m_startPixelCache.add( start );
      }
    }

    // return start pixel coordinate for cell index taking scroll into account
    return m_startPixelCache.get( index ) - scroll;
  }

  /*********************************** getIndexFromCoordinate ************************************/
  public int getIndexFromCoordinate( int coordinate, int scroll )
  {
    // check if before table
    if ( coordinate < 0 )
      return BEFORE;

    // check if header
    if ( coordinate < getHeaderPixels() )
      return HEADER;

    // check if after table
    coordinate += scroll;
    if ( coordinate >= getTotalPixels() )
      return AFTER;

    // check within start cache
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

    // find position by binary search of cache
    int startPos = 0;
    int endPos = m_startPixelCache.size();
    while ( startPos != endPos )
    {
      int rowPos = ( endPos + startPos ) / 2;
      if ( m_startPixelCache.get( rowPos ) <= coordinate )
        startPos = rowPos + 1;
      else
        endPos = rowPos;
    }
    return startPos - 1;
  }

  /************************************* truncatePixelCaches *************************************/
  protected void truncatePixelCaches( int newCacheSize, int deltaPixels )
  {
    // update body size cache if not invalid
    if ( m_totalPixelsCache.get() != INVALID )
      m_totalPixelsCache.set( m_totalPixelsCache.get() + deltaPixels );

    // truncate cache length if greater than specified new size
    if ( m_startPixelCache.size() > newCacheSize )
      m_startPixelCache.subList( newCacheSize, m_startPixelCache.size() ).clear();
  }

  /************************************** reorderExceptions **************************************/
  protected void reorderExceptions( ArrayList<Integer> movedSorted, int insertIndex )
  {
    // update size exceptions taking into account moves
    var newSizeExceptions = new HashMap<Integer, Integer>();
    for ( int exceptionIndex : new TreeSet<Integer>( m_sizeExceptions.keySet() ) )
    {
      int moved = movedSorted.indexOf( exceptionIndex );
      if ( moved >= 0 )
        // moved index
        newSizeExceptions.put( insertIndex + moved, m_sizeExceptions.get( exceptionIndex ) );
      else
        // not-moved index
        newSizeExceptions.put( adjustedIndex( exceptionIndex, insertIndex, movedSorted ),
            m_sizeExceptions.get( exceptionIndex ) );
    }
    m_sizeExceptions = newSizeExceptions;
  }

  /**************************************** adjustedIndex ****************************************/
  private Integer adjustedIndex( int exceptionIndex, int insertIndex, ArrayList<Integer> movedSorted )
  {
    // count of moved before exception index
    int before = 0;
    while ( movedSorted.size() > before && movedSorted.get( before ) < exceptionIndex )
      before++;

    if ( exceptionIndex < insertIndex + before )
      return exceptionIndex - before;

    return exceptionIndex - before + movedSorted.size();
  }

  /*************************************** setZoomProperty ***************************************/
  public void setZoomProperty( ReadOnlyDouble zoomProperty )
  {
    // remove listening from old zoom property
    if ( m_zoomProperty != null )
      m_zoomProperty.removeListener( this );

    // add listening to new zoom property
    if ( zoomProperty != null )
      zoomProperty.addListener( this );

    // adopt new zoom
    m_zoomProperty = zoomProperty;
    m_startPixelCache.clear();
    m_totalPixelsCache.set( INVALID );
  }

  /******************************************** zoom *********************************************/
  private int zoom( int size )
  {
    // convenience method to return pixels from size
    if ( m_zoomProperty == null )
      return size;

    return (int) ( size * m_zoomProperty.get() );
  }

  /***************************************** isResizable *****************************************/
  public boolean isResizable( int index )
  {
    // overload this function if prevention of row/column resizing is wanted
    return index > HEADER;
  }

  /*************************************** isIndexVisible ****************************************/
  public boolean isIndexVisible( int index )
  {
    // return true if cell is visible body cell
    return index >= FIRSTCELL && index < getCount() && m_sizeExceptions.getOrDefault( index, m_defaultSize ) > 0;
  }

}