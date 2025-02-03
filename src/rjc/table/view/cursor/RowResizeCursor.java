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

package rjc.table.view.cursor;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/*************************************************************************************************/
/*************************** Mouse cursor for resizing table-view rows ***************************/
/*************************************************************************************************/

public class RowResizeCursor extends ResizeCursor
{

  /**************************************** constructor ******************************************/
  public RowResizeCursor( String imageFile, int xHotspot, int yHotstop )
  {
    super( imageFile, xHotspot, yHotstop );
  }

  /**************************************** handlePressed ****************************************/
  @Override
  public void handlePressed( MouseEvent event )
  {
    // mouse button pressed whilst hovering to resize columns
    extractDetails( event );
    view.requestFocus();

    // if primary mouse button not pressed, don't do anything else
    if ( button != MouseButton.PRIMARY )
      return;

    // start resizing row(s) - if selected is "null" means all indexes selected
    m_axis = view.getRowsAxis();
    m_scrollbar = view.getVerticalScrollBar();
    var selected = view.getSelection().getResizableRows();
    if ( selected == null )
      startAll( y );
    else
      start( y, selected );
  }

  /**************************************** handleDragged ****************************************/
  @Override
  public void handleDragged( MouseEvent event )
  {
    // resizing row(s)
    drag( (int) event.getY() );
  }

  /*************************************** handleReleased ****************************************/
  @Override
  public void handleReleased( MouseEvent event )
  {
    // resizing finished
    end();
  }
}
