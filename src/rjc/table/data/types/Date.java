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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.IsoFields;
import java.util.Objects;

/**************************************************************************************************/
/****************************** Date (calendar date, no time-zone) ********************************/
/**************************************************************************************************/

/**
 * Immutable calendar date without timezone information, stored internally as an {@code int} of
 * days since the epoch (1970-01-01). This gives approximately ±5.8 million years of range at
 * only 4 bytes per instance — more compact than {@link LocalDate}.
 * <p>
 * Most arithmetic and component-extraction operations are performed via allocation-free static
 * helper methods ({@link #addMonths}, {@link #addYears}, {@link #epochYear}, etc.) based on the
 * Hinnant civil/days algorithm. {@link LocalDate} is used only for ISO week-of-year calculation
 * and pattern-based formatting.
 */
public final class Date implements Serializable, Comparable<Date>
{
  private static final long serialVersionUID = 1L;

  // internal storage: days since 1970-01-01 (epoch day)
  private final int         m_epochDay;

  // ---- named constants ----
  public static final Date  MIN_VALUE        = new Date( Integer.MIN_VALUE );
  public static final Date  MAX_VALUE        = new Date( Integer.MAX_VALUE );
  public static final Date  EPOCH            = new Date( 0 );

  // ---- formatting constants ----
  // half-year pattern letter and placeholder used during format substitution
  private static final char HALF_YEAR_CHAR   = 'U';
  private static final char HALF_YEAR_S1     = '\u0001';                     // U → digit only
  private static final char HALF_YEAR_S2     = '\u0002';                     // UU → H1 / H2
  private static final char HALF_YEAR_S3     = '\u0003';                     // UUU → "1st half" / "2nd half"

  // epoch day 0 (1970-01-01) was a Thursday; adding 3 then floorMod 7 gives 0=Mon..6=Sun
  private static final int  DOW_OFFSET       = 3;

  /**
   * Enumeration of calendar interval units for rounding, stepping, and navigation.
   */
  public enum IntervalUnit
  {
    DAY, WEEK, MONTH, QUARTER_YEAR, HALF_YEAR, YEAR
  }

  // =================================== Constructor ===================================

  /***************************************** constructor ******************************************/
  /**
   * Private constructor — use factory methods to create instances.
   *
   * @param epochDay days since 1970-01-01 (may be negative)
   */
  private Date( int epochDay )
  {
    m_epochDay = epochDay;
  }

  // =================================== Factory Methods ===================================

  /***************************************** ofEpochDay *******************************************/
  /**
   * Creates a {@code Date} directly from an epoch day value.
   *
   * @param epochDay days since 1970-01-01 (negative for dates before the epoch)
   * @return a new {@code Date} instance
   */
  public static Date ofEpochDay( int epochDay )
  {
    return new Date( epochDay );
  }

  /********************************************** of **********************************************/
  /**
   * Creates a {@code Date} from year, month, and day components.
   * <p>
   * Month and day are validated without allocating a {@link LocalDate}. Year is not
   * range-checked beyond what can be represented as an {@code int} epoch day.
   *
   * @param year  proleptic Gregorian year (negative for BC)
   * @param month month-of-year (1 = January … 12 = December)
   * @param day   day-of-month (1–31 depending on month and leap year)
   * @return a new {@code Date} instance
   * @throws DateTimeException if month or day is out of range for the given year
   */
  public static Date of( int year, int month, int day )
  {
    // validate without allocating a LocalDate
    if ( month < 1 || month > 12 || day < 1 || day > monthLength( year, month ) )
      throw new DateTimeException( String.format( "Invalid date: %04d-%02d-%02d", year, month, day ) );
    return ofEpochDay( ymdToEpochDay( year, month, day ) );
  }

  /********************************************** of **********************************************/
  /**
   * Creates a {@code Date} from a {@link LocalDate}.
   *
   * @param localDate the {@link LocalDate} to convert (must not be null)
   * @return a new {@code Date} instance
   * @throws NullPointerException if {@code localDate} is null
   */
  public static Date of( LocalDate localDate )
  {
    Objects.requireNonNull( localDate, "localDate must not be null" );
    return ofEpochDay( (int) localDate.toEpochDay() );
  }

  /********************************************** now *********************************************/
  /**
   * Creates a {@code Date} representing today in the system default time-zone.
   *
   * @return a new {@code Date} for today
   */
  public static Date now()
  {
    return of( LocalDate.now() );
  }

  // =================================== Static Primitive Helpers ===================================

