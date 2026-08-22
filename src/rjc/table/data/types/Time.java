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

import java.io.Serial;
import java.io.Serializable;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/*************************************************************************************************/
/************** Time of day from 00:00:00.000 to 24:00:00.000 inclusive (ms precision) ***********/
/*************************************************************************************************/

/**
 * Millisecond-precision linear stored endpoint from {@code 00:00} through {@code 24:00}. The two
 * endpoints are distinct for equality, ordering, predicates, and differences. Arithmetic is
 * deliberately cyclic and does not change that stored-value contract.
 *
 * <p>Instances are immutable and thread-safe.</p>
 */
public final class Time implements Serializable, Comparable<Time>
{
  @Serial
  private static final long serialVersionUID  = 2L;

  /** Milliseconds in one SI second. */
  public static final int   MILLIS_PER_SECOND = 1_000;
  /** Milliseconds in one minute. */
  public static final int   MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
  /** Milliseconds in one hour. */
  public static final int   MILLIS_PER_HOUR   = 60 * MILLIS_PER_MINUTE;
  /** Milliseconds in one 24-hour day. */
  public static final int   MILLIS_PER_DAY    = 24 * MILLIS_PER_HOUR;

  /** Start-of-day endpoint, {@code 00:00}. */
  public static final Time  MIN_VALUE         = new Time( 0 );
  /** Midday value, {@code 12:00}. */
  public static final Time  NOON              = new Time( 12 * MILLIS_PER_HOUR );
  /** Explicit end-of-day endpoint, {@code 24:00}. */
  public static final Time  MAX_VALUE         = new Time( MILLIS_PER_DAY );

  /** Stored millisecond coordinate from 0 through one full day. */
  private final int         milliseconds;

  private Time( int milliseconds )
  {
    this.milliseconds = milliseconds;
  }

  // ================================= Factory methods =================================

  /**
   * Creates a time on an exact hour.
   *
   * @param hour hour from 0 through 24
   * @return represented time
   * @throws DateTimeException if the hour is invalid
   */
  public static Time of( int hour )
  {
    return of( hour, 0, 0, 0 );
  }

  /**
   * Creates a time with minute precision.
   *
   * @param hour hour from 0 through 24
   * @param minute minute from 0 to 59
   * @return represented time
   * @throws DateTimeException if the components are invalid
   */
  public static Time of( int hour, int minute )
  {
    return of( hour, minute, 0, 0 );
  }

  /**
   * Creates a time with second precision.
   *
   * @param hour hour from 0 through 24
   * @param minute minute from 0 to 59
   * @param second second from 0 to 59
   * @return represented time
   * @throws DateTimeException if the components are invalid
   */
  public static Time of( int hour, int minute, int second )
  {
    return of( hour, minute, second, 0 );
  }

  /**
   * Creates a millisecond-precision time. Hour 24 is valid only when every smaller component is
   * zero.
   *
   * @param hour hour from 0 through 24
   * @param minute minute from 0 to 59
   * @param second second from 0 to 59
   * @param millisecond millisecond from 0 to 999
   * @return represented time
   * @throws DateTimeException if the components are invalid
   */
  public static Time of( int hour, int minute, int second, int millisecond )
  {
    validate( hour, minute, second, millisecond );
    return new Time( hour * MILLIS_PER_HOUR + minute * MILLIS_PER_MINUTE + second * MILLIS_PER_SECOND + millisecond );
  }

  /**
   * Creates a time from its stored millisecond coordinate.
   *
   * @param milliseconds milliseconds from 0 through one full day
   * @return represented time
   * @throws DateTimeException if the coordinate is outside the stored range
   */
  public static Time ofMilliseconds( int milliseconds )
  {
    if ( milliseconds < 0 || milliseconds > MILLIS_PER_DAY )
      throw new DateTimeException( "Milliseconds outside stored time range: " + milliseconds );
    return new Time( milliseconds );
  }

  /**
   * Creates a time from fractional hours, rounded to the nearest millisecond.
   *
   * @param hours finite hours from 0 through 24
   * @return represented time
   * @throws DateTimeException if the value is non-finite or outside the stored range
   */
  public static Time ofHours( double hours )
  {
    if ( !Double.isFinite( hours ) || hours < 0.0 || hours > 24.0 )
      throw new DateTimeException( "Hours outside stored time range: " + hours );
    return ofMilliseconds( Math.toIntExact( Math.round( hours * MILLIS_PER_HOUR ) ) );
  }

