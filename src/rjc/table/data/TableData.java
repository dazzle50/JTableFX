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

import rjc.table.Utils;
import rjc.table.signal.ISignal;
import rjc.table.signal.ObservableInteger;
import rjc.table.signal.ObservableInteger.ReadOnlyInteger;
import rjc.table.view.action.Sort.IntComparator;
import rjc.table.view.cell.CellVisual;

/*************************************************************************************************/
/************** Table data source, column & row counts, signals to announce changes **************/
/*************************************************************************************************/

/**
 * Foundation class for table data implementations, providing observable properties for table
 * dimensions and a signalling system for coordinating view updates when data changes.
 *
 * <p>Column and row indices start at {@code 0} for table body cells, with {@link #HEADER}
 * ({@code -1}) representing header cells.
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

  /** Column and row index for header cells; body indices start at {@code 0}. */
  public static final int         HEADER        = -1;

  /**
   * Signal types for notifying observers about different scopes of data changes.
   */
  public enum DataChange
  {
    CELL_VALUE, ROW_VALUES, COLUMN_VALUES, WHOLE_TABLE
  }

  /*************************************** getColumnCount ****************************************/
  /**
   * Returns the number of columns in the table body (excluding the header column).
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
   * Sets the number of columns in the table body via an observable property.
   *
   * @param columnCount the new column count, must be non-negative
   * @throws IllegalArgumentException if {@code columnCount} is negative
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
   * Returns the number of rows in the table body (excluding the header row).
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
   * Sets the number of rows in the table body via an observable property.
   *
   * @param rowCount the new row count, must be non-negative
   * @throws IllegalArgumentException if {@code rowCount} is negative
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
   * Returns a read-only observable property for the column count.
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
   * Returns a read-only observable property for the row count.
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
   * Returns visual appearance settings for a specific cell.
   * Override to provide non-default styling (fonts, colours, alignment, etc.).
   *
   * @param dataColumn the column index ({@code 0}-based for body, {@link #HEADER} for header column)
   * @param dataRow    the row index ({@code 0}-based for body, {@link #HEADER} for header row)
   * @return {@link CellVisual} containing appearance settings for the specified cell
   */
  public CellVisual getVisual( int dataColumn, int dataRow )
  {
    // return default visual settings for all cells — override for custom styling
    return m_cellVisual;
  }

  /****************************************** getValue *******************************************/
  /**
   * Returns the display value for a specific cell.
   * This base implementation provides default header labels and placeholder body values.
   * Override to return actual data from your data source.
   *
   * @param dataColumn the column index ({@code 0}-based for body, {@link #HEADER} for header column)
   * @param dataRow    the row index ({@code 0}-based for body, {@link #HEADER} for header row)
   * @return the cell value to display; may be any {@link Object} type
   */
  public Object getValue( int dataColumn, int dataRow )
  {
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
   * Attempts to set a new value for the specified cell, with full validation and signalling.
   *
   * @param dataColumn the column index (must be {@code >= 0} and {@code < columnCount})
   * @param dataRow    the row index (must be {@code >= 0} and {@code < rowCount})
   * @param newValue   the new value to set; accepted type depends on cell data type
   * @return {@code null} if successful, or a String describing the problem
   */
  final public String setValue( int dataColumn, int dataRow, Object newValue )
  {
    // delegate to tryValue with commit=true to validate and persist
    return tryValue( dataColumn, dataRow, newValue, true );
  }

  /****************************************** testValue ******************************************/
  /**
   * Tests whether a value would be accepted for the specified cell without modifying any data.
   * Use during user input to provide immediate validation feedback.
   *
   * @param dataColumn the column index (must be {@code >= 0} and {@code < columnCount})
   * @param dataRow    the row index (must be {@code >= 0} and {@code < rowCount})
   * @param newValue   the value to test for validity
   * @return {@code null} if the value would be accepted, or a String describing why not
   */
  final public String testValue( int dataColumn, int dataRow, Object newValue )
  {
    // delegate to tryValue with commit=false to validate without persisting
    return tryValue( dataColumn, dataRow, newValue, false );
  }

  private String tryValue( int dataColumn, int dataRow, Object newValue, boolean commit )
  {
    // validate cell coordinates are within valid body range
    if ( dataColumn <= HEADER || dataColumn >= getColumnCount() || dataRow <= HEADER || dataRow >= getRowCount() )
      return "Cell reference out of range " + dataColumn + " " + dataRow;

    try
    {
      String outcome = setValue( dataColumn, dataRow, newValue, commit );

      // notify observers only on a committed, successful set
      if ( outcome == null && commit )
        signalCellChanged( dataColumn, dataRow );
      return outcome;
    }
    catch ( Exception exception )
    {
      // convert any unexpected exception into a descriptive rejection
      String msg = "Error " + dataColumn + " " + dataRow + " " + commit + " ";
      return msg + Utils.objectsString( newValue ) + " : " + exception.toString();
    }
  }

  /****************************************** setValue *******************************************/
  /**
   * Template method for validating and optionally persisting a cell value.
   * Override to implement data validation and storage logic for a concrete data source.
   *
   * <p>When {@code commit} is {@code false}, only validate; when {@code true}, also persist.
   * Cell coordinates are guaranteed valid when this method is called; change signalling
   * is handled by the caller.
   *
   * @param dataColumn the column index (guaranteed valid)
   * @param dataRow    the row index (guaranteed valid)
   * @param newValue   the value to validate and potentially store
   * @param commit     {@code true} to persist the value, {@code false} to validate only
   * @return {@code null} if valid (and stored when {@code commit} is {@code true}),
   *         or a String describing the problem
   */
  protected String setValue( int dataColumn, int dataRow, Object newValue, boolean commit )
  {
    // base implementation has no data storage
    return "Not implemented";
  }

  /************************************** signalCellChanged **************************************/
  /**
   * Notifies observers that a specific cell's value has changed.
   *
   * @param dataColumn the column index of the changed cell
   * @param dataRow    the row index of the changed cell
   */
  final public void signalCellChanged( int dataColumn, int dataRow )
  {
    // emit cell-level change signal with coordinates for targeted view updates
    signal( DataChange.CELL_VALUE, dataColumn, dataRow );
  }

  /************************************* signalColumnChanged *************************************/
  /**
   * Notifies observers that all values in a specific column have changed.
   *
   * @param dataColumn the column index that has changed
   */
  final public void signalColumnChanged( int dataColumn )
  {
    // emit column-level change signal for bulk column updates
    signal( DataChange.COLUMN_VALUES, dataColumn );
  }

  /*************************************** signalRowChanged **************************************/
  /**
   * Notifies observers that all values in a specific row have changed.
   *
   * @param dataRow the row index that has changed
   */
  final public void signalRowChanged( int dataRow )
  {
    // emit row-level change signal for bulk row updates
    signal( DataChange.ROW_VALUES, dataRow );
  }

  /************************************** signalTableChanged *************************************/
  /**
   * Notifies observers that multiple or all table values have changed, triggering a complete
   * view refresh. Use sparingly — this is the most expensive update operation.
   */
  final public void signalTableChanged()
  {
    // emit table-level change signal for complete table refresh
    signal( DataChange.WHOLE_TABLE );
  }

  /***************************************** setUserData *****************************************/
  /**
   * Stores arbitrary application-specific data associated with this table.
   *
   * @param object any object to associate with this table, or {@code null} to clear
   */
  final public void setUserData( Object object )
  {
    // store reference to user-provided data object
    m_userData = object;
  }

  /***************************************** getUserData *****************************************/
  /**
   * Returns the application-specific data previously stored via {@link #setUserData(Object)}.
   *
   * @return the previously stored user data, or {@code null} if none was set
   */
  final public Object getUserData()
  {
    // return previously stored user data reference
    return m_userData;
  }

  /************************************* getColumnComparator *************************************/
  /**
   * Returns a comparator for sorting rows by values in the specified column.
   * This default implementation delegates to {@link GenericComparator}.
   * Override when necessary to provide an optimised comparator for specific columns.
   *
   * @param dataColumn the column index to create a comparator for
   * @return an {@link IntComparator} comparing two row indices by their values in the given column
   */
  public IntComparator getColumnComparator( int dataColumn )
  {
    // compare rows using cell values from the specified column
    return ( dataRow1, dataRow2 ) ->
    {
      var obj1 = getValue( dataColumn, dataRow1 );
      var obj2 = getValue( dataColumn, dataRow2 );
      return GenericComparator.compare( obj1, obj2 );
    };
  }

  /*************************************** getRowComparator **************************************/
  /**
   * Returns a comparator for sorting columns by values in the specified row.
   * This default implementation delegates to {@link GenericComparator}.
   * Override when necessary to provide an optimised comparator for specific rows.
   *
   * @param dataRow the row index to create a comparator for
   * @return an {@link IntComparator} comparing two column indices by their values in the given row
   */
  public IntComparator getRowComparator( int dataRow )
  {
    // compare columns using cell values from the specified row
    return ( dataColumn1, dataColumn2 ) ->
    {
      var obj1 = getValue( dataColumn1, dataRow );
      var obj2 = getValue( dataColumn2, dataRow );
      return GenericComparator.compare( obj1, obj2 );
    };
  }

  /****************************************** toString *******************************************/
  /**
   * Returns a concise string representation showing this table's current dimensions.
   *
   * @return string identifying this instance with column and row counts
   */
  @Override
  public String toString()
  {
    // return concise string representation showing current table dimensions
    return Utils.name( this ) + "[m_columnCount=" + m_columnCount + " m_rowCount=" + m_rowCount + "]";
  }
}