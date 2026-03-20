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

import java.util.List;

import javafx.geometry.Orientation;
import rjc.table.HashSetInt;
import rjc.table.data.IDataSwapColumns;
import rjc.table.data.IDataSwapRows;
import rjc.table.data.TableData;
import rjc.table.undo.IUndoCommand;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/******************* UndoCommand for reordering columns or rows in table-data ********************/
/*************************************************************************************************/

/**
 * Undoable command for reordering columns or rows using minimal swaps at data-level,
 * keeping all associated {@link TableAxis} index sizes synchronised.
 * <p>
 * Moves selected items to a target position via cycle decomposition, operating
 * only on the affected range. Uses the mathematically optimal number of swaps.
 * The same permutation is applied to the data and to each axis's nominal sizes,
 * ensuring column/row widths and heights follow the reorder.
 * <p>
 * Example: moving [0,1] to position 3 in [0,1,2,3,4] (i.e. between 2 &amp; 3)
 * produces [2,0,1,3,4].
 *
 * @see IUndoCommand
 * @see TableData
 * @see TableAxis
 * @see IDataSwapColumns
 * @see IDataSwapRows
 */
public class CommandReorderData implements IUndoCommand
{
  @FunctionalInterface
  private interface Swapper // functional interface for swapping two indexed elements
  {
    void swap( int i, int j );
  }

  private TableData             m_data;            // table data to reorder (null if no-op)
  private final Orientation     m_orientation;     // HORIZONTAL for columns, VERTICAL for rows
  private final int             m_minIndex;        // cached minimum data-index in affected range
  private final int             m_maxIndex;        // cached maximum data-index in affected range
  private final int[]           m_selectedIndexes; // sorted array of data-indices being moved
  private final int             m_insertIndex;     // data-index where selected items are inserted
  private final Swapper         m_dataSwapper;     // swaps columns or rows in underlying data
  private final Swapper         m_sizeSwapper;     // swaps index sizes across all axes
  private final List<TableAxis> m_axes;            // all axes whose sizes must stay in sync
  private String                m_text;            // cached command description for undo/redo UI

  /**************************************** constructor ******************************************/
  /**
   * Creates and immediately executes the reordering operation. If no items
   * actually move, {@link #isValid()} returns {@code false} and this command
   * should not be pushed onto the undo stack.
   *
   * @param data        the table data to reorder; must implement {@link IDataSwapColumns}
   *                    for {@code HORIZONTAL} orientation or {@link IDataSwapRows} for
   *                    {@code VERTICAL} orientation
   * @param orientation {@code HORIZONTAL} for columns, {@code VERTICAL} for rows
   * @param selected    set of data-indices to move; must be non-empty
   * @param insertIndex data-index at which the selected items are inserted
   * @param axes        all {@link TableAxis} instances whose nominal sizes must be kept
   *                    in sync with the reorder; may be empty but must not be {@code null}
   * @throws ClassCastException if {@code data} does not implement the required swap interface
   */
  public CommandReorderData( TableData data, Orientation orientation, HashSetInt selected, int insertIndex,
      List<TableAxis> axes )
  {
    // prepare reorder command
    m_data = data;
    m_orientation = orientation;
    m_selectedIndexes = selected.toSortedArray();
    m_insertIndex = insertIndex;
    m_axes = axes;

    // assign data-swapper method reference based on orientation
    if ( m_orientation == Orientation.HORIZONTAL )
      m_dataSwapper = ( (IDataSwapColumns) m_data )::swapColumns;
    else
      m_dataSwapper = ( (IDataSwapRows) m_data )::swapRows;

    // size-swapper applies the same swap to every axis in one call
    m_sizeSwapper = ( i, j ) -> m_axes.forEach( axis -> axis.swapSizes( i, j ) );

    // calculate affected range for permutation
    int selMin = m_selectedIndexes[0];
    int selMax = m_selectedIndexes[m_selectedIndexes.length - 1];
    m_minIndex = Math.min( selMin, insertIndex );
    m_maxIndex = Math.max( selMax, insertIndex + m_selectedIndexes.length - 1 );

    // apply permutation; sizes only need reordering when data actually moved
    var perm = buildPermutation();
    if ( applyPermutation( perm, m_dataSwapper ) > 0 )
    {
      applyPermutation( perm, m_sizeSwapper );
      m_data.signalTableChanged();
    }
    else
      m_data = null;
  }

  /********************************************* redo ********************************************/
  /**
   * Re-executes the reordering operation after an undo.
   */
  @Override
  public void redo()
  {
    // apply forward permutation to data and sizes, then signal change
    var perm = buildPermutation();
    applyPermutation( perm, m_dataSwapper );
    applyPermutation( perm, m_sizeSwapper );
    m_data.signalTableChanged();
  }

