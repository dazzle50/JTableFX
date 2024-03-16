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

import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import rjc.table.Utils;
import rjc.table.view.TableView;

/*************************************************************************************************/
/************************** Handles mouse move events from table-view ****************************/
/*************************************************************************************************/

public class MouseMoved implements EventHandler<MouseEvent>
{

  /******************************************* handle ********************************************/
  @Override
  public void handle( MouseEvent event )
  {
    // handle mouse movement events (no buttons pressed)
    event.consume();
    int x = (int) event.getX();
    int y = (int) event.getY();
    TableView view = TableView.getEventView( event.getSource() );

    Utils.trace( event );

    // update mouse cell position and cursor
    view.getMouseCell().setXY( x, y, true );
  }

}