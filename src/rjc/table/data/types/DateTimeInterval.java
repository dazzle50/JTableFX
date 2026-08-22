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
/******************************* DateTime interval marker ****************************************/
/*************************************************************************************************/

/**
 * Closed union of the interval types accepted by {@link DateTime#plus(long, DateTimeInterval)},
 * {@link DateTime#startOf(DateTimeInterval)}, and {@link DateTime#endOf(DateTimeInterval)}.
 * {@link DateInterval} values select calendar arithmetic and containing periods;
 * {@link TimeInterval} values select fixed-duration arithmetic and within-day alignment grids.
 */
public sealed interface DateTimeInterval permits DateInterval, TimeInterval
{
}
