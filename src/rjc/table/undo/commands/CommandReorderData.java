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

import java.util.Arrays;

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
 * Command for reordering columns or rows in table data with full undo/redo support.
 * <p>
 * This implementation uses an efficient block rotation algorithm to move non-contiguous
 * selections to a target position. The algorithm automatically detects contiguous blocks
 * within the selection and processes them using a triple-reverse rotation technique,
 * minimising the number of swap operations required.
 * <p>
 * The reordering operation splits processing into two passes to avoid complex index tracking:
 * <ul>
 * <li>Items before the insert point are moved rightward (processed right-to-left)</li>
 * <li>Items after the insert point are moved leftward (processed left-to-right)</li>
 * </ul>
 * This approach ensures that moves in one direction do not affect the relative indices of
 * pending blocks in the opposite direction.
 * <p>
 * <b>Performance Characteristics:</b>
 * <ul>
 * <li>Time complexity: O(n × m) where n = number of blocks, m = average distance moved</li>
 * <li>Space complexity: O(k) where k = number of selected items</li>
 * <li>All operations performed in-place with no auxiliary arrays</li>
 * </ul>
 *
 * @see IUndoCommand
 * @see TableData
 * @see IDataSortColumns
 * @see IDataSortRows
 */
public class CommandReorderData implements IUndoCommand
{
  private TableData    m_data;        // table data
  private Orientation  m_orientation; // orientation being reordered
  private int[]        m_fromIndexes; // sorted data-indexes being moved
  private int          m_insert;      // insert position
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
   * Creates a command to reorder columns or rows in table data.
   * <p>
   * The command immediately executes the reordering operation.
   *
   * @param data        the table data model to reorder
   * @param orientation the orientation (HORIZONTAL for columns, VERTICAL for rows)
   * @param selected    the set of data indexes to move
   * @param insertIndex the target insert position
   */
  public CommandReorderData( TableData data, Orientation orientation, HashSetInt selected, int insertIndex )
  {
    // store command parameters
    m_data = data;
    m_orientation = orientation;
    m_fromIndexes = selected.toSortedArray();
    m_insert = insertIndex;

    // check if any actual movement required
    if ( isAlreadyInPosition( m_fromIndexes, m_insert ) )
    {
      m_fromIndexes = new int[0];
      return;
    }

    // store appropriate swap function based on orientation
    if ( m_orientation == Orientation.HORIZONTAL )
      m_swapFunc = ( (IDataReorderColumns) m_data )::swapColumns;
    else
      m_swapFunc = ( (IDataReorderRows) m_data )::swapRows;

    // action the reorder
    redo();
  }

  /********************************************* redo ********************************************/
  /**
   * Re-executes the reordering operation.
   * <p>
   * Applies the same transformation that was performed during construction, moving the
   * originally selected items to their target position.
   */
  @Override
  public void redo()
  {
    // re-execute the reorder operation
    rearrange( m_fromIndexes, m_insert );
    m_data.signalTableChanged();
  }

  /********************************************* undo ********************************************/
  /**
   * Reverses the reordering operation.
   * <p>
   * Calculates where the moved items ended up and moves them back to their original positions.
   * After the undo, all moved items are restored to the positions they occupied before the
   * initial reorder operation.
   */
  @Override
  public void undo()
  {
    // calculate where moved items ended up
    int finalPosition = calculateFinalPosition();

    // all moved items are now consecutive
    int[] reverseIndexes = new int[m_fromIndexes.length];
    Arrays.setAll( reverseIndexes, i -> finalPosition + i );

    // calculate where to insert them back
    int reverseInsert = m_fromIndexes[0];

    // execute the reverse reorder
    rearrange( reverseIndexes, reverseInsert );
    m_data.signalTableChanged();
  }

  /********************************************* text ********************************************/
  /**
   * Returns a human-readable description of this command.
   * <p>
   * The description indicates the number of columns or rows moved, using proper
   * pluralisation (e.g., "Moved 1 column" or "Moved 3 rows").
   *
   * @return description text for display in undo/redo menus
   */
  @Override
  public String text()
  {
    // command description
    if ( m_text == null )
      m_text = "Moved " + m_fromIndexes.length + ( m_orientation == Orientation.HORIZONTAL ? " column" : " row" )
          + ( m_fromIndexes.length > 1 ? "s" : "" );

    return m_text;
  }

  /******************************************* isValid *******************************************/
  /**
   * Checks whether this command represents a valid reordering operation.
   * <p>
   * A command is considered valid only if it actually moves items to a different position.
   * If the selected items were already at the target position, the command is invalid.
   *
   * @return {@code true} if the command performed an actual reordering, {@code false} otherwise
   */
  @Override
  public boolean isValid()
  {
    // command is valid only if reorder results in difference
    return m_fromIndexes.length > 0;
  }

