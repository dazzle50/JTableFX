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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/*************************************************************************************************/
/******************************** Zone-free stored DateTime **************************************/
/*************************************************************************************************/

/**
 * Compact zone-free date-time retaining the explicit stored end-of-day notation {@code 24:00}.
 * Stored equality and ordering are lexicographical by epoch day and milliseconds of day. Serial
 * and Java-time conversion normalize {@code D 24:00} to {@code D+1 00:00}; this local timeline is
 * not an instant and carries no time zone.
 *
 * <p>Instances are immutable and thread-safe.</p>
 */
public final class DateTime implements Serializable, Comparable<DateTime>
{
  @Serial
  private static final long    serialVersionUID = 2L;

  private static final long    MIN_SERIAL       = (long) Integer.MIN_VALUE * Time.MILLIS_PER_DAY;
  private static final long    MAX_SERIAL       = ( (long) Integer.MAX_VALUE + 1L ) * Time.MILLIS_PER_DAY;

  /** Unix epoch coordinate, {@code 1970-01-01 00:00}. */
  public static final DateTime EPOCH            = new DateTime( 0, 0 );
  /** Earliest representable stored date-time. */
  public static final DateTime MIN_VALUE        = new DateTime( Integer.MIN_VALUE, 0 );
  /** Latest representable stored date-time, ending at explicit {@code 24:00}. */
  public static final DateTime MAX_VALUE        = new DateTime( Integer.MAX_VALUE, Time.MILLIS_PER_DAY );

  /** Stored epoch-day date component. */
  private final int            epochDay;
  /** Stored millisecond time component from 0 through one full day. */
  private final int            milliseconds;

  private DateTime( int epochDay, int milliseconds )
  {
    this.epochDay = epochDay;
    this.milliseconds = milliseconds;
  }

  // ================================= Factory methods =================================

  /**
   * Combines a compact date and stored time without normalizing {@code 24:00}.
   *
   * @param date stored date
   * @param time stored time
   * @return combined date-time
   * @throws NullPointerException if either argument is null
   */
  public static DateTime of( Date date, Time time )
  {
    Objects.requireNonNull( date, "date" );
    Objects.requireNonNull( time, "time" );
    return new DateTime( date.getEpochDay(), time.toMillisecondsOfDay() );
  }

  /**
   * Creates a second-precision date-time.
   *
   * @param year proleptic year
   * @param month month from 1 to 12
   * @param day day of month
   * @param hour hour from 0 through 24
   * @param minute minute from 0 to 59
   * @param second second from 0 to 59
   * @return represented date-time
   * @throws java.time.DateTimeException if any component combination is invalid
   * @throws ArithmeticException if the date is outside compact storage
   */
  public static DateTime of( int year, int month, int day, int hour, int minute, int second )
  {
    return of( year, month, day, hour, minute, second, 0 );
  }

  /**
   * Creates a millisecond-precision date-time.
   *
   * @param year proleptic year
   * @param month month from 1 to 12
   * @param day day of month
   * @param hour hour from 0 through 24
   * @param minute minute from 0 to 59
   * @param second second from 0 to 59
   * @param millisecond millisecond from 0 to 999
   * @return represented date-time
   * @throws java.time.DateTimeException if any component combination is invalid
   * @throws ArithmeticException if the date is outside compact storage
   */
  public static DateTime of( int year, int month, int day, int hour, int minute, int second, int millisecond )
  {
    return of( Date.of( year, month, day ), Time.of( hour, minute, second, millisecond ) );
  }

  /**
   * Converts a Java local date-time, truncating sub-millisecond precision.
   *
   * @param value source value
   * @return represented date-time
   * @throws NullPointerException if {@code value} is null
   * @throws ArithmeticException if the date is outside compact storage
   */
  public static DateTime of( LocalDateTime value )
  {
    Objects.requireNonNull( value, "value" );
    return of( Date.of( value.toLocalDate() ), Time.of( value.toLocalTime() ) );
  }

