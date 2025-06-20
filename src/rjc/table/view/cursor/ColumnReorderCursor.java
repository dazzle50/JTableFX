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
/************************ Mouse cursor for reordering table-view columns *************************/
/*************************************************************************************************/

public class ColumnReorderCursor extends AbstractReorderCursor
{

  /**************************************** constructor ******************************************/
  public ColumnReorderCursor( String imageFile, int xHotspot, int yHotspot )
  {
    super( imageFile, xHotspot, yHotspot );
  }

  /**************************************** handlePressed ****************************************/
  @Override
  public void handlePressed( MouseEvent event )
  {
    // mouse button pressed whilst hovering to reorder columns
    extractDetails( event );
    m_view.requestFocus();

    // if primary mouse button not pressed, don't do anything else
    if ( m_button != MouseButton.PRIMARY )
      return;

    // user starting to select insert point for selected columns
    startHorizontal();
  }

  /**************************************** handleDragged ****************************************/
  @Override
  public void handleDragged( MouseEvent event )
  {
    // user changing insert point
    m_x = (int) event.getX();
    m_y = (int) event.getY();
    checkScrollingX();
    dragHorizontal( m_x );
    m_mouseCell.setXY( m_x, m_y, false );
  }

  /*************************************** handleReleased ****************************************/
  @Override
  public void handleReleased( MouseEvent event )
  {
    // user confirming insert point
    end();
  }

  /**************************************** tableScrolled ****************************************/
  @Override
  public void tableScrolled()
  {
    // table-view scrolled whilst dragging mouse
    checkSelectPosition();
    dragHorizontal( m_x );
  }

}
