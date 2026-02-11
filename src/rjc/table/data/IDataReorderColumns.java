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

/*************************************************************************************************/
/************************ Interface for reordering columns in table data *************************/
/*************************************************************************************************/

/**  
 * Provides interface for {@link TableData} implementations that support column
 * reordering including sorting at the data-level within the data model.
 *   
 * @see IDataReorderRows  
 */
public interface IDataReorderColumns
{
  /***************************************** swapColumns *****************************************/
  /**
   * Swaps the position of two columns in the table model at data-level.
   * This method is typically used during reordering and sorting operations.
   * 
   * @param column1 index of the first column to swap
   * @param column2 index of the second column to swap
   * @return {@code true} if the swap was successful and resulted in a different column order,
   *         {@code false} otherwise
   */
  default public boolean swapColumns( int column1, int column2 )
  {
    return false;
  }

}