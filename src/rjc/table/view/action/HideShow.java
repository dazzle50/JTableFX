/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
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

import rjc.table.HashSetInt;
import rjc.table.signal.ObservableStatus.Level;
import rjc.table.undo.commands.CommandHideIndexes;
import rjc.table.undo.commands.CommandUnhideIndexes;
import rjc.table.view.TableView;

/*************************************************************************************************/
/********************************* Hide table-view columns/rows **********************************/
/*************************************************************************************************/

public class HideShow
{
  /****************************************** hideRows *******************************************/
  /**
   * Hide the specified row, or if that row is selected, hide all selected rows.
   * Cannot hide all rows (at least one row must remain visible).
   * 
   * @param view TableView to operate on
   * @param viewRow  row index to hide (or all selected rows if that row is selected)
   * @return true if undo-command successfully pushed to undo-stack, else false
   */
  public static boolean hideRows( TableView view, int viewRow )
  {
    // if mouse row is selected, hide all selected rows, else just hide mouse row
    var viewRows = view.getSelection().isRowSelected( viewRow ) ? view.getSelection().getSelectedRows()
        : new HashSetInt( 1 );

    // prevent hiding all rows
    if ( viewRows == null || view.getSelection().areAllVisibleRowsSelected()
        || view.getRowsAxis().getFirstVisible() == view.getRowsAxis().getLastVisible() )
    {
      view.getStatus().update( Level.INFO, "Cannot hide all rows" );
      view.getStatus().clearAfterMillisecs( 2500 );
      return false;
    }

    // if no rows selected, hide specified row (usually mouse row)
    if ( viewRows.isEmpty() )
      viewRows.add( viewRow );

    // hide the rows via undo-command and add to undostack
    var command = new CommandHideIndexes( view, view.getRowsAxis(), viewRows );
    return view.getUndoStack().push( command );
  }

  /***************************************** hideColumns *****************************************/
  /**
   * Hide the specified column, or if that column is selected, hide all selected columns.
   * Cannot hide all columns (at least one column must remain visible).
   *
   * @param view TableView to operate on
   * @param viewColumn column index to hide (or all selected columns if that column is selected)
   * @return true if undo-command successfully pushed to undo-stack, else false
   */
  public static boolean hideColumns( TableView view, int viewColumn )
  {
    // if mouse column is selected, hide all selected columns, else just hide mouse column
    var viewColumns = view.getSelection().isColumnSelected( viewColumn ) ? view.getSelection().getSelectedColumns()
        : new HashSetInt( 1 );

    // prevent hiding all columns
    if ( viewColumns == null || view.getSelection().areAllVisibleColumnsSelected()
        || view.getColumnsAxis().getFirstVisible() == view.getColumnsAxis().getLastVisible() )
    {
      view.getStatus().update( Level.INFO, "Cannot hide all columns" );
      view.getStatus().clearAfterMillisecs( 2500 );
      return false;
    }

    // if no columns selected, hide specified column (usually mouse column)
    if ( viewColumns.isEmpty() )
      viewColumns.add( viewColumn );

    // hide the columns via undo-command and add to undostack
    var command = new CommandHideIndexes( view, view.getColumnsAxis(), viewColumns );
    return view.getUndoStack().push( command );
  }

  /**************************************** showColumns ******************************************/
  /**
   * Unhide any hidden columns that are currently selected.
   *  
   * @param view TableView to operate on
   * @return true if undo-command successfully pushed to undo-stack, else false
   */
  public static boolean showColumns( TableView view )
  {
    // get currently selected columns
    var viewColumns = view.getSelection().getSelectedColumns();
    if ( viewColumns == null ) // all visible columns are selected so unhide all
      viewColumns = view.getColumnsAxis().getHiddenDataIndexes();

    // unhide the columns via undo-command and add to undostack
    var command = new CommandUnhideIndexes( view, view.getColumnsAxis(), viewColumns );
    return view.getUndoStack().push( command );
  }

  /****************************************** showRows ******************************************/
  /**
   * Unhide any hidden rows that are currently selected.
   *  
   * @param view TableView to operate on
   * @return true if undo-command successfully pushed to undo-stack, else false
   */
  public static boolean showRows( TableView view )
  {
    // get currently selected rows
    var viewRows = view.getSelection().getSelectedRows();
    if ( viewRows == null ) // all visible rows are selected so unhide all
      viewRows = view.getRowsAxis().getHiddenDataIndexes();

    // unhide the rows via undo-command and add to undostack
    var command = new CommandUnhideIndexes( view, view.getRowsAxis(), viewRows );
    return view.getUndoStack().push( command );
  }
}