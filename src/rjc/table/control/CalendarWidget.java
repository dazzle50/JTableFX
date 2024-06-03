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

package rjc.table.control;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import rjc.table.data.types.Date;
import rjc.table.signal.ISignal;
import rjc.table.view.Colours;

/*************************************************************************************************/
/************************* Interactive widget showing one month calendar *************************/
/*************************************************************************************************/

public class CalendarWidget extends Canvas implements ISignal, IOverflowField
{
  private LocalDate        m_date;            // currently selected date
  private GraphicsContext  m_gc;              // graphics context

  private static String    days[];            // day names for first row

  private static final int COLUMN_WIDTH  = 28;
  private static final int ROW_HEIGHT    = 18;
  private static final int DAYS_IN_WEEK  = 7;
  private static final int CALENDAR_ROWS = 7;

  /**************************************** constructor ******************************************/
  public CalendarWidget()
  {
    // initialise
    setWidth( COLUMN_WIDTH * DAYS_IN_WEEK );
    setHeight( ROW_HEIGHT * CALENDAR_ROWS );
    setFocusTraversable( true );

    // prepare static day names (first two characters of day of week name)
    if ( days == null )
    {
      days = new String[DAYS_IN_WEEK];
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "E" );
      for ( int dayOfWeek = 0; dayOfWeek < DAYS_IN_WEEK; dayOfWeek++ )
      {
        // local-date day four is a Monday (first day of week)
        LocalDate date = LocalDate.ofEpochDay( dayOfWeek + 4 );
        days[dayOfWeek] = date.format( formatter ).substring( 0, 2 );
      }
    }

    // redraw when focus changes
    focusedProperty().addListener( ( observable, oldFocus, newFocus ) ->
    {
      if ( newFocus.booleanValue() )
        setStyle( "-fx-effect: dropshadow(gaussian, #039ed3, 4, 0.75, 0, 0);" );
      else
        setStyle( "" );
    } );

    // react to mouse + keyboard
    setOnMouseReleased( event -> mouseReleased( event ) );
    setOnKeyPressed( event -> keyPressed( event ) );

    // set default date to today
    m_gc = getGraphicsContext2D();
    m_gc.setFontSmoothingType( FontSmoothingType.LCD );
    setDate( LocalDate.now() );
  }

  /***************************************** keyPressed ******************************************/
  private void keyPressed( KeyEvent event )
  {
    // update date depending on key pressed
    switch ( event.getCode() )
    {
      case RIGHT: // right -> arrow key
      case KP_RIGHT:
        changeValue( 1 );
        break;

      case LEFT: // left <- arrow key
      case KP_LEFT:
        changeValue( -1 );
        break;

      case DOWN: // down arrow key
      case KP_DOWN:
        changeValue( DAYS_IN_WEEK );
        break;

      case UP: // up arrow key
      case KP_UP:
        changeValue( -DAYS_IN_WEEK );
        break;

      case PAGE_DOWN: // page down key
        setDate( m_date.plusMonths( 1 ) );
        break;

      case PAGE_UP: // page up key
        setDate( m_date.plusMonths( -1 ) );
        break;

      case HOME: // home key - first day of month
        changeValue( 1 - m_date.getDayOfMonth() );
        break;

      case END: // end key - last day of month
        changeValue( m_date.lengthOfMonth() - m_date.getDayOfMonth() );
        break;

      case TAB: // don't consume tab to enable focus traversal
        return;

      default:
    }

    // consume event
    event.consume();
  }

  /***************************************** changeValue *****************************************/
  @Override
  public void changeValue( double delta )
  {
    // adjust calendar date by delta days
    setDate( m_date.plusDays( (long) delta ) );
  }

  /**************************************** mouseReleased ****************************************/
  private void mouseReleased( MouseEvent event )
  {
    // request focus and set date to selected cell
    event.consume();
    requestFocus();
    int col = (int) ( event.getX() / COLUMN_WIDTH );
    int row = (int) ( event.getY() / ROW_HEIGHT ) - 1;

    setDate( getFirstDate().plusDays( col + row * DAYS_IN_WEEK ) );
  }

  /******************************************* setDate *******************************************/
  private void setDate( LocalDate date )
  {
    // if new date, re-paint widget and emit signal
    if ( m_date == null || !m_date.isEqual( date ) )
    {
      m_date = date;
      paint();
      signal( new Date( date ) );
    }
  }

  /******************************************* setDate *******************************************/
  public void setDate( Date date )
  {
    // set widget date (and emit signal if different)
    setDate( date.localDate() );
  }

  /******************************************* getDate *******************************************/
  public Date getDate()
  {
    // get widget date
    return new Date( m_date );
  }

  /**************************************** getFirstDate *****************************************/
  public LocalDate getFirstDate()
  {
    // get widget first date in top-right corner
    LocalDate first = m_date.withDayOfMonth( 1 );
    int offset = 1 - first.getDayOfWeek().getValue();
    return first.plusDays( offset == 0 ? -7 : offset );
  }

  /******************************************** paint ********************************************/
  private void paint()
  {
    // first row is day-of-week labels
    m_gc.setFill( Color.LIGHTYELLOW );
    m_gc.fillRect( 0, 0, COLUMN_WIDTH * DAYS_IN_WEEK, ROW_HEIGHT );
    m_gc.setFill( Color.BLACK );
    for ( int col = 0; col < 7; col++ )
    {
      int x = col * COLUMN_WIDTH;
      int y = 0;

      // draw label
      Bounds bounds = ( new Text( days[col] ) ).getLayoutBounds();
      x += ( COLUMN_WIDTH - bounds.getWidth() ) / 2.0;
      y += ( ROW_HEIGHT - bounds.getHeight() ) / 2.0 - bounds.getMinY();
      m_gc.fillText( days[col], x, y );
    }

    // following rows show day-of-month number
    LocalDate date = getFirstDate();
    for ( int row = 1; row < CALENDAR_ROWS; row++ )
      for ( int col = 0; col < DAYS_IN_WEEK; col++ )
      {
        int x = col * COLUMN_WIDTH;
        int y = row * ROW_HEIGHT;

        // draw background
        m_gc.setFill( getCellPaint( col, row, date ) );
        m_gc.fillRect( x, y, COLUMN_WIDTH, ROW_HEIGHT );

        // draw number
        m_gc.setFill( getTextPaint( col, row, date ) );
        String txt = String.valueOf( date.getDayOfMonth() );
        Bounds bounds = ( new Text( txt ) ).getLayoutBounds();
        x += ( COLUMN_WIDTH - bounds.getWidth() ) / 2.0;
        y += ( ROW_HEIGHT - bounds.getHeight() ) / 2.0 - bounds.getMinY();
        m_gc.fillText( txt, x, y );

        date = date.plusDays( 1 );
      }
  }

  /**************************************** getCellPaint *****************************************/
  protected Paint getCellPaint( int col, int row, LocalDate date )
  {
    // highlight currently selected date
    if ( date.isEqual( m_date ) )
      return Color.DEEPSKYBLUE;

    // shade weekend days
    if ( col > 4 )
      return Color.ALICEBLUE;
    return Color.WHITE;
  }

  /***************************************** getTextPaint ****************************************/
  protected Paint getTextPaint( int col, int row, LocalDate date )
  {
    // highlight today
    if ( date.isEqual( LocalDate.now() ) )
      return Color.RED;

    if ( date.getMonth() != m_date.getMonth() )
      return Color.GREY;

    return Colours.TEXT_DEFAULT;
  }
}