  /**
   * Creates the canonical stored representation of a serial local millisecond coordinate.
   * Interior exact-day boundaries become {@code 00:00}; the one-past-maximum boundary becomes
   * {@link #MAX_VALUE}.
   *
   * @param serialMilliseconds coordinate from {@code 1970-01-01T00:00}
   * @return the canonical representable value
   * @throws ArithmeticException if the coordinate is outside the inclusive supported range
   */
  public static DateTime ofSerialMilliseconds( long serialMilliseconds )
  {
    if ( serialMilliseconds < MIN_SERIAL || serialMilliseconds > MAX_SERIAL )
      throw new ArithmeticException( "Serial milliseconds outside compact storage: " + serialMilliseconds );
    if ( serialMilliseconds == MAX_SERIAL )
      return MAX_VALUE;
    long day = Math.floorDiv( serialMilliseconds, Time.MILLIS_PER_DAY );
    int millis = (int) Math.floorMod( serialMilliseconds, Time.MILLIS_PER_DAY );
    return new DateTime( EpochDayMath.checkedEpochDay( day ), millis );
  }

  /**
   * Obtains the current local date-time from the system-default clock.
   *
   * @return current local date-time
   */
  public static DateTime now()
  {
    return now( Clock.systemDefaultZone() );
  }

  /**
   * Obtains the current local date-time from a clock, truncating sub-millisecond precision.
   *
   * @param clock source of the current instant and time zone
   * @return the clock's current local date-time
   * @throws NullPointerException if {@code clock} is null
   * @throws ArithmeticException if the clock's date is outside compact storage
   */
  public static DateTime now( Clock clock )
  {
    return of( LocalDateTime.now( Objects.requireNonNull( clock, "clock" ) ) );
  }

  /**
   * Parses intelligent date-time text using current system defaults.
   *
   * @param text text to parse
   * @return parsed date-time
   * @throws NullPointerException if {@code text} is null
   * @throws java.time.format.DateTimeParseException if parsing fails
   * @throws ArithmeticException if relative arithmetic or compact storage overflows
   */
  public static DateTime parse( String text )
  {
    return DateTimeParser.systemDefault().parse( text );
  }

  // ================================= Component access =================================

  /**
   * Returns the stored date component.
   *
   * @return stored date component
   */
  public Date getDate()
  {
    return Date.ofEpochDay( epochDay );
  }

  /**
   * Returns the stored time component.
   *
   * @return stored time component, including an explicit {@code 24:00} endpoint
   */
  public Time getTime()
  {
    return Time.ofMilliseconds( milliseconds );
  }

  /**
   * Returns the stored epoch-day coordinate.
   *
   * @return stored epoch day
   */
  public int getEpochDay()
  {
    return epochDay;
  }

  /**
   * Returns the stored millisecond time coordinate.
   *
   * @return stored milliseconds of day from 0 through one full day
   */
  public int getMillisecondsOfDay()
  {
    return milliseconds;
  }

  /**
   * Replaces the stored date while preserving the stored time notation.
   *
   * @param date replacement date
   * @return adjusted value, or this instance if unchanged
   * @throws NullPointerException if {@code date} is null
   */
  public DateTime withDate( Date date )
  {
    Objects.requireNonNull( date, "date" );
    return date.getEpochDay() == epochDay ? this : new DateTime( date.getEpochDay(), milliseconds );
  }

  /**
   * Replaces the stored time while preserving the stored date.
   *
   * @param time replacement time
   * @return adjusted value, or this instance if unchanged
   * @throws NullPointerException if {@code time} is null
   */
  public DateTime withTime( Time time )
  {
    Objects.requireNonNull( time, "time" );
    int newMilliseconds = time.toMillisecondsOfDay();
    return newMilliseconds == milliseconds ? this : new DateTime( epochDay, newMilliseconds );
  }

  /**
   * Returns the canonical next-day-midnight representation of an interior stored {@code 24:00}.
   *
   * @return {@code this} for ordinary times, otherwise following-day {@code 00:00}
   * @throws ArithmeticException for {@link #MAX_VALUE}, whose effective day is not storable
   */
  public DateTime normalized()
  {
    if ( milliseconds != Time.MILLIS_PER_DAY )
      return this;
    return new DateTime( EpochDayMath.addDays( epochDay, 1L ), 0 );
  }

