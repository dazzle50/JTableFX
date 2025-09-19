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

package rjc.table.view;

import java.util.HashSet;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import rjc.table.undo.commands.CommandHideIndexes;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/************************ Context menu for table-view header & body cells ************************/
/*************************************************************************************************/

/**
 * Custom context menu that provides contextual actions for table cells.
 * Displays different menu items based on whether the right-click occurred
 * on header cells or body cells, offering operations like insert, delete,
 * hide/unhide, filter, sort, and clipboard operations.
 */
public class TableContextMenu extends ContextMenu
{
  private TableView        m_view;                          // the table view
  private int              m_mouseRow;                      // row index where context menu triggered
  private int              m_mouseCol;                      // column index where context menu triggered

  private static final int FIRSTCELL = TableAxis.FIRSTCELL; // index of first body row/column
  private static final int HEADER    = TableAxis.HEADER;    // index of header row/column
  private static final int AFTER     = TableAxis.AFTER;     // index of after-last row/column

  /******************************************** show *********************************************/
  public static void show( TableView view, double x, double y )
  {
    // get the cell coordinates where the context menu was triggered
    int mouseRow = view.getMouseCell().getRow();
    int mouseCol = view.getMouseCell().getColumn();

    // if after last row/column then no context menu
    if ( mouseRow == AFTER || mouseCol == AFTER )
      return;

    // generate the context-menu and show
    var menu = new TableContextMenu( view, mouseRow, mouseCol );
    menu.show( view.getScene().getWindow(), x, y );
  }

  /**************************************** constructor ******************************************/
  private TableContextMenu( TableView view, int mouseRow, int mouseCol )
  {
    // prepare initially empty context menu
    m_view = view;
    m_mouseRow = mouseRow;
    m_mouseCol = mouseCol;

    // add menu items depending on where context menu was triggered
    if ( mouseRow == HEADER && mouseCol == HEADER )
      addTODO();

    if ( mouseRow == HEADER && mouseCol >= FIRSTCELL )
      addHideColumn().addSeparator().addShowAllColumns();

    if ( mouseCol == HEADER && mouseRow >= FIRSTCELL )
      addTODO();

    if ( mouseRow >= FIRSTCELL && mouseCol >= FIRSTCELL )
      addTODO();
  }

  /****************************************** addTODO ********************************************/
  private TableContextMenu addTODO()
  {
    // add a disabled TODO menu item
    MenuItem item = new MenuItem( "TODO" );
    item.setDisable( true );
    getItems().add( item );
    return this;
  }

  /**************************************** addSeparator *****************************************/
  private TableContextMenu addSeparator()
  {
    // add a separator menu item
    getItems().add( new SeparatorMenuItem() );
    return this;
  }

  /************************************* addShowAllColumns ***************************************/
  private TableContextMenu addShowAllColumns()
  {
    // add a show all columns menu item
    MenuItem item = new MenuItem( "Show All Columns" );
    getItems().add( item );
    return this;
  }

  /*************************************** addHideColumn *****************************************/
  private TableContextMenu addHideColumn()
  {
    // add a hide column(s) menu item
    MenuItem item = new MenuItem( "Hide Column(s)" );

    item.setOnAction( event ->
    {
      // get set of columns to hide
      var indexes = m_view.getSelection().isColumnSelected( m_mouseCol ) ? m_view.getSelection().getSelectedColumns()
          : new HashSet<Integer>( 1 );
      if ( indexes.isEmpty() )
        indexes.add( m_mouseCol );

      // hide the columns via undo-command and add to undostack
      var command = new CommandHideIndexes( m_view, m_view.getColumnsAxis(), indexes );
      m_view.getUndoStack().push( command );
    } );

    getItems().add( item );
    return this;
  }

}
