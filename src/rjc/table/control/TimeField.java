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
import rjc.table.control.dropdown.AbstractDropDownField;
import rjc.table.data.types.Time;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/******************************* Time field control with drop-down *******************************/
/*************************************************************************************************/

/**
 * Editable time field with a configurable {@link TimeWidget} drop-down.
 * <p>
 * Parseable text updates the most recent valid {@link Time} and emits it through {@link #signal}.
 * Invalid text is styled as an error while editing and is replaced by the most recent valid time
 * on focus loss or Escape. A non-null status object must be assigned before that normalization
 * because it clears the shared status. Arrow and wheel modifiers step hours, minutes, seconds, or
 * milliseconds.
 */
public class TimeField extends AbstractDropDownField
{
  private Time       m_time;       // field current time (or most recent valid)
  private TimeWidget m_timeWidget; // time-widget in drop-down

  /**************************************** constructor ******************************************/
  /**
   * Creates a time field showing all four components and initially set to the current hour.
   */
  public TimeField()
  {
    // prepare time-widget and add to the drop-down
    m_timeWidget = new TimeWidget();
    getDropDownGrid().addRow( 0, m_timeWidget );
    setAllowed( "^(\\d+[-:. h]?\\d*[-:. m]?\\d*[-. s]?\\d*([ap]m?)?)?$" );

    // if time-widget has focus and changes, update the field time to match
    var weak = new WeakReference<TimeField>( this );
    m_timeWidget.addListener( ( sender, time ) ->
    {
      if ( weak.get().m_timeWidget.isFocusWithin() )
        weak.get().setTime( (Time) time[0] );
    } );

    // add status later as not yet set
    Platform.runLater( () -> m_timeWidget.setStatus( getStatus() ) );

    // set initial time truncated to hour
    setTime( Time.ofHours( Time.now().getHour() ) );
  }

  /**************************************** constructor ******************************************/
  /**
   * Creates a time field with selected components visible in its drop-down.
   *
   * @param showHours whether to show hours
   * @param showMins  whether to show minutes
   * @param showSecs  whether to show seconds
   * @param showMilli whether to show milliseconds
   * @throws IllegalArgumentException if all components are hidden
   */
  public TimeField( boolean showHours, boolean showMins, boolean showSecs, boolean showMilli )
  {
    // create time-field with specified time components in drop-down
    this();
    m_timeWidget.showFields( showHours, showMins, showSecs, showMilli );
    setText( format( getTime() ) );
  }

  /************************************ updateDropDownWidgets ************************************/
  /**
   * Synchronises the drop-down widget with the current valid time.
   */
  @Override
  protected void updateDropDownWidgets()
  {
    // setTime suppresses nested field signals while synchronising the widget
    m_timeWidget.setTime( m_time );
  }

  /****************************************** getTime ********************************************/
  /**
   * Returns the current valid time, or the most recently parsed valid time while the text is invalid.
   *
   * @return current valid time
   */
  public Time getTime()
  {
    // invalid edit text deliberately leaves the last successfully parsed value intact
    return m_time;
  }

  /****************************************** setTime ********************************************/
  /**
   * Sets the field from a time or literal text.
   * <p>
   * A {@link Time} is formatted according to the configured component precision. Other non-null
   * values are converted with {@link Object#toString()}, while null displays blank text. Invalid
   * text does not replace the most recent valid time.
   *
   * @param value time or text-like value to display
   */
  public void setTime( Object value )
  {
    // typed times use configured formatting; other values enter the normal validation path
    if ( value instanceof Time time )
      setTime( time );
    else
    {
      String txt = value == null ? "" : value.toString();
      setText( txt );
    }
  }

  /****************************************** setTime ********************************************/
  /**
   * Sets the current time, updates the drop-down, and signals when the value changes.
   *
   * @param time time to set
   * @throws NullPointerException if {@code time} is null
   */
  public void setTime( Time time )
  {
    // suppress redundant widget updates and external signals
    if ( !time.equals( m_time ) )
    {
      m_time = time;
      setText( format( time ) );
      updateDropDownWidgets();
      signal( time );
    }
  }

  /******************************************* format ********************************************/
  /**
   * Formats a time using the component precision implied by the drop-down configuration.
   *
   * @param time time to format
   * @return display text
   * @throws NullPointerException if {@code time} is null
   */
  public String format( Time time )
  {
    // format to the precision implied by the most precise visible widget field
    return time.formatComponents( m_timeWidget.getFieldCount() );
  }

  /**************************************** formatStatus *****************************************/
  /**
   * Formats a time for the informational status message.
   *
   * @param time time to format
   * @return status text
   * @throws NullPointerException if {@code time} is null
   */
  public String formatStatus( Time time )
  {
    // keep this overridable separately from the field display format
    return format( time );
  }

  /***************************************** parseText *******************************************/
  /**
   * Parses cleaned field text and signals when it represents a new valid time.
   *
   * @param text cleaned text to parse
   * @throws java.time.format.DateTimeParseException if the text cannot be parsed
   */
  @Override
  protected void parseText( String text )
  {
    // let AbstractDropDownField translate parse failures into error status
    Time time = Time.parse( text );
    if ( !time.equals( m_time ) )
    {
      m_time = time;
      signal( time );
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
    return level == Level.INFO ? "Time: " + formatStatus( m_time ) : "Time format is not recognised";
  }

  /***************************************** validText *******************************************/
  /**
   * Restores canonical text for the most recent valid time and clears the shared status.
   *
   * @throws NullPointerException if no status object has been assigned
   */
  @Override
  protected void validText()
  {
    // ensure field displays last valid time
    setText( format( m_time ) );
    getStatus().clear();
  }

  /***************************************** changeValue *****************************************/
  /**
   * Steps the time component selected by the supplied modifiers.
   * <p>
   * With neither Shift nor Control, no modifier changes hours and Alt changes milliseconds. Shift
   * without Control changes minutes, and Control without Shift changes seconds; Alt is ignored in
   * both cases. Pressing Shift and Control together leaves the time unchanged but retains the
   * existing signal behaviour.
   *
   * @param delta amount to move, truncated to an integer
   * @param shift whether to change minutes
   * @param ctrl  whether to change seconds
   * @param alt   whether to change milliseconds when Shift and Control are not held
   */
  @Override
  public void changeValue( double delta, boolean shift, boolean ctrl, boolean alt )
  {
    // Shift and Control take precedence over Alt when selecting the component
    if ( !shift && !ctrl & !alt )
      m_time = m_time.plusHours( (int) delta );
    if ( shift && !ctrl )
      m_time = m_time.plusMinutes( (int) delta );
    if ( !shift && ctrl )
      m_time = m_time.plusSeconds( (int) delta );
    if ( !shift && !ctrl && alt )
      m_time = m_time.plusMilliseconds( (int) delta );

    // preserve signalling when Shift and Control together leave the value unchanged
    setText( format( m_time ) );
    signal( m_time );
  }
}
