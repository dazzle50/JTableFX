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

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*************************************************************************************************/
/********************************* Intelligent Date parser ***************************************/
/*************************************************************************************************/

/**
 * Immutable, thread-safe parser for natural-language, numeric, standard, and configured date
 * forms. Recognized English keywords include {@code today}, {@code tomorrow}, {@code yesterday},
 * and the start or end of the current month or year. Signed day, week, month, and year offsets are
 * supported, as are {@code next}, {@code last}, and {@code this} followed by a localized weekday
 * name. Partial dates use the reference year.
 *
 * <p>Each intelligent {@link #parse(String)} call reads the configured clock exactly once, so all
 * relative and partial strategies use the same reference date. Configured patterns are tried in
 * insertion order before built-in forms.</p>
 */
public final class DateParser
{
  private static final Pattern          OFFSET = Pattern.compile( "^([+-])\\s*(\\d+)\\s*([a-z]+)$" );

  private final Clock                   clock;
  private final Locale                  locale;
  private final List<DateTimeFormatter> customPatterns;
  private final List<DateTimeFormatter> standardPatterns;
  private final List<DateTimeFormatter> partialPatterns;

  private DateParser( Clock clock, Locale locale, List<String> patterns )
  {
    this.clock = Objects.requireNonNull( clock, "clock" );
    this.locale = Objects.requireNonNull( locale, "locale" );
    customPatterns = patterns.stream().map( pattern -> TemporalPatternSupport.compileDatePattern( pattern, locale ) )
        .toList();
    standardPatterns = compileStandardPatterns( locale );
    partialPatterns = compilePartialPatterns( locale );
  }

  // ================================= Parser construction =================================

  /**
   * Creates a parser using the system-default time zone and locale observed when this method is
   * called. Each later intelligent parse obtains a fresh instant from that clock.
   *
   * @return system-default parser
   */
  public static DateParser systemDefault()
  {
    return builder().build();
  }

  /**
   * Creates a parser builder.
   *
   * @return a mutable builder whose result is immutable
   */
  public static Builder builder()
  {
    return new Builder();
  }

  // ===================================== Parsing =====================================

  /**
   * Parses intelligent date text. Configured patterns are tried in insertion order before the
   * built-in complete and partial forms.
   *
   * @param text text to parse
   * @return parsed date
   * @throws NullPointerException if {@code text} is null
   * @throws DateTimeParseException if no supported interpretation succeeds
   * @throws ArithmeticException if relative arithmetic or compact storage overflows
   */
  public Date parse( String text )
  {
    return parse( text, ParseContext.capture( clock, locale ) );
  }

  /**
   * Parses text exclusively with the supplied pattern and this parser's locale. This operation
   * does not read the clock.
   *
   * @param text text to parse
   * @param pattern parsing pattern, including the optional {@code U} half-year extension
   * @return parsed date
   * @throws NullPointerException if either argument is null
   * @throws IllegalArgumentException if the pattern is invalid
   * @throws DateTimeParseException if the text does not resolve to a date
   * @throws ArithmeticException if the resolved date is outside compact storage
   */
  public Date parse( String text, String pattern )
  {
    return Date.ofEpochDay( TemporalPatternSupport.parseDate( text, pattern, locale ) );
  }

  Date parse( String text, ParseContext context )
  {
    Objects.requireNonNull( text, "text" );
    Objects.requireNonNull( context, "context" );
    String input = text.trim().replaceAll( "\\s+", " " );
    if ( input.isEmpty() )
      throw new DateTimeParseException( "Cannot parse empty date", text, 0 );

    Date natural = tryNatural( input, context );
    if ( natural != null )
      return natural;

    String normalized = input.replaceAll( "[\\s\\-.:/\\\\_,|=]+", "/" ).replaceAll( "/$", "" );
    Date parsed = tryFormatters( normalized, customPatterns );
    if ( parsed == null && !normalized.equals( input ) )
      parsed = tryFormatters( input, customPatterns );
    if ( parsed == null )
      parsed = tryFormatters( normalized, standardPatterns );
    if ( parsed != null )
      return parsed;

    // Missing years are resolved against the single reference captured at parse entry.
    for ( DateTimeFormatter formatter : partialPatterns )
      try
      {
        MonthDay partial = MonthDay.from( formatter.parse( normalized ) );
        return Date.of( context.reference().getYear(), partial.getMonthValue(), partial.getDayOfMonth() );
      }
      catch ( DateTimeParseException ignored )
      {
      }

    throw new DateTimeParseException( "Unable to parse date: " + text, text, 0 );
  }

