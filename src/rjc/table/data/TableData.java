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

import rjc.table.Utils;
import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableInteger;
import rjc.table.signal.ObservableInteger.ReadOnlyInteger;
import rjc.table.view.cell.CellVisual;

/*************************************************************************************************/
/************** Table data source, column & row counts, signals to announce changes **************/
/*************************************************************************************************/

/**
 * This class serves as the foundation for table data implementations, offering observable properties
 * for table dimensions and a signaling system for coordinating view updates when data changes.
 * 
 * Column and row indices start at 0 for table body cells, with -1 representing header cells.
 */
public class TableData implements ISignal
{
  // observable integers for tracking table body dimensions with automatic change notification
  private final ObservableInteger m_columnCount = new ObservableInteger( 3 );
  private final ObservableInteger m_rowCount    = new ObservableInteger( 10 );

  // optional arbitrary user data storage for application-specific data
  private Object                  m_userData;

  // default cell visual appearance settings applied to all cells
  private CellVisual              m_cellVisual  = new CellVisual();

  // signal types for notifying observers about different scopes of data changes.
  public enum Signal
  {
    CELL_VALUE_CHANGED, ROW_VALUES_CHANGED, COLUMN_VALUES_CHANGED, TABLE_VALUES_CHANGED
  }

  // column & row index starts at 0 for table body, index of -1 is for header
  final static public int HEADER = -1;

  /*************************************** getColumnCount ****************************************/
  /**
   * Gets the number of columns in the table body (excluding header column).
   * 
   * @return the current column count, always non-negative
   */
  final public int getColumnCount()
  {
    // return current column count from observable property
    return m_columnCount.get();
  }

  /*************************************** setColumnCount ****************************************/
  /**
   * Sets the number of columns in the table body, in observable property which can be listened to.
   * 
   * @param columnCount the new column count, must be non-negative
   * @throws IllegalArgumentException if columnCount is negative
   */
  final public void setColumnCount( int columnCount )
  {
    // validate column count is not negative before setting
    if ( columnCount < 0 )
      throw new IllegalArgumentException( "Column count must not be negative " + columnCount );
    m_columnCount.set( columnCount );
  }

  /**************************************** getRowCount ******************************************/
  /**
   * Gets the number of rows in the table body (excluding header row).
   * 
   * @return the current row count, always non-negative
   */
  final public int getRowCount()
  {
    // return current row count from observable property
    return m_rowCount.get();
  }

  /**************************************** setRowCount ******************************************/
  /**
   * Sets the number of rows in the table body, in observable property which can be listened to.
   * 
   * @param rowCount the new row count, must be non-negative
   * @throws IllegalArgumentException if rowCount is negative
   */
  final public void setRowCount( int rowCount )
  {
    // validate row count is not negative before setting
    if ( rowCount < 0 )
      throw new IllegalArgumentException( "Row count must not be negative " + rowCount );
    m_rowCount.set( rowCount );
  }

  /************************************* columnCountProperty *************************************/
  /**
   * Gets a read-only observable property for the column count, that can be listened to.
   * 
   * @return read-only integer property tracking column count changes
   */
  final public ReadOnlyInteger columnCountProperty()
  {
    // return read-only wrapper to prevent external modification
    return m_columnCount.getReadOnly();
  }

  /************************************** rowCountProperty ***************************************/
  /**
   * Gets a read-only observable property for the row count, that can be listened to.
   * 
   * @return read-only integer property tracking row count changes
   */
  final public ReadOnlyInteger rowCountProperty()
  {
    // return read-only wrapper to prevent external modification
    return m_rowCount.getReadOnly();
  }

  /***************************************** getVisual *******************************************/
  /**
   * Gets the visual appearance settings for a specific cell.
   * Override this method to provide non-default styling (fonts, colours, alignment, etc.).
   * 
   * @param dataColumn the column index (0-based for body, HEADER for header column)
   * @param dataRow the row index (0-based for body, HEADER for header row)
   * @return CellVisual object containing appearance settings for the specified cell
   */
  public CellVisual getVisual( int dataColumn, int dataRow )
  {
    // return default visual settings for all cells - override for custom styling
    return m_cellVisual;
  }

  /****************************************** getValue *******************************************/
  /**
   * Gets the display value for a specific cell.
   * This base implementation provides default header labels and placeholder body values.
   * Override this method to return actual data from your data source.
   * 
   * @param dataColumn the column index (0-based for body, HEADER for header column)
   * @param dataRow the row index (0-based for body, HEADER for header row)
   * @return the cell value to display, may be any Object type
   */
  public Object getValue( int dataColumn, int dataRow )
  {
    // return appropriate default value based on cell location
    // header corner cell shows separator
    if ( dataColumn == HEADER && dataRow == HEADER )
      return "-";

    // row header shows row identifier
    if ( dataColumn == HEADER )
      return "R" + dataRow;

    // column header shows column identifier
    if ( dataRow == HEADER )
      return "C" + dataColumn;

    // body cells show coordinate placeholder
    return "{" + dataColumn + "," + dataRow + "}";
  }

