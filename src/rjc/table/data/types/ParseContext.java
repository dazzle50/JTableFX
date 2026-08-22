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
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

/*************************************************************************************************/
/******************************* Parser reference snapshot ***************************************/
/*************************************************************************************************/

/** Immutable reference date-time and locale shared by every strategy in one intelligent parse. */
record ParseContext( LocalDateTime reference, Locale locale )
{
  ParseContext
  {
    Objects.requireNonNull( reference, "reference" );
    Objects.requireNonNull( locale, "locale" );
  }

  static ParseContext capture( Clock clock, Locale locale )
  {
    Objects.requireNonNull( clock, "clock" );
    Objects.requireNonNull( locale, "locale" );
    // Read the instant exactly once so a parse cannot straddle a clock tick or midnight.
    return new ParseContext( LocalDateTime.ofInstant( clock.instant(), clock.getZone() ), locale );
  }
}
