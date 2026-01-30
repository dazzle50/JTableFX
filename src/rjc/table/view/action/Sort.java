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

package rjc.table.view.action;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import rjc.table.data.IDataSortRows;
import rjc.table.undo.IUndoCommand;
import rjc.table.undo.commands.CommandSortData;
import rjc.table.undo.commands.CommandSortView;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/************************* Sort table-view columns/rows via undo command *************************/
/*************************************************************************************************/

public class Sort
{
  // maps to hold view-specific column sort states
  private static final Map<TableView, Map<Integer, SortType>> COLUMN_SORTS = new WeakHashMap<>();

  // maps to hold view-specific row sort states
  private static final Map<TableView, Map<Integer, SortType>> ROW_SORTS    = new WeakHashMap<>();

  /**
   * Enumeration representing the sorting state of a column or row.
   */
  public enum SortType
  {
    ASCENDING, DESCENDING, NOTSORTED
  }

  /**
   * Functional interface for comparing two integer indices.
   * Used to determine sort order when comparing data elements.
   */
  @FunctionalInterface
  public interface IntComparator
  {
    /**
     * Compares two integer indices for order.
     * 
     * @param index1 the first index to compare
     * @param index2 the second index to compare
     * @return negative if index1 < index2, zero if index1 == index2, positive if index1 > index2
     */
    int compare( int index1, int index2 );
  }

  /*************************************** isColumnSorted ****************************************/
  /**
   * Retrieves the current sort status for a specified data column in a table view.
   * 
   * @param view the table view to query
   * @param dataColumn the data column index
   * @return the sort type (ASCENDING, DESCENDING, or NOTSORTED)
   */
  public static SortType isColumnSorted( TableView view, int dataColumn )
  {
    // lookup sort state for this view and column, defaulting to unsorted
    return COLUMN_SORTS.getOrDefault( view, Map.of() ).getOrDefault( dataColumn, SortType.NOTSORTED );
  }

  /***************************************** isRowSorted *****************************************/
  /**
   * Retrieves the current sort status for a specified data row in a table view.
   * 
   * @param view the table view to query
   * @param dataRow the data row index
   * @return the sort type (ASCENDING, DESCENDING, or NOTSORTED)
   */
  public static SortType isRowSorted( TableView view, int dataRow )
  {
    // lookup sort state for this view and row, defaulting to unsorted
    return ROW_SORTS.getOrDefault( view, Map.of() ).getOrDefault( dataRow, SortType.NOTSORTED );
  }

  /************************************** setColumnSorted ****************************************/
  /**
   * Sets the sort status for a specified data column in a table view.
   * Creates the view's sort map if it doesn't exist.
   * 
   * @param view the table view to update
   * @param dataColumn the data column index
   * @param status the new sort status
   */
  public static void setColumnSorted( TableView view, int dataColumn, SortType status )
  {
    // ensure view has a sort map, then update the column's sort status
    COLUMN_SORTS.computeIfAbsent( view, v -> new HashMap<>( 4 ) ).put( dataColumn, status );
  }

  /*************************************** setRowSorted ******************************************/
  /**
   * Sets the sort status for a specified data row in a table view.
   * Creates the view's sort map if it doesn't exist.
   * 
   * @param view the table view to update
   * @param dataRow the data row index
   * @param status the new sort status
   */
  public static void setRowSorted( TableView view, int dataRow, SortType status )
  {
    // ensure view has a sort map, then update the row's sort status
    ROW_SORTS.computeIfAbsent( view, v -> new HashMap<>( 4 ) ).put( dataRow, status );
  }

  /*************************************** columnSort ********************************************/
  /**
   * Sorts visible rows in the table view based on values in the specified column.
   * Creates and executes an undo-command that reorders rows according to the
   * column's comparator and specified sort direction. Only visible rows are sorted.
   * 
   * @param view the table view to sort
   * @param viewColumn the view column index to sort by
   * @param type the sort direction (ASCENDING or DESCENDING)
   * @return true if sort was performed, false if no change needed
   */
  public static boolean columnSort( TableView view, int viewColumn, SortType type )
  {
    // get comparator for the specified data column
    var comparator = view.getData().getColumnComparator( view.getColumnsAxis().getDataIndex( viewColumn ) );

    // collect indices of visible rows only
    var visibleViewRows = getVisibleIndices( view.getRowsAxis() );

    // convert visible view indices to data indices before sorting
    var beforeDataRows = new int[visibleViewRows.length];
    for ( int i = 0; i < visibleViewRows.length; i++ )
      beforeDataRows[i] = view.getRowsAxis().getDataIndex( visibleViewRows[i] );

    // perform stable sort to get new data row order
    var afterDataRows = stableSort( beforeDataRows, comparator, type );

    // check if sort would actually change anything
    if ( Arrays.equals( beforeDataRows, afterDataRows ) )
      return false;

    // create appropriate command based on whether data layer supports sorting
    IUndoCommand command = view.getData() instanceof IDataSortRows ? new CommandSortData( view, viewColumn, type )
        : new CommandSortView( view, view.getRowsAxis(), visibleViewRows, afterDataRows );

    // execute command through undo stack
    return view.getUndoStack().push( command );
  }

