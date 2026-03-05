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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Objects;

/*************************************************************************************************/
/*********************************** DateTime (with no time-zone) ********************************/
/*************************************************************************************************/

/**
 * Immutable date-time without timezone information.
 * <p>
 * Stored as two {@code int} fields: {@code m_epochDay} (days since 1970-01-01) and
 * {@code m_milliseconds} (milliseconds since midnight), mirroring {@link Date} and
 * {@link Time} respectively.
 * <p>
 * Distinguishes {@code 24:00} (end of current date) from {@code 00:00} (start of next date);
 * {@code 24:00} of day N orders before {@code 00:00} of day N+1.
 *
 * @see Date
 * @see Time
 */
public final class DateTime implements Serializable, Comparable<DateTime>
{
  private static final long    serialVersionUID = 1L;

  // date & time components: days since 1970-01-01 and milliseconds since midnight
  private final int            m_epochDay;
  private final int            m_milliseconds;

  // ---- sentinel constants ----
  public static final DateTime EPOCH            = new DateTime( 0, 0 );
  public static final DateTime MIN_VALUE        = new DateTime( Integer.MIN_VALUE, 0 );
  public static final DateTime MAX_VALUE        = new DateTime( Integer.MAX_VALUE, Time.MILLIS_PER_DAY );

  /**
   * Interval units for truncation, stepping, and rounding.
   */
  public enum IntervalUnit
  {
    YEAR, HALF_YEAR, QUARTER_YEAR, MONTH, WEEK, DAY, HALF_DAY, QUARTER_DAY, HOUR, HALF_HOUR, TEN_MINUTE, MINUTE, SECOND
  }

  // ================================= Constructor =================================

  /***************************************** constructor *****************************************/
  /**
   * Private constructor — use factory methods to create instances.
   *
   * @param epochDay     days since 1970-01-01 (may be negative)
   * @param milliseconds milliseconds since midnight (0 to {@link Time#MILLIS_PER_DAY} inclusive)
   */
  private DateTime( int epochDay, int milliseconds )
  {
    m_epochDay = epochDay;
    m_milliseconds = milliseconds;
  }

  // ================================= Factory Methods =================================

  /********************************************** of **********************************************/
  /**
   * Creates a {@code DateTime} from separate {@link Date} and {@link Time} components.
   *
   * @param date the date component, must not be null
   * @param time the time component, must not be null
   * @return a new {@code DateTime} instance
   * @throws NullPointerException if either argument is null
   */
  public static DateTime of( Date date, Time time )
  {
    // extract raw ints directly — no intermediate allocation
    return new DateTime( date.getEpochDay(), time.toMillisecondsOfDay() );
  }

  /********************************************** of **********************************************/
  /**
   * Creates a {@code DateTime} from individual date and time field values.
   *
   * @param year   proleptic Gregorian year
   * @param month  month (1 = January … 12 = December)
   * @param day    day-of-month (1–31 depending on month and year)
   * @param hour   hour (0–23)
   * @param minute minute (0–59)
   * @param second second (0–59)
   * @return a new {@code DateTime} instance
   * @throws java.time.DateTimeException if any field value is invalid
   */
  public static DateTime of( int year, int month, int day, int hour, int minute, int second )
  {
    // delegate validation to Date.of and Time.of
    return new DateTime( Date.of( year, month, day ).getEpochDay(),
        Time.of( hour, minute, second ).toMillisecondsOfDay() );
  }

  /********************************************** of **********************************************/
  /**
   * Creates a {@code DateTime} from a {@link LocalDateTime}.
   *
   * @param ldt the {@code LocalDateTime} to convert, must not be null
   * @return a new {@code DateTime} instance
   * @throws NullPointerException if {@code ldt} is null
   */
  public static DateTime of( LocalDateTime ldt )
  {
    Objects.requireNonNull( ldt, "ldt must not be null" );
    return new DateTime( (int) ldt.toLocalDate().toEpochDay(), ldt.toLocalTime().get( ChronoField.MILLI_OF_DAY ) );
  }