  /**
   * Converts a Java time, truncating sub-millisecond precision.
   *
   * @param value source time
   * @return represented time
   * @throws NullPointerException if {@code value} is null
   */
  public static Time of( LocalTime value )
  {
    Objects.requireNonNull( value, "value" );
    return new Time( (int) ( value.toNanoOfDay() / 1_000_000L ) );
  }

  /**
   * Obtains the current local time from the system-default clock.
   *
   * @return current local time
   */
  public static Time now()
  {
    return now( Clock.systemDefaultZone() );
  }

  /**
   * Obtains the current local time from a clock, truncating sub-millisecond precision.
   *
   * @param clock source of the current instant and time zone
   * @return the clock's current local time
   * @throws NullPointerException if {@code clock} is null
   */
  public static Time now( Clock clock )
  {
    return of( LocalTime.now( Objects.requireNonNull( clock, "clock" ) ) );
  }

  /**
   * Parses intelligent time text using current system defaults.
   *
   * @param text text to parse
   * @return parsed time
   * @throws NullPointerException if {@code text} is null
   * @throws java.time.format.DateTimeParseException if parsing fails
   */
  public static Time parse( String text )
  {
    return TimeParser.systemDefault().parse( text );
  }

  /**
   * Parses time text using a pattern and current default locale.
   *
   * @param text text to parse
   * @param pattern parsing pattern
   * @return parsed time
   * @throws NullPointerException if either argument is null
   * @throws IllegalArgumentException if the pattern is invalid
   * @throws java.time.format.DateTimeParseException if parsing fails
   */
  public static Time parse( String text, String pattern )
  {
    return TimeParser.systemDefault().parse( text, pattern );
  }

  // ================================= Component access =================================

  /**
   * Returns the stored hour.
   *
   * @return stored hour from 0 through 24
   */
  public int getHour()
  {
    return milliseconds / MILLIS_PER_HOUR;
  }

  /**
   * Returns the minute component.
   *
   * @return minute from 0 to 59
   */
  public int getMinute()
  {
    return milliseconds / MILLIS_PER_MINUTE % 60;
  }

  /**
   * Returns the second component.
   *
   * @return second from 0 to 59
   */
  public int getSecond()
  {
    return milliseconds / MILLIS_PER_SECOND % 60;
  }

  /**
   * Returns the millisecond component.
   *
   * @return millisecond from 0 to 999
   */
  public int getMillisecond()
  {
    return milliseconds % MILLIS_PER_SECOND;
  }

  /**
   * Returns the linear stored coordinate.
   *
   * @return stored milliseconds from 0 through one full day
   */
  public int toMillisecondsOfDay()
  {
    return milliseconds;
  }

  /**
   * Returns the stored time as fractional hours.
   *
   * @return stored time expressed as fractional hours
   */
  public double toHours()
  {
    return (double) milliseconds / MILLIS_PER_HOUR;
  }

  /**
   * Converts to {@link LocalTime}; stored {@code 24:00} is lossily mapped to midnight.
   *
   * @return equivalent Java time, with {@code 24:00} represented as {@code 00:00}
   */
  public LocalTime toLocalTime()
  {
    return milliseconds == MILLIS_PER_DAY ? LocalTime.MIDNIGHT : LocalTime.ofNanoOfDay( milliseconds * 1_000_000L );
  }

  // =================================== Time arithmetic ===================================

  /**
   * Adds cyclic milliseconds after reducing the amount modulo one day. Positive non-zero additions
   * landing at midnight return {@code 24:00}; negative ones return {@code 00:00}.
   *
   * @param amount milliseconds to add
   * @return adjusted stored endpoint, or this instance for zero
   */
  public Time plusMilliseconds( long amount )
  {
    if ( amount == 0L )
      return this;
    long base = milliseconds == MILLIS_PER_DAY ? 0L : milliseconds;
    int result = (int) Math.floorMod( base + amount % MILLIS_PER_DAY, MILLIS_PER_DAY );
    return amount > 0L && result == 0 ? MAX_VALUE : new Time( result );
  }

  /**
   * Adds seconds cyclically, reducing the amount before multiplication.
   *
   * @param amount seconds to add
   * @return adjusted stored time
   */
  public Time plusSeconds( long amount )
  {
    return plusReduced( amount, MILLIS_PER_SECOND );
  }

