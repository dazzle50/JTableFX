/**************************************************************************
 *  Copyright (C) 2024 by Richard Crook                                   *
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

public interface IOverflowField
{
  /***************************************** changeValue *****************************************/
  // change value by delta overflowing to overflow-field if available
  public void changeValue( double delta );

  /***************************************** changeValue *****************************************/
  // change value by delta/shift/ctrl overflowing to overflow-field if available
  default public void changeValue( double delta, boolean shift, boolean ctrl, boolean alt )
  {
    // default behaviour is to ignore shift/ctrl
    changeValue( delta );
  }

}