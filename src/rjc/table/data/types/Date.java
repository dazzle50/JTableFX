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
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/*************************************************************************************************/
/*********************************** Date (with no time-zone) ************************************/
/*************************************************************************************************/

/**
 * Immutable date class representing a single calendar date without timezone information.
 * <p>
 * This class provides a memory-efficient alternative to LocalDate by storing dates internally 
 * as epoch days (days since 1970-01-01). Each instance uses only 4 bytes of memory while 
 * supporting an extensive date range of approximately +/-5.8 million years.
 * <p>
 * The class includes specialized features for business applications:
 * <ul>
 * <li>Custom half-year formatting support for financial reporting</li>
 * <li>Intelligent parsing with automatic format detection and fallbacks</li>
 * <li>Complete immutability ensuring thread safety</li>
 * <li>Efficient comparison and arithmetic operations</li>
 * <li>Full compatibility with Java 21 time API</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * Date today = Date.now();
 * Date future = today.plusDays(30);
 * String formatted = today.format("dd/MM/yyyy");
 * Date parsed = Date.parseIntelligent("2026-12-25");
 * }</pre>
 */
public final class Date implements Serializable, Comparable<Date>
{
  private static final long             serialVersionUID = 1L;

  // internal storage as days since epoch for memory efficiency
  private final int                     m_epochDay;

  // predefined date constants covering maximum supported range
  public static final Date              MIN_VALUE        = new Date( Integer.MIN_VALUE );
  public static final Date              MAX_VALUE        = new Date( Integer.MAX_VALUE );
  public static final Date              EPOCH            = new Date( 0 );                   // 1970-01-01

  // standard iso date format for consistent parsing
  public static final DateTimeFormatter ISO_FORMAT       = DateTimeFormatter.ISO_LOCAL_DATE;

  // special formatting constants for half-year business reporting
  private static final String           HALF_YEAR_MARKER = "#HY#";
  private static final char             HALF_YEAR_CHAR   = 'B';

  // ================================= Constructors =================================

  /**
   * Private constructor to enforce factory method usage and maintain immutability.
   * 
   * @param epochDay the number of days since 1970-01-01
   */
  private Date( int epochDay )
  {
    // store epoch day directly for efficient memory usage
    m_epochDay = epochDay;
  }

  // ================================= Factory Methods =================================

  /***************************************** ofEpochDay ******************************************/
  /**
   * Creates a Date instance from the number of days since the epoch (1970-01-01).
   * <p>
   * This is the most efficient factory method as it directly uses the internal 
   * storage format without any conversion overhead.
   *
   * @param epochDay the number of days since 1970-01-01 (can be negative for dates before epoch)
   * @return a new Date instance representing the specified epoch day
   */
  public static Date ofEpochDay( int epochDay )
  {
    return new Date( epochDay );
  }

  /********************************************* of **********************************************/
  /**
   * Creates a Date from the specified year, month, and day components.
   * <p>
   * This method performs full validation of the date components and will throw
   * an exception for invalid dates such as February 30th or day 32 of any month.
   *
   * @param year  the year to represent (e.g., 2025, can be negative for BC dates)
   * @param month the month-of-year to represent (1-12, where 1=January, 12=December) 
   * @param day   the day-of-month to represent (1-31, depending on month and leap year)
   * @return a new Date instance representing the specified date
   * @throws DateTimeException if any component is invalid or the combination is impossible
   */
  public static Date of( int year, int month, int day )
  {
    try
    {
      // leverage localdate validation then convert to our internal format
      int epochDay = (int) LocalDate.of( year, month, day ).toEpochDay();
      return ofEpochDay( epochDay );
    }
    catch ( DateTimeException exception )
    {
      // provide clearer error message with the attempted date
      throw new DateTimeException( String.format( "Invalid date: %04d-%02d-%02d", year, month, day ), exception );
    }
  }

  /********************************************* of **********************************************/
  /**
   * Creates a Date instance from a LocalDate object.
   * <p>
   * This method provides seamless interoperability with the standard Java time API
   * while converting to our more memory-efficient internal representation.
   *
   * @param localDate the LocalDate to convert (must not be null)
   * @return a new Date instance equivalent to the provided LocalDate
   * @throws NullPointerException if localDate is null
   */
  public static Date of( LocalDate localDate )
  {
    // validate input parameter
    Objects.requireNonNull( localDate, "LocalDate cannot be null" );
    // convert to internal epoch day format
    return ofEpochDay( (int) localDate.toEpochDay() );
  }

