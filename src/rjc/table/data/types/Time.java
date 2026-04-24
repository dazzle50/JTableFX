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
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/*************************************************************************************************/
/************** Time of day from 00:00:00.000 to 24:00:00.000 inclusive (ms precision) ***********/
/*************************************************************************************************/

/**
 * Immutable time-of-day from {@code 00:00:00.000} to {@code 24:00:00.000} inclusive, stored
 * internally as a single {@code int} of milliseconds since midnight {@code 00:00:00.000}.
 * <p>
 * Unlike {@link LocalTime}, this class permits {@code 24:00:00.000} to represent the end of a
 * calendar day, distinct from {@code 00:00:00.000} (start of the same or next day). This
 * distinction is important for scheduling and financial applications.
 * <p>
 * Arithmetic methods (e.g. {@link #plusHours}, {@link #plusMilliseconds}) wrap around day
 * boundaries modulo 24 hours. End-of-day ({@code 24:00:00.000}) participates in wrapping: adding
 * any positive amount to it produces a time in the next cycle.
 * <p>
 * Example usage:
 * <pre>{@code
 * Time open   = Time.of( 9, 0 );                    // 09:00:00.000
 * Time close  = Time.of( 17, 30 );                  // 17:30:00.000
 * Time noon   = Time.NOON;                           // 12:00:00.000
 * boolean isOpen = noon.isBetween( open, close );   // true
 * Time later  = open.plusHours( 2 );                // 11:00:00.000
 * String s    = close.format( 2 );                  // "17:30"
 * }</pre>
 */
public final class Time implements Serializable, Comparable<Time>
{
  private static final long serialVersionUID  = 1L;

  // ---- millisecond conversion constants (all fit safely within int) ----
  public static final int   MILLIS_PER_SECOND = 1_000;
  public static final int   MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
  public static final int   MILLIS_PER_HOUR   = 60 * MILLIS_PER_MINUTE;
  public static final int   MILLIS_PER_DAY    = 24 * MILLIS_PER_HOUR;

  // ---- commonly used named instances ----
  public static final Time  MIN_VALUE         = new Time( 0 );                    // 00:00:00.000
  public static final Time  NOON              = new Time( 12 * MILLIS_PER_HOUR ); // 12:00:00.000 (noon)
  public static final Time  MAX_VALUE         = new Time( MILLIS_PER_DAY );       // 24:00:00.000 (end of day)

  // internal state - milliseconds since midnight 00:00:00.000 (0 to MILLIS_PER_DAY inclusive)
  private final int         m_milliseconds;

  /**
   * Enumeration of time interval units for truncation, stepping and rounding operations.
   */
  public enum IntervalUnit
  {
    HALF_DAY( 12 * MILLIS_PER_HOUR ), // 00:00 and 12:00
    QUARTER_DAY( 6 * MILLIS_PER_HOUR ), // 00:00, 06:00, 12:00, and 18:00
    HOUR( MILLIS_PER_HOUR ), //
    HALF_HOUR( 30 * MILLIS_PER_MINUTE ), //
    TEN_MINUTE( 10 * MILLIS_PER_MINUTE ), //
    MINUTE( MILLIS_PER_MINUTE ), //
    SECOND( MILLIS_PER_SECOND );

    private final int milliseconds;

    IntervalUnit( int milliseconds )
    {
      this.milliseconds = milliseconds;
    }

    public int getMilliseconds()
    {
      return milliseconds;
    }
  }

  // =================================== Constructor ===================================

  /***************************************** constructor *****************************************/
  /**
   * Private constructor. Use factory methods to create instances.
   *
   * @param milliseconds milliseconds since midnight in range {@code [0, MILLIS_PER_DAY]}
   */
  private Time( int milliseconds )
  {
    m_milliseconds = milliseconds;
  }

  // =================================== Factory Methods ===================================

  /*********************************************** of ********************************************/
  /**
   * Creates a {@code Time} from hours, minutes, seconds, and milliseconds.
   * <p>
   * The only valid time with {@code hours == 24} is {@code 24:00:00.000}; any non-zero
   * minutes, seconds, or milliseconds combined with hour 24 is rejected.
   *
   * @param hours   hour component (0–24)
   * @param minutes minute component (0–59)
   * @param seconds second component (0–59)
   * @param millis  millisecond component (0–999)
   * @return a new {@code Time} instance
   * @throws IllegalArgumentException if any component is out of range
   */
  public static Time of( int hours, int minutes, int seconds, int millis )
  {
    // validate before any arithmetic to give clear error messages
    validateComponents( hours, minutes, seconds, millis );
    return new Time( hours * MILLIS_PER_HOUR + minutes * MILLIS_PER_MINUTE + seconds * MILLIS_PER_SECOND + millis );
  }

