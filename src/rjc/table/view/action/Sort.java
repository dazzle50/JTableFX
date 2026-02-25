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

import javafx.geometry.Orientation;
import rjc.table.data.IDataSwapColumns;
import rjc.table.data.IDataSwapRows;
import rjc.table.undo.IUndoCommand;
import rjc.table.undo.commands.CommandSortData;
import rjc.table.undo.commands.CommandSortView;
import rjc.table.view.TableView;

/*************************************************************************************************/
/************************* Sort table-view columns/rows via undo command *************************/
/*************************************************************************************************/

/**
 * Provides stable sorting for table view columns and rows with undo support.
 * Tracks sort state per view using weak references and generates undo commands
 * for all sort operations. Only visible columns and rows are affected by sorting.
 */
public class Sort
{
  // sort state per view/data-model and data-index; visible externally to avoid wrapper methods
  public static final WeakSortMap<Object, SortType> columnSort = new WeakSortMap<>();
  public static final WeakSortMap<Object, SortType> rowSort    = new WeakSortMap<>();

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
     * @return negative if value at index1 &lt; value at index2, zero if equal, else positive
     */
    int compare( int index1, int index2 );
  }

  /*************************************** columnSort ********************************************/
  /**
   * Sorts visible rows in the table view based on values in the specified column.
   * Creates and executes an undo-able command that reorders rows according to the
   * column's comparator and specified sort direction. Only visible rows are sorted.
   *
   * @param view       the table view to sort
   * @param viewColumn the view column index to sort by
   * @param type       the sort direction (ASCENDING or DESCENDING)
   * @return {@code true} if the sort was performed, {@code false} if no change was needed
   */
  public static boolean columnSort( TableView view, int viewColumn, SortType type )
  {
    // get comparator for the specified data column
    int dataColumn = view.getColumnsAxis().getDataIndex( viewColumn );
    var comparator = view.getData().getColumnComparator( dataColumn );

    // collect indices of visible rows only
    var visibleViewRows = view.getRowsAxis().getAllVisible();

    // convert visible view indices to data indices before sorting
    var beforeDataRows = new int[visibleViewRows.length];
    for ( int i = 0; i < visibleViewRows.length; i++ )
      beforeDataRows[i] = view.getRowsAxis().getDataIndex( visibleViewRows[i] );

    // perform stable sort to get new data row order
    var afterDataRows = stableSort( beforeDataRows, comparator, type );
    if ( Arrays.equals( beforeDataRows, afterDataRows ) )
      return false; // no change in order, so no need to execute command

    // create appropriate command based on whether data layer supports reordering
    IUndoCommand command = view.getData() instanceof IDataSwapRows
        ? new CommandSortData( view.getData(), Orientation.VERTICAL, beforeDataRows, afterDataRows, dataColumn, type )
        : new CommandSortView( view, view.getRowsAxis(), visibleViewRows, afterDataRows, dataColumn, type );

    return view.getUndoStack().push( command );
  }

  /***************************************** rowSort *********************************************/
  /**
   * Sorts visible columns in the table view based on values in the specified row.
   * Creates and executes an undo-able command that reorders columns according to the
   * row's comparator and specified sort direction. Only visible columns are sorted.
   *
   * @param view    the table view to sort
   * @param viewRow the view row index to sort by
   * @param type    the sort direction (ASCENDING or DESCENDING)
   * @return {@code true} if the sort was performed, {@code false} if no change was needed
   */
  public static boolean rowSort( TableView view, int viewRow, SortType type )
  {
    // get comparator for the specified data row
    int dataRow = view.getRowsAxis().getDataIndex( viewRow );
    var comparator = view.getData().getRowComparator( dataRow );

    // collect indices of visible columns only
    var visibleViewColumns = view.getColumnsAxis().getAllVisible();

    // convert visible view indices to data indices before sorting
    var beforeDataColumns = new int[visibleViewColumns.length];
    for ( int i = 0; i < visibleViewColumns.length; i++ )
      beforeDataColumns[i] = view.getColumnsAxis().getDataIndex( visibleViewColumns[i] );

    // perform stable sort to get new data column order
    var afterDataColumns = stableSort( beforeDataColumns, comparator, type );
    if ( Arrays.equals( beforeDataColumns, afterDataColumns ) )
      return false; // no change in order, so no need to execute command

    // create appropriate command based on whether data layer supports reordering
    IUndoCommand command = view.getData() instanceof IDataSwapColumns
        ? new CommandSortData( view.getData(), Orientation.HORIZONTAL, beforeDataColumns, afterDataColumns, dataRow,
            type )
        : new CommandSortView( view, view.getColumnsAxis(), visibleViewColumns, afterDataColumns, dataRow, type );

    return view.getUndoStack().push( command );
  }

  /************************************** stableSort *********************************************/
  /**
   * Performs a stable sort on the specified integer array using the provided comparator.
   * Stability is maintained by using original indices as tie-breakers when elements compare equal.
   * The input array is not modified; a new sorted array is returned.
   *
   * @param dataIndex  the array of data indices to sort (not modified)
   * @param comparator the comparator determining element order
   * @param type       the sort direction (ASCENDING or DESCENDING)
   * @return a new array containing the sorted data indices
   */
  public static int[] stableSort( int[] dataIndex, IntComparator comparator, SortType type )
  {
    int[] indices = new int[dataIndex.length];
    for ( int i = 0; i < dataIndex.length; i++ )
      indices[i] = i;

    boolean descending = ( type == SortType.DESCENDING );
    int[] temp = new int[dataIndex.length];
    mergeSort( dataIndex, indices, temp, 0, dataIndex.length - 1, comparator, descending );

    // build result array in sorted order
    int[] sorted = new int[dataIndex.length];
    for ( int i = 0; i < dataIndex.length; i++ )
      sorted[i] = dataIndex[indices[i]];

    return sorted;
  }

  /**************************************** mergeSort ********************************************/
  /**
   * Recursively sorts the indices array using the merge sort algorithm.
   * Stability is maintained through original index order for equal elements.
   *
   * @param dataIndex  the data values to compare
   * @param indices    the array of indices being sorted
   * @param temp       temporary array for merging
   * @param left       the left boundary (inclusive)
   * @param right      the right boundary (inclusive)
   * @param comparator the comparator determining element order
   * @param descending {@code true} for descending sort, {@code false} for ascending
   */
  private static void mergeSort( int[] dataIndex, int[] indices, int[] temp, int left, int right,
      IntComparator comparator, boolean descending )
  {
    if ( left >= right )
      return;

    int mid = left + ( right - left ) / 2;
    mergeSort( dataIndex, indices, temp, left, mid, comparator, descending );
    mergeSort( dataIndex, indices, temp, mid + 1, right, comparator, descending );
    merge( dataIndex, indices, temp, left, mid, right, comparator, descending );
  }

  /****************************************** merge **********************************************/
  /**
   * Merges two sorted sub-arrays into a single sorted array.
   *
   * @param dataIndex  the data values to compare
   * @param indices    the array of indices being sorted
   * @param temp       temporary array for merging
   * @param left       the left boundary
   * @param mid        the middle point
   * @param right      the right boundary
   * @param comparator the comparator determining element order
   * @param descending {@code true} for descending sort, {@code false} for ascending
   */
  private static void merge( int[] dataIndex, int[] indices, int[] temp, int left, int mid, int right,
      IntComparator comparator, boolean descending )
  {
    // copy to temp array
    for ( int i = left; i <= right; i++ )
      temp[i] = indices[i];

    int i = left;
    int j = mid + 1;
    int k = left;

    while ( i <= mid && j <= right )
    {
      int cmp = comparator.compare( dataIndex[temp[i]], dataIndex[temp[j]] );
      if ( descending )
        cmp = -cmp;

      // use <= on left side to maintain stability
      if ( cmp <= 0 )
        indices[k++] = temp[i++];
      else
        indices[k++] = temp[j++];
    }

    // copy remaining elements from left half
    while ( i <= mid )
      indices[k++] = temp[i++];

    // copy remaining elements from right half
    while ( j <= right )
      indices[k++] = temp[j++];
  }

}