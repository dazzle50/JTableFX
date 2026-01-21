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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*************************************************************************************************/
/****************************** DateParser (Intelligent Parsing) *********************************/
/*************************************************************************************************/

/**
 * Utility class providing intelligent date parsing capabilities.
 * <p>
 * This class handles the complex logic required to convert various string representations
 * into {@link Date} objects. It supports:
 * <ul>
 * <li>Standard ISO format (e.g., "2024-12-25")</li>
 * <li>Flexible numeric formats with variable digits (e.g., "1/1/2024", "25/12/2024")</li>
 * <li>Two-digit year formats (e.g., "25/12/24")</li>
 * <li>Compact format without separators (e.g., "25122024")</li>
 * <li>Relative natural language (e.g., "today", "tomorrow", "yesterday")</li>
 * <li>Day of week references (e.g., "next friday", "last monday", "this tuesday")</li>
 * <li>Arithmetic offsets (e.g., "+7 days", "-1 month")</li>
 * <li>Business terms (e.g., "start of month", "end of year")</li>
 * <li>Month names with day and optional year (e.g., "25 Dec", "December 25 2024", "Dec 25 2024")</li>
 * <li>Tolerant of extra spaces and mixed separators (e.g., "25 - 12 - 2024")</li>
 * </ul>
 * <p>
 * All parsing uses the system's default locale for month names and day names.
 * <p>
 * The parser normalises input by collapsing whitespace and converting various separators
 * (hyphens, dots, colons, etc.) to forward slashes for consistent processing.
 *
 * @see Date
 */
public final class DateParser
{
  private static final Pattern                 OFFSET_PATTERN   = Pattern.compile( "^([+-])\\s*(\\d+)\\s*([a-z]+)$" );
  private static final List<DateTimeFormatter> m_customPatterns = new ArrayList<>();
  private static final DateTimeFormatter       STD_FORMATTER;
  private static final DateTimeFormatter       PARTIAL_FORMATTER;

  static
  {
    Locale loc = Locale.getDefault();

    // composite formatter supporting iso, compact (ddMMyyyy), european (d/M/y), us month names (MMMM/d/y and MMM/d/y), and 2-digit years
    STD_FORMATTER = new DateTimeFormatterBuilder().parseCaseInsensitive()
        .appendOptional( DateTimeFormatter.ofPattern( "uuuu/M/d", loc ) ) // ISO format with slashes
        .appendOptional( DateTimeFormatter.ofPattern( "ddMM[uuuuu][uuuu][uuu][uu][u]", loc ) )
        .appendOptional( DateTimeFormatter.ofPattern( "d/M/[uuuuu][uuuu][uuu][uu][u]", loc ) )
        .appendOptional( DateTimeFormatter.ofPattern( "d/MMMM/[uuuuu][uuuu][uuu][uu][u]", loc ) )
        .appendOptional( DateTimeFormatter.ofPattern( "MMMM/d/[uuuuu][uuuu][uuu][uu][u]", loc ) )
        .appendOptional( DateTimeFormatter.ofPattern( "d/MMM/[uuuuu][uuuu][uuu][uu][u]", loc ) )
        .appendOptional( DateTimeFormatter.ofPattern( "MMM/d/[uuuuu][uuuu][uuu][uu][u]", loc ) ).toFormatter( loc );

    // partial formatter for day/month only inputs (ddMM, d/M, d/MMMM, d/MMM, etc.), defaults year to 0 for later replacement with current year
    PARTIAL_FORMATTER = new DateTimeFormatterBuilder().parseCaseInsensitive()
        .appendOptional( DateTimeFormatter.ofPattern( "ddMM", loc ) )
        .appendOptional( DateTimeFormatter.ofPattern( "d/MMMM", loc ) )
        .appendOptional( DateTimeFormatter.ofPattern( "MMMM/d", loc ) )
        .appendOptional( DateTimeFormatter.ofPattern( "d/MMM", loc ) )
        .appendOptional( DateTimeFormatter.ofPattern( "MMM/d", loc ) )
        .appendOptional( DateTimeFormatter.ofPattern( "d/M", loc ) ).parseDefaulting( ChronoField.YEAR, 0 )
        .toFormatter( loc );
  }

  /************************************** constructor **************************************/
  /**
   * Private constructor to prevent instantiation of utility class.
   */
  private DateParser()
  {
    throw new UnsupportedOperationException( "Utility class" );
  }