  /*********************************************** of ********************************************/
  /**
   * Creates a {@code Time} from hours, minutes, and seconds (milliseconds default to 0).
   *
   * @param hours   hour component (0–24)
   * @param minutes minute component (0–59)
   * @param seconds second component (0–59)
   * @return a new {@code Time} instance
   * @throws IllegalArgumentException if any component is out of range
   */
  public static Time of( int hours, int minutes, int seconds )
  {
    return of( hours, minutes, seconds, 0 );
  }

  /*********************************************** of ********************************************/
  /**
   * Creates a {@code Time} from hours and minutes (seconds and milliseconds default to 0).
   *
   * @param hours   hour component (0–24)
   * @param minutes minute component (0–59)
   * @return a new {@code Time} instance
   * @throws IllegalArgumentException if any component is out of range
   */
  public static Time of( int hours, int minutes )
  {
    return of( hours, minutes, 0, 0 );
  }

  /*********************************************** of ********************************************/
  /**
   * Creates a {@code Time} from whole hours only (minutes, seconds, milliseconds default to 0).
   * <p>
   * Useful for simple hour-boundary times such as {@code Time.of(9)} for 9 AM.
   *
   * @param hours hour component (0–24)
   * @return a new {@code Time} instance
   * @throws IllegalArgumentException if {@code hours} is outside {@code [0, 24]}
   */
  public static Time of( int hours )
  {
    return of( hours, 0, 0, 0 );
  }

  /*************************************** ofMilliseconds ****************************************/
  /**
   * Creates a {@code Time} from total milliseconds since midnight.
   *
   * @param milliseconds milliseconds since midnight in range {@code [0, MILLIS_PER_DAY]}
   * @return a new {@code Time} instance
   * @throws IllegalArgumentException if {@code milliseconds} is outside the valid range
   */
  public static Time ofMilliseconds( int milliseconds )
  {
    if ( milliseconds < 0 || milliseconds > MILLIS_PER_DAY )
      throw new IllegalArgumentException(
          "Milliseconds " + milliseconds + " not in range [0, " + MILLIS_PER_DAY + "]" );
    return new Time( milliseconds );
  }

  /******************************************** ofHours ******************************************/
  /**
   * Creates a {@code Time} from fractional hours since midnight (e.g. {@code 9.5} = 09:30).
   * <p>
   * The fractional value is converted to the nearest millisecond via rounding.
   *
   * @param hours fractional hours in range {@code [0.0, 24.0]}
   * @return a new {@code Time} instance
   * @throws IllegalArgumentException if {@code hours} is outside {@code [0.0, 24.0]}
   */
  public static Time ofHours( double hours )
  {
    if ( hours < 0.0 || hours > 24.0 )
      throw new IllegalArgumentException( "Hours " + hours + " not in range [0.0, 24.0]" );

    // round to nearest millisecond then clamp in case floating-point overshoot
    int ms = (int) Math.round( hours * MILLIS_PER_HOUR );
    if ( ms > MILLIS_PER_DAY )
      ms = MILLIS_PER_DAY;
    return new Time( ms );
  }

  /*********************************************** of ********************************************/
  /**
   * Creates a {@code Time} from a {@link LocalTime}.
   * <p>
   * Sub-millisecond precision is truncated. {@code LocalTime} cannot represent
   * {@code 24:00:00.000}; use {@link #MAX_VALUE} for that value.
   *
   * @param localTime the {@link LocalTime} to convert (must not be null)
   * @return a new {@code Time} instance
   * @throws NullPointerException if {@code localTime} is null
   */
  public static Time of( LocalTime localTime )
  {
    Objects.requireNonNull( localTime, "localTime must not be null" );
    // truncate nanoseconds to milliseconds via integer division
    return new Time( (int) ( localTime.toNanoOfDay() / 1_000_000L ) );
  }

