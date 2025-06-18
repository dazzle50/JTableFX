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

import java.lang.ref.WeakReference;

import javafx.application.Platform;
import rjc.table.control.dropdown.AbstractDropDownField;
import rjc.table.data.types.Time;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/******************************* Time field control with drop-down *******************************/
/*************************************************************************************************/

public class TimeField extends AbstractDropDownField
{
  private Time       m_time;       // field current time (or most recent valid)
  private TimeWidget m_timeWidget; // time-widget in drop-down

  /**************************************** constructor ******************************************/
  public TimeField()
  {
    // prepare time-widget and add to the drop-down
    m_timeWidget = new TimeWidget();
    getDropDownGrid().addRow( 0, m_timeWidget );
    setAllowed( "\\d*[:. ]?\\d*[:. ]?\\d*[. ]?\\d*[a]?[p]?" );

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
    setTime( Time.fromHours( Time.now().getHours() ) );
  }

  /**************************************** constructor ******************************************/
  public TimeField( boolean showHours, boolean showMins, boolean showSecs, boolean showMilli )
  {
    // create time-field with specified time components in drop-down
    this();
    m_timeWidget.showFields( showHours, showMins, showSecs, showMilli );
  }

  /************************************ updateDropDownWidgets ************************************/
  @Override
  protected void updateDropDownWidgets()
  {
    // update the widgets in drop-down to reflect current time
    m_timeWidget.setTime( m_time );
  }

  /****************************************** getTime ********************************************/
  public Time getTime()
  {
    // return field time (or most recent valid)
    return m_time;
  }

  /****************************************** setTime ********************************************/
  public void setTime( Object value )
  {
    // set field value depending on object type
    if ( value instanceof Time time )
      setTime( time );
    else
    {
      String txt = value == null ? "" : value.toString();
      setText( txt );
    }
  }

  /****************************************** setTime ********************************************/
  public void setTime( Time time )
  {
    // set current field time, display in text and signal change
    if ( !time.equals( m_time ) )
    {
      m_time = time;
      setText( format( time ) );
      updateDropDownWidgets();
      signal( time );
    }
  }

  /******************************************* format ********************************************/
  public String format( Time time )
  {
    // return time in display format
    return time.toString();
  }

  /**************************************** formatStatus *****************************************/
  public String formatStatus( Time time )
  {
    // return time in status format
    return time.toString();
  }

  /***************************************** parseText *******************************************/
  @Override
  protected void parseText( String text )
  {
    // convert text to time, and if different signal (any exception handled in abstract)
    Time time = Time.fromString( text );
    if ( !time.equals( m_time ) )
    {
      m_time = time;
      signal( time );
    }
  }

  /***************************************** statusText ******************************************/
  @Override
  protected String statusText( Level level )
  {
    // return status text appropriate to the level
    return level == Level.NORMAL ? "Time: " + formatStatus( m_time ) : "Time format is not recognised";
  }

  /***************************************** validText *******************************************/
  @Override
  protected void validText()
  {
    // ensure field displays last valid time
    setText( format( m_time ) );
    getStatus().clear();
  }

  /***************************************** changeValue *****************************************/
  @Override
  public void changeValue( double delta, boolean shift, boolean ctrl, boolean alt )
  {
    // modify field value
    if ( !shift && !ctrl )
      m_time.addMilliseconds( (int) ( delta * Time.ONE_HOUR ) );
    if ( shift && !ctrl )
      m_time.addMilliseconds( (int) ( delta * Time.ONE_MINUTE ) );
    if ( !shift && ctrl )
      m_time.addMilliseconds( (int) ( delta * Time.ONE_SECOND ) );

    // display in text and signal change
    setText( format( m_time ) );
    signal( m_time );
  }
}
