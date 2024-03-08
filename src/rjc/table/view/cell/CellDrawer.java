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

package rjc.table.view.cell;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/*************************************** Draws table cell ****************************************/
/*************************************************************************************************/

public class CellDrawer extends CellStyle
{

  /******************************************** draw *********************************************/
  public void draw()
  {
    // clip drawing to cell boundaries
    gc.save();
    gc.beginPath();

    if ( viewColumn == TableAxis.HEADER || viewRow == TableAxis.HEADER )
      gc.rect( x, y, w, h );
    else
    {
      int headerWidth = view.getCanvas().getColumnsAxis().getHeaderPixels();
      int headerHeight = view.getCanvas().getRowsAxis().getHeaderPixels();

      double cx = x > headerWidth ? x : headerWidth;
      double cy = y > headerHeight ? y : headerHeight;
      double cw = w + x - cx;
      double ch = h + y - cy;
      gc.rect( cx, cy, cw, ch );
    }

    gc.clip();
    drawUnclipped();

    // remove clip
    gc.restore();
  }

  /**************************************** drawUnclipped ****************************************/
  public void drawUnclipped()
  {
    // draw table body or header cell
    drawBackground();
    drawContent();
    drawBorder();
  }

  /*************************************** drawBackground ****************************************/
  protected void drawBackground()
  {
    // draw cell background
    gc.setFill( getBackgroundPaint() );
    gc.fillRect( x, y, w, h );
  }

  /***************************************** drawBorder ******************************************/
  protected void drawBorder()
  {
    // draw cell border
    gc.setStroke( getBorderPaint() );
    gc.strokeLine( x + w - 0.5, y + 0.5, x + w - 0.5, y + h - 0.5 );
    gc.strokeLine( x + 0.5, y + h - 0.5, x + w - 1.5, y + h - 0.5 );
  }

  /**************************************** drawContent ******************************************/
  protected void drawContent()
  {
    // draw cell contents
    drawText( getText() );
  }

  /****************************************** drawText *******************************************/
  protected void drawText( String cellText )
  {
    // get text inserts, font, alignment, and convert string into text lines
    Insets insets = getZoomTextInsets();
    Font font = getZoomFont();
    Pos alignment = getTextAlignment();
    CellText formattedText = new CellText( cellText, font, insets, alignment, w, h );

    // draw the text lines in cell
    gc.setFont( font );
    gc.setFill( getTextPaint() );
    formattedText.getLines().forEach( line -> gc.fillText( line.txt, x + line.x, y + line.y ) );
  }
}
