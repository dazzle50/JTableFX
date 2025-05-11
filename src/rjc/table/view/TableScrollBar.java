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

package rjc.table.view;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import rjc.table.Utils;
import rjc.table.view.axis.TableAxis;
import rjc.table.view.cursor.Cursors;

/*************************************************************************************************/
/*************** Extended version of ScrollBar with special increment & decrement ****************/
/*************************************************************************************************/

public class TableScrollBar extends ScrollBar
{
  private TableAxis       m_axis;                   // associated table axis
  private Timeline        m_timeline;               // used for animated table scrolling
  private int             m_scrollingTo;            // destination scroll-bar value for current animation
  private long            m_lastScrollNanos;        // last time scroll bar value changed
  private Animation       m_animation;              // currently active animation

  final public static int SIZE               = 18;  // pixels
  final public static int SCROLL_TO_DURATION = 100; // milliseconds
  final public static int INVALID            = -2;  // no active animation destination

  // types of scroll bar animations
  public static enum Animation
  {
    NONE, TO_POSITION, TO_START, TO_END
  }

  /**************************************** constructor ******************************************/
  public TableScrollBar( TableAxis axis, Orientation orientation )
  {
    // create scroll bar
    m_axis = axis;
    setOrientation( orientation );

    // set width/height
    if ( orientation == Orientation.VERTICAL )
    {
      setMinWidth( SIZE );
      setMinHeight( USE_PREF_SIZE );
      setMaxHeight( USE_PREF_SIZE );
    }
    else
    {
      setMinWidth( USE_PREF_SIZE );
      setMaxWidth( USE_PREF_SIZE );
      setMinHeight( SIZE );
    }

    // record last time scroll bar value changed to support smoother animated scrolling
    valueProperty().addListener( ( property, oldV, newV ) -> m_lastScrollNanos = System.nanoTime() );

    // change cursor to default when mouse enters
    addEventFilter( MouseEvent.MOUSE_ENTERED, event -> setCursor( Cursors.DEFAULT ) );
  }

  /************************************** scrollToShowIndex **************************************/
  public void scrollToShowIndex( int index )
  {
    // if scroll bar not visible, no need to scroll
    if ( !isVisible() )
      return;

    // check if need to scroll towards start to show cell start
    int start = m_axis.getStartPixel( index, (int) getValue() ) - m_axis.getHeaderPixels();
    if ( start < 0 )
    {
      scrollToValue( (int) getValue() + start, SCROLL_TO_DURATION );
      return;
    }

    // check if need to scroll towards end to show cell end, without hiding start
    int size = getOrientation() == Orientation.VERTICAL ? (int) getHeight() : (int) getWidth();
    int end = size - m_axis.getStartPixel( index + 1, (int) getValue() );
    if ( -end > start )
      end = -start;
    if ( end < 0 )
      scrollToValue( (int) getValue() - end, SCROLL_TO_DURATION );
  }

  /**************************************** scrollToValue ****************************************/
  public void scrollToValue( int newValue, int duration_ms )
  {
    // ensure new value is valid
    newValue = Utils.clamp( newValue, (int) getMin(), (int) getMax() );

    // if already scrolling to specified new-value, no need to start new animation
    if ( newValue == m_scrollingTo )
      return;

    // stop any current animation first
    if ( m_timeline != null )
      m_timeline.pause();

    // create scrolling animation
    m_scrollingTo = newValue;
    KeyValue kv = new KeyValue( valueProperty(), newValue );
    KeyFrame kf = new KeyFrame( Duration.millis( duration_ms ), kv );
    m_timeline = new Timeline( kf );
    m_timeline.setOnFinished( event ->
    {
      m_timeline = null;
      m_scrollingTo = INVALID;
      m_animation = Animation.NONE;
    } );
    m_timeline.play();
    m_animation = Animation.TO_POSITION;
  }

  /************************************* laterScrollToValue **************************************/
  public void laterScrollToValue( int newValue )
  {
    // finish any existing animation and scroll to new value (later)
    Platform.runLater( () ->
    {
      finishAnimation();
      scrollToValue( newValue, TableScrollBar.SCROLL_TO_DURATION );
    } );
  }