  /***************************************** rowSort *********************************************/
  /**
   * Sorts visible columns in the table view based on values in the specified row.
   * Creates and executes an undo-able command that reorders columns according to the
   * row's comparator and specified sort direction. Only visible columns are sorted.
   * 
   * @param view the table view to sort
   * @param viewRow the view row index to sort by
   * @param type the sort direction (ASCENDING or DESCENDING)
   * @return true if sort was performed, false if no change needed
   */
  public static boolean rowSort( TableView view, int viewRow, SortType type )
  {
    // get comparator for the specified data row
    var comparator = view.getData().getRowComparator( view.getRowsAxis().getDataIndex( viewRow ) );

    // collect indices of visible columns only
    var visibleViewColumns = getVisibleIndices( view.getColumnsAxis() );

    // convert visible view indices to data indices before sorting
    var beforeDataColumns = new int[visibleViewColumns.length];
    for ( int i = 0; i < visibleViewColumns.length; i++ )
      beforeDataColumns[i] = view.getColumnsAxis().getDataIndex( visibleViewColumns[i] );

    // perform stable sort to get new data column order
    var afterDataColumns = stableSort( beforeDataColumns, comparator, type );

    // check if sort would actually change anything
    if ( Arrays.equals( beforeDataColumns, afterDataColumns ) )
      return false;

    // create view sort command for columns axis
    IUndoCommand command = new CommandSortView( view, view.getColumnsAxis(), visibleViewColumns, afterDataColumns );

    // execute command through undo stack
    return view.getUndoStack().push( command );
  }

  /************************************** getVisibleIndices **************************************/
  /**
   * Extracts the indices of visible rows or columns from the specified axis.
   * An index is considered visible if it has a non-zero pixel span.
   * 
   * @param axis the table axis to query
   * @return array of visible view indices
   */
  private static int[] getVisibleIndices( TableAxis axis )
  {
    // get total count of indices in this axis
    int count = axis.getCount();

    // allocate temporary array (worst case: all indices visible)
    int[] tempVisible = new int[count];
    int visibleCount = 0;
    int pixelStart = axis.getPixelStart( 0, 0 );

    // scan through all indices checking for non-zero pixel spans
    for ( int viewIndex = 0; viewIndex < count; viewIndex++ )
    {
      int pixelEnd = axis.getPixelStart( viewIndex + 1, 0 );
      if ( pixelEnd - pixelStart > 0 )
        tempVisible[visibleCount++] = viewIndex;
      pixelStart = pixelEnd;
    }

    // trim array to actual visible count
    int[] result = new int[visibleCount];
    System.arraycopy( tempVisible, 0, result, 0, visibleCount );

    return result;
  }

  /************************************** stableSort *********************************************/
  /**
   * Performs a stable sort on the specified integer array using the provided comparator.
   * Stability is maintained by using original indices as tie-breakers when elements compare equal.
   * The input array is not modified; a new sorted array is returned.
   * 
   * @param dataIndex the array of data indices to sort (not modified)
   * @param comparator the comparator determining element order
   * @param type the sort direction (ASCENDING or DESCENDING)
   * @return a new array containing the sorted data indices
   */
  public static int[] stableSort( int[] dataIndex, IntComparator comparator, SortType type )
  {
    // create index array tracking original positions
    Integer[] indices = new Integer[dataIndex.length];
    for ( int i = 0; i < dataIndex.length; i++ )
      indices[i] = i;

    // sort indices using comparator with stability preserved by index tie-breaker
    Arrays.sort( indices, ( i, j ) ->
    {
      int cmp = comparator.compare( dataIndex[i], dataIndex[j] );
      return cmp != 0 ? cmp : Integer.compare( i, j );
    } );

    // build result array in requested order
    int[] sorted = new int[dataIndex.length];
    if ( type == SortType.DESCENDING )
      for ( int i = 0; i < dataIndex.length; i++ )
        sorted[i] = dataIndex[indices[dataIndex.length - 1 - i]];
    else
      for ( int i = 0; i < dataIndex.length; i++ )
        sorted[i] = dataIndex[indices[i]];

    return sorted;
  }
}