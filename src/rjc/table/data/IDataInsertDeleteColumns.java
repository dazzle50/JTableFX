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

import java.util.List;

/*************************************************************************************************/
/****************** Interface for inserting and deleting columns in table data *******************/
/*************************************************************************************************/

/**
 * Enables data-level column insertion and deletion for table data models.
 * <p>
 * Each element in a column data list represents one complete column's worth of data; the internal
 * structure of each element is determined by the data-model implementation.
 * <p>
 * The two methods are designed to be symmetric: data returned by {@link #deleteColumns} may be
 * passed directly to {@link #insertColumns} at the same index to undo the deletion.
 *
 * @see IDataInsertDeleteRows
 */
public interface IDataInsertDeleteColumns
{
  /*************************************** insertColumns *****************************************/
  /**
   * Inserts one or more columns at the specified position using the provided column data.
   * <p>
   * The number of columns inserted equals {@code columnData.size()}. Each element in
   * {@code columnData} represents one complete column's data as defined by the data-model
   * implementation. If any element within the list is {@code null}, that column should be
   * populated with model-determined default content.
   * <p>
   * Columns at and after {@code insertIndex} are shifted right to accommodate the new columns.
   * <p>
   * To undo a previous {@link #deleteColumns} call, pass its returned list to this method
   * using the same index.
   *
   * @param insertIndex  data-based index at which to insert; must be &gt;= 0
   * @param columnData   non-null list whose elements each represent one complete column's data to
   *                     insert; an empty list results in no columns being inserted; individual
   *                     elements may be {@code null} to request model-determined default content
   *                     for that column
   * @return {@code true} if at least one column was successfully inserted, {@code false} otherwise
   * @throws Exceptions if invalid input parameters are provided
   * @implNote The default implementation returns {@code false} (no operation performed).
   */
  default boolean insertColumns( int insertIndex, List<Object> columnData )
  {
    // default: no-op — override to provide insertion behaviour
    return false;
  }

  /*************************************** deleteColumns *****************************************/
  /**
   * Deletes a contiguous range of columns from the table data model.
   * <p>
   * Each element in the returned list represents one complete column's data in the same form
   * accepted by {@link #insertColumns}, preserving the undo round-trip contract of this interface.
   *
   * @param deleteIndex  data-based index of the first column to delete; must be &gt;= 0
   * @param count        number of consecutive columns to delete; must be &gt; 0
   * @return a list of the deleted columns' data if deletion was successful, or {@code null}
   *         if the operation failed or no columns were deleted
   * @throws Exceptions if invalid input parameters are provided
   * @implNote The default implementation returns {@code null} (no operation performed).
   */
  default List<Object> deleteColumns( int deleteIndex, int count )
  {
    // default: no-op — override to provide deletion behaviour
    return null;
  }
}