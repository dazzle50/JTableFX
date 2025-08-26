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
 * Immutable date class representing a single calendar date.
 * <p>
 * This class stores dates internally as epoch days (days since 1970-01-01) for efficient storage and computation.
 * <p>
 * Key features:
 * <ul>
 * <li>Immutable and thread-safe</li>
 * <li>Memory efficient (4 bytes per instance)</li>
 * <li>Wide date range support (±5.8 million years)</li>
 * <li>Custom half-year formatting support</li>
 * <li>Flexible parsing with intelligent fallbacks</li>
 * </ul>
 */
public final class Date implements Serializable, Comparable<Date>
{
  private static final long             serialVersionUID = 1L;

  // simple count of days where day 0 is 01-Jan-1970
  private final int                     m_epochDay;

  // date constants (approximate ±5.8 million years)
  public static final Date              MIN_VALUE        = new Date( Integer.MIN_VALUE );
  public static final Date              MAX_VALUE        = new Date( Integer.MAX_VALUE );
  public static final Date              EPOCH            = new Date( 0 );                   // 1970-01-01

  // parsing patterns
  public static final DateTimeFormatter ISO_FORMAT       = DateTimeFormatter.ISO_LOCAL_DATE;

  // half-year formatting constants
  private static final String           HALF_YEAR_MARKER = "#HY#";
  private static final char             HALF_YEAR_CHAR   = 'B';

  // ================================= Constructors =================================

  /**
   * Private constructor - use factory methods instead.
   */
  private Date( int epochDay )
  {
    m_epochDay = epochDay;
  }

  // ================================= Factory Methods =================================

  /**
   * Creates a Date from epoch-day.
   *
   * @param epochDay days since 1970-01-01
   * @return new Date instance
   */
  public static Date ofEpochDay( int epochDay )
  {
    return new Date( epochDay );
  }

  /**
   * Creates a Date from year, month, and day.
   *
   * @param year  the year (e.g., 2023)
   * @param month the month (1-12)
   * @param day   the day of month (1-31)
   * @return new Date
   * @throws DateTimeException if the date is invalid
   */
  public static Date of( int year, int month, int day )
  {
    try
    {
      int epochDay = (int) LocalDate.of( year, month, day ).toEpochDay();
      return ofEpochDay( epochDay );
    }
    catch ( DateTimeException exception )
    {
      throw new DateTimeException( String.format( "Invalid date: %04d-%02d-%02d", year, month, day ), exception );
    }
  }

  /**
   * Creates a Date from LocalDate.
   *
   * @param localDate the LocalDate to convert
   * @return new Date
   * @throws NullPointerException if localDate is null
   */
  public static Date of( LocalDate localDate )
  {
    Objects.requireNonNull( localDate, "LocalDate cannot be null" );
    return ofEpochDay( (int) localDate.toEpochDay() );
  }

  /**
   * Creates a Date representing today.
   *
   * @return Date for current date
   */
  public static Date now()
  {
    return of( LocalDate.now() );
  }

  // ================================ Accessor Methods ================================

  /**
   * Gets the epoch day (days since 1970-01-01).
   *
   * @return the epoch day
   */
  public int getEpochDay()
  {
    return m_epochDay;
  }

  /**
   * Gets the year.
   *
   * @return the year
   */
  public int getYear()
  {
    return toLocalDate().getYear();
  }

  /**
   * Gets the month as a number (1-12).
   *
   * @return the month (1=January, 12=December)
   */
  public int getMonth()
  {
    return toLocalDate().getMonthValue();
  }

  /**
   * Gets the month as an enum.
   *
   * @return the Month enum
   */
  public Month getMonthEnum()
  {
    return toLocalDate().getMonth();
  }

  /**
   * Gets the day of the month (1-31).
   *
   * @return the day of the month
   */
  public int getDayOfMonth()
  {
    return toLocalDate().getDayOfMonth();
  }

  /**
   * Gets the day of the week.
   *
   * @return the DayOfWeek enum
   */
  public DayOfWeek getDayOfWeek()
  {
    return toLocalDate().getDayOfWeek();
  }

  /**
   * Gets the day of the year (1-366).
   *
   * @return the day of the year
   */
  public int getDayOfYear()
  {
    return toLocalDate().getDayOfYear();
  }

  /**
   * Checks if this is a leap year.
   *
   * @return true if leap year
   */
  public boolean isLeapYear()
  {
    return toLocalDate().isLeapYear();
  }

  // ================================= Date Arithmetic =================================

  /**
   * Returns a new Date with the specified number of days added.
   *
   * @param days the days to add (can be negative)
   * @return new Date
   * @throws ArithmeticException if the result overflows
   */
  public Date plusDays( int days )
  {
    if ( days == 0 )
      return this;
    return ofEpochDay( m_epochDay + days );
  }