  /***************************************** scrollToEnd *****************************************/
  public void scrollToEnd( double speed )
  {
    // animate scroll to end of axis if not already there
    if ( isVisible() && getValue() < getMax() )
    {
      // if last scroll value change less than SCROLL_TO_DURATION ago, update value to make animation smoother
      double pixelsPerSec = speed * Math.sqrt( speed );
      long elapsedNanos = System.nanoTime() - m_lastScrollNanos;
      if ( elapsedNanos < SCROLL_TO_DURATION * 1e6 )
        setValue( Math.min( getValue() + elapsedNanos * pixelsPerSec / 1e9, getMax() ) );

      // setup new animation
      double ms = ( getMax() - getValue() ) * 1e3 / pixelsPerSec;
      m_scrollingTo = INVALID;
      scrollToValue( (int) getMax(), (int) ms );
      m_animation = Animation.TO_END;
    }
  }

  /**************************************** scrollToStart ****************************************/
  public void scrollToStart( double speed )
  {
    // animate scroll to beginning of axis if not already there
    if ( isVisible() && getValue() > getMin() )
    {
      // if last scroll value change less than SCROLL_TO_DURATION ago, update value to make animation smoother
      double pixelsPerSec = speed * Math.sqrt( speed );
      long elapsedNanos = System.nanoTime() - m_lastScrollNanos;
      if ( elapsedNanos < SCROLL_TO_DURATION * 1e6 )
        setValue( Math.max( getValue() - elapsedNanos * pixelsPerSec / 1e9, getMin() ) );

      // setup new animation
      double ms = getValue() * 1e3 / pixelsPerSec;
      m_scrollingTo = INVALID;
      scrollToValue( 0, (int) ms );
      m_animation = Animation.TO_START;
    }
  }

  /************************************* isAnimationStartEnd *************************************/
  public boolean isAnimationStartEnd()
  {
    // return true is scrolling to start or end edge
    return m_animation == Animation.TO_START || m_animation == Animation.TO_END;
  }

  /************************************ stopAnimationStartEnd ************************************/
  public void stopAnimationStartEnd()
  {
    // stop any scrolling to edges
    if ( m_animation == Animation.TO_START || m_animation == Animation.TO_END )
      stopAnimation();
  }

  /*************************************** finishAnimation ***************************************/
  public void finishAnimation()
  {
    // finish animation by jumping to end
    if ( m_timeline != null )
    {
      m_timeline.jumpTo( "end" );
      setValue( m_scrollingTo );
    }
  }

  /**************************************** stopAnimation ****************************************/
  public void stopAnimation()
  {
    // stop animation where it is
    if ( m_timeline != null )
    {
      m_timeline.pause();
      m_timeline = null;
      m_scrollingTo = INVALID;
      m_animation = Animation.NONE;
    }
  }

  /**************************************** getAnimation *****************************************/
  public Animation getAnimation()
  {
    // return current in progress animation
    return m_animation;
  }

  /****************************************** increment ******************************************/
  @Override
  public void increment()
  {
    // increase scroll bar value to next table cell boundary
    int headerSize = m_axis.getHeaderPixels();
    int index = m_axis.getIndexFromCoordinate( headerSize, (int) getValue() );
    int nextIndex = m_axis.getNextVisible( index );
    int start = m_axis.getStartPixel( nextIndex, 0 ) - headerSize;

    scrollToValue( start, SCROLL_TO_DURATION );
  }

  /****************************************** decrement ******************************************/
  @Override
  public void decrement()
  {
    // decrease scroll bar value to next table cell boundary
    int headerSize = m_axis.getHeaderPixels();
    int index = m_axis.getIndexFromCoordinate( headerSize, (int) getValue() );
    int start = m_axis.getStartPixel( index, 0 ) - headerSize;

    if ( start < getValue() )
      scrollToValue( start, SCROLL_TO_DURATION );
    else
    {
      int previousIndex = m_axis.getPreviousVisible( index );
      start = m_axis.getStartPixel( previousIndex, 0 ) - headerSize;
      scrollToValue( start, SCROLL_TO_DURATION );
    }
  }
}