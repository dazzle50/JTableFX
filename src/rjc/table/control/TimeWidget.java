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

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import rjc.table.data.types.Time;
import rjc.table.signal.IListener;
import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/******************* Number spin fields for Hour/Minutes/Seconds/Milliseconds ********************/
/*************************************************************************************************/

/**
 * A composite time editor consisting of spin fields for hours, minutes,
 * seconds, and milliseconds.
 * <p>
 * The widget supports configurable precision by showing or hiding individual
 * fields, validates entered values, propagates overflow into an associated
 * {@link CalendarWidget}, and emits signals when the represented
 * {@link Time} changes.
 * </p>
 * <p>
 * The maximum representable value is {@code 24:00:00.000}.
 * </p>
 */
public class TimeWidget extends HBox implements ISignal, IObservableStatus
{
  private NumberSpinField        m_hours      = new NumberSpinField();
  private NumberSpinField        m_mins       = new NumberSpinField();
  private NumberSpinField        m_secs       = new NumberSpinField();
  private NumberSpinField        m_millisecs  = new NumberSpinField();

  private Time                   m_time;
  private Time                   m_lastSignal;
  private ObservableStatus       m_status;
  private double                 m_width;

  private boolean                m_settingFields;
  private boolean                m_settling;
  private boolean                m_signalPending;

  private static final int       BORDER       = 4;

  // ratios for initial field widths
  private static final double    HOURS_RATIO  = 0.24;
  private static final double    MINS_RATIO   = 0.24;
  private static final double    SECS_RATIO   = 0.24;
  private static final double    MILLIS_RATIO = 0.28;

  // listener shared by all fields
  private static final IListener SIGNAL_TIME  = new IListener()
                                              {
                                                @Override
                                                public void slot( ISignal sender, Object... msg )
                                                {
                                                  if ( sender instanceof NumberSpinField field )
                                                    if ( field.getParent() instanceof TimeWidget widget )
                                                      widget.fieldChanged();
                                                }
                                              };

  /**************************************** constructor ******************************************/
  /**
   * Creates a time widget with an internal calendar used to handle
   * date overflow when time values exceed a single day.
   */
  public TimeWidget()
  {
    // create time widget (with non-visible calendar for overflow)
    createWidget( new CalendarWidget() );
  }

  /**************************************** constructor ******************************************/
  /**
   * Creates a time widget that uses the specified calendar to handle
   * date overflow and underflow operations.
   *
   * @param calendar the calendar associated with this time widget
   */
  public TimeWidget( CalendarWidget calendar )
  {
    // create time widget with this calendar
    createWidget( calendar );
  }

  /**************************************** createWidget *****************************************/
  private void createWidget( CalendarWidget calendar )
  {
    // create layout with the four number spin fields
    m_width = calendar.getWidth() - 3 * BORDER;

    configureField( m_hours, "00", 4, 0, 24, "Hours" );
    m_hours.setStepPage( 1, 6 );
    m_hours.setOverflowField( calendar );

    configureField( m_mins, "00", 4, 0, 59, "Minutes" );
    m_mins.setOverflowField( m_hours );

    configureField( m_secs, "00", 4, 0, 59, "Seconds" );
    m_secs.setOverflowField( m_mins );

    configureField( m_millisecs, "000", 6, 0, 999, "Milliseconds" );
    m_millisecs.setStepPage( 1, 100 );
    m_millisecs.setOverflowField( m_secs );

    setSpacing( BORDER - 1 );
    showFields( true, true, true, true );
    setTime( Time.MIN_VALUE );

    // filter for key events (e.g., ESCAPE resets the time).
    addEventFilter( KeyEvent.KEY_PRESSED, event -> keyPressed( event ) );

    // listen for focus changes to update or clear the status accordingly
    focusWithinProperty().addListener( ( property, oldFocus, newFocus ) ->
    {
      if ( newFocus )
        updateStatus( Level.INFO );
      else
      {
        setTime( m_time );
        if ( getStatus() != null )
          getStatus().clear();
      }
    } );
  }