  /**
   * Returns the zone-free millisecond coordinate from {@code 1970-01-01T00:00}.
   *
   * @return normalized local serial milliseconds
   */
  public long toSerialMilliseconds()
  {
    return (long) epochDay * Time.MILLIS_PER_DAY + milliseconds;
  }

  /**
   * Converts to Java time; stored {@code 24:00} becomes following-day midnight.
   *
   * @return the normalized Java local date-time with millisecond precision
   */
  public LocalDateTime toLocalDateTime()
  {
    long effectiveDay = milliseconds == Time.MILLIS_PER_DAY ? (long) epochDay + 1L : epochDay;
    int effectiveMilliseconds = milliseconds == Time.MILLIS_PER_DAY ? 0 : milliseconds;
    return LocalDateTime.of( LocalDate.ofEpochDay( effectiveDay ),
        LocalTime.ofNanoOfDay( effectiveMilliseconds * 1_000_000L ) );
  }

  // =================================== Date-time arithmetic ===================================

  /**
   * Adds calendar days while retaining the stored time notation.
   *
   * @param days days to add
   * @return adjusted value
   * @throws ArithmeticException if arithmetic or compact storage overflows
   */
  public DateTime plusDays( long days )
  {
    return days == 0L ? this : new DateTime( EpochDayMath.addDays( epochDay, days ), milliseconds );
  }

  /**
   * Adds calendar weeks while retaining the stored time notation.
   *
   * @param weeks weeks to add
   * @return adjusted value
   * @throws ArithmeticException if arithmetic or compact storage overflows
   */
  public DateTime plusWeeks( long weeks )
  {
    return plusDays( Math.multiplyExact( weeks, 7L ) );
  }

  /**
   * Adds calendar months, clamping the date and retaining the stored time notation.
   *
   * @param months months to add
   * @return adjusted value
   * @throws ArithmeticException if arithmetic or compact storage overflows
   */
  public DateTime plusMonths( long months )
  {
    return months == 0L ? this : new DateTime( EpochDayMath.addMonths( epochDay, months ), milliseconds );
  }

  /**
   * Adds calendar years, clamping leap day and retaining the stored time notation.
   *
   * @param years years to add
   * @return adjusted value
   * @throws ArithmeticException if arithmetic or compact storage overflows
   */
  public DateTime plusYears( long years )
  {
    return years == 0L ? this : new DateTime( EpochDayMath.addYears( epochDay, years ), milliseconds );
  }

  /**
   * Adds a fixed duration with checked storage. A positive non-zero result landing at midnight is
   * stored as the preceding day at {@code 24:00}; a negative one is stored as the resulting day at
   * {@code 00:00}. Fixed-duration inverses and regrouping therefore preserve normalized position,
   * but need not preserve stored equality.
   *
   * @param amount milliseconds to add
   * @return the adjusted value, or this instance for zero
   * @throws ArithmeticException if arithmetic or compact storage overflows
   */
  public DateTime plusMilliseconds( long amount )
  {
    if ( amount == 0L )
      return this;
    long serial = Math.addExact( toSerialMilliseconds(), amount );
    if ( serial < MIN_SERIAL || serial > MAX_SERIAL )
      throw new ArithmeticException( "DateTime arithmetic outside compact storage" );
    // Preserve endpoint notation according to travel direction when landing exactly at midnight.
    if ( Math.floorMod( serial, Time.MILLIS_PER_DAY ) == 0L )
    {
      long effectiveDay = Math.floorDiv( serial, Time.MILLIS_PER_DAY );
      if ( amount > 0L )
        return new DateTime( EpochDayMath.checkedEpochDay( effectiveDay - 1L ), Time.MILLIS_PER_DAY );
      return new DateTime( EpochDayMath.checkedEpochDay( effectiveDay ), 0 );
    }
    return ofSerialMilliseconds( serial );
  }

