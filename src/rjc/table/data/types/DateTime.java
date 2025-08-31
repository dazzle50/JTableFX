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
import java.time.format.DateTimeParseException;
import java.util.Objects;

/*************************************************************************************************/
/*********************************** DateTime (with no time-zone) ********************************/
/*************************************************************************************************/

/**
 * Immutable date-time class representing a specific instant in time without timezone information.
 * <p>
 * This class combines the {@link Date} and {@link Time} classes to provide a comprehensive
 * representation of a date and time, properly distinguishing between 24:00 (end of current date) 
 * and 00:00 (start of next date). The class is immutable and thread-safe.
 * <p>
 * In comparison operations, 24:00 of day N is considered earlier than 00:00 of day N+1,
 * maintaining logical consistency with time progression.
 * <p>
 * Key features:
 * <ul>
 * <li>Immutable - all operations return new instances</li>
 * <li>Thread-safe - can be safely used in concurrent environments</li>
 * <li>Serializable - can be persisted and transmitted</li>
 * <li>Comparable - supports natural ordering</li>
 * <li>Flexible parsing - supports multiple date-time formats</li>
 * <li>Arithmetic operations - add/subtract time intervals</li>
 * <li>Truncation/rounding - align to specific time boundaries</li>
 * </ul>
 * 
 * @see Date
 * @see Time
 */
public final class DateTime implements Serializable, Comparable<DateTime>
{
  private static final long serialVersionUID = 1L;

  // immutable date and time components forming this datetime instance
  private final Date        m_date;
  private final Time        m_time;

  /**
   * Enumeration of time interval units for truncation, stepping and rounding operations.
   * <p>
   * Units range from milliseconds (finest granularity) to years (coarsest granularity).
   * Fractional units like HALF_YEARS and QUARTER_YEARS provide additional flexibility
   * for business-oriented time calculations.
   */
  public enum IntervalUnit
  {
    // year boundaries (January 1st)
    YEARS,
    // half-year boundaries (January 1st, July 1st)
    HALF_YEARS,
    // quarter-year boundaries (January, April, July, October 1st)
    QUARTER_YEARS,
    // month boundaries (1st of each month)
    MONTHS,
    // week boundaries (Monday of each week, ISO 8601)
    WEEKS,
    // day boundaries (00:00:00.000 of each day)
    DAYS,
    // half-day boundaries (00:00, 12:00)
    HALF_DAYS,
    // quarter-day boundaries (00:00, 06:00, 12:00, 18:00)
    QUARTER_DAYS,
    // hour boundaries (:00 of each hour)
    HOURS,
    // minute boundaries (:00 of each minute)
    MINUTES,
    // second boundaries (.000 of each second)
    SECONDS,
    // millisecond precision (no truncation)
    MILLISECONDS
  }

  // ================================= Constructors =================================

  /**
   * Private constructor to enforce use of factory methods for instance creation.
   * <p>
   * This ensures proper validation and consistent object creation patterns.
   *
   * @param date the date component, must not be null
   * @param time the time component, must not be null
   * @throws NullPointerException if either date or time is null
   */
  private DateTime( Date date, Time time )
  {
    m_date = Objects.requireNonNull( date, "Date cannot be null" );
    m_time = Objects.requireNonNull( time, "Time cannot be null" );
  }

  // ================================= Factory Methods =================================

  /********************************************* of **********************************************/
  /**
   * Creates a DateTime from separate Date and Time components.
   * <p>
   * This is the primary factory method for combining existing Date and Time objects.
   *
   * @param date the date component, must not be null
   * @param time the time component, must not be null
   * @return a new DateTime instance combining the date and time
   * @throws NullPointerException if either parameter is null
   */
  public static DateTime of( Date date, Time time )
  {
    return new DateTime( date, time );
  }

