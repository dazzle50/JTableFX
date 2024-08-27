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

import javafx.application.Platform;
import javafx.scene.layout.HBox;
import rjc.table.data.types.Time;
import rjc.table.signal.ISignal;

/*************************************************************************************************/
/******************* Number spin fields for Hour/Minutes/Seconds/Milliseconds ********************/
/*************************************************************************************************/

public class TEMP_TimeWidget extends HBox implements ISignal
{
  private NumberSpinField  m_hours     = new NumberSpinField();
  private NumberSpinField  m_mins      = new NumberSpinField();
  private NumberSpinField  m_secs      = new NumberSpinField();
  private NumberSpinField  m_millisecs = new NumberSpinField();
  private Time             m_lastSignal;

  private static final int BORDER      = 4;

  /**************************************** constructor ******************************************/
  public TEMP_TimeWidget( CalendarWidget calendar )
  {
    // create layout with the four number spin fields
    double width = calendar.getWidth() - 3 * BORDER;

    m_hours.setMaxWidth( width * 0.24 );
    m_hours.setFormat( "00", 0 );
    m_hours.setRange( 0, 23 );
    m_hours.setStepPage( 1, 6 );
    m_hours.setOverflowField( calendar );
    m_hours.setValue( 0 );
    m_hours.addListener( ( sender, msg ) -> signalTime() );

    m_mins.setMaxWidth( width * 0.24 );
    m_mins.setFormat( "00", 0 );
    m_mins.setRange( 0, 59 );
    m_mins.setOverflowField( m_hours );
    m_mins.setValue( 0 );
    m_mins.addListener( ( sender, msg ) -> signalTime() );

    m_secs.setMaxWidth( width * 0.24 );
    m_secs.setFormat( "00", 0 );
    m_secs.setRange( 0, 59 );
    m_secs.setOverflowField( m_mins );
    m_secs.setValue( 0 );
    m_secs.addListener( ( sender, msg ) -> signalTime() );

    m_millisecs.setMaxWidth( 1 + width - m_hours.getMaxWidth() - m_mins.getMaxWidth() - m_secs.getMaxWidth() );
    m_millisecs.setFormat( "000", 0 );
    m_millisecs.setRange( 0, 999 );
    m_millisecs.setStepPage( 1, 100 );
    m_millisecs.setOverflowField( m_secs );
    m_millisecs.setValue( 0 );
    m_millisecs.addListener( ( sender, msg ) -> signalTime() );

    setSpacing( BORDER - 1 );
    getChildren().addAll( m_hours, m_mins, m_secs, m_millisecs );
  }

  /******************************************* getTime *******************************************/
  public Time getTime()
  {
    // get time from spin fields
    return new Time( m_hours.getInteger(), m_mins.getInteger(), m_secs.getInteger(), m_millisecs.getInteger() );
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

}