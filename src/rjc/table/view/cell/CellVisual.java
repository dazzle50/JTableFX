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

package rjc.table.view.cell;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import rjc.table.view.Colours;

/*************************************************************************************************/
/**************************** Default table-view cell visual settings ****************************/
/*************************************************************************************************/

public class CellVisual
{
  protected final static Insets CELL_TEXT_INSERTS = new Insets( 0.0, 5.0, 1.0, 4.0 );

  public Pos                    textAlignment     = Pos.CENTER;
  public String                 textFamily        = Font.getDefault().getFamily();
  public double                 textSize          = Font.getDefault().getSize();
  public FontWeight             textWeight        = FontWeight.NORMAL;
  public FontPosture            textPosture       = FontPosture.REGULAR;
  public Insets                 textInsets        = CELL_TEXT_INSERTS;
  public Paint                  textPaint         = Colours.TEXT_DEFAULT;
  public Paint                  borderPaint       = Colours.CELL_BORDER;
  public Paint                  cellBackground    = Colours.CELL_DEFAULT_BACKGROUND;
}