  /******************************************** packYMD *******************************************/
  // Hinnant civil_from_days: decomposes epoch day to packed long ((year<<9)|(month<<5)|day)
  // uses Math.floorDiv for era so negative epoch days (dates before 1970) are handled correctly;
  // all subsequent divisions are on non-negative values so Java truncation equals floor
  private static long packYMD( int epochDay )
  {
    int z = epochDay + 719468;
    int era = Math.floorDiv( z, 146097 );
    int doe = z - era * 146097; // day-of-era [0, 146096]
    int yoe = ( doe - doe / 1460 + doe / 36524 - doe / 146096 ) / 365; // year-of-era [0, 399]
    int y = yoe + era * 400;
    int doy = doe - ( 365 * yoe + yoe / 4 - yoe / 100 ); // day-of-year [0, 365]
    int mp = ( 5 * doy + 2 ) / 153; // month-prime [0, 11]
    int d = doy - ( 153 * mp + 2 ) / 5 + 1; // day [1, 31]
    int m = mp < 10 ? mp + 3 : mp - 9; // month [1, 12]
    if ( m <= 2 )
      y++;
    return ( (long) y << 9 ) | ( m << 5 ) | d;
  }

  /*************************************** ymdToEpochDay ******************************************/
  /**
   * Converts year, month, and day to an epoch day without allocating a {@link LocalDate}.
   * <p>
   * No validation is performed; callers are responsible for ensuring the components are valid.
   * Use the {@link #of(int, int, int)} factory for validated construction.
   *
   * @param year  proleptic Gregorian year
   * @param month month-of-year (1–12)
   * @param day   day-of-month (1–31)
   * @return epoch day (days since 1970-01-01)
   */
  public static int ymdToEpochDay( int year, int month, int day )
  {
    int y = year;
    if ( month <= 2 )
      y--;
    int era = Math.floorDiv( y, 400 );
    int yoe = y - era * 400; // year-of-era [0, 399]
    int mp = month > 2 ? month - 3 : month + 9; // month-prime [0, 11]
    int doy = ( 153 * mp + 2 ) / 5 + day - 1; // day-of-year [0, 365]
    int doe = yoe * 365 + yoe / 4 - yoe / 100 + doy; // day-of-era [0, 146096]
    return era * 146097 + doe - 719468;
  }

  /****************************************** isLeapYear ******************************************/
  /**
   * Returns {@code true} if the given proleptic Gregorian year is a leap year.
   * <p>
   * A year is a leap year if it is divisible by 4, except for century years, which must be
   * divisible by 400. Handles year 0 (1 BC) and negative years correctly.
   *
   * @param year proleptic Gregorian year
   * @return {@code true} if {@code year} is a leap year
   */
  public static boolean isLeapYear( int year )
  {
    // Java % on negatives gives 0 iff exactly divisible, so this is safe for BC years
    return ( year % 4 == 0 && year % 100 != 0 ) || year % 400 == 0;
  }

  /***************************************** monthLength ******************************************/
  /**
   * Returns the number of days in the given month of the given year (28–31).
   *
   * @param year  proleptic Gregorian year (used only for the February leap-year check)
   * @param month month-of-year (1–12)
   * @return length of the month in days
   */
  public static int monthLength( int year, int month )
  {
    return switch ( month )
    {
      case 2 -> isLeapYear( year ) ? 29 : 28;
      case 4, 6, 9, 11 -> 30;
      default -> 31;
    };
  }

  /****************************************** addMonths *******************************************/
  /**
   * Adds the specified number of months to an epoch day without object allocation.
   * <p>
   * Month-end clamping is applied: adding one month to the epoch day of 31 January yields
   * the epoch day of 28 or 29 February depending on the target year.
   *
   * @param epochDay days since 1970-01-01
   * @param months   months to add (may be negative)
   * @return new epoch day after adding {@code months}
   */
  public static int addMonths( int epochDay, int months )
  {
    if ( months == 0 )
      return epochDay;
    long ymd = packYMD( epochDay );
    int y = (int) ( ymd >> 9 );
    int m = (int) ( ( ymd >> 5 ) & 0xF );
    int d = (int) ( ymd & 0x1F );
    // floorDiv/Mod handle negative months (e.g. subtracting past January) correctly
    int totalM = y * 12 + ( m - 1 ) + months;
    int newYear = Math.floorDiv( totalM, 12 );
    int newMon = Math.floorMod( totalM, 12 ) + 1;
    // clamp day to the last valid day of the target month
    return ymdToEpochDay( newYear, newMon, Math.min( d, monthLength( newYear, newMon ) ) );
  }

  /******************************************* addYears *******************************************/
  /**
   * Adds the specified number of years to an epoch day without object allocation.
   * <p>
   * Leap-year clamping is applied: adding one year to the epoch day of 29 February yields
   * the epoch day of 28 February in a non-leap year.
   *
   * @param epochDay days since 1970-01-01
   * @param years    years to add (may be negative)
   * @return new epoch day after adding {@code years}
   */
  public static int addYears( int epochDay, int years )
  {
    // delegate to addMonths; years*12 cannot overflow int within the ±5.8 M year int range
    return addMonths( epochDay, years * 12 );
  }

