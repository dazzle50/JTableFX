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

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.text.FontSmoothingType;
import rjc.table.HashSetInt;
import rjc.table.view.axis.TableAxis;
import rjc.table.view.cell.CellDrawer;

/*************************************************************************************************/
/************************** Canvas for table-views with redraw methods ***************************/
/*************************************************************************************************/

public class TableCanvasDraw extends Canvas
{
  private TableView        m_view;
  private TableOverlay     m_overlay;

  private AtomicBoolean    m_redrawIsRequested;                      // flag if redraw has been scheduled
  private boolean          m_fullRedraw;                             // full view redraw (headers & body including overlay)
  private boolean          m_overlayRedraw;                          // just overlay redraw
  private HashSetInt       m_columns;                                // requested view column indexes
  private HashSetInt       m_rows;                                   // requested view row indexes
  private HashSet<Long>    m_cells;                                  // long = (long) column << 32 | row & 0xFFFFFFFFL
  private int              m_redrawCount;                            // count to ensure canvas cleared periodically

  // column & row index starts at 0 for table body, index of -1 is for axis header
  final static public int  INVALID             = TableAxis.INVALID;
  final static public int  HEADER              = TableAxis.HEADER;
  final static public int  FIRSTCELL           = TableAxis.FIRSTCELL;

  final static private int REDRAW_COUNT_MAX    = 1000;
  final static private int REDRAW_COUNT_CELL   = 1;
  final static private int REDRAW_COUNT_COLUMN = 20;
  final static private int REDRAW_COUNT_ROW    = 5;

  /**************************************** constructor ******************************************/
  public TableCanvasDraw( TableView tableView )
  {
    // prepare main & overlay canvas
    m_view = tableView;
    m_overlay = new TableOverlay( tableView );
    m_redrawIsRequested = new AtomicBoolean();
    m_columns = new HashSetInt();
    m_rows = new HashSetInt();
    m_cells = new HashSet<>();

    getGraphicsContext2D().setFontSmoothingType( FontSmoothingType.LCD );
  }

  /******************************************* redraw ********************************************/
  public void redraw()
  {
    // request redraw full visible table (headers and body)
    if ( m_fullRedraw )
      return;
    m_fullRedraw = true;
    schedule();
  }

  /******************************************* redrawOverlay ********************************************/
  public void redrawOverlay()
  {
    // request redraw table overlay (shows selection etc)
    if ( m_overlayRedraw || m_fullRedraw )
      return;
    m_overlayRedraw = true;
    schedule();
  }

  /***************************************** redrawCell ******************************************/
  public void redrawCell( int viewColumn, int viewRow )
  {
    // request redraw specified table body or header cell
    if ( m_fullRedraw )
      return;
    if ( m_cells.add( (long) viewColumn << 32 | viewRow & 0xFFFFFFFFL ) )
      m_redrawCount += REDRAW_COUNT_CELL;
    schedule();
  }

  /**************************************** redrawColumn *****************************************/
  public void redrawColumn( int viewColumn )
  {
    // request redraw visible bit of column including header
    if ( m_fullRedraw )
      return;
    if ( m_columns.add( viewColumn ) )
      m_redrawCount += REDRAW_COUNT_COLUMN;
    schedule();
  }

  /****************************************** redrawRow ******************************************/
  public void redrawRow( int viewRow )
  {
    // request redraw visible bit of row including header
    if ( m_fullRedraw )
      return;
    if ( m_rows.add( viewRow ) )
      m_redrawCount += REDRAW_COUNT_ROW;
    schedule();
  }

  /****************************************** schedule *******************************************/
  private void schedule()
  {
    // schedule redrawing of what has been requested
    if ( m_redrawIsRequested.compareAndSet( false, true ) )
      Platform.runLater( () -> performRedraws() );
  }

  /*************************************** performRedraws ****************************************/
  private void performRedraws()
  {
    // redraw parts of table or overlay that have been requested
    m_redrawIsRequested.set( false );

    if ( isVisible() )
    {
      // redraw overlay if requested or full redraw requested
      if ( m_overlayRedraw || m_fullRedraw )
        getOverlay().redrawNow();

      // ensure full canvas is redrawn if many columns/rows/cells are redrawn
      if ( m_fullRedraw || m_redrawCount > REDRAW_COUNT_MAX )
      {
        // full redraw requested so don't need to redraw anything else
        redrawNow();
        m_redrawCount = 0;
      }
      else
      {
        // redraw requested cells that aren't covered by requested columns & rows
        for ( long hash : m_cells )
        {
          int viewColumn = (int) ( hash >> 32 );
          int viewRow = (int) hash;
          if ( !m_columns.contains( viewColumn ) && !m_rows.contains( viewRow ) )
            redrawCellNow( viewColumn, viewRow );
        }

        // redraw requested columns & rows
        var columnIter = m_columns.iterator();
        while ( columnIter.hasNext() )
          redrawColumnNow( columnIter.next() );
        var rowIter = m_rows.iterator();
        while ( rowIter.hasNext() )
          redrawRowNow( rowIter.next() );
      }
    }

    // clear requests
    m_fullRedraw = false;
    m_overlayRedraw = false;
    m_columns.clear();
    m_rows.clear();
    m_cells.clear();
  }

