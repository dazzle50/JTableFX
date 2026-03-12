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

import javafx.geometry.Orientation;
import rjc.table.HashSetInt;
import rjc.table.data.IDataInsertDeleteColumns;
import rjc.table.data.IDataInsertDeleteRows;
import rjc.table.undo.commands.CommandInsertIndexes;
import rjc.table.view.TableView;

/*************************************************************************************************/
/************************* Insert table-data columns/rows via undo command ***********************/
/*************************************************************************************************/

/**
 * Provides insertion of new visible table-data columns and rows with undo support.
 * <p>
 * Only operates when the data model implements {@link IDataInsertDeleteColumns} or
 * {@link IDataInsertDeleteRows} respectively.
 * If the targeted column or row is selected, one new column or row is inserted before each
 * selected index (matching Excel behaviour). Otherwise a single column or row is inserted
 * before the specified index.
 */
public class Insert
{
  /***************************************** insertRows ******************************************/
  /**
   * Inserts one row before the specified row, or if that row is selected, inserts one row
   * before each selected row.
   *
   * @param view    the table view to operate on
   * @param viewRow the view row index before which to insert (or anchor for selected rows)
   * @return {@code true} if the undo command was successfully pushed to the undo stack,
   *         {@code false} otherwise
   */
  public static boolean insertRows( TableView view, int viewRow )
  {
    HashSetInt viewRows;
    if ( view.getSelection().isRowSelected( viewRow ) )
    {
      // null means all rows selected — populate explicitly with every view row index
      viewRows = view.getSelection().getSelectedRows();
      if ( viewRows == null )
      {
        int count = view.getData().getRowCount();
        viewRows = new HashSetInt( count );
        for ( int i = 0; i < count; i++ )
          viewRows.add( i );
      }
    }
    else
      viewRows = new HashSetInt( 1 );

    // if no rows selected, insert before specified row (usually mouse row)
    if ( viewRows.isEmpty() )
      viewRows.add( viewRow );

    // insert rows via undo command and push to undo stack
    var command = new CommandInsertIndexes( view, Orientation.VERTICAL, viewRows );
    if ( command.isValid() )
      view.getSelection().clear();
    return view.getUndoStack().push( command );
  }

  /**************************************** insertColumns ****************************************/
  /**
   * Inserts one column before the specified column, or if that column is selected, inserts one
   * column before each selected column.
   *
   * @param view       the table view to operate on
   * @param viewColumn the view column index before which to insert (or anchor for selected columns)
   * @return {@code true} if the undo command was successfully pushed to the undo stack,
   *         {@code false} otherwise
   */
  public static boolean insertColumns( TableView view, int viewColumn )
  {
    HashSetInt viewColumns;
    if ( view.getSelection().isColumnSelected( viewColumn ) )
    {
      // null means all columns selected — populate explicitly with every view column index
      viewColumns = view.getSelection().getSelectedColumns();
      if ( viewColumns == null )
      {
        int count = view.getData().getColumnCount();
        viewColumns = new HashSetInt( count );
        for ( int i = 0; i < count; i++ )
          viewColumns.add( i );
      }
    }
    else
      viewColumns = new HashSetInt( 1 );

    // if no columns selected, insert before specified column (usually mouse column)
    if ( viewColumns.isEmpty() )
      viewColumns.add( viewColumn );

    // insert columns via undo command and push to undo stack
    var command = new CommandInsertIndexes( view, Orientation.HORIZONTAL, viewColumns );
    if ( command.isValid() )
      view.getSelection().clear();
    return view.getUndoStack().push( command );
  }

}