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

import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/*************************************************************************************************/
/******************************* Temporal pattern adapter ****************************************/
/*************************************************************************************************/

/** Shared pattern, custom half-year field, and stored-{@code 24:00} formatting support. */
final class TemporalPatternSupport
{
  private static final TemporalField     HALF_OF_YEAR = new HalfOfYearField();
  private static final Map<Long, String> HALF_SHORT   = Map.of( 1L, "H1", 2L, "H2" );
  private static final Map<Long, String> HALF_FULL    = Map.of( 1L, "1st half", 2L, "2nd half" );

  private TemporalPatternSupport()
  {
  }

  static String formatDate( int epochDay, String pattern )
  {
    Objects.requireNonNull( pattern, "pattern" );
    return extendedFormatter( pattern, Locale.getDefault() ).format( StoredTemporal.date( epochDay ) );
  }

  static String formatDate( int epochDay, DateTimeFormatter formatter )
  {
    return Objects.requireNonNull( formatter, "formatter" ).format( StoredTemporal.date( epochDay ) );
  }

  static String formatTime( int milliseconds, String pattern )
  {
    Objects.requireNonNull( pattern, "pattern" );
    return extendedFormatter( pattern, Locale.getDefault() ).format( StoredTemporal.time( milliseconds ) );
  }

  static String formatTime( int milliseconds, DateTimeFormatter formatter )
  {
    return Objects.requireNonNull( formatter, "formatter" ).format( StoredTemporal.time( milliseconds ) );
  }

  static String formatDateTime( int epochDay, int milliseconds, String pattern )
  {
    Objects.requireNonNull( pattern, "pattern" );
    return extendedFormatter( pattern, Locale.getDefault() )
        .format( StoredTemporal.dateTime( epochDay, milliseconds ) );
  }

  static String formatDateTime( int epochDay, int milliseconds, DateTimeFormatter formatter )
  {
    return Objects.requireNonNull( formatter, "formatter" ).format( StoredTemporal.dateTime( epochDay, milliseconds ) );
  }

  static int parseDate( String text, String pattern, Locale locale )
  {
    Objects.requireNonNull( text, "text" );
    Objects.requireNonNull( pattern, "pattern" );
    Objects.requireNonNull( locale, "locale" );
    DateTimeFormatter formatter = extendedFormatter( pattern, locale );
    return Date.of( LocalDate.parse( text, formatter ) ).getEpochDay();
  }

  static DateTimeFormatter compileDatePattern( String pattern, Locale locale )
  {
    return extendedFormatter( Objects.requireNonNull( pattern, "pattern" ),
        Objects.requireNonNull( locale, "locale" ) );
  }

  static int parseTime( String text, String pattern, Locale locale )
  {
    Objects.requireNonNull( text, "text" );
    Objects.requireNonNull( pattern, "pattern" );
    Objects.requireNonNull( locale, "locale" );

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern( pattern, locale );
    ParsePosition position = new ParsePosition( 0 );
    // Inspect raw fields before Java's resolver rejects the otherwise intentional hour 24.
    TemporalAccessor unresolved = formatter.parseUnresolved( text, position );
    if ( unresolved == null || position.getErrorIndex() >= 0 || position.getIndex() != text.length() )
      throw new DateTimeParseException( "Text does not match time pattern", text,
          position.getErrorIndex() >= 0 ? position.getErrorIndex() : position.getIndex() );

    rejectNonTimeFields( unresolved, text );
    if ( isEndpointCandidate( unresolved ) )
    {
      validateEndpointFields( unresolved, text );
      return Time.MILLIS_PER_DAY;
    }

    LocalTime time = LocalTime.parse( text, formatter );
    return (int) ( time.toNanoOfDay() / 1_000_000L );
  }

