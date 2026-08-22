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
/******************************* Intelligent stored-Time parser **********************************/
/*************************************************************************************************/

/**
 * Immutable, thread-safe parser for named, relative, compact numeric, patterned, and conventional
 * time forms. Recognized English names include {@code now}, {@code noon}, {@code midday},
 * {@code midnight}, {@code start of day}, and {@code end of day}. Signed hour, minute, second, and
 * millisecond offsets are applied cyclically to the reference time. Compact numeric input accepts
 * one or two digits as an hour, three or four as {@code HHmm}, and five or six as {@code HHmmss}.
 *
 * <p>Each intelligent {@link #parse(String)} call reads the configured clock exactly once.
 * Configured patterns are tried in insertion order before compact numeric and built-in forms. The
 * explicit {@code 24:00} stored endpoint is retained.</p>
 */
public final class TimeParser
{
  private static final Pattern    OFFSET     = Pattern.compile( "^([+-])\\s*(\\d+)\\s*([a-z]+)$" );
  private static final Pattern    NUMERIC    = Pattern.compile( "^\\d+$" );
  private static final Pattern    END_OF_DAY = Pattern.compile( "^24[:\\s.h]?0*[:\\s.m]?0*[:\\s.s]?0*$" );

  private final Clock             clock;
  private final Locale            locale;
  private final List<String>      customPatterns;
  private final DateTimeFormatter standardFormatter;

  private TimeParser( Clock clock, Locale locale, List<String> patterns )
  {
    this.clock = Objects.requireNonNull( clock, "clock" );
    this.locale = Objects.requireNonNull( locale, "locale" );
    for ( String pattern : patterns )
      DateTimeFormatter.ofPattern( pattern, locale );
    customPatterns = List.copyOf( patterns );
    standardFormatter = new DateTimeFormatterBuilder().parseCaseInsensitive()
        .appendOptional( DateTimeFormatter.ofPattern( "h[[:][.][ ]m[[:][.][ ]s]][ ]a", locale ) )
        .appendOptional( DateTimeFormatter
            .ofPattern( "H[:][.][ ]['h']m['m'][[:][.][ ]['m']s['s'][[:][.][ ]['s'][SSS][SS][S]]]", locale ) )
        .toFormatter( locale );
  }

  // ================================= Parser construction =================================

  /**
   * Creates a parser using the system-default time zone and locale observed when this method is
   * called. Each later intelligent parse obtains a fresh instant from that clock.
   *
   * @return system-default parser
   */
  public static TimeParser systemDefault()
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
   * Parses intelligent stored-time text, using one clock snapshot for relative forms.
   *
   * @param text text to parse
   * @return parsed stored time
   * @throws NullPointerException if {@code text} is null
   * @throws DateTimeParseException if no supported interpretation succeeds
   */
  public Time parse( String text )
  {
    return parse( text, ParseContext.capture( clock, locale ) );
  }

  /**
   * Parses text exclusively with the supplied time pattern and this parser's locale. This
   * operation does not read the clock and accepts exact {@code 24:00} where the fields permit it.
   * Parsed precision below one millisecond is truncated.
   *
   * @param text text to parse
   * @param pattern parsing pattern
   * @return parsed stored time
   * @throws NullPointerException if either argument is null
   * @throws IllegalArgumentException if the pattern is invalid
   * @throws DateTimeParseException if the text cannot resolve to a valid stored time
   */
  public Time parse( String text, String pattern )
  {
    return Time.ofMilliseconds( TemporalPatternSupport.parseTime( text, pattern, locale ) );
  }