  /**
   * Adds fixed-duration seconds on the normalized local timeline.
   *
   * @param seconds seconds to add
   * @return adjusted value
   * @throws ArithmeticException if multiplication, arithmetic, or storage overflows
   */
  public DateTime plusSeconds( long seconds )
  {
    return plusMilliseconds( Math.multiplyExact( seconds, Time.MILLIS_PER_SECOND ) );
  }

  /**
   * Adds fixed-duration minutes on the normalized local timeline.
   *
   * @param minutes minutes to add
   * @return adjusted value
   * @throws ArithmeticException if multiplication, arithmetic, or storage overflows
   */
  public DateTime plusMinutes( long minutes )
  {
    return plusMilliseconds( Math.multiplyExact( minutes, Time.MILLIS_PER_MINUTE ) );
  }

  /**
   * Adds fixed-duration hours on the normalized local timeline.
   *
   * @param hours hours to add
   * @return adjusted value
   * @throws ArithmeticException if multiplication, arithmetic, or storage overflows
   */
  public DateTime plusHours( long hours )
  {
    return plusMilliseconds( Math.multiplyExact( hours, Time.MILLIS_PER_HOUR ) );
  }

  /**
   * Adds either a calendar or fixed-duration interval. Calendar intervals use the stored date and
   * preserve the stored time notation; time intervals use {@link #plusMilliseconds(long)}.
   *
   * @param amount number of intervals to add
   * @param interval calendar or fixed-duration interval
   * @return the adjusted value
   * @throws NullPointerException if {@code interval} is null, including for zero amount
   * @throws ArithmeticException if arithmetic or compact storage overflows
   */
  public DateTime plus( long amount, DateTimeInterval interval )
  {
    Objects.requireNonNull( interval, "interval" );
    if ( amount == 0L )
      return this;
    if ( interval instanceof DateInterval dateInterval )
      return switch ( dateInterval )
      {
        case DAY -> plusDays( amount );
        case WEEK -> plusWeeks( amount );
        case MONTH -> plusMonths( amount );
        case QUARTER_YEAR -> plusMonths( Math.multiplyExact( amount, 3L ) );
        case HALF_YEAR -> plusMonths( Math.multiplyExact( amount, 6L ) );
        case YEAR -> plusYears( amount );
      };
    TimeInterval timeInterval = (TimeInterval) interval;
    return plusMilliseconds( Math.multiplyExact( amount, timeInterval.milliseconds() ) );
  }

  /**
   * Returns the start boundary selected by a calendar period or fixed-duration time grid.
   *
   * <ul>
   *   <li>For a {@link DateInterval}, returns {@code 00:00} on the first stored date of the
   *       containing calendar period. The stored date is used without normalizing {@code 24:00};
   *       therefore {@code 2026-08-20 24:00.startOf(DAY)} is
   *       {@code 2026-08-20 00:00}, not {@code 2026-08-21 00:00}.</li>
   *   <li>For a {@link TimeInterval}, preserves the stored date and returns the nearest aligned
   *       time at or before the stored time. For example,
   *       {@code 2026-08-20 10:37.startOf(HALF_HOUR)} is
   *       {@code 2026-08-20 10:30}.</li>
   * </ul>
   *
   * <p>This method returns {@code this} when it already represents the selected boundary. Every
   * aligned time, including stored {@code 24:00}, is unchanged for a {@code TimeInterval}; for a
   * {@code DateInterval}, both the first stored date and {@code 00:00} must match.</p>
   *
   * @param interval calendar period or fixed-duration alignment grid
   * @return the selected start boundary, or {@code this} when already at that boundary
   * @throws NullPointerException if {@code interval} is null
   * @throws ArithmeticException if a calendar-period start is outside compact date storage
   */
  public DateTime startOf( DateTimeInterval interval )
  {
    Objects.requireNonNull( interval, "interval" );
    if ( interval instanceof DateInterval dateInterval )
    {
      int day = EpochDayMath.checkedEpochDay( EpochDayMath.periodStart( epochDay, dateInterval ) );
      return day == epochDay && milliseconds == 0 ? this : new DateTime( day, 0 );
    }
    Time aligned = getTime().startOf( (TimeInterval) interval );
    return aligned.toMillisecondsOfDay() == milliseconds ? this
        : new DateTime( epochDay, aligned.toMillisecondsOfDay() );
  }

