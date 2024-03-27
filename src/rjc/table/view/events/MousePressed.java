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

import javafx.scene.input.MouseEvent;
import rjc.table.Utils;
import rjc.table.view.cursor.Cursors;

/*************************************************************************************************/
/********************** Handles mouse button pressed events from table-view **********************/
/*************************************************************************************************/

public class MousePressed extends MouseEventHandler
{
  /******************************************* handle ********************************************/
  @Override
  public void handle( MouseEvent event )
  {
    // clear status, request focus & update mouse cell position and cursor
    super.handle( event );
    view.requestFocus();
    mouse.setXY( x, y, true );

    // depending on cursor
    if ( cursor == Cursors.CROSS )
      handleCrossCursor();
    else if ( cursor == Cursors.H_RESIZE )
      handleHortizontalResizeCursor();
    else if ( cursor == Cursors.V_RESIZE )
      handleVerticalResizeCursor();
    else
      Utils.trace( "Unhandled mouse pressed cursor " + cursor );
  }

  /********************************* handleVerticalResizeCursor **********************************/
  private void handleVerticalResizeCursor()
  {
    // TODO Auto-generated method stub
    Utils.trace( "V RESIZE " + cursor.toString() );
  }

  /******************************** handleHortizontalResizeCursor ********************************/
  private void handleHortizontalResizeCursor()
  {
    // TODO Auto-generated method stub
    Utils.trace( "H RESIZE " + cursor.toString() );
  }

  /************************************* handleCrossCursor ***************************************/
  private void handleCrossCursor()
  {
    // clear previous selections unless shift xor control pressed
    if ( shift == control )
      view.getSelection().clear();

    // update select & focus cell positions and selections
    view.setCursor( Cursors.SELECTING_CELLS );
    if ( !shift || control )
    {
      view.getSelection().select();
      focus.setPosition( mouse );
    }
    select.setPosition( mouse );
  }

}