  /********************************************* now **********************************************/
  /**
   * Creates a {@code DateTime} representing the current system date and time.
   *
   * @return a {@code DateTime} for the current moment
   */
  public static DateTime now()
  {
    return ofMilliseconds( System.currentTimeMillis() );
  }

  /******************************************** parse *********************************************/
  /**
   * Parses a date-time string with automatic format detection.
   * <p>
   * Splits on the last {@code ' '} or {@code 'T'} separator (tried in order), then delegates
   * each part to {@link DateParser} and {@link TimeParser} respectively.
   *
   * @param text the date-time string to parse, must not be null
   * @return a new {@code DateTime} instance
   * @throws NullPointerException   if {@code text} is null
   * @throws DateTimeParseException if the text cannot be parsed in any supported format
   */
  public static DateTime parse( String text )
  {
    Objects.requireNonNull( text, "text must not be null" );
    String trimmed = text.trim();

    // try common date-time separators in order of preference
    for ( String sep : new String[] { " ", "T" } )
    {
      int idx = trimmed.lastIndexOf( sep );
      if ( idx > 0 )
      {
        try
        {
          Date date = DateParser.parse( trimmed.substring( 0, idx ) );
          Time time = TimeParser.parse( trimmed.substring( idx + 1 ) );
          return new DateTime( date.getEpochDay(), time.toMillisecondsOfDay() );
        }
        catch ( IllegalArgumentException | DateTimeParseException ignored )
        {
          // try next separator
        }
      }
    }

    throw new DateTimeParseException( "Unable to parse DateTime: " + text, trimmed, 0 );
  }

  // ================================= Accessor Methods =================================

  /******************************************* getDate ********************************************/
  /**
   * Returns the date component.
   *
   * @return the date component, never null
   */
  public Date getDate()
  {
    return Date.ofEpochDay( m_epochDay );
  }

  /******************************************* getTime ********************************************/
  /**
   * Returns the time component.
   *
   * @return the time component, never null
   */
  public Time getTime()
  {
    return Time.ofMilliseconds( m_milliseconds );
  }

  /****************************************** getEpochDay *****************************************/
  /**
   * Returns the date component as an epoch-day, the number of days since {@code 1970-01-01}.
   *
   * @return the epoch-day value (days since {@code 1970-01-01})
   */
  public int getEpochDay()
  {
    return m_epochDay;
  }

  /************************************** getMillisecondsOfDay ************************************/
  /**
   * Returns the time-of-day component as milliseconds since midnight start of day.
   * <p>
   * The value is in the range {@code [0, Time.MILLIS_PER_DAY]}.
   * A value of {@code Time.MILLIS_PER_DAY} represents {@code 24:00:00.000} (end of day).
   *
   * @return milliseconds since midnight in {@code [0, Time.MILLIS_PER_DAY]}
   */
  public int getMillisecondsOfDay()
  {
    return m_milliseconds;
  }

  /******************************************* withDate *******************************************/
  /**
   * Returns a copy with the date component replaced.
   *
   * @param newDate the replacement date, must not be null
   * @return a new {@code DateTime} with the specified date and the same time
   * @throws NullPointerException if {@code newDate} is null
   */
  public DateTime withDate( Date newDate )
  {
    Objects.requireNonNull( newDate, "newDate must not be null" );
    return new DateTime( newDate.getEpochDay(), m_milliseconds );
  }

  /******************************************* withTime *******************************************/
  /**
   * Returns a copy with the time component replaced.
   *
   * @param newTime the replacement time, must not be null
   * @return a new {@code DateTime} with the same date and the specified time
   * @throws NullPointerException if {@code newTime} is null
   */
  public DateTime withTime( Time newTime )
  {
    Objects.requireNonNull( newTime, "newTime must not be null" );
    return new DateTime( m_epochDay, newTime.toMillisecondsOfDay() );
  }

  // ================================= Arithmetic Methods =================================