  /**
   * Returns a new Date with the specified number of weeks added.
   *
   * @param weeks the weeks to add (can be negative)
   * @return new Date
   */
  public Date plusWeeks( int weeks )
  {
    return plusDays( weeks * 7 );
  }

  /**
   * Returns a new Date with the specified number of months added.
   *
   * @param months the months to add (can be negative)
   * @return new Date
   */
  public Date plusMonths( int months )
  {
    if ( months == 0 )
      return this;
    return of( toLocalDate().plusMonths( months ) );
  }

  /**
   * Returns a new Date with the specified number of years added.
   *
   * @param years the years to add (can be negative)
   * @return new Date
   */
  public Date plusYears( int years )
  {
    if ( years == 0 )
      return this;
    return of( toLocalDate().plusYears( years ) );
  }

  // ================================= Comparison Methods =================================

  /**
   * Checks if this date is before the specified date.
   *
   * @param other the other date
   * @return true if this date is before the other
   */
  public boolean isBefore( Date other )
  {
    Objects.requireNonNull( other, "Other date cannot be null" );
    return m_epochDay < other.m_epochDay;
  }

  /**
   * Checks if this date is after the specified date.
   *
   * @param other the other date
   * @return true if this date is after the other
   */
  public boolean isAfter( Date other )
  {
    Objects.requireNonNull( other, "Other date cannot be null" );
    return m_epochDay > other.m_epochDay;
  }

  /**
   * Checks if this date is equal to the specified date.
   *
   * @param other the other date
   * @return true if the dates are equal
   */
  public boolean isEqual( Date other )
  {
    return other != null && m_epochDay == other.m_epochDay;
  }

  /**
   * Calculates the number of days between this date and another date.
   *
   * @param other the other date
   * @return the number of days between the dates (can be negative)
   */
  public int daysUntil( Date other )
  {
    Objects.requireNonNull( other, "Other date cannot be null" );
    return other.m_epochDay - m_epochDay;
  }

  @Override
  public int compareTo( Date other )
  {
    Objects.requireNonNull( other, "Other date cannot be null" );
    return Integer.compare( m_epochDay, other.m_epochDay );
  }

  // ================================= String Conversion =================================

  @Override
  public String toString()
  {
    return toLocalDate().toString(); // ISO format (yyyy-MM-dd)
  }

  /**
   * Formats this date using the specified pattern.
   * Supports special half-year formatting using 'B' characters:
   * <ul>
   * <li>B = half-year number (1 or 2)</li>
   * <li>BB = H1 or H2</li>
   * <li>BBB = "1st half" or "2nd half"</li>
   * </ul>
   *
   * @param pattern the formatting pattern
   * @return formatted date string
   * @throws IllegalArgumentException if pattern is invalid
   */
  public String format( String pattern )
  {
    Objects.requireNonNull( pattern, "Pattern cannot be null" );
    if ( !pattern.contains( String.valueOf( HALF_YEAR_CHAR ) ) )
      return toLocalDate().format( DateTimeFormatter.ofPattern( pattern ) );

    return formatWithHalfYear( pattern );
  }

  /**
   * Formats date with half-year support.
   */
  private String formatWithHalfYear( String pattern )
  {
    // replace B patterns with placeholder to avoid DateTimeFormatter issues
    String processedPattern = replaceHalfYearPatterns( pattern );

    // format with standard patterns
    String result = toLocalDate().format( DateTimeFormatter.ofPattern( processedPattern ) );

    // replace placeholders with half-year values
    return substituteHalfYearValues( result );
  }

  /**
   * Replaces half-year patterns with safe placeholders.
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
        inQuotes = !inQuotes;
        result.append( ch );
        i++;
      }
      else if ( !inQuotes && ch == HALF_YEAR_CHAR )
      {
        // count consecutive B's
        int count = 0;
        while ( i < pattern.length() && pattern.charAt( i ) == HALF_YEAR_CHAR )
        {
          count++;
          i++;
        }

        if ( count > 3 )
          throw new IllegalArgumentException( "Too many 'B' pattern letters: " + count );

        // replace with quoted placeholder
        result.append( '\'' ).append( HALF_YEAR_MARKER ).append( count ).append( '\'' );
      }
      else
      {
        result.append( ch );
        i++;
      }
    }

    return result.toString();
  }

  /**
   * Substitutes half-year placeholders with actual values.
   */
  private String substituteHalfYearValues( String formatted )
  {
    String result = formatted;
    int halfYear = getMonth() <= 6 ? 1 : 2;

    // replace placeholders
    result = result.replace( HALF_YEAR_MARKER + "3", halfYear == 1 ? "1st half" : "2nd half" );
    result = result.replace( HALF_YEAR_MARKER + "2", "H" + halfYear );
    result = result.replace( HALF_YEAR_MARKER + "1", String.valueOf( halfYear ) );

    return result;
  }

  // ================================= Parsing Methods =================================