  private static DateTimeFormatter extendedFormatter( String pattern, Locale locale )
  {
    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
    StringBuilder standard = new StringBuilder();
    boolean quoted = false;

    // Scan structurally so U and pU inside quoted literals retain normal pattern meaning.
    for ( int index = 0; index < pattern.length(); )
    {
      char character = pattern.charAt( index );
      if ( character == '\'' )
      {
        standard.append( character );
        if ( index + 1 < pattern.length() && pattern.charAt( index + 1 ) == '\'' )
        {
          standard.append( '\'' );
          index += 2;
          continue;
        }
        quoted = !quoted;
        index++;
        continue;
      }

      if ( !quoted && character == 'p' )
      {
        int padEnd = index;
        while ( padEnd < pattern.length() && pattern.charAt( padEnd ) == 'p' )
          padEnd++;
        if ( padEnd < pattern.length() && pattern.charAt( padEnd ) == 'U' )
        {
          appendStandard( builder, standard );
          index = appendPaddedHalfToken( builder, pattern, padEnd, padEnd - index );
          continue;
        }
      }

      if ( !quoted && character == 'U' )
      {
        appendStandard( builder, standard );
        index = appendHalfToken( builder, pattern, index );
        continue;
      }

      standard.append( character );
      index++;
    }

    appendStandard( builder, standard );
    return builder.toFormatter( locale );
  }

  private static int appendHalfToken( DateTimeFormatterBuilder builder, String pattern, int start )
  {
    int end = start;
    while ( end < pattern.length() && pattern.charAt( end ) == 'U' )
      end++;
    int count = end - start;
    if ( count > 3 )
      throw new IllegalArgumentException( "Too many consecutive half-year pattern letters: " + count );
    if ( count == 1 )
      builder.appendValue( HALF_OF_YEAR );
    else
    {
      builder.parseCaseInsensitive();
      builder.appendText( HALF_OF_YEAR, count == 2 ? HALF_SHORT : HALF_FULL );
      builder.parseCaseSensitive();
    }
    return end;
  }

  private static int appendPaddedHalfToken( DateTimeFormatterBuilder builder, String pattern, int start, int width )
  {
    int end = start;
    while ( end < pattern.length() && pattern.charAt( end ) == 'U' )
      end++;
    int count = end - start;
    if ( count > 3 )
      throw new IllegalArgumentException( "Too many consecutive half-year pattern letters: " + count );
    if ( count == 1 )
    {
      // padNext must immediately precede the element it decorates in the formatter builder.
      builder.padNext( width );
      builder.appendValue( HALF_OF_YEAR );
    }
    else
    {
      builder.parseCaseInsensitive();
      builder.padNext( width );
      builder.appendText( HALF_OF_YEAR, count == 2 ? HALF_SHORT : HALF_FULL );
      builder.parseCaseSensitive();
    }
    return end;
  }

  private static void appendStandard( DateTimeFormatterBuilder builder, StringBuilder standard )
  {
    if ( !standard.isEmpty() )
    {
      builder.appendPattern( standard.toString() );
      standard.setLength( 0 );
    }
  }

  private static boolean isEndpointCandidate( TemporalAccessor temporal )
  {
    return valueEquals( temporal, ChronoField.HOUR_OF_DAY, 24L )
        || valueEquals( temporal, ChronoField.CLOCK_HOUR_OF_DAY, 24L )
        || valueEquals( temporal, ChronoField.MILLI_OF_DAY, Time.MILLIS_PER_DAY )
        || valueEquals( temporal, ChronoField.NANO_OF_DAY, (long) Time.MILLIS_PER_DAY * 1_000_000L );
  }

  private static boolean valueEquals( TemporalAccessor temporal, ChronoField field, long expected )
  {
    return temporal.isSupported( field ) && temporal.getLong( field ) == expected;
  }

  private static void validateEndpointFields( TemporalAccessor temporal, String text )
  {
    // Every supplied sub-hour field must describe exact end-of-day, never 24:xx.
    for ( ChronoField field : ChronoField.values() )
      if ( field.isTimeBased() && temporal.isSupported( field )
          && temporal.getLong( field ) != StoredTemporal.endpointValue( field ) )
        throw new DateTimeParseException( "Time field conflicts with 24:00 endpoint: " + field, text, 0 );
  }

