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

package rjc.table.control;

/*************************************************************************************************/
/************************** Interface for fields that support overflow ***************************/
/*************************************************************************************************/

/**
 * Field contract for controls whose value can be stepped by a delta.
 * <p>
 * Implementations may use this for simple stepping, or may pass overflow to another field when
 * their own value wraps beyond its range. Modifier flags allow callers to request larger or
 * alternate step behaviour.
 */
public interface IOverflowField
{
  /***************************************** changeValue *****************************************/
  /**
   * Changes the field value by the supplied delta with no modifier-key behaviour.
   *
   * @param delta amount to add to the current value
   */
  default void changeValue( double delta )
  {
    // change value assuming no shift/ctrl/alt behaviour
    changeValue( delta, false, false, false );
  }

  /***************************************** changeValue *****************************************/
  /**
   * Changes the field value by the supplied delta, optionally using modifier-key behaviour.
   *
   * @param delta amount to add to the current value
   * @param shift whether Shift was held for this change
   * @param ctrl  whether Control was held for this change
   * @param alt   whether Alt was held for this change
   */
  default void changeValue( double delta, boolean shift, boolean ctrl, boolean alt )
  {
    // default behaviour is to ignore shift/ctrl/alt
    changeValue( delta );
  }

}