  /**
   * Parses a date string using the specified pattern.
   *
   * @param text    the text to parse
   * @param pattern the pattern to use
   * @return parsed Date
   * @throws DateTimeParseException if parsing fails
   */
  public static Date parse( String text, String pattern )
  {
    Objects.requireNonNull( text, "Text cannot be null" );
    Objects.requireNonNull( pattern, "Pattern cannot be null" );

    Optional<Date> result = tryParse( text, pattern );
    if ( result.isPresent() )
      return result.get();

    throw new DateTimeParseException( "Unable to parse date", text, 0 );
  }

  /**
   * Attempts to parse a date string, returning empty Optional if parsing fails.
   *
   * @param text    the text to parse
   * @param pattern the pattern to use
   * @return Optional containing parsed date, or empty if parsing failed
   */
  public static Optional<Date> tryParse( String text, String pattern )
  {
    if ( text == null || pattern == null )
      return Optional.empty();

    return IntelligentParser.parse( text.trim(), pattern );
  }

  /**
   * Parses a date string using intelligent pattern matching.
   * Tries multiple common formats automatically.
   *
   * @param text the text to parse
   * @return parsed Date
   * @throws DateTimeParseException if parsing fails with all attempted patterns
   */
  public static Date parseIntelligent( String text )
  {
    Objects.requireNonNull( text, "Text cannot be null" );

    Optional<Date> result = IntelligentParser.parseWithFallbacks( text.trim() );
    if ( result.isPresent() )
      return result.get();

    throw new DateTimeParseException( "Unable to parse date with any known format", text, 0 );
  }

  // ================================= Conversion Methods =================================

  /**
   * Converts to LocalDate.
   *
   * @return equivalent LocalDate
   */
  public LocalDate toLocalDate()
  {
    return LocalDate.ofEpochDay( m_epochDay );
  }

  // ================================= Object Methods =================================

  @Override
  public boolean equals( Object obj )
  {
    if ( this == obj )
      return true;
    if ( !( obj instanceof Date other ) )
      return false;
    return m_epochDay == other.m_epochDay;
  }

  @Override
  public int hashCode()
  {
    return m_epochDay;
  }

  // ================================= Inner Classes =================================

  /**
   * Intelligent parser that handles various date formats with fallbacks.
   */
  private static class IntelligentParser
  {

    private static final List<String> COMMON_PATTERNS = List.of( "yyyy-MM-dd", // ISO format
        "yyyy/MM/dd", // alternative ISO
        "dd/MM/yyyy", // European
        "MM/dd/yyyy", // American
        "dd-MM-yyyy", // European with dashes
        "MM-dd-yyyy", // American with dashes
        "d/M/yyyy", // single digit variants
        "d/M/yy", // two digit year
        "dd/MM/yy", // European short
        "MM/dd/yy", // American short
        "yyyy-M-d", // ISO with single digits
        "yyyy/M/d" // alternative ISO with single digits
    );

    static Optional<Date> parse( String text, String pattern )
    {
      try
      {
        LocalDate localDate = LocalDate.parse( text, DateTimeFormatter.ofPattern( pattern ) );
        return Optional.of( of( localDate ) );
      }
      catch ( Exception e )
      {
        // try simplified pattern
        return trySimplifiedPattern( text, pattern );
      }
    }

    static Optional<Date> parseWithFallbacks( String text )
    {
      // try common patterns first
      for ( String pattern : COMMON_PATTERNS )
      {
        Optional<Date> result = parse( text, pattern );
        if ( result.isPresent() )
          return result;
      }

      // Try with current year if only day/month provided
      return tryWithCurrentYear( text );
    }

    private static Optional<Date> trySimplifiedPattern( String text, String pattern )
    {
      try
      {
        // Simplify pattern by removing repeated characters and unsupported symbols
        String simplified = simplifyPattern( pattern );
        LocalDate localDate = LocalDate.parse( text, DateTimeFormatter.ofPattern( simplified ) );
        return Optional.of( of( localDate ) );
      }
      catch ( Exception e )
      {
        return Optional.empty();
      }
    }

    private static Optional<Date> tryWithCurrentYear( String text )
    {
      String[] parts = text.split( "[/-]" );
      if ( parts.length == 2 )
      {
        try
        {
          String withYear = text + "/" + LocalDate.now().getYear();
          return parseWithFallbacks( withYear );
        }
        catch ( Exception e )
        {
          // Try different order
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

    private static String simplifyPattern( String pattern )
    {
      return pattern.replaceAll( "G+", "" ) // Remove era
          .replaceAll( "B+", "" ) // Remove half-year
          .replaceAll( "Q+", "" ) // Remove quarter
          .replaceAll( "E+", "" ) // Remove day of week
          .replaceAll( "'[^']*'", "" ) // Remove quoted text
          .replaceAll( "([yMd])\\1+", "$1$1" ); // Simplify repeated chars to pairs
    }
  }
}