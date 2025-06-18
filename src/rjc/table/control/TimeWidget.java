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
                                                      widget.signalTime();
                                                }
                                              };

  /**************************************** constructor ******************************************/
  public TimeWidget()
  {
    // create time widget (with non-visible calendar for overflow)
    createWidget( new CalendarWidget() );
  }

  /**************************************** constructor ******************************************/
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

    m_hours.setMaxWidth( m_width * HOURS_RATIO );
    m_hours.setFormat( "00", 4, 0 );
    m_hours.setRange( 0, 24 );
    m_hours.setStepPage( 1, 6 );
    m_hours.setOverflowField( calendar );
    m_hours.setId( "Hours" );
    m_hours.addListener( SIGNAL_TIME );

    m_mins.setMaxWidth( m_width * MINS_RATIO );
    m_mins.setFormat( "00", 4, 0 );
    m_mins.setRange( 0, 59 );
    m_mins.setOverflowField( m_hours );
    m_mins.setId( "Minutes" );
    m_mins.addListener( SIGNAL_TIME );

    m_secs.setMaxWidth( m_width * SECS_RATIO );
    m_secs.setFormat( "00", 4, 0 );
    m_secs.setRange( 0, 59 );
    m_secs.setOverflowField( m_mins );
    m_secs.setId( "Seconds" );
    m_secs.addListener( SIGNAL_TIME );

    m_millisecs.setMaxWidth( 1 + m_width - m_hours.getMaxWidth() - m_mins.getMaxWidth() - m_secs.getMaxWidth() );
    m_millisecs.setFormat( "000", 6, 0 );
    m_millisecs.setRange( 0, 999 );
    m_millisecs.setStepPage( 1, 100 );
    m_millisecs.setOverflowField( m_secs );
    m_millisecs.setId( "Milliseconds" );
    m_millisecs.addListener( SIGNAL_TIME );

    setSpacing( BORDER - 1 );
    getChildren().addAll( m_hours, m_mins, m_secs, m_millisecs );
    setTime( Time.fromHours( Time.now().getHours() ) );

    // filter for key events (e.g., ESCAPE resets the time).
    addEventFilter( KeyEvent.KEY_PRESSED, event -> keyPressed( event ) );

    // listen for focus changes to update or clear the status accordingly
    focusWithinProperty().addListener( ( property, oldFocus, newFocus ) ->
    {
      if ( newFocus )
        // gained focus
        updateStatus( Level.NORMAL );
      else
      {
        // lost focus
        setTime( m_time );
        getStatus().clear();
      }
    } );
  }

  /***************************************** showFields ******************************************/
  public void showFields( boolean showHours, boolean showMins, boolean showSecs, boolean showMilli )
  {
    // check at least one field is shown
    if ( !showHours && !showMins && !showSecs && !showMilli )
      throw new IllegalArgumentException( "Must show at least one field" );

    // control which fields are show
    double ratio = showHours ? HOURS_RATIO : 0.0;
    ratio += showMins ? MINS_RATIO : 0.0;
    ratio += showSecs ? SECS_RATIO : 0.0;
    ratio += showMilli ? MILLIS_RATIO : 0.0;

    // calculate new max widths accounting for hidden fields
    m_width += ( showHours ? 0 : BORDER ) + ( showMins ? 0 : BORDER ) + ( showSecs ? 0 : BORDER )
        + ( showMilli ? 0 : BORDER );
    m_hours.setMaxWidth( m_width * HOURS_RATIO / ratio );
    m_mins.setMaxWidth( m_width * MINS_RATIO / ratio );
    m_secs.setMaxWidth( m_width * SECS_RATIO / ratio );
    m_millisecs.setMaxWidth( m_width * MILLIS_RATIO / ratio );
    getChildren().clear();

    // add fields and adjust width of last visible field
    if ( showHours )
    {
      getChildren().add( m_hours );
      if ( !showMins && !showSecs && !showMilli )
        m_hours.setMinWidth( m_width );
    }

    if ( showMins )
    {
      getChildren().add( m_mins );
      if ( !showSecs && !showMilli )
        m_mins.setMinWidth( 1 + m_width - ( showHours ? m_hours.getMaxWidth() : 0 ) );
    }

    if ( showSecs )
    {
      getChildren().add( m_secs );
      if ( !showMilli )
        m_secs.setMinWidth(
            1 + m_width - ( showHours ? m_hours.getMaxWidth() : 0 ) - ( showMins ? m_mins.getMaxWidth() : 0 ) );
    }

    if ( showMilli )
    {
      getChildren().add( m_millisecs );
      m_millisecs.setMinWidth( 1 + m_width - ( showHours ? m_hours.getMaxWidth() : 0 )
          - ( showMins ? m_mins.getMaxWidth() : 0 ) - ( showSecs ? m_secs.getMaxWidth() : 0 ) );
    }
  }

  /***************************************** keyPressed ******************************************/
  public void keyPressed( KeyEvent event )
  {
    // react to certain key presses
    if ( event.getCode() == KeyCode.ESCAPE )
      setTime( m_time );
  }

  /******************************************* getTime *******************************************/
  public Time getTime()
  {
    // check if spin-fields represent a valid time, and update status
    boolean msValid = isValid( m_millisecs, 0, 999 );
    boolean sValid = isValid( m_secs, 0, 59 );
    boolean mValid = isValid( m_mins, 0, 59 );
    boolean hValid = isValid( m_hours, 0, 24 );

    if ( hValid && mValid && sValid && msValid )
    {
      // check time does not exceed max 24h
      int hrs = m_hours.getInteger();
      int mins = m_mins.getInteger();
      int secs = m_secs.getInteger();
      int ms = m_millisecs.getInteger();
      if ( hrs == 24 && ( mins > 0 || secs > 0 || ms > 0 ) )
      {
        getStatus().update( Level.ERROR, "Max time is 24:00:00.000" );
        m_mins.setStyle( getStatus().getStyle() );
        m_secs.setStyle( getStatus().getStyle() );
        m_millisecs.setStyle( getStatus().getStyle() );
        return m_time;
      }

      // collect the new valid time from widgets
      m_time = new Time( hrs, mins, secs, ms );
      updateStatus( Level.NORMAL );
    }

    return m_time;
  }

  /******************************************* isValid *******************************************/
  private boolean isValid( NumberSpinField field, int min, int max )
  {
    // return if field value is between min & max inclusive + set status
    String msg;
    try
    {
      int value = Integer.parseInt( field.getText() );
      if ( value >= min && value <= max )
      {
        field.setStyle( ObservableStatus.getStyle( Level.NORMAL ) );
        return true;
      }

      msg = field.getId() + " needs to between " + min + " and " + max + " (inclusive)";
    }
    catch ( Exception exception )
    {
      msg = field.getId() + " is invalid";
    }

    getStatus().update( Level.ERROR, msg );
    field.setStyle( getStatus().getStyle() );
    return false;
  }

  /**************************************** formatStatus *****************************************/
  public String formatStatus( Time time )
  {
    // return date in status format
    return time.toString();
  }

  /**************************************** updateStatus *****************************************/
  private void updateStatus( Level level )
  {
    // if focused, update status with level and appropriate text
    if ( getStatus() != null && focusWithinProperty().get() )
    {
      // if widget in popup don't update status (assume status updated elsewhere)
      if ( getScene().getWindow() instanceof Popup )
        return;

      String msg = level == Level.NORMAL ? "Time: " + formatStatus( m_time ) : "Time format is not recognised";
      getStatus().update( level, msg );
    }
  }

  /******************************************* setTime *******************************************/
  public void setTime( Time time )
  {
    // set spin fields to represent specified time
    m_hours.setValue( time.getHours() );
    m_mins.setValue( time.getMinutes() );
    m_secs.setValue( time.getSeconds() );
    m_millisecs.setValue( time.getMilliseconds() );
  }

  /***************************************** signalTime ******************************************/
  private void signalTime()
  {
    // signal new time - but run later to allow any field wrapping to complete first
    Platform.runLater( () ->
    {
      Time time = getTime();
      if ( !time.equals( m_lastSignal ) )
      {
        m_lastSignal = time;
        signal( time );
      }
    } );
  }

  /***************************************** setStatus *******************************************/
  @Override
  public void setStatus( ObservableStatus status )
  {
    // set widget status
    m_status = status;
  }

  /***************************************** getStatus *******************************************/
  @Override
  public ObservableStatus getStatus()
  {
    // return widget status
    return m_status;
  }
}