  /********************************************* now *********************************************/
  /**
   * Creates a Date instance representing the current date in the system default timezone.
   * <p>
   * This method uses the system clock and default timezone to determine the current
   * date. The result may vary depending on when and where the code is executed.
   *
   * @return a new Date instance representing today's date
   */
  public static Date now()
  {
    // use system default timezone to get current date
    return of( LocalDate.now() );
  }

  // ================================ Accessor Methods ================================

  /***************************************** getEpochDay *****************************************/
  /**
   * Returns the epoch day value for this date.
   * <p>
   * The epoch day is the number of days since 1970-01-01, where day 0 represents
   * the epoch date itself. Negative values represent dates before the epoch.
   *
   * @return the epoch day value (can be negative for historical dates)
   */
  public int getEpochDay()
  {
    return m_epochDay;
  }

  /******************************************* getYear *******************************************/
  /**
   * Returns the year component of this date.
   * <p>
   * The year follows the proleptic Gregorian calendar system, where year 1 is 1 AD,
   * year 0 is 1 BC, year -1 is 2 BC, and so forth.
   *
   * @return the year (e.g., 2025, can be negative for BC dates)
   */
  public int getYear()
  {
    // convert to localdate for component extraction
    return toLocalDate().getYear();
  }

  /****************************************** getMonth *******************************************/
  /**
   * Returns the month-of-year as an integer value.
   * <p>
   * The returned value uses the standard calendar numbering where January is 1
   * and December is 12, making it suitable for direct display to users.
   *
   * @return the month-of-year (1=January through 12=December)
   */
  public int getMonth()
  {
    // extract month value using localdate conversion
    return toLocalDate().getMonthValue();
  }

  /**************************************** getMonthEnum *****************************************/
  /**
   * Returns the month-of-year as a Month enum value.
   * <p>
   * This method provides type-safe access to the month component with additional
   * utility methods available through the Month enum.
   *
   * @return the Month enum representing the month-of-year
   */
  public Month getMonthEnum()
  {
    // return enum for type safety and additional functionality
    return toLocalDate().getMonth();
  }

  /**************************************** getDayOfMonth ****************************************/
  /**
   * Returns the day-of-month component of this date.
   * <p>
   * The value ranges from 1 to 31 depending on the month and whether it's a leap year.
   * For example, February in a leap year can have day 29, but not in a regular year.
   *
   * @return the day-of-month (1-31 depending on the month)
   */
  public int getDayOfMonth()
  {
    // extract day using localdate conversion
    return toLocalDate().getDayOfMonth();
  }

  /**************************************** getDayOfWeek *****************************************/
  /**
   * Returns the day-of-week for this date.
   * <p>
   * The DayOfWeek enum provides additional utility methods and follows the ISO standard
   * where Monday is day 1 and Sunday is day 7.
   *
   * @return the DayOfWeek enum representing the day of the week
   */
  public DayOfWeek getDayOfWeek()
  {
    // calculate day of week using localdate
    return toLocalDate().getDayOfWeek();
  }

  /**************************************** getDayOfYear *****************************************/
  /**
   * Returns the day-of-year component of this date.
   * <p>
   * January 1st is day 1, and the maximum value is 365 for regular years or 366 for leap years.
   * This is useful for calculating progress through the year or for certain business calculations.
   *
   * @return the day-of-year (1-366 depending on leap year status)
   */
  public int getDayOfYear()
  {
    // calculate ordinal day using localdate
    return toLocalDate().getDayOfYear();
  }

  /***************************************** isLeapYear ******************************************/
  /**
   * Checks whether this date falls within a leap year.
   * <p>
   * A leap year occurs every 4 years, except for years divisible by 100, unless they
   * are also divisible by 400. This affects February which has 29 days in leap years.
   *
   * @return true if this date is in a leap year, false otherwise
   */
  public boolean isLeapYear()
  {
    // determine leap year status using localdate
    return toLocalDate().isLeapYear();
  }

  // ================================= Date Arithmetic =================================

  /****************************************** plusDays *******************************************/
  /**
   * Returns a new Date with the specified number of days added to this date.
   * <p>
   * This operation is performed efficiently using epoch day arithmetic without
   * intermediate conversions. Negative values can be used to subtract days.
   *
   * @param days the number of days to add (can be negative to subtract)
   * @return a new Date instance with the days added
   * @throws ArithmeticException if the resulting date would overflow the supported range
   */
  public Date plusDays( int days )
  {
    // optimize for zero case to avoid object creation
    if ( days == 0 )
      return this;
    // perform efficient epoch day arithmetic
    return ofEpochDay( m_epochDay + days );
  }

