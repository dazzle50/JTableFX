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
import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableStatus;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/******************* Number spin fields for Hour/Minutes/Seconds/Milliseconds ********************/
/*************************************************************************************************/

public class TimeWidget extends HBox implements ISignal, IObservableStatus
{
  private NumberSpinField  m_hours     = new NumberSpinField();
  private NumberSpinField  m_mins      = new NumberSpinField();
  private NumberSpinField  m_secs      = new NumberSpinField();
  private NumberSpinField  m_millisecs = new NumberSpinField();
  private Time             m_time;
  private Time             m_lastSignal;
  private ObservableStatus m_status;

  private static final int BORDER      = 4;

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
    double width = calendar.getWidth() - 3 * BORDER;

    m_hours.setMaxWidth( width * 0.24 );
    m_hours.setFormat( "00", 4, 0 );
    m_hours.setRange( 0, 24 );
    m_hours.setStepPage( 1, 6 );
    m_hours.setOverflowField( calendar );
    m_hours.setId( "Hours" );
    m_hours.addListener( ( sender, msg ) -> signalTime() );

    m_mins.setMaxWidth( width * 0.24 );
    m_mins.setFormat( "00", 4, 0 );
    m_mins.setRange( 0, 59 );
    m_mins.setOverflowField( m_hours );
    m_mins.setId( "Minutes" );
    m_mins.addListener( ( sender, msg ) -> signalTime() );

    m_secs.setMaxWidth( width * 0.24 );
    m_secs.setFormat( "00", 4, 0 );
    m_secs.setRange( 0, 59 );
    m_secs.setOverflowField( m_mins );
    m_secs.setId( "Seconds" );
    m_secs.addListener( ( sender, msg ) -> signalTime() );

    m_millisecs.setMaxWidth( 1 + width - m_hours.getMaxWidth() - m_mins.getMaxWidth() - m_secs.getMaxWidth() );
    m_millisecs.setFormat( "000", 6, 0 );
    m_millisecs.setRange( 0, 999 );
    m_millisecs.setStepPage( 1, 100 );
    m_millisecs.setOverflowField( m_secs );
    m_millisecs.setId( "Milliseconds" );
    m_millisecs.addListener( ( sender, msg ) -> signalTime() );

    setSpacing( BORDER - 1 );
    getChildren().addAll( m_hours, m_mins, m_secs, m_millisecs );
    setTime( Time.fromHours( Time.now().getHours() ) );

    addEventFilter( KeyEvent.KEY_PRESSED, event -> keyPressed( event ) );
    focusWithinProperty().addListener( ( observable, oldFocus, newFocus ) ->
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
