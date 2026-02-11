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
import rjc.table.data.IDataReorderColumns;
import rjc.table.data.IDataReorderRows;
import rjc.table.data.TableData;
import rjc.table.undo.IUndoCommand;

/*************************************************************************************************/
/****************** UndoCommand for sorting columns or rows in table-data ***********************/
/*************************************************************************************************/

/**
 * Implements an undoable/redoable command that reorders table data columns or rows.
 * <p>
 * This command performs minimal-swap reordering using cycle decomposition,
 * where columns or rows are permuted from an old order to a new order. Both undo and
 * redo operations are supported by reversing the permutation direction.
 * <p>
 * The command stores both the before and after orderings as arrays of
 * data indices, allowing efficient reversal without additional computation.
 * <p>
 * This method treats the transformation from oldOrder to newOrder as a permutation,
 * then decomposes it into disjoint cycles. Each cycle is resolved using (cycle_length - 1)
 * swaps, which is optimal. For example, if position i should contain what's currently at
 * position j, position j should contain what's at k, and k should contain what's at i,
 * this forms a 3-cycle resolved with just 2 swaps.
 * <p>
 * <b>Performance Characteristics:</b>
 * <ul>
 * <li>Time complexity: O(n) where n is the array length</li>
 * <li>Space complexity: O(max(oldOrder)) for inverse mapping plus O(n) for visited tracking</li>
 * </ul>
 *
 * @see IUndoCommand
 * @see TableData
 * @see IDataSortColumns
 * @see IDataSortRows
 */
public class CommandSortData implements IUndoCommand
{
  private TableData    m_data;        // table data
  private Orientation  m_orientation; // orientation being sorted
  private int[]        m_oldOrder;    // data-indexes order before sort
  private int[]        m_newOrder;    // data-indexes order after sort
  private String       m_text;        // text describing command
  private SwapFunction m_swapFunc;    // function to swap indexes

  // functional interface for swapping
  @FunctionalInterface
  private interface SwapFunction
  {
    void swap( int index1, int index2 );
  }

  /**************************************** constructor ******************************************/
  /**
   * Creates and executes a column or row reordering command.
   * <p>
   * The command is executed immediately upon construction (via redo).
   * 
   * @param data        the table data to reorder
   * @param orientation the orientation (HORIZONTAL for columns, VERTICAL for rows)
   * @param beforeOrder array of data indices representing current positions
   * @param afterOrder  array of data indices representing desired positions  
   * @param label       description of the sort column/row for display purposes
   */
  public CommandSortData( TableData data, Orientation orientation, int[] beforeOrder, int[] afterOrder, String label )
  {
    // store command parameters
    m_data = data;
    m_orientation = orientation;
    m_oldOrder = beforeOrder;
    m_newOrder = afterOrder;

    // build command description text
    if ( m_orientation == Orientation.HORIZONTAL )
      m_text = "Sort columns by " + label;
    else
      m_text = "Sort rows by " + label;

    // store appropriate swap function based on orientation
    if ( m_orientation == Orientation.HORIZONTAL )
      m_swapFunc = ( (IDataReorderColumns) m_data )::swapColumns;
    else
      m_swapFunc = ( (IDataReorderRows) m_data )::swapRows;

    // execute the sort
    redo();
  }

  /******************************************* redo **********************************************/
  /**
   * Reapplies the sort operation, reordering from old to new order.
   * Notifies observers that the table contents has changed.
   */
  @Override
  public void redo()
  {
    // execute the reorder operation
    reorder( m_oldOrder, m_newOrder );
    m_data.signalTableChanged();
  }

  /******************************************* undo **********************************************/
  /**
   * Reverses the sort operation, restoring to original order.
   * Notifies observers that the table contents has changed.
   */
  @Override
  public void undo()
  {
    // reverse the reorder operation
    reorder( m_newOrder, m_oldOrder );
    m_data.signalTableChanged();
  }

  /******************************************* text **********************************************/
  /**
   * Returns the text description of this command for display in undo/redo lists.
   * 
   * @return text description of the command
   */
  @Override
  public String text()
  {
    return m_text;
  }

  /**************************************** reorder **********************************************/
  /**
   * Reorders elements using minimal swaps via cycle decomposition algorithm.
   * <p>
   * This method treats the transformation from oldOrder to newOrder as a permutation,
   * then decomposes it into disjoint cycles. Each cycle is resolved using (cycle_length - 1)
   * swaps, which is optimal. For example, if position i should contain what's currently at
   * position j, position j should contain what's at k, and k should contain what's at i,
   * this forms a 3-cycle resolved with just 2 swaps.
   * <p>
   * Example: oldOrder=[5,3,7,1] newOrder=[1,7,3,5] means "the element currently at data
   * index 5 should move to where data index 1 is, the element at 3 should move to where 7 is", etc.
   * <p>
   * Time complexity: O(n) where n is the array length<br>
   * Space complexity: O(max(oldOrder)) for inverse mapping plus O(n) for visited tracking
   * 
   * @param oldOrder array mapping positions to current data indices
   * @param newOrder array mapping positions to desired data indices
   */
  private void reorder( int[] oldOrder, int[] newOrder )
  {
    // build reverse lookup: data-index -> position in currentOrder
    int[] currentOrder = oldOrder.clone();
    int[] oldToIndex = createInverseMapping( currentOrder );

    // track processed positions to avoid revisiting cycles
    boolean[] visited = new boolean[currentOrder.length];

    // process each position, following cycles until all resolved
    for ( int i = 0; i < currentOrder.length; i++ )
    {
      if ( visited[i] || currentOrder[i] == newOrder[i] )
        continue;

      // trace this cycle, swapping elements into correct positions
      int current = i;
      while ( !visited[current] )
      {
        visited[current] = true;

        int target = newOrder[current];
        int targetIndex = oldToIndex[target];

        if ( targetIndex != current )
        {
          m_swapFunc.swap( currentOrder[current], currentOrder[targetIndex] );

          // maintain currentOrder and oldToIndex consistency after swap
          int temp = currentOrder[current];
          currentOrder[current] = currentOrder[targetIndex];
          currentOrder[targetIndex] = temp;

          oldToIndex[currentOrder[current]] = current;
          oldToIndex[currentOrder[targetIndex]] = targetIndex;
        }

        current = targetIndex;
      }
    }
  }

  /********************************** createInverseMapping **************************************/
  /**
   * Builds a reverse lookup table from data indices to array positions.
   * <p>
   * Given an array where order[i] contains data index values, this creates
   * an inverse mapping where result[dataIndex] gives the position of that
   * data index in the order array. This enables O(1) lookup when searching
   * for which position currently holds a specific data index.
   * <p>
   * The resulting array is sized to accommodate the maximum data index value,
   * with unused positions marked as -1.
   * 
   * @param order array of data indices
   * @return inverse mapping array where result[order[i]] = i for all valid i
   * @throws IllegalArgumentException if order contains duplicate data indices
   */
  private int[] createInverseMapping( int[] order )
  {
    // find maximum data index to determine array size needed
    int max = order[0];
    for ( int i = 1; i < order.length; i++ )
      if ( order[i] > max )
        max = order[i];

    // initialise all positions as unused
    int[] inverse = new int[max + 1];
    for ( int i = 0; i < inverse.length; i++ )
      inverse[i] = -1;

    // map each data index to its position, detecting duplicates
    for ( int i = 0; i < order.length; i++ )
    {
      if ( inverse[order[i]] != -1 )
        throw new IllegalArgumentException( "Duplicate value in order array: " + order[i] );
      inverse[order[i]] = i;
    }

    return inverse;
  }
}