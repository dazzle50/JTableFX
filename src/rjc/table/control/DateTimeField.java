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

import javafx.application.Platform;
import rjc.table.data.types.Date;
import rjc.table.data.types.DateInterval;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;
import rjc.table.data.types.TimeInterval;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/**************************** Date-time field control with drop-down *****************************/
/*************************************************************************************************/

/**
 * Editable date-time field combining the {@link DateField} calendar picker with a
 * {@link TimeWidget}.
 * <p>
 * Parseable text updates the most recent valid {@link DateTime}. The drop-down keeps its calendar
 * and time controls synchronised with that value. Arrow or wheel modifiers step date components;
 * holding Alt instead steps time components. Invalid text is replaced by the most recent valid
 * value on focus loss or Escape; a non-null status object must be assigned before that
 * normalization because it clears the shared status.
 */
public class DateTimeField extends DateField
{
  private DateTime   m_datetime;   // field current date-time (or most recent valid)
  private TimeWidget m_timeWidget; // time-widget in drop-down

  /**************************************** constructor ******************************************/
  /**
   * Creates a date-time field initially set to the start of the current hour.
   */
  public DateTimeField()
  {
    // prepare time-widget and add to the date drop-down
    m_timeWidget = new TimeWidget( getCalendar() );
    getDropDownGrid().add( m_timeWidget, 0, 3, 2, 1 );

    // listen to changes to keep field & drop-down aligned
    var weak = new WeakReference<DateTimeField>( this );
    m_timeWidget.addListener( ( sender, time ) -> weak.get().setTime( (Time) time[0] ) );

    // add status later as not yet set
    Platform.runLater( () -> m_timeWidget.setStatus( getStatus() ) );

    // set default date-time to now truncated to hour
    setDateTime( DateTime.now().startOf( TimeInterval.HOUR ) );
  }

  /************************************ updateDropDownWidgets ************************************/
  /**
   * Synchronises the inherited date controls and the time widget with the current date-time.
   */
  @Override
  protected void updateDropDownWidgets()
  {
    // update the inherited date state before its widgets read m_date
    m_date = m_datetime.getDate();
    super.updateDropDownWidgets();
    if ( m_timeWidget != null )
      m_timeWidget.setTime( m_datetime.getTime() );
  }

  /****************************************** setTime ********************************************/
  private void setTime( Time time )
  {
    // combine a time-widget change with the date currently held by this field
    if ( time.getHour() == 24 && ButtonField.LAST_CHANGE_DELTA < 0.0 )
      time = time.plusHours( -1 ); // represent a backward midnight rollover as hour 23
    setDateTime( m_datetime.withTime( time ) );
  }

  /**************************************** getDateTime ******************************************/
  /**
   * Returns the current valid date-time, or the most recently parsed valid value while text is invalid.
   *
   * @return current valid date-time
   */
  public DateTime getDateTime()
  {
    // invalid edit text deliberately leaves the last successfully parsed value intact
    return m_datetime;
  }

  /**************************************** setDateTime ******************************************/
  /**
   * Sets the field from a date-time or literal text.
   * <p>
   * A {@link DateTime} is formatted canonically. Other non-null values are converted with
   * {@link Object#toString()}, while null displays blank text. Invalid text does not replace the
   * most recent valid date-time.
   *
   * @param value date-time or text-like value to display
   */
  public void setDateTime( Object value )
  {
    // typed values use canonical formatting; other values enter the normal validation path
    if ( value instanceof DateTime dt )
      setDateTime( dt );
    else
    {
      String txt = value == null ? "" : value.toString();
      setText( txt );
    }
  }

  /**************************************** setDateTime ******************************************/
  /**
   * Sets the current date-time, updates the drop-down, and signals when the value changes.
   *
   * @param datetime date-time to set
   * @throws NullPointerException if {@code datetime} is null
   */
  public void setDateTime( DateTime datetime )
  {
    // suppress redundant widget updates and external signals
    if ( !datetime.equals( m_datetime ) )
    {
      m_datetime = datetime;
      setText( format( datetime ) );
      updateDropDownWidgets();
      signal( datetime );
    }
  }