  private static void rejectNonTimeFields( TemporalAccessor temporal, String text )
  {
    for ( ChronoField field : ChronoField.values() )
      if ( temporal.isSupported( field ) && !field.isTimeBased() )
        throw new DateTimeParseException( "Pattern contains a non-time field: " + field, text, 0 );
  }

  private static final class StoredTemporal implements TemporalAccessor
  {
    private final LocalDate date;
    private final int       milliseconds;
    private final boolean   hasTime;

    private StoredTemporal( LocalDate date, int milliseconds, boolean hasTime )
    {
      this.date = date;
      this.milliseconds = milliseconds;
      this.hasTime = hasTime;
    }

    static StoredTemporal date( int epochDay )
    {
      return new StoredTemporal( LocalDate.ofEpochDay( epochDay ), 0, false );
    }

    static StoredTemporal time( int milliseconds )
    {
      return new StoredTemporal( null, milliseconds, true );
    }

    static StoredTemporal dateTime( int epochDay, int milliseconds )
    {
      return new StoredTemporal( LocalDate.ofEpochDay( epochDay ), milliseconds, true );
    }

    @Override
    public boolean isSupported( TemporalField field )
    {
      if ( field == HALF_OF_YEAR )
        return date != null;
      if ( field instanceof ChronoField chronoField && chronoField.isTimeBased() )
        return hasTime;
      return date != null && date.isSupported( field );
    }

    @Override
    public long getLong( TemporalField field )
    {
      if ( field == HALF_OF_YEAR )
        return date.getMonthValue() <= 6 ? 1L : 2L;
      if ( field instanceof ChronoField chronoField && chronoField.isTimeBased() )
      {
        if ( !hasTime )
          throw new UnsupportedTemporalTypeException( field.toString() );
        return timeValue( chronoField, milliseconds );
      }
      if ( date == null )
        throw new UnsupportedTemporalTypeException( field.toString() );
      return date.getLong( field );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <R> R query( TemporalQuery<R> query )
    {
      if ( query == TemporalQueries.chronology() )
        return date == null ? null : (R) IsoChronology.INSTANCE;
      if ( query == TemporalQueries.localDate() )
        return (R) date;
      if ( query == TemporalQueries.localTime() )
        return hasTime && milliseconds < Time.MILLIS_PER_DAY ? (R) LocalTime.ofNanoOfDay( milliseconds * 1_000_000L )
            : null;
      if ( query == TemporalQueries.precision() )
        return (R) ChronoUnit.MILLIS;
      if ( query == TemporalQueries.zone() || query == TemporalQueries.zoneId() || query == TemporalQueries.offset() )
        return null;
      return TemporalAccessor.super.query( query );
    }

    static long endpointValue( ChronoField field )
    {
      return timeValue( field, Time.MILLIS_PER_DAY );
    }

    private static long timeValue( ChronoField field, int milliseconds )
    {
      long hour = milliseconds / Time.MILLIS_PER_HOUR;
      long minute = milliseconds / Time.MILLIS_PER_MINUTE;
      long second = milliseconds / Time.MILLIS_PER_SECOND;
      boolean endpoint = milliseconds == Time.MILLIS_PER_DAY;
      return switch ( field )
      {
        case NANO_OF_SECOND -> milliseconds % Time.MILLIS_PER_SECOND * 1_000_000L;
        case NANO_OF_DAY -> milliseconds * 1_000_000L;
        case MICRO_OF_SECOND -> milliseconds % Time.MILLIS_PER_SECOND * 1_000L;
        case MICRO_OF_DAY -> milliseconds * 1_000L;
        case MILLI_OF_SECOND -> milliseconds % Time.MILLIS_PER_SECOND;
        case MILLI_OF_DAY -> milliseconds;
        case SECOND_OF_MINUTE -> second % 60L;
        case SECOND_OF_DAY -> second;
        case MINUTE_OF_HOUR -> minute % 60L;
        case MINUTE_OF_DAY -> minute;
        case HOUR_OF_AMPM -> hour % 12L;
        case CLOCK_HOUR_OF_AMPM -> hour % 12L == 0L ? 12L : hour % 12L;
        case HOUR_OF_DAY -> hour;
        case CLOCK_HOUR_OF_DAY -> hour == 0L ? 24L : hour;
        case AMPM_OF_DAY -> endpoint ? 0L : hour / 12L;
        default -> throw new UnsupportedTemporalTypeException( field.toString() );
      };
    }
  }