  /****************************************** setValue *******************************************/
  /**
   * Attempts to set a new value for the specified cell.
   * This is the primary method for updating cell data with full validation and signaling.
   * 
   * @param dataColumn the column index (must be >= 0 and < columnCount)
   * @param dataRow the row index (must be >= 0 and < rowCount)
   * @param newValue the new value to set, type depends on column data type
   * @return null if successful, otherwise a String describing why the operation failed
   */
  final public String setValue( int dataColumn, int dataRow, Object newValue )
  {
    // delegate to tryValue with commit=true to actually set the value
    return tryValue( dataColumn, dataRow, newValue, true );
  }

  /****************************************** testValue ******************************************/
  /**
   * Tests whether a value could be set for the specified cell without actually setting it.
   * Use this for validation during user input to provide immediate feedback.
   * 
   * @param dataColumn the column index (must be >= 0 and < columnCount)
   * @param dataRow the row index (must be >= 0 and < rowCount)
   * @param newValue the value to test for validity
   * @return null if the value would be accepted, otherwise a String describing why it would be rejected
   */
  final public String testValue( int dataColumn, int dataRow, Object newValue )
  {
    // delegate to tryValue with commit=false to only validate without setting
    return tryValue( dataColumn, dataRow, newValue, false );
  }

  /******************************************* tryValue ******************************************/
  final private String tryValue( int dataColumn, int dataRow, Object newValue, boolean commit )
  {
    // validate cell coordinates are within valid range
    if ( dataColumn <= HEADER || dataColumn >= getColumnCount() || dataRow <= HEADER || dataRow >= getRowCount() )
      return "Cell reference out of range " + dataColumn + " " + dataRow;

    try
    {
      // delegate to override setValue method for actual validation/setting logic
      String decline = setValue( dataColumn, dataRow, newValue, commit );

      // if successful and committing, notify observers that cell has changed
      if ( decline == null && commit )
        signalCellChanged( dataColumn, dataRow );
      return decline;
    }
    catch ( Exception exception )
    {
      // catch any exceptions during processing and return descriptive error message
      String msg = "Error " + dataColumn + " " + dataRow + " " + commit + " ";
      return msg + Utils.objectsString( newValue ) + " : " + exception.toString();
    }
  }

  /****************************************** setValue *******************************************/
  /**
   * Template method for validating and optionally setting cell values.
   * Override this method to implement actual data validation and storage logic.
   * This base implementation always returns "Not implemented" to indicate no data storage is available.
   * 
   * @param dataColumn the column index (guaranteed to be valid when called)
   * @param dataRow the row index (guaranteed to be valid when called)
   * @param newValue the value to validate and potentially set
   * @param commit true to actually set the value, false to only validate
   * @return null if the operation is successful/valid, otherwise a String describing the problem
   */
  protected String setValue( int dataColumn, int dataRow, Object newValue, boolean commit )
  {
    // base implementation has no data storage, so always decline with explanation
    return "Not implemented";
  }

  /************************************** signalCellChanged **************************************/
  /**
   * Notifies observers that a specific cell's value has changed.
   * This triggers view updates for the affected cell.
   * 
   * @param dataColumn the column index of the changed cell
   * @param dataRow the row index of the changed cell
   */
  final public void signalCellChanged( int dataColumn, int dataRow )
  {
    // emit cell-level change signal with coordinates for targeted updates
    signal( Signal.CELL_VALUE_CHANGED, dataColumn, dataRow );
  }

  /************************************* signalColumnChanged *************************************/
  /**
   * Notifies observers that all values in a specific column have changed.
   * This triggers view updates for the entire column.
   * 
   * @param dataColumn the column index that has changed
   */
  final public void signalColumnChanged( int dataColumn )
  {
    // emit column-level change signal for bulk column updates
    signal( Signal.COLUMN_VALUES_CHANGED, dataColumn );
  }

  /*************************************** signalRowChanged **************************************/
  /**
   * Notifies observers that all values in a specific row have changed.
   * This triggers view updates for the entire row.
   * 
   * @param dataRow the row index that has changed
   */
  final public void signalRowChanged( int dataRow )
  {
    // emit row-level change signal for bulk row updates
    signal( Signal.ROW_VALUES_CHANGED, dataRow );
  }

  /************************************** signalTableChanged *************************************/
  /**
   * Notifies observers that multiple or all table values have changed.
   * This triggers a complete view refresh of the entire table view.
   * Use sparingly as it's the most expensive update operation.
   */
  final public void signalTableChanged()
  {
    // emit table-level change signal for complete table refresh
    signal( Signal.TABLE_VALUES_CHANGED );
  }

  /***************************************** setUserData *****************************************/
  /**
   * Stores arbitrary application-specific data associated with this table.
   * This provides a convenient way to attach custom metadata or state.
   * 
   * @param object any object to associate with this table, or null to clear
   */
  final public void setUserData( Object object )
  {
    // store reference to user-provided data object
    m_userData = object;
  }

  /***************************************** getUserData *****************************************/
  /**
   * Retrieves the application-specific data previously stored with setUserData().
   * 
   * @return the previously stored user data object, or null if none was set
   */
  final public Object getUserData()
  {
    // return previously stored user data reference
    return m_userData;
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return concise string representation showing current table dimensions
    return Utils.name( this ) + "[m_columnCount=" + m_columnCount + " m_rowCount=" + m_rowCount + "]";
  }

}