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

package rjc.table.data;

import rjc.table.data.types.Date;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/******************* Utility for comparing heterogeneous (mixed typed) objects *******************/
/*************************************************************************************************/

/**
 * Utility class for comparing heterogeneous objects with a consistent type hierarchy.
 * <p>
 * Implements a stable ordering across different object types:
 * null &lt; Boolean &lt; Number &lt; Character &lt; Time &lt; Date/DateTime &lt; String &lt; Enum &lt; other objects
 * <p>
 * Within each type category, objects are compared using their natural ordering.
 * Objects of unrecognised types are ordered by class name, then identity hash code.
 * <p>
 * <strong>Note on Date/DateTime comparison:</strong> When comparing Date objects with DateTime objects,
 * Date values are treated as representing midnight (00:00:00.000) on that date. This means a Date
 * for "2026-01-26" will be considered less than a DateTime for "2026-01-26 00:00:00.001".
 * <p>
 * <strong>Note on Enum comparison:</strong> Enums are compared using their {@code toString()} representation.
 * If enums have custom {@code toString()} implementations, the ordering may differ from declaration order.
 */
public class GenericComparator
{
  /**
   * Type categories for comparison ordering.
   */
  private enum TypeCategory
  {
    NULL, BOOLEAN, NUMBER, CHARACTER, TIME, DATE_TIME, STRING, ENUM, OTHER
  }

  /***************************************** categorise ******************************************/
  /**
   * Determines the type category of an object for comparison ordering.
   * <p>
   * Maps each object to one of the predefined type categories used to establish
   * the comparison hierarchy. This categorisation ensures consistent ordering
   * when comparing objects of different types.
   *
   * @param obj the object to categorise (may be null)
   * @return the type category corresponding to the object's runtime type
   */
  private static TypeCategory categorise( Object obj )
  {
    return switch ( obj )
    {
      case null -> TypeCategory.NULL;
      case Boolean _ -> TypeCategory.BOOLEAN;
      case Number _ -> TypeCategory.NUMBER;
      case Character _ -> TypeCategory.CHARACTER;
      case Time _ -> TypeCategory.TIME;
      case Date _ -> TypeCategory.DATE_TIME;
      case DateTime _ -> TypeCategory.DATE_TIME;
      case String _ -> TypeCategory.STRING;
      case Enum<?> _ -> TypeCategory.ENUM;
      default -> TypeCategory.OTHER;
    };
  }

  /******************************************* compare *******************************************/
  /**
   * Compares two objects with support for heterogeneous types.
   * <p>
   * Returns a negative integer, zero, or positive integer as the first argument
   * is less than, equal to, or greater than the second.
   * <p>
   * Type ordering hierarchy (lower types are "less than" higher types):
   * <ol>
   *   <li>null values (always least)</li>
   *   <li>Booleans (false &lt; true)</li>
   *   <li>Numbers (compared by double value)</li>
   *   <li>Characters (compared by char value)</li>
   *   <li>Times (compared by milliseconds)</li>
   *   <li>Dates and DateTimes (compared by epoch milliseconds, Date treated as midnight)</li>
   *   <li>Strings (compared lexicographically)</li>
   *   <li>Enums (compared by toString() representation)</li>
   *   <li>Other objects (compared by class name, then identity hash)</li>
   * </ol>
   * <p>
   * For objects in the "other" category that implement {@code Comparable} and share
   * the same runtime class, their natural ordering is used.
   *
   * @param obj1 the first object to compare (may be null)
   * @param obj2 the second object to compare (may be null)
   * @return negative if obj1 &lt; obj2, zero if obj1 == obj2, positive if obj1 &gt; obj2
   */
  public static int compare( Object obj1, Object obj2 )
  {
    // categorise both objects to determine their type hierarchy positions
    TypeCategory cat1 = categorise( obj1 );
    TypeCategory cat2 = categorise( obj2 );

    // if different categories, order by category hierarchy
    if ( cat1 != cat2 )
      return Integer.compare( cat1.ordinal(), cat2.ordinal() );

    // same category - compare within the category using appropriate logic
    return switch ( cat1 )
    {
      case NULL -> 0; // both null, equal

      case BOOLEAN -> Boolean.compare( (Boolean) obj1, (Boolean) obj2 );

      case NUMBER -> Double.compare( ( (Number) obj1 ).doubleValue(), ( (Number) obj2 ).doubleValue() );

      case CHARACTER -> Character.compare( (Character) obj1, (Character) obj2 );

      case TIME -> ( (Time) obj1 ).compareTo( (Time) obj2 );

      case DATE_TIME -> {
        // convert both to epoch milliseconds for comparison, date are treated as midnight on that day
        long ms1 = ( obj1 instanceof Date date ) ? date.getEpochDay() * (long) Time.MILLIS_PER_DAY
            : ( (DateTime) obj1 ).toMilliseconds();
        long ms2 = ( obj2 instanceof Date date ) ? date.getEpochDay() * (long) Time.MILLIS_PER_DAY
            : ( (DateTime) obj2 ).toMilliseconds();
        yield Long.compare( ms1, ms2 );
      }

      case STRING -> ( (String) obj1 ).compareTo( (String) obj2 );

      case ENUM -> ( (Enum<?>) obj1 ).toString().compareTo( ( (Enum<?>) obj2 ).toString() );

      case OTHER -> {
        // if same class and implements Comparable, use natural ordering
        if ( obj1.getClass() == obj2.getClass() && obj1 instanceof Comparable<?> comp )
          yield compareSameType( comp, obj2 );

        // otherwise order by class name for stability
        int classComp = obj1.getClass().getName().compareTo( obj2.getClass().getName() );
        if ( classComp != 0 )
          yield classComp;

        // finally if same class name, order by identity hash code to ensure consistent ordering
        yield Integer.compare( System.identityHashCode( obj1 ), System.identityHashCode( obj2 ) );
      }
    };
  }

  /*************************************** compareSameType ***************************************/
  /**
   * Compares two objects of the same runtime type using their natural ordering.
   * <p>
   * This method safely performs the comparison when both objects share the same class
   * and implement {@code Comparable}. The unchecked cast is safe because the caller
   * guarantees (via class equality check) that both objects have identical runtime types,
   * ensuring the cast and subsequent {@code compareTo()} call are type-safe.
   *
   * @param <T>  the type of objects being compared
   * @param obj1 the first object, already verified to be Comparable
   * @param obj2 the second object, guaranteed by caller to have same class as obj1
   * @return negative if obj1 &lt; obj2, zero if equal, positive if obj1 &gt; obj2
   */
  @SuppressWarnings( "unchecked" )
  private static <T> int compareSameType( Comparable<T> obj1, Object obj2 )
  {
    // cast is safe because caller has verified obj1.getClass() == obj2.getClass()
    return obj1.compareTo( (T) obj2 );
  }

}