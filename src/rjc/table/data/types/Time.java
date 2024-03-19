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

package rjc.table.data.types;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import rjc.table.Utils;

/*************************************************************************************************/
/******************** Time of day from 00:00:00.000 to 24:00:00.000 inclusive ********************/
/*************************************************************************************************/

public class Time implements Serializable
{
  private static final long          serialVersionUID    = Utils.VERSION.hashCode();

  // milliseconds from 00:00:00.000 start of day
  private int                        m_milliseconds;

  // anything between MIN_VALUE and MAX_VALUE inclusive is valid, anything else invalid
  public static final int            MILLISECONDS_IN_DAY = 24 * 3600 * 1000;
  public static final int            ONE_SECOND          = 1000;
  public static final int            ONE_MINUTE          = 60 * 1000;
  public static final int            ONE_HOUR            = 3600 * 1000;
  public static final Time           MIN_VALUE           = Time.fromMilliseconds( 0 );
  public static final Time           MAX_VALUE           = Time.fromMilliseconds( MILLISECONDS_IN_DAY );
  public static final int            TZ_MS_OFFSET        = OffsetDateTime.now().getOffset().getTotalSeconds() * 1000;

  private static final StringBuilder BUFFER              = new StringBuilder();

  /* ======================================= constructor ======================================= */
  private Time( int milliseconds )
  {
    // constructor (from pre-validated milliseconds) hence *PRIVATE*
    m_milliseconds = milliseconds;
  }

  /**************************************** constructor ******************************************/
  public Time( LocalTime localTime )
  {
    // return a new Time from a java.time.LocalTime
    m_milliseconds = (int) ( localTime.toNanoOfDay() / 1_000_000L );
  }

  /**************************************** constructor ******************************************/
  public Time( int hours, int mins, int secs, int ms )
  {
    // valid inputs
    if ( hours < 0 || hours > 24 )
      throw new IllegalArgumentException( "hours=" + hours );

    if ( mins < 0 || mins > 59 )
      throw new IllegalArgumentException( "minutes=" + mins );

    if ( secs < 0 || secs > 59 )
      throw new IllegalArgumentException( "seconds=" + secs );

    if ( ms < 0 || ms > 999 )
      throw new IllegalArgumentException( "milliseconds=" + ms );

    if ( hours == 24 && ( mins > 0 || secs > 0 || ms > 0 ) )
      throw new IllegalArgumentException( "time beyond 24H" );

    m_milliseconds = hours * ONE_HOUR + mins * ONE_MINUTE + secs * ONE_SECOND + ms;
  }

  /************************************* getDayMilliseconds **************************************/
  public int getDayMilliseconds()
  {
    // return number of milliseconds from start of day
    return m_milliseconds;
  }

  /***************************************** fromString ******************************************/
  public static Time fromString( String str )
  {
    // if simple integer, treats as hours or hours+minutes depending on length
    try
    {
      int num = Integer.valueOf( str );
      if ( num < 100 )
        return new Time( num, 0, 0, 0 );
      else
        return new Time( num / 100, num % 100, 0, 0 );
    }
    catch ( Exception exception )
    {
    }

    // split the time hours:mins:secs by colon separator
    String[] parts = str.split( ":" );
    if ( parts.length < 2 )
      throw new IllegalArgumentException( "str=" + str );

    // hours & minutes parts must be integers
    int hours = Integer.parseInt( parts[0] );
    int mins = Integer.parseInt( parts[1] );
    if ( parts.length == 2 )
      return new Time( hours, mins, 0, 0 );

    // split seconds into integer and milliseconds sections
    String[] seconds = parts[2].split( "\\." );
    int secs = Integer.parseInt( seconds[0] );
    if ( seconds.length == 1 )
      return new Time( hours, mins, secs, 0 );

    // ensure we look at first three digits only for milliseconds
    String milli = ( seconds[1] + "00" ).substring( 0, 3 );
    int ms = Integer.parseInt( milli );
    return new Time( hours, mins, secs, ms );
  }