  /********************************************* of **********************************************/
  /**
   * Creates a DateTime from individual date and time field values.
   * <p>
   * This convenience method constructs both Date and Time components from primitives.
   *
   * @param year   the year value
   * @param month  the month value (1-12, where 1=January)
   * @param day    the day of month value (1-31, validated against month/year)
   * @param hour   the hour value (0-23)
   * @param minute the minute value (0-59)
   * @param second the second value (0-59)
   * @return a new DateTime instance
   * @throws IllegalArgumentException if any field value is invalid
   */
  public static DateTime of( int year, int month, int day, int hour, int minute, int second )
  {
    return new DateTime( Date.of( year, month, day ), Time.of( hour, minute, second ) );
  }

  /********************************************* of **********************************************/
  /**
   * Creates a DateTime from a Java LocalDateTime instance.
   * <p>
   * This method provides interoperability with the standard Java time API.
   * The conversion preserves the date and time values exactly.
   *
   * @param localDateTime the LocalDateTime to convert, must not be null
   * @return a new DateTime instance with equivalent date and time values
   * @throws NullPointerException if localDateTime is null
   */
  public static DateTime of( LocalDateTime localDateTime )
  {
    Objects.requireNonNull( localDateTime, "LocalDateTime cannot be null" );
    return new DateTime( Date.of( localDateTime.toLocalDate() ), Time.of( localDateTime.toLocalTime() ) );
  }

  /********************************************* now *********************************************/
  /**
   * Creates a DateTime representing the current system date and time.
   * <p>
   * This method captures the current moment using the system clock and default timezone.
   * The returned DateTime reflects the local date and time at the moment of invocation.
   *
   * @return a DateTime representing the current moment
   */
  public static DateTime now()
  {
    return of( LocalDateTime.now() );
  }

  /******************************************** parse ********************************************/
  /**
   * Parses a text string to create a DateTime instance.
   * <p>
   * This method intelligently handles various common date-time formats by attempting
   * to split the input on common separators (space, 'T') and parsing the resulting
   * date and time portions separately. Supported formats include:
   * <ul>
   * <li>"yyyy-MM-dd HH:mm:ss" (ISO-like with space separator)</li>
   * <li>"yyyy-MM-dd'T'HH:mm:ss" (ISO 8601 format)</li>
   * <li>Other combinations using Date.parseIntelligent() and Time.parse()</li>
   * </ul>
   * <p>
   * The method tries multiple parsing strategies and returns the first successful result.
   *
   * @param text the text string to parse, must not be null
   * @return the parsed DateTime instance
   * @throws NullPointerException if text is null
   * @throws DateTimeParseException if the text cannot be parsed as a valid DateTime
   */
  public static DateTime parse( String text )
  {
    Objects.requireNonNull( text, "Text cannot be null" );
    String trimmedText = text.trim();

    // attempt parsing with common date-time separators
    String[] separators = { " ", "T" };

    for ( String separator : separators )
    {
      int sepIndex = trimmedText.lastIndexOf( separator );
      if ( sepIndex > 0 )
      {
        // split into date and time portions for separate parsing
        String dateStr = trimmedText.substring( 0, sepIndex );
        String timeStr = trimmedText.substring( sepIndex + 1 );
        try
        {
          Date date = Date.parseIntelligent( dateStr );
          Time time = Time.parse( timeStr );
          return new DateTime( date, time );
        }
        catch ( IllegalArgumentException | DateTimeParseException e )
        {
          // continue to next separator strategy
        }
      }
    }

    throw new DateTimeParseException( "Unable to parse DateTime: " + text, trimmedText, 0 );
  }

  // ================================ Accessor Methods ================================

  /******************************************* getDate *******************************************/
  /**
   * Returns the date component of this DateTime.
   * <p>
   * The returned Date object is immutable and represents the calendar date
   * portion (year, month, day) of this DateTime.
   *
   * @return the date component, never null
   */
  public Date getDate()
  {
    return m_date;
  }

