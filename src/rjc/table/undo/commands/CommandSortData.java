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

package rjc.table.undo.commands;

import javafx.geometry.Orientation;
import rjc.table.data.IDataSwapColumns;
import rjc.table.data.IDataSwapRows;
import rjc.table.data.TableData;
import rjc.table.undo.IUndoCommand;
import rjc.table.view.action.Sort;
import rjc.table.view.action.Sort.SortType;

/*************************************************************************************************/
/****************** UndoCommand for sorting columns or rows in table-data ***********************/
/*************************************************************************************************/

/**
 * Undoable command for sorting columns or rows using minimal swaps.
 * <p>
 * Reorders data from old to new order via cycle decomposition, using
 * mathematically optimal number of swaps. Both undo and redo are supported
 * by reversing permutation direction. Sort state is recorded in
 * {@link Sort#columnSort} or {@link Sort#rowSort} so the overlay can
 * display sort-direction indicators.
 *
 * @see IUndoCommand
 * @see TableData
 * @see IDataSwapColumns
 * @see IDataSwapRows
 */
public class CommandSortData implements IUndoCommand
{
  @FunctionalInterface
  private interface Swapper // functional interface for swapping columns or rows
  {
    void swap( int i, int j );
  }

  private final TableData   m_data;          // table data to reorder
  private final Orientation m_orientation;   // HORIZONTAL for columns, VERTICAL for rows
  private final int[]       m_oldOrder;      // data-indices order before sort
  private final int[]       m_newOrder;      // data-indices order after sort
  private final Swapper     m_swapper;       // method reference for swapping columns or rows
  private final int         m_sortDataIndex; // data index of the column or row used as sort key
  private final SortType    m_sortType;      // sort direction
  private SortType          m_previousSort;  // map value present before last redo(); NOTSORTED = absent
  private String            m_text;          // lazily constructed description for undo/redo UI

  /****************************************** constructor *****************************************/
  /**
   * Creates and executes a column or row sorting command.
   *
   * @param data          the table data to reorder
   * @param orientation   {@code HORIZONTAL} for columns, {@code VERTICAL} for rows
   * @param beforeOrder   array of data-indices representing current positions
   * @param afterOrder    array of data-indices representing desired positions
   * @param sortDataIndex data index of the column or row used as the sort key
   * @param sortType      the sort direction ({@code ASCENDING} or {@code DESCENDING})
   */
  public CommandSortData( TableData data, Orientation orientation, int[] beforeOrder, int[] afterOrder,
      int sortDataIndex, SortType sortType )
  {
    // prepare sort command
    m_data = data;
    m_orientation = orientation;
    m_oldOrder = beforeOrder;
    m_newOrder = afterOrder;
    m_sortDataIndex = sortDataIndex;
    m_sortType = sortType;

    // assign swapper method reference based on orientation
    if ( m_orientation == Orientation.HORIZONTAL )
      m_swapper = ( (IDataSwapColumns) m_data )::swapColumns;
    else
      m_swapper = ( (IDataSwapRows) m_data )::swapRows;

    redo();
  }

  /********************************************** redo ********************************************/
  /**
   * Executes the sort operation and records sort state for overlay highlighting.
   * Captures the pre-existing map value so {@link #undo()} can restore it exactly.
   */
  @Override
  public void redo()
  {
    var map = m_orientation == Orientation.VERTICAL ? Sort.columnSort : Sort.rowSort;

    // snapshot whatever is in the map before overwriting (NOTSORTED signals absent)
    m_previousSort = map.get( m_data, m_sortDataIndex, SortType.NOTSORTED );

    // action command, record sort state, and signal data change
    reorder( m_oldOrder, m_newOrder );
    map.put( m_data, m_sortDataIndex, m_sortType );
    m_data.signalTableChanged();
  }

  /********************************************** undo ********************************************/
  /**
   * Reverses the sort operation and restores the previous sort state in the overlay.
   * If no sort state existed before this command, the map entry is removed entirely.
   */
  @Override
  public void undo()
  {
    var map = m_orientation == Orientation.VERTICAL ? Sort.columnSort : Sort.rowSort;

    // revert sort, restore previous map state, and signal data change
    reorder( m_newOrder, m_oldOrder );
    if ( m_previousSort == SortType.NOTSORTED )
      map.remove( m_data, m_sortDataIndex );
    else
      map.put( m_data, m_sortDataIndex, m_previousSort );
    m_data.signalTableChanged();
  }

