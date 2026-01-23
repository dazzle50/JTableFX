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

package rjc.table.data.types;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/*************************************************************************************************/
/******************** Time of day from 00:00:00.000 to 24:00:00.000 inclusive ********************/
/*************************************************************************************************/

/**
 * Immutable representation of time-of-day from 00:00:00.000 to 24:00:00.000 inclusive.
 *
 * <p>This class represents a time within a single day, stored internally as milliseconds since
 * midnight start of day. Unlike {@link LocalTime}, this class supports the boundary case of
 * 24:00:00.000 to represent the end of day (equivalent to 00:00:00.000 of next day).
 *
 * <p>All instances are immutable and thread-safe. Arithmetic operations return new instances.
 */
public final class Time implements Serializable, Comparable<Time>
{
  private static final long serialVersionUID  = 1L;

  // time constants
  public static final int   MILLIS_PER_SECOND = 1000;
  public static final int   MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
  public static final int   MILLIS_PER_HOUR   = 60 * MILLIS_PER_MINUTE;
  public static final int   MILLIS_PER_DAY    = 24 * MILLIS_PER_HOUR;

  // boundary values
  public static final Time  MIN_VALUE         = new Time( 0 );                    // 00:00:00.000
  public static final Time  NOON              = new Time( 12 * MILLIS_PER_HOUR ); // 12:00:00.000 (noon)
  public static final Time  MAX_VALUE         = new Time( MILLIS_PER_DAY );       // 24:00:00.000 (end of day)

  // internal state - milliseconds since midnight 00:00:00.000 (0 to MILLIS_PER_DAY inclusive)
  private final int         m_milliseconds;

  /***************************************** constructor *****************************************/
  /**
   * Private constructor - use factory methods instead.
   *
   * @param milliseconds milliseconds since midnight (0 to MILLIS_PER_DAY inclusive)
   */
  private Time( int milliseconds )
  {
    // store the milliseconds value
    m_milliseconds = milliseconds;
  }

  /********************************************* of **********************************************/
  /**
   * Creates a Time from hours, minutes, seconds and milliseconds.
   *
   * @param hours        the hour component (0-24)
   * @param minutes      the minute component (0-59)
   * @param seconds      the second component (0-59)
   * @param millis       the millisecond component (0-999)
   * @return a new Time instance
   * @throws IllegalArgumentException if any component is out of range
   */
  public static Time of( int hours, int minutes, int seconds, int millis )
  {
    // validate all components are within range
    validateTimeComponents( hours, minutes, seconds, millis );

    // calculate total milliseconds from individual components
    int totalMillis = hours * MILLIS_PER_HOUR + minutes * MILLIS_PER_MINUTE + seconds * MILLIS_PER_SECOND + millis;
    return new Time( totalMillis );
  }

  /********************************************* of **********************************************/
  /**
   * Creates a Time from hours, minutes and seconds (milliseconds default to 0).
   *
   * @param hours        the hour component (0-24)
   * @param minutes      the minute component (0-59)
   * @param seconds      the second component (0-59)
   * @return a new Time instance
   * @throws IllegalArgumentException if any component is out of range
   */
  public static Time of( int hours, int minutes, int seconds )
  {
    // delegate to full constructor with zero milliseconds
    return of( hours, minutes, seconds, 0 );
  }

  /********************************************* of **********************************************/
  /**
   * Creates a Time from hours and minutes (seconds and milliseconds default to 0).
   *
   * @param hours        the hour component (0-24)
   * @param minutes      the minute component (0-59)
   * @return a new Time instance
   * @throws IllegalArgumentException if any component is out of range
   */
  public static Time of( int hours, int minutes )
  {
    // delegate to full constructor with zero seconds and milliseconds
    return of( hours, minutes, 0, 0 );
  }

  /*************************************** ofMilliseconds ****************************************/
  /**
   * Creates a Time from milliseconds since midnight start of day.
   *
   * @param milliseconds milliseconds since midnight (0 to MILLIS_PER_DAY inclusive)
   * @return a new Time instance
   * @throws IllegalArgumentException if milliseconds is out of valid range
   */
  public static Time ofMilliseconds( int milliseconds )
  {
    // validate milliseconds is within valid day range
    if ( milliseconds < 0 || milliseconds > MILLIS_PER_DAY )
      throw new IllegalArgumentException(
          "Milliseconds must be between 0 and " + MILLIS_PER_DAY + ", got: " + milliseconds );
    return new Time( milliseconds );
  }

