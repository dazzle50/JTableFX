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

package rjc.table.control;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import rjc.table.Utils;
import rjc.table.data.types.Date;
import rjc.table.signal.ISignal;
import rjc.table.view.Colours;

/*************************************************************************************************/
/************************* Interactive widget showing one month calendar *************************/
/*************************************************************************************************/

/**
 * Canvas-based calendar for selecting a date from a six-week, Monday-first grid.
 * <p>
 * The header uses compact names from the format locale in effect when the widget is constructed.
 * Mouse, keyboard, and wheel input update the selected date and emit an {@link ISignal} containing
 * the corresponding {@link Date}. Shift-wheel changes months and Control-wheel changes years.
 * <p>
 * Subclasses can customise cell and text colours through {@link #getCellPaint(int, int, LocalDate)}
 * and {@link #getTextPaint(int, int, LocalDate)}.
 */
public class CalendarWidget extends Canvas implements ISignal, IOverflowField
{
  private LocalDate        m_localdate;       // currently selected date
  private GraphicsContext  m_gc;              // graphics context

  private final String[]   m_days;            // day names for first row

  private static final int COLUMN_WIDTH  = 28;
  private static final int ROW_HEIGHT    = 18;
  private static final int DAYS_IN_WEEK  = 7;
  private static final int CALENDAR_ROWS = 7;

  /**************************************** constructor ******************************************/
  /**
   * Creates a calendar initially selecting today.
   */
  public CalendarWidget()
  {
    // use a fixed grid so popup layout is stable before the calendar is shown
    setWidth( COLUMN_WIDTH * DAYS_IN_WEEK );
    setHeight( ROW_HEIGHT * CALENDAR_ROWS );
    setFocusTraversable( true );

    // prepare compact localised day names in the calendar's Monday-to-Sunday order
    m_days = new String[DAYS_IN_WEEK];
    Locale locale = Locale.getDefault( Locale.Category.FORMAT );
    for ( int dayOfWeek = 0; dayOfWeek < DAYS_IN_WEEK; dayOfWeek++ )
      m_days[dayOfWeek] = DayOfWeek.of( dayOfWeek + 1 ).getDisplayName( TextStyle.NARROW_STANDALONE, locale );

    // handler for mouse scroll events to change selected date
    final EventHandler<ScrollEvent> scrollHandler = event ->
    {
      event.consume();
      double step = event.getDeltaY() > 0 ? 1 : -1;
      changeValue( step, event.isShiftDown(), event.isControlDown(), event.isAltDown() );
    };

    // when has focus add drop-shadow and handle scroll events
    focusedProperty().addListener( ( property, oldFocus, newFocus ) ->
    {
      setStyle( newFocus ? "-fx-effect: dropshadow(gaussian, #039ed3, 4, 0.75, 0, 0);" : "" );
      getScene().getRoot().setOnScroll( newFocus ? scrollHandler : null );
    } );

    // route mouse and keyboard changes through setDate for consistent repainting and signals
    setOnMouseReleased( event -> mouseReleased( event ) );
    setOnKeyPressed( event -> keyPressed( event ) );

    // initialise the graphics context before selecting today triggers the first paint
    m_gc = getGraphicsContext2D();
    m_gc.setFontSmoothingType( FontSmoothingType.LCD );
    setDate( LocalDate.now() );
  }

  /***************************************** keyPressed ******************************************/
  private void keyPressed( KeyEvent event )
  {
    // map navigation keys to conventional calendar movements
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
        setDate( m_localdate.plusMonths( 1 ) );
        break;

      case PAGE_UP: // page up key
        setDate( m_localdate.plusMonths( -1 ) );
        break;

      case HOME: // home key - first day of month
        changeValue( 1 - m_localdate.getDayOfMonth() );
        break;

      case END: // end key - last day of month
        changeValue( m_localdate.lengthOfMonth() - m_localdate.getDayOfMonth() );
        break;

      case TAB: // don't consume tab to enable focus traversal
        return;

      case ESCAPE: // don't consume escape to enable cancel
        return;

      case ENTER: // don't consume enter to enable commit
        return;

      default:
    }

