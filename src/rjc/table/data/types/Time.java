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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Pattern;

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

  private static final long              serialVersionUID   = 1L;

  // time constants
  public static final int                MILLIS_PER_SECOND  = 1000;
  public static final int                MILLIS_PER_MINUTE  = 60 * MILLIS_PER_SECOND;
  public static final int                MILLIS_PER_HOUR    = 60 * MILLIS_PER_MINUTE;
  public static final int                MILLIS_PER_DAY     = 24 * MILLIS_PER_HOUR;

  // boundary values
  public static final Time               MIN_VALUE          = new Time( 0 );                                          // 00:00:00.000
  public static final Time               NOON               = new Time( 12 * MILLIS_PER_HOUR );                       // 12:00:00.000 (noon)
  public static final Time               MAX_VALUE          = new Time( MILLIS_PER_DAY );                             // 24:00:00.000 (end of day)

  // parsing patterns (compiled once for performance)
  private static final Pattern           NUMERIC_PATTERN    = Pattern.compile( "^\\d+$" );
  private static final Pattern           END_OF_DAY_PATTERN = Pattern.compile( "^24[:\\s.]?0*[:\\s.]?0*[:\\s.]?0*$" );

  // standard formatters
  private static final DateTimeFormatter FLEXIBLE_FORMATTER = DateTimeFormatter
      .ofPattern( "H[:][.][ ][-][m][:][.][ ][-][s][:][.][ ][-][SSS][SS][S]" );

  // internal state - milliseconds since midnight 00:00:00.000 (0 to MILLIS_PER_DAY inclusive)
  private final int                      m_milliseconds;

  // ================================ Constructors ================================

  /**
   * Private constructor - use factory methods instead.
   */
  private Time( int milliseconds )
  {
    this.m_milliseconds = milliseconds;
  }

  // ================================ Factory Methods ================================

  /**
   * Creates a Time from hours, minutes, seconds and milliseconds.
   * 
   * @param hours the hour component (0-24)
   * @param minutes the minute component (0-59)  
   * @param seconds the second component (0-59)
   * @param millis the millisecond component (0-999)
   * @return a new Time instance
   * @throws IllegalArgumentException if any component is out of range
   */
  public static Time of( int hours, int minutes, int seconds, int millis )
  {
    validateTimeComponents( hours, minutes, seconds, millis );
    int totalMillis = hours * MILLIS_PER_HOUR + minutes * MILLIS_PER_MINUTE + seconds * MILLIS_PER_SECOND + millis;
    return new Time( totalMillis );
  }

  /**
   * Creates a Time from hours, minutes and seconds.
   */
  public static Time of( int hours, int minutes, int seconds )
  {
    return of( hours, minutes, seconds, 0 );
  }

  /**
   * Creates a Time from hours and minutes.
   */
  public static Time of( int hours, int minutes )
  {
    return of( hours, minutes, 0, 0 );
  }

  /**
   * Creates a Time from milliseconds since midnight start of day.
   * 
   * @param milliseconds milliseconds since midnight (0 to MILLIS_PER_DAY inclusive)
   * @return a new Time instance
   * @throws IllegalArgumentException if milliseconds is out of valid range
   */
  public static Time ofMilliseconds( int milliseconds )
  {
    if ( milliseconds < 0 || milliseconds > MILLIS_PER_DAY )
      throw new IllegalArgumentException(
          "Milliseconds must be between 0 and " + MILLIS_PER_DAY + ", got: " + milliseconds );
    return new Time( milliseconds );
  }

  /**
   * Creates a Time from fractional hours.
   * 
   * @param hours fractional hours (0.0 to 24.0 inclusive)
   * @return a new Time instance
   * @throws IllegalArgumentException if hours is out of valid range
   */
  public static Time ofHours( double hours )
  {
    if ( hours < 0.0 || hours > 24.0 )
      throw new IllegalArgumentException( "Hours must be between 0.0 and 24.0, got: " + hours );
    return new Time( (int) Math.round( hours * MILLIS_PER_HOUR ) );
  }

  /**
   * Creates a Time from a LocalTime.
   * 
   * @param localTime the LocalTime to convert
   * @return a new Time instance
   * @throws NullPointerException if localTime is null
   */
  public static Time of( LocalTime localTime )
  {
    Objects.requireNonNull( localTime, "LocalTime cannot be null" );
    return new Time( (int) ( localTime.toNanoOfDay() / 1_000_000L ) );
  }

  /**
   * Creates a Time from the current system time.
   * 
   * @return a new Time representing the current time of day
   */
  public static Time now()
  {
    return of( LocalTime.now() );
  }

  /**
   * Parses a time string into a Time instance.
   * 
   * <p>Supports various formats including:
   * <ul>
   * <li>Numeric: "8" (8:00), "830" (8:30), "83045" (8:30:45)</li>
   * <li>Standard: "8:30:45.123", "08:30", "24:00:00"</li>
   * <li>Flexible separators: "8.30", "8 30 45"</li>
   * </ul>
   * 
   * @param timeString the string to parse
   * @return a new Time instance
   * @throws IllegalArgumentException if the string cannot be parsed
   * @throws NullPointerException if timeString is null
   */
  public static Time parse( String timeString )
  {
    Objects.requireNonNull( timeString, "Time string cannot be null" );

    String cleaned = timeString.trim();
    if ( cleaned.isEmpty() )
      throw new IllegalArgumentException( "Time string cannot be empty" );

    try
    {
      // try parsing as pure number first (fastest path)
      if ( NUMERIC_PATTERN.matcher( cleaned ).matches() )
        return parseNumericString( cleaned );

      // check for end-of-day pattern (24:00:00)
      if ( END_OF_DAY_PATTERN.matcher( cleaned ).matches() )
        return MAX_VALUE;

      // use LocalTime parser with flexible format
      LocalTime localTime = LocalTime.parse( cleaned, FLEXIBLE_FORMATTER );
      return of( localTime );

    }
    catch ( DateTimeParseException | NumberFormatException e )
    {
      throw new IllegalArgumentException( "Cannot parse time string: '" + timeString + "'", e );
    }
  }

  // ================================ Accessor Methods ================================

  /**
   * Gets the hours component (0-24).
   */
  public int getHours()
  {
    return m_milliseconds / MILLIS_PER_HOUR;
  }

  /**
   * Gets the minutes component (0-59).
   */
  public int getMinutes()
  {
    return ( m_milliseconds / MILLIS_PER_MINUTE ) % 60;
  }

  /**
   * Gets the seconds component (0-59).
   */
  public int getSeconds()
  {
    return ( m_milliseconds / MILLIS_PER_SECOND ) % 60;
  }

  /**
   * Gets the milliseconds component (0-999).
   */
  public int getMilliseconds()
  {
    return m_milliseconds % MILLIS_PER_SECOND;
  }

  /**
   * Gets the total milliseconds since midnight.
   */
  public int toMillisecondsOfDay()
  {
    return m_milliseconds;
  }

  /**
   * Converts to fractional hours.
   */
  public double toHours()
  {
    return (double) m_milliseconds / MILLIS_PER_HOUR;
  }

  /**
   * Converts to a LocalTime (24:00:00 becomes 00:00:00).
   */
  public LocalTime toLocalTime()
  {
    return m_milliseconds == MILLIS_PER_DAY ? LocalTime.MIDNIGHT : LocalTime.ofNanoOfDay( m_milliseconds * 1_000_000L );
  }

  // ================================ Arithmetic Methods ================================

  /**
   * Returns a copy with the specified milliseconds added.
   * Values wrap around day boundaries.
   */
  public Time plusMilliseconds( int millisToAdd )
  {
    int newMillis = ( m_milliseconds + millisToAdd ) % MILLIS_PER_DAY;
    if ( newMillis < 0 )
      newMillis += MILLIS_PER_DAY;
    return new Time( newMillis );
  }

  /**
   * Returns a copy with the specified seconds added.
   */
  public Time plusSeconds( int secondsToAdd )
  {
    return plusMilliseconds( secondsToAdd * MILLIS_PER_SECOND );
  }

  /**
   * Returns a copy with the specified minutes added.
   */
  public Time plusMinutes( int minutesToAdd )
  {
    return plusMilliseconds( minutesToAdd * MILLIS_PER_MINUTE );
  }

  /**
   * Returns a copy with the specified hours added.
   */
  public Time plusHours( int hoursToAdd )
  {
    return plusMilliseconds( hoursToAdd * MILLIS_PER_HOUR );
  }

  // ================================ Comparison Methods ================================

  /**
   * Checks if this time is before the specified time.
   */
  public boolean isBefore( Time other )
  {
    Objects.requireNonNull( other );
    return m_milliseconds < other.m_milliseconds;
  }

  /**
   * Checks if this time is after the specified time.
   */
  public boolean isAfter( Time other )
  {
    Objects.requireNonNull( other );
    return m_milliseconds > other.m_milliseconds;
  }

  /**
   * Returns the difference in milliseconds between this time and another.
   * Positive if this time is later, negative if earlier.
   */
  @Override
  public int compareTo( Time other )
  {
    return Integer.compare( m_milliseconds, other.m_milliseconds );
  }

  /**
   * Returns the absolute difference in milliseconds between two times.
   */
  public int differenceInMillis( Time other )
  {
    Objects.requireNonNull( other );
    return Math.abs( m_milliseconds - other.m_milliseconds );
  }

  // ================================ Formatting Methods ================================

  /**
   * Returns time formatted as "HH:MM:SS.mmm".
   */
  @Override
  public String toString()
  {
    return formatTime( 4 );
  }

  /**
   * Returns time formatted as "HH:MM".
   */
  public String toShortString()
  {
    return formatTime( 2 );
  }

  /**
   * Returns time with specified number of components.
   * 
   * @param components 1=HH, 2=HH:MM, 3=HH:MM:SS, 4+=HH:MM:SS.mmm
   */
  public String format( int components )
  {
    if ( components < 1 )
      throw new IllegalArgumentException( "Components must be >= 1" );
    return formatTime( components );
  }

  // ================================ Object Methods ================================

  @Override
  public boolean equals( Object obj )
  {
    if ( this == obj )
      return true;
    if ( obj instanceof Time other )
      return m_milliseconds == other.m_milliseconds;
    return false;
  }

  @Override
  public int hashCode()
  {
    return Integer.hashCode( m_milliseconds );
  }

  // ================================ Private Helper Methods ================================

  private static void validateTimeComponents( int hours, int minutes, int seconds, int millis )
  {
    if ( hours < 0 || hours > 24 )
      throw new IllegalArgumentException( "Hours must be 0-24, got: " + hours );
    if ( minutes < 0 || minutes > 59 )
      throw new IllegalArgumentException( "Minutes must be 0-59, got: " + minutes );
    if ( seconds < 0 || seconds > 59 )
      throw new IllegalArgumentException( "Seconds must be 0-59, got: " + seconds );
    if ( millis < 0 || millis > 999 )
      throw new IllegalArgumentException( "Milliseconds must be 0-999, got: " + millis );
    if ( hours == 24 && ( minutes > 0 || seconds > 0 || millis > 0 ) )
      throw new IllegalArgumentException( "Time components beyond 24:00:00.000 not allowed" );
  }

  private static Time parseNumericString( String numStr )
  {
    int num = Integer.parseInt( numStr );
    int len = numStr.length();

    return switch ( len )
    {
      case 1, 2 -> of( num, 0, 0, 0 ); // "8" -> 08:00:00
      case 3, 4 -> of( num / 100, num % 100, 0, 0 ); // "830" -> 08:30:00
      case 5, 6 -> of( num / 10000, ( num / 100 ) % 100, num % 100, 0 ); // "83045" -> 08:30:45
      default -> throw new IllegalArgumentException( "Numeric string too long: " + numStr );
    };
  }

  private String formatTime( int components )
  {
    StringBuilder sb = new StringBuilder( 12 );

    // hours (always included)
    int h = getHours();
    if ( h < 10 )
      sb.append( '0' );
    sb.append( h );

    if ( components >= 2 )
    {
      // minutes
      int m = getMinutes();
      sb.append( ':' );
      if ( m < 10 )
        sb.append( '0' );
      sb.append( m );

      if ( components >= 3 )
      {
        // seconds
        int s = getSeconds();
        sb.append( ':' );
        if ( s < 10 )
          sb.append( '0' );
        sb.append( s );

        if ( components >= 4 )
        {
          // milliseconds
          int ms = getMilliseconds();
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