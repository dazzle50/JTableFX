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

import javafx.scene.Cursor;

/*************************************************************************************************/
/************************** Mouse cursors available for the table views **************************/
/*************************************************************************************************/

public final class Cursors
{
  public static final Cursor DEFAULT         = Cursor.DEFAULT;

  public static final Cursor CELLS_HOVER     = new CellHoverCursor( "cross.png", 16, 20 );
  public static final Cursor CELLS_SELECT    = new CellSelectCursor( "cross.png", 16, 20 );
  public static final Cursor CORNER_CELL     = Cursor.DISAPPEAR;

  public static final Cursor COLUMNS_HOVER   = new ViewBaseCursor( "arrowdown.png", 7, 16 );
  public static final Cursor COLUMNS_SELECT  = new ViewBaseCursor( "arrowdown.png", 7, 16 );
  public static final Cursor COLUMNS_RESIZE  = Cursor.H_RESIZE;
  public static final Cursor COLUMNS_REORDER = new ViewBaseCursor( "move.png", 16, 16 );

  public static final Cursor ROWS_HOVER      = new ViewBaseCursor( "arrowright.png", 16, 24 );
  public static final Cursor ROWS_SELECTING  = new ViewBaseCursor( "arrowright.png", 16, 24 );
  public static final Cursor ROWS_RESIZE     = Cursor.V_RESIZE;
  public static final Cursor ROWS_REORDER    = new ViewBaseCursor( "move.png", 16, 16 );
}