  /**
   * Adds minutes cyclically, reducing the amount before multiplication.
   *
   * @param amount minutes to add
   * @return adjusted stored time
   */
  public Time plusMinutes( long amount )
  {
    return plusReduced( amount, MILLIS_PER_MINUTE );
  }

  /**
   * Adds hours cyclically, reducing the amount before multiplication.
   *
   * @param amount hours to add
   * @return adjusted stored time
   */
  public Time plusHours( long amount )
  {
    return plusReduced( amount, MILLIS_PER_HOUR );
  }

  /**
   * Adds a fixed-duration interval cyclically.
   *
   * @param amount number of intervals to add
   * @param interval fixed-duration interval
   * @return adjusted stored time
   * @throws NullPointerException if {@code interval} is null
   */
  public Time plus( long amount, TimeInterval interval )
  {
    Objects.requireNonNull( interval, "interval" );
    return plusReduced( amount, interval.milliseconds() );
  }

  /**
   * Returns the greatest time on the interval's alignment grid that is not after this time.
   * The grid starts at {@code 00:00} and repeats within the stored day. For example,
   * {@code 10:37.startOf(HALF_HOUR)} is {@code 10:30}.
   *
   * <p>An already aligned value is both the start and end of adjacent grid ranges, so this method
   * returns {@code this}. This includes {@code 00:00}, interior boundaries such as {@code 10:30},
   * and the explicitly stored endpoint {@code 24:00}.</p>
   *
   * @param interval fixed-duration alignment grid
   * @return the nearest aligned time at or before this time, or {@code this} when already aligned
   * @throws NullPointerException if {@code interval} is null
   */
  public Time startOf( TimeInterval interval )
  {
    Objects.requireNonNull( interval, "interval" );
    if ( milliseconds == MILLIS_PER_DAY )
      return this;
    int span = Math.toIntExact( interval.milliseconds() );
    int remainder = milliseconds % span;
    return remainder == 0 ? this : new Time( milliseconds - remainder );
  }

  /**
   * Returns the least time on the interval's alignment grid that is not before this time.
   * The grid starts at {@code 00:00} and repeats within the stored day. For example,
   * {@code 10:37.endOf(HALF_HOUR)} is {@code 11:00}.
   *
   * <p>An already aligned value is both the start and end of adjacent grid ranges, so this method
   * returns {@code this}; it does not advance to the next boundary. This includes {@code 00:00},
   * interior boundaries such as {@code 10:30}, and the explicitly stored endpoint {@code 24:00}.</p>
   *
   * @param interval fixed-duration alignment grid
   * @return the nearest aligned time at or after this time, or {@code this} when already aligned
   * @throws NullPointerException if {@code interval} is null
   */
  public Time endOf( TimeInterval interval )
  {
    Objects.requireNonNull( interval, "interval" );
    if ( milliseconds == MILLIS_PER_DAY )
      return this;
    int span = Math.toIntExact( interval.milliseconds() );
    int remainder = milliseconds % span;
    return remainder == 0 ? this : new Time( milliseconds + span - remainder );
  }

  // ================================= Comparison and distance ================================

  /**
   * Tests stored ordering, where {@code 24:00} follows all earlier times.
   *
   * @param other time to compare
   * @return whether this time is earlier
   * @throws NullPointerException if {@code other} is null
   */
  public boolean isBefore( Time other )
  {
    return compareTo( other ) < 0;
  }

  /**
   * Tests stored ordering, where {@code 24:00} follows all earlier times.
   *
   * @param other time to compare
   * @return whether this time is later
   * @throws NullPointerException if {@code other} is null
   */
  public boolean isAfter( Time other )
  {
    return compareTo( other ) > 0;
  }

  /**
   * Tests membership in an inclusive, non-wrapping stored-time range.
   *
   * @param start inclusive lower bound
   * @param end inclusive upper bound
   * @return whether this time is within the bounds
   * @throws NullPointerException if either bound is null
   */
  public boolean isBetween( Time start, Time end )
  {
    Objects.requireNonNull( start, "start" );
    Objects.requireNonNull( end, "end" );
    return compareTo( start ) >= 0 && compareTo( end ) <= 0;
  }

  /**
   * Returns the signed linear stored-time distance.
   *
   * @param other target time
   * @return {@code other - this} in milliseconds
   * @throws NullPointerException if {@code other} is null
   */
  public int millisUntil( Time other )
  {
    Objects.requireNonNull( other, "other" );
    return other.milliseconds - milliseconds;
  }

