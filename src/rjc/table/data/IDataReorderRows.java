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

package rjc.table.data;

import java.util.HashSet;

import rjc.table.view.axis.AxisBase;

/*************************************************************************************************/
/************************** Interface for reordering rows in table data **************************/
/*************************************************************************************************/

public interface IDataReorderRows
{
  /***************************************** reorderRows *****************************************/
  default public int reorderRows( HashSet<Integer> fromIndexes, int insertIndex )
  {
    // return start index of moved columns if reordering successful, otherwise return INVALID
    return AxisBase.INVALID;
  }
}