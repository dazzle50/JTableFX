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
import java.util.List;
import java.util.function.IntFunction;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import rjc.table.data.IDataSwapColumns;
import rjc.table.data.IDataSwapRows;
import rjc.table.signal.ObservableStatus.Level;
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
   * Enumeration representing the sort direction of a column or row.
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

  /***************************************** columnSort ******************************************/
  /**
   * Sorts visible rows in the table view based on values in the specified column.
   * Creates and executes an undo-able command that reorders rows according to the
   * column's comparator and specified sort direction. Only visible rows are sorted.
   * Rows whose column value is {@code ""} are placed second-to-last; {@code null} values last,
   * regardless of sort direction.
   *
   * @param view       the table view to sort
   * @param viewColumn the view column index to sort by
   * @param type       the sort direction (ASCENDING or DESCENDING)
   * @see #sortAxis
   */
  public static void columnSort( TableView view, int viewColumn, SortType type )
  {
    sortAxis( view, Orientation.VERTICAL, viewColumn, type );
  }

  /******************************************* rowSort *******************************************/
  /**
   * Sorts visible columns in the table view based on values in the specified row.
   * Creates and executes an undo-able command that reorders columns according to the
   * row's comparator and specified sort direction. Only visible columns are sorted.
   * Columns whose row value is {@code ""} are placed second-to-last; {@code null} values last,
   * regardless of sort direction.
   *
   * @param view    the table view to sort
   * @param viewRow the view row index to sort by
   * @param type    the sort direction (ASCENDING or DESCENDING)
   * @see #sortAxis
   */
  public static void rowSort( TableView view, int viewRow, SortType type )
  {
    sortAxis( view, Orientation.HORIZONTAL, viewRow, type );
  }

  /****************************************** sortAxis *******************************************/
  /**
   * Generic helper that performs sorting on either the row axis or the column axis.
   * This single implementation eliminates all duplication while preserving identical
   * semantics, stable ordering, sentinel-value placement, threading rules, and
   * undo-command construction.
   *
   * <p>The {@code orientation} parameter together with the derived {@code reorderAxis}
   * and {@code keyAxis} provides the minimal axis accessor abstraction required.
   * Both axes expose the identical contract {@code getAllVisible()} and
   * {@code getDataIndex(int)}.</p>
   *
   * @param view        the table view to sort
   * @param orientation VERTICAL to sort rows by a column; HORIZONTAL to sort columns by a row
   * @param viewIndex   view index of the column (VERTICAL) or row (HORIZONTAL) used as the sort key
   * @param type        the sort direction (ASCENDING or DESCENDING)
   */
  private static void sortAxis( TableView view, Orientation orientation, int viewIndex, SortType type )
  {
    // sorting can be slow so update status to busy, and cursor to wait
    view.setBusyState( true, "Sorting ..." );

    Thread.ofVirtual().start( () ->
    {
      try
      {
        // axis abstraction: orientation selects the correct reorder and key axes
        boolean isVertical = orientation == Orientation.VERTICAL;
        var keyAxis = isVertical ? view.getColumnsAxis() : view.getRowsAxis();
        var reorderAxis = isVertical ? view.getRowsAxis() : view.getColumnsAxis();
        int dataKeyIndex = keyAxis.getDataIndex( viewIndex );

        // comparator and value fetcher differ only by axis orientation
        var data = view.getData();
        IntComparator comparator = isVertical ? data.getColumnComparator( dataKeyIndex )
            : data.getRowComparator( dataKeyIndex );
        IntFunction<Object> valueFetcher = isVertical ? row -> data.getValue( dataKeyIndex, row )
            : col -> data.getValue( col, dataKeyIndex );

        // convert view indices to data indices
        var visibleViewIndices = reorderAxis.getAllVisible();
        var beforeDataIndices = new int[visibleViewIndices.length];
        for ( int i = 0; i < visibleViewIndices.length; i++ )
          beforeDataIndices[i] = reorderAxis.getDataIndex( visibleViewIndices[i] );

        // perform sort and if change create command
        var afterDataIndices = getSortedDataIndices( beforeDataIndices, valueFetcher, comparator, type );
        if ( !Arrays.equals( beforeDataIndices, afterDataIndices ) )
        {
          // choose command based on whether the data layer supports direct swapping
          boolean canSwapData = orientation == Orientation.VERTICAL ? data instanceof IDataSwapRows
              : data instanceof IDataSwapColumns;
          var axes = List.of( orientation == Orientation.VERTICAL ? view.getRowsAxis() : view.getColumnsAxis() );

          IUndoCommand command = canSwapData
              ? new CommandSortData( data, orientation, beforeDataIndices, afterDataIndices, dataKeyIndex, type, axes )
              : new CommandSortView( view, reorderAxis, visibleViewIndices, afterDataIndices, dataKeyIndex, type );
          view.getUndoStack().push( command );
        }

        Platform.runLater( () -> view.setBusyState( false, null ) );
      }
      catch ( Exception ex )
      {
        Platform.runLater( () ->
        {
          view.setBusyState( false, null );
          view.getStatus().update( Level.ERROR, "Sort failed: " + ex.toString() );
        } );
      }
    } );
  }

  /************************************ getSortedDataIndices *************************************/
  /**
   * Partitions the supplied data indices into sortable, empty-string, and {@code null} buckets,
   * sorts the sortable bucket in place, then reassembles the full ordered array.
   * Empty-string values are placed second-to-last and {@code null} values last, regardless of
   * sort direction.
   *
   * @param dataIndices  the original data indices in current visible order
   * @param valueFetcher function returning the cell value for a given data index
   * @param comparator   the comparator determining sort order of non-sentinel values
   * @param type         the sort direction (ASCENDING or DESCENDING)
   * @return a new array of data indices in the required sorted order
   */
  private static int[] getSortedDataIndices( int[] dataIndices, IntFunction<Object> valueFetcher,
      IntComparator comparator, SortType type )
  {
    // allocate worst-case-sized buckets; actual fill counts are tracked separately
    int[] sortable = new int[dataIndices.length];
    int[] empty = new int[dataIndices.length];
    int[] nulls = new int[dataIndices.length];
    int sCount = 0, eCount = 0, nCount = 0;

    // partition in a single pass to avoid repeated value lookups
    for ( int index : dataIndices )
    {
      Object val = valueFetcher.apply( index );
      if ( val == null )
        nulls[nCount++] = index;
      else if ( "".equals( val ) )
        empty[eCount++] = index;
      else
        sortable[sCount++] = index;
    }

    // trim the sortable bucket to its fill count, then sort it in place
    int[] partition = Arrays.copyOf( sortable, sCount );
    sortInPlace( partition, comparator, type == SortType.DESCENDING );

    // reassemble into sortable as the result buffer — length equals dataIndices.length
    System.arraycopy( partition, 0, sortable, 0, sCount );
    System.arraycopy( empty, 0, sortable, sCount, eCount );
    System.arraycopy( nulls, 0, sortable, sCount + eCount, nCount );
    return sortable;
  }

  /***************************************** sortInPlace *****************************************/
  /**
   * Sorts {@code indices} in place using a stable merge sort.
   * No defensive copy is made; the caller is responsible for preserving the original if needed.
   * Returns immediately for arrays of length zero or one.
   *
   * @param indices    the array of data indices to sort (modified in place)
   * @param comparator the comparator determining element order; must not be {@code null}
   * @param descending {@code true} for descending order, {@code false} for ascending
   */
  private static void sortInPlace( int[] indices, IntComparator comparator, boolean descending )
  {
    int n = indices.length;
    if ( n <= 1 )
      return;

    int[] temp = new int[n];
    mergeSort( indices, temp, 0, n - 1, comparator, descending );
  }

  /****************************************** mergeSort ******************************************/
  /**
   * Recursively divides and sorts the range {@code [left, right]} within {@code indices}
   * using the merge sort algorithm. Stability is preserved by preferring the left element
   * on equal comparisons during the merge phase.
   *
   * @param indices    the array of data indices being sorted (modified in place)
   * @param temp       scratch array of the same length as {@code indices}
   * @param left       left boundary of the sub-array (inclusive)
   * @param right      right boundary of the sub-array (inclusive)
   * @param comparator the comparator determining element order
   * @param descending {@code true} for descending order, {@code false} for ascending
   */
  private static void mergeSort( int[] indices, int[] temp, int left, int right, IntComparator comparator,
      boolean descending )
  {
    if ( left >= right )
      return;

    int mid = left + ( right - left ) / 2;
    mergeSort( indices, temp, left, mid, comparator, descending );
    mergeSort( indices, temp, mid + 1, right, comparator, descending );
    merge( indices, temp, left, mid, right, comparator, descending );
  }

  /******************************************** merge ********************************************/
  /**
   * Merges two adjacent sorted sub-arrays — {@code indices[left..mid]} and
   * {@code indices[mid+1..right]} — into a single sorted range within {@code indices}.
   * Uses {@code temp} as scratch space. Stability is maintained by choosing the left
   * element whenever the comparator returns zero.
   *
   * @param indices    the array being sorted (modified in place)
   * @param temp       scratch array of the same length as {@code indices}
   * @param left       left boundary (inclusive)
   * @param mid        end of the left partition (inclusive)
   * @param right      right boundary (inclusive)
   * @param comparator the comparator determining element order
   * @param descending {@code true} for descending order, {@code false} for ascending
   */
  private static void merge( int[] indices, int[] temp, int left, int mid, int right, IntComparator comparator,
      boolean descending )
  {
    // copy the working range into temp before overwriting indices
    System.arraycopy( indices, left, temp, left, right - left + 1 );

    int i = left;
    int j = mid + 1;
    int k = left;

    while ( i <= mid && j <= right )
    {
      int cmp = comparator.compare( temp[i], temp[j] );
      if ( descending )
        cmp = -cmp;

      // prefer left element on equality to maintain the stable sort guarantee
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