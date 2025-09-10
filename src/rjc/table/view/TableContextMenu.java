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

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import rjc.table.Utils;

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

  /**************************************** constructor ******************************************/
  /**
   * Creates a new context menu for the specified table view.
   * The menu items displayed depend on the mouse position and cell type.
   * 
   * @param view the table view that this context menu belongs to
   */
  public TableContextMenu( TableView view )
  {
    // get the cell coordinates where the context menu was triggered
    int mouseRow = view.getMouseCell().getRow();
    int mouseCol = view.getMouseCell().getColumn();
    Utils.trace( "CONTEXT MENU !!!", mouseRow, mouseCol );

    // construct menu items based on whether header or body cell
    var item = new MenuItem( "TODO" );
    getItems().addAll( item, new SeparatorMenuItem() );
  }

}
