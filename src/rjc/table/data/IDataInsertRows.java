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
/************************** Interface for inserting rows in table data ***************************/
/*************************************************************************************************/

/**  
 * Enables data-level row insertion operations for table data models.
 * <p>
 * This interface provides an extension point for {@link TableData} implementations
 * that support row insertion at the data-level within the data model.
 *   
 * @see IDataInsertColumns  
 */
public interface IDataInsertRows
{
  /**************************************** insertRows *****************************************/
  /**  
   * Inserts the specified number of new 'default' rows at the given position.  
   * <p>  
   * This method should insert {@code count} new rows at the position specified by  
   * {@code insertIndex}. The implementation should handle the insertion logic and  
   * update all related data structures accordingly.  
   *   
   * @param insertIndex the position where new rows should be inserted (data-based index, not view-based)  
   * @param count the number of rows to insert  
   * @return {@code true} if the insertion was successful and rows were added,  
   *         {@code false} if the operation failed or no rows were inserted  
   * @throws Exceptions if invalid input parameters are provided  
   * @implNote The default implementation returns {@code false} (no operation performed)
   */
  default public boolean insertRows( int insertIndex, int count )
  {
    // return if insertion successful (and rows were added)
    return false;
  }

  /**************************************** insertRows *****************************************/
  /**  
   * Inserts new rows with the specified data at the given position.  
   * <p>  
   * This method should insert new rows at the position specified by {@code insertIndex}  
   * using the provided row data. The number of rows inserted equals the size of the  
   * {@code rowData} list. The implementation should handle the insertion logic and  
   * update all related data structures accordingly.  
   *   
   * @param insertIndex the position where new rows should be inserted (data-based index, not view-based)  
   * @param rowData a list of objects representing the data for each row to insert  
   * @return {@code true} if the insertion was successful and rows were added,  
   *         {@code false} if the operation failed or no rows were inserted  
   * @throws Exceptions if invalid input parameters are provided  
   * @implNote The default implementation returns {@code false} (no operation performed)
   */
  default public boolean insertRows( int insertIndex, List<Object> rowData )
  {
    // return if insertion successful (and rows were added)
    return false;
  }
}