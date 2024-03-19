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

package rjc.table.data;

import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableInteger;
import rjc.table.signal.ObservableInteger.ReadOnlyInteger;

/*************************************************************************************************/
/************** Table data source, column & row counts, signals to announce changes **************/
/*************************************************************************************************/

public class TableData implements ISignal
{
  // observable integers for table body column & row counts
  private ObservableInteger m_columnCount = new ObservableInteger( 3 );
  private ObservableInteger m_rowCount    = new ObservableInteger( 10 );

  public enum Signal
  {
    CELL_VALUE_CHANGED, ROW_VALUES_CHANGED, COLUMN_VALUES_CHANGED, TABLE_VALUES_CHANGED
  }

  // column & row index starts at 0 for table body, index of -1 is for header
  final static public int HEADER = -1;

  /*************************************** getColumnCount ****************************************/
  public int getColumnCount()
  {
    // return number of columns in table body
    return m_columnCount.get();
  }

  /*************************************** setColumnCount ****************************************/
  public void setColumnCount( int columnCount )
  {
    // set number of columns in table body
    m_columnCount.set( columnCount );
  }

  /**************************************** getRowCount ******************************************/
  public int getRowCount()
  {
    // return number of rows in table body
    return m_rowCount.get();
  }

  /**************************************** setRowCount ******************************************/
  public void setRowCount( int rowCount )
  {
    // set number of rows in table body
    m_rowCount.set( rowCount );
  }

  /************************************* columnCountProperty *************************************/
  public ReadOnlyInteger columnCountProperty()
  {
    // return read-only property for column count
    return m_columnCount.getReadOnly();
  }

  /************************************** rowCountProperty ***************************************/
  public ReadOnlyInteger rowCountProperty()
  {
    // return read-only property for row count
    return m_rowCount.getReadOnly();
  }

  /****************************************** getValue *******************************************/
  public Object getValue( int dataColumn, int dataRow )
  {
    // return header corner cell value
    if ( dataColumn == HEADER && dataRow == HEADER )
      return "-";

    // return row value for specified row index
    if ( dataColumn == HEADER )
      return "R" + dataRow;

    // return column value for specified column index
    if ( dataRow == HEADER )
      return "C" + dataColumn;

    // return cell value for specified cell index
    return "{" + dataColumn + "," + dataRow + "}";
  }

  /****************************************** setValue *******************************************/
  public String setValue( int dataColumn, int dataRow, Object newValue, Boolean... check )
  {
    // returns null if cell value successfully set to new-value for specified cell index
    // returns non-null reason description string if failed to set cell value
    // if optional check is true, cell value not set but still validated and returns null or reason
    return "Not implemented";
  }

  /************************************** signalCellChanged **************************************/
  public void signalCellChanged( int dataColumn, int dataRow )
  {
    // signal that a table cell value has changed (usually to trigger cell redraw)
    signal( Signal.CELL_VALUE_CHANGED, dataColumn, dataRow );
  }

  /************************************* signalColumnChanged *************************************/
  public void signalColumnChanged( int dataColumn )
  {
    // signal that table column values have changed (usually to trigger column redraw)
    signal( Signal.COLUMN_VALUES_CHANGED, dataColumn );
  }

  /*************************************** signalRowChanged **************************************/
  public void signalRowChanged( int dataRow )
  {
    // signal that table row values have changed (usually to trigger row redraw)
    signal( Signal.ROW_VALUES_CHANGED, dataRow );
  }

  /************************************** signalTableChanged *************************************/
  public void signalTableChanged()
  {
    // signal that table values have changed (usually to trigger table redraw)
    signal( Signal.TABLE_VALUES_CHANGED );
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return as string
    return getClass().getSimpleName() + "@" + Integer.toHexString( System.identityHashCode( this ) ) + "[m_columnCount="
        + m_columnCount + " m_rowCount=" + m_rowCount + "]";
  }
}