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

/*************************************************************************************************/
/********************************** Calendar intervals *******************************************/
/*************************************************************************************************/

/**
 * Locale-independent proleptic-Gregorian calendar intervals used for calendar arithmetic and
 * containing-period boundaries. {@link #WEEK} is always an ISO Monday-through-Sunday week.
 */
public enum DateInterval implements DateTimeInterval
{
  /** One calendar day. */
  DAY,
  /** One ISO week, beginning Monday and ending Sunday. */
  WEEK,
  /** One calendar month. */
  MONTH,
  /** One three-month calendar quarter. */
  QUARTER_YEAR,
  /** One six-month calendar half-year. */
  HALF_YEAR,
  /** One calendar year. */
  YEAR
}