  /******************************************* ofHours *******************************************/
  /**
   * Creates a Time from fractional hours.
   *
   * @param hours        fractional hours (0.0 to 24.0 inclusive)
   * @return a new Time instance
   * @throws IllegalArgumentException if hours is out of valid range
   */
  public static Time ofHours( double hours )
  {
    // validate hours is within valid range
    if ( hours < 0.0 || hours > 24.0 )
      throw new IllegalArgumentException( "Hours must be between 0.0 and 24.0, got: " + hours );

    // convert fractional hours to milliseconds with rounding
    return new Time( (int) Math.round( hours * MILLIS_PER_HOUR ) );
  }

  /********************************************* of **********************************************/
  /**
   * Creates a Time from a LocalTime.
   *
   * @param localTime    the LocalTime to convert
   * @return a new Time instance
   * @throws NullPointerException if localTime is null
   */
  public static Time of( LocalTime localTime )
  {
    // ensure localTime is not null
    Objects.requireNonNull( localTime, "LocalTime cannot be null" );

    // convert nanoseconds of day to milliseconds
    return new Time( (int) ( localTime.toNanoOfDay() / 1_000_000L ) );
  }

  /********************************************* now *********************************************/
  /**
   * Creates a Time representing the current system time.
   *
   * @return a new Time representing the current time of day
   */
  public static Time now()
  {
    // get current system time and convert to time representation
    return of( LocalTime.now() );
  }

  /******************************************** parse ********************************************/
  /**
   * Parses a time string into a Time instance.
   *
   * Delegates parsing logic to {@link TimeParser} for intelligent interpretation of 12h/24h,
   * compact, and natural language formats.
   *
   * @param timeString   the string to parse
   * @return a new Time instance
   * @throws IllegalArgumentException if the string cannot be parsed
   * @throws NullPointerException if timeString is null
   */
  public static Time parse( String timeString )
  {
    try
    {
      // delegate to intelligent parser for flexible interpretation
      return TimeParser.parse( timeString );
    }
    catch ( DateTimeParseException e )
    {
      throw new IllegalArgumentException( "Cannot parse time string: '" + timeString + "'", e );
    }
  }

  /******************************************* getHour *******************************************/
  /**
   * Gets the hours component of this time (0-24).
   *
   * @return the hour component
   */
  public int getHour()
  {
    // extract hours by dividing total milliseconds by milliseconds per hour
    return m_milliseconds / MILLIS_PER_HOUR;
  }

  /****************************************** getMinute ******************************************/
  /**
   * Gets the minutes component of this time (0-59).
   *
   * @return the minute component
   */
  public int getMinute()
  {
    // extract minutes within the current hour
    return ( m_milliseconds / MILLIS_PER_MINUTE ) % 60;
  }

  /****************************************** getSecond ******************************************/
  /**
   * Gets the seconds component of this time (0-59).
   *
   * @return the second component
   */
  public int getSecond()
  {
    // extract seconds within the current minute
    return ( m_milliseconds / MILLIS_PER_SECOND ) % 60;
  }

  /*************************************** getMillisecond ****************************************/
  /**
   * Gets the milliseconds component of this time (0-999).
   *
   * @return the millisecond component
   */
  public int getMillisecond()
  {
    // extract milliseconds within the current second
    return m_milliseconds % MILLIS_PER_SECOND;
  }

  /************************************* toMillisecondsOfDay *************************************/
  /**
   * Gets the total milliseconds since midnight for this time.
   *
   * @return milliseconds since midnight (0 to MILLIS_PER_DAY inclusive)
   */
  public int toMillisecondsOfDay()
  {
    // return the internal milliseconds representation
    return m_milliseconds;
  }

  /******************************************* toHours *******************************************/
  /**
   * Converts this time to fractional hours since midnight.
   *
   * @return fractional hours (0.0 to 24.0)
   */
  public double toHours()
  {
    // convert milliseconds to fractional hours
    return (double) m_milliseconds / MILLIS_PER_HOUR;
  }

