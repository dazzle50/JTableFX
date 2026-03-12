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
import java.util.function.BiFunction;

import javafx.geometry.Orientation;
import rjc.table.HashSetInt;
import rjc.table.data.IDataInsertDeleteColumns;
import rjc.table.data.IDataInsertDeleteRows;
import rjc.table.data.TableData;
import rjc.table.undo.IUndoCommand;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/************** Undoable command for inserting arbitrary columns or rows in table-data ***********/
/*************************************************************************************************/

/**
 * Undoable command for inserting new columns or rows into a table data model.
 * <p>
 * View-based indexes are converted to data-based indexes and grouped into contiguous runs.
 * Insertion is applied high-to-low so that lower run indexes remain valid throughout; each run
 * inserts model-default content via a null-element list. Axis sizes and mapping are updated
 * <em>after</em> each data insertion so the count signal fires with the axis already consistent.
 * Undo deletes the inserted runs low-to-high — the stored data indexes remain correct because
 * each low deletion shifts the next inserted row back to its original stored position — capturing
 * and removing axis mapping and sizes before each data deletion, mirroring
 * {@link CommandDeleteIndexes#redo()}.
 *
 * @see IUndoCommand
 * @see IDataInsertDeleteColumns
 * @see IDataInsertDeleteRows
 */
public class CommandInsertIndexes implements IUndoCommand
{
  private final TableData   m_data;        // data model on which insertions are performed
  private final TableAxis   m_axis;        // axis whose sizes must stay aligned with data
  private final Orientation m_orientation; // HORIZONTAL for columns, VERTICAL for rows
  private final int[][]     m_runs;        // [startIndex, count] per run, high-to-low
  private final int         m_totalCount;  // total number of columns or rows inserted
  private String            m_text;        // lazily constructed description for undo/redo UI

  /***************************************** constructor *****************************************/
  /**
   * Creates and immediately executes an insert command for the specified view-based indexes.
   * <p>
   * View indexes are resolved to data indexes, grouped into contiguous runs, and inserted
   * from highest to lowest to preserve index validity throughout. Each inserted column or row
   * is initialised with model-determined default content.
   *
   * @param view        the table view providing axis and data access
   * @param orientation {@code HORIZONTAL} to insert columns, {@code VERTICAL} to insert rows
   * @param viewIndexes non-null, non-empty set of view-based indexes before which to insert
   */
  public CommandInsertIndexes( TableView view, Orientation orientation, HashSetInt viewIndexes )
  {
    m_data = view.getData();
    m_orientation = orientation;

    // resolve the correct axis for view->data index conversion and size management
    m_axis = orientation == Orientation.HORIZONTAL ? view.getColumnsAxis() : view.getRowsAxis();

    // convert view indexes to sorted data indexes
    int[] viewArray = viewIndexes.toArray();
    int[] raw = new int[viewArray.length];
    for ( int i = 0; i < viewArray.length; i++ )
      raw[i] = m_axis.getDataIndex( viewArray[i] );
    Arrays.sort( raw );

    // build contiguous runs in descending order (insertion order)
    m_runs = buildRuns( raw );
    m_totalCount = raw.length;

    redo();
  }

  /******************************************** redo *********************************************/
  /**
   * Executes the insertion, adding default-content runs from highest to lowest data index.
   * Axis sizes and mapping are updated after each data insertion.
   */
  @Override
  public void redo()
  {
    // resolve insertion function once before looping
    BiFunction<Integer, List<Object>, Boolean> insert = m_orientation == Orientation.HORIZONTAL
        ? ( (IDataInsertDeleteColumns) m_data )::insertColumns
        : ( (IDataInsertDeleteRows) m_data )::insertRows;

    // insert runs high-to-low so lower index positions remain valid throughout;
    // update axis after data insertion so the count-change signal fires with axis consistent
    for ( int i = 0; i < m_runs.length; i++ )
    {
      // null elements instruct the data model to initialise with default content
      insert.apply( m_runs[i][0], new ArrayList<>( Collections.nCopies( m_runs[i][1], null ) ) );
      m_axis.insertSizes( m_runs[i][0], m_runs[i][1] );
      m_axis.insertMapping( m_runs[i][0], m_runs[i][1] );
    }
    m_data.signalTableChanged();
  }

  /******************************************** undo *********************************************/
  /**
   * Reverses the insertion by deleting all runs from lowest to highest data index.
   * Axis mapping and sizes are removed before each data deletion, mirroring
   * {@link CommandDeleteIndexes#redo()}.
   */
  @Override
  public void undo()
  {
    // resolve deletion function once before looping
    BiFunction<Integer, Integer, List<Object>> delete = m_orientation == Orientation.HORIZONTAL
        ? ( (IDataInsertDeleteColumns) m_data )::deleteColumns
        : ( (IDataInsertDeleteRows) m_data )::deleteRows;

    // delete runs low-to-high (reverse of m_runs order); stored data indexes remain correct
    // because each low deletion shifts the next inserted entry back to its original position;
    // remove axis mapping and sizes before data deletion to keep alignment throughout
    for ( int i = m_runs.length - 1; i >= 0; i-- )
    {
      m_axis.deleteMapping( m_runs[i][0], m_runs[i][1] );
      m_axis.deleteSizes( m_runs[i][0], m_runs[i][1] );
      delete.apply( m_runs[i][0], m_runs[i][1] );
    }
    m_data.signalTableChanged();
  }

  /******************************************** text *********************************************/
  /**
   * Returns a concise description of this command for the undo/redo UI.
   *
   * @return command description, e.g. {@code "Insert 3 columns"} or {@code "Insert 1 row"}
   */
  @Override
  public String text()
  {
    if ( m_text == null )
    {
      // lazily build description from orientation and total count
      String noun = m_orientation == Orientation.HORIZONTAL ? ( m_totalCount == 1 ? "column" : "columns" )
          : ( m_totalCount == 1 ? "row" : "rows" );
      m_text = "Insert " + m_totalCount + " " + noun;
    }
    return m_text;
  }

  /******************************************* isValid *******************************************/
  /**
   * Indicates whether this command has work to perform.
   *
   * @return {@code true} if at least one index was targeted for insertion
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

    // reverse so index 0 holds the highest run (insertion order)
    Collections.reverse( runs );
    return runs.toArray( int[][]::new );
  }
}