  /******************************************* plusDays *******************************************/
  /**
   * Returns a copy with the specified number of days added, preserving the time component.
   *
   * @param days days to add (may be negative)
   * @return {@code this} if {@code days == 0}, otherwise a new {@code DateTime}
   */
  public DateTime plusDays( int days )
  {
    if ( days == 0 )
      return this;
    return new DateTime( m_epochDay + days, m_milliseconds );
  }

  /*************************************** plusMilliseconds ***************************************/
  /**
   * Returns a copy with the specified number of milliseconds added.
   * <p>
   * Day-boundary overflow and underflow are handled via floor division so negative values
   * behave correctly.
   *
   * @param millis milliseconds to add (may be negative)
   * @return {@code this} if {@code millis == 0}, otherwise a new {@code DateTime}
   */
  public DateTime plusMilliseconds( long millis )
  {
    if ( millis == 0 )
      return this;

    // floor division ensures correct handling of negative values
    long total = m_milliseconds + millis;
    int dayShift = (int) Math.floorDiv( total, Time.MILLIS_PER_DAY );
    int newMillis = Math.floorMod( total, Time.MILLIS_PER_DAY );
    return new DateTime( m_epochDay + dayShift, newMillis );
  }

  /***************************************** plusSeconds ******************************************/
  /**
   * Returns a copy with the specified number of seconds added.
   *
   * @param seconds seconds to add (may be negative)
   * @return a new {@code DateTime} with seconds added
   */
  public DateTime plusSeconds( int seconds )
  {
    return plusMilliseconds( (long) seconds * Time.MILLIS_PER_SECOND );
  }

  /***************************************** plusMinutes ******************************************/
  /**
   * Returns a copy with the specified number of minutes added.
   *
   * @param minutes minutes to add (may be negative)
   * @return a new {@code DateTime} with minutes added
   */
  public DateTime plusMinutes( int minutes )
  {
    return plusMilliseconds( (long) minutes * Time.MILLIS_PER_MINUTE );
  }

  /****************************************** plusHours *******************************************/
  /**
   * Returns a copy with the specified number of hours added, adjusting the date if a day
   * boundary is crossed.
   *
   * @param hours hours to add (may be negative)
   * @return a new {@code DateTime} with hours added
   */
  public DateTime plusHours( int hours )
  {
    return plusMilliseconds( (long) hours * Time.MILLIS_PER_HOUR );
  }

  /****************************************** plusWeeks *******************************************/
  /**
   * Returns a copy with the specified number of weeks added, preserving the time component.
   *
   * @param weeks weeks to add (may be negative)
   * @return a new {@code DateTime} with weeks added
   */
  public DateTime plusWeeks( int weeks )
  {
    return plusDays( weeks * 7 );
  }

  /****************************************** plusMonths ******************************************/
  /**
   * Returns a copy with the specified number of months added.
   * <p>
   * Month-end clamping applies: adding one month to 31 January yields 28/29 February.
   *
   * @param months months to add (may be negative)
   * @return {@code this} if {@code months == 0}, otherwise a new {@code DateTime}
   */
  public DateTime plusMonths( int months )
  {
    if ( months == 0 )
      return this;
    // delegate to Date static helper — no intermediate Date object needed
    return new DateTime( Date.addMonths( m_epochDay, months ), m_milliseconds );
  }

  /****************************************** plusYears *******************************************/
  /**
   * Returns a copy with the specified number of years added.
   * <p>
   * Leap-year clamping applies: adding one year to 29 February yields 28 February in a
   * non-leap year.
   *
   * @param years years to add (may be negative)
   * @return {@code this} if {@code years == 0}, otherwise a new {@code DateTime}
   */
  public DateTime plusYears( int years )
  {
    if ( years == 0 )
      return this;
    // delegate to Date static helper — no intermediate Date object needed
    return new DateTime( Date.addYears( m_epochDay, years ), m_milliseconds );
  }

