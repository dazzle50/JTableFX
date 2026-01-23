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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*************************************************************************************************/
/****************************** TimeParser (Intelligent Parsing) *********************************/
/*************************************************************************************************/

/**
 * Utility class providing intelligent time parsing capabilities.
 * <p>
 * This class handles the logic required to convert various string representations
 * into {@link Time} objects. It supports:
 * <ul>
 * <li>Standard ISO format (e.g., "14:30:00")</li>
 * <li>12-hour AM/PM format (e.g., "9:20pm", "10.30 am")</li>
 * <li>Letter-based formats (e.g., "14h25", "14h25m30s")</li>
 * <li>Flexible separators (e.g., "14.30", "14 30")</li>
 * <li>Compact numeric formats (e.g., "1430", "083045")</li>
 * <li>Natural language (e.g., "now", "noon", "midnight")</li>
 * <li>Arithmetic offsets (e.g., "+1 hour", "-30 mins")</li>
 * <li>End of day representation ("24:00", "24:00:00")</li>
 * </ul>
 * <p>
 * The parser is tolerant of whitespace and formatting variations, normalising input
 * before processing.
 *
 * @see Time
 */
public final class TimeParser
{
  private static final Pattern                 OFFSET_PATTERN     = Pattern.compile( "^([+-])\\s*(\\d+)\\s*([a-z]+)$" );
  private static final Pattern                 NUMERIC_PATTERN    = Pattern.compile( "^\\d+$" );
  private static final Pattern                 END_OF_DAY_PATTERN = Pattern
      .compile( "^24[:\\s.h]?0*[:\\s.m]?0*[:\\s.s]?0*$" );
  private static final List<DateTimeFormatter> m_customPatterns   = new ArrayList<>();
  private static final DateTimeFormatter       STD_FORMATTER;

  static
  {
    // composite formatter supporting various time structures
    // checks for 12-hour format with AM/PM first, then 24-hour flexible format
    STD_FORMATTER = new DateTimeFormatterBuilder().parseCaseInsensitive()
        // 12-hour format: h:m:s with optional AM/PM marker (e.g. 9:20pm, 9.20 am)
        .appendOptional( DateTimeFormatter.ofPattern( "h[[:][.][ ]m[[:][.][ ]s]][ ]a" ) )
        // 24-hour format: H:m:s with flexible separators including letters (e.g. 14:30, 14h30m)
        .appendOptional(
            DateTimeFormatter.ofPattern( "H[:][.][ ]['h']m['m'][[:][.][ ]['m']s['s'][[:][.][ ]['s'][SSS][SS][S]]]" ) )
        .toFormatter( Locale.getDefault() );
  }

  /************************************** constructor **************************************/
  /**
   * Private constructor to prevent instantiation of utility class.
   */
  private TimeParser()
  {
    throw new UnsupportedOperationException( "Utility class" );
  }

