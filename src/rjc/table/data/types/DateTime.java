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
 * Immutable date-time class representing a specific instant in time.
 * <p>
 * This class combines the {@link Date} and {@link Time} classes to provide a comprehensive
 * representation of a date and time distinguishing 24:00 (of current date) and 00:00 (of next date). 
 * It is immutable and thread-safe, with 24:00 considered earlier in comparisons.
 */
public final class DateTime implements Serializable, Comparable<DateTime>
{
  private static final long serialVersionUID = 1L;

  // private variables holding the date and time components
  private final Date        m_date;
  private final Time        m_time;

  // different time interval units for truncation, stepping & rounding operations
  public enum IntervalUnit
  {
    YEARS, HALF_YEARS, QUARTER_YEARS, MONTHS, WEEKS, DAYS, HALF_DAYS, QUARTER_DAYS, HOURS, MINUTES, SECONDS, MILLISECONDS
  }

  // ================================= Constructors =================================

  /**
   * Private constructor - use factory methods instead.
   *
   * @param date The date part.
   * @param time The time part.
   */
  private DateTime( Date date, Time time )
  {
    m_date = Objects.requireNonNull( date, "Date cannot be null" );
    m_time = Objects.requireNonNull( time, "Time cannot be null" );
  }

  // ================================= Factory Methods =================================

  /**
   * Creates a DateTime from a Date and a Time.
   *
   * @param date The date.
   * @param time The time.
   * @return A new DateTime instance.
   */
  public static DateTime of( Date date, Time time )
  {
    return new DateTime( date, time );
  }

  /**
   * Creates a DateTime from year, month, day, hour, minute, and second.
   *
   * @param year   The year.
   * @param month  The month (1-12).
   * @param day    The day of the month (1-31).
   * @param hour   The hour (0-23).
   * @param minute The minute (0-59).
   * @param second The second (0-59).
   * @return A new DateTime instance.
   */
  public static DateTime of( int year, int month, int day, int hour, int minute, int second )
  {
    return new DateTime( Date.of( year, month, day ), Time.of( hour, minute, second ) );
  }

  /**
   * Creates a DateTime from a LocalDateTime.
   *
   * @param localDateTime The LocalDateTime to convert.
   * @return A new DateTime instance.
   */
  public static DateTime of( LocalDateTime localDateTime )
  {
    Objects.requireNonNull( localDateTime, "LocalDateTime cannot be null" );
    return new DateTime( Date.of( localDateTime.toLocalDate() ), Time.of( localDateTime.toLocalTime() ) );
  }

  /**
   * Creates a DateTime representing the current moment.
   *
   * @return A DateTime for the current date and time.
   */
  public static DateTime now()
  {
    return of( LocalDateTime.now() );
  }

  /**
   * Parses a string to create a DateTime.
   * <p>
   * The string is expected to be in a format like "yyyy-MM-dd HH:mm:ss" or "yyyy-MM-dd'T'HH:mm:ss".
   * It intelligently splits the string into date and time parts for parsing.
   *
   * @param text The text to parse.
   * @return The parsed DateTime.
   * @throws DateTimeParseException if the text cannot be parsed.
   */
  public static DateTime parse( String text )
  {
    Objects.requireNonNull( text, "Text cannot be null" );
    String trimmedText = text.trim();

    // Common separators between date and time
    String[] separators = { " ", "T" };

    for ( String separator : separators )
    {
      int sepIndex = trimmedText.lastIndexOf( separator );
      if ( sepIndex > 0 )
      {
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
          // Continue to next separator or fail
        }
      }
    }