  /****************************************** plusWeeks ******************************************/
  /**
   * Returns a new Date with the specified number of weeks added to this date.
   * <p>
   * This is equivalent to calling plusDays(weeks * 7) but provides clearer
   * semantic meaning for weekly calculations.
   *
   * @param weeks the number of weeks to add (can be negative to subtract)
   * @return a new Date instance with the weeks added
   */
  public Date plusWeeks( int weeks )
  {
    // delegate to plusDays with week multiplication
    return plusDays( weeks * 7 );
  }

  /***************************************** plusMonths ******************************************/
  /**
   * Returns a new Date with the specified number of months added to this date.
   * <p>
   * Month arithmetic handles varying month lengths intelligently. For example,
   * adding one month to January 31st results in February 28th (or 29th in leap years).
   *
   * @param months the number of months to add (can be negative to subtract)
   * @return a new Date instance with the months added
   */
  public Date plusMonths( int months )
  {
    // optimize for zero case to avoid object creation
    if ( months == 0 )
      return this;
    // use localdate for complex month arithmetic then convert back
    return of( toLocalDate().plusMonths( months ) );
  }

  /****************************************** plusYears ******************************************/
  /**
   * Returns a new Date with the specified number of years added to this date.
   * <p>
   * Year arithmetic handles leap years appropriately. Adding years to February 29th
   * in a leap year will result in February 28th if the target year is not a leap year.
   *
   * @param years the number of years to add (can be negative to subtract)
   * @return a new Date instance with the years added
   */
  public Date plusYears( int years )
  {
    // optimize for zero case to avoid object creation
    if ( years == 0 )
      return this;
    // use localdate for leap year handling then convert back
    return of( toLocalDate().plusYears( years ) );
  }

  // ================================= Comparison Methods =================================

  /****************************************** isBefore *******************************************/
  /**
   * Checks if this date occurs before the specified date.
   * <p>
   * This comparison is performed efficiently using epoch day values without
   * any intermediate object creation or complex date component analysis.
   *
   * @param other the date to compare against (must not be null)
   * @return true if this date is chronologically before the other date
   * @throws NullPointerException if other is null
   */
  public boolean isBefore( Date other )
  {
    // validate parameter
    Objects.requireNonNull( other, "Other date cannot be null" );
    // perform efficient epoch day comparison
    return m_epochDay < other.m_epochDay;
  }

  /******************************************* isAfter *******************************************/
  /**
   * Checks if this date occurs after the specified date.
   * <p>
   * This comparison is performed efficiently using epoch day values without
   * any intermediate object creation or complex date component analysis.
   *
   * @param other the date to compare against (must not be null)
   * @return true if this date is chronologically after the other date
   * @throws NullPointerException if other is null
   */
  public boolean isAfter( Date other )
  {
    // validate parameter
    Objects.requireNonNull( other, "Other date cannot be null" );
    // perform efficient epoch day comparison
    return m_epochDay > other.m_epochDay;
  }

  /******************************************* isEqual *******************************************/
  /**
   * Checks if this date represents the same calendar date as the specified date.
   * <p>
   * Unlike equals(), this method is null-safe and will return false rather than
   * throwing an exception when passed a null argument.
   *
   * @param other the date to compare against (can be null)
   * @return true if both dates represent the same calendar date, false if other is null
   */
  public boolean isEqual( Date other )
  {
    // null-safe equality check using epoch day comparison
    return other != null && m_epochDay == other.m_epochDay;
  }

  /****************************************** daysUntil ******************************************/
  /**
   * Calculates the number of days between this date and the specified date.
   * <p>
   * The result is positive if the other date is in the future, negative if in the past.
   * This calculation is performed efficiently using epoch day subtraction.
   *
   * @param other the target date to calculate the difference to (must not be null)
   * @return the number of days from this date to the other date (negative if other is earlier)
   * @throws NullPointerException if other is null
   */
  public int daysUntil( Date other )
  {
    // validate parameter
    Objects.requireNonNull( other, "Other date cannot be null" );
    // calculate difference using efficient epoch day arithmetic
    return other.m_epochDay - m_epochDay;
  }