  Time parse( String text, ParseContext context )
  {
    Objects.requireNonNull( text, "text" );
    Objects.requireNonNull( context, "context" );
    String input = text.trim().replaceAll( "\\s+", " " );
    if ( input.isEmpty() )
      throw new DateTimeParseException( "Cannot parse empty time", text, 0 );

    String lower = input.toLowerCase( Locale.ROOT );
    Time reference = Time.of( context.reference().toLocalTime() );
    switch ( lower )
    {
      case "now":
        return reference;
      case "noon", "midday":
        return Time.NOON;
      case "midnight", "start of day":
        return Time.MIN_VALUE;
      case "end of day":
        return Time.MAX_VALUE;
      default:
    }

    TimeOffset offset = parseOffset( lower );
    if ( offset != null )
      return offset.apply( reference );
    if ( END_OF_DAY.matcher( lower ).matches() )
      return Time.MAX_VALUE;

    // Preserve insertion order so applications can resolve deliberately overlapping patterns.
    for ( String pattern : customPatterns )
      try
      {
        return Time.ofMilliseconds( TemporalPatternSupport.parseTime( input, pattern, locale ) );
      }
      catch ( DateTimeParseException ignored )
      {
      }

    if ( NUMERIC.matcher( input ).matches() )
      try
      {
        return parseNumeric( input );
      }
      catch ( RuntimeException ignored )
      {
      }

    try
    {
      return Time.of( LocalTime.parse( input, standardFormatter ) );
    }
    catch ( DateTimeParseException exception )
    {
      throw new DateTimeParseException( "Unable to parse time: " + text, text, 0, exception );
    }
  }

  static TimeOffset parseOffset( String input )
  {
    Matcher matcher = OFFSET.matcher( input.toLowerCase( Locale.ROOT ) );
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
      throw new DateTimeParseException( "Time offset is outside supported range", input, 0, exception );
    }
    OffsetUnit unit = switch ( matcher.group( 3 ) )
    {
      case "h", "hour", "hours" -> OffsetUnit.HOUR;
      case "m", "min", "mins", "minute", "minutes" -> OffsetUnit.MINUTE;
      case "s", "sec", "secs", "second", "seconds" -> OffsetUnit.SECOND;
      case "ms", "millis", "millisecond", "milliseconds" -> OffsetUnit.MILLISECOND;
      default -> null;
    };
    return unit == null ? null : new TimeOffset( amount, unit );
  }

  private static Time parseNumeric( String input )
  {
    int value = Integer.parseInt( input );
    return switch ( input.length() )
    {
      case 1, 2 -> Time.of( value );
      case 3, 4 -> Time.of( value / 100, value % 100 );
      case 5, 6 -> Time.of( value / 10_000, value / 100 % 100, value % 100 );
      default -> throw new IllegalArgumentException( "Numeric time is too long" );
    };
  }

  record TimeOffset( long amount, OffsetUnit unit )
  {
    Time apply( Time base )
    {
      return switch ( unit )
      {
        case MILLISECOND -> base.plusMilliseconds( amount );
        case SECOND -> base.plusSeconds( amount );
        case MINUTE -> base.plusMinutes( amount );
        case HOUR -> base.plusHours( amount );
      };
    }

    DateTime apply( DateTime base )
    {
      return switch ( unit )
      {
        case MILLISECOND -> base.plusMilliseconds( amount );
        case SECOND -> base.plusSeconds( amount );
        case MINUTE -> base.plusMinutes( amount );
        case HOUR -> base.plusHours( amount );
      };
    }
  }

  enum OffsetUnit
  {
    MILLISECOND, SECOND, MINUTE, HOUR
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
     * Sets the clock whose current local time supplies {@code now} and relative values. A parse
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
     * Sets the locale used by configured and built-in formatter patterns, including localized
     * text such as AM/PM markers. Named and relative keywords remain English.
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
     * Adds a time pattern at the end of the configured-pattern search order. Validation is
     * deferred until {@link #build()}, using the builder's final locale. Exact {@code 24:00} input
     * is accepted only when the configured fields describe that endpoint without conflict.
     *
     * @param pattern time pattern
     * @return this builder
     * @throws NullPointerException if {@code pattern} is null
     */
    public Builder addPattern( String pattern )
    {
      patterns.add( Objects.requireNonNull( pattern, "pattern" ) );
      return this;
    }

    /**
     * Validates configured patterns and creates an immutable parser snapshot.
     *
     * @return configured parser
     * @throws IllegalArgumentException if any configured pattern is invalid
     */
    public TimeParser build()
    {
      return new TimeParser( clock, locale, List.copyOf( patterns ) );
    }
  }
}
