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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.Objects;

/*************************************************************************************************/
/*********************************** Date (calendar date) ****************************************/
/*************************************************************************************************/

/**
 * Compact proleptic-Gregorian calendar date with an {@code int} epoch-day representation. All
 * arithmetic and calendar boundaries are checked; compact storage never wraps silently.
 *
 * <p>Instances are immutable, thread-safe, and ordered by epoch day.</p>
 */
public final class Date implements Serializable, Comparable<Date>
{
  @Serial
  private static final long serialVersionUID = 2L;

  /** Earliest representable compact date. */
  public static final Date  MIN_VALUE        = new Date( Integer.MIN_VALUE );
  /** Latest representable compact date. */
  public static final Date  MAX_VALUE        = new Date( Integer.MAX_VALUE );
  /** Unix epoch date, 1970-01-01. */
  public static final Date  EPOCH            = new Date( 0 );

  /** Signed days from 1970-01-01. */
  private final int         epochDay;

  private Date( int epochDay )
  {
    this.epochDay = epochDay;
  }

  // ================================= Factory methods =================================

  /**
   * Creates a date from its signed distance in days from 1970-01-01.
   *
   * @param epochDay epoch-day coordinate
   * @return the represented date
   * @throws ArithmeticException if {@code epochDay} cannot be stored as an {@code int}
   */
  public static Date ofEpochDay( long epochDay )
  {
    return new Date( EpochDayMath.checkedEpochDay( epochDay ) );
  }

  /**
   * Creates a validated proleptic-Gregorian date.
   *
   * @param year proleptic year
   * @param month month from 1 to 12
   * @param day day of month
   * @return the represented date
   * @throws java.time.DateTimeException if the calendar date is invalid
   * @throws ArithmeticException if the date is outside compact storage
   */
  public static Date of( int year, int month, int day )
  {
    return new Date( EpochDayMath.fromYmd( year, month, day ) );
  }

  /**
   * Converts a Java date to its compact representation.
   *
   * @param localDate source date
   * @return the represented date
   * @throws NullPointerException if {@code localDate} is null
   * @throws ArithmeticException if the date is outside compact storage
   */
  public static Date of( LocalDate localDate )
  {
    Objects.requireNonNull( localDate, "localDate" );
    return ofEpochDay( localDate.toEpochDay() );
  }

  /**
   * Obtains today's date using the system-default time zone.
   *
   * @return today's local date
   */
  public static Date now()
  {
    return now( Clock.systemDefaultZone() );
  }

  /**
   * Obtains today's date from the supplied clock.
   *
   * @param clock source of the current instant and time zone
   * @return the clock's current local date
   * @throws NullPointerException if {@code clock} is null
   * @throws ArithmeticException if the clock's date is outside compact storage
   */
  public static Date now( Clock clock )
  {
    return of( LocalDate.now( Objects.requireNonNull( clock, "clock" ) ) );
  }

  // ================================= Component access =================================

  /**
   * Returns the compact epoch-day coordinate.
   *
   * @return signed days from 1970-01-01
   */
  public int getEpochDay()
  {
    return epochDay;
  }

  /**
   * Returns the proleptic year.
   *
   * @return proleptic year
   */
  public int getYear()
  {
    return EpochDayMath.year( epochDay );
  }

  /**
   * Returns the numeric month.
   *
   * @return month from 1 to 12
   */
  public int getMonth()
  {
    return EpochDayMath.month( epochDay );
  }

  /**
   * Returns the month enum.
   *
   * @return month enum
   */
  public Month getMonthEnum()
  {
    return Month.of( getMonth() );
  }

  /**
   * Returns the day of month.
   *
   * @return day of month
   */
  public int getDayOfMonth()
  {
    return EpochDayMath.dayOfMonth( epochDay );
  }

  /**
   * Returns the ISO day of week.
   *
   * @return ISO day of week
   */
  public DayOfWeek getDayOfWeek()
  {
    return DayOfWeek.of( (int) Math.floorMod( (long) epochDay + 3L, 7L ) + 1 );
  }