  /*************************************** configureField ***************************************/
  private void configureField( NumberSpinField field, String format, int maxIntegerDigits, int min, int max, String id )
  {
    // configure numeric field and reset spin delta for direct text input
    field.setFormat( format, maxIntegerDigits, 0 );
    field.setRange( min, max );
    field.setId( id );
    field.addEventFilter( KeyEvent.KEY_TYPED, event -> ButtonField.LAST_CHANGE_DELTA = 0.0 );
    field.addListener( SIGNAL_TIME );
  }

  /***************************************** showFields ******************************************/
  /**
   * Controls which time fields are visible.
   * <p>
   * Hidden fields are reset to zero and are excluded from validation
   * and formatting. At least one field must remain visible.
   * </p>
   *
   * @param showHours {@code true} to display the hours field
   * @param showMins  {@code true} to display the minutes field
   * @param showSecs  {@code true} to display the seconds field
   * @param showMilli {@code true} to display the milliseconds field
   *
   * @throws IllegalArgumentException if all fields are hidden
   */
  public void showFields( boolean showHours, boolean showMins, boolean showSecs, boolean showMilli )
  {
    // check at least one field is shown
    if ( !showHours && !showMins && !showSecs && !showMilli )
      throw new IllegalArgumentException( "Must show at least one field" );

    // hidden fields are not part of the edited precision, so reset them to zero
    m_settingFields = true;
    if ( !showHours )
      m_hours.setValue( 0 );
    if ( !showMins )
      m_mins.setValue( 0 );
    if ( !showSecs )
      m_secs.setValue( 0 );
    if ( !showMilli )
      m_millisecs.setValue( 0 );
    m_settingFields = false;

    // control which fields are show
    double ratio = showHours ? HOURS_RATIO : 0.0;
    ratio += showMins ? MINS_RATIO : 0.0;
    ratio += showSecs ? SECS_RATIO : 0.0;
    ratio += showMilli ? MILLIS_RATIO : 0.0;

    // calculate new max widths accounting for hidden fields
    double width = m_width + ( showHours ? 0 : BORDER ) + ( showMins ? 0 : BORDER ) + ( showSecs ? 0 : BORDER )
        + ( showMilli ? 0 : BORDER );
    m_hours.setMaxWidth( width * HOURS_RATIO / ratio );
    m_mins.setMaxWidth( width * MINS_RATIO / ratio );
    m_secs.setMaxWidth( width * SECS_RATIO / ratio );
    m_millisecs.setMaxWidth( width * MILLIS_RATIO / ratio );
    getChildren().clear();

    // add fields and adjust width of last visible field
    if ( showHours )
    {
      getChildren().add( m_hours );
      if ( !showMins && !showSecs && !showMilli )
        m_hours.setMinWidth( width );
    }

    if ( showMins )
    {
      getChildren().add( m_mins );
      if ( !showSecs && !showMilli )
        m_mins.setMinWidth( 1 + width - ( showHours ? m_hours.getMaxWidth() : 0 ) );
    }

    if ( showSecs )
    {
      getChildren().add( m_secs );
      if ( !showMilli )
        m_secs.setMinWidth(
            1 + width - ( showHours ? m_hours.getMaxWidth() : 0 ) - ( showMins ? m_mins.getMaxWidth() : 0 ) );
    }

    if ( showMilli )
    {
      getChildren().add( m_millisecs );
      m_millisecs.setMinWidth( 1 + width - ( showHours ? m_hours.getMaxWidth() : 0 )
          - ( showMins ? m_mins.getMaxWidth() : 0 ) - ( showSecs ? m_secs.getMaxWidth() : 0 ) );
    }

    getTime();
  }

  /**************************************** getFieldCount ****************************************/
  /**
   * Returns the number of currently visible time fields.
   *
   * @return the visible field count, from {@code 1} to {@code 4}
   */
  public int getFieldCount()
  {
    // return the number of fields the string format of time should show
    if ( getChildren().contains( m_millisecs ) )
      return 4;
    if ( getChildren().contains( m_secs ) )
      return 3;
    if ( getChildren().contains( m_mins ) )
      return 2;
    return 1;
  }

