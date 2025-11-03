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
import rjc.table.undo.commands.CommandHideIndexes;
import rjc.table.undo.commands.CommandUnhideAllIndexes;
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
   * @param row  row index to hide (or all selected rows if that row is selected)
   * @return true if undo-command successfully pushed to undo-stack, else false
   */
  public static boolean hideRows( TableView view, int row )
  {
    // if mouse row is selected, hide all selected rows, else just hide mouse row
    var indexes = view.getSelection().isRowSelected( row ) ? view.getSelection().getSelectedRows() : new HashSetInt(1);

    if ( indexes == null || view.getSelection().areAllVisibleRowsSelected() )
      return false; // cannot hide all rows

    if ( view.getRowsAxis().getFirstVisible() == view.getRowsAxis().getLastVisible() )
      return false; // cannot hide last remaining row

    if ( indexes.isEmpty() )
      indexes.add( row );

    // hide the rows via undo-command and add to undostack
    var command = new CommandHideIndexes( view, view.getRowsAxis(), indexes );
    return view.getUndoStack().push( command );
  }

  /***************************************** hideColumns *****************************************/
  /**
   * Hide the specified column, or if that column is selected, hide all selected columns.
   * Cannot hide all columns (at least one column must remain visible).
   *
   * @param view TableView to operate on
   * @param column column index to hide (or all selected columns if that column is selected)
   * @return true if undo-command successfully pushed to undo-stack, else false
   */
  public static boolean hideColumns( TableView view, int column )
  {
    // if mouse column is selected, hide all selected columns, else just hide mouse column
    var indexes = view.getSelection().isColumnSelected( column ) ? view.getSelection().getSelectedColumns()
        : new HashSetInt(1);

    if ( indexes == null || view.getSelection().areAllVisibleColumnsSelected() )
      return false; // cannot hide all columns

    if ( view.getColumnsAxis().getFirstVisible() == view.getColumnsAxis().getLastVisible() )
      return false; // cannot hide last remaining column

    if ( indexes.isEmpty() )
      indexes.add( column );

    // hide the columns via undo-command and add to undostack
    var command = new CommandHideIndexes( view, view.getColumnsAxis(), indexes );
    return view.getUndoStack().push( command );
  }

  /**************************************** showAllColumns ****************************************/
  /**
   * Unhide all hidden columns in the specified TableView
   * 
   * @param view TableView to operate on
   * @return true if undo-command successfully pushed to undo-stack, else false
   */
  public static boolean showAllColumns( TableView view )
  {
    // unhide all hidden columns via undo-command and add to undostack
    var command = new CommandUnhideAllIndexes( view, view.getColumnsAxis() );
    return view.getUndoStack().push( command );
  }

  /****************************************** showAllRows *****************************************/
  /**
   * Unhide all hidden rows in the specified TableView
   * 
   * @param view TableView to operate on
   * @return true if undo-command successfully pushed to undo-stack, else false
   */
  public static boolean showAllRows( TableView view )
  {
    // unhide all hidden rows via undo-command and add to undostack
    var command = new CommandUnhideAllIndexes( view, view.getRowsAxis() );
    return view.getUndoStack().push( command );
  }
}