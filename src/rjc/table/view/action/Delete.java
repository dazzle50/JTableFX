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
import javafx.scene.control.Alert;
import javafx.stage.Window;
import rjc.table.HashSetInt;
import rjc.table.data.IDataInsertDeleteColumns;
import rjc.table.data.IDataInsertDeleteRows;
import rjc.table.undo.commands.CommandDeleteIndexes;
import rjc.table.view.TableView;

/*************************************************************************************************/
/************************* Delete table-data columns/rows via undo command ***********************/
/*************************************************************************************************/

/**
 * Utility class providing deletion of table-data columns and rows with undo support.
 * <p>
 * Callers supply a data model implementing {@link IDataInsertDeleteColumns} or
 * {@link IDataInsertDeleteRows}. If the targeted index is part of the current selection,
 * all selected columns or rows are deleted; otherwise only the specified index is deleted.
 */
public class Delete
{
  /***************************************** constructor *****************************************/
  private Delete()
  {
    // private constructor to prevent instantiation of this utility class
  }

  /**************************************** deleteRows *******************************************/
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
    var viewRows = getViewIndexes( view.getSelection().isRowSelected( viewRow ), view.getSelection().getSelectedRows(),
        viewRow, view.getData().getRowCount() );
    var dataRows = view.getRowsAxis().getDataIndexes( viewRows );

    // check rows can be deleted, show warning and abort if not
    String deletable = ( (IDataInsertDeleteRows) view.getData() ).checkRowsDeletable( dataRows );
    if ( deletable != null )
    {
      showWarning( view.getScene().getWindow(), deletable );
      return false;
    }

    // push delete command to undo stack, clearing selection if command is valid
    var command = new CommandDeleteIndexes( view, Orientation.VERTICAL, dataRows );
    if ( command.isValid() )
      view.getSelection().clear();
    return view.getUndoStack().push( command );
  }

  /*************************************** deleteColumns *****************************************/
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
    var viewColumns = getViewIndexes( view.getSelection().isColumnSelected( viewColumn ),
        view.getSelection().getSelectedColumns(), viewColumn, view.getData().getColumnCount() );
    var dataColumns = view.getColumnsAxis().getDataIndexes( viewColumns );

    // check columns can be deleted, show warning and abort if not
    String deletable = ( (IDataInsertDeleteColumns) view.getData() ).checkColumnsDeletable( dataColumns );
    if ( deletable != null )
    {
      showWarning( view.getScene().getWindow(), deletable );
      return false;
    }

    // push delete command to undo stack, clearing selection if command is valid
    var command = new CommandDeleteIndexes( view, Orientation.HORIZONTAL, dataColumns );
    if ( command.isValid() )
      view.getSelection().clear();
    return view.getUndoStack().push( command );
  }

  /*************************************** getViewIndexes ****************************************/
  /**
   * Returns the set of view indexes that should be operated on, taking into account
   * the current selection state and fallback index.
   * <p>
   * If {@code isSelected} is {@code false}, returns a single-element set containing
   * {@code fallbackIndex}. If {@code isSelected} is {@code true} and {@code selected} is
   * non-null, returns {@code selected} directly. If {@code selected} is {@code null}
   * (indicating all indexes are selected), returns a new set containing every index from
   * {@code 0} to {@code totalCount − 1}.
   *
   * @param isSelected    whether the fallback index is within the current selection
   * @param selected      the currently selected view indexes, or {@code null} if all are selected
   * @param fallbackIndex the index to use when nothing is selected
   * @param totalCount    the total number of indexes; used only when all are selected
   * @return a non-empty {@link HashSetInt} of view indexes to delete
   */
  private static HashSetInt getViewIndexes( boolean isSelected, HashSetInt selected, int fallbackIndex, int totalCount )
  {
    if ( !isSelected )
    {
      // nothing selected — operate on the single fallback index only
      var single = new HashSetInt( 1 );
      single.add( fallbackIndex );
      return single;
    }
    if ( selected != null )
      return selected;
    // null means all indexes selected — expand explicitly
    var all = new HashSetInt( totalCount );
    for ( int i = 0; i < totalCount; i++ )
      all.add( i );
    return all;
  }

  /**************************************** showWarning ******************************************/
  /**
   * Displays a modal warning alert titled "Cannot delete" with the given message.
   *
   * @param owner   the owner window for the alert dialog
   * @param message the warning text to display
   */
  private static void showWarning( Window owner, String message )
  {
    var alert = new Alert( Alert.AlertType.WARNING );
    alert.initOwner( owner );
    alert.setTitle( "Cannot delete" );
    alert.setHeaderText( null );
    alert.setContentText( message );
    alert.showAndWait();
  }

}