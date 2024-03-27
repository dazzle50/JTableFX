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
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import rjc.table.view.TableView;
import rjc.table.view.cell.MousePosition;
import rjc.table.view.cell.ViewPosition;

/*************************************************************************************************/
/***************************** Collect data for mouse event handlers *****************************/
/*************************************************************************************************/

public class MouseEventHandler implements EventHandler<MouseEvent>
{
  public int           x;
  public int           y;
  public TableView     view;
  public MouseButton   button;
  public ViewPosition  select;
  public ViewPosition  focus;
  public MousePosition mouse;
  public Cursor        cursor;
  public boolean       shift;
  public boolean       control;
  public boolean       alt;
  public MouseEvent    event;

  /******************************************* handle ********************************************/
  @Override
  public void handle( MouseEvent mouseEvent )
  {
    // collect data for mouse event handlers
    mouseEvent.consume();
    x = (int) mouseEvent.getX();
    y = (int) mouseEvent.getY();
    view = TableView.getEventView( mouseEvent.getSource() );
    button = mouseEvent.getButton();
    select = view.getSelectCell();
    focus = view.getFocusCell();
    mouse = view.getMouseCell();
    cursor = view.getCursor();
    shift = mouseEvent.isShiftDown();
    control = mouseEvent.isControlDown();
    alt = mouseEvent.isAltDown();
    event = mouseEvent;
  }
}
