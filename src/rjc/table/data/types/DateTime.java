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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import rjc.table.Utils;

/*************************************************************************************************/
/******************************** Date-time (with no time-zone) **********************************/
/*************************************************************************************************/

public class DateTime implements Comparable<DateTime>, Serializable
{
  private static final long    serialVersionUID    = Utils.VERSION.hashCode();

  // milliseconds from 00:00:00.000 start of epoch-day
  private long                 m_milliseconds;

  // milliseconds in day
  public static final long     MILLISECONDS_IN_DAY = Time.MILLISECONDS_IN_DAY;

  // range constrained by valid Date (approx 5,800,000 BC to 5,800,000 AD)
  public static final DateTime MIN_VALUE           = new DateTime( Date.MIN_VALUE, Time.MAX_VALUE );
  public static final DateTime MAX_VALUE           = new DateTime( Date.MAX_VALUE, Time.MIN_VALUE );

  public enum Interval
  {
    YEAR, HALFYEAR, QUARTERYEAR, MONTH, WEEK, DAY
  }

  private static final char   QUOTE = '\'';
  private static final char   CHARB = 'B';
  private static final String CODE  = "#@B!";

  /***************************************** constructor *****************************************/
  public DateTime( long ms )
  {
    // constructor
    m_milliseconds = ms;
  }

  /***************************************** constructor *****************************************/
  public DateTime( DateTime dt )
  {
    // constructor
    m_milliseconds = dt.m_milliseconds;
  }

  /***************************************** constructor *****************************************/
  public DateTime( String str )
  {
    // constructor, date must be split from time by a space
    int split = str.indexOf( 'T' );
    Date date = Date.fromString( str.substring( 0, split ) );
    Time time = Time.fromString( str.substring( split + 1, str.length() ) );
    m_milliseconds = date.getEpochday() * MILLISECONDS_IN_DAY + time.getDayMilliseconds();
  }

  /***************************************** constructor *****************************************/
  public DateTime( Date date, Time time )
  {
    // constructor
    m_milliseconds = date.getEpochday() * MILLISECONDS_IN_DAY + time.getDayMilliseconds();
  }

  /***************************************** constructor *****************************************/
  public DateTime( LocalDateTime dt )
  {
    // constructor
    Date date = new Date( dt.toLocalDate() );
    Time time = new Time( dt.toLocalTime() );
    m_milliseconds = date.getEpochday() * MILLISECONDS_IN_DAY + time.getDayMilliseconds();
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // convert to string to "YYYY-MM-DDThh:mm:ss.mmm" format
    return getDate().toString() + "T" + getTime().toString();
  }

  /****************************************** toString *******************************************/
  public String toString( String format )
  {
    // convert to string in specified format
    long secs = m_milliseconds / 1000L;
    int nanos = (int) ( m_milliseconds % 1000L * 1000000L );
    if ( nanos < 0 )
    {
      nanos += 1000000000;
      secs--;
    }
    LocalDateTime ldt = LocalDateTime.ofEpochSecond( secs, nanos, ZoneOffset.UTC );

    // to support half-of-year using Bs, quote any unquoted Bs in format
    StringBuilder newFormat = new StringBuilder();
    boolean inQuote = false;
    boolean inB = false;
    char here;
    for ( int i = 0; i < format.length(); i++ )
    {
      here = format.charAt( i );

      // are we in quoted text?
      if ( here == QUOTE )
        inQuote = !inQuote;

      // replace unquoted Bs with special code
      if ( inB && here == CHARB )
      {
        newFormat.append( CODE );
        continue;
      }

      // come to end of unquoted Bs
      if ( inB && here != CHARB )
      {
        newFormat.append( QUOTE );
        inB = false;
        inQuote = false;
      }

      // start of unquoted Bs, start quote with special code
      if ( !inQuote && here == CHARB )
      {
        // avoid creating double quotes
        if ( newFormat.length() > 0 && newFormat.charAt( newFormat.length() - 1 ) == QUOTE )
        {
          newFormat.deleteCharAt( newFormat.length() - 1 );
          newFormat.append( CODE );
        }
        else
          newFormat.append( "'" + CODE );
        inQuote = true;
        inB = true;
      }
      else
      {
        newFormat.append( here );
      }
    }

    // close quote if quote still open
    if ( inQuote )
      newFormat.append( QUOTE );

    String str = ldt.format( DateTimeFormatter.ofPattern( newFormat.toString() ) );

    // no special code so can return string immediately
    if ( !str.contains( CODE ) )
      return str;

    // determine half-of-year
    String yearHalf;
    if ( getDate().getMonth() < 7 )
      yearHalf = "1";
    else
      yearHalf = "2";

    // four or more Bs is not allowed
    String Bs = CODE + CODE + CODE + CODE;
    if ( str.contains( Bs ) )
      throw new IllegalArgumentException( "Too many pattern letters: B" );

    // replace three Bs
    Bs = CODE + CODE + CODE;
    if ( yearHalf.equals( "1" ) )
      str = str.replace( Bs, yearHalf + "st half" );
    else
      str = str.replace( Bs, yearHalf + "nd half" );

    // replace two Bs
    Bs = CODE + CODE;
    str = str.replace( Bs, "H" + yearHalf );

    // replace one Bs
    Bs = CODE;
    str = str.replace( Bs, yearHalf );

    return str;
  }

