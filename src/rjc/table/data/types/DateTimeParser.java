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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/*************************************************************************************************/
/**************************** Intelligent zone-free DateTime parser ******************************/
/*************************************************************************************************/

/**
 * Immutable, thread-safe parser composing {@link DateParser} and {@link TimeParser}. All branches
 * in one intelligent parse share one captured reference date-time and locale. Combined input may
 * separate its date and time with a space or {@code T}; date-only input uses {@code 00:00}, and
 * time-only input uses the captured reference date.
 */
public final class DateTimeParser
{
  private final Clock      clock;
  private final Locale     locale;
  private final DateParser dateParser;
  private final TimeParser timeParser;

  private DateTimeParser( Clock clock, Locale locale, List<String> datePatterns, List<String> timePatterns )
  {
    this.clock = Objects.requireNonNull( clock, "clock" );
    this.locale = Objects.requireNonNull( locale, "locale" );
    DateParser.Builder dateBuilder = DateParser.builder().clock( clock ).locale( locale );
    datePatterns.forEach( dateBuilder::addPattern );
    dateParser = dateBuilder.build();
    TimeParser.Builder timeBuilder = TimeParser.builder().clock( clock ).locale( locale );
    timePatterns.forEach( timeBuilder::addPattern );
    timeParser = timeBuilder.build();
  }

  // ================================= Parser construction =================================

  /**
   * Creates a composed parser using the system-default time zone and locale observed when this
   * method is called. Each later parse obtains a fresh instant from that clock.
   *
   * @return system-default composed parser
   */
  public static DateTimeParser systemDefault()
  {
    return builder().build();
  }

  /**
   * Creates a composed-parser builder.
   *
   * @return a mutable builder whose result is immutable
   */
  public static Builder builder()
  {
    return new Builder();
  }

  // ===================================== Parsing =====================================

  /**
   * Parses combined date-time text, date-only text at {@code 00:00}, or time-only text on the
   * reference date. A standalone time offset is applied to the captured reference date-time; an
   * offset following a parsed date is applied to {@code 00:00} on that date. Either form can cross
   * stored-date boundaries and follows {@link DateTime}'s direction-sensitive {@code 24:00}
   * notation.
   *
   * @param text text to parse
   * @return parsed date-time
   * @throws NullPointerException if {@code text} is null
   * @throws DateTimeParseException if no supported interpretation succeeds
   * @throws ArithmeticException if relative arithmetic or compact storage overflows
   */
  public DateTime parse( String text )
  {
    Objects.requireNonNull( text, "text" );
    ParseContext context = ParseContext.capture( clock, locale );
    String input = text.trim().replaceAll( "\\s+", " " );
    if ( input.isEmpty() )
      throw new DateTimeParseException( "Cannot parse empty date-time", text, 0 );
    if ( input.equalsIgnoreCase( "now" ) )
      return DateTime.of( context.reference() );

    TimeParser.TimeOffset standaloneOffset = TimeParser.parseOffset( input );
    if ( standaloneOffset != null )
      return standaloneOffset.apply( DateTime.of( context.reference() ) );

    DateTime combined = tryCombined( input, context, ' ' );
    if ( combined == null )
      combined = tryCombined( input, context, 'T' );
    if ( combined != null )
      return combined;

    try
    {
      return DateTime.of( dateParser.parse( input, context ), Time.MIN_VALUE );
    }
    catch ( DateTimeParseException ignored )
    {
    }
    try
    {
      return DateTime.of( Date.of( context.reference().toLocalDate() ), timeParser.parse( input, context ) );
    }
    catch ( DateTimeParseException exception )
    {
      throw new DateTimeParseException( "Unable to parse date-time: " + text, text, 0, exception );
    }
  }

  private DateTime tryCombined( String input, ParseContext context, char separator )
  {
    for ( int from = input.length(); from > 0; )
    {
      int index = input.lastIndexOf( separator, from - 1 );
      if ( index < 0 )
        return null;
      from = index;
      if ( index == 0 || index == input.length() - 1 )
        continue;
      String dateText = input.substring( 0, index ).trim();
      String timeText = input.substring( index + 1 ).trim();
      try
      {
        Date date = dateParser.parse( dateText, context );
        TimeParser.TimeOffset offset = TimeParser.parseOffset( timeText );
        if ( offset != null )
          return offset.apply( DateTime.of( date, Time.MIN_VALUE ) );
        return DateTime.of( date, timeParser.parse( timeText, context ) );
      }
      catch ( DateTimeParseException ignored )
      {
      }
    }
    return null;
  }

  /** Mutable construction helper; built parsers are immutable. */
  public static final class Builder
  {
    private Clock              clock        = Clock.systemDefaultZone();
    private Locale             locale       = Locale.getDefault();
    private final List<String> datePatterns = new ArrayList<>();
    private final List<String> timePatterns = new ArrayList<>();

    private Builder()
    {
    }

    /**
     * Sets the clock used by every component parser. A top-level parse reads it exactly once using
     * the clock's own time zone.
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
     * Sets the locale used by both component parsers. Localized weekday, month, and formatter text
     * use this locale; natural-language operators remain English.
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
     * Adds a date pattern to the composed date parser in insertion order. Validation is deferred
     * until {@link #build()}, using the builder's final locale. The pattern may use the
     * {@code U}, {@code UU}, and {@code UUU} half-year extension.
     *
     * @param pattern date pattern
     * @return this builder
     * @throws NullPointerException if {@code pattern} is null
     */
    public Builder addDatePattern( String pattern )
    {
      datePatterns.add( Objects.requireNonNull( pattern, "pattern" ) );
      return this;
    }

    /**
     * Adds a time pattern to the composed time parser in insertion order. Validation is deferred
     * until {@link #build()}, using the builder's final locale. Exact {@code 24:00} input is
     * accepted only when the configured fields describe that endpoint without conflict.
     *
     * @param pattern time pattern
     * @return this builder
     * @throws NullPointerException if {@code pattern} is null
     */
    public Builder addTimePattern( String pattern )
    {
      timePatterns.add( Objects.requireNonNull( pattern, "pattern" ) );
      return this;
    }

    /**
     * Creates an immutable parser snapshot and validates all configured patterns.
     *
     * @return configured parser
     * @throws IllegalArgumentException if any configured pattern is invalid
     */
    public DateTimeParser build()
    {
      return new DateTimeParser( clock, locale, List.copyOf( datePatterns ), List.copyOf( timePatterns ) );
    }
  }
}