  /****************************************** rearrange ******************************************/
  /**
   * Executes the rearranging operation using efficient block rotation algorithm.
   * <p>
   * Splits processing into two passes: items before the insert point (processed
   * right-to-left) and items after the insert point (processed left-to-right).
   * This avoids complex index tracking as moves in one direction do not affect
   * the relative indices of pending blocks in the opposite direction.
   *
   * @param fromIndexes sorted array of indexes to move
   * @param insertIndex target insert position
   */
  private void rearrange( int[] fromIndexes, int insertIndex )
  {
    if ( fromIndexes.length == 0 )
      return;

    // find split point where indexes cross the insert position
    int splitIndex = 0;
    while ( splitIndex < fromIndexes.length && fromIndexes[splitIndex] < insertIndex )
      splitIndex++;

    // process items before insertIndex (moving right)
    // iterate backwards so we process the block closest to insertIndex first
    // as we stack items, the target moves left
    int currentInsert = insertIndex;
    int i = splitIndex - 1;
    while ( i >= 0 )
    {
      // find contiguous block ending at i
      int blockEnd = i;
      while ( i > 0 && fromIndexes[i - 1] == fromIndexes[i] - 1 )
        i--;

      int blockStart = i;
      int blockLen = blockEnd - blockStart + 1;
      int source = fromIndexes[blockStart];

      rotateBlock( source, blockLen, currentInsert );

      currentInsert -= blockLen;
      i = blockStart - 1; // move to next item
    }

    // process items after insertIndex (moving left)
    // iterate forwards so we process the block closest to insertIndex first
    // as we stack items, the target moves right
    currentInsert = insertIndex;
    i = splitIndex;
    while ( i < fromIndexes.length )
    {
      // find contiguous block starting at i
      int blockStart = i;
      while ( i < fromIndexes.length - 1 && fromIndexes[i + 1] == fromIndexes[i] + 1 )
        i++;

      int blockEnd = i;
      int blockLen = blockEnd - blockStart + 1;
      int source = fromIndexes[blockStart];

      rotateBlock( source, blockLen, currentInsert );

      currentInsert += blockLen;
      i = blockEnd + 1; // move to next item
    }
  }

  /************************************* isAlreadyInPosition *************************************/
  /**
   * Checks if the selected indexes are already at the target position.
   *
   * @param fromIndexes sorted array of indexes to check
   * @param insertIndex target insert position
   * @return {@code true} if items are already in position
   */
  private boolean isAlreadyInPosition( int[] fromIndexes, int insertIndex )
  {
    // check if range is contiguous and matches insert position
    // logic: first index matches insert, and range length matches array length
    return fromIndexes.length > 0 && fromIndexes[0] == insertIndex
        && fromIndexes[fromIndexes.length - 1] == insertIndex + fromIndexes.length - 1;
  }

  /**************************************** rotateBlock ******************************************/
  /**
   * Rotates a contiguous block to a new position using triple-reverse algorithm.
   * <p>
   * To move block [start, start+len) to position target:
   * <ul>
   * <li>If moving left: reverse [target, start), then [start, start+len), then [target, start+len)</li>
   * <li>If moving right: reverse [start, start+len), then [start+len, target), then [start, target)</li>
   * </ul>
   *
   * @param start  starting index of block to move
   * @param length length of the block
   * @param target target insert position
   */
  private void rotateBlock( int start, int length, int target )
  {
    // check if block is already at target
    if ( start == target )
      return;

    if ( target < start )
    {
      // moving block leftward: rotate [target, start+length)
      reverseRange( target, start );
      reverseRange( start, start + length );
      reverseRange( target, start + length );
    }
    else
    {
      // moving block rightward: rotate [start, target)
      reverseRange( start, start + length );
      reverseRange( start + length, target );
      reverseRange( start, target );
    }
  }

  /**************************************** reverseRange *****************************************/
  /**
   * Reverses the order of items in a range [start, end) using swaps.
   *
   * @param start starting index (inclusive)
   * @param end   ending index (exclusive)
   */
  private void reverseRange( int start, int end )
  {
    // work from ends toward middle
    int left = start;
    int right = end - 1;

    while ( left < right )
      m_swapFunc.swap( left++, right-- );
  }

  /********************************** calculateFinalPosition *************************************/
  /**
   * Calculates where the moved items ended up after rearrangement.
   *
   * @return starting position of consecutively-placed moved items
   */
  private int calculateFinalPosition()
  {
    // count selected items that were before insert point
    int selectedBefore = 0;

    // fast path for common cases since array is sorted
    if ( m_fromIndexes.length > 0 )
    {
      if ( m_fromIndexes[m_fromIndexes.length - 1] < m_insert )
        selectedBefore = m_fromIndexes.length;
      else if ( m_fromIndexes[0] < m_insert )
      {
        // mixed case: find count of items < insert
        for ( int idx : m_fromIndexes )
          if ( idx < m_insert )
            selectedBefore++;
          else
            break;
      }
    }

    // final position accounts for items that were removed before insert
    return m_insert - selectedBefore;
  }
}