  /******************************************* epochYear ******************************************/
  /**
   * Returns the proleptic Gregorian year for the given epoch day.
   *
   * @param epochDay days since 1970-01-01
   * @return proleptic Gregorian year
   */
  public static int epochYear( int epochDay )
  {
    return (int) ( packYMD( epochDay ) >> 9 );
  }

  /****************************************** epochMonth ******************************************/
  /**
   * Returns the month-of-year (1–12) for the given epoch day.
   *
   * @param epochDay days since 1970-01-01
   * @return month-of-year (1–12)
   */
  public static int epochMonth( int epochDay )
  {
    return (int) ( ( packYMD( epochDay ) >> 5 ) & 0xF );
  }

  /*************************************** epochDayOfMonth ****************************************/
  /**
   * Returns the day-of-month (1–31) for the given epoch day.
   *
   * @param epochDay days since 1970-01-01
   * @return day-of-month (1–31)
   */
  public static int epochDayOfMonth( int epochDay )
  {
    return (int) ( packYMD( epochDay ) & 0x1F );
  }

  // =================================== Accessor Methods ===================================

  /***************************************** getEpochDay ******************************************/
  /**
   * Returns the raw epoch day value (days since 1970-01-01).
   * <p>
   * Negative for dates before the epoch.
   *
   * @return epoch day value
   */
  public int getEpochDay()
  {
    return m_epochDay;
  }

  /******************************************** getYear *******************************************/
  /**
   * Returns the year component of this date.
   * <p>
   * Follows the proleptic Gregorian calendar: year 0 is 1 BC, year −1 is 2 BC, and so on.
   *
   * @return proleptic Gregorian year
   */
  public int getYear()
  {
    return epochYear( m_epochDay );
  }

  /******************************************* getMonth *******************************************/
  /**
   * Returns the month-of-year as an integer (1 = January … 12 = December).
   *
   * @return month-of-year (1–12)
   */
  public int getMonth()
  {
    return epochMonth( m_epochDay );
  }

  /***************************************** getMonthEnum *****************************************/
  /**
   * Returns the month-of-year as a {@link Month} enum value.
   * <p>
   * Prefer {@link #getMonth()} for simple numeric comparisons; use this method when
   * enum-specific behaviour (e.g. {@link Month#length}) is needed.
   *
   * @return {@link Month} enum for this date
   */
  public Month getMonthEnum()
  {
    return Month.of( epochMonth( m_epochDay ) );
  }

  /**************************************** getDayOfMonth *****************************************/
  /**
   * Returns the day-of-month component (1–31 depending on month and year).
   *
   * @return day-of-month
   */
  public int getDayOfMonth()
  {
    return epochDayOfMonth( m_epochDay );
  }

  /***************************************** getDayOfWeek *****************************************/
  /**
   * Returns the day-of-week for this date.
   * <p>
   * Follows ISO-8601: Monday = 1 … Sunday = 7.
   *
   * @return {@link DayOfWeek} enum
   */
  public DayOfWeek getDayOfWeek()
  {
    // floorMod gives 0=Mon..6=Sun; DayOfWeek.of expects 1=Mon..7=Sun
    return DayOfWeek.of( Math.floorMod( m_epochDay + DOW_OFFSET, 7 ) + 1 );
  }

  /***************************************** getDayOfYear *****************************************/
  /**
   * Returns the day-of-year (1 on 1 January; 365 or 366 on 31 December).
   *
   * @return day-of-year (1–366)
   */
  public int getDayOfYear()
  {
    // subtract epoch day of 1 Jan same year; +1 for 1-based result
    int y = epochYear( m_epochDay );
    return m_epochDay - ymdToEpochDay( y, 1, 1 ) + 1;
  }

  /**************************************** getWeekOfYear *****************************************/
  /**
   * Returns the ISO-8601 week number within the week-based year (1–53).
   * <p>
   * Week 1 is the week containing the first Thursday of January. The week-based year may
   * differ from the calendar year for dates near 1 January.
   * <p>
   * This method allocates a {@link LocalDate} internally as no pure-integer equivalent
   * exists for the ISO week-based year algorithm.
   *
   * @return ISO week-of-week-based-year (1–53)
   */
  public int getWeekOfYear()
  {
    // IsoFields.WEEK_OF_WEEK_BASED_YEAR requires a LocalDate; no pure-int equivalent
    return toLocalDate().get( IsoFields.WEEK_OF_WEEK_BASED_YEAR );
  }

  /****************************************** getQuarter ******************************************/
  /**
   * Returns the calendar quarter this date falls in (1–4).
   * <p>
   * Q1 = January–March, Q2 = April–June, Q3 = July–September, Q4 = October–December.
   *
   * @return quarter (1–4)
   */
  public int getQuarter()
  {
    // integer division maps months 1-3→1, 4-6→2, 7-9→3, 10-12→4
    return ( getMonth() - 1 ) / 3 + 1;
  }