  /**
   * Returns the one-based day within the year.
   *
   * @return day of year from 1 to 365 or 366
   */
  public int getDayOfYear()
  {
    return Math.toIntExact( (long) epochDay - EpochDayMath.periodStart( epochDay, DateInterval.YEAR ) + 1L );
  }

  /**
   * Returns the ISO-8601 week number.
   *
   * @return ISO-8601 week number
   */
  public int getWeekOfYear()
  {
    return toLocalDate().get( IsoFields.WEEK_OF_WEEK_BASED_YEAR );
  }

  /**
   * Returns the quarter containing this date.
   *
   * @return quarter number from 1 to 4
   */
  public int getQuarter()
  {
    return ( getMonth() - 1 ) / 3 + 1;
  }

  /**
   * Returns the half-year containing this date.
   *
   * @return half-year number, 1 or 2
   */
  public int getHalfYear()
  {
    return getMonth() <= 6 ? 1 : 2;
  }

  /**
   * Tests the represented year using proleptic-Gregorian leap rules.
   *
   * @return whether the represented proleptic year is a leap year
   */
  public boolean isLeapYear()
  {
    return EpochDayMath.isLeapYear( getYear() );
  }

  /**
   * Returns the represented month's length.
   *
   * @return number of days in the represented month
   */
  public int lengthOfMonth()
  {
    return EpochDayMath.monthLength( getYear(), getMonth() );
  }

  /**
   * Returns the represented year's length.
   *
   * @return number of days in the represented year
   */
  public int lengthOfYear()
  {
    return isLeapYear() ? 366 : 365;
  }

  /**
   * Converts this value to the equivalent Java date.
   *
   * @return equivalent {@link LocalDate}
   */
  public LocalDate toLocalDate()
  {
    return LocalDate.ofEpochDay( epochDay );
  }

  // =================================== Date arithmetic ===================================

  /**
   * Adds calendar days with checked compact storage.
   *
   * @param days days to add
   * @return adjusted date
   * @throws ArithmeticException if arithmetic or compact storage overflows
   */
  public Date plusDays( long days )
  {
    return days == 0L ? this : new Date( EpochDayMath.addDays( epochDay, days ) );
  }

  /**
   * Adds ISO weeks with checked multiplication and storage.
   *
   * @param weeks weeks to add
   * @return adjusted date
   * @throws ArithmeticException if arithmetic or compact storage overflows
   */
  public Date plusWeeks( long weeks )
  {
    return plusDays( Math.multiplyExact( weeks, 7L ) );
  }

  /**
   * Adds calendar months, clamping the day to the target month's last valid day.
   *
   * @param months months to add
   * @return adjusted date
   * @throws ArithmeticException if arithmetic or compact storage overflows
   */
  public Date plusMonths( long months )
  {
    return months == 0L ? this : new Date( EpochDayMath.addMonths( epochDay, months ) );
  }

  /**
   * Adds calendar years, clamping leap day when necessary.
   *
   * @param years years to add
   * @return adjusted date
   * @throws ArithmeticException if arithmetic or compact storage overflows
   */
  public Date plusYears( long years )
  {
    return years == 0L ? this : new Date( EpochDayMath.addYears( epochDay, years ) );
  }

  /**
   * Adds a locale-independent calendar interval with checked multiplication and storage.
   *
   * @param amount number of intervals
   * @param interval calendar interval
   * @return adjusted date
   * @throws NullPointerException if {@code interval} is null, including for zero amount
   * @throws ArithmeticException if arithmetic or compact storage overflows
   */
  public Date plus( long amount, DateInterval interval )
  {
    Objects.requireNonNull( interval, "interval" );
    if ( amount == 0L )
      return this;
    return switch ( interval )
    {
      case DAY -> plusDays( amount );
      case WEEK -> plusWeeks( amount );
      case MONTH -> plusMonths( amount );
      case QUARTER_YEAR -> plusMonths( Math.multiplyExact( amount, 3L ) );
      case HALF_YEAR -> plusMonths( Math.multiplyExact( amount, 6L ) );
      case YEAR -> plusYears( amount );
    };
  }

