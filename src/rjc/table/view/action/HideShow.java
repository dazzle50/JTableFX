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

import rjc.table.HashSetInt;
import rjc.table.undo.commands.CommandHideIndexes;
import rjc.table.undo.commands.CommandUnhideIndexes;
import rjc.table.view.TableView;

/*************************************************************************************************/
/********************************* Hide table-view columns/rows **********************************/
/*************************************************************************************************/

/**
 * Performs undoable row and column visibility changes using current view-coordinate selection.
 * Hide uses the triggered member unless it is fully selected, in which case it uses the selected
 * group. Unhide additionally reveals all hidden members when every visible member is fully selected.
 *
 * <p>Commands resolve view indexes through the corresponding axis and execute during construction;
 * only commands that changed visibility enter the view's undo stack. These actions do not prepare
 * selection, validate a popup request or finish editing. Call on the JavaFX Application Thread.</p>
 */
public class HideShow
{
  /****************************************** hideRows *******************************************/
  /**
   * Hides the supplied row, or all fully selected rows if that row has complete visible-column
   * coverage. A selected-index result of null expands to all currently visible rows. Only actual
   * visibility changes are recorded by the undo command.
   * 
   * @param view the owning table view
   * @param viewRow a valid body row view index, used as the fallback or selected-group anchor
   * @return true if a visibility change was recorded on the undo stack; false if no row changed
   */
  public static boolean hideRows( TableView view, int viewRow )
  {
    // if mouse row is selected, hide all selected rows, else just hide mouse row
    var viewRows = view.getSelection().isRowSelected( viewRow ) ? view.getSelection().getSelectedRows()
        : new HashSetInt( 1 );

    // if all visible rows are selected, hide all visible rows
    if ( viewRows == null )
    {
      int count = view.getRowsAxis().getCount();
      viewRows = new HashSetInt( view.getRowsAxis().getCount() );
      for ( int row = 0; row < count; row++ )
        if ( view.getRowsAxis().isVisible( row ) )
          viewRows.add( row );
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
   * Hides the supplied column, or all fully selected columns if that column has complete visible-row
   * coverage. A selected-index result of null expands to all currently visible columns. Only actual
   * visibility changes are recorded by the undo command.
   *
   * @param view the owning table view
   * @param viewColumn a valid body column view index, used as the fallback or selected-group anchor
   * @return true if a visibility change was recorded on the undo stack; false if no column changed
   */
  public static boolean hideColumns( TableView view, int viewColumn )
  {
    // if mouse column is selected, hide all selected columns, else just hide mouse column
    var viewColumns = view.getSelection().isColumnSelected( viewColumn ) ? view.getSelection().getSelectedColumns()
        : new HashSetInt( 1 );

    // if all visible columns are selected, hide all visible columns
    if ( viewColumns == null )
    {
      int count = view.getColumnsAxis().getCount();
      viewColumns = new HashSetInt( view.getColumnsAxis().getCount() );
      for ( int col = 0; col < count; col++ )
        if ( view.getColumnsAxis().isVisible( col ) )
          viewColumns.add( col );
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
   * Unhides selected hidden columns. If the selected-index result is null or every visible column
   * is fully selected, unhides every hidden column, including members outside explicit selection.
   * This convenience does not expand selection membership for other commands such as Delete.
   *  
   * @param view the owning table view
   * @return true if an unhide command was recorded; false if no column became visible
   */
  public static boolean showColumns( TableView view )
  {
    // get currently selected columns
    var viewColumns = view.getSelection().getSelectedColumns();
    if ( viewColumns == null || view.getSelection().areAllVisibleColumnsSelected() ) // visible coverage means unhide all
      viewColumns = view.getColumnsAxis().getHiddenViewIndexes();

    // unhide the columns via undo-command and add to undostack
    var command = new CommandUnhideIndexes( view, view.getColumnsAxis(), viewColumns );
    return view.getUndoStack().push( command );
  }

  /****************************************** showRows ******************************************/
  /**
   * Unhides selected hidden rows. If the selected-index result is null or every visible row is
   * fully selected, unhides every hidden row, including members outside explicit selection.
   * This convenience does not expand selection membership for other commands such as Delete.
   *  
   * @param view the owning table view
   * @return true if an unhide command was recorded; false if no row became visible
   */
  public static boolean showRows( TableView view )
  {
    // get currently selected rows
    var viewRows = view.getSelection().getSelectedRows();
    if ( viewRows == null || view.getSelection().areAllVisibleRowsSelected() ) // visible coverage means unhide all
      viewRows = view.getRowsAxis().getHiddenViewIndexes();

    // unhide the rows via undo-command and add to undostack
    var command = new CommandUnhideIndexes( view, view.getRowsAxis(), viewRows );
    return view.getUndoStack().push( command );
  }
}