  /***************************************** getHalfYear ******************************************/
  /**
   * Returns the calendar half-year this date falls in (1 or 2).
   * <p>
   * H1 = January–June, H2 = July–December.
   *
   * @return half-year (1 or 2)
   */
  public int getHalfYear()
  {
    return getMonth() <= 6 ? 1 : 2;
  }

  /***************************************** isLeapYear *******************************************/
  /**
   * Returns {@code true} if this date falls within a leap year.
   * <p>
   * Delegates to {@link #isLeapYear(int)}.
   *
   * @return {@code true} if the year is a leap year
   */
  public boolean isLeapYear()
  {
    return isLeapYear( epochYear( m_epochDay ) );
  }

  /**************************************** lengthOfMonth *****************************************/
  /**
   * Returns the number of days in the month of this date (28–31).
   *
   * @return length of the month in days
   */
  public int lengthOfMonth()
  {
    long ymd = packYMD( m_epochDay );
    return monthLength( (int) ( ymd >> 9 ), (int) ( ( ymd >> 5 ) & 0xF ) );
  }

  /***************************************** lengthOfYear *****************************************/
  /**
   * Returns the number of days in the year of this date (365 or 366).
   *
   * @return length of the year in days
   */
  public int lengthOfYear()
  {
    return isLeapYear() ? 366 : 365;
  }

  // =================================== Rounding Methods ===================================

  /****************************************** roundDown *******************************************/
  /**
   * Returns a copy of this date rounded down (floored) to the nearest boundary of the
   * specified interval unit. If this date is already exactly on a boundary, the same
   * instance is returned.
   * <p>
   * Boundaries per unit:
   * <ul>
   *   <li>{@code DAY} — always aligned; returns {@code this}</li>
   *   <li>{@code WEEK} — Monday of the current ISO week</li>
   *   <li>{@code MONTH} — 1st of the current month</li>
   *   <li>{@code QUARTER_YEAR} — 1st of January, April, July, or October</li>
   *   <li>{@code HALF_YEAR} — 1st of January or 1st of July</li>
   *   <li>{@code YEAR} — 1st of January</li>
   * </ul>
   * All arms use allocation-free integer arithmetic.
   *
   * @param unit the interval unit to round to (must not be null)
   * @return {@code this} if already on a boundary, otherwise a new {@code Date}
   * @throws NullPointerException if {@code unit} is null
   */
  public Date roundDown( IntervalUnit unit )
  {
    Objects.requireNonNull( unit, "unit must not be null" );
    return switch ( unit )
    {
      case DAY -> this;

      case WEEK -> {
        // offset 0=Mon..6=Sun; subtract offset to land on Monday
        int offset = Math.floorMod( m_epochDay + DOW_OFFSET, 7 );
        yield offset == 0 ? this : ofEpochDay( m_epochDay - offset );
      }

      case MONTH -> {
        long ymd = packYMD( m_epochDay );
        int y = (int) ( ymd >> 9 ), m = (int) ( ( ymd >> 5 ) & 0xF ), d = (int) ( ymd & 0x1F );
        yield d == 1 ? this : ofEpochDay( ymdToEpochDay( y, m, 1 ) );
      }

      case QUARTER_YEAR -> {
        long ymd = packYMD( m_epochDay );
        int y = (int) ( ymd >> 9 ), m = (int) ( ( ymd >> 5 ) & 0xF ), d = (int) ( ymd & 0x1F );
        int firstM = ( ( m - 1 ) / 3 ) * 3 + 1;
        yield ( m == firstM && d == 1 ) ? this : ofEpochDay( ymdToEpochDay( y, firstM, 1 ) );
      }

      case HALF_YEAR -> {
        long ymd = packYMD( m_epochDay );
        int y = (int) ( ymd >> 9 ), m = (int) ( ( ymd >> 5 ) & 0xF ), d = (int) ( ymd & 0x1F );
        int firstM = m <= 6 ? 1 : 7;
        yield ( m == firstM && d == 1 ) ? this : ofEpochDay( ymdToEpochDay( y, firstM, 1 ) );
      }

      case YEAR -> {
        long ymd = packYMD( m_epochDay );
        int y = (int) ( ymd >> 9 ), m = (int) ( ( ymd >> 5 ) & 0xF ), d = (int) ( ymd & 0x1F );
        yield ( m == 1 && d == 1 ) ? this : ofEpochDay( ymdToEpochDay( y, 1, 1 ) );
      }
    };
  }

