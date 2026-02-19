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

package rjc.table.view;

import java.util.ArrayList;
import java.util.function.IntConsumer;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.FontSmoothingType;
import rjc.table.view.action.Filter;
import rjc.table.view.action.Sort;
import rjc.table.view.action.Sort.SortType;
import rjc.table.view.axis.TableAxis;
import rjc.table.view.cell.ViewPosition;

/*************************************************************************************************/
/****************** Canvas overlay for table-views (highlights selection etc) ********************/
/*************************************************************************************************/

/**
 * Transparent canvas overlay drawn on top of a {@link TableView} to render selection
 * highlights, focus-cell borders, and header indicators for hidden, filtered, and sorted
 * columns and rows.
 * <p>
 * Drawing is split into two passes: clipped (selection and focus, constrained to the table
 * body so headers are not overdrawn) and unclipped (hidden/filtered/sorted indicators that
 * must overdraw header cells).
 */
public class TableOverlay extends Canvas
{
  private TableView       m_view;
  private GraphicsContext m_gc;

  /**************************************** constructor ******************************************/
  /**
   * Constructs an overlay canvas associated with the given table view.
   *
   * @param tableView the {@link TableView} this overlay belongs to
   */
  public TableOverlay( TableView tableView )
  {
    // prepare canvas overlay
    m_view = tableView;
    m_gc = getGraphicsContext2D();
    m_gc.setFontSmoothingType( FontSmoothingType.LCD );
  }

  /******************************************* getView *******************************************/
  /**
   * Returns the table view associated with this overlay.
   *
   * @return the owning {@link TableView}
   */
  public TableView getView()
  {
    return m_view;
  }

  /****************************************** redrawNow ******************************************/
  /**
   * Redraws the entire overlay immediately.
   * Does nothing if the canvas has no positive dimensions.
   */
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
  /**
   * Draws a funnel icon in the column header for each currently filtered column.
   */
  private void highlightFilteredColumns()
  {
    forEachVisibleColumn( viewColumn ->
    {
      int dataColumn = m_view.getColumnsAxis().getDataIndex( viewColumn );
      if ( Filter.columnFilterCount.hasFilter( m_view, dataColumn ) )
        drawFunnel( viewColumn, TableAxis.HEADER );
    } );
  }

  /************************************ highlightFilteredRows ************************************/
  /**
   * Draws a funnel icon in the row header for each currently filtered row.
   */
  private void highlightFilteredRows()
  {
    forEachVisibleRow( viewRow ->
    {
      int dataRow = m_view.getRowsAxis().getDataIndex( viewRow );
      if ( Filter.rowFilterCount.hasFilter( m_view, dataRow ) )
        drawFunnel( TableAxis.HEADER, viewRow );
    } );
  }

  /*********************************** highlightSortedColumns ************************************/
  /**
   * Draws an arrow icon in the column header for each currently sorted column.
   */
  private void highlightSortedColumns()
  {
    forEachVisibleColumn( viewColumn ->
    {
      int dataColumn = m_view.getColumnsAxis().getDataIndex( viewColumn );
      SortType sortType = Sort.columnSort.get( m_view, dataColumn, SortType.NOTSORTED );
      if ( sortType == SortType.NOTSORTED ) // sort might be at data-model level
        sortType = Sort.columnSort.get( m_view.getData(), dataColumn, SortType.NOTSORTED );
      if ( sortType == SortType.ASCENDING )
        drawUpArrow( viewColumn, TableAxis.HEADER );
      else if ( sortType == SortType.DESCENDING )
        drawDownArrow( viewColumn, TableAxis.HEADER );
    } );
  }

  /************************************* highlightSortedRows *************************************/
  /**
   * Draws an arrow icon in the row header for each currently sorted row.
   */
  private void highlightSortedRows()
  {
    forEachVisibleRow( viewRow ->
    {
      int dataRow = m_view.getRowsAxis().getDataIndex( viewRow );
      SortType sortType = Sort.rowSort.get( m_view, dataRow, SortType.NOTSORTED );
      if ( sortType == SortType.NOTSORTED ) // sort might be at data-model level
        sortType = Sort.rowSort.get( m_view.getData(), dataRow, SortType.NOTSORTED );
      if ( sortType == SortType.ASCENDING )
        drawUpArrow( TableAxis.HEADER, viewRow );
      else if ( sortType == SortType.DESCENDING )
        drawDownArrow( TableAxis.HEADER, viewRow );
    } );
  }