  /***************************************** toLocalTime *****************************************/
  /**
   * Converts this time to a LocalTime instance (24:00:00 becomes 00:00:00).
   *
   * @return equivalent LocalTime
   */
  public LocalTime toLocalTime()
  {
    // handle special case of end-of-day (24:00:00 -> 00:00:00)
    if ( m_milliseconds == MILLIS_PER_DAY )
      return LocalTime.MIDNIGHT;
    return LocalTime.ofNanoOfDay( m_milliseconds * 1_000_000L );
  }

  // ================================ Arithmetic Methods ================================

  /************************************** plusMilliseconds ***************************************/
  /**
   * Returns a copy of this time with the specified milliseconds added.
   * Values wrap around day boundaries (e.g., adding to 23:59:59.999 wraps to next day).
   *
   * @param millisToAdd  the milliseconds to add (can be negative)
   * @return a new Time instance with milliseconds added
   */
  public Time plusMilliseconds( int millisToAdd )
  {
    // calculate new milliseconds with wrapping at day boundary
    int newMillis = ( m_milliseconds + millisToAdd ) % MILLIS_PER_DAY;

    // handle negative results by adding a full day
    if ( newMillis < 0 )
      newMillis += MILLIS_PER_DAY;
    return new Time( newMillis );
  }

  /***************************************** plusSeconds *****************************************/
  /**
   * Returns a copy of this time with the specified seconds added.
   *
   * @param secondsToAdd the seconds to add (can be negative)
   * @return a new Time instance with seconds added
   */
  public Time plusSeconds( int secondsToAdd )
  {
    // convert seconds to milliseconds and delegate
    return plusMilliseconds( secondsToAdd * MILLIS_PER_SECOND );
  }

  /***************************************** plusMinutes *****************************************/
  /**
   * Returns a copy of this time with the specified minutes added.
   *
   * @param minutesToAdd the minutes to add (can be negative)
   * @return a new Time instance with minutes added
   */
  public Time plusMinutes( int minutesToAdd )
  {
    // convert minutes to milliseconds and delegate
    return plusMilliseconds( minutesToAdd * MILLIS_PER_MINUTE );
  }

  /****************************************** plusHours ******************************************/
  /**
   * Returns a copy of this time with the specified hours added.
   *
   * @param hoursToAdd   the hours to add (can be negative)
   * @return a new Time instance with hours added
   */
  public Time plusHours( int hoursToAdd )
  {
    // convert hours to milliseconds and delegate
    return plusMilliseconds( hoursToAdd * MILLIS_PER_HOUR );
  }

  // ================================ Comparison Methods ================================

  /****************************************** isBefore *******************************************/
  /**
   * Checks if this time is before the specified time.
   *
   * @param other        the time to compare with
   * @return true if this time is before the other time
   * @throws NullPointerException if other is null
   */
  public boolean isBefore( Time other )
  {
    // ensure other is not null then compare milliseconds
    Objects.requireNonNull( other );
    return m_milliseconds < other.m_milliseconds;
  }

  /******************************************* isAfter *******************************************/
  /**
   * Checks if this time is after the specified time.
   *
   * @param other        the time to compare with
   * @return true if this time is after the other time
   * @throws NullPointerException if other is null
   */
  public boolean isAfter( Time other )
  {
    // ensure other is not null then compare milliseconds
    Objects.requireNonNull( other );
    return m_milliseconds > other.m_milliseconds;
  }

  /****************************************** compareTo ******************************************/
  /**
   * Compares this time with another time for ordering.
   *
   * @param other        the time to compare with
   * @return negative if this time is earlier, positive if later, zero if equal
   * @throws NullPointerException if other is null
   */
  @Override
  public int compareTo( Time other )
  {
    // ensure other is not null
    Objects.requireNonNull( other, "Other time cannot be null" );

    // compare milliseconds and return appropriate ordering result
    return m_milliseconds < other.m_milliseconds ? -1 : m_milliseconds > other.m_milliseconds ? 1 : 0;
  }

  /************************************* differenceInMillis **************************************/
  /**
   * Returns the absolute difference in milliseconds between this time and another.
   *
   * @param other        the time to compare with
   * @return absolute difference in milliseconds
   * @throws NullPointerException if other is null
   */
  public int differenceInMillis( Time other )
  {
    // ensure other is not null then calculate absolute difference
    Objects.requireNonNull( other );
    return Math.abs( m_milliseconds - other.m_milliseconds );
  }

