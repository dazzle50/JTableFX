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

package rjc.table.view;

import javafx.scene.paint.Color;

/*************************************************************************************************/
/************************** Miscellaneous utility public static colours **************************/
/*************************************************************************************************/

public class Colours
{
  // general gui colours
  public static final Color SELECTED_HIGHLIGHT       = Color.rgb( 0, 150, 255, 0.3 ); // opaque blue
  public static final Color SELECTED_BORDER          = Color.rgb( 0, 150, 255 );      // light blue

  public static final Color CELL_BORDER              = Color.gray( 0.8 );
  public static final Color CELL_DEFAULT_BACKGROUND  = Color.WHITE;
  public static final Color CELL_DISABLED_BACKGROUND = Color.gray( 0.9 );

  public static final Color HEADER_DEFAULT_FILL      = Color.gray( 0.95 );
  public static final Color HEADER_FOCUS_FILL        = Color.LIGHTYELLOW;
  public static final Color HEADER_SELECTED_FILL     = Color.gray( 0.85 );

  public static final Color TEXT_DEFAULT             = Color.BLACK;
  public static final Color TEXT_DEFAULT_BACKGROUND  = Color.WHITE;
  public static final Color TEXT_SELECTED            = Color.WHITE;
  public static final Color TEXT_SELECTED_BACKGROUND = Color.rgb( 51, 153, 255 );     // mid blue

  public static final Color BUTTON_ARROW             = Color.BLACK;
  public static final Color BUTTON_BACKGROUND        = Color.gray( 0.85 );

  public static final Color REORDER_LINE             = Color.RED;
}