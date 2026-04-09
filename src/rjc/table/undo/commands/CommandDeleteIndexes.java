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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import javafx.geometry.Orientation;
import rjc.table.data.IDataInsertDeleteColumns;
import rjc.table.data.IDataInsertDeleteRows;
import rjc.table.data.TableData;
import rjc.table.undo.IUndoCommand;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/************** Undoable command for deleting arbitrary columns or rows in table-data ************/
/*************************************************************************************************/

/**
 * Undoable command for deleting an arbitrary set of columns or rows from a table data model.
 * <p>
 * Indexes are grouped into the fewest possible contiguous runs, each resolved with a single call
 * to {@link IDataInsertDeleteColumns#deleteColumns} or {@link IDataInsertDeleteRows#deleteRows}.
 * Deletion is applied high-to-low so that lower run indexes remain valid throughout. For each
 * run, axis view-to-data mapping entries and nominal sizes are both captured and shifted
 * <em>before</em> the data deletion so that view reordering and custom widths/heights remain
 * correctly aligned with data indexes throughout. Undo re-inserts runs in the reverse order
 * (low-to-high), restoring data, sizes, and mapping to their original state.
 *
 * @see IUndoCommand
 * @see IDataInsertDeleteColumns
 * @see IDataInsertDeleteRows
 */
public class CommandDeleteIndexes implements IUndoCommand
{
  private final TableData      m_data;            // data model on which deletions are performed
  private final TableAxis      m_axis;            // axis whose sizes must stay aligned with data
  private final Orientation    m_orientation;     // HORIZONTAL for columns, VERTICAL for rows
  private final int[][]        m_runs;            // [startIndex, count] per run, high-to-low
  private final List<Object>[] m_deletedData;     // data captured per run during redo; used by undo
  private final short[][]      m_capturedSizes;   // axis sizes captured per run during redo; used by undo
  private final int[][][]      m_capturedMapping; // axis mapping pairs captured per run during redo; used by undo
  private final int            m_totalCount;      // total number of columns or rows deleted
  private String               m_text;            // lazily constructed description for undo/redo UI

  /***************************************** constructor *****************************************/
  /**
   * Creates and immediately executes a delete command for the specified data-indexes.
   * <p>
   * Indexes are grouped into contiguous runs, and deleted from highest to lowest to preserve
   * index validity throughout the operation.  Axis nominal sizes are captured and shifted in
   * lock-step with each data deletion so that custom column/row sizes remain correctly aligned
   * after the operation and after undo.
   *
   * @param view        the table view providing axis and data access
   * @param orientation {@code HORIZONTAL} to delete columns, {@code VERTICAL} to delete rows
   * @param dataIndexes non-null, non-empty array of data-indexes to delete
   */
  @SuppressWarnings( "unchecked" )
  public CommandDeleteIndexes( TableView view, Orientation orientation, int[] dataIndexes )
  {
    m_data = view.getData();
    m_orientation = orientation;
    m_axis = orientation == Orientation.HORIZONTAL ? view.getColumnsAxis() : view.getRowsAxis();

    // build contiguous runs in descending order (deletion order)
    Arrays.sort( dataIndexes );
    m_runs = buildRuns( dataIndexes );
    m_deletedData = new List[m_runs.length];
    m_capturedSizes = new short[m_runs.length][];
    m_capturedMapping = new int[m_runs.length][][];
    m_totalCount = dataIndexes.length;

    redo();
  }

