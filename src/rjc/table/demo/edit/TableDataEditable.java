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

package rjc.table.demo.edit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rjc.table.data.IDataInsertDeleteRows;
import rjc.table.data.IDataSwapRows;
import rjc.table.data.TableData;
import rjc.table.demo.edit.EditableData.Column;

/*************************************************************************************************/
/******************** Example customised table data source for editable table ********************/
/*************************************************************************************************/

/**
 * Editable table data source supporting row reordering, insertion, and deletion.
 */
public class TableDataEditable extends TableData implements IDataSwapRows, IDataInsertDeleteRows
{
  private static final int   COLUMN_COUNT = Column.MAX.ordinal();
  private static final int   ROW_COUNT    = 10;

  private List<EditableData> m_rows       = new ArrayList<>();

  /**************************************** constructor ******************************************/
  /**
   * Initialises the table data with {@code ROW_COUNT} rows of default {@link EditableData}.
   */
  public TableDataEditable()
  {
    // populate the private variables with table contents
    setColumnCount( COLUMN_COUNT );
    setRowCount( ROW_COUNT );

    for ( int row = 0; row < ROW_COUNT; row++ )
      m_rows.add( new EditableData( row ) );
  }

  /****************************************** getValue *******************************************/
  /**
   * Returns the value for the specified data column and row indices.
   *
   * @param dataColumn  the data-based column index, or {@code HEADER}
   * @param dataRow     the data-based row index, or {@code HEADER}
   * @return the cell value, or {@code null} for the header corner
   */
  @Override
  public Object getValue( int dataColumn, int dataRow )
  {
    // return header corner cell value
    if ( dataColumn == HEADER && dataRow == HEADER )
      return null;

    // return row value for specified row index
    if ( dataColumn == HEADER )
      return String.valueOf( dataRow + 1 );

    // return column value for specified column index
    if ( dataRow == HEADER )
      return Column.values()[dataColumn];

    // return cell value for body cell
    return m_rows.get( dataRow ).getValue( dataColumn );
  }

  /****************************************** setValue *******************************************/
  /**
   * Attempts to set the value for the specified data column and row indices.
   *
   * @param dataColumn  the data-based column index
   * @param dataRow     the data-based row index
   * @param newValue    the new value to apply
   * @param commit      {@code true} to commit the change, {@code false} to validate only
   * @return {@code null} on success, or an error message string if the value is invalid
   */
  @Override
  protected String setValue( int dataColumn, int dataRow, Object newValue, boolean commit )
  {
    // delegate validation and assignment to the row data object
    return m_rows.get( dataRow ).setValue( dataColumn, newValue, commit );
  }

  /****************************************** swapRows *******************************************/
  /**
   * IDataSwapRows - Swaps two rows in the data model to support row reordering/sorting.
   *
   * @param row1  data-based index of the first row
   * @param row2  data-based index of the second row
   * @return {@code true} always, as the swap cannot fail
   */
  @Override
  public boolean swapRows( int row1, int row2 )
  {
    Collections.swap( m_rows, row1, row2 );
    return true;
  }

  /***************************************** insertRows ******************************************/
  /**
   * IDataInsertDeleteRows - Inserts one or more rows at the specified position.
   *
   * @param insertIndex  data-based index at which to insert; must be &gt;= 0
   * @param rowData      non-null list of row data; {@code null} elements receive default content
   * @return {@code true} if at least one row was inserted, {@code false} if the list is empty
   * @throws IllegalArgumentException if {@code insertIndex} is out of range or
   *                                  {@code rowData} is {@code null}
   */
  @Override
  public boolean insertRows( int insertIndex, List<Object> rowData )
  {
    // build the list of rows to insert, substituting defaults for null elements
    var toInsert = new ArrayList<EditableData>( rowData.size() );
    int count = 0;
    for ( var data : rowData )
      toInsert.add( data == null ? new EditableData() : (EditableData) data );

    m_rows.addAll( insertIndex, toInsert );
    setRowCount( getRowCount() + toInsert.size() );
    return true;
  }

  /***************************************** deleteRows ******************************************/
  /**
   * IDataInsertDeleteRows - Deletes a contiguous range of rows from the data model.
   *
   * @param deleteIndex  data-based index of the first row to delete; must be &gt;= 0
   * @param count        number of consecutive rows to delete; must be &gt; 0
   * @return a list of the deleted {@link EditableData} rows, or {@code null} if the
   *         operation failed
   * @throws IllegalArgumentException if {@code deleteIndex} is out of range or
   *                                  {@code count} is not positive
   */
  @Override
  public List<Object> deleteRows( int deleteIndex, int count )
  {
    // snapshot the rows being removed before clearing the sublist
    var deleted = new ArrayList<Object>( m_rows.subList( deleteIndex, deleteIndex + count ) );
    m_rows.subList( deleteIndex, deleteIndex + count ).clear();
    setRowCount( getRowCount() - count );
    return deleted;
  }
}