  /*********************************************** now *******************************************/
  /**
   * Creates a {@code Time} representing the current system time.
   * <p>
   * Sub-millisecond precision is truncated.
   *
   * @return a new {@code Time} representing the current time of day
   */
  public static Time now()
  {
    return of( LocalTime.now() );
  }

  /********************************************** parse ******************************************/
  /**
   * Parses a time string into a {@code Time} instance using {@link TimeParser}.
   * <p>
   * Supports 12-hour, 24-hour, compact, and natural-language formats.
   *
   * @param text the string to parse (must not be null)
   * @return a new {@code Time} instance
   * @throws NullPointerException   if {@code text} is null
   * @throws DateTimeParseException if the string cannot be parsed
   */
  public static Time parse( String text )
  {
    return TimeParser.parse( text );
  }

  // =================================== Accessor Methods ===================================

  /******************************************** getHour ******************************************/
  /**
   * Returns the hour component of this time (0–24).
   *
   * @return hour component
   */
  public int getHour()
  {
    return m_milliseconds / MILLIS_PER_HOUR;
  }

  /******************************************* getMinute *****************************************/
  /**
   * Returns the minute component of this time (0–59).
   *
   * @return minute component
   */
  public int getMinute()
  {
    return ( m_milliseconds / MILLIS_PER_MINUTE ) % 60;
  }

  /******************************************* getSecond *****************************************/
  /**
   * Returns the second component of this time (0–59).
   *
   * @return second component
   */
  public int getSecond()
  {
    return ( m_milliseconds / MILLIS_PER_SECOND ) % 60;
  }

  /***************************************** getMillisecond **************************************/
  /**
   * Returns the millisecond component of this time (0–999).
   *
   * @return millisecond component
   */
  public int getMillisecond()
  {
    return m_milliseconds % MILLIS_PER_SECOND;
  }

  /*************************************** toMillisecondsOfDay ***********************************/
  /**
   * Returns total milliseconds since start of day (00:00:00.000) for this time.
   *
   * @return milliseconds since start of day in range {@code [0, MILLIS_PER_DAY]}
   */
  public int toMillisecondsOfDay()
  {
    return m_milliseconds;
  }

  /******************************************** toHours ******************************************/
  /**
   * Converts this time to fractional hours since start of day (00:00:00.000).
   * <p>
   * For example, {@code 09:30:00.000} returns {@code 9.5}.
   *
   * @return fractional hours in range {@code [0.0, 24.0]}
   */
  public double toHours()
  {
    return (double) m_milliseconds / MILLIS_PER_HOUR;
  }

  /******************************************* toLocalTime ***************************************/
  /**
   * Converts this time to a {@link LocalTime}.
   * <p>
   * {@code 24:00:00.000} maps to {@link LocalTime#MIDNIGHT} ({@code 00:00:00}) because
   * {@link LocalTime} has no end-of-day representation.
   *
   * @return equivalent {@link LocalTime}
   */
  public LocalTime toLocalTime()
  {
    // special-case end-of-day since LocalTime.ofNanoOfDay rejects MILLIS_PER_DAY * 1e6
    if ( m_milliseconds == MILLIS_PER_DAY )
      return LocalTime.MIDNIGHT;
    return LocalTime.ofNanoOfDay( m_milliseconds * 1_000_000L );
  }

  // =================================== Arithmetic Methods ===================================

  /***************************************** plusMilliseconds ************************************/
  /**
   * Returns a copy of this time with the specified milliseconds added, wrapping at day boundaries.
   * <p>
   * Negative values subtract milliseconds. If a positive addition lands exactly at the end of
   * the day, {@link #MAX_VALUE} is returned to preserve the special {@code 24:00} value.
   *
   * @param millisToAdd the milliseconds to add, negative to subtract
   * @return the adjusted time, or this instance if no change is required
   */
  public Time plusMilliseconds( long millisToAdd )
  {
    if ( millisToAdd == 0 )
      return this;

    // add and wrap around day boundaries using modulo arithmetic
    int ms = (int) ( ( m_milliseconds + millisToAdd ) % MILLIS_PER_DAY );
    if ( ms < 0 )
      ms += MILLIS_PER_DAY;

    // when adding a positive amount, if we land exactly at the end of the day, return MAX_VALUE to preserve 24:00
    if ( millisToAdd > 0 && ms == 0 )
      return MAX_VALUE;

    return new Time( ms );
  }

