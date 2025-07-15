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

package rjc.table.data.types;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import rjc.table.Utils;

/*************************************************************************************************/
/******************** Time of day from 00:00:00.000 to 24:00:00.000 inclusive ********************/
/*************************************************************************************************/

public class Time implements Serializable
{
  private static final long serialVersionUID    = Utils.VERSION.hashCode();

  // milliseconds from 00:00:00.000 start of day
  private int               m_milliseconds;

  // anything between MIN_VALUE and MAX_VALUE inclusive is valid, anything else invalid
  public static final int   ONE_SECOND          = 1000;
  public static final int   ONE_MINUTE          = 60 * ONE_SECOND;
  public static final int   ONE_HOUR            = 60 * ONE_MINUTE;
  public static final int   MILLISECONDS_IN_DAY = 24 * ONE_HOUR;
  public static final Time  MIN_VALUE           = Time.fromMilliseconds( 0 );
  public static final Time  MAX_VALUE           = Time.fromMilliseconds( MILLISECONDS_IN_DAY );
  public static final int   TZ_MS_OFFSET        = OffsetDateTime.now().getOffset().getTotalSeconds() * 1000;

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
    // check input is not null or blank
    if ( str == null )
      throw new NullPointerException();
    str = Utils.clean( str );
    if ( str.isBlank() )
      throw new IllegalArgumentException( "Time string must not be null or empty" );

    // if simple integer, treats as hours or hours+minutes etc depending on length
    try
    {
      int num = Integer.valueOf( str );
      if ( num < 100 )
        return new Time( num, 0, 0, 0 );
      if ( num < 10000 )
        return new Time( num / 100, num % 100, 0, 0 );
      return new Time( num / 10000, ( num / 100 ) % 100, num % 100, 0 );
    }
    catch ( NumberFormatException exception )
    {
    }

    // check if 24:00:00.000 time
    try
    {
      if ( str.matches( "^24[\\:\\s\\.]?(?:0{1,2})?[\\:\\s\\.]?(?:0{1,2})?.*$" ) )
        return new Time( MILLISECONDS_IN_DAY );
    }
    catch ( Exception exception )
    {
    }

    // otherwise try
    return new Time( LocalTime.parse( str,
        DateTimeFormatter.ofPattern( "H[:][.][ ][-][m][:][.][ ][-][s][:][.][ ][-][SSS][SS][S]" ) ) );
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
    return toString( 4 );
  }

  /**************************************** toStringShort ****************************************/
  public String toStringShort()
  {
    // convert to string to "hh:mm" format
    return toString( 2 );
  }

  /****************************************** toString *******************************************/
  public String toString( int fieldCount )
  {
    // return time as string in "hh:mm:ss.mmm" or shorter format depending on field-count value
    if ( fieldCount < 1 )
      throw new IllegalArgumentException( "Field count must be greater than 1 (" + fieldCount + ")" );
    StringBuilder sb = new StringBuilder( 12 );

    // hours component
    int hour = m_milliseconds / ONE_HOUR;
    if ( hour < 10 )
      sb.append( '0' );
    sb.append( hour );
    if ( fieldCount == 1 )
      return sb.toString();

    // minutes component
    int minute = m_milliseconds / ONE_MINUTE % 60;
    sb.append( ':' );
    if ( minute < 10 )
      sb.append( '0' );
    sb.append( minute );
    if ( fieldCount == 2 )
      return sb.toString();

    // seconds component
    int second = m_milliseconds / ONE_SECOND % 60;
    sb.append( ':' );
    if ( second < 10 )
      sb.append( '0' );
    sb.append( second );
    if ( fieldCount == 3 )
      return sb.toString();

    // milliseconds component
    int milli = m_milliseconds % ONE_SECOND;
    sb.append( '.' );
    if ( milli < 100 )
      sb.append( '0' );
    if ( milli < 10 )
      sb.append( '0' );
    sb.append( milli );

    // return full "hh:mm:ss.mmm" format when field-count 4 or greater
    return sb.toString();

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
    if ( other != null && other instanceof Time time )
      return m_milliseconds == time.m_milliseconds;

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