  /***************************************** plusInterval *****************************************/
  /**
   * Returns a copy with the specified interval added.
   * <p>
   * Calendar-based units ({@code YEAR}, {@code HALF_YEAR}, {@code QUARTER_YEAR}, {@code MONTH})
   * account for varying month lengths. All other units use millisecond arithmetic.
   *
   * @param amount amount to add (may be negative)
   * @param unit   interval unit, must not be null
   * @return {@code this} if {@code amount == 0}, otherwise a new {@code DateTime}
   * @throws NullPointerException if {@code unit} is null
   */
  public DateTime plusInterval( int amount, IntervalUnit unit )
  {
    Objects.requireNonNull( unit, "unit must not be null" );
    if ( amount == 0 )
      return this;

    return switch ( unit )
    {
      case YEAR -> plusYears( amount );
      case HALF_YEAR -> plusMonths( amount * 6 );
      case QUARTER_YEAR -> plusMonths( amount * 3 );
      case MONTH -> plusMonths( amount );
      case WEEK -> plusDays( amount * 7 );
      case DAY -> plusDays( amount );
      case HALF_DAY -> plusMilliseconds( (long) amount * 12 * Time.MILLIS_PER_HOUR );
      case QUARTER_DAY -> plusMilliseconds( (long) amount * 6 * Time.MILLIS_PER_HOUR );
      case HOUR -> plusHours( amount );
      case HALF_HOUR -> plusMinutes( 30 * amount );
      case TEN_MINUTE -> plusMinutes( 10 * amount );
      case MINUTE -> plusMinutes( amount );
      case SECOND -> plusSeconds( amount );
    };
  }

  // ================================= Rounding Methods =================================

  /***************************************** toDateUnit *******************************************/
  // maps calendar-level DateTime interval units to their Date.IntervalUnit equivalents
  private static Date.IntervalUnit toDateUnit( IntervalUnit unit )
  {
    return switch ( unit )
    {
      case YEAR -> Date.IntervalUnit.YEAR;
      case HALF_YEAR -> Date.IntervalUnit.HALF_YEAR;
      case QUARTER_YEAR -> Date.IntervalUnit.QUARTER_YEAR;
      case MONTH -> Date.IntervalUnit.MONTH;
      case WEEK -> Date.IntervalUnit.WEEK;
      default -> throw new IllegalArgumentException( "no date unit for: " + unit );
    };
  }