  /****************************************** fromHours ******************************************/
  public static Time fromHours( double hours )
  {
    // return a Time from double hours
    if ( hours < 0.0 || hours > 24.0 )
      throw new IllegalArgumentException( "hours=" + hours );

    return new Time( (int) Math.round( hours * ONE_HOUR ) );
  }

  /************************************** fromMilliseconds ***************************************/
  public static Time fromMilliseconds( int milliseconds )
  {
    // return a Time from int milliseconds
    if ( milliseconds < 0 || milliseconds > MILLISECONDS_IN_DAY )
      throw new IllegalArgumentException( "milliseconds=" + milliseconds );

    return new Time( milliseconds );
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // convert to string to "hh:mm:ss.mmm" format
    BUFFER.setLength( 0 );
    int hour = getHours();
    int minute = getMinutes();
    int second = getSeconds();
    int milli = m_milliseconds % ONE_SECOND;

    if ( hour < 10 )
      BUFFER.append( '0' );
    BUFFER.append( hour );
    BUFFER.append( ':' );
    if ( minute < 10 )
      BUFFER.append( '0' );
    BUFFER.append( minute );
    BUFFER.append( ':' );
    if ( second < 10 )
      BUFFER.append( '0' );
    BUFFER.append( second );
    BUFFER.append( '.' );
    if ( milli < 100 )
      BUFFER.append( '0' );
    if ( milli < 10 )
      BUFFER.append( '0' );
    BUFFER.append( milli );

    return BUFFER.toString();
  }

  /****************************************** toString *******************************************/
  public String toStringShort()
  {
    // convert to string to "hh:mm" format
    BUFFER.setLength( 0 );
    int hour = getHours();
    int minute = getMinutes();

    if ( hour < 10 )
      BUFFER.append( '0' );
    BUFFER.append( hour );
    BUFFER.append( ':' );
    if ( minute < 10 )
      BUFFER.append( '0' );
    BUFFER.append( minute );

    return BUFFER.toString();
  }

  /********************************************* now *********************************************/
  public static Time now()
  {
    // return a new Time from current system clock
    return new Time( (int) ( ( System.currentTimeMillis() + TZ_MS_OFFSET ) % MILLISECONDS_IN_DAY ) );
  }

  /****************************************** getHours *******************************************/
  public int getHours()
  {
    // return hours (0 to 24 inclusive)
    return m_milliseconds / ONE_HOUR;
  }

  /***************************************** getMinutes ******************************************/
  public int getMinutes()
  {
    // return minutes (0 to 59 inclusive)
    return m_milliseconds / ONE_MINUTE % 60;
  }

  /***************************************** getSeconds ******************************************/
  public int getSeconds()
  {
    // return seconds (0 to 59 inclusive)
    return m_milliseconds / ONE_SECOND % 60;
  }

  /*************************************** getMilliseconds ***************************************/
  public int getMilliseconds()
  {
    // return milliseconds fraction of seconds (0 to 999 inclusive)
    return m_milliseconds % ONE_SECOND;
  }

  /******************************************* equals ********************************************/
  @Override
  public boolean equals( Object other )
  {
    // return true if other object represents same time
    if ( other != null && other instanceof Time )
      return m_milliseconds == ( (Time) other ).m_milliseconds;

    return false;
  }

  /****************************************** hashCode ******************************************/
  @Override
  public int hashCode()
  {
    // time hash code is simply the day milliseconds
    return m_milliseconds;
  }

  /*************************************** addMilliseconds ***************************************/
  public void addMilliseconds( int ms )
  {
    // add milliseconds to this time
    m_milliseconds += ms;
    if ( m_milliseconds < 0 )
      m_milliseconds = m_milliseconds % MILLISECONDS_IN_DAY + MILLISECONDS_IN_DAY;
    if ( m_milliseconds > MILLISECONDS_IN_DAY )
      m_milliseconds = m_milliseconds % MILLISECONDS_IN_DAY;
  }
}