  private static final class HalfOfYearField implements TemporalField
  {
    @Override
    public TemporalUnitBase getBaseUnit()
    {
      return TemporalUnitBase.HALF_YEARS;
    }

    @Override
    public ChronoUnit getRangeUnit()
    {
      return ChronoUnit.YEARS;
    }

    @Override
    public ValueRange range()
    {
      return ValueRange.of( 1L, 2L );
    }

    @Override
    public boolean isDateBased()
    {
      return true;
    }

    @Override
    public boolean isTimeBased()
    {
      return false;
    }

    @Override
    public boolean isSupportedBy( TemporalAccessor temporal )
    {
      return temporal.isSupported( ChronoField.MONTH_OF_YEAR );
    }

    @Override
    public ValueRange rangeRefinedBy( TemporalAccessor temporal )
    {
      return range();
    }

    @Override
    public long getFrom( TemporalAccessor temporal )
    {
      return ( temporal.getLong( ChronoField.MONTH_OF_YEAR ) - 1L ) / 6L + 1L;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <R extends Temporal> R adjustInto( R temporal, long newValue )
    {
      range().checkValidValue( newValue, this );
      return (R) temporal.with( ChronoField.MONTH_OF_YEAR, newValue == 1L ? 1L : 7L );
    }

    @Override
    public TemporalAccessor resolve( Map<TemporalField, Long> fields, TemporalAccessor partial,
        ResolverStyle resolverStyle )
    {
      long half = range().checkValidValue( fields.remove( this ), this );
      if ( !fields.containsKey( ChronoField.YEAR ) && !fields.containsKey( ChronoField.YEAR_OF_ERA ) )
        throw new DateTimeException( "Half-year parsing requires a year" );
      long firstMonth = half == 1L ? 1L : 7L;
      Long month = fields.get( ChronoField.MONTH_OF_YEAR );
      if ( month == null )
        fields.put( ChronoField.MONTH_OF_YEAR, firstMonth );
      else if ( ( month - 1L ) / 6L + 1L != half )
        throw new DateTimeException( "Month conflicts with parsed half-year" );
      fields.putIfAbsent( ChronoField.DAY_OF_MONTH, 1L );
      return null;
    }

    @Override
    public String getDisplayName( Locale locale )
    {
      return "HalfOfYear";
    }

    @Override
    public String toString()
    {
      return "HalfOfYear";
    }
  }

  /** Narrow unit adapter required by {@link TemporalField}; not exposed outside this class. */
  private enum TemporalUnitBase implements java.time.temporal.TemporalUnit
  {
    HALF_YEARS;

    @Override
    public java.time.Duration getDuration()
    {
      return java.time.Duration.ofDays( 182 );
    }

    @Override
    public boolean isDurationEstimated()
    {
      return true;
    }

    @Override
    public boolean isDateBased()
    {
      return true;
    }

    @Override
    public boolean isTimeBased()
    {
      return false;
    }

    @Override
    public boolean isSupportedBy( Temporal temporal )
    {
      return temporal.isSupported( ChronoField.MONTH_OF_YEAR );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <R extends Temporal> R addTo( R temporal, long amount )
    {
      return (R) temporal.plus( Math.multiplyExact( amount, 6L ), ChronoUnit.MONTHS );
    }

    @Override
    public long between( Temporal first, Temporal second )
    {
      return ChronoUnit.MONTHS.between( first, second ) / 6L;
    }
  }
}