  /***************************************** parse *****************************************/
  /**
   * Parses a text string using a specific pattern.
   * <p>
   * This method uses standard Java date-time pattern syntax.
   *
   * @param text    the text to parse, must not be null
   * @param pattern the pattern to use, must not be null
   * @return the parsed Time
   * @throws NullPointerException   if text or pattern is null
   * @throws DateTimeParseException if parsing fails
   */
  public static Time parse( String text, String pattern )
  {
    Objects.requireNonNull( text, "Text cannot be null" );
    Objects.requireNonNull( pattern, "Pattern cannot be null" );

    try
    {
      // check for end of day special case first as LocalTime cannot parse "24:00"
      if ( END_OF_DAY_PATTERN.matcher( text.trim() ).matches() )
        return Time.MAX_VALUE;

      return Time.of( LocalTime.parse( text, DateTimeFormatter.ofPattern( pattern ) ) );
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
   * <li>Natural language expressions (now, noon, +1 hour)</li>
   * <li>Special "End of Day" formats (24:00)</li>
   * <li>Custom user-registered patterns</li>
   * <li>Numeric compact formats (830, 143000)</li>
   * <li>Standard flexible formats (14:30, 14h25, 9:20pm)</li>
   * </ol>
   *
   * @param text the text to parse, must not be null
   * @return the parsed Time
   * @throws NullPointerException   if text is null
   * @throws DateTimeParseException if all parsing strategies fail
   */
  public static Time parse( String text )
  {
    Objects.requireNonNull( text, "Text cannot be null" );

    String input = text.trim().replaceAll( "\\s+", " " );

    if ( input.isEmpty() )
      throw new DateTimeParseException( "Cannot parse empty string", text, 0 );

    try
    {
      // try natural language parsing (relative times, offsets, named times)
      if ( isNaturalLanguage( input ) )
      {
        try
        {
          return parseNaturalLanguage( input.toLowerCase( Locale.ROOT ) );
        }
        catch ( DateTimeParseException ignored )
        {
        }
      }

      // handle special end-of-day case (24:00:00) which LocalTime does not support
      if ( END_OF_DAY_PATTERN.matcher( input ).matches() )
        return Time.MAX_VALUE;

      // try custom user-registered patterns
      for ( DateTimeFormatter formatter : m_customPatterns )
      {
        try
        {
          return Time.of( LocalTime.parse( input, formatter ) );
        }
        catch ( DateTimeParseException ignored )
        {
        }
      }

      // try numeric compact parsing (e.g. "830" -> 08:30)
      if ( NUMERIC_PATTERN.matcher( input ).matches() )
      {
        try
        {
          return parseNumericString( input );
        }
        catch ( IllegalArgumentException ignored )
        {
        }
      }

      // try standard flexible formatter (covers 12h AM/PM and 24h ISO/Letters)
      return Time.of( LocalTime.parse( input, STD_FORMATTER ) );
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
   * Custom patterns are attempted after natural language but before standard formats.
   *
   * @param pattern the pattern string to register using Java date-time pattern syntax
   * @throws IllegalArgumentException if the pattern is invalid
   */
  public static void registerCustomPattern( String pattern )
  {
    // add the custom pattern formatter to the list
    m_customPatterns.add( DateTimeFormatter.ofPattern( pattern ) );
  }

  /********************************** isNaturalLanguage ************************************/
  // checks if input appears to be natural language rather than a standard time format
  private static boolean isNaturalLanguage( String input )
  {
    // natural language inputs start with a letter, plus sign, or minus sign
    char first = input.charAt( 0 );
    return Character.isLetter( first ) || first == '+' || first == '-';
  }

  /******************************** parseNaturalLanguage ***********************************/
  // parses natural language time expressions including relative times and offsets
  private static Time parseNaturalLanguage( String input )
  {
    Time now = Time.now();

    // handle simple keyword expressions
    switch ( input )
    {
      case "now" -> {
        return now;
      }
      case "noon", "midday" -> {
        return Time.NOON;
      }
      case "midnight", "start of day" -> {
        return Time.MIN_VALUE;
      }
      case "end of day" -> {
        return Time.MAX_VALUE;
      }
    }

    // handle offset expressions (+1 hour, -30 mins, etc.)
    Matcher m = OFFSET_PATTERN.matcher( input );
    if ( m.matches() )
    {
      int amount = Integer.parseInt( m.group( 2 ) ) * ( m.group( 1 ).equals( "-" ) ? -1 : 1 );
      String unit = m.group( 3 );

      return switch ( unit )
      {
        case "h", "hour", "hours" -> now.plusHours( amount );
        case "m", "min", "mins", "minute", "minutes" -> now.plusMinutes( amount );
        case "s", "sec", "secs", "second", "seconds" -> now.plusSeconds( amount );
        case "ms", "millis", "millisecond", "milliseconds" -> now.plusMilliseconds( amount );
        default -> throw new DateTimeParseException( "Unknown time unit: " + unit, input, 0 );
      };
    }

    throw new DateTimeParseException( "Unknown expression: " + input, input, 0 );
  }

  /********************************* parseNumericString ************************************/
  // parses a pure numeric string into time components based on string length
  private static Time parseNumericString( String numStr )
  {
    int num = Integer.parseInt( numStr );
    int len = numStr.length();

    // logic mirrors Time.parseNumericString but allows for standalone utility usage
    return switch ( len )
    {
      case 1, 2 -> Time.of( num, 0, 0, 0 ); // "8" -> 08:00:00
      case 3, 4 -> Time.of( num / 100, num % 100, 0, 0 ); // "830" -> 08:30:00
      case 5, 6 -> Time.of( num / 10000, ( num / 100 ) % 100, num % 100, 0 ); // "83045" -> 08:30:45
      default -> throw new IllegalArgumentException( "Numeric string too long" );
    };
  }
}