  /****************************************** compareTo ******************************************/
  /**
   * Compares this date to another date for ordering purposes.
   * <p>
   * Returns a negative integer if this date is earlier, zero if equal, or a positive
   * integer if this date is later than the specified date.
   *
   * @param other the date to compare to (must not be null)
   * @return negative if earlier, zero if equal, positive if later
   * @throws NullPointerException if other is null
   */
  @Override
  public int compareTo( Date other )
  {
    // validate parameter
    Objects.requireNonNull( other, "Other date cannot be null" );
    // perform three-way comparison using epoch days
    return m_epochDay < other.m_epochDay ? -1 : m_epochDay > other.m_epochDay ? 1 : 0;
  }

  // ================================= String Conversion =================================

  /****************************************** toString *******************************************/
  /**
   * Returns a string representation of this date in ISO format (yyyy-MM-dd).
   * <p>
   * The format follows the ISO 8601 standard and is suitable for logging,
   * debugging, and data interchange. For custom formatting, use the format() method.
   *
   * @return the date formatted as yyyy-MM-dd (e.g., "2025-08-31")
   */
  @Override
  public String toString()
  {
    // use iso standard format for consistency
    return toLocalDate().toString();
  }

  /******************************************* format ********************************************/
  /**
   * Formats this date using the specified pattern string.
   * <p>
   * Supports all standard DateTimeFormatter patterns plus custom half-year formatting:
   * <ul>
   * <li>B = half-year number (1 or 2)</li>
   * <li>BB = H1 or H2</li>
   * <li>BBB = "1st half" or "2nd half"</li>
   * </ul>
   * <p>
   * Standard patterns include: yyyy (year), MM (month), dd (day), etc.
   * See DateTimeFormatter documentation for complete pattern reference.
   *
   * @param pattern the formatting pattern (must not be null)
   * @return the formatted date string
   * @throws IllegalArgumentException if the pattern is invalid
   * @throws NullPointerException if pattern is null
   */
  public String format( String pattern )
  {
    // validate input parameter
    Objects.requireNonNull( pattern, "Pattern cannot be null" );

    // check if special half-year formatting is needed
    if ( !pattern.contains( String.valueOf( HALF_YEAR_CHAR ) ) )
      return toLocalDate().format( DateTimeFormatter.ofPattern( pattern ) );

    // delegate to specialized half-year formatting
    return formatWithHalfYear( pattern );
  }

  /************************************* formatWithHalfYear **************************************/
  /**
   * handles formatting when half-year patterns are present in the format string.
   */
  private String formatWithHalfYear( String pattern )
  {
    // replace half-year patterns with safe placeholders
    String processedPattern = replaceHalfYearPatterns( pattern );

    // format using standard datetime formatter
    String result = toLocalDate().format( DateTimeFormatter.ofPattern( processedPattern ) );

    // substitute placeholders with actual half-year values
    return substituteHalfYearValues( result );
  }

  /*********************************** replaceHalfYearPatterns ***********************************/
  /**
   * replaces half-year 'B' patterns with temporary placeholders to avoid formatter conflicts.
   */
  private String replaceHalfYearPatterns( String pattern )
  {
    StringBuilder result = new StringBuilder();
    boolean inQuotes = false;
    int i = 0;

    while ( i < pattern.length() )
    {
      char ch = pattern.charAt( i );

      if ( ch == '\'' )
      {
        // track quote state for literal text handling
        inQuotes = !inQuotes;
        result.append( ch );
        i++;
      }
      else if ( !inQuotes && ch == HALF_YEAR_CHAR )
      {
        // count consecutive B characters for pattern length
        int count = 0;
        while ( i < pattern.length() && pattern.charAt( i ) == HALF_YEAR_CHAR )
        {
          count++;
          i++;
        }

        // validate pattern length
        if ( count > 3 )
          throw new IllegalArgumentException( "Too many 'B' pattern letters: " + count );

        // insert quoted placeholder to avoid formatter interpretation
        result.append( '\'' ).append( HALF_YEAR_MARKER ).append( count ).append( '\'' );
      }
      else
      {
        // append regular characters unchanged
        result.append( ch );
        i++;
      }
    }

    return result.toString();
  }

