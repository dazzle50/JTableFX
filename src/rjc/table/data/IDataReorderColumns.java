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

import java.util.Set;

/*************************************************************************************************/
/************************ Interface for reordering columns in table data *************************/
/*************************************************************************************************/

/**  
 * Enables data-level column reordering operations for table data models.
 * <p>
 * This interface provides an extension point for {@link TableData} implementations
 * that support column reordering at the data-level within the data model.
 *   
 * @see IDataReorderRows  
 */
public interface IDataReorderColumns
{
  /*************************************** reorderColumns ****************************************/
  /**  
   * Reorders the specified columns by moving them to a new insert position.  
   * <p>  
   * This method should move the columns specified by {@code fromIndexes} to the  
   * position indicated by {@code insertIndex}. The implementation should handle  
   * the reordering logic and update the underlying data model accordingly.  
   *   
   * @param fromIndexes a set of column indexes to be moved (data-based indexes, not view-based)  
   * @param insertIndex the target position where the columns should be inserted  
   * @return {@code true} if the reordering was successful and resulted in a different  
   *         column order, {@code false} if the operation failed or resulted in no change  
   * @throws Exceptions if invalid input parameters are provided  
   * @implNote The default implementation returns {@code false} (no operation performed)
   */
  default public boolean reorderColumns( Set<Integer> fromIndexes, int insertIndex )
  {
    // return if reordering successful (and resulted in different column order)
    return false;
  }
}