  /**
   * Returns the first date of the calendar period containing this date.
   * Periods use the proleptic-Gregorian calendar and are locale-independent: weeks begin on
   * Monday, quarters begin in January, April, July, and October, and half-years begin in January
   * and July. For example, {@code 2026-08-20.startOf(WEEK)} is {@code 2026-08-17} and
   * {@code 2026-08-20.startOf(QUARTER_YEAR)} is {@code 2026-07-01}.
   *
   * <p>{@link DateInterval#DAY} and a date already at the start of its requested period return
   * {@code this}.</p>
   *
   * @param interval calendar period whose start is required
   * @return the first date of the containing period, or {@code this} when already at that date
   * @throws NullPointerException if {@code interval} is null
   * @throws ArithmeticException if the period starts before the compact date range
   */
  public Date startOf( DateInterval interval )
  {
    Objects.requireNonNull( interval, "interval" );
    int result = EpochDayMath.checkedEpochDay( EpochDayMath.periodStart( epochDay, interval ) );
    return result == epochDay ? this : new Date( result );
  }

  /**
   * Returns the last date of the calendar period containing this date.
   * Periods use the proleptic-Gregorian calendar and are locale-independent: weeks end on Sunday,
   * and month, quarter, half-year, and year ends follow their calendar boundaries. For example,
   * {@code 2026-08-20.endOf(WEEK)} is {@code 2026-08-23} and
   * {@code 2026-08-20.endOf(MONTH)} is {@code 2026-08-31}.
   *
   * <p>This method returns the final date <em>within</em> the period, not the first date of the
   * following period. {@link DateInterval#DAY} and a date already at the end of its requested
   * period return {@code this}.</p>
   *
   * @param interval calendar period whose end is required
   * @return the last date of the containing period, or {@code this} when already at that date
   * @throws NullPointerException if {@code interval} is null
   * @throws ArithmeticException if the period ends after the compact date range
   */
  public Date endOf( DateInterval interval )
  {
    Objects.requireNonNull( interval, "interval" );
    int result = EpochDayMath.checkedEpochDay( EpochDayMath.periodEnd( epochDay, interval ) );
    return result == epochDay ? this : new Date( result );
  }

  // ================================= Comparison and distance ================================

  /**
   * Tests whether this date precedes another.
   *
   * @param other date to compare
   * @return {@code true} when this date is earlier
   * @throws NullPointerException if {@code other} is null
   */
  public boolean isBefore( Date other )
  {
    return compareTo( other ) < 0;
  }

  /**
   * Tests whether this date follows another.
   *
   * @param other date to compare
   * @return {@code true} when this date is later
   * @throws NullPointerException if {@code other} is null
   */
  public boolean isAfter( Date other )
  {
    return compareTo( other ) > 0;
  }

  /**
   * Tests membership in an inclusive, non-wrapping range.
   *
   * @param start inclusive lower bound
   * @param end inclusive upper bound
   * @return whether this date is within the bounds
   * @throws NullPointerException if either bound is null
   */
  public boolean isBetween( Date start, Date end )
  {
    Objects.requireNonNull( start, "start" );
    Objects.requireNonNull( end, "end" );
    return compareTo( start ) >= 0 && compareTo( end ) <= 0;
  }

  /**
   * Returns the signed number of days from this date to another.
   *
   * @param other target date
   * @return {@code other - this} in days
   * @throws NullPointerException if {@code other} is null
   */
  public long daysUntil( Date other )
  {
    Objects.requireNonNull( other, "other" );
    return (long) other.epochDay - epochDay;
  }

  /**
   * Returns the absolute distance between two dates.
   *
   * @param other date to compare
   * @return non-negative difference in days
   * @throws NullPointerException if {@code other} is null
   */
  public long differenceInDays( Date other )
  {
    return Math.abs( daysUntil( other ) );
  }

  // ================================= Formatting and parsing ================================