  private Date tryNatural( String input, ParseContext context )
  {
    if ( !( Character.isLetter( input.charAt( 0 ) ) || input.charAt( 0 ) == '+' || input.charAt( 0 ) == '-' ) )
      return null;
    String lower = input.toLowerCase( Locale.ROOT );
    Date today = Date.of( context.reference().toLocalDate() );
    switch ( lower )
    {
      case "now", "today":
        return today;
      case "tomorrow":
        return today.plusDays( 1 );
      case "yesterday":
        return today.plusDays( -1 );
      case "start of month", "first of month":
        return today.startOf( DateInterval.MONTH );
      case "end of month", "last of month":
        return today.endOf( DateInterval.MONTH );
      case "start of year", "first of year":
        return today.startOf( DateInterval.YEAR );
      case "end of year", "last of year":
        return today.endOf( DateInterval.YEAR );
      default:
    }

    if ( lower.startsWith( "next " ) || lower.startsWith( "last " ) || lower.startsWith( "this " ) )
    {
      String[] parts = lower.split( "\\s+" );
      if ( parts.length != 2 )
        return null;
      DayOfWeek target = parseDayOfWeek( parts[1] );
      int current = today.getDayOfWeek().getValue();
      int targetValue = target.getValue();
      long days = switch ( parts[0] )
      {
        case "next" -> Math.floorMod( targetValue - current - 1, 7 ) + 1L;
        case "last" -> -( Math.floorMod( current - targetValue - 1, 7 ) + 1L );
        default -> Math.floorMod( targetValue - current, 7 );
      };
      return today.plusDays( days );
    }

    Matcher matcher = OFFSET.matcher( lower );
    if ( !matcher.matches() )
      return null;
    long amount;
    try
    {
      amount = Long.parseLong( matcher.group( 2 ) );
      if ( matcher.group( 1 ).equals( "-" ) )
        amount = Math.negateExact( amount );
    }
    catch ( ArithmeticException | NumberFormatException exception )
    {
      throw new DateTimeParseException( "Date offset is outside supported range", input, 0, exception );
    }
    return switch ( matcher.group( 3 ) )
    {
      case "d", "day", "days" -> today.plusDays( amount );
      case "w", "week", "weeks" -> today.plusWeeks( amount );
      case "m", "month", "months" -> today.plusMonths( amount );
      case "y", "year", "years" -> today.plusYears( amount );
      default -> null;
    };
  }

  private DayOfWeek parseDayOfWeek( String text )
  {
    for ( DayOfWeek day : DayOfWeek.values() )
      if ( day.getDisplayName( TextStyle.FULL, locale ).equalsIgnoreCase( text )
          || day.getDisplayName( TextStyle.SHORT, locale ).equalsIgnoreCase( text ) )
        return day;
    throw new DateTimeParseException( "Invalid weekday: " + text, text, 0 );
  }

  private static Date tryFormatters( String text, List<DateTimeFormatter> formatters )
  {
    for ( DateTimeFormatter formatter : formatters )
      try
      {
        return Date.of( LocalDate.parse( text, formatter ) );
      }
      catch ( DateTimeParseException ignored )
      {
      }
    return null;
  }

  private static List<DateTimeFormatter> compileStandardPatterns( Locale locale )
  {
    return List
        .of( "uuuu/M/d", "ddMMuuuu", "ddMMuu", "d/M/uuuu", "d/M/uu", "d/MMMM/uuuu", "MMMM/d/uuuu", "d/MMM/uuuu",
            "MMM/d/uuuu", "d/MMMM/uu", "MMMM/d/uu", "d/MMM/uu", "MMM/d/uu" )
        .stream().map( pattern -> new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern( pattern )
            .toFormatter( locale ) )
        .toList();
  }

  private static List<DateTimeFormatter> compilePartialPatterns( Locale locale )
  {
    return List.of( "ddMM", "d/M", "d/MMMM", "MMMM/d", "d/MMM", "MMM/d" ).stream()
        .map( pattern -> new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern( pattern )
            .parseDefaulting( ChronoField.YEAR, 2000 ).toFormatter( locale ) )
        .toList();
  }

  /** Mutable construction helper; built parsers are immutable. */
  public static final class Builder
  {
    private Clock              clock    = Clock.systemDefaultZone();
    private Locale             locale   = Locale.getDefault();
    private final List<String> patterns = new ArrayList<>();

    private Builder()
    {
    }

    /**
     * Sets the clock whose current local date supplies relative values and missing years. A parse
     * operation reads the clock once using the clock's own time zone.
     *
     * @param clock reference clock
     * @return this builder
     * @throws NullPointerException if {@code clock} is null
     */
    public Builder clock( Clock clock )
    {
      this.clock = Objects.requireNonNull( clock, "clock" );
      return this;
    }

    /**
     * Sets the locale used for weekday and month names and for configured patterns. Natural
     * language operators such as {@code next} and {@code tomorrow} remain English.
     *
     * @param locale parsing locale
     * @return this builder
     * @throws NullPointerException if {@code locale} is null
     */
    public Builder locale( Locale locale )
    {
      this.locale = Objects.requireNonNull( locale, "locale" );
      return this;
    }

    /**
     * Adds a complete date pattern at the end of the configured-pattern search order. Compilation
     * is deferred until {@link #build()}, using the builder's final locale; the pattern may use the
     * {@code U}, {@code UU}, and {@code UUU} half-year extension.
     *
     * @param pattern date pattern
     * @return this builder
     * @throws NullPointerException if {@code pattern} is null
     */
    public Builder addPattern( String pattern )
    {
      patterns.add( Objects.requireNonNull( pattern, "pattern" ) );
      return this;
    }

    /**
     * Compiles configured patterns and creates an immutable parser snapshot.
     *
     * @return configured parser
     * @throws IllegalArgumentException if any configured pattern is invalid
     */
    public DateParser build()
    {
      return new DateParser( clock, locale, List.copyOf( patterns ) );
    }
  }
}
