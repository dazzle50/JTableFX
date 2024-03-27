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
import rjc.table.view.TableScrollBar;
import rjc.table.view.cursor.Cursors;

/*************************************************************************************************/
/************* Handles mouse drag (move with button pressed) events from table-view **************/
/*************************************************************************************************/

public class MouseDragged extends MouseEventHandler
{
  /******************************************* handle ********************************************/
  @Override
  public void handle( MouseEvent event )
  {
    // exit immediately if not dragging with primary mouse button
    super.handle( event );
    if ( button != MouseButton.PRIMARY )
      return;

    // check if table scrolling is wanted
    verticalScrolling();
    horizontalScrolling();

    // update mouse cell position
    mouse.setXY( x, y, false );
  }

  /************************************* horizontalScrolling *************************************/
  private void horizontalScrolling()
  {
    // determine whether any horizontal scrolling needed
    TableScrollBar scrollbar = view.getHorizontalScrollBar();
    int header = view.getHeaderWidth();
    int width = (int) view.getCanvas().getWidth();
    boolean scroll = cursor == Cursors.H_RESIZE || cursor == Cursors.H_MOVE || cursor == Cursors.SELECTING_CELLS
        || cursor == Cursors.SELECTING_COLS;

    // update or stop view scrolling depending on mouse position
    if ( scroll && x >= width && scrollbar.getValue() < scrollbar.getMax() )
      scrollbar.scrollToEnd( x - width );
    else if ( scroll && x < header && scrollbar.getValue() > 0.0 )
      scrollbar.scrollToStart( header - x );
    else
      scrollbar.stopAnimationStartEnd();
  }

  /************************************** verticalScrolling **************************************/
  private void verticalScrolling()
  {
    // determine whether any vertical scrolling needed
    TableScrollBar scrollbar = view.getVerticalScrollBar();
    int height = (int) view.getCanvas().getHeight();
    int header = view.getHeaderHeight();
    boolean scroll = cursor == Cursors.V_RESIZE || cursor == Cursors.V_MOVE || cursor == Cursors.SELECTING_CELLS
        || cursor == Cursors.SELECTING_ROWS;

    // update or stop view scrolling depending on mouse position
    if ( scroll & y >= height && scrollbar.getValue() < scrollbar.getMax() )
      scrollbar.scrollToEnd( y - height );
    else if ( scroll && y < header && scrollbar.getValue() > 0.0 )
      scrollbar.scrollToStart( header - y );
    else
      scrollbar.stopAnimationStartEnd();
  }
}
