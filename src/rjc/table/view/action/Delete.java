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
import rjc.table.undo.commands.CommandDeleteIndexes;
import rjc.table.view.TableView;

/*************************************************************************************************/
/************************* Delete table-view columns/rows via undo command ***********************/
/*************************************************************************************************/

/**
 * Provides deletion of visible table view columns and rows with undo support.
 * <p>
 * Only operates when the data model implements {@link IDataInsertDeleteColumns} or
 * {@link IDataInsertDeleteRows} respectively. At least one column or row must remain
 * visible after deletion; the operation is blocked with a status message otherwise.
 * If the targeted column or row is selected, all selected columns or rows are deleted.
 */
public class Delete
{
  /***************************************** deleteRows ******************************************/
  /**
   * Deletes the specified row, or if that row is selected, deletes all selected rows.
   *
   * @param view    the table view to operate on
   * @param viewRow the view row index to delete (or all selected rows if that row is selected)
   * @return {@code true} if the undo command was successfully pushed to the undo stack,
   *         {@code false} otherwise
   */
  public static boolean deleteRows( TableView view, int viewRow )
  {
    // if mouse row is selected, delete all selected rows, else just delete mouse row
    var viewRows = view.getSelection().isRowSelected( viewRow ) ? view.getSelection().getSelectedRows()
        : new HashSetInt( 1 );

    // if no rows selected, delete specified row (usually mouse row)
    if ( viewRows.isEmpty() )
      viewRows.add( viewRow );

    // delete rows via undo command and push to undo stack
    var command = new CommandDeleteIndexes( view, Orientation.VERTICAL, viewRows );
    if ( command.isValid() )
      view.getSelection().clear();
    return view.getUndoStack().push( command );
  }

  /**************************************** deleteColumns ****************************************/
  /**
   * Deletes the specified column, or if that column is selected, deletes all selected columns.
   *
   * @param view       the table view to operate on
   * @param viewColumn the view column index to delete (or all selected columns if that column
   *                   is selected)
   * @return {@code true} if the undo command was successfully pushed to the undo stack,
   *         {@code false} otherwise
   */
  public static boolean deleteColumns( TableView view, int viewColumn )
  {
    // if mouse column is selected, delete all selected columns, else just delete mouse column
    var viewColumns = view.getSelection().isColumnSelected( viewColumn ) ? view.getSelection().getSelectedColumns()
        : new HashSetInt( 1 );

    // if no columns selected, delete specified column (usually mouse column)
    if ( viewColumns.isEmpty() )
      viewColumns.add( viewColumn );

    // delete columns via undo command and push to undo stack
    var command = new CommandDeleteIndexes( view, Orientation.HORIZONTAL, viewColumns );
    if ( command.isValid() )
      view.getSelection().clear();
    return view.getUndoStack().push( command );
  }
}