  /***************************************** parse *****************************************/
  /**
   * Parses a text string using a specific pattern.
   * <p>
   * This method uses the standard Java date-time pattern syntax. For example:
   * <ul>
   * <li>"yyyy-MM-dd" for ISO format</li>
   * <li>"dd/MM/yyyy" for European format</li>
   * <li>"MM/dd/yyyy" for US format</li>
   * <li>"d MMM yyyy" for abbreviated month names</li>
   * </ul>
   *
   * @param text    the text to parse, must not be null
   * @param pattern the pattern to use, must not be null
   * @return the parsed Date
   * @throws NullPointerException   if text or pattern is null
   * @throws DateTimeParseException if parsing fails
   */
  public static Date parse( String text, String pattern )
  {
    Objects.requireNonNull( text, "Text cannot be null" );
    Objects.requireNonNull( pattern, "Pattern cannot be null" );

    try
    {
      // parse text using the specified pattern and convert to date
      return Date.of( LocalDate.parse( text, DateTimeFormatter.ofPattern( pattern ) ) );
    }
    catch ( IllegalArgumentException | DateTimeParseException e )
    {
      throw new DateTimeParseException( "Failed to parse '" + text + "' with '" + pattern + "'", text, 0, e );
    }
  }

  /***************************************** parse *****************************************/
  /**
   * Intelligently parses text attempting to interpret it through multiple strategies.
   * <p>
   * The parser tries the following approaches in order:
   * <ol>
   * <li>Natural language expressions (today, tomorrow, next friday, +7 days, etc.)</li>
   * <li>Custom user-registered patterns</li>
   * <li>Standard date formats (ISO, compact, European, US with month names, two-digit years)</li>
   * <li>Partial dates with current year (25/12, Dec 25)</li>
   * </ol>
   * <p>
   * The parser is tolerant of formatting variations:
   * <ul>
   * <li>Extra whitespace is collapsed</li>
   * <li>Spaces around separators are removed</li>
   * <li>Various separators (-, ., :, /) are treated equivalently</li>
   * <li>Single or double digit days and months are accepted</li>
   * </ul>
   * <p>
   * Examples of valid input:
   * <pre>
   * "2024-12-25"           → 25 December 2024
   * "25/12/2024"           → 25 December 2024
   * "25 - 12 - 2024"       → 25 December 2024
   * "1/1/24"               → 1 January 2024
   * "25122024"             → 25 December 2024
   * "25 Dec 2024"          → 25 December 2024
   * "December 25 2024"     → 25 December 2024
   * "25/12"                → 25 December (current year)
   * "Dec 25"               → 25 December (current year)
   * "today"                → current date
   * "tomorrow"             → current date + 1 day
   * "next friday"          → next occurrence of Friday
   * "+7 days"              → current date + 7 days
   * "start of month"       → first day of current month
   * </pre>
   *
   * @param text the text to parse, must not be null
   * @return the parsed Date
   * @throws NullPointerException   if text is null
   * @throws DateTimeParseException if all parsing strategies fail
   */
  public static Date parse( String text )
  {
    Objects.requireNonNull( text, "Text cannot be null" );

    String input = text.trim().replaceAll( "\\s+", " " );

    if ( input.isEmpty() )
      throw new DateTimeParseException( "Cannot parse empty string", text, 0 );

    try
    {
      // try natural language parsing (relative dates, offsets, business terms)
      try
      {
        if ( isNaturalLanguage( input ) )
          return parseNaturalLanguage( input.toLowerCase( Locale.ROOT ) );
      }
      catch ( DateTimeParseException ignored )
      {
      }

      String normalised = input.replaceAll( "[\\s\\-.:/\\\\_,|=]+", "/" ).replaceAll( "/$", "" );

      // try custom user-registered patterns using both normalised and original text for maximum flexibility
      for ( DateTimeFormatter formatter : m_customPatterns )
      {
        try
        {
          return Date.of( LocalDate.parse( normalised, formatter ) );
        }
        catch ( DateTimeParseException ignored )
        {
        }

        try
        {
          return Date.of( LocalDate.parse( text, formatter ) );
        }
        catch ( DateTimeParseException ignored )
        {
        }
      }

      // try standard format patterns using normalised input
      try
      {
        return Date.of( LocalDate.parse( normalised, STD_FORMATTER ) );
      }
      catch ( DateTimeParseException ignored )
      {
      }

      // try partial patterns (day/month only) and apply current year
      LocalDate partial = LocalDate.parse( normalised, PARTIAL_FORMATTER );
      return Date.of( partial.withYear( LocalDate.now().getYear() ) );
    }
    catch ( DateTimeParseException e )
    {
      // legitimate parsing failure - preserve original exception
      throw e;
    }
    catch ( Exception e )
    {
      // unexpected exception indicates parser bug - wrap with context
      throw new DateTimeParseException( "Parser internal error for: " + text, text, 0, e );
    }
  }