  /******************************************* getTime *******************************************/
  /**
   * Returns the time component of this DateTime.
   * <p>
   * The returned Time object is immutable and represents the time-of-day
   * portion (hour, minute, second, millisecond) of this DateTime.
   *
   * @return the time component, never null
   */
  public Time getTime()
  {
    return m_time;
  }

  /****************************************** withTime *******************************************/
  /**
   * Returns a copy of this DateTime with the specified time component.
   * <p>
   * This method creates a new DateTime instance with the same date but different time.
   * The original DateTime remains unchanged.
   *
   * @param newTime the replacement time component, must not be null
   * @return a new DateTime with the specified time
   * @throws NullPointerException if newTime is null
   */
  public DateTime withTime( Time newTime )
  {
    return new DateTime( m_date, newTime );
  }

  /****************************************** withDate *******************************************/
  /**
   * Returns a copy of this DateTime with the specified date component.
   * <p>
   * This method creates a new DateTime instance with the same time but different date.
   * The original DateTime remains unchanged.
   *
   * @param newDate the replacement date component, must not be null
   * @return a new DateTime with the specified date
   * @throws NullPointerException if newDate is null
   */
  public DateTime withDate( Date newDate )
  {
    return new DateTime( newDate, m_time );
  }

  // ================================= Date Arithmetic =================================

  /****************************************** plusDays *******************************************/
  /**
   * Returns a copy of this DateTime with the specified number of days added.
   * <p>
   * This method adds (or subtracts if negative) the specified number of days
   * to the date component while preserving the time component exactly.
   *
   * @param days the number of days to add, may be negative
   * @return a new DateTime instance with the days added
   */
  public DateTime plusDays( int days )
  {
    return new DateTime( m_date.plusDays( days ), m_time );
  }

  /************************************** plusMilliseconds ***************************************/
  /**
   * Returns a copy of this DateTime with the specified number of milliseconds added.
   * <p>
   * This method handles overflow/underflow by adjusting both the time and date
   * components as necessary. For example, adding milliseconds that exceed a day
   * boundary will increment the date and adjust the time accordingly.
   *
   * @param millis the number of milliseconds to add, may be negative
   * @return a new DateTime instance with the milliseconds added
   */
  public DateTime plusMilliseconds( long millis )
  {
    if ( millis == 0 )
      return this;

    // calculate total milliseconds and determine day overflow/underflow
    long totalMillis = m_time.toMillisecondsOfDay() + millis;
    long dayChange = totalMillis / Time.MILLIS_PER_DAY;
    long newMillis = totalMillis % Time.MILLIS_PER_DAY;

    // handle negative remainder by borrowing from previous day
    if ( newMillis < 0 )
    {
      newMillis += Time.MILLIS_PER_DAY;
      dayChange--;
    }

    Date newDate = m_date.plusDays( (int) dayChange );
    Time newTime = Time.ofMilliseconds( (int) newMillis );

    return new DateTime( newDate, newTime );
  }

  /***************************************** plusSeconds *****************************************/
  /**
   * Returns a copy of this DateTime with the specified number of seconds added.
   * <p>
   * This is a convenience method that converts seconds to milliseconds and
   * delegates to plusMilliseconds() for consistent overflow handling.
   *
   * @param seconds the number of seconds to add, may be negative
   * @return a new DateTime instance with the seconds added
   */
  public DateTime plusSeconds( int seconds )
  {
    return plusMilliseconds( (long) seconds * Time.MILLIS_PER_SECOND );
  }

  /***************************************** plusMinutes *****************************************/
  /**
   * Returns a copy of this DateTime with the specified number of minutes added.
   * <p>
   * This is a convenience method that converts minutes to milliseconds and
   * delegates to plusMilliseconds() for consistent overflow handling.
   *
   * @param minutes the number of minutes to add, may be negative
   * @return a new DateTime instance with the minutes added
   */
  public DateTime plusMinutes( int minutes )
  {
    return plusMilliseconds( (long) minutes * Time.MILLIS_PER_MINUTE );
  }

