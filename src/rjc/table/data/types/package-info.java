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

/**
 * Compact, millisecond-precision, zone-free date and time values.
 *
 * <p>{@link rjc.table.data.types.DateTime} and {@link rjc.table.data.types.Time} preserve the
 * explicit stored endpoint notation {@code 24:00}. For DateTime, stored equality, hashing, natural
 * ordering, ordering predicates, and calendar arithmetic use the stored date and time without
 * normalization. Calendar boundaries use the stored date and return explicit {@code 00:00} or
 * {@code 24:00} endpoints. Serial conversion, elapsed differences, Java time conversion, and
 * {@link rjc.table.data.types.DateTime#hasSameTimelinePosition(DateTime)} use its normalized
 * following-day-midnight position. Time remains a linear stored endpoint value: its {@code 00:00}
 * and {@code 24:00} endpoints are distinct even though conversion to {@link java.time.LocalTime}
 * is lossy.</p>
 *
 * <p>{@link rjc.table.data.types.DateInterval} and
 * {@link rjc.table.data.types.TimeInterval} prevent invalid calendar/clock interval combinations.
 * Calendar arithmetic is checked and never wraps compact epoch-day storage. Time arithmetic wraps
 * deliberately and reduces large amounts before multiplication.</p>
 *
 * <p>Pattern-string formatting uses the current default locale and adds the locale-independent
 * {@code U}, {@code UU}, and {@code UUU} half-year tokens for date fields. Pattern and compatible
 * formatter-object methods retain stored {@code 24:00}; zone, offset, instant, and other fields
 * absent from a value are unsupported.</p>
 *
 * <p>Required reference arguments are rejected with {@link java.lang.NullPointerException}; normal
 * {@code equals(null)} behavior is the exception. Invalid calendar or clock values use
 * {@link java.time.DateTimeException}, compact-storage or arithmetic overflow uses
 * {@link java.lang.ArithmeticException}, malformed patterns use
 * {@link java.lang.IllegalArgumentException}, and valid patterns whose input cannot resolve use
 * {@link java.time.format.DateTimeParseException}. Formatting with an unsupported field uses
 * {@code DateTimeException}, normally {@link java.time.temporal.UnsupportedTemporalTypeException}.</p>
 *
 * <p>The value types remain serializable for newly written data. Serialized forms from releases
 * predating this breaking API revision are not supported.</p>
 */

package rjc.table.data.types;
