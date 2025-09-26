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

import java.util.HashSet;

import rjc.table.Utils;
import rjc.table.undo.commands.CommandHideIndexes;
import rjc.table.view.TableView;

/*************************************************************************************************/
/************************ Filter table-view columns/rows via undo command ************************/
/*************************************************************************************************/

public class Filter
{
  /************************************* columnTextContains **************************************/
  public static void columnTextContains( TableView view, int mouseCol, String text )
  {
    Utils.trace( ">>>>>>>>>>>", view, mouseCol, text );

    // TODO implement column filtering
    var toHide = new HashSet<Integer>();
    int dataColumn = view.getColumnsAxis().getDataIndex( mouseCol );
    int nextRow = view.getRowsAxis().getFirstVisible();
    int viewRow;

    // loop round all visible rows
    do
    {
      viewRow = nextRow;
      int dataRow = view.getRowsAxis().getDataIndex( viewRow );

      var value = view.getData().getValue( dataColumn, dataRow );
      var valueText = value == null ? "" : value.toString();

      if ( valueText.indexOf( text ) < 0 )
        toHide.add( viewRow );

      nextRow = view.getRowsAxis().getNextVisible( viewRow );
    }
    while ( nextRow != viewRow );

    // hide the columns that don't match via undo-command and add to undostack
    Utils.trace( ">>>>>>>>>>>", toHide );
    var command = new CommandHideIndexes( view, view.getRowsAxis(), toHide );
    view.getUndoStack().push( command );
  }
}