  /************************************ highlightHiddenRows **************************************/
  /**
   * Highlights locations where rows are hidden by drawing a thin horizontal bar
   * across the row header at the hidden boundary.
   */
  private void highlightHiddenRows()
  {
    Color colour = m_view.isFocused() ? Colours.SELECTED_BORDER : Colours.SELECTED_BORDER.desaturate();
    m_gc.setFill( colour );

    // determine visible row range
    int firstVisible = m_view.getRowIndex( m_view.getHeaderHeight() );
    int lastVisible = Math.min( m_view.getRowIndex( (int) getHeight() ), m_view.getData().getRowCount() );

    int previousStartY = Integer.MIN_VALUE; // initialise to impossible y value
    boolean previousWasHidden = false;
    double x = 0;
    double w = m_view.getHeaderWidth() - 1;

    for ( int row = firstVisible; row <= lastVisible; row++ )
    {
      // row is hidden if it has same start y as previous row
      int startY = m_view.getRowStartY( row );
      boolean isHidden = startY == previousStartY;

      // indicate hidden row(s) only at the start of a new hidden block
      if ( isHidden && !previousWasHidden )
        m_gc.fillRect( x, startY - 1.5, w, 2 );

      previousStartY = startY;
      previousWasHidden = isHidden;
    }
  }

  /********************************** highlightHiddenColumns *************************************/
  /**
   * Highlights locations where columns are hidden by drawing a thin vertical bar
   * across the column header at the hidden boundary.
   */
  private void highlightHiddenColumns()
  {
    Color colour = m_view.isFocused() ? Colours.SELECTED_BORDER : Colours.SELECTED_BORDER.desaturate();
    m_gc.setFill( colour );

    // determine visible column range
    int firstVisible = m_view.getColumnIndex( m_view.getHeaderWidth() );
    int lastVisible = Math.min( m_view.getColumnIndex( (int) getWidth() ), m_view.getData().getColumnCount() );

    int previousStartX = Integer.MIN_VALUE; // initialise to impossible x value
    boolean previousWasHidden = false;
    double y = 0;
    double h = m_view.getHeaderHeight() - 1;

    for ( int column = firstVisible; column <= lastVisible; column++ )
    {
      // column is hidden if it has same start x as previous column
      int startX = m_view.getColumnStartX( column );
      boolean isHidden = startX == previousStartX;

      // indicate hidden column(s) only at the start of a new hidden block
      if ( isHidden && !previousWasHidden )
        m_gc.fillRect( startX - 1.5, y, 2, h );

      previousStartX = startX;
      previousWasHidden = isHidden;
    }
  }

  /************************************** drawUpArrow ********************************************/
  /**
   * Draws an upward-pointing arrow icon in the specified header cell to indicate ascending sort.
   *
   * @param viewColumn the view column index
   * @param viewRow    the view row index
   */
  private void drawUpArrow( int viewColumn, int viewRow )
  {
    var shape = new int[] { 0, 1, 2, 3, 4, 0, 0, 0, 0, 0 };
    drawShape( viewColumn, viewRow, shape, 7.5, -4.5 );
  }

  /************************************* drawDownArrow *******************************************/
  /**
   * Draws a downward-pointing arrow icon in the specified header cell to indicate descending sort.
   *
   * @param viewColumn the view column index
   * @param viewRow    the view row index
   */
  private void drawDownArrow( int viewColumn, int viewRow )
  {
    var shape = new int[] { 0, 0, 0, 0, 0, 4, 3, 2, 1, 0 };
    drawShape( viewColumn, viewRow, shape, 7.5, -4.5 );
  }

  /***************************************** drawFunnel ******************************************/
  /**
   * Draws a funnel icon in the specified header cell to indicate an active filter.
   *
   * @param viewColumn the view column index
   * @param viewRow    the view row index
   */
  private void drawFunnel( int viewColumn, int viewRow )
  {
    var shape = new int[] { 4, 4, 3, 2, 1, 1, 1, 1, 1, 0 };
    drawShape( viewColumn + 1, viewRow, shape, -8.5, -4.5 );
  }

