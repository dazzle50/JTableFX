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
import rjc.table.data.types.Date;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/**************************** Date-time field control with drop-down *****************************/
/*************************************************************************************************/

public class DateTimeField extends DateField
{
  private DateTime   m_datetime;   // field current date-time (or most recent valid)
  private TimeWidget m_timeWidget; // time-widget in drop-down

  /**************************************** constructor ******************************************/
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
    long now = DateTime.now().getMilliseconds() / Time.ONE_HOUR;
    setDateTime( new DateTime( now * Time.ONE_HOUR ) );
  }

  /************************************ updateDropDownWidgets ************************************/
  @Override
  protected void updateDropDownWidgets()
  {
    // update the widgets in drop-down to reflect current date-time
    m_date = m_datetime.getDate();
    super.updateDropDownWidgets();
    if ( m_timeWidget != null )
      m_timeWidget.setTime( m_datetime.getTime() );
  }

  /****************************************** setTime ********************************************/
  private void setTime( Time time )
  {
    // set time component of date-time
    if ( time.getHours() == 24 && ButtonField.LAST_CHANGE_DELTA < 0.0 )
      time.addMilliseconds( -Time.ONE_HOUR );
    setDateTime( new DateTime( m_datetime.getDate(), time ) );
  }

  /**************************************** getDateTime ******************************************/
  public DateTime getDateTime()
  {
    // return field date-time (or most recent valid)
    return m_datetime;
  }

  /**************************************** setDateTime ******************************************/
  public void setDateTime( Object value )
  {
    // set field value depending on object type
    if ( value instanceof DateTime dt )
      setDateTime( dt );
    else
    {
      String txt = value == null ? "" : value.toString();
      setText( txt );
    }
  }

  /**************************************** setDateTime ******************************************/
  public void setDateTime( DateTime datetime )
  {
    // set current field date, display in text, signal change
    if ( !datetime.equals( m_datetime ) )
    {
      m_datetime = datetime;
      setText( format( datetime ) );
      updateDropDownWidgets();
      signal( datetime );
    }
  }

  /****************************************** setDate ********************************************/
  @Override
  public void setDate( Date date )
  {
    // overloaded to add time component
    Time time = m_datetime == null ? Time.MIN_VALUE : m_datetime.getTime();
    DateTime dt = new DateTime( date, time );
    setDateTime( dt );
  }

  /******************************************* format ********************************************/
  public String format( DateTime datetime )
  {
    // return date-time in display format
    return datetime.toString( "yyyy-MM-dd HH:mm:ss.SSS" );
  }

  /**************************************** formatStatus *****************************************/
  public String formatStatus( DateTime datetime )
  {
    // return date in status format
    return datetime.toString( "eeee d MMMM yyyy HH:mm:ss.SSS" );
  }

  /***************************************** parseText *******************************************/
  @Override
  protected void parseText( String text )
  {
    // convert text to date-time, and if different signal (any exception handled in abstract)
    DateTime dt = DateTime.parse( text,
        "[uuuuMMdd][uu-M-d][uuuu-M-d][uuuu-DDD][['T'][' '][HHmmss][HHmm][H:m:s][H:m][.SSS][.SS][.S]]" );
    if ( !dt.equals( m_datetime ) )
    {
      m_datetime = dt;
      signal( dt );
    }
  }

  /***************************************** statusText ******************************************/
  @Override
  protected String statusText( Level level )
  {
    // return status text appropriate to the level
    return level == Level.NORMAL ? "Date-time: " + formatStatus( m_datetime ) : "Date-time format is not recognised";
  }

  /***************************************** validText *******************************************/
  @Override
  protected void validText()
  {
    // ensure field displays last valid date-time
    setText( format( m_datetime ) );
    getStatus().clear();
  }

  /***************************************** changeValue *****************************************/
  @Override
  public void changeValue( double delta, boolean shift, boolean ctrl, boolean alt )
  {
    if ( alt )
    {
      // modify field time-part
      if ( !shift && !ctrl )
        setDateTime( getDateTime().plusMilliseconds( (int) delta * Time.ONE_HOUR ) );
      if ( shift && !ctrl )
        setDateTime( getDateTime().plusMilliseconds( (int) delta * Time.ONE_MINUTE ) );
      if ( !shift && ctrl )
        setDateTime( getDateTime().plusMilliseconds( (int) delta * Time.ONE_SECOND ) );
    }
    else
    {
      // modify field date-part
      if ( !shift && !ctrl )
        setDateTime( getDateTime().plusDays( (int) delta ) );
      if ( shift && !ctrl )
        setDateTime( getDateTime().plusMonths( (int) delta ) );
      if ( !shift && ctrl )
        setDateTime( getDateTime().plusYears( (int) delta ) );
    }
  }

}
