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
/******************************** Fixed-duration intervals ***************************************/
/*************************************************************************************************/

/**
 * Fixed-duration arithmetic units and alignment grids within a stored day. Every duration divides
 * a 24-hour day exactly, with alignment measured from {@code 00:00}.
 */
public enum TimeInterval implements DateTimeInterval
{
  /** One second. */
  SECOND( Time.MILLIS_PER_SECOND ),
  /** One minute. */
  MINUTE( Time.MILLIS_PER_MINUTE ),
  /** Ten minutes. */
  TEN_MINUTE( 10L * Time.MILLIS_PER_MINUTE ),
  /** Thirty minutes. */
  HALF_HOUR( 30L * Time.MILLIS_PER_MINUTE ),
  /** One hour. */
  HOUR( Time.MILLIS_PER_HOUR ),
  /** Six hours. */
  QUARTER_DAY( 6L * Time.MILLIS_PER_HOUR ),
  /** Twelve hours. */
  HALF_DAY( 12L * Time.MILLIS_PER_HOUR );

  private final long milliseconds;

  TimeInterval( long milliseconds )
  {
    this.milliseconds = milliseconds;
  }

  /**
   * Returns the exact fixed duration represented by this interval.
   *
   * @return interval duration in milliseconds
   */
  public long milliseconds()
  {
    return milliseconds;
  }
}