  /***************************************** keyPressed ******************************************/
  /**
   * Handles key press events for the widget.
   * <p>
   * Pressing {@link KeyCode#ESCAPE} restores the last committed time.
   * </p>
   *
   * @param event the key event
   */
  public void keyPressed( KeyEvent event )
  {
    // react to certain key presses
    if ( event.getCode() == KeyCode.ESCAPE )
      setTime( m_time );
  }

  /******************************************* getTime *******************************************/
  /**
   * Returns the current time represented by the widget.
   * <p>
   * The field contents are validated before the value is returned.
   * If validation fails, the previously valid time is returned and
   * the widget status is updated with an error message.
   * </p>
   *
   * @return the current valid time value
   */
  public Time getTime()
  {
    // check if spin-fields represent a valid time, and update status
    clearFieldStyles();

    boolean msValid = isFieldValid( m_millisecs, 0, 999 );
    boolean sValid = isFieldValid( m_secs, 0, 59 );
    boolean mValid = isFieldValid( m_mins, 0, 59 );
    boolean hValid = isFieldValid( m_hours, 0, 24 );

    if ( hValid && mValid && sValid && msValid )
    {
      // check time does not exceed max 24h
      int hrs = getFieldValue( m_hours );
      int mins = getFieldValue( m_mins );
      int secs = getFieldValue( m_secs );
      int ms = getFieldValue( m_millisecs );
      if ( hrs == 24 && ( mins > 0 || secs > 0 || ms > 0 ) )
      {
        setError( "Max time is 24:00:00.000" );
        markIfNonZero( m_mins );
        markIfNonZero( m_secs );
        markIfNonZero( m_millisecs );
        return m_time;
      }

      // collect the new valid time from widgets
      m_time = Time.of( hrs, mins, secs, ms );
      updateStatus( Level.INFO );
    }

    return m_time;
  }

  /*************************************** getMilliseconds ***************************************/
  private int getMilliseconds()
  {
    // return time-widget time in milliseconds (might be invalid greater than 24:00:00.000)
    int hrs = getFieldValue( m_hours );
    int mins = getFieldValue( m_mins );
    int secs = getFieldValue( m_secs );
    int ms = getFieldValue( m_millisecs );
    return hrs * Time.MILLIS_PER_HOUR + mins * Time.MILLIS_PER_MINUTE + secs * Time.MILLIS_PER_SECOND + ms;
  }

  /**************************************** getFieldValue ****************************************/
  private int getFieldValue( NumberSpinField field )
  {
    // if field is shown return field integer value, otherwise return zero
    if ( isFieldShown( field ) )
      return field.getInteger();
    else
      return 0;
  }

  /***************************************** isFieldShown ****************************************/
  private boolean isFieldShown( NumberSpinField field )
  {
    // return true if field is part of the current visible precision
    return getChildren().contains( field );
  }

  /**************************************** isFieldValid *****************************************/
  private boolean isFieldValid( NumberSpinField field, int min, int max )
  {
    // hidden fields have already been reset and are not part of validation
    if ( !isFieldShown( field ) )
      return true;

    try
    {
      int value = Integer.parseInt( field.getText() );
      if ( value >= min && value <= max )
        return true;

      setError( field.getId() + " must be between " + min + " and " + max + " (inclusive)" );
    }
    catch ( Exception exception )
    {
      setError( field.getId() + " is invalid" );
    }

    field.setStyle( errorStyle() );
    return false;
  }

  /**************************************** formatStatus *****************************************/
  /**
   * Returns a status string representation of the supplied time.
   *
   * @param time the time to format
   *
   * @return the formatted status text
   */
  public String formatStatus( Time time )
  {
    // return time in status format
    return time.toString();
  }

  /**************************************** updateStatus *****************************************/
  private void updateStatus( Level level )
  {
    // if focused, update status with level and appropriate text
    if ( getStatus() != null && focusWithinProperty().get() )
    {
      // if widget in popup don't update status (assume status updated elsewhere)
      if ( getScene() != null && getScene().getWindow() instanceof Popup )
        return;

      String msg = level == Level.INFO ? "Time: " + formatStatus( m_time ) : "Time format is not recognised";
      getStatus().update( level, msg );
    }
  }

