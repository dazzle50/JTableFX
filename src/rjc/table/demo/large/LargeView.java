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

package rjc.table.demo.large;

import rjc.table.data.TableData;
import rjc.table.view.TableView;
import rjc.table.view.cell.CellDrawer;

/*************************************************************************************************/
/********************** Example customised table view for extra large table **********************/
/*************************************************************************************************/

public class LargeView extends TableView
{

  /**************************************** constructor ******************************************/
  public LargeView( TableData data, String name )
  {
    // construct customised table view
    super( data, name );
    getColumnsAxis().setHeaderNominalSize( 60 );

    // when mouse moved to new cell, redraw table to move shading
    getMouseCell().addListener( ( sender, msg ) -> redraw() );
  }

  /**************************************** getCellDrawer ****************************************/
  @Override
  public CellDrawer getCellDrawer()
  {
    // return new instance of class that draws table cells
    return new LargeCellDrawer();
  }

}