    // consume event
    event.consume();
  }

  /***************************************** changeValue *****************************************/
  /**
   * Moves the selected date using the supplied wheel-style modifiers.
   * <p>
   * With no Shift or Control modifier, {@code delta} represents days. Shift selects months and
   * Control selects years. Holding Shift and Control together makes no change. Alt is ignored when
   * selecting the movement unit.
   *
   * @param delta amount to move, truncated to an integer
   * @param shift whether to move by months
   * @param ctrl  whether to move by years
   * @param alt   unused modifier flag
   */
  @Override
  public void changeValue( double delta, boolean shift, boolean ctrl, boolean alt )
  {
    // modifiers promote the movement unit from days to months or years
    if ( !shift && !ctrl )
      setDate( getDate().plusDays( (int) delta ) );
    if ( shift && !ctrl )
      setDate( getDate().plusMonths( (int) delta ) );
    if ( !shift && ctrl )
      setDate( getDate().plusYears( (int) delta ) );
  }

  /**************************************** mouseReleased ****************************************/
  private void mouseReleased( MouseEvent event )
  {
    // request focus and translate the pointer position into an offset from the first grid date
    event.consume();
    requestFocus();
    int col = (int) ( event.getX() / COLUMN_WIDTH );
    int row = (int) ( event.getY() / ROW_HEIGHT ) - 1;

    setDate( getFirstDate().plusDays( col + row * DAYS_IN_WEEK ) );
  }

  /******************************************* setDate *******************************************/
  private void setDate( LocalDate localdate )
  {
    // if new date, re-paint widget and emit signal
    if ( m_localdate == null || !m_localdate.isEqual( localdate ) )
    {
      m_localdate = localdate;
      paint();
      signal( Date.of( localdate ) );
    }
  }

  /******************************************* setDate *******************************************/
  /**
   * Selects the supplied date, repainting and signalling only when it differs from the current date.
   *
   * @param date date to select
   * @throws NullPointerException if {@code date} is null
   */
  public void setDate( Date date )
  {
    // convert at the public API boundary; the private overload owns repainting and signalling
    setDate( date.toLocalDate() );
  }

  /******************************************* getDate *******************************************/
  /**
   * Returns the currently selected date.
   *
   * @return selected date
   */
  public Date getDate()
  {
    // expose the project Date type rather than the LocalDate used for painting
    return Date.of( m_localdate );
  }

  /**************************************** getFirstDate *****************************************/
  /**
   * Returns the date shown in the top-left grid cell.
   * <p>
   * The grid begins on the Monday before the first day of the selected month. If the month itself
   * starts on Monday, the preceding Monday is used so the previous month remains visible.
   *
   * @return first displayed date
   */
  public LocalDate getFirstDate()
  {
    // always include a preceding Monday, even when the month begins on Monday
    LocalDate first = m_localdate.withDayOfMonth( 1 );
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
      Bounds bounds = ( new Text( m_days[col] ) ).getLayoutBounds();
      x += ( COLUMN_WIDTH - bounds.getWidth() ) / 2.0;
      y += ( ROW_HEIGHT - bounds.getHeight() ) / 2.0 - bounds.getMinY();
      m_gc.fillText( m_days[col], x, y );
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
  /**
   * Returns the background paint for a date cell.
   * <p>
   * The default highlights the selected date and shades weekend columns.
   *
   * @param col       zero-based calendar column
   * @param row       one-based calendar row below the header
   * @param localdate date represented by the cell
   * @return cell background paint
   */
  protected Paint getCellPaint( int col, int row, LocalDate localdate )
  {
    // highlight currently selected date
    if ( localdate.isEqual( m_localdate ) )
      return Color.DEEPSKYBLUE;

    // shade weekend days
    if ( col > 4 )
      return Color.ALICEBLUE;
    return Color.WHITE;
  }

  /***************************************** getTextPaint ****************************************/
  /**
   * Returns the text paint for a date cell.
   * <p>
   * The default highlights today and dims dates outside the selected month.
   *
   * @param col       zero-based calendar column
   * @param row       one-based calendar row below the header
   * @param localdate date represented by the cell
   * @return cell text paint
   */
  protected Paint getTextPaint( int col, int row, LocalDate localdate )
  {
    // highlight today
    if ( localdate.isEqual( LocalDate.now() ) )
      return Color.RED;

    if ( localdate.getMonth() != m_localdate.getMonth() )
      return Color.GREY;

    return Colours.TEXT_DEFAULT;
  }

  /****************************************** toString *******************************************/
  /**
   * Returns a compact debug representation containing the selected date.
   */
  @Override
  public String toString()
  {
    // include the current selection in diagnostic output
    return Utils.name( this ) + "[" + getDate() + "]";
  }
}