  /****************************************** redrawNow ******************************************/
  private void redrawNow()
  {
    // request complete redraw of table canvas
    if ( getHeight() > 0.0 )
    {
      getGraphicsContext2D().clearRect( 0.0, 0.0, getWidth(), getHeight() );
      int minColumnPos = m_view.getColumnIndex( m_view.getHeaderWidth() );
      int maxColumnPos = m_view.getColumnIndex( (int) getWidth() );
      redrawColumnsNow( minColumnPos, maxColumnPos );
      redrawColumnNow( HEADER );
    }
  }

  /*************************************** redrawCellNow *****************************************/
  private void redrawCellNow( int viewColumn, int viewRow )
  {
    // redraw table body or header cell
    CellDrawer cell = m_view.getCellDrawer();
    if ( viewColumn >= HEADER && viewRow >= HEADER )
    {
      cell.setIndex( m_view, viewColumn, viewRow );
      if ( cell.w > 0.0 && cell.h > 0.0 )
        cell.draw();
    }
  }

  /*************************************** redrawColumnNow ***************************************/
  protected void redrawColumnNow( int viewColumn )
  {
    // redraw visible bit of column including column header if column is not hidden
    int columnWidth = m_view.getColumnsAxis().getPixelSize( viewColumn );
    if ( columnWidth <= 0 )
      return;

    // prepare cell drawer
    CellDrawer cell = m_view.getCellDrawer();
    cell.view = m_view;
    cell.gc = getGraphicsContext2D();
    cell.viewColumn = viewColumn;
    cell.x = m_view.getColumnStartX( viewColumn );
    cell.w = columnWidth;

    // redraw between visible min and max rows inclusive
    int minRow = m_view.getRowIndex( m_view.getHeaderHeight() );
    int maxRow = m_view.getRowIndex( (int) getHeight() );
    int max = m_view.getData().getRowCount() - 1;
    if ( minRow < FIRSTCELL )
      minRow = FIRSTCELL;
    if ( maxRow > max )
      maxRow = max;

    // redraw the column visible body cells
    cell.y = m_view.getRowStartY( minRow );
    for ( cell.viewRow = minRow; cell.viewRow <= maxRow; cell.viewRow++ )
    {
      cell.h = m_view.getRowStartY( cell.viewRow + 1 ) - cell.y;
      if ( cell.h > 0.0 )
      {
        cell.draw();
        cell.y += cell.h;
      }
    }

    // redraw column header cell
    cell.viewRow = HEADER;
    cell.y = 0.0;
    cell.h = m_view.getHeaderHeight();
    cell.draw();
  }

  /**************************************** redrawRowNow *****************************************/
  protected void redrawRowNow( int viewRow )
  {
    // redraw visible bit of row including row header if row is not hidden
    int rowHeight = m_view.getRowsAxis().getPixelSize( viewRow );
    if ( rowHeight <= 0 )
      return;

    // prepare cell drawer
    CellDrawer cell = m_view.getCellDrawer();
    cell.view = m_view;
    cell.gc = getGraphicsContext2D();
    cell.viewRow = viewRow;
    cell.y = m_view.getRowStartY( viewRow );
    cell.h = rowHeight;

    // redraw between visible min and max columns inclusive
    int minColumn = m_view.getColumnIndex( m_view.getHeaderWidth() );
    int maxColumn = m_view.getColumnIndex( (int) getWidth() );
    int max = m_view.getData().getColumnCount() - 1;
    if ( minColumn < FIRSTCELL )
      minColumn = FIRSTCELL;
    if ( maxColumn > max )
      maxColumn = max;

    // redraw the row visible body cells
    cell.x = m_view.getColumnStartX( minColumn );
    for ( cell.viewColumn = minColumn; cell.viewColumn <= maxColumn; cell.viewColumn++ )
    {
      cell.w = m_view.getColumnStartX( cell.viewColumn + 1 ) - cell.x;
      if ( cell.w > 0.0 )
      {
        cell.draw();
        cell.x += cell.w;
      }
    }

    // redraw row header cell
    cell.viewColumn = HEADER;
    cell.x = 0.0;
    cell.w = m_view.getHeaderWidth();
    cell.draw();
  }

  /************************************* redrawColumnsNow ****************************************/
  protected void redrawColumnsNow( int minColumn, int maxColumn )
  {
    // redraw all table body columns between min and max column positions inclusive
    int max = m_view.getData().getColumnCount() - 1;
    if ( minColumn <= max && maxColumn >= FIRSTCELL )
    {
      if ( minColumn < FIRSTCELL )
        minColumn = FIRSTCELL;
      if ( maxColumn > max )
        maxColumn = max;

      for ( int index = minColumn; index <= maxColumn; index++ )
        redrawColumnNow( index );
    }
  }

  /*************************************** redrawRowsNow *****************************************/
  protected void redrawRowsNow( int minRow, int maxRow )
  {
    // redraw all table body rows between min and max row positions inclusive
    int max = m_view.getData().getRowCount() - 1;
    if ( minRow <= max && maxRow >= FIRSTCELL )
    {
      if ( minRow < FIRSTCELL )
        minRow = FIRSTCELL;
      if ( maxRow > max )
        maxRow = max;

      for ( int index = minRow; index <= maxRow; index++ )
        redrawRowNow( index );
    }
  }

  /***************************************** getOverlay ******************************************/
  public TableOverlay getOverlay()
  {
    // return the table canvas overlay
    return m_overlay;
  }

}