  /******************************************* roundUp ********************************************/
  /**
   * Returns a copy of this date rounded up (ceiled) to the nearest boundary of the
   * specified interval unit. If this date is already exactly on a boundary, the same
   * instance is returned.
   * <p>
   * Boundaries are the same as for {@link #roundDown(IntervalUnit)}:
   * <ul>
   *   <li>{@code DAY} — always aligned; returns {@code this}</li>
   *   <li>{@code WEEK} — Monday of the next ISO week, or {@code this} if already Monday</li>
   *   <li>{@code MONTH} — 1st of the next month, or {@code this} if already the 1st</li>
   *   <li>{@code QUARTER_YEAR} — 1st of the next quarter, or {@code this}</li>
   *   <li>{@code HALF_YEAR} — next 1st January or 1st July, or {@code this}</li>
   *   <li>{@code YEAR} — 1st January of the next year, or {@code this}</li>
   * </ul>
   * All arms use allocation-free integer arithmetic.
   *
   * @param unit the interval unit to round to (must not be null)
   * @return {@code this} if already on a boundary, otherwise a new {@code Date}
   * @throws NullPointerException if {@code unit} is null
   */
  public Date roundUp( IntervalUnit unit )
  {
    Objects.requireNonNull( unit, "unit must not be null" );
    return switch ( unit )
    {
      case DAY -> this;

      case WEEK -> {
        // offset 0=Mon..6=Sun; advance (7 - offset) days to reach next Monday
        int offset = Math.floorMod( m_epochDay + DOW_OFFSET, 7 );
        yield offset == 0 ? this : ofEpochDay( m_epochDay + ( 7 - offset ) );
      }

      case MONTH -> {
        long ymd = packYMD( m_epochDay );
        int y = (int) ( ymd >> 9 ), m = (int) ( ( ymd >> 5 ) & 0xF ), d = (int) ( ymd & 0x1F );
        if ( d == 1 )
          yield this;
        // advance to 1st of next month, wrapping year if needed
        int nextM = m + 1, nextY = y;
        if ( nextM > 12 )
        {
          nextM = 1;
          nextY++;
        }
        yield ofEpochDay( ymdToEpochDay( nextY, nextM, 1 ) );
      }

      case QUARTER_YEAR -> {
        long ymd = packYMD( m_epochDay );
        int y = (int) ( ymd >> 9 ), m = (int) ( ( ymd >> 5 ) & 0xF ), d = (int) ( ymd & 0x1F );
        int firstM = ( ( m - 1 ) / 3 ) * 3 + 1;
        if ( m == firstM && d == 1 )
          yield this;
        // first month of next quarter, wrapping year if needed
        int nextM = firstM + 3, nextY = y;
        if ( nextM > 12 )
        {
          nextM -= 12;
          nextY++;
        }
        yield ofEpochDay( ymdToEpochDay( nextY, nextM, 1 ) );
      }

      case HALF_YEAR -> {
        long ymd = packYMD( m_epochDay );
        int y = (int) ( ymd >> 9 ), m = (int) ( ( ymd >> 5 ) & 0xF ), d = (int) ( ymd & 0x1F );
        int firstM = m <= 6 ? 1 : 7;
        if ( m == firstM && d == 1 )
          yield this;
        // next boundary is Jul 1 same year (H1) or Jan 1 next year (H2)
        yield firstM == 1 ? ofEpochDay( ymdToEpochDay( y, 7, 1 ) ) : ofEpochDay( ymdToEpochDay( y + 1, 1, 1 ) );
      }

      case YEAR -> {
        long ymd = packYMD( m_epochDay );
        int y = (int) ( ymd >> 9 ), m = (int) ( ( ymd >> 5 ) & 0xF ), d = (int) ( ymd & 0x1F );
        yield ( m == 1 && d == 1 ) ? this : ofEpochDay( ymdToEpochDay( y + 1, 1, 1 ) );
      }
    };
  }

  // =================================== Arithmetic Methods ===================================

  /******************************************** plusDays ******************************************/
  /**
   * Returns a copy of this date with the specified number of days added.
   *
   * @param days days to add (may be negative)
   * @return {@code this} if {@code days == 0}, otherwise a new {@code Date}
   */
  public Date plusDays( int days )
  {
    if ( days == 0 )
      return this;
    return ofEpochDay( m_epochDay + days );
  }

  /******************************************* plusWeeks ******************************************/
  /**
   * Returns a copy of this date with the specified number of weeks added.
   * <p>
   * Equivalent to {@code plusDays(weeks * 7)}.
   *
   * @param weeks weeks to add (may be negative)
   * @return a new {@code Date} with the weeks added
   */
  public Date plusWeeks( int weeks )
  {
    return plusDays( weeks * 7 );
  }

