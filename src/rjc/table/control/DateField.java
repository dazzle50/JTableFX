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

import java.lang.ref.WeakReference;
import java.time.Month;
import java.time.YearMonth;

import javafx.scene.control.Button;
import rjc.table.control.dropdown.AbstractDropDownField;
import rjc.table.control.dropdown.DropDown;
import rjc.table.data.types.Date;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/******************************* Date field control with drop-down *******************************/
/*************************************************************************************************/

/**
 * Editable date field with a drop-down month, year, and calendar picker.
 * <p>
 * Parseable text updates the most recent valid {@link Date} and emits it through {@link #signal}.
 * Invalid text is styled as an error while editing and is replaced by the most recent valid date
 * on focus loss or Escape. A non-null status object must be assigned before that normalisation
 * because it clears the shared status. Arrow or wheel modifiers step days, months, or years.
 */
public class DateField extends AbstractDropDownField
{
  protected Date          m_date;       // field current date (or most recent valid)

  private MonthSpinField  m_monthField;
  private NumberSpinField m_yearField;
  private CalendarWidget  m_calendar;
  private Button          m_todayButton;

  /**************************************** constructor ******************************************/
  /**
   * Creates a date field initially set to today.
   */
  public DateField()
  {
    // create the date widgets for the drop-down
    m_monthField = new MonthSpinField();
    m_yearField = new NumberSpinField();
    m_calendar = new CalendarWidget();
    m_todayButton = new Button( "Today" );

    // layout the date widgets
    getDropDownGrid().addRow( 0, m_monthField, m_yearField );
    getDropDownGrid().add( m_calendar, 0, 1, 2, 1 );
    getDropDownGrid().add( m_todayButton, 0, 2, 2, 1 );

    // configure the date widgets
    int w = (int) ( m_calendar.getWidth() * 0.6 );
    m_monthField.setMaxWidth( w );
    m_monthField.setOverflowField( m_yearField );
    m_yearField.setMaxWidth( m_calendar.getWidth() - DropDown.GRID_BORDER - w );
    m_yearField.setRange( 0, 5000 );
    m_yearField.setFormat( "0", 6, 0 );
    m_todayButton.setPrefWidth( m_calendar.getWidth() );
    m_calendar.requestFocus();

    // listen to widget changes
    var weak = new WeakReference<DateField>( this );
    m_yearField.addListener( ( sender, year ) -> weak.get().setYear( ( (Double) year[0] ).intValue() ) );
    m_monthField.addListener( ( sender, month ) -> weak.get().setMonth( (Month) month[0] ) );
    m_calendar.addListener( ( sender, date ) -> weak.get().setDate( (Date) date[0] ) );
    m_todayButton.setOnAction( event -> weak.get().setDate( Date.now() ) );

    // set default date to today
    setDate( Date.now() );
  }

  /************************************ updateDropDownWidgets ************************************/
  /**
   * Synchronises the calendar, month, and year controls with the current valid date.
   */
  @Override
  protected void updateDropDownWidgets()
  {
    // programmatic widget signals settle back on the same date and do not recurse
    m_calendar.setDate( m_date );
    m_monthField.setValue( m_date.getMonth() );
    m_yearField.setValue( m_date.getYear() );
  }

  /**************************************** getCalendar ******************************************/
  /**
   * Returns the calendar used by the drop-down.
   *
   * @return calendar picker
   */
  protected CalendarWidget getCalendar()
  {
    // expose the calendar to DateTimeField so it can share date overflow
    return m_calendar;
  }

  /****************************************** getDate ********************************************/
  /**
   * Returns the current valid date, or the most recently parsed valid date while the text is invalid.
   * <p>
   * In {@link DateTimeField}, this inherited stored date can lag behind text parsing until the
   * drop-down widgets are synchronised. Use {@link DateTimeField#getDateTime()} and its
   * {@code getDate()} method for the current date component in that subclass.
   *
   * @return stored valid date used by the calendar controls
   */
  public Date getDate()
  {
    // invalid edit text deliberately leaves the last successfully parsed value intact
    return m_date;
  }