  /**
   * Returns the end boundary selected by a calendar period or fixed-duration time grid.
   *
   * <ul>
   *   <li>For a {@link DateInterval}, returns explicit {@code 24:00} on the last stored date of the
   *       containing calendar period. The stored date is used without normalization; for example,
   *       {@code 2026-08-20 10:37.endOf(MONTH)} is
   *       {@code 2026-08-31 24:00}, and {@code endOf(DAY)} is
   *       {@code 2026-08-20 24:00}.</li>
   *   <li>For a {@link TimeInterval}, preserves the stored date and returns the nearest aligned
   *       time at or after the stored time. For example,
   *       {@code 2026-08-20 10:37.endOf(HALF_HOUR)} is
   *       {@code 2026-08-20 11:00}.</li>
   * </ul>
   *
   * <p>This method returns {@code this} when it already represents the selected boundary. Every
   * aligned time, including stored {@code 24:00}, is unchanged for a {@code TimeInterval}; for a
   * {@code DateInterval}, both the last stored date and {@code 24:00} must match. A calendar result
   * deliberately retains the ending {@code 24:00} notation rather than normalizing it to the next
   * date at {@code 00:00}.</p>
   *
   * @param interval calendar period or fixed-duration alignment grid
   * @return the selected end boundary, or {@code this} when already at that boundary
   * @throws NullPointerException if {@code interval} is null
   * @throws ArithmeticException if a calendar-period end is outside compact date storage
   */
  public DateTime endOf( DateTimeInterval interval )
  {
    Objects.requireNonNull( interval, "interval" );
    if ( interval instanceof DateInterval dateInterval )
    {
      int day = EpochDayMath.checkedEpochDay( EpochDayMath.periodEnd( epochDay, dateInterval ) );
      return day == epochDay && milliseconds == Time.MILLIS_PER_DAY ? this : new DateTime( day, Time.MILLIS_PER_DAY );
    }
    Time aligned = getTime().endOf( (TimeInterval) interval );
    return aligned.toMillisecondsOfDay() == milliseconds ? this
        : new DateTime( epochDay, aligned.toMillisecondsOfDay() );
  }

  // ================================= Comparison and distance ================================

  /**
   * Tests lexicographical stored ordering without normalizing {@code 24:00}.
   *
   * @param other value to compare
   * @return whether this stored value is earlier
   * @throws NullPointerException if {@code other} is null
   */
  public boolean isBefore( DateTime other )
  {
    return compareTo( other ) < 0;
  }

  /**
   * Tests lexicographical stored ordering without normalizing {@code 24:00}.
   *
   * @param other value to compare
   * @return whether this stored value is later
   * @throws NullPointerException if {@code other} is null
   */
  public boolean isAfter( DateTime other )
  {
    return compareTo( other ) > 0;
  }

  /**
   * Tests membership in an inclusive, non-wrapping stored-value range.
   *
   * @param start inclusive lower bound
   * @param end inclusive upper bound
   * @return whether this value is within the bounds
   * @throws NullPointerException if either bound is null
   */
  public boolean isBetween( DateTime start, DateTime end )
  {
    Objects.requireNonNull( start, "start" );
    Objects.requireNonNull( end, "end" );
    return compareTo( start ) >= 0 && compareTo( end ) <= 0;
  }

  /**
   * Tests normalized position on the zone-free local timeline; this is not instant equality.
   *
   * @param other value to compare
   * @return whether both values have the same normalized serial coordinate
   * @throws NullPointerException if {@code other} is null
   */
  public boolean hasSameTimelinePosition( DateTime other )
  {
    Objects.requireNonNull( other, "other" );
    return toSerialMilliseconds() == other.toSerialMilliseconds();
  }

