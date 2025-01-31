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
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/********************** Mouse cursor when hovering over column header cells **********************/
/*************************************************************************************************/

public class ColumnHoverCursor extends ViewBaseCursor
{

  /**************************************** constructor ******************************************/
  public ColumnHoverCursor( String imageFile, int xHotspot, int yHotstop )
  {
    super( imageFile, xHotspot, yHotstop );
  }

  /**************************************** handlePressed ****************************************/
  @Override
  public void handlePressed( MouseEvent event )
  {
    // mouse button pressed whilst hovering over column header cells
    extractDetails( event );
    view.requestFocus();

    // clear previous selections unless shift xor control pressed
    if ( shift == control )
      view.getSelection().clear();

    // if primary mouse button not pressed, don't do anything else
    if ( button != MouseButton.PRIMARY )
      return;

    // start selecting columns
    view.setCursor( Cursors.COLUMNS_SELECT );
    if ( !shift || control )
    {
      view.getSelection().select();
      int topRow = view.getRowIndex( view.getHeaderHeight() );
      focusCell.setPosition( mouseCell.getColumn(), topRow );
      view.scrollTo( focusCell );
    }
    selectCell.setPosition( mouseCell.getColumn(), TableAxis.AFTER );
  }
}
