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
/******************* Interface for inserting and deleting rows in table data *********************/
/*************************************************************************************************/

/**
 * Enables data-level row insertion and deletion for table data models.
 * <p>
 * Each element in a row data list represents one complete row's worth of data; the internal
 * structure of each element is determined by the data-model implementation.
 * <p>
 * The two methods are designed to be symmetric: data returned by {@link #deleteRows} may be
 * passed directly to {@link #insertRows} at the same index to undo the deletion.
 *
 * @see IDataInsertDeleteColumns
 */
public interface IDataInsertDeleteRows
{
  /* **************************************** insertRows ******************************************/
  /**
   * Inserts one or more rows at the specified position using the provided row data.
   * <p>
   * The number of rows inserted equals {@code rowData.size()}. Each element in {@code rowData}
   * represents one complete row's data as defined by the data-model implementation. If any element
   * within the list is {@code null}, that row should be populated with model-determined default
   * content.
   * <p>
   * Rows at and after {@code insertIndex} are shifted down to accommodate the new rows.
   * <p>
   * To undo a previous {@link #deleteRows} call, pass its returned list to this method using
   * the same index.
   *
   * @param insertIndex  data-based index at which to insert; must be &gt;= 0
   * @param rowData      non-null list whose elements each represent one complete row's data to
   *                     insert; an empty list results in no rows being inserted; individual
   *                     elements may be {@code null} to request model-determined default content
   *                     for that row
   * @return {@code true} if at least one row was successfully inserted, {@code false} otherwise
   * @throws Exceptions if invalid input parameters are provided
   * @implNote The default implementation returns {@code false} (no operation performed).
   */
  default boolean insertRows( int insertIndex, List<Object> rowData )
  {
    // default: no-op — override to provide insertion behaviour
    return false;
  }

  /* **************************************** deleteRows ******************************************/
  /**
   * Deletes a contiguous range of rows from the table data model.
   * <p>
   * Each element in the returned list represents one complete row's data in the same form
   * accepted by {@link #insertRows}, preserving the undo round-trip contract of this interface.
   *
   * @param deleteIndex  data-based index of the first row to delete; must be &gt;= 0
   * @param count        number of consecutive rows to delete; must be &gt; 0
   * @return a list of the deleted rows' data if deletion was successful, or {@code null}
   *         if the operation failed or no rows were deleted
   * @throws Exceptions if invalid input parameters are provided
   * @implNote The default implementation returns {@code null} (no operation performed).
   */
  default List<Object> deleteRows( int deleteIndex, int count )
  {
    // default: no-op — override to provide deletion behaviour
    return null;
  }
}