  /****************************************** roundDown *******************************************/
  /**
   * Returns a copy truncated (rounded down) to the specified unit boundary.
   * <p>
   * Returns {@code this} if already aligned. Calendar-based units ({@code WEEK} through
   * {@code YEAR}) delegate to {@link Date#roundDown(Date.IntervalUnit)} to avoid duplicating
   * calendar arithmetic.
   *
   * @param unit the unit to truncate to, must not be null
   * @return {@code this} if already aligned, otherwise a new {@code DateTime}
   * @throws NullPointerException if {@code unit} is null
   */
  public DateTime roundDown( IntervalUnit unit )
  {
    Objects.requireNonNull( unit, "unit must not be null" );

    return switch ( unit )
    {
      case SECOND -> {
        int t = ( m_milliseconds / Time.MILLIS_PER_SECOND ) * Time.MILLIS_PER_SECOND;
        yield t == m_milliseconds ? this : new DateTime( m_epochDay, t );
      }

      case MINUTE -> {
        int t = ( m_milliseconds / Time.MILLIS_PER_MINUTE ) * Time.MILLIS_PER_MINUTE;
        yield t == m_milliseconds ? this : new DateTime( m_epochDay, t );
      }

      case TEN_MINUTE -> {
        int t = ( m_milliseconds / Time.MILLIS_PER_MINUTE / 10 ) * 10 * Time.MILLIS_PER_MINUTE;
        yield t == m_milliseconds ? this : new DateTime( m_epochDay, t );
      }

      case HALF_HOUR -> {
        int t = ( m_milliseconds / Time.MILLIS_PER_MINUTE / 30 ) * 30 * Time.MILLIS_PER_MINUTE;
        yield t == m_milliseconds ? this : new DateTime( m_epochDay, t );
      }

      case HOUR -> {
        int t = ( m_milliseconds / Time.MILLIS_PER_HOUR ) * Time.MILLIS_PER_HOUR;
        yield t == m_milliseconds ? this : new DateTime( m_epochDay, t );
      }

      case QUARTER_DAY -> {
        // 6-hour boundaries: 00:00, 06:00, 12:00, 18:00
        int t = ( m_milliseconds / ( 6 * Time.MILLIS_PER_HOUR ) ) * ( 6 * Time.MILLIS_PER_HOUR );
        yield t == m_milliseconds ? this : new DateTime( m_epochDay, t );
      }

      case HALF_DAY -> {
        // 12-hour boundaries: 00:00 and 12:00
        int t = ( m_milliseconds / ( 12 * Time.MILLIS_PER_HOUR ) ) * ( 12 * Time.MILLIS_PER_HOUR );
        yield t == m_milliseconds ? this : new DateTime( m_epochDay, t );
      }

      case DAY -> m_milliseconds == 0 ? this : new DateTime( m_epochDay, 0 );

      // calendar units delegate entirely to Date.roundDown to avoid duplicating logic
      case WEEK, MONTH, QUARTER_YEAR, HALF_YEAR, YEAR -> {
        int newDay = Date.ofEpochDay( m_epochDay ).roundDown( toDateUnit( unit ) ).getEpochDay();
        yield ( newDay == m_epochDay && m_milliseconds == 0 ) ? this : new DateTime( newDay, 0 );
      }
    };
  }

  /******************************************* roundUp ********************************************/
  /**
   * Returns a copy rounded up (ceiling) to the specified unit boundary.
   * <p>
   * Returns {@code this} if already aligned.
   *
   * @param unit the unit to round up to, must not be null
   * @return {@code this} if already aligned, otherwise the next unit boundary
   * @throws NullPointerException if {@code unit} is null
   */
  public DateTime roundUp( IntervalUnit unit )
  {
    DateTime truncated = roundDown( unit );

    // if already on a boundary, return this unchanged
    if ( equals( truncated ) )
      return this;

    // advance to next boundary
    return truncated.plusInterval( 1, unit );
  }

  // ================================= Conversion Methods =================================

  /**************************************** toLocalDateTime ***************************************/
  /**
   * Converts this {@code DateTime} to a {@link LocalDateTime}.
   * <p>
   * The time component {@code 24:00:00.000} is normalised to {@code 00:00:00.000} of the
   * following day, since {@link LocalDateTime} has no representation for end-of-day.
   *
   * @return equivalent {@link LocalDateTime}
   */
  public LocalDateTime toLocalDateTime()
  {
    // normalise 24:00 to 00:00 next day before converting
    int epochDay = m_milliseconds == Time.MILLIS_PER_DAY ? m_epochDay + 1 : m_epochDay;
    int millis = m_milliseconds == Time.MILLIS_PER_DAY ? 0 : m_milliseconds;
    return LocalDateTime.of( Date.ofEpochDay( epochDay ).toLocalDate(), Time.ofMilliseconds( millis ).toLocalTime() );
  }

  /**************************************** toMilliseconds ****************************************/
  /**
   * Converts this {@code DateTime} to milliseconds since the Unix epoch (1970-01-01T00:00:00.000).
   *
   * @return milliseconds since the Unix epoch
   */
  public long toMilliseconds()
  {
    // combine epoch-day and millis-of-day for total epoch milliseconds
    return (long) m_epochDay * Time.MILLIS_PER_DAY + m_milliseconds;
  }