  /********************************************* undo ********************************************/
  /**
   * Reverses the reordering operation.
   */
  @Override
  public void undo()
  {
    // apply inverse permutation to data and sizes, then signal change
    var perm = invertPermutation( buildPermutation() );
    applyPermutation( perm, m_dataSwapper );
    applyPermutation( perm, m_sizeSwapper );
    m_data.signalTableChanged();
  }

  /********************************************* text ********************************************/
  /**
   * Returns a description of this command for undo/redo UI.
   *
   * @return command description
   */
  @Override
  public String text()
  {
    // lazily build and cache command description
    if ( m_text == null )
    {
      String type = m_orientation == Orientation.HORIZONTAL ? "column" : "row";
      String plural = m_selectedIndexes.length == 1 ? "" : "s";
      m_text = "Moved " + m_selectedIndexes.length + " " + type + plural;
    }
    return m_text;
  }

  /******************************************* isValid *******************************************/
  /**
   * Checks if this command is valid.
   *
   * @return {@code true} if items were actually moved
   */
  @Override
  public boolean isValid()
  {
    // command is valid if data moves were needed
    return m_data != null;
  }

  /************************************** buildPermutation ***************************************/
  /**
   * Computes the target permutation of data-indices within the affected range
   * [{@code m_minIndex}…{@code m_maxIndex}] after the selected items are moved
   * to the insertion point.
   *
   * @return array of length {@code (m_maxIndex - m_minIndex + 1)} where
   *         {@code result[i]} is the data-index that should occupy position
   *         {@code (m_minIndex + i)} after reordering
   */
  private int[] buildPermutation()
  {
    int size = m_maxIndex - m_minIndex + 1;
    int[] result = new int[size];

    // mark which positions in the affected range are currently selected
    boolean[] isSelected = new boolean[size];
    for ( int idx : m_selectedIndexes )
      isSelected[idx - m_minIndex] = true;

    // step 1: non-selected items before the insertion point (original order)
    int pos = 0;
    for ( int i = m_minIndex; i < m_insertIndex && i <= m_maxIndex; i++ )
      if ( !isSelected[i - m_minIndex] )
        result[pos++] = i;

    // step 2: all selected items (preserving relative order)
    for ( int sel : m_selectedIndexes )
      result[pos++] = sel;

    // step 3: non-selected items at or after the insertion point
    for ( int i = m_insertIndex; i <= m_maxIndex; i++ )
      if ( !isSelected[i - m_minIndex] )
        result[pos++] = i;

    return result;
  }

  /************************************* invertPermutation ***************************************/
  /**
   * Inverts a permutation such that applying the inverse restores the original order.
   * Indices are absolute (offset by {@code m_minIndex}).
   *
   * @param permutation the permutation to invert
   * @return the inverse permutation, using the same absolute index convention
   */
  private int[] invertPermutation( int[] permutation )
  {
    int[] inverse = new int[permutation.length];
    for ( int i = 0; i < permutation.length; i++ )
      inverse[permutation[i] - m_minIndex] = i + m_minIndex;
    return inverse;
  }

  /************************************** applyPermutation ***************************************/
  /**
   * Applies a permutation using cycle decomposition, performing the minimum number
   * of swaps via the supplied {@link Swapper}. Indices are absolute (offset by
   * {@code m_minIndex}).
   *
   * @param permutation target arrangement
   * @param swapper     the swap operation to apply (data or sizes)
   * @return number of swaps performed
   */
  private int applyPermutation( int[] permutation, Swapper swapper )
  {
    // bidirectional tracking arrays map positions to data and data to positions
    int len = permutation.length;
    int swapCount = 0;
    int[] dataToPos = new int[len];
    int[] posToData = new int[len];
    for ( int i = 0; i < len; i++ )
    {
      dataToPos[i] = i;
      posToData[i] = i;
    }

    // track visited positions to avoid redundant processing
    boolean[] visited = new boolean[len];

    // process each position in the permutation
    for ( int pos = 0; pos < len; pos++ )
    {
      int desiredData = permutation[pos] - m_minIndex;
      if ( visited[pos] || posToData[pos] == desiredData )
        continue;

      int current = pos;
      while ( posToData[current] != permutation[current] - m_minIndex )
      {
        visited[current] = true;
        int targetData = permutation[current] - m_minIndex;
        int targetPos = dataToPos[targetData];

        swapper.swap( current + m_minIndex, targetPos + m_minIndex );
        swapCount++;

        // update both tracking arrays after swap
        int leftData = posToData[current];
        int rightData = posToData[targetPos];
        dataToPos[leftData] = targetPos;
        dataToPos[rightData] = current;
        posToData[current] = rightData;
        posToData[targetPos] = leftData;

        current = targetPos;
      }
      visited[current] = true;
    }

    return swapCount;
  }

}