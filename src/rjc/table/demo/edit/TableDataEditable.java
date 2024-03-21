/**************************************************************************
 *  Copyright (C) 2024 by Richard Crook                                   *
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

import rjc.table.data.IDataReorderRows;
import rjc.table.data.TableData;
import rjc.table.demo.edit.RowData.Column;

/*************************************************************************************************/
/******************** Example customised table data source for editable table ********************/
/*************************************************************************************************/

public class TableDataEditable extends TableData implements IDataReorderRows
{
  private final int          COLUMN_COUNT = Column.MAX.ordinal();
  private final int          ROW_COUNT    = 30;

  private ArrayList<RowData> m_rows       = new ArrayList<>();

  /**************************************** constructor ******************************************/
  public TableDataEditable()
  {
    // populate the private variables with table contents
    setColumnCount( COLUMN_COUNT );
    setRowCount( ROW_COUNT );

    for ( int row = 0; row < ROW_COUNT; row++ )
      m_rows.add( new RowData( row ) );
  }

  /****************************************** getValue *******************************************/
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

    // return cell value for specified cell index
    return m_rows.get( dataRow ).getValue( dataColumn );
  }
}
