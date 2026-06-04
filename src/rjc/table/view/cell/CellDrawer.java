/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
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

/**
 * Draws a single table cell onto the table canvas using the location, value, and visual state
 * prepared for the current view cell.
 *
 * <p>Instances are created by {@link rjc.table.view.TableView#getCellDrawer()} and are normally
 * reused while a visible row or column is being redrawn. Before {@link #draw()} is called, the
 * inherited cell location fields identify the table view, graphics context, view indices, and
 * canvas rectangle to draw.</p>
 *
 * <p>Subclasses can customise rendering by overriding the protected drawing hooks, style accessors
 * inherited from {@link CellStyle}, or both. The default implementation clips drawing to the cell
 * rectangle, resolves the cell value and {@link CellVisual}, then draws background, text content,
 * and border in that order.</p>
 *
 * @see CellStyle
 * @see CellLocation
 * @see rjc.table.view.TableView#getCellDrawer()
 */
public class CellDrawer extends CellStyle
{

  /******************************************** draw *********************************************/
  /**
   * Draws the current cell clipped to its visible canvas rectangle.
   *
   * <p>Body cells are additionally clipped so they cannot draw over the fixed header row or
   * column. Header cells are clipped to their full cell rectangle. The actual drawing sequence is
   * delegated to {@link #drawUnclipped()}.</p>
   */
  public void draw()
  {
    // clip drawing to cell boundaries
    gc.save();
    gc.beginPath();

    if ( viewColumn == TableAxis.HEADER || viewRow == TableAxis.HEADER )
      gc.rect( x, y, w, h );
    else
    {
      int headerWidth = view.getHeaderWidth();
      int headerHeight = view.getHeaderHeight();

      // ensure body cell does not draw over header column or row
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
  /**
   * Draws the current cell after clipping has already been applied.
   *
   * <p>The default implementation resolves the cell value and visual settings, then draws the
   * background, content, and border. Subclasses may override this method to replace the full drawing
   * sequence, or override the narrower drawing hooks for targeted customisation.</p>
   */
  protected void drawUnclipped()
  {
    // draw table body or header cell
    getValueVisual();
    drawBackground();
    drawContent();
    drawBorder();
  }

  /*************************************** drawBackground ****************************************/
  /**
   * Draws the current cell background using {@link #getBackgroundPaint()}.
   *
   * <p>No background is drawn when the background paint is {@code null}.</p>
   */
  protected void drawBackground()
  {
    // draw cell background
    if ( getBackgroundPaint() != null )
    {
      gc.setFill( getBackgroundPaint() );
      gc.fillRect( x, y, w, h );
    }
  }

  /***************************************** drawBorder ******************************************/
  /**
   * Draws the current cell border using {@link #getBorderPaint()}.
   *
   * <p>The default border is drawn on the right and bottom edges of the cell. No border is drawn
   * when the border paint is {@code null}.</p>
   */
  protected void drawBorder()
  {
    // draw cell border
    if ( getBorderPaint() != null )
    {
      gc.setStroke( getBorderPaint() );
      gc.strokeLine( x + w - 0.5, y + 0.5, x + w - 0.5, y + h - 0.5 );
      gc.strokeLine( x + 0.5, y + h - 0.5, x + w - 1.5, y + h - 0.5 );
    }
  }

  /**************************************** drawContent ******************************************/
  /**
   * Draws the current cell content.
   *
   * <p>The default implementation draws the text returned by {@link #getText()} when
   * {@link #getTextPaint()} is not {@code null}.</p>
   */
  protected void drawContent()
  {
    // draw cell contents
    if ( getTextPaint() != null )
      drawText( getText() );
  }

  /****************************************** drawText *******************************************/
  /**
   * Draws cell text using the current font, alignment, insets, and text paint.
   *
   * <p>The supplied text is formatted by {@link CellText} to fit within the current cell rectangle,
   * including wrapping, truncation, and ellipsis handling where needed.</p>
   *
   * @param cellText the text to draw, or {@code null} to draw nothing
   */
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