  /******************************************* plusSeconds ***************************************/
  /**
   * Returns a copy of this time with the specified seconds added, wrapping at day boundaries.
   *
   * @param secondsToAdd seconds to add (may be negative)
   * @return a new {@code Time} with seconds added
   */
  public Time plusSeconds( int secondsToAdd )
  {
    return plusMilliseconds( (long) secondsToAdd * MILLIS_PER_SECOND );
  }

  /******************************************* plusMinutes ***************************************/
  /**
   * Returns a copy of this time with the specified minutes added, wrapping at day boundaries.
   *
   * @param minutesToAdd minutes to add (may be negative)
   * @return a new {@code Time} with minutes added
   */
  public Time plusMinutes( int minutesToAdd )
  {
    return plusMilliseconds( (long) minutesToAdd * MILLIS_PER_MINUTE );
  }

  /******************************************** plusHours ****************************************/
  /**
   * Returns a copy of this time with the specified hours added, wrapping at day boundaries.
   *
   * @param hoursToAdd hours to add (may be negative)
   * @return a new {@code Time} with hours added
   */
  public Time plusHours( int hoursToAdd )
  {
    return plusMilliseconds( (long) hoursToAdd * MILLIS_PER_HOUR );
  }

  /****************************************** roundDown ******************************************/
  /**
   * Returns a copy of this time rounded down (floored) to the nearest boundary
   * of the specified interval unit. If this time is already exactly on a boundary
   * of the given interval, the same instance is returned.
   *
   * @param interval the time interval unit to round to
   * @return this instance if already aligned, otherwise a new {@code Time} instance
   * @throws NullPointerException if {@code interval} is null
   */
  public Time roundDown( IntervalUnit interval )
  {
    Objects.requireNonNull( interval, "interval must not be null" );

    int remainder = m_milliseconds % interval.getMilliseconds();
    if ( remainder == 0 )
      return this;

    return new Time( m_milliseconds - remainder );
  }

  /******************************************* roundUp *******************************************/
  /**
   * Returns a copy of this time rounded up (ceiled) to the nearest boundary
   * of the specified interval unit. If this time is already exactly on a boundary
   * of the given interval, the same instance is returned.
   * <p>
   * If rounding up would cross midnight, the result is {@link #MAX_VALUE} (24:00:00.000).
   * The value 24:00:00.000 itself remains unchanged under any rounding operation.
   *
   * @param interval the time interval unit to round to
   * @return this instance if already aligned, otherwise a new {@code Time} instance
   * @throws NullPointerException if {@code interval} is null
   */
  public Time roundUp( IntervalUnit interval )
  {
    Objects.requireNonNull( interval, "interval must not be null" );

    if ( m_milliseconds == MILLIS_PER_DAY )
      return this;

    int step = interval.getMilliseconds();
    int remainder = m_milliseconds % step;
    if ( remainder == 0 )
      return this;

    int ms = m_milliseconds + ( step - remainder );
    if ( ms > MILLIS_PER_DAY )
      return Time.MAX_VALUE;

    return new Time( ms );
  }

  /**************************************** plusInterval *****************************************/
  /**
   * Returns a copy of this time with the specified number of intervals added.
   * Positive values advance the time; negative values retreat it. The result wraps
   * around day boundaries modulo 24 hours, consistent with the other arithmetic
   * methods of this class.
   *
   * @param count    number of intervals to add (positive = forward, negative = backward, zero = no change)
   * @param interval the size of each interval step
   * @return this instance if {@code count == 0}, otherwise a new {@code Time} instance
   * @throws NullPointerException if {@code interval} is null
   */
  public Time plusInterval( int count, IntervalUnit interval )
  {
    Objects.requireNonNull( interval, "interval must not be null" );

    if ( count == 0 )
      return this;

    long millisToAdd = (long) count * interval.getMilliseconds();
    return plusMilliseconds( millisToAdd );
  }

  // =================================== Predicate Methods ===================================

  /******************************************** isBefore *****************************************/
  /**
   * Returns {@code true} if this time is strictly before {@code other}.
   *
   * @param other the time to compare against (must not be null)
   * @return {@code true} if this time is chronologically earlier
   */
  public boolean isBefore( Time other )
  {
    return m_milliseconds < other.m_milliseconds;
  }