  /**************************************** drawShape ********************************************/
  /**
   * Draws a pixel-art icon at the specified cell position using an array of half-widths,
   * one per horizontal scan line. Each scan line is rendered as a filled rectangle of
   * height 1, which is both clearer and faster than stroking individual lines on a
   * GPU-backed canvas.
   *
   * @param viewColumn the view column index of the target header cell
   * @param viewRow    the view row index of the target header cell
   * @param shape      array of half-widths for each horizontal scan line of the shape
   * @param xOffset    horizontal offset in pixels from the cell's left edge
   * @param yOffset    vertical offset in pixels from the cell's vertical centre
   */
  private void drawShape( int viewColumn, int viewRow, int[] shape, double xOffset, double yOffset )
  {
    // use desaturated colour when the view does not have focus
    Color colour = m_view.isFocused() ? Colours.SELECTED_BORDER : Colours.SELECTED_BORDER.desaturate();
    m_gc.setFill( colour );

    // calculate base position for the shape
    double x = m_view.getColumnStartX( viewColumn ) + xOffset;
    double y = ( m_view.getRowStartY( viewRow ) + m_view.getRowStartY( viewRow + 1 ) ) / 2.0 + yOffset;

    // draw each scan line as a filled 1px-high rectangle
    for ( int i = 0; i < shape.length; i++ )
    {
      int hw = shape[i];
      if ( hw > 0 )
        m_gc.fillRect( x - hw, y + i, hw * 2, 1 );
    }
  }

  /*********************************** highlightSelectedAreas ************************************/
  /**
   * Fills and outlines each selected rectangular area with the selection highlight colour.
   *
   * @param areas list of selected areas, each as {@code [colMin, rowMin, colMax, rowMax]}
   */
  private void highlightSelectedAreas( ArrayList<int[]> areas )
  {
    Color fill = m_view.isFocused() ? Colours.SELECTED_HIGHLIGHT : Colours.SELECTED_HIGHLIGHT.desaturate();
    m_gc.setFill( fill );
    m_gc.setStroke( Colours.SELECTED_BORDER );

    // coordinate limits prevent rendering artefacts on very large tables
    final int MIN_COORD = -999;
    final int MAX_COORD = 99999;

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
  /**
   * Clears the focus cell and draws a double-border highlight around it.
   *
   * @param focus the current focus cell position
   */
  private void highlightFocusCell( ViewPosition focus )
  {
    Color colour = m_view.isFocused() ? Colours.SELECTED_BORDER : Colours.SELECTED_BORDER.desaturate();
    m_gc.setStroke( colour );

    if ( focus.isVisible() )
    {
      int column = focus.getColumn();
      int row = focus.getRow();
      int x = m_view.getColumnStartX( column );
      int y = m_view.getRowStartY( row );
      int w = m_view.getColumnStartX( column + 1 ) - x;
      int h = m_view.getRowStartY( row + 1 ) - y;
      m_gc.clearRect( x, y, w, h );

      // outer and inner borders form the double-border focus indicator
      m_gc.strokeRect( x - 0.5, y - 0.5, w, h );
      m_gc.strokeRect( x + 0.5, y + 0.5, w - 2, h - 2 );
    }
  }

  /*********************************** forEachVisibleColumn **************************************/
  /**
   * Iterates over every visible column within the current canvas width, invoking
   * {@code action} with each view column index. Exits early if there are no visible columns.
   *
   * @param action the action to perform for each visible view column index
   */
  private void forEachVisibleColumn( IntConsumer action )
  {
    int viewColumn = m_view.getColumnIndex( m_view.getHeaderWidth() );
    if ( viewColumn == TableAxis.AFTER )
      return;

    int lastColumn = m_view.getColumnIndex( (int) getWidth() );
    while ( viewColumn <= lastColumn )
    {
      action.accept( viewColumn );

      // advance to next visible column; break if axis reports no further progress
      int previous = viewColumn;
      viewColumn = m_view.getColumnsAxis().getNextVisible( viewColumn );
      if ( viewColumn == previous )
        break;
    }
  }

  /************************************* forEachVisibleRow ***************************************/
  /**
   * Iterates over every visible row within the current canvas height, invoking
   * {@code action} with each view row index. Exits early if there are no visible rows.
   *
   * @param action the action to perform for each visible view row index
   */
  private void forEachVisibleRow( IntConsumer action )
  {
    int viewRow = m_view.getRowIndex( m_view.getHeaderHeight() );
    if ( viewRow == TableAxis.AFTER )
      return;

    int lastRow = m_view.getRowIndex( (int) getHeight() );
    while ( viewRow <= lastRow )
    {
      action.accept( viewRow );

      // advance to next visible row; break if axis reports no further progress
      int previous = viewRow;
      viewRow = m_view.getRowsAxis().getNextVisible( viewRow );
      if ( viewRow == previous )
        break;
    }
  }

}