  /****************************************** setDate ********************************************/
  /**
   * Replaces the date component while preserving the current time component.
   * <p>
   * During superclass construction, before a date-time exists, midnight is used.
   *
   * @param date replacement date
   * @throws NullPointerException if {@code date} is null
   */
  @Override
  public void setDate( Date date )
  {
    // preserve time when the inherited calendar changes the date
    Time time = m_datetime == null ? Time.MIN_VALUE : m_datetime.getTime();
    setDateTime( DateTime.of( date, time ) );
  }

  /******************************************* format ********************************************/
  /**
   * Formats a date-time for display using ISO-style date text and millisecond time precision.
   *
   * @param datetime date-time to format
   * @return display text
   * @throws NullPointerException if {@code datetime} is null
   */
  public String format( DateTime datetime )
  {
    // the drop-down always exposes all four time components
    return datetime.format( "yyyy-MM-dd", 4 );
  }

  /**************************************** formatStatus *****************************************/
  /**
   * Formats a date-time for the informational status message.
   *
   * @param datetime date-time to format
   * @return localized long-date text with millisecond time precision
   * @throws NullPointerException if {@code datetime} is null
   */
  public String formatStatus( DateTime datetime )
  {
    // use a descriptive date while retaining full time precision
    return datetime.format( "eeee d MMMM yyyy", 4 );
  }

  /***************************************** parseText *******************************************/
  /**
   * Parses cleaned field text and signals when it represents a new valid date-time.
   *
   * @param text cleaned text to parse
   * @throws java.time.format.DateTimeParseException if the text cannot be parsed
   * @throws ArithmeticException if relative arithmetic or compact storage overflows
   */
  @Override
  protected void parseText( String text )
  {
    // let AbstractDropDownField translate parse failures into error status
    DateTime dt = DateTime.parse( text );
    if ( !dt.equals( m_datetime ) )
    {
      m_datetime = dt;
      signal( dt );
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
    return level == Level.INFO ? "Date-time: " + formatStatus( m_datetime ) : "Date-time format is not recognised";
  }

  /***************************************** validText *******************************************/
  /**
   * Restores canonical text for the most recent valid date-time and clears the shared status.
   *
   * @throws NullPointerException if no status object has been assigned
   */
  @Override
  protected void validText()
  {
    // ensure field displays last valid date-time
    setText( format( m_datetime ) );
    getStatus().clear();
  }

  /***************************************** changeValue *****************************************/
  /**
   * Steps a date or time component according to the supplied modifiers.
   * <p>
   * Without Alt, no modifier changes days, Shift changes months, and Control changes years. With
   * Alt, no modifier changes hours, Shift changes minutes, and Control changes seconds. A
   * simultaneous Shift-Control combination makes no change.
   *
   * @param delta amount to move, truncated to an integer
   * @param shift selects months or minutes
   * @param ctrl  selects years or seconds
   * @param alt   whether to change the time component
   */
  @Override
  public void changeValue( double delta, boolean shift, boolean ctrl, boolean alt )
  {
    if ( alt )
    {
      // modify field time-part
      if ( !shift && !ctrl )
        setDateTime( getDateTime().plusMilliseconds( (int) delta * Time.MILLIS_PER_HOUR ) );
      if ( shift && !ctrl )
        setDateTime( getDateTime().plusMilliseconds( (int) delta * Time.MILLIS_PER_MINUTE ) );
      if ( !shift && ctrl )
        setDateTime( getDateTime().plusMilliseconds( (int) delta * Time.MILLIS_PER_SECOND ) );
    }
    else
    {
      // modify field date-part
      if ( !shift && !ctrl )
        setDateTime( getDateTime().plusDays( (int) delta ) );
      if ( shift && !ctrl )
        setDateTime( getDateTime().plus( (long) delta, DateInterval.MONTH ) );
      if ( !shift && ctrl )
        setDateTime( getDateTime().plus( (long) delta, DateInterval.YEAR ) );
    }
  }

}