  /******************************************* plusMonths *****************************************/
  /**
   * Returns a copy of this date with the specified number of months added.
   * <p>
   * Month-end clamping is applied: adding one month to 31 January yields 28/29 February.
   *
   * @param months months to add (may be negative)
   * @return {@code this} if {@code months == 0}, otherwise a new {@code Date}
   */
  public Date plusMonths( int months )
  {
    if ( months == 0 )
      return this;
    return ofEpochDay( addMonths( m_epochDay, months ) );
  }

  /******************************************* plusYears ******************************************/
  /**
   * Returns a copy of this date with the specified number of years added.
   * <p>
   * Leap-year clamping is applied: adding one year to 29 February yields 28 February
   * in a non-leap year.
   *
   * @param years years to add (may be negative)
   * @return {@code this} if {@code years == 0}, otherwise a new {@code Date}
   */
  public Date plusYears( int years )
  {
    if ( years == 0 )
      return this;
    return ofEpochDay( addYears( m_epochDay, years ) );
  }

  /****************************************** plusInterval ****************************************/
  /**
   * Returns a copy of this date with the specified number of intervals added.
   * <p>
   * Positive values advance the date; negative values retreat it.
   * <ul>
   *   <li>{@code DAY} — epoch-day arithmetic (no {@link LocalDate} allocation)</li>
   *   <li>{@code WEEK} — epoch-day arithmetic × 7</li>
   *   <li>{@code MONTH} — allocation-free calendar-month arithmetic</li>
   *   <li>{@code QUARTER_YEAR} — {@code count × 3} calendar months</li>
   *   <li>{@code HALF_YEAR} — {@code count × 6} calendar months</li>
   *   <li>{@code YEAR} — allocation-free calendar-year arithmetic</li>
   * </ul>
   *
   * @param count number of intervals to add (positive = forward, negative = backward)
   * @param unit  the interval unit (must not be null)
   * @return {@code this} if {@code count == 0}, otherwise a new {@code Date}
   * @throws NullPointerException if {@code unit} is null
   */
  public Date plusInterval( int count, IntervalUnit unit )
  {
    Objects.requireNonNull( unit, "unit must not be null" );
    if ( count == 0 )
      return this;
    return switch ( unit )
    {
      case DAY -> plusDays( count );
      case WEEK -> plusDays( count * 7 );
      case MONTH -> plusMonths( count );
      case QUARTER_YEAR -> plusMonths( count * 3 );
      case HALF_YEAR -> plusMonths( count * 6 );
      case YEAR -> plusYears( count );
    };
  }

  // =================================== Predicate Methods ===================================

  /******************************************** isBefore ******************************************/
  /**
   * Returns {@code true} if this date is strictly before {@code other}.
   *
   * @param other the date to compare against (must not be null)
   * @return {@code true} if this date is chronologically earlier
   * @throws NullPointerException if {@code other} is null
   */
  public boolean isBefore( Date other )
  {
    return m_epochDay < other.m_epochDay;
  }

  /********************************************* isAfter ******************************************/
  /**
   * Returns {@code true} if this date is strictly after {@code other}.
   *
   * @param other the date to compare against (must not be null)
   * @return {@code true} if this date is chronologically later
   * @throws NullPointerException if {@code other} is null
   */
  public boolean isAfter( Date other )
  {
    return m_epochDay > other.m_epochDay;
  }

  /********************************************* isEqual ******************************************/
  /**
   * Returns {@code true} if this date represents the same calendar day as {@code other}.
   * <p>
   * Returns {@code false} rather than throwing when {@code other} is null, making this
   * convenient for null-safe equality tests.
   *
   * @param other the date to compare against (may be null)
   * @return {@code true} if both dates have the same epoch day
   */
  public boolean isEqual( Date other )
  {
    // null-safe: consistent with ChronoLocalDate.isEqual convention
    return other != null && m_epochDay == other.m_epochDay;
  }

  /******************************************* isBetween ******************************************/
  /**
   * Returns {@code true} if this date falls within the inclusive range {@code [start, end]}.
   * <p>
   * Returns {@code false} if {@code start} is after {@code end} (empty range).
   *
   * @param start range start, inclusive (must not be null)
   * @param end   range end, inclusive (must not be null)
   * @return {@code true} if {@code start <= this <= end}
   * @throws NullPointerException if either argument is null
   */
  public boolean isBetween( Date start, Date end )
  {
    Objects.requireNonNull( start, "start must not be null" );
    Objects.requireNonNull( end, "end must not be null" );
    return m_epochDay >= start.m_epochDay && m_epochDay <= end.m_epochDay;
  }

  // =================================== Difference Methods ===================================

  /******************************************* daysUntil ******************************************/
  /**
   * Returns the signed number of days from this date to {@code other}.
   * <p>
   * Positive if {@code other} is later, negative if earlier, zero if equal.
   *
   * @param other the target date (must not be null)
   * @return signed day difference
   * @throws NullPointerException if {@code other} is null
   */
  public int daysUntil( Date other )
  {
    Objects.requireNonNull( other, "other must not be null" );
    return other.m_epochDay - m_epochDay;
  }