  /********************************************* isAfter *****************************************/
  /**
   * Returns {@code true} if this time is strictly after {@code other}.
   *
   * @param other the time to compare against (must not be null)
   * @return {@code true} if this time is chronologically later
   */
  public boolean isAfter( Time other )
  {
    return m_milliseconds > other.m_milliseconds;
  }

  /********************************************* isEqual *****************************************/
  /**
   * Returns {@code true} if this time represents the same instant as {@code other}.
   * <p>
   * Returns {@code false} (rather than throwing) if {@code other} is null.
   * Use {@link #equals} for strict object equality.
   *
   * @param other the time to compare against (may be null)
   * @return {@code true} if both times have the same millisecond value
   */
  public boolean isEqual( Time other )
  {
    // null-safe: consistent with ChronoLocalDate.isEqual convention
    return other != null && m_milliseconds == other.m_milliseconds;
  }

  /******************************************** isBetween ****************************************/
  /**
   * Returns {@code true} if this time falls within the inclusive range {@code [start, end]}.
   * <p>
   * If {@code start} is after {@code end} the range is considered empty and this method
   * returns {@code false}.
   *
   * @param start range start, inclusive (must not be null)
   * @param end   range end, inclusive (must not be null)
   * @return {@code true} if {@code start <= this <= end}
   * @throws NullPointerException if either argument is null
   */
  public boolean isBetween( Time start, Time end )
  {
    Objects.requireNonNull( start, "start must not be null" );
    Objects.requireNonNull( end, "end must not be null" );
    return m_milliseconds >= start.m_milliseconds && m_milliseconds <= end.m_milliseconds;
  }

  // =================================== Difference Methods ===================================

  /******************************************* millisUntil ***************************************/
  /**
   * Returns the signed number of milliseconds from this time to {@code other}.
   * <p>
   * The result is positive if {@code other} is later, negative if earlier, and zero if equal.
   * This is equivalent to {@code other.toMillisecondsOfDay() - this.toMillisecondsOfDay()}.
   *
   * @param other the target time (must not be null)
   * @return signed millisecond difference
   * @throws NullPointerException if {@code other} is null
   */
  public int millisUntil( Time other )
  {
    Objects.requireNonNull( other, "other must not be null" );
    return other.m_milliseconds - m_milliseconds;
  }

  /***************************************** differenceInMillis **********************************/
  /**
   * Returns the absolute number of milliseconds between this time and {@code other}.
   * <p>
   * The result is always non-negative. For a signed difference, use {@link #millisUntil}.
   *
   * @param other the time to compare with (must not be null)
   * @return absolute millisecond difference
   * @throws NullPointerException if {@code other} is null
   */
  public int differenceInMillis( Time other )
  {
    Objects.requireNonNull( other, "other must not be null" );
    return Math.abs( m_milliseconds - other.m_milliseconds );
  }

  // =================================== Comparison Methods ===================================

  /******************************************** compareTo ****************************************/
  /**
   * Compares this time to another time for natural ordering.
   *
   * @param other the time to compare to (must not be null)
   * @return negative if this is earlier, zero if equal, positive if later
   * @throws NullPointerException if {@code other} is null
   */
  @Override
  public int compareTo( Time other )
  {
    Objects.requireNonNull( other, "other must not be null" );
    return Integer.compare( m_milliseconds, other.m_milliseconds );
  }

  // =================================== Formatting Methods ===================================

  /******************************************** toString *****************************************/
  /**
   * Returns this time as a full string in the format {@code HH:MM:SS.mmm}.
   *
   * @return time string with all four components
   */
  @Override
  public String toString()
  {
    return formatTime( 4 );
  }

  /******************************************** toString *****************************************/
  /**
   * Returns this time formatted according to the specified pattern.
   * <p>
   * The formatting is performed using {@link DateTimeFormatter#ofPattern(String)}
   * applied to the {@link LocalTime} representation of this object.
   *
   * @param pattern  the format pattern (e.g., "HH:mm:ss", "h:mm a", "HH:mm:ss.SSS")
   * @return         formatted time string
   * @throws DateTimeException   if the pattern is invalid or formatting fails
   * @throws NullPointerException if {@code pattern} is null
   * @see DateTimeFormatter#ofPattern(String)
   */
  public String toString( String pattern )
  {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern( pattern );
    return formatter.format( toLocalTime() );
  }

