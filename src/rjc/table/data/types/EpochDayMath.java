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

import java.time.DateTimeException;

/*************************************************************************************************/
/*************************** Allocation-free Gregorian date kernel *******************************/
/*************************************************************************************************/

/** Allocation-free proleptic-Gregorian conversion, arithmetic, and boundary kernel. */
final class EpochDayMath
{
  private static final long DAYS_0000_TO_1970 = 719_468L;
  private static final long DAYS_PER_ERA      = 146_097L;

  private EpochDayMath()
  {
  }

  static int checkedEpochDay( long epochDay )
  {
    if ( epochDay < Integer.MIN_VALUE || epochDay > Integer.MAX_VALUE )
      throw new ArithmeticException( "Epoch day outside compact storage: " + epochDay );
    return (int) epochDay;
  }

  static int fromYmd( int year, int month, int day )
  {
    if ( month < 1 || month > 12 )
      throw new DateTimeException( "Invalid month: " + month );
    int monthLength = monthLength( year, month );
    if ( day < 1 || day > monthLength )
      throw new DateTimeException( "Invalid day " + day + " for " + year + "-" + month );
    return checkedEpochDay( toEpochDay( year, month, day ) );
  }

  static long packedYmd( long epochDay )
  {
    // Shift to the March-based civil calendar used by the 400-year era decomposition.
    long z = Math.addExact( epochDay, DAYS_0000_TO_1970 );
    long era = Math.floorDiv( z, DAYS_PER_ERA );
    long doe = z - era * DAYS_PER_ERA;
    long yoe = ( doe - doe / 1_460L + doe / 36_524L - doe / 146_096L ) / 365L;
    long y = yoe + era * 400L;
    long doy = doe - ( 365L * yoe + yoe / 4L - yoe / 100L );
    long mp = ( 5L * doy + 2L ) / 153L;
    long day = doy - ( 153L * mp + 2L ) / 5L + 1L;
    long month = mp < 10L ? mp + 3L : mp - 9L;
    if ( month <= 2L )
      y++;
    return ( y << 9 ) | ( month << 5 ) | day;
  }

  static int year( int epochDay )
  {
    return (int) ( packedYmd( epochDay ) >> 9 );
  }

  static int month( int epochDay )
  {
    return (int) ( ( packedYmd( epochDay ) >> 5 ) & 0xFL );
  }

  static int dayOfMonth( int epochDay )
  {
    return (int) ( packedYmd( epochDay ) & 0x1FL );
  }

  static int addDays( int epochDay, long days )
  {
    return checkedEpochDay( Math.addExact( (long) epochDay, days ) );
  }

  static int addMonths( int epochDay, long months )
  {
    if ( months == 0L )
      return epochDay;
    long ymd = packedYmd( epochDay );
    long oldYear = ymd >> 9;
    int oldMonth = (int) ( ( ymd >> 5 ) & 0xFL );
    int oldDay = (int) ( ymd & 0x1FL );
    // A continuous month coordinate makes negative years and large signed offsets symmetric.
    long totalMonths = Math.addExact( Math.addExact( Math.multiplyExact( oldYear, 12L ), oldMonth - 1L ), months );
    long newYear = Math.floorDiv( totalMonths, 12L );
    int newMonth = (int) Math.floorMod( totalMonths, 12L ) + 1;
    int year = checkedYear( newYear );
    return fromYmd( year, newMonth, Math.min( oldDay, monthLength( year, newMonth ) ) );
  }

  static int addYears( int epochDay, long years )
  {
    if ( years == 0L )
      return epochDay;
    long ymd = packedYmd( epochDay );
    long newYear = Math.addExact( ymd >> 9, years );
    int year = checkedYear( newYear );
    int month = (int) ( ( ymd >> 5 ) & 0xFL );
    int day = (int) ( ymd & 0x1FL );
    return fromYmd( year, month, Math.min( day, monthLength( year, month ) ) );
  }

  static long periodStart( long storedEpochDay, DateInterval interval )
  {
    return switch ( interval )
    {
      case DAY -> storedEpochDay;
      case WEEK -> Math.subtractExact( storedEpochDay, Math.floorMod( storedEpochDay + 3L, 7L ) );
      case MONTH, QUARTER_YEAR, HALF_YEAR, YEAR -> {
        long ymd = packedYmd( storedEpochDay );
        long year = ymd >> 9;
        int month = (int) ( ( ymd >> 5 ) & 0xFL );
        int firstMonth = switch ( interval )
        {
          case MONTH -> month;
          case QUARTER_YEAR -> ( ( month - 1 ) / 3 ) * 3 + 1;
          case HALF_YEAR -> month <= 6 ? 1 : 7;
          case YEAR -> 1;
          default -> throw new AssertionError( interval );
        };
        yield toEpochDay( year, firstMonth, 1 );
      }
    };
  }

  static long periodEnd( long storedEpochDay, DateInterval interval )
  {
    return switch ( interval )
    {
      case DAY -> storedEpochDay;
      case WEEK -> Math.addExact( periodStart( storedEpochDay, interval ), 6L );
      case MONTH, QUARTER_YEAR, HALF_YEAR, YEAR -> {
        long ymd = packedYmd( storedEpochDay );
        long year = ymd >> 9;
        int month = (int) ( ( ymd >> 5 ) & 0xFL );
        int lastMonth = switch ( interval )
        {
          case MONTH -> month;
          case QUARTER_YEAR -> ( ( month - 1 ) / 3 ) * 3 + 3;
          case HALF_YEAR -> month <= 6 ? 6 : 12;
          case YEAR -> 12;
          default -> throw new AssertionError( interval );
        };
        yield toEpochDay( year, lastMonth, monthLength( checkedYear( year ), lastMonth ) );
      }
    };
  }

  static boolean isLeapYear( int year )
  {
    return ( year % 4 == 0 && year % 100 != 0 ) || year % 400 == 0;
  }

  static int monthLength( int year, int month )
  {
    return switch ( month )
    {
      case 2 -> isLeapYear( year ) ? 29 : 28;
      case 4, 6, 9, 11 -> 30;
      case 1, 3, 5, 7, 8, 10, 12 -> 31;
      default -> throw new DateTimeException( "Invalid month: " + month );
    };
  }

  private static int checkedYear( long year )
  {
    if ( year < Integer.MIN_VALUE || year > Integer.MAX_VALUE )
      throw new ArithmeticException( "Year outside supported calculation range: " + year );
    return (int) year;
  }

  private static long toEpochDay( long year, int month, int day )
  {
    // January and February belong to the preceding March-based computational year.
    long adjustedYear = month <= 2 ? Math.subtractExact( year, 1L ) : year;
    long era = Math.floorDiv( adjustedYear, 400L );
    long yearOfEra = adjustedYear - era * 400L;
    int monthPrime = month > 2 ? month - 3 : month + 9;
    long dayOfYear = ( 153L * monthPrime + 2L ) / 5L + day - 1L;
    long dayOfEra = yearOfEra * 365L + yearOfEra / 4L - yearOfEra / 100L + dayOfYear;
    return Math.subtractExact( Math.addExact( Math.multiplyExact( era, DAYS_PER_ERA ), dayOfEra ), DAYS_0000_TO_1970 );
  }
}