  /*************************************** differenceInDays ***************************************/
  /**
   * Returns the absolute number of days between this date and {@code other}.
   * <p>
   * Always non-negative. For a signed difference use {@link #daysUntil}.
   *
   * @param other the date to compare with (must not be null)
   * @return absolute day difference
   * @throws NullPointerException if {@code other} is null
   */
  public int differenceInDays( Date other )
  {
    Objects.requireNonNull( other, "other must not be null" );
    return Math.abs( m_epochDay - other.m_epochDay );
  }

  // =================================== Formatting Methods ===================================

  /******************************************** toString ******************************************/
  /**
   * Returns this date as an ISO-8601 string ({@code yyyy-MM-dd}), e.g. {@code "2026-02-27"}.
   *
   * @return ISO-8601 date string
   */
  @Override
  public String toString()
  {
    return formatDate( 3 );
  }

  /********************************************* format *******************************************/
  /**
   * Returns this date formatted with the specified number of components.
   * <table border="1" summary="Component count to format mapping">
   * <tr><th>components</th><th>format</th><th>example</th></tr>
   * <tr><td>1</td><td>{@code yyyy}</td><td>{@code 2026}</td></tr>
   * <tr><td>2</td><td>{@code yyyy-MM}</td><td>{@code 2026-02}</td></tr>
   * <tr><td>3+</td><td>{@code yyyy-MM-dd}</td><td>{@code 2026-02-27}</td></tr>
   * </table>
   *
   * @param components number of date components to include (must be ≥ 1)
   * @return formatted date string
   * @throws IllegalArgumentException if {@code components} is less than 1
   */
  public String format( int components )
  {
    if ( components < 1 )
      throw new IllegalArgumentException( "components must be >= 1, got: " + components );
    return formatDate( components );
  }

  /******************************************** toString ******************************************/
  /**
   * Formats this date using the specified pattern string.
   * <p>
   * Supports all standard {@link DateTimeFormatter} patterns plus an extended half-year
   * letter {@code B}:
   * <ul>
   *   <li>{@code B}   — half-year number: {@code 1} or {@code 2}</li>
   *   <li>{@code BB}  — half-year label: {@code H1} or {@code H2}</li>
   *   <li>{@code BBB} — half-year long form: {@code "1st half"} or {@code "2nd half"}</li>
   * </ul>
   *
   * @param pattern the formatting pattern (must not be null)
   * @return the formatted date string
   * @throws IllegalArgumentException if the pattern is invalid or contains more than three
   *                                  {@code B} letters
   * @throws NullPointerException     if {@code pattern} is null
   */
  public String toString( String pattern )
  {
    Objects.requireNonNull( pattern, "pattern must not be null" );
    // fast path: no half-year letter present
    if ( pattern.indexOf( HALF_YEAR_CHAR ) < 0 )
      return toLocalDate().format( DateTimeFormatter.ofPattern( pattern ) );
    return formatWithHalfYear( pattern );
  }

  // =================================== Parsing Methods ===================================

  /********************************************* parse ********************************************/
  /**
   * Parses a date string using the specified pattern via {@link DateParser}.
   *
   * @param text    the date string to parse (must not be null)
   * @param pattern the {@link DateTimeFormatter} pattern (must not be null)
   * @return a new {@code Date} instance
   * @throws DateTimeParseException if {@code text} cannot be parsed with {@code pattern}
   * @throws NullPointerException   if either argument is null
   */
  public static Date parse( String text, String pattern )
  {
    return DateParser.parse( text, pattern );
  }

  /********************************************* parse ********************************************/
  /**
   * Parses a date string using {@link DateParser} with automatic format detection.
   * <p>
   * Supports ISO format, common locale formats, relative expressions such as
   * {@code "today"} or {@code "next monday"}, and other formats handled by {@link DateParser}.
   *
   * @param text the date string to parse (must not be null)
   * @return a new {@code Date} instance
   * @throws DateTimeParseException if {@code text} cannot be parsed with any supported format
   * @throws NullPointerException   if {@code text} is null
   */
  public static Date parse( String text )
  {
    return DateParser.parse( text );
  }

  // =================================== Conversion Methods ===================================

  /***************************************** toLocalDate ******************************************/
  /**
   * Converts this {@code Date} to a {@link LocalDate}.
   * <p>
   * The sentinel values {@link #MIN_VALUE} and {@link #MAX_VALUE} cannot be converted and
   * will throw a {@link DateTimeException}.
   *
   * @return equivalent {@link LocalDate}
   * @throws DateTimeException if this date's epoch day is outside {@link LocalDate}'s
   *                           supported range
   */
  public LocalDate toLocalDate()
  {
    return LocalDate.ofEpochDay( m_epochDay );
  }