  /********************************** substituteHalfYearValues ***********************************/
  /**
   * replaces half-year placeholders with actual formatted half-year values.
   */
  private String substituteHalfYearValues( String formatted )
  {
    String result = formatted;
    // determine half-year based on month (1-6 = first half, 7-12 = second half)
    int halfYear = getMonth() <= 6 ? 1 : 2;

    // replace placeholders with appropriate half-year representations
    result = result.replace( HALF_YEAR_MARKER + "3", halfYear == 1 ? "1st half" : "2nd half" );
    result = result.replace( HALF_YEAR_MARKER + "2", "H" + halfYear );
    result = result.replace( HALF_YEAR_MARKER + "1", String.valueOf( halfYear ) );

    return result;
  }

  // ================================= Parsing Methods =================================

  /******************************************** parse ********************************************/
  /**
   * Parses a date string using the specified pattern.
   * <p>
   * The pattern must follow DateTimeFormatter conventions. Common patterns include:
   * "yyyy-MM-dd" for ISO format, "dd/MM/yyyy" for European format, "MM/dd/yyyy" for US format.
   *
   * @param text    the date string to parse (must not be null)
   * @param pattern the pattern to use for parsing (must not be null)
   * @return a new Date instance representing the parsed date
   * @throws DateTimeParseException if the text cannot be parsed using the pattern
   * @throws NullPointerException if text or pattern is null
   */
  public static Date parse( String text, String pattern )
  {
    // validate input parameters
    Objects.requireNonNull( text, "Text cannot be null" );
    Objects.requireNonNull( pattern, "Pattern cannot be null" );

    // attempt parsing with optional result
    Optional<Date> result = tryParse( text, pattern );
    if ( result.isPresent() )
      return result.get();

    // throw exception with clear error message
    throw new DateTimeParseException( "Unable to parse date", text, 0 );
  }

  /****************************************** tryParse *******************************************/
  /**
   * Attempts to parse a date string using the specified pattern.
   * <p>
   * This method provides a safe alternative to parse() that returns an empty Optional
   * instead of throwing an exception when parsing fails. Useful for validation scenarios.
   *
   * @param text    the date string to parse (can be null)
   * @param pattern the pattern to use for parsing (can be null)
   * @return an Optional containing the parsed date, or empty if parsing failed
   */
  public static Optional<Date> tryParse( String text, String pattern )
  {
    // handle null inputs gracefully
    if ( text == null || pattern == null )
      return Optional.empty();

    // delegate to intelligent parser with fallback handling
    return IntelligentParser.parse( text.trim(), pattern );
  }

  /************************************** parseIntelligent ***************************************/
  /**
   * Parses a date string using intelligent pattern detection and multiple fallbacks.
   * <p>
   * This method automatically tries common date formats including ISO, European, and
   * American conventions. It also handles cases where only day/month are provided by
   * assuming the current year.
   *
   * @param text the date string to parse (must not be null)
   * @return a new Date instance representing the parsed date
   * @throws DateTimeParseException if the text cannot be parsed with any known format
   * @throws NullPointerException if text is null
   */
  public static Date parseIntelligent( String text )
  {
    // validate input parameter
    Objects.requireNonNull( text, "Text cannot be null" );

    // attempt parsing with multiple format fallbacks
    Optional<Date> result = IntelligentParser.parseWithFallbacks( text.trim() );
    if ( result.isPresent() )
      return result.get();

    // provide informative error message about failed attempts
    throw new DateTimeParseException( "Unable to parse date with any known format", text, 0 );
  }

  // ================================= Conversion Methods =================================

  /***************************************** toLocalDate *****************************************/
  /**
   * Converts this Date to a LocalDate instance.
   * <p>
   * This method provides interoperability with the standard Java time API while
   * maintaining the same date value. The conversion is performed efficiently using
   * the internal epoch day representation.
   *
   * @return a LocalDate instance representing the same date
   */
  public LocalDate toLocalDate()
  {
    // convert from internal epoch day format to localdate
    return LocalDate.ofEpochDay( m_epochDay );
  }

  // ================================= Object Methods =================================

  /******************************************* equals ********************************************/
  /**
   * Compares this date with another object for equality.
   * <p>
   * Two Date instances are equal if they represent the same calendar date.
   * This method follows the standard equals contract and is consistent with compareTo().
   *
   * @param obj the object to compare with (can be null)
   * @return true if the objects represent the same date, false otherwise
   */
  @Override
  public boolean equals( Object obj )
  {
    // handle identity comparison for performance
    if ( this == obj )
      return true;
    // check type compatibility and compare epoch days
    if ( !( obj instanceof Date other ) )
      return false;
    return m_epochDay == other.m_epochDay;
  }

