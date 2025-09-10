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

/*************************************************************************************************/
/************************* Interface for inserting columns in table data *************************/
/*************************************************************************************************/

/**  
 * Enables data-level column insertion operations for table data models.
 * <p>
 * This interface provides an extension point for {@link TableData} implementations
 * that support column insertion at the data-level within the data model.
 *   
 * @see IDataInsertRows  
 */
public interface IDataInsertColumns
{
  /*************************************** insertColumns *****************************************/
  /**  
   * Inserts the specified number of new 'default' columns at the given position.  
   * <p>  
   * This method should insert {@code count} new columns at the position specified by  
   * {@code insertIndex}. The implementation should handle the insertion logic and  
   * update all related data structures accordingly.  
   *   
   * @param insertIndex the position where new columns should be inserted (data-based index, not view-based)  
   * @param count the number of columns to insert  
   * @return {@code true} if the insertion was successful and columns were added,  
   *         {@code false} if the operation failed or no columns were inserted  
   * @throws Exceptions if invalid input parameters are provided  
   * @implNote The default implementation returns {@code false} (no operation performed)
   */
  default public boolean insertColumns( int insertIndex, int count )
  {
    // return if insertion successful (and columns were added)
    return false;
  }

  /*************************************** insertColumns *****************************************/
  /**  
   * Inserts new columns with the specified data at the given position.  
   * <p>  
   * This method should insert new columns at the position specified by {@code insertIndex}  
   * using the provided column data. The number of columns inserted equals the size of the  
   * {@code columnData} list. The implementation should handle the insertion logic and  
   * update all related data structures accordingly.  
   *   
   * @param insertIndex the position where new columns should be inserted (data-based index, not view-based)  
   * @param columnData a list of objects representing the data for each column to insert  
   * @return {@code true} if the insertion was successful and columns were added,  
   *         {@code false} if the operation failed or no columns were inserted  
   * @throws Exceptions if invalid input parameters are provided  
   * @implNote The default implementation returns {@code false} (no operation performed)
   */
  default public boolean insertColumns( int insertIndex, List<Object> columnData )
  {
    // return if insertion successful (and columns were added)
    return false;
  }
}