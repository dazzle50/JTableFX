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

package rjc.table.view.cursor;

import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.image.Image;

/*************************************************************************************************/
/************************** Mouse cursors available for the table views **************************/
/*************************************************************************************************/

public final class Cursors
{
  public static final Cursor H_RESIZE        = Cursor.H_RESIZE;
  public static final Cursor V_RESIZE        = Cursor.V_RESIZE;
  public static final Cursor DEFAULT         = Cursor.DEFAULT;

  public static final Cursor DOWNARROW       = makeCursor( "arrowdown.png", 7, 16 );
  public static final Cursor SELECTING_COLS  = makeCursor( "arrowdown.png", 7, 16 );
  public static final Cursor RIGHTARROW      = makeCursor( "arrowright.png", 16, 24 );
  public static final Cursor SELECTING_ROWS  = makeCursor( "arrowright.png", 16, 24 );
  public static final Cursor CROSS           = makeCursor( "cross.png", 16, 20 );
  public static final Cursor SELECTING_CELLS = makeCursor( "cross.png", 16, 20 );
  public static final Cursor H_MOVE          = makeCursor( "move.png", 16, 16 );
  public static final Cursor V_MOVE          = makeCursor( "move.png", 16, 16 );

  /***************************************** makeCursor ******************************************/
  private static Cursor makeCursor( String file, int x, int y )
  {
    // return a cursor based on image file with specified x & y hot spot
    return new ImageCursor( new Image( Cursors.class.getResourceAsStream( file ) ), x, y );
  }
}