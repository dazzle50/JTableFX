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
    // exit immediately without drawing if no size
    if ( getHeight() <= 0 || getWidth() <= 0 )
      return;

    // some highlights are clipped to table body so to not overdraw the headers
    m_gc.clearRect( 0.0, 0.0, getWidth(), getHeight() );
    m_gc.save();
    m_gc.beginPath();
    m_gc.rect( m_view.getHeaderWidth() - 1, m_view.getHeaderHeight() - 1, getWidth(), getHeight() );
    m_gc.clip();

    // draw selected areas and focus cell (with clipping)
    highlightSelectedAreas( m_view.getSelection().getAreas() );
    highlightFocusCell( m_view.getFocusCell() );

    // remove the clipping
    m_gc.restore();

    // draw hidden/filtered/sorted columns/rows highlights (without clipping)
    highlightHiddenColumns();
    highlightHiddenRows();
    highlightFilteredColumns();
    highlightFilteredRows();
    highlightSortedColumns();
    highlightSortedRows();
  }

  /********************************** highlightFilteredColumns ***********************************/
  private void highlightFilteredColumns()
  {
    // TODO highlight columns with filters applied
    // drawFunnel( 1, -1 );
  }

  /************************************ highlightFilteredRows ************************************/
  private void highlightFilteredRows()
  {
    // TODO Auto-generated method stub
    // drawFunnel( -1, 1 );
  }

  /*********************************** highlightSortedColumns ************************************/
  private void highlightSortedColumns()
  {
    // TODO Auto-generated method stub
    // drawDownArrow( 1, -1 );
    // drawUpArrow( 0, -1 );
  }

  /************************************* highlightSortedRows *************************************/
  private void highlightSortedRows()
  {
    // TODO Auto-generated method stub
    // drawDownArrow( -1, 1 );
    // drawUpArrow( -1, 0 );
  }

  /************************************ highlightHiddenRows **************************************/
  /**
   * Highlights locations where rows are hidden by drawing visual indicators.
   */
  private void highlightHiddenRows()
  {
    // highlight where rows are hidden
    Color stroke = m_view.isFocused() ? Colours.SELECTED_BORDER : Colours.SELECTED_BORDER.desaturate();
    m_gc.setStroke( stroke );

    // check between visible min and max rows inclusive
    int row = m_view.getRowIndex( m_view.getHeaderHeight() ) - 1;
    int maxRow = m_view.getRowIndex( (int) getHeight() );
    maxRow = Math.min( maxRow, m_view.getData().getRowCount() );

    double x = 0.5;
    double w = m_view.getHeaderWidth() - 1;
    int y = m_view.getRowStartY( row );

    while ( row <= maxRow )
    {
      // find next hidden row, by skipping contiguous visible rows
      int nextY = m_view.getRowStartY( ++row );
      while ( nextY > y && row <= maxRow )
      {
        y = nextY;
        nextY = m_view.getRowStartY( ++row );
      }

      // draw line to indicate hidden row(s)
      if ( row <= maxRow && y >= m_view.getHeaderHeight() )
        m_gc.strokeLine( x, y - 0.5, w - 0.5, y - 0.5 );
    }
  }

  /********************************** highlightHiddenColumns *************************************/
  /**
   * Highlights locations where columns are hidden by drawing visual indicators.
   */
  private void highlightHiddenColumns()
  {
    // highlight where columns are hidden
    Color stroke = m_view.isFocused() ? Colours.SELECTED_BORDER : Colours.SELECTED_BORDER.desaturate();
    m_gc.setStroke( stroke );

    // check between visible min and max columns inclusive
    int column = m_view.getColumnIndex( m_view.getHeaderWidth() ) - 1;
    int maxColumn = m_view.getColumnIndex( (int) getWidth() );
    maxColumn = Math.min( maxColumn, m_view.getData().getColumnCount() );

    double y = 0.5;
    double h = m_view.getHeaderHeight() - 1;
    int x = m_view.getColumnStartX( column );

    while ( column <= maxColumn )
    {
      // find next hidden column, by skipping contiguous visible columns
      int nextX = m_view.getColumnStartX( ++column );
      while ( nextX > x && column <= maxColumn )
      {
        x = nextX;
        nextX = m_view.getColumnStartX( ++column );
      }

      // draw line to indicate hidden column(s)
      if ( column <= maxColumn && x >= m_view.getHeaderWidth() )
        m_gc.strokeLine( x - 0.5, y, x - 0.5, h - 0.5 );
    }
  }

  /************************************** drawUpArrow ********************************************/
  private void drawUpArrow( int viewColumn, int viewRow )
  {
    // draw up arrow to indicate descending sort column/row
    var shape = new int[] { 0, 1, 2, 3, 4, 0, 0, 0, 0, 0 };
    drawShape( viewColumn, viewRow, shape, 7.5, -4.5 );
  }

  /************************************* drawDownArrow *******************************************/
  private void drawDownArrow( int viewColumn, int viewRow )
  {
    // draw down arrow to indicate ascending sort column/row
    var shape = new int[] { 0, 0, 0, 0, 0, 4, 3, 2, 1, 0 };
    drawShape( viewColumn, viewRow, shape, 7.5, -4.5 );
  }

  /***************************************** drawFunnel ******************************************/
  private void drawFunnel( int viewColumn, int viewRow )
  {
    // draw funnel shape to indicate filtered column/row
    var shape = new int[] { 4, 4, 3, 2, 1, 1, 1, 1, 1, 0 };
    drawShape( viewColumn + 1, viewRow, shape, -8.5, -4.5 );
  }

  /**************************************** drawShape ********************************************/
  /**
   * Draws a shape icon at the specified cell position using an array of line widths.
   * 
   * @param viewColumn the column index
   * @param viewRow the row index
   * @param shape array of half-widths for each horizontal line of the shape
   * @param xOffset horizontal offset from the calculated base position
   * @param yOffset vertical offset from the calculated base position
   */
  private void drawShape( int viewColumn, int viewRow, int[] shape, double xOffset, double yOffset )
  {
    // draw shape icon with appropriate stroke color based on focus state
    Color stroke = m_view.isFocused() ? Colours.SELECTED_BORDER : Colours.SELECTED_BORDER.desaturate();
    m_gc.setStroke( stroke );

    // calculate base position for the shape
    double x = m_view.getColumnStartX( viewColumn ) + xOffset;
    double y = ( m_view.getRowStartY( viewRow ) + m_view.getRowStartY( viewRow + 1 ) ) / 2.0 + yOffset;

    // draw each horizontal line of the shape
    for ( int i = 0; i < shape.length; i++ )
      m_gc.strokeLine( x - shape[i], y + i, x + shape[i], y + i );
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
      m_gc.strokeRect( x + 0.5, y + 0.5, w - 2, h - 2 );
    }
  }

}