  /**************************************** ofMilliseconds ****************************************/
  /**
   * Creates a {@code DateTime} from milliseconds since the Unix epoch (1970-01-01T00:00:00.000).
   * <p>
   * Inverse of {@link #toMilliseconds()}.
   * <p>
   * The resulting epoch day is cast to {@code int}. Values beyond approximately ±2.15 billion
   * days (±5.8 million years) will overflow silently, consistent with the range documented for
   * {@link Date}.
   *
   * @param epochMillis milliseconds since the Unix epoch
   * @return a new {@code DateTime}
   * @throws ArithmeticException if the epoch-day component overflows {@code int}
   */
  public static DateTime ofMilliseconds( long epochMillis )
  {
    // floor-divide to handle pre-epoch (negative) values correctly
    long epochDay = Math.floorDiv( epochMillis, Time.MILLIS_PER_DAY );
    if ( epochDay < Integer.MIN_VALUE || epochDay > Integer.MAX_VALUE )
      throw new ArithmeticException( "epoch day overflow: " + epochDay );
    int millis = Math.floorMod( epochMillis, Time.MILLIS_PER_DAY );
    return new DateTime( (int) epochDay, millis );
  }

  // ================================= Comparison Methods =================================

  /******************************************* isBefore *******************************************/
  /**
   * Returns {@code true} if this is chronologically before {@code other}.
   *
   * @param other the {@code DateTime} to compare against, must not be null
   * @return {@code true} if this is strictly before {@code other}
   * @throws NullPointerException if {@code other} is null
   */
  public boolean isBefore( DateTime other )
  {
    return compareTo( other ) < 0;
  }

  /******************************************* isAfter ********************************************/
  /**
   * Returns {@code true} if this is chronologically after {@code other}.
   *
   * @param other the {@code DateTime} to compare against, must not be null
   * @return {@code true} if this is strictly after {@code other}
   * @throws NullPointerException if {@code other} is null
   */
  public boolean isAfter( DateTime other )
  {
    return compareTo( other ) > 0;
  }

  /******************************************* isEqual ********************************************/
  /**
   * Returns {@code true} if this represents the same instant as {@code other}.
   * <p>
   * Null-safe: returns {@code false} rather than throwing when {@code other} is null.
   *
   * @param other the {@code DateTime} to compare (may be null)
   * @return {@code true} if both fields are identical
   */
  public boolean isEqual( DateTime other )
  {
    return other != null && m_epochDay == other.m_epochDay && m_milliseconds == other.m_milliseconds;
  }

  /****************************************** isBetween *******************************************/
  /**
   * Returns {@code true} if this falls within the inclusive range {@code [start, end]}.
   * <p>
   * Returns {@code false} if {@code start} is after {@code end} (empty range).
   *
   * @param start range start, inclusive (must not be null)
   * @param end   range end, inclusive (must not be null)
   * @return {@code true} if {@code start <= this <= end}
   * @throws NullPointerException if either argument is null
   */
  public boolean isBetween( DateTime start, DateTime end )
  {
    Objects.requireNonNull( start, "start must not be null" );
    Objects.requireNonNull( end, "end must not be null" );
    return compareTo( start ) >= 0 && compareTo( end ) <= 0;
  }

  /****************************************** compareTo *******************************************/
  /**
   * Compares this {@code DateTime} to another for chronological ordering.
   * <p>
   * Epoch-day is compared first; milliseconds-of-day is the tiebreaker. Note that
   * {@code 24:00} of day N orders before {@code 00:00} of day N+1.
   *
   * @param other the {@code DateTime} to compare to, must not be null
   * @return negative if before, zero if equal, positive if after
   * @throws NullPointerException if {@code other} is null
   */
  @Override
  public int compareTo( DateTime other )
  {
    // compare epoch days first — the common fast path
    int cmp = Integer.compare( m_epochDay, other.m_epochDay );
    if ( cmp != 0 )
      return cmp;

    // same day — tiebreak on milliseconds within the day
    return Integer.compare( m_milliseconds, other.m_milliseconds );
  }

  // ================================= Difference Methods =================================

