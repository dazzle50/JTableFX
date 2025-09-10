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
/************************* Interface for deleting columns in table data **************************/
/*************************************************************************************************/

/**  
 * Enables data-level column deletion operations for table data models.
 * <p>
 * This interface provides an extension point for {@link TableData} implementations
 * that support column deletion at the data-level within the data model.
 *   
 * @see IDataDeleteRows  
 */
public interface IDataDeleteColumns
{
  /*************************************** deleteColumns *****************************************/
  /**  
   * Deletes the specified columns from the table data model.  
   * <p>  
   * This method should remove the columns specified by {@code columnIndexes} from  
   * the underlying data model. The implementation should handle the deletion logic  
   * and update all related data structures accordingly.  
   *   
   * @param columnIndexes a set of column indexes to be deleted (data-based indexes, not view-based)  
   * @return a list containing the data of the deleted columns if the deletion was successful,  
   *         or {@code null} if the operation failed or no columns were deleted  
   * @throws Exceptions if invalid input parameters are provided  
   * @implNote The default implementation returns {@code null} (no operation performed)
   */
  default public List<Object> deleteColumns( Set<Integer> columnIndexes )
  {
    // return null if deletion failed or no columns were deleted
    return null;
  }
}