  /********************************************* text *********************************************/
  /**
   * Returns a description of this command for undo/redo UI.
   * <p>
   * Lazily constructs the description from the sort-key header and direction arrow.
   * Arrow convention: column sorts use {@code ↓} for ascending, row sorts use {@code ↑}.
   *
   * @return command description
   */
  @Override
  public String text()
  {
    if ( m_text == null )
    {
      // retrieve header label for the sort-key column or row
      String header = m_orientation == Orientation.VERTICAL
          ? m_data.getValue( m_sortDataIndex, TableData.HEADER ).toString()
          : m_data.getValue( TableData.HEADER, m_sortDataIndex ).toString();

      // arrow direction: column sorts use ↓ for ascending, row sorts use ↑ for ascending
      boolean ascending = m_sortType == SortType.ASCENDING;
      String arrow = m_orientation == Orientation.VERTICAL ? ( ascending ? "▲" : "▼" ) : ( ascending ? "◀" : "▶" );

      m_text = ( m_orientation == Orientation.VERTICAL ? "Sort rows by " : "Sort columns by " ) + header + " " + arrow;
    }
    return m_text;
  }

  /******************************************* isValid ********************************************/
  /**
   * Checks if this command is valid.
   *
   * @return {@code true} if data moves were needed
   */
  @Override
  public boolean isValid()
  {
    // command is valid if data moves were needed
    return m_data != null;
  }

  /******************************************* reorder ********************************************/
  /**
   * Reorders elements using minimal swaps via cycle decomposition.
   * <p>
   * Treats transformation from oldOrder to newOrder as a permutation, decomposing
   * into disjoint cycles. Each cycle resolved with (cycle_length - 1) swaps.
   * <p>
   * Example: {@code oldOrder=[5,3,7,1]} {@code newOrder=[1,7,3,5]} means position 0
   * currently has data-index 5 but should have data-index 1. Swap function called
   * with positions, not data-indices.
   *
   * @param oldOrder array mapping positions to current data-indices
   * @param newOrder array mapping positions to desired data-indices
   */
  private void reorder( int[] oldOrder, int[] newOrder )
  {
    // build reverse lookup: data-index -> position in currentOrder
    int[] currentOrder = oldOrder.clone();
    int[] dataIndexToPosition = createInverseMapping( currentOrder );

    // track visited positions to avoid redundant processing
    boolean[] visited = new boolean[currentOrder.length];

    // process each position, following cycles until all resolved
    for ( int pos = 0; pos < currentOrder.length; pos++ )
    {
      if ( visited[pos] || currentOrder[pos] == newOrder[pos] )
        continue;

      // follow cycle until complete
      int currentPos = pos;
      while ( currentOrder[currentPos] != newOrder[currentPos] )
      {
        visited[currentPos] = true;

        // find where the data-index we need is currently located
        int targetDataIndex = newOrder[currentPos];
        int targetPos = dataIndexToPosition[targetDataIndex];

        // swap by position (not data-index)
        m_swapper.swap( currentPos, targetPos );

        // maintain currentOrder and dataIndexToPosition consistency after swap
        int temp = currentOrder[currentPos];
        currentOrder[currentPos] = currentOrder[targetPos];
        currentOrder[targetPos] = temp;

        dataIndexToPosition[currentOrder[currentPos]] = currentPos;
        dataIndexToPosition[currentOrder[targetPos]] = targetPos;

        // move to the position we just emptied
        currentPos = targetPos;
      }

      // mark final position in cycle as visited
      visited[currentPos] = true;
    }
  }

  /************************************* createInverseMapping *************************************/
  /**
   * Builds a reverse lookup table from data-indices to array positions.
   * <p>
   * Given {@code order[i]} contains data-index values, creates inverse mapping
   * where {@code result[dataIndex]} gives position of that data-index in order array.
   * Enables O(1) lookup when searching for which position holds a specific data-index.
   *
   * @param order array of data-indices
   * @return inverse mapping array where {@code result[order[i]] = i} for all valid {@code i}
   * @throws IllegalArgumentException if order contains duplicate data-indices
   */
  private int[] createInverseMapping( int[] order )
  {
    // find maximum data-index to determine array size needed
    int max = order[0];
    for ( int i = 1; i < order.length; i++ )
      if ( order[i] > max )
        max = order[i];

    // initialise all positions as unused
    int[] inverse = new int[max + 1];
    for ( int i = 0; i < inverse.length; i++ )
      inverse[i] = -1;

    // map each data-index to its position, detecting duplicates
    for ( int i = 0; i < order.length; i++ )
    {
      if ( inverse[order[i]] != -1 )
        throw new IllegalArgumentException( "Duplicate value in order array: " + order[i] );
      inverse[order[i]] = i;
    }

    return inverse;
  }

}