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

import java.util.ArrayList;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.FontSmoothingType;
import rjc.table.view.cell.ViewPosition;

/*************************************************************************************************/
/****************** Canvas overlay for table-views (highlights selection etc) ********************/
/*************************************************************************************************/

public class TableOverlay extends Canvas
{
  private TableView       m_view;
  private GraphicsContext m_gc;

  final public static int MIN_COORD = -999;  // highlighting coordinate limit
  final public static int MAX_COORD = 99999; // highlighting coordinate limit

  /**************************************** constructor ******************************************/
  public TableOverlay( TableView tableView )
  {
    // prepare canvas overlay
    m_view = tableView;
    m_gc = getGraphicsContext2D();

    m_gc.setFontSmoothingType( FontSmoothingType.LCD );
  }

  /******************************************* getView *******************************************/
  public TableView getView()
  {
    // return the table-view for this overlay
    return m_view;
  }

  /****************************************** redrawNow ******************************************/
  public void redrawNow()
  {
    // clip overlay drawing to table body
    m_gc.clearRect( 0.0, 0.0, getWidth(), getHeight() );
    m_gc.save();
    m_gc.beginPath();
    m_gc.rect( m_view.getHeaderWidth() - 1, m_view.getHeaderHeight() - 1, getWidth(), getHeight() );
    m_gc.clip();

    // draw overlay
    highlightSelectedAreas( m_view.getSelection().getAreas() );
    highlightFocusCell( m_view.getFocusCell() );

    // remove clip
    m_gc.restore();
  }

  /*********************************** highlightSelectedAreas ************************************/
  private void highlightSelectedAreas( ArrayList<int[]> areas )
  {
    // highlight selected areas
    Color fill = m_view.isFocused() ? Colours.SELECTED_HIGHLIGHT : Colours.SELECTED_HIGHLIGHT.desaturate();
    m_gc.setFill( fill );
    m_gc.setStroke( Colours.SELECTED_BORDER );

    // fill each selected rectangle with opaque colour & border
    for ( var area : areas )
    {
      // limit highlighted area to avoid drawing overflow artifacts on large tables
      int x = m_view.getColumnStartX( area[0] );
      x = x < MIN_COORD ? MIN_COORD : x;
      int y = m_view.getRowStartY( area[1] );
      y = y < MIN_COORD ? MIN_COORD : y;
      int w = m_view.getColumnStartX( area[2] + 1 ) - x;
      w = w > MAX_COORD ? MAX_COORD : w;
      int h = m_view.getRowStartY( area[3] + 1 ) - y;
      h = h > MAX_COORD ? MAX_COORD : h;

      m_gc.fillRect( x, y, w - 1, h - 1 );
      m_gc.strokeRect( x - 0.5, y - 0.5, w, h );
    }
  }

  /************************************* highlightFocusCell **************************************/
  private void highlightFocusCell( ViewPosition focus )
  {
    // clear highlight on focus cell and draw border
    Color stroke = m_view.isFocused() ? Colours.SELECTED_BORDER : Colours.SELECTED_BORDER.desaturate();
    m_gc.setStroke( stroke );

    if ( focus.isVisible() )
    {
      int column = focus.getColumn();
      int row = focus.getRow();
      int x = m_view.getColumnStartX( column );
      int y = m_view.getRowStartY( row );
      int w = m_view.getColumnStartX( column + 1 ) - x;
      int h = m_view.getRowStartY( row + 1 ) - y;
      m_gc.clearRect( x, y, w, h );

      m_gc.strokeRect( x - 0.5, y - 0.5, w, h );
      if ( m_view.isFocused() )
        m_gc.strokeRect( x + 0.5, y + 0.5, w - 2, h - 2 );
    }
  }

}