    throw new DateTimeParseException( "Unable to parse DateTime: " + text, trimmedText, 0 );
  }

  // ================================ Accessor Methods ================================

  /**
   * Gets the date part of this date-time.
   *
   * @return The date part.
   */
  public Date getDate()
  {
    return m_date;
  }

  /**
   * Gets the time part of this date-time.
   *
   * @return The time part.
   */
  public Time getTime()
  {
    return m_time;
  }

  /**
   * Returns a copy of this DateTime with the time portion replaced.
   */
  public DateTime withTime( Time newTime )
  {
    return new DateTime( m_date, newTime );
  }

  /**
   * Returns a copy of this DateTime with the date portion replaced.
   */
  public DateTime withDate( Date newDate )
  {
    return new DateTime( newDate, m_time );
  }

  // ================================= Date Arithmetic =================================

  /**
   * Returns a copy of this DateTime with the specified number of days added.
   *
   * @param days The days to add (can be negative).
   * @return A new DateTime instance.
   */
  public DateTime plusDays( int days )
  {
    return new DateTime( m_date.plusDays( days ), m_time );
  }

  /**
   * Returns a copy of this DateTime with the specified number of milliseconds added.
   *
   * @param millis The milliseconds to add (can be negative).
   * @return A new DateTime instance.
   */
  public DateTime plusMilliseconds( long millis )
  {
    if ( millis == 0 )
      return this;

    long totalMillis = m_time.toMillisecondsOfDay() + millis;
    long dayChange = totalMillis / Time.MILLIS_PER_DAY;
    long newMillis = totalMillis % Time.MILLIS_PER_DAY;

    // adjust for negative remainder
    if ( newMillis < 0 )
    {
      newMillis += Time.MILLIS_PER_DAY;
      dayChange--;
    }

    Date newDate = m_date.plusDays( (int) dayChange );
    Time newTime = Time.ofMilliseconds( (int) newMillis );

    return new DateTime( newDate, newTime );
  }

  /**
   * Returns a copy of this DateTime with the specified number of seconds added.
   *
   * @param seconds The seconds to add (can be negative).
   * @return A new DateTime instance.
   */
  public DateTime plusSeconds( int seconds )
  {
    return plusMilliseconds( (long) seconds * Time.MILLIS_PER_SECOND );
  }

  /**
   * Returns a copy of this DateTime with the specified number of minutes added.
   *
   * @param minutes The minutes to add (can be negative).
   * @return A new DateTime instance.
   */
  public DateTime plusMinutes( int minutes )
  {
    return plusMilliseconds( (long) minutes * Time.MILLIS_PER_MINUTE );
  }

  /**
   * Returns a copy of this DateTime with the specified number of hours added.
   * This may change the date.
   *
   * @param hours The hours to add (can be negative).
   * @return A new DateTime instance.
   */
  public DateTime plusHours( int hours )
  {
    return plusMilliseconds( (long) hours * Time.MILLIS_PER_HOUR );
  }

  /**
   * Returns a copy of this DateTime with the date-time rounded down to the specified unit.
   * <p>
   * This method truncates both date and time portions as appropriate. For example:
   * <ul>
   * <li>Truncating to YEARS sets date to January 1st and time to 00:00:00.000</li>
   * <li>Truncating to HALF_YEARS sets date to January 1st or July 1st and time to 00:00:00.000</li>
   * <li>Truncating to QUARTER_YEARS sets date to Jan 1st, Apr 1st, Jul 1st, or Oct 1st and time to 00:00:00.000</li>
   * <li>Truncating to MONTHS sets date to 1st of current month and time to 00:00:00.000</li>
   * <li>Truncating to WEEKS sets date to Monday of current week and time to 00:00:00.000</li>
   * <li>Truncating to DAYS sets time to 00:00:00.000</li>
   * <li>Truncating to HALF_DAYS sets time to 00:00:00.000 or 12:00:00.000</li>
   * <li>Truncating to QUARTER_DAYS sets time to 00:00, 06:00, 12:00, or 18:00</li>
   * <li>Truncating to HOURS sets minutes, seconds, and milliseconds to 0</li>
   * <li>Truncating to MINUTES sets seconds and milliseconds to 0</li>
   * <li>Truncating to SECONDS sets milliseconds to 0</li>
   * <li>Truncating to MILLISECONDS returns the same DateTime (no change)</li>
   * </ul>
   *
   * @param unit The unit to round down to.
   * @return A new DateTime instance with the date/time truncated.
   * @throws IllegalArgumentException if unit is null.
   */
  public DateTime roundDown( IntervalUnit unit )
  {
    Objects.requireNonNull( unit, "TimeUnit cannot be null" );

    switch ( unit )
    {
      case YEARS:
        return new DateTime( Date.of( m_date.getYear(), 1, 1 ), Time.of( 0, 0, 0 ) );

      case HALF_YEARS:
        int halfYear = ( m_date.getMonth() <= 6 ) ? 1 : 7;
        return new DateTime( Date.of( m_date.getYear(), halfYear, 1 ), Time.of( 0, 0, 0 ) );

      case QUARTER_YEARS:
        int quarterMonth = ( ( m_date.getMonth() - 1 ) / 3 ) * 3 + 1; // 1, 4, 7, or 10
        return new DateTime( Date.of( m_date.getYear(), quarterMonth, 1 ), Time.of( 0, 0, 0 ) );

      case MONTHS:
        return new DateTime( Date.of( m_date.getYear(), m_date.getMonth(), 1 ), Time.of( 0, 0, 0 ) );

      case WEEKS:
        // truncate to Monday of current week (ISO 8601 standard)
        int dayOfWeek = m_date.getDayOfWeek().getValue(); // assuming 1=Monday, 7=Sunday
        int daysToSubtract = ( dayOfWeek == 7 ) ? 6 : dayOfWeek - 1; // handle Sunday as 7
        Date mondayDate = m_date.plusDays( -daysToSubtract );
        return new DateTime( mondayDate, Time.of( 0, 0, 0 ) );

      case DAYS:
        return new DateTime( m_date, Time.of( 0, 0, 0 ) );

      case HALF_DAYS:
        int halfDayHour = ( m_time.getHour() < 12 ) ? 0 : 12;
        return new DateTime( m_date, Time.of( halfDayHour, 0, 0 ) );

      case QUARTER_DAYS:
        int quarterHour = ( m_time.getHour() / 6 ) * 6; // 0, 6, 12, or 18
        return new DateTime( m_date, Time.of( quarterHour, 0, 0 ) );

      case HOURS:
        return new DateTime( m_date, Time.of( m_time.getHour(), 0, 0 ) );

      case MINUTES:
        return new DateTime( m_date, Time.of( m_time.getHour(), m_time.getMinute(), 0 ) );

      case SECONDS:
        return new DateTime( m_date, Time.of( m_time.getHour(), m_time.getMinute(), m_time.getSecond() ) );

      case MILLISECONDS:
        return this; // no truncation needed

      default:
        throw new IllegalArgumentException( "Unsupported TimeUnit: " + unit );
    }
  }

  /**
   * Returns a copy of this DateTime with the specified amount of time added.
   * <p>
   * This method adds the specified amount of the given time unit to this DateTime.
   * The amount can be negative to subtract time. For example:
   * <ul>
   * <li>Adding YEARS adds the specified number of years</li>
   * <li>Adding HALF_YEARS adds 6-month periods</li>
   * <li>Adding QUARTER_YEARS adds 3-month periods</li>
   * <li>Adding MONTHS adds the specified number of months</li>
   * <li>Adding WEEKS adds 7-day periods</li>
   * <li>Adding DAYS adds the specified number of days</li>
   * <li>Adding HALF_DAYS adds 12-hour periods</li>
   * <li>Adding QUARTER_DAYS adds 6-hour periods</li>
   * <li>Adding HOURS adds the specified number of hours</li>
   * <li>Adding MINUTES adds the specified number of minutes</li>
   * <li>Adding SECONDS adds the specified number of seconds</li>
   * <li>Adding MILLISECONDS adds the specified number of milliseconds</li>
   * </ul>
   *
   * @param amount The amount to add (can be negative to subtract).
   * @param unit The unit of time to add.
   * @return A new DateTime instance with the time added.
   * @throws IllegalArgumentException if unit is null.
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
        return new DateTime( m_date.plusMonths( amount * 6 ), m_time );

      case QUARTER_YEARS:
        return new DateTime( m_date.plusMonths( amount * 3 ), m_time );

      case MONTHS:
        return new DateTime( m_date.plusMonths( amount ), m_time );

      case WEEKS:
        return plusDays( amount * 7 );

      case DAYS:
        return plusDays( amount );

      case HALF_DAYS:
        return plusHours( amount * 12 );

      case QUARTER_DAYS:
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

  /**
   * Returns a copy of this DateTime rounded up (ceiling) to the specified unit.
   * <p>
   * This method rounds the DateTime up to the next boundary of the specified unit.
   * If the DateTime is already at an exact boundary, it returns the same DateTime.
   * For example:
   * <ul>
   * <li>Rounding up to YEARS rounds up to January 1st of the next year (if not already Jan 1st at 00:00)</li>
   * <li>Rounding up to MONTHS rounds up to the 1st of the next month (if not already 1st at 00:00)</li>
   * <li>Rounding up to DAYS rounds up to 00:00:00.000 of the next day (if not already at 00:00)</li>
   * <li>Rounding up to HOURS rounds up to the next hour boundary (if not already at :00)</li>
   * </ul>
   *
   * @param unit The unit to round up to.
   * @return A new DateTime instance rounded up to the unit boundary.
   * @throws IllegalArgumentException if unit is null.
   */
  public DateTime roundUp( IntervalUnit unit )
  {
    Objects.requireNonNull( unit, "TimeUnit cannot be null" );
    DateTime truncated = roundDown( unit );

    // if already at boundary, return as-is
    if ( this.equals( truncated ) )
      return this;

    // otherwise, add one unit to the truncated value
    return truncated.plusInterval( 1, unit );
  }

  /**
   * Converts this DateTime to milliseconds since epoch (January 1, 1970, 00:00:00 UTC).
   * <p>
   * This method directly calculates epoch milliseconds using the date's epoch day
   * and the time's milliseconds of day.
   *
   * @return The number of milliseconds since the date-time epoch.
   */
  public long toMilliseconds()
  {
    long epochDay = m_date.getEpochDay();
    long millisOfDay = m_time.toMillisecondsOfDay();
    return epochDay * Time.MILLIS_PER_DAY + millisOfDay;
  }

  /**
   * Creates a DateTime from milliseconds since epoch (January 1, 1970, 00:00:00 UTC).
   *
   * @param epochMillis The milliseconds since the date-time epoch.
   * @return A new DateTime instance.
   */
  public static DateTime ofMilliseconds( long epochMillis )
  {
    int epochDay = (int) ( epochMillis / Time.MILLIS_PER_DAY );
    int millisOfDay = (int) ( epochMillis % Time.MILLIS_PER_DAY );

    Date date = Date.ofEpochDay( epochDay );
    Time time = Time.ofMilliseconds( millisOfDay );
    return new DateTime( date, time );
  }

  // ================================= Comparison Methods =================================

  /**
   * Checks if this DateTime is before the specified DateTime.
   *
   * @param other The other DateTime to compare to.
   * @return true if this is before other.
   */
  public boolean isBefore( DateTime other )
  {
    return compareTo( other ) < 0;
  }

  /**
   * Checks if this DateTime is after the specified DateTime.
   *
   * @param other The other DateTime to compare to.
   * @return true if this is after other.
   */
  public boolean isAfter( DateTime other )
  {
    return compareTo( other ) > 0;
  }

  @Override
  public int compareTo( DateTime other )
  {
    Objects.requireNonNull( other, "Other DateTime cannot be null" );
    int dateCmp = m_date.compareTo( other.m_date );
    if ( dateCmp != 0 )
      return dateCmp;
    return m_time.compareTo( other.m_time );
  }

  // ================================= Object Methods =================================

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

  @Override
  public int hashCode()
  {
    return Objects.hash( m_date, m_time );
  }

  /**
   * Returns the string representation of the DateTime, following the
   * defaults for Date and Time separated by a space.
   *
   * @return The formatted string.
   */
  @Override
  public String toString()
  {
    return m_date.toString() + " " + m_time.toString();
  }

  /**
   * Returns the string representation of the DateTime using the specified date format
   * and specified number of time components.
   *
   * @param datePattern,  The pattern to use for data formatting.
   * @param timeComponents The time components 1=HH, 2=HH:MM, 3=HH:MM:SS, 4+=HH:MM:SS.mmm.
   * @return The formatted string.
   */
  public String format( String datePattern, int timeComponents )
  {
    return m_date.format( datePattern ) + " " + m_time.format( timeComponents );
  }
}