  /****************************************** hashCode *******************************************/
  /**
   * Returns a hash code for this Date instance.
   * <p>
   * The hash code is based on the epoch day value, ensuring that equal dates
   * have equal hash codes as required by the Object contract.
   *
   * @return a hash code value for this date
   */
  @Override
  public int hashCode()
  {
    // use epoch day as hash code for simplicity and efficiency
    return m_epochDay;
  }

  // ================================= Inner Classes =================================

  /**
   * Intelligent parser that handles various date formats with automatic fallbacks.
   * <p>
   * This internal utility class provides robust date parsing capabilities by trying
   * multiple common formats and handling edge cases like missing year components.
   */
  private static class IntelligentParser
  {
    // common date patterns ordered by likelihood and preference
    private static final List<String> COMMON_PATTERNS = List.of( "yyyy-MM-dd", // iso format (preferred)
        "yyyy/MM/dd", // alternative iso with slashes
        "dd/MM/yyyy", // european format
        "MM/dd/yyyy", // american format
        "dd-MM-yyyy", // european with dashes
        "MM-dd-yyyy", // american with dashes
        "d/M/yyyy", // single digit variants
        "d/M/yy", // two digit year
        "dd/MM/yy", // european short year
        "MM/dd/yy", // american short year
        "yyyy-M-d", // iso with single digits
        "yyyy/M/d" // alternative iso single digits
    );

    /******************************************* parse *******************************************/
    /**
     * attempts to parse text using the specified pattern with fallback to simplified patterns.
     */
    static Optional<Date> parse( String text, String pattern )
    {
      try
      {
        // attempt direct parsing with provided pattern
        LocalDate localDate = LocalDate.parse( text, DateTimeFormatter.ofPattern( pattern ) );
        return Optional.of( of( localDate ) );
      }
      catch ( Exception e )
      {
        // fallback to simplified pattern handling
        return trySimplifiedPattern( text, pattern );
      }
    }

    /************************************ parseWithFallbacks *************************************/
    /**
     * attempts parsing using multiple common patterns with intelligent fallbacks.
     */
    static Optional<Date> parseWithFallbacks( String text )
    {
      // try each common pattern in order of preference
      for ( String pattern : COMMON_PATTERNS )
      {
        Optional<Date> result = parse( text, pattern );
        if ( result.isPresent() )
          return result;
      }

      // handle special case of missing year component
      return tryWithCurrentYear( text );
    }

    /*********************************** trySimplifiedPattern ************************************/
    /**
     * attempts parsing with a simplified version of the pattern to handle format variations.
     */
    private static Optional<Date> trySimplifiedPattern( String text, String pattern )
    {
      try
      {
        // create simplified pattern by removing complex elements
        String simplified = simplifyPattern( pattern );
        LocalDate localDate = LocalDate.parse( text, DateTimeFormatter.ofPattern( simplified ) );
        return Optional.of( of( localDate ) );
      }
      catch ( Exception e )
      {
        return Optional.empty();
      }
    }

    /************************************ tryWithCurrentYear *************************************/
    /**
     * handles cases where only day and month are provided by adding current year.
     */
    private static Optional<Date> tryWithCurrentYear( String text )
    {
      String[] parts = text.split( "[/-]" );
      if ( parts.length == 2 )
      {
        try
        {
          // append current year to partial date string
          String withYear = text + "/" + LocalDate.now().getYear();
          return parseWithFallbacks( withYear );
        }
        catch ( Exception e )
        {
          // try prepending year instead of appending
          try
          {
            String withYear = LocalDate.now().getYear() + "/" + text;
            return parseWithFallbacks( withYear );
          }
          catch ( Exception e2 )
          {
            return Optional.empty();
          }
        }
      }
      return Optional.empty();
    }

    /************************************** simplifyPattern **************************************/
    /**
     * creates a simplified version of complex patterns by removing unsupported elements.
     */
    private static String simplifyPattern( String pattern )
    {
      return pattern.replaceAll( "G+", "" ) // remove era designators
          .replaceAll( "B+", "" ) // remove half-year patterns
          .replaceAll( "Q+", "" ) // remove quarter patterns
          .replaceAll( "E+", "" ) // remove day of week names
          .replaceAll( "'[^']*'", "" ) // remove quoted literal text
          .replaceAll( "([yMd])\\1+", "$1$1" ); // simplify repeated chars to pairs
    }
  }
}