  // =================================== Object Methods ===================================

  /********************************************* equals *******************************************/
  /**
   * Returns {@code true} if {@code obj} is a {@code Date} representing the same calendar day.
   * <p>
   * Consistent with {@link #compareTo}: two dates are equal iff their epoch days are equal.
   *
   * @param obj the object to compare with (may be null)
   * @return {@code true} if {@code obj} is a {@code Date} with the same epoch day
   */
  @Override
  public boolean equals( Object obj )
  {
    return obj instanceof Date other && m_epochDay == other.m_epochDay;
  }

  /******************************************** hashCode ******************************************/
  /**
   * Returns a hash code for this date.
   * <p>
   * Consistent with {@link #equals}: equal dates have equal hash codes.
   *
   * @return hash code equal to the epoch day value
   */
  @Override
  public int hashCode()
  {
    return m_epochDay;
  }

  /******************************************* compareTo ******************************************/
  /**
   * Compares this date to another date for natural chronological ordering.
   *
   * @param other the date to compare to (must not be null)
   * @return negative if earlier, zero if equal, positive if later
   * @throws NullPointerException if {@code other} is null
   */
  @Override
  public int compareTo( Date other )
  {
    return Integer.compare( m_epochDay, other.m_epochDay );
  }

  // =================================== Private Helpers ===================================

  /************************************** formatWithHalfYear **************************************/
  // delegates to pattern pre-processing then post-substitution for half-year 'B' patterns
  private String formatWithHalfYear( String pattern )
  {
    // replace 'B' sequences with quoted placeholders safe for DateTimeFormatter
    String processed = replaceHalfYearPatterns( pattern );
    String result = toLocalDate().format( DateTimeFormatter.ofPattern( processed ) );
    return substituteHalfYearValues( result );
  }

  /************************************ replaceHalfYearPatterns ***********************************/
  // scans pattern and replaces runs of 'U' outside quotes with raw control-char sentinels
  private String replaceHalfYearPatterns( String pattern )
  {
    StringBuilder sb = new StringBuilder( pattern.length() );
    boolean inQuotes = false;
    int i = 0;

    while ( i < pattern.length() )
    {
      char ch = pattern.charAt( i );

      if ( ch == '\'' )
      {
        // toggle quote state; '' (escaped apostrophe) toggles twice — net zero, correct
        inQuotes = !inQuotes;
        sb.append( ch );
        i++;
      }
      else if ( !inQuotes && ch == HALF_YEAR_CHAR )
      {
        // count consecutive 'U' characters
        int count = 0;
        while ( i < pattern.length() && pattern.charAt( i ) == HALF_YEAR_CHAR )
        {
          count++;
          i++;
        }
        if ( count > 3 )
          throw new IllegalArgumentException( "Too many '" + HALF_YEAR_CHAR + "' pattern letters: " + count );
        // inject raw sentinel — no quoting needed, no adjacency collision possible
        sb.append( switch ( count )
        {
          case 1 -> HALF_YEAR_S1;
          case 2 -> HALF_YEAR_S2;
          default -> HALF_YEAR_S3;
        } );
      }
      else
      {
        sb.append( ch );
        i++;
      }
    }

    return sb.toString();
  }

  /*********************************** substituteHalfYearValues ***********************************/
  // replaces control-char sentinels with the actual half-year text for this date
  private String substituteHalfYearValues( String formatted )
  {
    int hy = getHalfYear();
    // replace in descending sentinel order — avoids any prefix-match ambiguity
    return formatted.replace( String.valueOf( HALF_YEAR_S3 ), hy == 1 ? "1st half" : "2nd half" )
        .replace( String.valueOf( HALF_YEAR_S2 ), "H" + hy )
        .replace( String.valueOf( HALF_YEAR_S1 ), String.valueOf( hy ) );
  }

  /****************************************** formatDate ******************************************/
  // hand-rolled date formatter — avoids DateTimeFormatter overhead for table cell rendering
  private String formatDate( int components )
  {
    long ymd = packYMD( m_epochDay );
    int y = (int) ( ymd >> 9 );
    int m = (int) ( ( ymd >> 5 ) & 0xF );
    int d = (int) ( ymd & 0x1F );

    // pre-size to maximum expected output length (10 for full yyyy-MM-dd)
    StringBuilder sb = new StringBuilder( 10 );
    sb.append( y );

    if ( components < 2 )
      return sb.toString();

    // append -MM with zero-padding
    sb.append( '-' );
    if ( m < 10 )
      sb.append( '0' );
    sb.append( m );

    if ( components < 3 )
      return sb.toString();

    // append -dd with zero-padding
    sb.append( '-' );
    if ( d < 10 )
      sb.append( '0' );
    sb.append( d );

    return sb.toString();
  }
}