  /****************************************** plusHours ******************************************/
  /**
   * Returns a copy of this DateTime with the specified number of hours added.
   * <p>
   * This method may change the date if the hour addition crosses day boundaries.
   * It converts hours to milliseconds and delegates to plusMilliseconds() for 
   * consistent overflow handling.
   *
   * @param hours the number of hours to add, may be negative
   * @return a new DateTime instance with the hours added
   */
  public DateTime plusHours( int hours )
  {
    return plusMilliseconds( (long) hours * Time.MILLIS_PER_HOUR );
  }

  /****************************************** roundDown ******************************************/
  /**
   * Returns a copy of this DateTime truncated (rounded down) to the specified time unit.
   * <p>
   * This method truncates both date and time components as appropriate to align
   * the DateTime to the nearest lower boundary of the specified unit. The truncation
   * behavior for each unit is as follows:
   * <ul>
   * <li><strong>YEARS:</strong> January 1st at 00:00:00.000</li>
   * <li><strong>HALF_YEARS:</strong> January 1st or July 1st at 00:00:00.000</li>
   * <li><strong>QUARTER_YEARS:</strong> Jan 1st, Apr 1st, Jul 1st, or Oct 1st at 00:00:00.000</li>
   * <li><strong>MONTHS:</strong> 1st day of current month at 00:00:00.000</li>
   * <li><strong>WEEKS:</strong> Monday of current week at 00:00:00.000 (ISO 8601)</li>
   * <li><strong>DAYS:</strong> Current date at 00:00:00.000</li>
   * <li><strong>HALF_DAYS:</strong> Current date at 00:00:00.000 or 12:00:00.000</li>
   * <li><strong>QUARTER_DAYS:</strong> Current date at 00:00, 06:00, 12:00, or 18:00</li>
   * <li><strong>HOURS:</strong> Current date and hour at :00:00.000</li>
   * <li><strong>MINUTES:</strong> Current date, hour, and minute at :00.000</li>
   * <li><strong>SECONDS:</strong> Current date, hour, minute, and second at .000</li>
   * <li><strong>MILLISECONDS:</strong> No change (already at finest granularity)</li>
   * </ul>
   *
   * @param unit the time unit to truncate to, must not be null
   * @return a new DateTime instance truncated to the specified unit boundary
   * @throws NullPointerException if unit is null
   * @throws IllegalArgumentException if unit is not supported
   */
  public DateTime roundDown( IntervalUnit unit )
  {
    Objects.requireNonNull( unit, "TimeUnit cannot be null" );

    switch ( unit )
    {
      case YEARS:
        return new DateTime( Date.of( m_date.getYear(), 1, 1 ), Time.of( 0, 0, 0 ) );

      case HALF_YEARS:
        // truncate to january 1st or july 1st of current year
        int halfYear = ( m_date.getMonth() <= 6 ) ? 1 : 7;
        return new DateTime( Date.of( m_date.getYear(), halfYear, 1 ), Time.of( 0, 0, 0 ) );

      case QUARTER_YEARS:
        // truncate to start of current quarter (jan, apr, jul, oct)
        int quarterMonth = ( ( m_date.getMonth() - 1 ) / 3 ) * 3 + 1; // 1, 4, 7, or 10
        return new DateTime( Date.of( m_date.getYear(), quarterMonth, 1 ), Time.of( 0, 0, 0 ) );

      case MONTHS:
        return new DateTime( Date.of( m_date.getYear(), m_date.getMonth(), 1 ), Time.of( 0, 0, 0 ) );

      case WEEKS:
        // truncate to monday of current week following iso 8601 standard
        int dayOfWeek = m_date.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        int daysToSubtract = ( dayOfWeek == 7 ) ? 6 : dayOfWeek - 1; // handle Sunday as 7
        Date mondayDate = m_date.plusDays( -daysToSubtract );
        return new DateTime( mondayDate, Time.of( 0, 0, 0 ) );

      case DAYS:
        return new DateTime( m_date, Time.of( 0, 0, 0 ) );

      case HALF_DAYS:
        // truncate to 00:00 or 12:00 of current day
        int halfDayHour = ( m_time.getHour() < 12 ) ? 0 : 12;
        return new DateTime( m_date, Time.of( halfDayHour, 0, 0 ) );

      case QUARTER_DAYS:
        // truncate to 6-hour boundaries: 00:00, 06:00, 12:00, 18:00
        int quarterHour = ( m_time.getHour() / 6 ) * 6; // 0, 6, 12, or 18
        return new DateTime( m_date, Time.of( quarterHour, 0, 0 ) );

      case HOURS:
        return new DateTime( m_date, Time.of( m_time.getHour(), 0, 0 ) );

      case MINUTES:
        return new DateTime( m_date, Time.of( m_time.getHour(), m_time.getMinute(), 0 ) );

      case SECONDS:
        return new DateTime( m_date, Time.of( m_time.getHour(), m_time.getMinute(), m_time.getSecond() ) );

      case MILLISECONDS:
        return this; // already at finest granularity

      default:
        throw new IllegalArgumentException( "Unsupported TimeUnit: " + unit );
    }
  }