  /******************************** registerCustomPattern **********************************/
  /**
   * Registers a custom pattern to be tried during intelligent parsing.
   * <p>
   * Custom patterns are attempted after natural language parsing but before
   * standard format parsing. This allows applications to support domain-specific
   * date formats whilst maintaining the intelligent parsing behaviour.
   * <p>
   * Multiple custom patterns can be registered and will be tried in registration order.
   *
   * @param pattern the pattern string to register using Java date-time pattern syntax
   * @throws IllegalArgumentException if the pattern is invalid
   */
  public static void registerCustomPattern( String pattern )
  {
    // add the custom pattern formatter to the list for use during parsing
    m_customPatterns.add( DateTimeFormatter.ofPattern( pattern ) );
  }

  /********************************** isNaturalLanguage ************************************/
  // checks if input appears to be natural language rather than a numeric date format
  private static boolean isNaturalLanguage( String input )
  {
    // natural language inputs start with a letter, plus sign, or minus sign
    char first = input.charAt( 0 );
    return Character.isLetter( first ) || first == '+' || first == '-';
  }

  /******************************** parseNaturalLanguage ***********************************/
  // parses natural language date expressions including relative dates, offsets, and business terms
  private static Date parseNaturalLanguage( String input )
  {
    LocalDate today = LocalDate.now();

    // handle simple keyword expressions
    switch ( input )
    {
      case "now", "today" -> {
        return Date.of( today );
      }
      case "tomorrow" -> {
        return Date.of( today.plusDays( 1 ) );
      }
      case "yesterday" -> {
        return Date.of( today.minusDays( 1 ) );
      }
      case "start of month", "first of month" -> {
        return Date.of( today.withDayOfMonth( 1 ) );
      }
      case "end of month", "last of month" -> {
        return Date.of( today.with( TemporalAdjusters.lastDayOfMonth() ) );
      }
      case "start of year", "first of year" -> {
        return Date.of( today.withDayOfYear( 1 ) );
      }
      case "end of year", "last of year" -> {
        return Date.of( today.with( TemporalAdjusters.lastDayOfYear() ) );
      }
    }

    // handle day of week expressions (next/last/this followed by day name)
    if ( input.startsWith( "next " ) || input.startsWith( "last " ) || input.startsWith( "this " ) )
    {
      String[] parts = input.split( "\\s+" );
      if ( parts.length < 2 )
        throw new DateTimeParseException( "Invalid day expression", input, 0 );

      DayOfWeek dow = parseDayOfWeek( parts[1] );
      if ( parts[0].equals( "next" ) )
        return Date.of( today.with( TemporalAdjusters.next( dow ) ) );
      if ( parts[0].equals( "last" ) )
        return Date.of( today.with( TemporalAdjusters.previous( dow ) ) );
      return Date.of( today.with( TemporalAdjusters.nextOrSame( dow ) ) );
    }

    // handle offset expressions (+7 days, -1 month, etc.)
    Matcher m = OFFSET_PATTERN.matcher( input );
    if ( m.matches() )
    {
      int amount = Integer.parseInt( m.group( 2 ) ) * ( m.group( 1 ).equals( "-" ) ? -1 : 1 );
      return switch ( m.group( 3 ) )
      {
        case "d", "day", "days" -> Date.of( today.plusDays( amount ) );
        case "w", "week", "weeks" -> Date.of( today.plusWeeks( amount ) );
        case "m", "month", "months" -> Date.of( today.plusMonths( amount ) );
        case "y", "year", "years" -> Date.of( today.plusYears( amount ) );
        default -> throw new DateTimeParseException( "Unknown unit", input, 0 );
      };
    }

    throw new DateTimeParseException( "Unknown expression: " + input, input, 0 );
  }

  /************************************ parseDayOfWeek *************************************/
  // parses day of week name using both full and abbreviated forms in the default locale
  private static DayOfWeek parseDayOfWeek( String text )
  {
    Locale loc = Locale.getDefault();
    for ( DayOfWeek d : DayOfWeek.values() )
    {
      if ( d.getDisplayName( java.time.format.TextStyle.FULL, loc ).equalsIgnoreCase( text )
          || d.getDisplayName( java.time.format.TextStyle.SHORT, loc ).equalsIgnoreCase( text ) )
        return d;
    }
    throw new DateTimeParseException( "Invalid day: " + text, text, 0 );
  }
}