  /**
   * Returns the absolute linear stored-time distance.
   *
   * @param other time to compare
   * @return non-negative difference in milliseconds
   * @throws NullPointerException if {@code other} is null
   */
  public int differenceInMillis( Time other )
  {
    return Math.abs( millisUntil( other ) );
  }

  // ================================= Formatting =================================

  /**
   * Formats this stored time using the current default locale and a {@link DateTimeFormatter}
   * pattern. Compatible local-time fields preserve the explicit {@code 24:00} endpoint; offset,
   * zone, instant, and date fields are unsupported when required by the pattern.
   *
   * @param pattern formatting pattern
   * @return formatted text
   * @throws NullPointerException if {@code pattern} is null
   * @throws IllegalArgumentException if the pattern is invalid
   * @throws DateTimeException if formatting fails, including for an unsupported field
   */
  public String format( String pattern )
  {
    return TemporalPatternSupport.formatTime( milliseconds, pattern );
  }

  /**
   * Formats this stored time with a precompiled formatter. Compatible local-time fields preserve
   * the explicit {@code 24:00} endpoint; formatters that require offset, zone, instant, or date
   * fields are unsupported.
   *
   * @param formatter formatter to use
   * @return formatted text
   * @throws NullPointerException if {@code formatter} is null
   * @throws DateTimeException if formatting fails, including for an unsupported field
   */
  public String format( DateTimeFormatter formatter )
  {
    return TemporalPatternSupport.formatTime( milliseconds, formatter );
  }

  /**
   * Formats one to four leading components as {@code HH}, {@code HH:mm}, {@code HH:mm:ss}, or
   * {@code HH:mm:ss.SSS}. Values below one produce the hour, values above four are ignored, and
   * stored {@code 24:00} retains hour 24.
   *
   * @param components requested component count
   * @return component text
   */
  public String formatComponents( int components )
  {
    StringBuilder result = new StringBuilder( 12 );
    appendTwoDigits( result, getHour() );
    if ( components >= 2 )
      appendTwoDigits( result.append( ':' ), getMinute() );
    if ( components >= 3 )
      appendTwoDigits( result.append( ':' ), getSecond() );
    if ( components >= 4 )
      appendThreeDigits( result.append( '.' ), getMillisecond() );
    return result.toString();
  }

  /** {@inheritDoc} */
  @Override
  public int compareTo( Time other )
  {
    Objects.requireNonNull( other, "other" );
    return Integer.compare( milliseconds, other.milliseconds );
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals( Object object )
  {
    return object instanceof Time other && milliseconds == other.milliseconds;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode()
  {
    return Integer.hashCode( milliseconds );
  }

  /** @return millisecond-precision component text in {@code HH:mm:ss.SSS} form */
  @Override
  public String toString()
  {
    return formatComponents( 4 );
  }

  private Time plusReduced( long amount, long unitMilliseconds )
  {
    if ( amount == 0L )
      return this;
    // Reduce first so extremely large caller values cannot overflow during unit conversion.
    long unitsPerDay = MILLIS_PER_DAY / unitMilliseconds;
    long reducedAmount = amount % unitsPerDay;
    return plusMilliseconds( reducedAmount * unitMilliseconds == 0L ? ( amount > 0L ? MILLIS_PER_DAY : -MILLIS_PER_DAY )
        : reducedAmount * unitMilliseconds );
  }

  private static void validate( int hour, int minute, int second, int millisecond )
  {
    if ( hour < 0 || hour > 24 )
      throw new DateTimeException( "Invalid hour: " + hour );
    if ( minute < 0 || minute > 59 )
      throw new DateTimeException( "Invalid minute: " + minute );
    if ( second < 0 || second > 59 )
      throw new DateTimeException( "Invalid second: " + second );
    if ( millisecond < 0 || millisecond > 999 )
      throw new DateTimeException( "Invalid millisecond: " + millisecond );
    if ( hour == 24 && ( minute | second | millisecond ) != 0 )
      throw new DateTimeException( "Only 24:00:00.000 is valid at hour 24" );
  }

  private static void appendTwoDigits( StringBuilder result, int value )
  {
    if ( value < 10 )
      result.append( '0' );
    result.append( value );
  }

  private static void appendThreeDigits( StringBuilder result, int value )
  {
    if ( value < 100 )
      result.append( '0' );
    if ( value < 10 )
      result.append( '0' );
    result.append( value );
  }
}
