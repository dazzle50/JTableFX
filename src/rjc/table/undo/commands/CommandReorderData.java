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
import rjc.table.HashSetInt;
import rjc.table.data.IDataReorderColumns;
import rjc.table.data.IDataReorderRows;
import rjc.table.data.TableData;
import rjc.table.undo.IUndoCommand;

/*************************************************************************************************/
/******************* UndoCommand for reordering columns or rows in table-data ********************/
/*************************************************************************************************/

/**
 * Undoable command for reordering columns or rows using minimal swaps at data-level.
 * <p>
 * Moves selected items to a target position via cycle decomposition, operating
 * only on the affected range. Uses mathematically optimal number of swaps.
 * <p>
 * Example: Moving [0,1] to position 3 in [0,1,2,3,4] (i.e. between 2 & 3) produces [2,0,1,3,4].
 * 
 * @see IUndoCommand
 * @see TableData
 * @see IDataSortColumns
 * @see IDataSortRows
 */
public class CommandReorderData implements IUndoCommand
{
  @FunctionalInterface
  private interface Swapper // functional interface for swapping columns or rows
  {
    void swap( int i, int j );
  }

  private TableData         m_data;            // table data to reorder
  private final Orientation m_orientation;     // HORIZONTAL for columns, VERTICAL for rows
  private final int         m_minIndex;        // cached minimum data-index in affected range
  private final int         m_maxIndex;        // cached maximum data-index in affected range
  private final int[]       m_selectedIndexes; // sorted array of data-indices being moved
  private final int         m_insertIndex;     // data-index where selected items will be inserted
  private final Swapper     m_swapper;         // method reference for swapping columns or rows
  private String            m_text;            // text describing command for undo/redo UI

  /**************************************** constructor ******************************************/
  /**
   * Creates and executes a reordering command.
   *
   * @param data        the table data to reorder
   * @param orientation {@code HORIZONTAL} for columns, {@code VERTICAL} for rows
   * @param selected    set of data-indices to move
   * @param insertIndex data-index where selected items should be inserted
   */
  public CommandReorderData( TableData data, Orientation orientation, HashSetInt selected, int insertIndex )
  {
    // prepare reorder command
    m_data = data;
    m_orientation = orientation;
    m_selectedIndexes = selected.toSortedArray();
    m_insertIndex = insertIndex;

    // assign swapper method reference based on orientation
    if ( m_orientation == Orientation.HORIZONTAL )
      m_swapper = ( (IDataReorderColumns) m_data )::swapColumns;
    else
      m_swapper = ( (IDataReorderRows) m_data )::swapRows;

    // calculate affected range for permutation
    int selMin = m_selectedIndexes[0];
    int selMax = m_selectedIndexes[m_selectedIndexes.length - 1];
    m_minIndex = Math.min( selMin, insertIndex );
    m_maxIndex = Math.max( selMax, insertIndex + m_selectedIndexes.length - 1 );

    // test if reorder changes mapping, if not make command invalid
    if ( applyPermutation( computePermutation() ) > 0 )
      m_data.signalTableChanged();
    else
      m_data = null;
  }

  /********************************************* redo ********************************************/
  /**
   * Executes the reordering operation.
   */
  @Override
  public void redo()
  {
    // action command and signal data change
    applyPermutation( computePermutation() );
    m_data.signalTableChanged();
  }

  /********************************************* undo ********************************************/
  /**
   * Reverses the reordering operation.
   */
  @Override
  public void undo()
  {
    // revert command by applying inverse permutation and signal data change
    applyPermutation( invertPermutation( computePermutation() ) );
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
    // command description
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
   * @return {@code true} if items were moved
   */
  @Override
  public boolean isValid()
  {
    // command is valid if data moves were needed
    return m_data != null;
  }

  /************************************** computePermutation **************************************/
  /**
   * Computes the permutation mapping for the affected range.
   *
   * @return array where {@code result[i]} = data-index at position {@code i + minIndex}
   */
  private int[] computePermutation()
  {
    int size = m_maxIndex - m_minIndex + 1;
    int[] result = new int[size];

    boolean[] isSelected = new boolean[size];
    for ( int idx : m_selectedIndexes )
      isSelected[idx - m_minIndex] = true;

    int pos = 0;
    for ( int i = m_minIndex; i < m_insertIndex && i <= m_maxIndex; i++ )
      if ( !isSelected[i - m_minIndex] )
        result[pos++] = i;

    for ( int sel : m_selectedIndexes )
      result[pos++] = sel;

    for ( int i = m_insertIndex; i <= m_maxIndex; i++ )
      if ( !isSelected[i - m_minIndex] )
        result[pos++] = i;

    return result;
  }

  /************************************* invertPermutation ****************************************/
  /**
   * Inverts a permutation.
   *
   * @param permutation the permutation to invert
   * @return inverted permutation
   */
  private int[] invertPermutation( int[] permutation )
  {
    int[] inverse = new int[permutation.length];
    for ( int i = 0; i < permutation.length; i++ )
      inverse[permutation[i] - m_minIndex] = i + m_minIndex;
    return inverse;
  }

  /************************************* applyPermutation *****************************************/
  /**
   * Applies a permutation using cycle decomposition with minimal swaps.
   *
   * @param   permutation target arrangement
   * @return  number of swaps performed
   */
  private int applyPermutation( int[] permutation )
  {
    // bidirectional tracking
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

        m_swapper.swap( current + m_minIndex, targetPos + m_minIndex );
        swapCount++;

        // update both mappings
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

    // return number of swaps performed
    return swapCount;
  }

}