  /******************************************* getDate *******************************************/
  public Date getDate()
  {
    if ( m_milliseconds < 0 )
      return new Date( (int) ( m_milliseconds / MILLISECONDS_IN_DAY ) - 1 );

    return new Date( (int) ( m_milliseconds / MILLISECONDS_IN_DAY ) );
  }

  /******************************************* getTime *******************************************/
  public Time getTime()
  {
    int ms = (int) ( m_milliseconds % MILLISECONDS_IN_DAY );
    if ( ms < 0 )
      ms += MILLISECONDS_IN_DAY;

    return Time.fromMilliseconds( ms );
  }

  /********************************************* now *********************************************/
  public static DateTime now()
  {
    // return a new DateTime from current system clock
    return new DateTime( System.currentTimeMillis() + Time.TZ_MS_OFFSET );
  }

  /************************************** plusMilliseconds ***************************************/
  public DateTime plusMilliseconds( long ms )
  {
    return new DateTime( m_milliseconds + ms );
  }

  /************************************** addMilliseconds ***************************************/
  public void addMilliseconds( long ms )
  {
    m_milliseconds += ms;
  }

  /*************************************** getMilliseconds ***************************************/
  public long getMilliseconds()
  {
    return m_milliseconds;
  }

  /**************************************** getTruncated *****************************************/
  public DateTime getTruncated( Interval interval )
  {
    // return new date-time truncated down to specified interval
    if ( interval == Interval.YEAR )
    {
      Date date = new Date( getDate().getYear(), 1, 1 );
      return new DateTime( date.getEpochday() * MILLISECONDS_IN_DAY );
    }

    if ( interval == Interval.HALFYEAR )
    {
      Date date = getDate();
      int month = ( ( date.getMonth() - 1 ) / 6 ) * 6 + 1;

      Date hy = new Date( date.getYear(), month, 1 );
      return new DateTime( hy.getEpochday() * MILLISECONDS_IN_DAY );
    }

    if ( interval == Interval.QUARTERYEAR )
    {
      Date date = getDate();
      int month = ( ( date.getMonth() - 1 ) / 3 ) * 3 + 1;

      Date qy = new Date( date.getYear(), month, 1 );
      return new DateTime( qy.getEpochday() * MILLISECONDS_IN_DAY );
    }

    if ( interval == Interval.MONTH )
    {
      Date date = getDate();
      Date md = new Date( date.getYear(), date.getMonth(), 1 );
      return new DateTime( md.getEpochday() * MILLISECONDS_IN_DAY );
    }

    if ( interval == Interval.WEEK )
    {
      int day = (int) ( m_milliseconds / MILLISECONDS_IN_DAY );
      int dayOfWeek = ( day + 3 ) % 7;
      return new DateTime( ( day - dayOfWeek ) * MILLISECONDS_IN_DAY );
    }

    if ( interval == Interval.DAY )
    {
      long ms = ( m_milliseconds / MILLISECONDS_IN_DAY ) * MILLISECONDS_IN_DAY;
      return new DateTime( ms );
    }

    throw new IllegalArgumentException( "interval=" + interval );
  }

  /****************************************** plusDays *******************************************/
  public DateTime plusDays( int days )
  {
    // return new date-time specified days added or subtracted
    return new DateTime( m_milliseconds + days * MILLISECONDS_IN_DAY );
  }

  /***************************************** plusMonths ******************************************/
  public DateTime plusMonths( int months )
  {
    // return new date-time specified months added or subtracted
    return new DateTime( getDate().plusMonths( months ), getTime() );
  }