  /************************************** millisecondsUntil ***************************************/
  /**
   * Returns the signed number of milliseconds from this instant to {@code other}.
   * <p>
   * Positive if {@code other} is later, negative if earlier.
   *
   * @param other the target instant, must not be null
   * @return signed millisecond difference
   * @throws NullPointerException if {@code other} is null
   */
  public long millisecondsUntil( DateTime other )
  {
    Objects.requireNonNull( other, "other must not be null" );
    return other.toMilliseconds() - toMilliseconds();
  }

  /************************************** differenceInMillis **************************************/
  /**
   * Returns the absolute number of milliseconds between this instant and {@code other}.
   * <p>
   * Always non-negative. For a signed difference use {@link #millisecondsUntil}.
   *
   * @param other the instant to compare with, must not be null
   * @return absolute millisecond difference
   * @throws NullPointerException if {@code other} is null
   */
  public long differenceInMillis( DateTime other )
  {
    Objects.requireNonNull( other, "other must not be null" );
    return Math.abs( toMilliseconds() - other.toMilliseconds() );
  }

  // ================================= Object Methods =================================

  /******************************************** equals ********************************************/
  /**
   * Returns {@code true} if {@code obj} is a {@code DateTime} with identical fields.
   *
   * @param obj the object to compare with (may be null)
   * @return {@code true} if both epoch-day and milliseconds are equal
   */
  @Override
  public boolean equals( Object obj )
  {
    return obj instanceof DateTime other && m_epochDay == other.m_epochDay && m_milliseconds == other.m_milliseconds;
  }

  /******************************************* hashCode *******************************************/
  /**
   * Returns a hash code for this {@code DateTime}.
   *
   * @return hash code derived from both internal fields
   */
  @Override
  public int hashCode()
  {
    // multiply one field and XOR with the other for a fast, well-distributed hash
    return m_epochDay * 1_000_003 ^ m_milliseconds;
  }

  /******************************************* toString *******************************************/
  /**
   * Returns this date-time like {@code "yyyy-MM-dd HH:mm:ss.SSS"}.
   *
   * @return ISO-style like date-time string
   */
  @Override
  public String toString()
  {
    return Date.ofEpochDay( m_epochDay ).toString() + " " + Time.ofMilliseconds( m_milliseconds ).toString();
  }

  /******************************************* toString *******************************************/
  /**
   * Returns this date-time formatted using separate date and time patterns, joined by a space.
   *
   * @param datePattern pattern for the date portion (see {@link Date#toString(String)})
   * @param timePattern pattern for the time portion (see {@link Time#toString(String)})
   * @return formatted date-time string
   */
  public String toString( String datePattern, String timePattern )
  {
    return Date.ofEpochDay( m_epochDay ).toString( datePattern ) + " "
        + Time.ofMilliseconds( m_milliseconds ).toString( timePattern );
  }

  /******************************************* toString *******************************************/
  /**
   * Returns a formatted string using a date pattern and a time component count.
   *
   * @param datePattern    pattern for the date portion (see {@link Date#toString(String)})
   * @param timeComponents number of time components (1–4+; see {@link Time#format(int)})
   * @return formatted date-time string
   */
  public String toString( String datePattern, int timeComponents )
  {
    return Date.ofEpochDay( m_epochDay ).toString( datePattern ) + " "
        + Time.ofMilliseconds( m_milliseconds ).format( timeComponents );
  }

  /******************************************* toString *******************************************/
  /**
   * Returns this date-time formatted using the supplied {@link DateTimeFormatter}.
   * <p>
   * The time component {@code 24:00:00.000} is normalised to {@code 00:00:00.000} of the
   * following day before formatting, since {@link DateTimeFormatter} cannot represent end-of-day.
   *
   * @param formatter the formatter to use, must not be null
   * @return formatted date-time string
   * @throws NullPointerException if {@code formatter} is null
   */
  public String toString( DateTimeFormatter formatter )
  {
    Objects.requireNonNull( formatter, "formatter must not be null" );
    // toLocalDateTime() handles 24:00 → 00:00 next-day normalisation
    return formatter.format( toLocalDateTime() );
  }
}