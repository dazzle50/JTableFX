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
/************************* Mouse cursor when selecting table body cells **************************/
/*************************************************************************************************/

public class SelectCursor extends ViewBaseCursor
{

  /**************************************** constructor ******************************************/
  public SelectCursor( String imageFile, int xHotspot, int yHotstop )
  {
    super( imageFile, xHotspot, yHotstop );
  }

  /*************************************** handleReleased ****************************************/
  @Override
  public void handleReleased( MouseEvent event )
  {
    // finishing selecting, so update cursor and stop any animations
    m_view.getMouseCell().setXY( m_x, m_y, true );
    m_view.getHorizontalScrollBar().stopAnimationStartEnd();
    m_view.getVerticalScrollBar().stopAnimationStartEnd();
  }

  /**************************************** handleDragged ****************************************/
  @Override
  public void handleDragged( MouseEvent event )
  {
    // selecting cells by dragging mouse whilst primary button held down
    extractDetails( event );
    if ( m_button != MouseButton.PRIMARY )
      return;

    // check if table scrolling is wanted
    checkScrollingX();
    checkScrollingY();

    // update mouse cell position
    m_mouseCell.setXY( m_x, m_y, false );
  }

  /***************************************** isSelecting *****************************************/
  @Override
  public boolean isSelecting()
  {
    // cursor is selecting cells
    return true;
  }
}