  // ================================ Formatting Methods ================================

  /****************************************** toString *******************************************/
  /**
   * Returns this time formatted as "HH:MM:SS.mmm".
   *
   * @return string representation of this time
   */
  @Override
  public String toString()
  {
    // format with all 4 components (hour, minute, second, millisecond)
    return formatTime( 4 );
  }

  /**************************************** toShortString ****************************************/
  /**
   * Returns this time formatted as "HH:MM".
   *
   * @return short string representation showing only hours and minutes
   */
  public String toShortString()
  {
    // format with only 2 components (hour, minute)
    return formatTime( 2 );
  }

  /******************************************* format ********************************************/
  /**
   * Returns this time formatted with specified number of components.
   *
   * @param components   number of components to include (1=HH, 2=HH:MM, 3=HH:MM:SS, 4+=HH:MM:SS.mmm)
   * @return formatted time string
   * @throws IllegalArgumentException if components is less than 1
   */
  public String format( int components )
  {
    // validate components is at least 1
    if ( components < 1 )
      throw new IllegalArgumentException( "Components must be >= 1" );

    // delegate to internal formatting method
    return formatTime( components );
  }

  // ================================ Object Methods ================================

  /******************************************* equals ********************************************/
  /**
   * Checks if this time is equal to another object.
   *
   * @param obj          the object to compare with
   * @return true if the objects represent the same time
   */
  @Override
  public boolean equals( Object obj )
  {
    // check for same reference
    if ( this == obj )
      return true;

    // check type and compare milliseconds
    if ( obj instanceof Time other )
      return m_milliseconds == other.m_milliseconds;
    return false;
  }

  /****************************************** hashCode *******************************************/
  /**
   * Returns a hash code for this time.
   *
   * @return hash code based on the milliseconds value
   */
  @Override
  public int hashCode()
  {
    // use milliseconds value for hash code
    return Integer.hashCode( m_milliseconds );
  }

  // ================================ Private Helper Methods ================================

  /********************************** validateTimeComponents *************************************/
  // validates that time components are within acceptable ranges
  private static void validateTimeComponents( int hours, int minutes, int seconds, int millis )
  {
    // validate hours is 0-24
    if ( hours < 0 || hours > 24 )
      throw new IllegalArgumentException( "Hours must be 0-24, got: " + hours );

    // validate minutes is 0-59
    if ( minutes < 0 || minutes > 59 )
      throw new IllegalArgumentException( "Minutes must be 0-59, got: " + minutes );

    // validate seconds is 0-59
    if ( seconds < 0 || seconds > 59 )
      throw new IllegalArgumentException( "Seconds must be 0-59, got: " + seconds );

    // validate milliseconds is 0-999
    if ( millis < 0 || millis > 999 )
      throw new IllegalArgumentException( "Milliseconds must be 0-999, got: " + millis );

    // validate special case: only 24:00:00.000 is valid, not 24:00:00.001 etc
    if ( hours == 24 && ( minutes > 0 || seconds > 0 || millis > 0 ) )
      throw new IllegalArgumentException( "Time components beyond 24:00:00.000 not allowed" );
  }

  /**************************************** formatTime *******************************************/
  // formats time with specified number of components using efficient string building
  private String formatTime( int components )
  {
    // create string builder with appropriate initial capacity
    StringBuilder sb = new StringBuilder( 12 );

    // always include hours with zero padding
    int h = getHour();
    if ( h < 10 )
      sb.append( '0' );
    sb.append( h );

    if ( components >= 2 )
    {
      // add minutes with zero padding
      int m = getMinute();
      sb.append( ':' );
      if ( m < 10 )
        sb.append( '0' );
      sb.append( m );

      if ( components >= 3 )
      {
        // add seconds with zero padding
        int s = getSecond();
        sb.append( ':' );
        if ( s < 10 )
          sb.append( '0' );
        sb.append( s );

        if ( components >= 4 )
        {
          // add milliseconds with zero padding (three digits)
          int ms = getMillisecond();
          sb.append( '.' );
          if ( ms < 100 )
            sb.append( '0' );
          if ( ms < 10 )
            sb.append( '0' );
          sb.append( ms );
        }
      }
    }

    return sb.toString();
  }
}