  /***************************************** plusYears *******************************************/
  public DateTime plusYears( int years )
  {
    // return new date-time specified months added or subtracted
    return new DateTime( getDate().plusYears( years ), getTime() );
  }

  /**************************************** plusInterval *****************************************/
  public DateTime plusInterval( Interval interval )
  {
    // add one specified interval to date-time
    if ( interval == Interval.YEAR )
      return plusYears( 1 );

    if ( interval == Interval.HALFYEAR )
      return plusMonths( 6 );

    if ( interval == Interval.QUARTERYEAR )
      return plusMonths( 3 );

    if ( interval == Interval.MONTH )
      return plusMonths( 1 );

    if ( interval == Interval.WEEK )
      return plusDays( 7 );

    if ( interval == Interval.DAY )
      return plusDays( 1 );

    throw new IllegalArgumentException( "interval=" + interval );
  }

  /****************************************** isLessThan *****************************************/
  public boolean isLessThan( DateTime other )
  {
    return m_milliseconds < other.m_milliseconds;
  }

  /******************************************* equals ********************************************/
  @Override
  public boolean equals( Object other )
  {
    // return true if other object represents same date-time
    if ( other != null && other instanceof DateTime )
      return m_milliseconds == ( (DateTime) other ).m_milliseconds;

    return false;
  }

  /****************************************** hashCode ******************************************/
  @Override
  public int hashCode()
  {
    // date-time hash code is based on the millisecond value
    return (int) ( m_milliseconds ^ ( m_milliseconds >>> 32 ) );
  }

  /****************************************** compareTo ******************************************/
  @Override
  public int compareTo( DateTime other )
  {
    long sign = m_milliseconds - other.m_milliseconds;
    if ( sign > 0 )
      return 1;
    if ( sign < 0 )
      return -1;
    return 0;
  }

  /******************************************** parse ********************************************/
  public static DateTime parse( String text, String format )
  {
    // return date-time if text can be parsed, otherwise return null
    DateTime datetime = tryFormat( text, format );
    if ( datetime != null )
      return datetime;

    // try date format
    Date date = Date.parse( text, format );
    if ( date != null )
      return new DateTime( date, Time.MIN_VALUE );

    // try "d/M/yy H:m:s.S" and "d/M/y H:m:s.S"
    datetime = tryDefaultFormat( text );
    if ( datetime != null )
      return datetime;

    // add milliseconds
    datetime = tryDefaultFormat( text + "0" );
    if ( datetime != null )
      return datetime;
    datetime = tryDefaultFormat( text + ".0" );
    if ( datetime != null )
      return datetime;

    // add seconds + milliseconds
    datetime = tryDefaultFormat( text + "0.0" );
    if ( datetime != null )
      return datetime;
    datetime = tryDefaultFormat( text + ":0.0" );
    if ( datetime != null )
      return datetime;

    // add minutes + seconds + milliseconds
    datetime = tryDefaultFormat( text + "0:0.0" );
    if ( datetime != null )
      return datetime;
    datetime = tryDefaultFormat( text + ":0:0.0" );
    if ( datetime != null )
      return datetime;

    // add hours + minutes + seconds + milliseconds
    datetime = tryDefaultFormat( text + "0:0:0.0" );
    if ( datetime != null )
      return datetime;
    return tryDefaultFormat( text + " 0:0:0.0" );
  }

  /************************************** tryDefaultFormat ***************************************/
  private static DateTime tryDefaultFormat( String text )
  {
    // try "d/M/yy H:m:s.S" and "d/M/y H:m:s.S"
    DateTime dt = tryFormat( text, "d/M/yy H:m:s.S" );
    if ( dt != null )
      return dt;
    return tryFormat( text, "d/M/y H:m:s.S" );
  }

  /****************************************** tryFormat ******************************************/
  private static DateTime tryFormat( String text, String format )
  {
    // return date-time if text can be parsed, otherwise return null
    try
    {
      LocalDateTime ldt = LocalDateTime.parse( text, DateTimeFormatter.ofPattern( format ) );
      return new DateTime( ldt );
    }
    catch ( Exception exception )
    {
      return null;
    }
  }

  /************************************** endOfDayMidnight ***************************************/
  public DateTime endOfDayMidnight()
  {
    // return new date-time with new time advanced to next midnight
    long day;

    if ( m_milliseconds < 0 )
      day = m_milliseconds / MILLISECONDS_IN_DAY;
    else
      day = m_milliseconds / MILLISECONDS_IN_DAY + 1;

    return new DateTime( day * MILLISECONDS_IN_DAY );
  }

}