  /******************************************** redo *********************************************/
  /**
   * Executes the deletion, removing runs from highest to lowest data index.
   * Axis mapping and sizes are captured and shifted before each data deletion.
   * The captured data is stored for use by {@link #undo()}.
   */
  @Override
  public void redo()
  {
    // resolve deletion function once before looping
    BiFunction<Integer, Integer, List<Object>> delete = m_orientation == Orientation.HORIZONTAL
        ? ( (IDataInsertDeleteColumns) m_data )::deleteColumns
        : ( (IDataInsertDeleteRows) m_data )::deleteRows;

    // delete runs high-to-low; capture axis mapping and sizes BEFORE data deletion so the
    // count-change signal's truncate() becomes a no-op (both components are already shifted)
    for ( int i = 0; i < m_runs.length; i++ )
    {
      m_capturedMapping[i] = m_axis.deleteMapping( m_runs[i][0], m_runs[i][1] );
      m_capturedSizes[i] = m_axis.deleteSizes( m_runs[i][0], m_runs[i][1] );
      m_deletedData[i] = delete.apply( m_runs[i][0], m_runs[i][1] );
    }
    m_data.signalTableChanged();
  }

  /******************************************** undo *********************************************/
  /**
   * Reverses the deletion by re-inserting all runs from lowest to highest data index,
   * restoring the exact data, sizes, and index mapping that existed before {@link #redo()}.
   */
  @Override
  public void undo()
  {
    // resolve insertion consumer once before looping
    BiConsumer<Integer, List<Object>> insert = m_orientation == Orientation.HORIZONTAL
        ? ( (IDataInsertDeleteColumns) m_data )::insertColumns
        : ( (IDataInsertDeleteRows) m_data )::insertRows;

    // insert runs low-to-high (reverse of m_runs order) to restore original positions;
    // restore axis sizes and mapping AFTER data insertion so the count is correct before
    // cache invalidation; mapping is restored after sizes so pixel cache is cleared once
    for ( int i = m_runs.length - 1; i >= 0; i-- )
    {
      insert.accept( m_runs[i][0], m_deletedData[i] );
      m_axis.insertSizes( m_runs[i][0], m_capturedSizes[i] );
      m_axis.insertMapping( m_runs[i][0], m_runs[i][1], m_capturedMapping[i] );
    }
    m_data.signalTableChanged();
  }

  /******************************************** text *********************************************/
  /**
   * Returns a concise description of this command for the undo/redo UI.
   *
   * @return command description, e.g. {@code "Delete 3 columns"} or {@code "Delete 1 row"}
   */
  @Override
  public String text()
  {
    if ( m_text == null )
    {
      // lazily build description from orientation and total count
      String noun = m_orientation == Orientation.HORIZONTAL ? ( m_totalCount == 1 ? "column" : "columns" )
          : ( m_totalCount == 1 ? "row" : "rows" );
      m_text = "Delete " + m_totalCount + " " + noun;
    }
    return m_text;
  }

  /******************************************* isValid *******************************************/
  /**
   * Indicates whether this command has work to perform.
   *
   * @return {@code true} if at least one index was targeted for deletion
   */
  @Override
  public boolean isValid()
  {
    return m_totalCount > 0;
  }

  /****************************************** buildRuns ******************************************/
  /**
   * Groups a sorted array of data indexes into contiguous runs, returned in descending order.
   * <p>
   * For example, {@code [2, 3, 4, 7, 9, 10]} produces
   * {@code [[9,2], [7,1], [2,3]]} (start-index and count per run, highest first).
   *
   * @param sortedIndexes ascending-sorted, duplicate-free array of data indexes
   * @return two-dimensional array where each element is {@code [startIndex, count]},
   *         ordered from highest to lowest start index
   */
  private int[][] buildRuns( int[] sortedIndexes )
  {
    if ( sortedIndexes.length == 0 )
      return new int[0][];

    var runs = new ArrayList<int[]>();
    runs.add( new int[] { sortedIndexes[0], 1 } );

    for ( int i = 1; i < sortedIndexes.length; i++ )
    {
      int[] current = runs.getLast();
      if ( sortedIndexes[i] == current[0] + current[1] )
        current[1]++; // extend current run
      else
        runs.add( new int[] { sortedIndexes[i], 1 } ); // start new run
    }

    // reverse so index 0 holds the highest run (deletion order)
    Collections.reverse( runs );
    return runs.toArray( int[][]::new );
  }

}