  /**************************************** plusInterval *****************************************/
  /**
   * Returns a copy of this DateTime with the specified amount of time added.
   * <p>
   * This method provides a unified interface for adding various time units to a DateTime.
   * The amount can be negative to subtract time. The behavior varies by unit:
   * <ul>
   * <li><strong>YEARS/HALF_YEARS/QUARTER_YEARS/MONTHS:</strong> Uses date arithmetic (may vary in actual duration)</li>
   * <li><strong>WEEKS/DAYS:</strong> Adds exact 7-day or 1-day periods</li>
   * <li><strong>HALF_DAYS/QUARTER_DAYS/HOURS/MINUTES/SECONDS/MILLISECONDS:</strong> Adds exact time periods</li>
   * </ul>
   * <p>
   * When adding calendar-based units (years, months), the day-of-month is preserved
   * where possible, with adjustments made for invalid dates (e.g., February 29th in non-leap years).
   *
   * @param amount the amount to add, may be negative to subtract
   * @param unit the time unit for the amount, must not be null
   * @return a new DateTime instance with the specified interval added
   * @throws NullPointerException if unit is null
   * @throws IllegalArgumentException if unit is not supported
   */
  public DateTime plusInterval( int amount, IntervalUnit unit )
  {
    Objects.requireNonNull( unit, "TimeUnit cannot be null" );

    if ( amount == 0 )
      return this;

    switch ( unit )
    {
      case YEARS:
        return new DateTime( m_date.plusYears( amount ), m_time );

      case HALF_YEARS:
        // add 6-month periods
        return new DateTime( m_date.plusMonths( amount * 6 ), m_time );

      case QUARTER_YEARS:
        // add 3-month periods
        return new DateTime( m_date.plusMonths( amount * 3 ), m_time );

      case MONTHS:
        return new DateTime( m_date.plusMonths( amount ), m_time );

      case WEEKS:
        // add exact 7-day periods
        return plusDays( amount * 7 );

      case DAYS:
        return plusDays( amount );

      case HALF_DAYS:
        // add exact 12-hour periods
        return plusHours( amount * 12 );

      case QUARTER_DAYS:
        // add exact 6-hour periods
        return plusHours( amount * 6 );

      case HOURS:
        return plusHours( amount );

      case MINUTES:
        return plusMinutes( amount );

      case SECONDS:
        return plusSeconds( amount );

      case MILLISECONDS:
        return plusMilliseconds( amount );

      default:
        throw new IllegalArgumentException( "Unsupported TimeUnit: " + unit );
    }
  }

