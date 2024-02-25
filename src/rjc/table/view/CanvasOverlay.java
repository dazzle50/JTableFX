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
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.FontSmoothingType;

/*************************************************************************************************/
/****************** Canvas overlay for table-views (highlights selection etc) ********************/
/*************************************************************************************************/

public class CanvasOverlay extends Canvas
{
  private TableView       m_view;
  private GraphicsContext m_gc;

  final public static int MIN_COORD = -999;  // highlighting coordinate limit
  final public static int MAX_COORD = 99999; // highlighting coordinate limit

  /**************************************** constructor ******************************************/
  public CanvasOverlay( TableView tableView )
  {
    // prepare canvas overlay
    m_view = tableView;
    m_gc = getGraphicsContext2D();

    m_gc.setFontSmoothingType( FontSmoothingType.LCD );
  }

  /****************************************** redrawNow ******************************************/
  public void redrawNow()
  {
    // clip overlay drawing to table body
    // TODO
  }
}
