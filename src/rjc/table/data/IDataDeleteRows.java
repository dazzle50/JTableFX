/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
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

import java.util.List;
import java.util.Set;

/*************************************************************************************************/
/*************************** Interface for deleting rows in table data ***************************/
/*************************************************************************************************/

/**  
 * Enables data-level row deletion operations for table data models.
 * <p>
 * This interface provides an extension point for {@link TableData} implementations
 * that support row deletion at the data-level within the data model.
 *   
 * @see IDataDeleteColumns  
 */
public interface IDataDeleteRows
{
  /***************************************** deleteRows ******************************************/
  /**  
   * Deletes the specified rows from the table data model.  
   * <p>  
   * This method should remove the rows specified by {@code rowIndexes} from  
   * the underlying data model. The implementation should handle the deletion logic  
   * and update all related data structures accordingly.  
   *   
   * @param rowIndexes a set of row indexes to be deleted (data-based indexes, not view-based)  
   * @return a list containing the data of the deleted rows if the deletion was successful,  
   *         or {@code null} if the operation failed or no rows were deleted  
   * @throws Exceptions if invalid input parameters are provided  
   * @implNote The default implementation returns {@code null} (no operation performed)
   */
  default public List<Object> deleteRows( Set<Integer> rowIndexes )
  {
    // return null if deletion failed or no rows were deleted
    return null;
  }
}