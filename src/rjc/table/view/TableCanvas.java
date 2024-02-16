/**************************************************************************
 *  Copyright (C) 2024 by Richard Crook                                   *
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

import javafx.scene.canvas.Canvas;

/*************************************************************************************************/
/************** Canvas showing the table headers & body cells + BLANK excess space ***************/
/*************************************************************************************************/

public class TableCanvas extends Canvas
{

  /**************************************** constructor ******************************************/
  public TableCanvas( TableView view )
  {
    // TODO Auto-generated constructor stub
  }

  /*************************************** getColumnAxis *****************************************/
  public TableAxis getColumnAxis()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /***************************************** getRowAxis ******************************************/
  public TableAxis getRowAxis()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /***************************************** getOverlay ******************************************/
  public TableOverlay getOverlay()
  {
    // TODO Auto-generated method stub
    return new TableOverlay();
  }

}
