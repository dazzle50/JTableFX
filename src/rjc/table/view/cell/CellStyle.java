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
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/********************************** Defines cell drawing style ***********************************/
/*************************************************************************************************/

public class CellStyle extends CellLocation
{
  // default text insets to avoid the border lines (which are on right & bottom)
  public int        dataColumn; // cell data column index
  public int        dataRow;    // cell data row index
  public Object     value;      // cell value from data-model
  public CellVisual visual;     // cell visual settings from data-model

  /*************************************** getValueVisual ****************************************/
  public void getValueVisual()
  {
    // get cell value and cell visuals from table-model
    dataColumn = getDataColumn();
    dataRow = getDataRow();
    value = view.getData().getValue( dataColumn, dataRow );
    visual = view.getData().getVisual( dataColumn, dataRow );
  }

  /****************************************** getText ********************************************/
  protected String getText()
  {
    // return cell value as string
    return value == null ? null : value.toString();
  }

  /************************************** getTextAlignment ***************************************/
  protected Pos getTextAlignment()
  {
    // return cell text alignment
    return visual.textAlignment;
  }

  /*************************************** getTextFamily *****************************************/
  protected String getTextFamily()
  {
    // return cell text family
    return visual.textFamily;
  }

  /**************************************** getTextSize ******************************************/
  protected double getTextSize()
  {
    // return cell text family
    return visual.textSize;
  }

  /*************************************** getTextWeight *****************************************/
  protected FontWeight getTextWeight()
  {
    // return cell text weight
    return visual.textWeight;
  }

  /*************************************** getTextPosture ****************************************/
  protected FontPosture getTextPosture()
  {
    // return cell text posture
    return visual.textPosture;
  }

  /**************************************** getTextInsets ****************************************/
  protected Insets getTextInsets()
  {
    // return cell text insets
    return visual.textInsets;
  }

  /************************************** getZoomTextInsets **************************************/
  public Insets getZoomTextInsets()
  {
    // get text inserts adjusted for zoom
    Insets insets = getTextInsets();
    double zoom = view.getZoom().get();

    if ( zoom != 1.0 )
      insets = new Insets( insets.getTop() * zoom, insets.getRight() * zoom, insets.getBottom() * zoom,
          insets.getLeft() * zoom );

    return insets;
  }

  /**************************************** getZoomFont ******************************************/
  public Font getZoomFont()
  {
    // get font adjusted for zoom
    return Font.font( getTextFamily(), getTextWeight(), getTextPosture(), getTextSize() * view.getZoom().get() );
  }

  /*************************************** getBorderPaint ****************************************/
  protected Paint getBorderPaint()
  {
    // return cell border paint
    return visual.borderPaint;
  }

  /************************************* getBackgroundPaint **************************************/
  protected Paint getBackgroundPaint()
  {
    // return cell background paint, starting with header cells
    if ( viewRow == TableAxis.HEADER || viewColumn == TableAxis.HEADER )
      return getBackgroundPaintHeader();

    // otherwise default background
    return getBackgroundPaintDefault();
  }

  /********************************** getBackgroundPaintDefault **********************************/
  protected Paint getBackgroundPaintDefault()
  {
    // default table cell background
    return visual.cellBackground;
  }

  /********************************** getBackgroundPaintHeader ***********************************/
  protected Paint getBackgroundPaintHeader()
  {
    // return header cell background
    if ( viewRow == TableAxis.HEADER )
    {
      if ( viewColumn == view.getFocusCell().getColumn() )
        return view.isFocused() ? Colours.HEADER_FOCUS_FILL
            : Colours.HEADER_FOCUS_FILL.interpolate( Colours.HEADER_SELECTED_FILL, 0.3 );
      else
        return view.getSelection().hasColumnSelection( viewColumn ) ? Colours.HEADER_SELECTED_FILL
            : Colours.HEADER_DEFAULT_FILL;
    }

    if ( viewColumn == TableAxis.HEADER )
    {
      if ( viewRow == view.getFocusCell().getRow() )
        return view.isFocused() ? Colours.HEADER_FOCUS_FILL
            : Colours.HEADER_FOCUS_FILL.interpolate( Colours.HEADER_SELECTED_FILL, 0.3 );
      else
        return view.getSelection().hasRowSelection( viewRow ) ? Colours.HEADER_SELECTED_FILL
            : Colours.HEADER_DEFAULT_FILL;
    }

    throw new IllegalArgumentException( "Not header " + viewColumn + " " + viewRow );
  }

  /**************************************** getTextPaint *****************************************/
  protected Paint getTextPaint()
  {
    // return cell text paint
    return visual.textPaint;
  }

}