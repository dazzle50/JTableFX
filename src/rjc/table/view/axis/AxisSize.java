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

import rjc.table.Utils;
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
  private int               m_defaultSize;
  private int               m_minimumSize;
  private int               m_headerSize;
  private ReadOnlyDouble    m_zoomProperty;

  private SizeExceptions    m_sizeExceptions         = new SizeExceptions();
  private PixelCache        m_startPixelCache        = new PixelCache();
  private IndexMapping      m_dataIndexFromViewIndex = new IndexMapping();

  // observable integer for cached axis size in pixels (includes header)
  private ObservableInteger m_totalPixelsCache       = new ObservableInteger( INVALID );

  /**************************************** constructor ******************************************/
  public AxisSize( ReadOnlyInteger countProperty )
  {
    // pass count property to super class
    super( countProperty );
  }

  /******************************************** reset ********************************************/
  public void reset()
  {
    // clear all axis position to index re-ordering
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
    Utils.trace( sender, msg );
  }

  /************************************** getMinimumPixels ***************************************/
  public int getMinimumPixels()
  {
    // return minimum cell size in pixels
    return zoom( m_minimumSize );
  }

  /*************************************** getHeaderPixels ***************************************/
  public int getHeaderPixels()
  {
    // return header cell size in pixels
    return zoom( m_headerSize );
  }

  /**************************************** getIndexPixels ***************************************/
  public double getIndexPixels( int viewIndex )
  {
    // TODO Auto-generated method stub
    return 0;
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
        m_sizeExceptions.setMinimumSize( minSize );

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

  /*********************************** getTotalPixelsProperty ************************************/
  public ReadOnlyInteger getTotalPixelsProperty()
  {
    // return return read-only version of axis total pixels size
    return m_totalPixelsCache.getReadOnly();
  }

  /*************************************** getTotalPixels ****************************************/
  public int getTotalPixels()
  {
    // return axis total size in pixels (including header)
    if ( m_totalPixelsCache.get() == INVALID )
    {
      // cached size is invalid, so re-calculate
      Utils.trace( "TODO - cached size is invalid, so re-calculate" );
    }

    return m_totalPixelsCache.get();
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

  /****************************************** isHidden *******************************************/
  public boolean isHidden( int index )
  {
    // overload this function if row/column hiding is wanted
    return false;
  }

  /*************************************** isIndexVisible ****************************************/
  public boolean isIndexVisible( int index )
  {
    // return true if cell is visible body cell
    return index >= FIRSTCELL && index < getCount() && !isHidden( index );
  }

}