  /***************************************** toShortString ***************************************/
  /**
   * Returns this time as a short string in the format {@code HH:MM}.
   *
   * @return time string with hour and minute components only
   */
  public String toShortString()
  {
    return formatTime( 2 );
  }

  /********************************************** format *****************************************/
  /**
   * Returns this time formatted with the specified number of components.
   * <table border="1">
   * <caption>Component count to format mapping</caption>
   * <tr><th>components</th><th>format</th><th>example</th></tr>
   * <tr><td>1</td><td>{@code HH}</td><td>{@code 14}</td></tr>
   * <tr><td>2</td><td>{@code HH:MM}</td><td>{@code 14:30}</td></tr>
   * <tr><td>3</td><td>{@code HH:MM:SS}</td><td>{@code 14:30:05}</td></tr>
   * <tr><td>4+</td><td>{@code HH:MM:SS.mmm}</td><td>{@code 14:30:05.123}</td></tr>
   * </table>
   *
   * @param components the number of time components to include (must be ≥ 1)
   * @return formatted time string
   * @throws IllegalArgumentException if {@code components} is less than 1
   */
  public String format( int components )
  {
    if ( components < 1 )
      throw new IllegalArgumentException( "components must be >= 1, got: " + components );
    return formatTime( components );
  }

  // =================================== Object Methods ===================================

  /********************************************** equals *****************************************/
  /**
   * Indicates whether this time is equal to another object.
   * <p>
   * Two {@code Time} instances are equal if they have the same millisecond value.
   *
   * @param obj the object to compare with
   * @return {@code true} if {@code obj} is a {@code Time} with the same millisecond value
   */
  @Override
  public boolean equals( Object obj )
  {
    return obj instanceof Time other && m_milliseconds == other.m_milliseconds;
  }

  /********************************************* hashCode ****************************************/
  /**
   * Returns a hash code for this time.
   * <p>
   * Consistent with {@link #equals}: equal times have equal hash codes.
   *
   * @return hash code equal to the millisecond value
   */
  @Override
  public int hashCode()
  {
    // millisecond value is already a well-distributed int; no further hashing needed
    return m_milliseconds;
  }

  // =================================== Private Helpers ===================================

  /*************************************** validateComponents ************************************/
  // validates that all four time components are within their acceptable ranges
  private static void validateComponents( int hours, int minutes, int seconds, int millis )
  {
    if ( hours < 0 || hours > 24 )
      throw new IllegalArgumentException( "hours must be 0–24, got: " + hours );
    if ( minutes < 0 || minutes > 59 )
      throw new IllegalArgumentException( "minutes must be 0–59, got: " + minutes );
    if ( seconds < 0 || seconds > 59 )
      throw new IllegalArgumentException( "seconds must be 0–59, got: " + seconds );
    if ( millis < 0 || millis > 999 )
      throw new IllegalArgumentException( "millis must be 0–999, got: " + millis );
    // only 24:00:00.000 is permitted at hour 24
    if ( hours == 24 && ( minutes | seconds | millis ) != 0 )
      throw new IllegalArgumentException(
          "Only 24:00:00.000 is valid at hour 24; got 24:" + minutes + ":" + seconds + "." + millis );
  }

  /******************************************** formatTime ***************************************/
  // formats this time with the given number of components using a hand-rolled builder for speed
  private String formatTime( int components )
  {
    // pre-size the builder to the maximum output length
    StringBuilder sb = new StringBuilder( 12 );

    // always output two-digit hour
    int h = getHour();
    if ( h < 10 )
      sb.append( '0' );
    sb.append( h );

    if ( components < 2 )
      return sb.toString();

    // append :MM
    int m = getMinute();
    sb.append( ':' );
    if ( m < 10 )
      sb.append( '0' );
    sb.append( m );

    if ( components < 3 )
      return sb.toString();

    // append :SS
    int s = getSecond();
    sb.append( ':' );
    if ( s < 10 )
      sb.append( '0' );
    sb.append( s );

    if ( components < 4 )
      return sb.toString();

    // append .mmm with three-digit zero-padding
    int ms = getMillisecond();
    sb.append( '.' );
    if ( ms < 100 )
      sb.append( '0' );
    if ( ms < 10 )
      sb.append( '0' );
    sb.append( ms );

    return sb.toString();
  }
}