  /**
   * Formats this date using the current default locale and a {@link DateTimeFormatter} pattern.
   * In addition to standard date fields, the locale-independent half-year extension accepts
   * {@code U} for {@code 1} or {@code 2}, {@code UU} for {@code H1} or {@code H2}, and
   * {@code UUU} for {@code 1st half} or {@code 2nd half}. Quoted {@code U} characters remain
   * literals; more than three consecutive unquoted {@code U} characters are invalid.
   *
   * @param pattern formatting pattern
   * @return formatted text
   * @throws NullPointerException if {@code pattern} is null
   * @throws IllegalArgumentException if the pattern is invalid
   * @throws java.time.DateTimeException if formatting fails, including for an unsupported field
   */
  public String format( String pattern )
  {
    return TemporalPatternSupport.formatDate( epochDay, pattern );
  }

  /**
   * Formats this date with a precompiled formatter. The formatter may request compatible local
   * date fields and ISO chronology; formatters that require time, offset, zone, or instant fields
   * are unsupported.
   *
   * @param formatter formatter to use
   * @return formatted text
   * @throws NullPointerException if {@code formatter} is null
   * @throws java.time.DateTimeException if formatting fails, including for an unsupported field
   */
  public String format( DateTimeFormatter formatter )
  {
    return TemporalPatternSupport.formatDate( epochDay, formatter );
  }

  /**
   * Formats one to three leading numeric components as {@code year}, {@code year-MM}, or
   * {@code year-MM-dd}. The proleptic year uses its ordinary decimal representation and is not
   * padded to four digits; month and day are zero-padded. Values below one produce the year, and
   * values above three are ignored.
   *
   * @param components requested component count
   * @return compact component text
   */
  public String formatComponents( int components )
  {
    long ymd = EpochDayMath.packedYmd( epochDay );
    int year = (int) ( ymd >> 9 );
    int month = (int) ( ( ymd >> 5 ) & 0xFL );
    int day = (int) ( ymd & 0x1FL );
    StringBuilder result = new StringBuilder( 12 ).append( year );
    if ( components >= 2 )
      appendTwoDigits( result.append( '-' ), month );
    if ( components >= 3 )
      appendTwoDigits( result.append( '-' ), day );
    return result.toString();
  }

  /**
   * Parses intelligent date text using a parser configured from current system defaults.
   *
   * @param text text to parse
   * @return parsed date
   * @throws NullPointerException if {@code text} is null
   * @throws java.time.format.DateTimeParseException if the text cannot be parsed
   * @throws ArithmeticException if relative arithmetic or compact storage overflows
   */
  public static Date parse( String text )
  {
    return DateParser.systemDefault().parse( text );
  }

  /**
   * Parses date text using the supplied pattern and current default locale. The pattern supports
   * the same {@code U}, {@code UU}, and {@code UUU} half-year extension as
   * {@link #format(String)}.
   *
   * @param text text to parse
   * @param pattern parsing pattern
   * @return parsed date
   * @throws NullPointerException if either argument is null
   * @throws IllegalArgumentException if the pattern is invalid
   * @throws java.time.format.DateTimeParseException if the text cannot be parsed
   * @throws ArithmeticException if the resolved date is outside compact storage
   */
  public static Date parse( String text, String pattern )
  {
    return DateParser.systemDefault().parse( text, pattern );
  }

  /** {@inheritDoc} */
  @Override
  public int compareTo( Date other )
  {
    Objects.requireNonNull( other, "other" );
    return Integer.compare( epochDay, other.epochDay );
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals( Object object )
  {
    return object instanceof Date other && epochDay == other.epochDay;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode()
  {
    return Integer.hashCode( epochDay );
  }

  /** @return full numeric component text in {@code year-MM-dd} form */
  @Override
  public String toString()
  {
    return formatComponents( 3 );
  }

  private static void appendTwoDigits( StringBuilder result, int value )
  {
    if ( value < 10 )
      result.append( '0' );
    result.append( value );
  }
}
