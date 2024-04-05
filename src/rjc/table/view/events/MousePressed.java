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
import rjc.table.view.action.Resize;
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

    // clear previous selections unless shift xor control pressed
    if ( shift == control
        && ( cursor == Cursors.CROSS || cursor == Cursors.DOWNARROW || cursor == Cursors.RIGHTARROW ) )
      view.getSelection().clear();

    // if primary mouse button not pressed, don't do anything else
    if ( button != MouseButton.PRIMARY )
      return;

    // depending on cursor
    if ( cursor == Cursors.CROSS )
      startSelectingCells();
    else if ( cursor == Cursors.CORNER_CELL )
      view.getSelection().selectAll();
    else if ( cursor == Cursors.DOWNARROW )
      startSelectingColumns();
    else if ( cursor == Cursors.RIGHTARROW )
      startSelectingRows();
    else if ( cursor == Cursors.H_RESIZE )
      Resize.startColumns( view, x );
    else if ( cursor == Cursors.V_RESIZE )
      Resize.startRows( view, y );
    else
      Utils.trace( "Unhandled mouse pressed cursor " + cursor );
  }

  /************************************ startSelectingColumns ************************************/
  private void startSelectingColumns()
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

  /************************************* startSelectingRows **************************************/
  private void startSelectingRows()
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

  /************************************ startSelectingCells **************************************/
  private void startSelectingCells()
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
