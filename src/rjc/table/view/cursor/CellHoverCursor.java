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
/*********************** Mouse cursor when hovering over table body cells ************************/
/*************************************************************************************************/

public class CellHoverCursor extends AbstractCursor
{

  /**************************************** constructor ******************************************/
  public CellHoverCursor( String imageFile, int xHotspot, int yHotspot )
  {
    super( imageFile, xHotspot, yHotspot );
  }

  /**************************************** handlePressed ****************************************/
  @Override
  public void handlePressed( MouseEvent event )
  {
    // mouse button pressed whilst hovering over body cells
    extractDetails( event );
    m_view.requestFocus();

    // clear previous selections unless shift xor control pressed
    if ( m_shift == m_control )
      m_view.getSelection().clear();

    // if primary mouse button not pressed, don't do anything else
    if ( m_button != MouseButton.PRIMARY )
      return;

    // start selecting cells
    m_view.setCursor( Cursors.CELLS_SELECT );
    if ( !m_shift || m_control )
    {
      m_view.getSelection().select();
      m_focusCell.setPosition( m_mouseCell );
    }
    m_selectCell.setPosition( m_mouseCell );
  }

  /**************************************** handleClicked ****************************************/
  @Override
  public void handleClicked( MouseEvent event )
  {
    // user has clicked the table
    extractDetails( event );
    boolean doubleClick = event.getClickCount() == 2 && m_button == MouseButton.PRIMARY;

    // double-click to start cell editor with cell contents
    if ( doubleClick )
      m_view.openEditor( m_focusCell.getData() );
  }

}