  /**
   * Returns the signed elapsed distance on the normalized local timeline.
   *
   * @param other target value
   * @return {@code other - this} in milliseconds
   * @throws NullPointerException if {@code other} is null
   */
  public long millisecondsUntil( DateTime other )
  {
    Objects.requireNonNull( other, "other" );
    return other.toSerialMilliseconds() - toSerialMilliseconds();
  }

  /**
   * Returns the absolute elapsed distance on the normalized local timeline.
   *
   * @param other value to compare
   * @return non-negative difference in milliseconds
   * @throws NullPointerException if {@code other} is null
   */
  public long differenceInMillis( DateTime other )
  {
    return Math.abs( millisecondsUntil( other ) );
  }

  // ================================= Formatting =================================

  /**
   * Formats the combined value using the current default locale and one date-time pattern.
   * Compatible local date/time fields preserve the stored date and explicit {@code 24:00}
   * notation. The date portion supports the {@code U}, {@code UU}, and {@code UUU} half-year
   * extension described by {@link Date#format(String)}. Offset, zone, and instant fields are
   * unsupported when required by the pattern.
   *
   * @param pattern formatting pattern
   * @return formatted text
   * @throws NullPointerException if {@code pattern} is null
   * @throws IllegalArgumentException if the pattern is invalid
   * @throws java.time.DateTimeException if formatting fails, including for an unsupported field
   */
  public String format( String pattern )
  {
    return TemporalPatternSupport.formatDateTime( epochDay, milliseconds, pattern );
  }

  /**
   * Formats the combined value with a precompiled formatter. Compatible local date/time fields
   * preserve the stored date and explicit {@code 24:00} notation; formatters that require offset,
   * zone, or instant fields are unsupported.
   *
   * @param formatter formatter to use
   * @return formatted text
   * @throws NullPointerException if {@code formatter} is null
   * @throws java.time.DateTimeException if formatting fails, including for an unsupported field
   */
  public String format( DateTimeFormatter formatter )
  {
    return TemporalPatternSupport.formatDateTime( epochDay, milliseconds, formatter );
  }

  /**
   * Formats date and time independently using the current default locale and joins them with one
   * space. The date pattern supports the half-year extension, and the time pattern preserves
   * explicit {@code 24:00}.
   *
   * @param datePattern date formatting pattern
   * @param timePattern time formatting pattern
   * @return combined formatted text
   * @throws NullPointerException if either pattern is null
   * @throws IllegalArgumentException if either pattern is invalid
   * @throws java.time.DateTimeException if formatting fails, including for an unsupported field
   */
  public String format( String datePattern, String timePattern )
  {
    return getDate().format( datePattern ) + " " + getTime().format( timePattern );
  }

  /**
   * Formats a patterned date and a component-count time, joined with one space. The date pattern
   * uses the current default locale and supports the half-year extension described by
   * {@link Date#format(String)}.
   *
   * @param datePattern date formatting pattern
   * @param timeComponents requested time component count
   * @return combined formatted text
   * @throws NullPointerException if {@code datePattern} is null
   * @throws IllegalArgumentException if {@code datePattern} is invalid
   * @throws java.time.DateTimeException if formatting fails, including for an unsupported field
   */
  public String format( String datePattern, int timeComponents )
  {
    return getDate().format( datePattern ) + " " + getTime().formatComponents( timeComponents );
  }

  /** {@inheritDoc} */
  @Override
  public int compareTo( DateTime other )
  {
    Objects.requireNonNull( other, "other" );
    int dateComparison = Integer.compare( epochDay, other.epochDay );
    return dateComparison != 0 ? dateComparison : Integer.compare( milliseconds, other.milliseconds );
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals( Object object )
  {
    return object instanceof DateTime other && epochDay == other.epochDay && milliseconds == other.milliseconds;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode()
  {
    return 31 * Integer.hashCode( epochDay ) + Integer.hashCode( milliseconds );
  }

  /** @return date and millisecond-precision time joined with one space */
  @Override
  public String toString()
  {
    return getDate() + " " + getTime();
  }
}