  /******************************************* roundUp *******************************************/
  /**
   * Returns a copy of this DateTime rounded up (ceiling) to the specified time unit.
   * <p>
   * This method rounds the DateTime up to the next boundary of the specified unit.
   * If the DateTime is already aligned to an exact unit boundary, it returns the 
   * same DateTime unchanged. Otherwise, it returns the next higher boundary.
   * <p>
   * Examples of rounding up behavior:
   * <ul>
   * <li><strong>YEARS:</strong> Next January 1st at 00:00:00.000 (if not already there)</li>
   * <li><strong>MONTHS:</strong> Next 1st of month at 00:00:00.000 (if not already there)</li>
   * <li><strong>DAYS:</strong> Next day at 00:00:00.000 (if not already at midnight)</li>
   * <li><strong>HOURS:</strong> Next hour at :00:00.000 (if not already at hour boundary)</li>
   * </ul>
   *
   * @param unit the time unit to round up to, must not be null
   * @return a new DateTime instance rounded up to the next unit boundary, or this instance if already aligned
   * @throws NullPointerException if unit is null
   * @throws IllegalArgumentException if unit is not supported
   */
  public DateTime roundUp( IntervalUnit unit )
  {
    Objects.requireNonNull( unit, "TimeUnit cannot be null" );
    DateTime truncated = roundDown( unit );

    // if already aligned to boundary, return unchanged
    if ( this.equals( truncated ) )
      return this;

    // otherwise advance to next boundary
    return truncated.plusInterval( 1, unit );
  }

  /*************************************** toMilliseconds ****************************************/
  /**
   * Converts this DateTime to milliseconds since the Unix epoch (January 1, 1970, 00:00:00 UTC).
   * <p>
   * This method calculates the total milliseconds by combining the date's epoch day
   * with the time's milliseconds within that day. The result is compatible with
   * Java's standard millisecond-based time representations.
   * <p>
   * Note that this conversion assumes the DateTime represents a local time and
   * does not perform any timezone adjustments.
   *
   * @return the number of milliseconds since the Unix epoch
   */
  public long toMilliseconds()
  {
    // combine epoch day with milliseconds of day for total epoch milliseconds
    long epochDay = m_date.getEpochDay();
    long millisOfDay = m_time.toMillisecondsOfDay();
    return epochDay * Time.MILLIS_PER_DAY + millisOfDay;
  }

  /*************************************** ofMilliseconds ****************************************/
  /**
   * Creates a DateTime from milliseconds since the Unix epoch (January 1, 1970, 00:00:00 UTC).
   * <p>
   * This factory method converts epoch milliseconds back into a DateTime by
   * calculating the appropriate date and time components. It serves as the
   * inverse operation to toMilliseconds().
   *
   * @param epochMillis the number of milliseconds since the Unix epoch
   * @return a new DateTime instance representing the specified epoch time
   */
  public static DateTime ofMilliseconds( long epochMillis )
  {
    // decompose epoch milliseconds into date and time components
    int epochDay = (int) ( epochMillis / Time.MILLIS_PER_DAY );
    int millisOfDay = (int) ( epochMillis % Time.MILLIS_PER_DAY );

    Date date = Date.ofEpochDay( epochDay );
    Time time = Time.ofMilliseconds( millisOfDay );
    return new DateTime( date, time );
  }

  // ================================= Comparison Methods =================================

  /****************************************** isBefore *******************************************/
  /**
   * Checks if this DateTime occurs chronologically before the specified DateTime.
   * <p>
   * This method provides a more readable alternative to compareTo() < 0 for
   * temporal ordering comparisons.
   *
   * @param other the DateTime to compare against, must not be null
   * @return true if this DateTime is before the other DateTime
   * @throws NullPointerException if other is null
   */
  public boolean isBefore( DateTime other )
  {
    return compareTo( other ) < 0;
  }

  /******************************************* isAfter *******************************************/
  /**
   * Checks if this DateTime occurs chronologically after the specified DateTime.
   * <p>
   * This method provides a more readable alternative to compareTo() > 0 for
   * temporal ordering comparisons.
   *
   * @param other the DateTime to compare against, must not be null
   * @return true if this DateTime is after the other DateTime
   * @throws NullPointerException if other is null
   */
  public boolean isAfter( DateTime other )
  {
    return compareTo( other ) > 0;
  }

