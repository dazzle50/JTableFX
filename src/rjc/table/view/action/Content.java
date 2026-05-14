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

import rjc.table.Utils;
import rjc.table.undo.commands.CommandSetNull;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/************************** Functionality for multi-cell content actions *************************/
/*************************************************************************************************/

public class Content
{

  /******************************************* delete ********************************************/
  public static void delete( TableView view )
  {
    // attempt to set all table-view selected cells to null
    var command = new CommandSetNull( view );
    var data = view.getData();
    int maxC = data.getColumnCount() - 1;
    int maxR = data.getRowCount() - 1;

    view.getSelection().getAreas().forEach( area ->
    {
      // get selected area
      int c1 = Utils.clamp( area[0], TableAxis.FIRSTCELL, maxC );
      int r1 = Utils.clamp( area[1], TableAxis.FIRSTCELL, maxR );
      int c2 = Utils.clamp( area[2], TableAxis.FIRSTCELL, maxC );
      int r2 = Utils.clamp( area[3], TableAxis.FIRSTCELL, maxR );

      // generate list of selected visible indexes
      var columnIndexes = view.getColumnsAxis().getAllVisible( c1, c2 );
      var rowIndexes = view.getRowsAxis().getAllVisible( r1, r2 );

      // for each visible selected cell if cell has editor and successfully deleted, add to command
      var cell = view.getCellDrawer();
      for ( int row : rowIndexes )
        for ( int col : columnIndexes )
        {
          cell.setIndex( view, col, row );
          cell.getValueVisual();
          if ( view.getCellEditor( cell ) != null )
            command.add( col, row );
        }
    } );

    // push command onto stack
    view.getUndoStack().push( command );
  }
}