  /******************************************* setTime *******************************************/
  /**
   * Updates the widget to display the specified time.
   * <p>
   * Values are truncated to the currently visible precision. Hidden
   * fields are set to zero.
   * </p>
   *
   * @param time the time to display, or {@code null} to display
   *             {@link Time#MIN_VALUE}
   */
  public void setTime( Time time )
  {
    // set spin fields to represent specified time truncated to visible precision
    Time value = time == null ? Time.MIN_VALUE : time;
    int hours = isFieldShown( m_hours ) ? value.getHour() : 0;
    int mins = isFieldShown( m_mins ) ? value.getMinute() : 0;
    int secs = isFieldShown( m_secs ) ? value.getSecond() : 0;
    int millisecs = isFieldShown( m_millisecs ) ? value.getMillisecond() : 0;

    m_time = Time.of( hours, mins, secs, millisecs );
    m_settingFields = true;
    m_hours.setValue( hours );
    m_mins.setValue( mins );
    m_secs.setValue( secs );
    m_millisecs.setValue( millisecs );
    m_settingFields = false;
    clearFieldStyles();
  }

  /**************************************** fieldChanged *****************************************/
  private void fieldChanged()
  {
    // one user spin can change several fields through overflow, so coalesce to a single signal
    if ( m_settingFields || m_settling )
      return;

    if ( ButtonField.LAST_CHANGE_DELTA == 0.0 )
      settleAndSignal();
    else if ( !m_signalPending )
    {
      m_signalPending = true;
      Platform.runLater( () ->
      {
        m_signalPending = false;
        settleAndSignal();
      } );
    }
  }

  /*************************************** settleAndSignal ***************************************/
  private void settleAndSignal()
  {
    // if a spin moved outside 00:00..24:00, overflow the date through the calendar
    m_settling = true;
    try
    {
      if ( ButtonField.LAST_CHANGE_DELTA != 0.0 )
        while ( getMilliseconds() > Time.MILLIS_PER_DAY )
          m_hours.changeValue( ButtonField.LAST_CHANGE_DELTA < 0 ? -1 : 1 );
    }
    finally
    {
      m_settling = false;
    }

    Time time = getTime();
    if ( !time.equals( m_lastSignal ) )
    {
      m_lastSignal = time;
      signal( time );
    }
  }

  /**************************************** clearFieldStyles *************************************/
  private void clearFieldStyles()
  {
    // clear prior error styling
    String style = ObservableStatus.getStyle( Level.INFO );
    m_hours.setStyle( style );
    m_mins.setStyle( style );
    m_secs.setStyle( style );
    m_millisecs.setStyle( style );
  }

  /***************************************** markIfNonZero ***************************************/
  private void markIfNonZero( NumberSpinField field )
  {
    // highlight only the non-zero lower-order fields that make 24:00 invalid
    if ( isFieldShown( field ) && field.getInteger() > 0 )
      field.setStyle( errorStyle() );
  }

  /******************************************* setError ******************************************/
  private void setError( String msg )
  {
    // update status if available
    if ( getStatus() != null )
      getStatus().update( Level.ERROR, msg );
  }

  /****************************************** errorStyle *****************************************/
  private String errorStyle()
  {
    // return status error style even before a status object has been attached
    return getStatus() == null ? ObservableStatus.getStyle( Level.ERROR ) : getStatus().getStyle();
  }

  /***************************************** setStatus *******************************************/
  /**
   * Associates a status object with this widget.
   *
   * @param status the status object used to display validation and
   *               informational messages
   */
  @Override
  public void setStatus( ObservableStatus status )
  {
    // set widget status
    m_status = status;
  }

  /***************************************** getStatus *******************************************/
  /**
   * Returns the status object associated with this widget.
   *
   * @return the current status object, or {@code null} if none has been assigned
   */
  @Override
  public ObservableStatus getStatus()
  {
    // return widget status
    return m_status;
  }
}