  /****************************************** compareTo ******************************************/
  /**
   * Compares this DateTime to another DateTime for chronological ordering.
   * <p>
   * The comparison is performed by first comparing the date components, and if
   * they are equal, then comparing the time components. This ensures a consistent
   * natural ordering for DateTime instances.
   * <p>
   * Special handling: 24:00 of day N is considered earlier than 00:00 of day N+1,
   * maintaining logical consistency with the distinction between end-of-day and
   * start-of-next-day representations.
   *
   * @param other the DateTime to compare to, must not be null
   * @return negative integer if this is before other, zero if equal, positive if after
   * @throws NullPointerException if other is null
   */
  @Override
  public int compareTo( DateTime other )
  {
    Objects.requireNonNull( other, "Other DateTime cannot be null" );

    // compare date components first
    int dateCmp = m_date.compareTo( other.m_date );
    if ( dateCmp != 0 )
      return dateCmp;

    // dates are equal, so compare time components
    return m_time.compareTo( other.m_time );
  }

  // ================================= Object Methods =================================

  /******************************************* equals ********************************************/
  /**
   * Indicates whether some other object is "equal to" this DateTime.
   * <p>
   * Two DateTime instances are considered equal if and only if they have
   * equal date and time components. This method follows the general contract
   * of Object.equals() and is consistent with compareTo().
   *
   * @param obj the reference object with which to compare
   * @return true if this object is equal to the obj argument; false otherwise
   */
  @Override
  public boolean equals( Object obj )
  {
    if ( this == obj )
      return true;
    if ( obj == null || getClass() != obj.getClass() )
      return false;

    DateTime other = (DateTime) obj;
    return m_date.equals( other.m_date ) && m_time.equals( other.m_time );
  }

  /****************************************** hashCode *******************************************/
  /**
   * Returns a hash code value for this DateTime.
   * <p>
   * The hash code is computed based on both the date and time components
   * to ensure that equal DateTime instances have equal hash codes, as
   * required by the general contract of Object.hashCode().
   *
   * @return a hash code value for this DateTime
   */
  @Override
  public int hashCode()
  {
    return Objects.hash( m_date, m_time );
  }

  /****************************************** toString *******************************************/
  /**
   * Returns a string representation of this DateTime in default format.
   * <p>
   * The format combines the default string representations of the date and time
   * components separated by a space. This provides a human-readable representation
   * suitable for debugging and logging purposes.
   * <p>
   * Example output: "2025-08-31 14:30:45.123"
   *
   * @return a string representation of this DateTime in the format "date time"
   */
  @Override
  public String toString()
  {
    return m_date.toString() + " " + m_time.toString();
  }

  /******************************************* format ********************************************/
  /**
   * Returns a formatted string representation of this DateTime using custom patterns.
   * <p>
   * This method allows fine-grained control over the output format by specifying
   * separate formatting patterns for the date and time components. The date pattern
   * follows standard date formatting conventions, while the time components parameter
   * controls the precision of time display.
   * <p>
   * Time components parameter values:
   * <ul>
   * <li>1 = HH (hours only)</li>
   * <li>2 = HH:MM (hours and minutes)</li>
   * <li>3 = HH:MM:SS (hours, minutes, and seconds)</li>
   * <li>4+ = HH:MM:SS.mmm (hours, minutes, seconds, and milliseconds)</li>
   * </ul>
   *
   * @param datePattern the pattern string for formatting the date component
   * @param timeComponents the number of time components to include (1-4+)
   * @return a formatted string representation combining the date and time parts
   * @throws IllegalArgumentException if datePattern is invalid or timeComponents is less than 1
   */
  public String format( String datePattern, int timeComponents )
  {
    return m_date.format( datePattern ) + " " + m_time.format( timeComponents );
  }
}