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
import rjc.table.Utils;
import rjc.table.view.axis.TableAxis;
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

    // if primary mouse button not pressed, don't do anything else
    if ( button != MouseButton.PRIMARY )
      return;

    // clear previous selections unless shift xor control pressed
    if ( shift == control )
      view.getSelection().clear();

    // depending on cursor
    if ( cursor == Cursors.CROSS )
      handleCrossCursor();
    else if ( cursor == Cursors.H_RESIZE )
      handleHortizontalResizeCursor();
    else if ( cursor == Cursors.V_RESIZE )
      handleVerticalResizeCursor();
    else if ( cursor == Cursors.DOWNARROW )
      handleDownArrowCursor();
    else if ( cursor == Cursors.RIGHTARROW )
      handleRightArrowCursor();
    else
      Utils.trace( "Unhandled mouse pressed cursor " + cursor );
  }

  /************************************ handleDownArrowCursor ************************************/
  private void handleDownArrowCursor()
  {
    // update select & focus cell positions and selections
    view.setCursor( Cursors.SELECTING_COLS );
    if ( !shift || control )
    {
      view.getSelection().select();
      int topRow = view.getRowIndex( view.getHeaderHeight() );
      focus.setPosition( mouse.getColumn(), topRow );
      view.scrollTo( focus );
    }
    select.setPosition( mouse.getColumn(), TableAxis.AFTER );
  }

  /*********************************** handleRightArrowCursor ************************************/
  private void handleRightArrowCursor()
  {
    // update select & focus cell positions and selections
    view.setCursor( Cursors.SELECTING_ROWS );
    if ( !shift || control )
    {
      view.getSelection().select();
      int leftColumn = view.getColumnIndex( view.getHeaderWidth() );
      focus.setPosition( leftColumn, mouse.getRow() );
      view.scrollTo( focus );
    }
    select.setPosition( TableAxis.AFTER, mouse.getRow() );
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
