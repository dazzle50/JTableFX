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

package rjc.table.view.events;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/*************************************************************************************************/
/********************* Handles mouse button released events from table-view **********************/
/*************************************************************************************************/

public class MouseReleased extends MouseEventHandler
{
  /******************************************* handle ********************************************/
  @Override
  public void handle( MouseEvent event )
  {
    // check for ending resize before updating cursor
    super.handle( event );
    if ( button == MouseButton.PRIMARY )
    {
      // check if ending resize column or row

      // check if ending column/row reordering
    }

    // update mouse cell position and cursor
    view.getMouseCell().setXY( x, y, true );
    view.getHorizontalScrollBar().stopAnimationStartEnd();
    view.getVerticalScrollBar().stopAnimationStartEnd();
  }

}
