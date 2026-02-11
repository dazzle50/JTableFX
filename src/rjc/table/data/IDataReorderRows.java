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
/*************************** Interface for reordering rows in table data *************************/
/*************************************************************************************************/

/**  
 * Provides interface for {@link TableData} implementations that support row
 * reordering including sorting at the data-level within the data model.
 *   
 * @see IDataReorderColumns  
 */
public interface IDataReorderRows
{
  /****************************************** swapRows *******************************************/
  /**
   * Swaps the position of two rows in the table model at data-level.
   * This method is typically used during reordering and sorting operations.
   * 
   * @param row1 index of the first row to swap
   * @param row2 index of the second row to swap
   * @return {@code true} if the swap was successful and resulted in a different row order,
   *         {@code false} otherwise
   */
  default public boolean swapRows( int row1, int row2 )
  {
    return false;
  }

}