  /****************************************** setDate ********************************************/
  /**
   * Sets the field from a date or literal text.
   * <p>
   * A {@link Date} is formatted canonically. Other non-null values are converted with
   * {@link Object#toString()}, while null displays blank text. Text is parsed by the normal field
   * validation listener, so invalid text does not replace the most recent valid date.
   *
   * @param value date or text-like value to display
   */
  public void setDate( Object value )
  {
    // typed dates use canonical formatting; other values enter the normal text-validation path
    if ( value instanceof Date date )
      setDate( date );
    else
    {
      String txt = value == null ? "" : value.toString();
      setText( txt );
    }
  }

  /****************************************** setDate ********************************************/
  /**
   * Sets the current valid date, updates the drop-down, and signals when the value changes.
   *
   * @param date date to set
   * @throws NullPointerException if {@code date} is null
   */
  public void setDate( Date date )
  {
    // suppress redundant widget updates and external signals
    if ( !date.equals( m_date ) )
    {
      m_date = date;
      setText( format( date ) );
      updateDropDownWidgets();
      signal( date );
    }
  }

  /****************************************** setMonth *******************************************/
  private void setMonth( Month month )
  {
    // change calendar widget month - ensuring day is valid
    Date date = m_calendar.getDate();
    int year = date.getYear();
    int day = date.getDayOfMonth();
    YearMonth ym = YearMonth.of( year, month );
    if ( day > ym.lengthOfMonth() )
      day = ym.lengthOfMonth();

    m_calendar.setDate( Date.of( year, month.getValue(), day ) );
  }

  /******************************************* setYear *******************************************/
  private void setYear( int year )
  {
    // change calendar widget year - ensuring day is valid
    Date date = m_calendar.getDate();
    int month = date.getMonth();
    int day = date.getDayOfMonth();
    YearMonth ym = YearMonth.of( year, month );
    if ( day > ym.lengthOfMonth() )
      day = ym.lengthOfMonth();

    m_calendar.setDate( Date.of( year, month, day ) );
  }

  /******************************************* format ********************************************/
  private String format( Date date )
  {
    // return date in display format
    return date.toString();
  }

  /**************************************** formatStatus *****************************************/
  private String formatStatus( Date date )
  {
    // return date in status format
    return date.format( "eeee d MMMM yyyy" );
  }

  /***************************************** parseText *******************************************/
  /**
   * Parses cleaned field text and signals when it represents a new valid date.
   *
   * @param text cleaned text to parse
   * @throws java.time.format.DateTimeParseException if the text cannot be parsed
   * @throws ArithmeticException if relative arithmetic or compact storage overflows
   */
  @Override
  protected void parseText( String text )
  {
    // let AbstractDropDownField translate parse failures into error status
    Date date = Date.parse( text );
    if ( !date.equals( m_date ) )
    {
      m_date = date;
      signal( date );
    }
  }

  /***************************************** statusText ******************************************/
  /**
   * Returns the informational or validation-error message for the field.
   *
   * @param level requested status severity
   * @return status message
   */
  @Override
  protected String statusText( Level level )
  {
    // only informational status includes the last valid value
    return level == Level.INFO ? "Date: " + formatStatus( m_date ) : "Date format is not recognised";
  }

  /***************************************** validText *******************************************/
  /**
   * Restores canonical text for the most recent valid date and clears the shared status.
   *
   * @throws NullPointerException if no status object has been assigned
   */
  @Override
  protected void validText()
  {
    // ensure field displays last valid date
    setText( format( m_date ) );
    getStatus().clear();
  }

  /***************************************** changeValue *****************************************/
  /**
   * Steps the date by days, months, or years according to the supplied modifiers.
   * <p>
   * No modifier changes days, Shift changes months, and Control changes years. Alt is ignored.
   * A simultaneous Shift-Control combination makes no change.
   *
   * @param delta amount to move, truncated to an integer
   * @param shift whether to move by months
   * @param ctrl  whether to move by years
   * @param alt   unused modifier flag
   */
  @Override
  public void changeValue( double delta, boolean shift, boolean ctrl, boolean alt )
  {
    // Shift and Control promote the movement unit from days to months or years
    if ( !shift && !ctrl )
      setDate( getDate().plusDays( (int) delta ) );
    if ( shift && !ctrl )
      setDate( getDate().plusMonths( (int) delta ) );
    if ( !shift && ctrl )
      setDate( getDate().plusYears( (int) delta ) );
  }

}
