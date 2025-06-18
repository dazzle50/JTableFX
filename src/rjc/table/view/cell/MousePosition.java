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

import javafx.application.Platform;
import rjc.table.signal.ObservablePosition;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;
import rjc.table.view.cursor.Cursors;

/*************************************************************************************************/
/********************** Observable table-view cell position from mouse x-y ***********************/
/*************************************************************************************************/

public class MousePosition extends ObservablePosition
{
  private TableView        m_view;                       // associated table-view

  private int              m_x;                          // latest event mouse x coordinate
  private int              m_y;                          // latest event mouse y coordinate
  private int              m_cellXstart;                 // current mouse cell X start
  private int              m_cellXend;                   // current mouse cell X end
  private int              m_cellYstart;                 // current mouse cell Y start
  private int              m_cellYend;                   // current mouse cell Y end

  final static private int PROXIMITY = 4;                // used to distinguish resize from reorder

  final static private int HEADER    = TableAxis.HEADER;
  final static private int BEFORE    = TableAxis.BEFORE;
  final static private int INVALID   = TableAxis.INVALID;

  /**************************************** constructor ******************************************/
  public MousePosition( TableView view )
  {
    // construct
    m_view = view;
  }

  /***************************************** setPosition *****************************************/
  @Override
  public void setPosition( int columnIndex, int rowIndex )
  {
    // don't allow direct setting position
    throw new UnsupportedOperationException( "Use setXY() or setInvalid() instead" );
  }

  /***************************************** setInvalid ******************************************/
  public void setInvalid()
  {
    // set position index to invalid
    m_cellXend = INVALID;
    m_cellYend = INVALID;
    m_x = INVALID;
    m_y = INVALID;
    super.setPosition( INVALID, INVALID );
  }

  /******************************************* checkXY *******************************************/
  public void checkXY()
  {
    // re-check mouse cell position
    m_cellXend = INVALID;
    m_cellYend = INVALID;
    Platform.runLater( () -> setXY( m_x, m_y, false ) );
  }

  /******************************************** setXY ********************************************/
  public void setXY( int x, int y, boolean updateCursor )
  {
    // determine mouse cell position
    m_x = x;
    m_y = y;

    // check if mouse moved outside current column
    int viewColumn = getColumn();
    int width = m_view.getTableWidth();
    int header = m_view.getHeaderWidth();

    if ( m_x < m_cellXstart || m_x >= m_cellXend )
    {
      if ( m_x < 0 )
      {
        m_cellXstart = Integer.MIN_VALUE;
        m_cellXend = 0;
        viewColumn = BEFORE;
      }
      else if ( m_x < header )
      {
        m_cellXstart = 0;
        m_cellXend = header;
        viewColumn = HEADER;
      }
      else if ( m_x >= width )
      {
        m_cellXstart = width;
        m_cellXend = Integer.MAX_VALUE;
        viewColumn = m_view.getData().getColumnCount() - 1;
      }
      else
      {
        viewColumn = m_view.getColumnIndex( m_x );
        m_cellXstart = Math.max( m_view.getColumnStartX( viewColumn ), header );
        m_cellXend = m_view.getColumnStartX( viewColumn + 1 );
      }
    }

    // check if mouse moved outside current row
    int viewRow = getRow();
    int height = m_view.getTableHeight();
    header = m_view.getHeaderHeight();

    if ( m_y < m_cellYstart || m_y >= m_cellYend )
    {
      if ( m_y < 0 )
      {
        m_cellYstart = Integer.MIN_VALUE;
        m_cellYend = 0;
        viewRow = BEFORE;
      }
      else if ( m_y < header )
      {
        m_cellYstart = 0;
        m_cellYend = header;
        viewRow = HEADER;
      }
      else if ( m_y >= height )
      {
        m_cellYstart = height;
        m_cellYend = Integer.MAX_VALUE;
        viewRow = m_view.getData().getRowCount() - 1;
      }
      else
      {
        viewRow = m_view.getRowIndex( m_y );
        m_cellYstart = Math.max( m_view.getRowStartY( viewRow ), header );
        m_cellYend = m_view.getRowStartY( viewRow + 1 );
      }
    }

    // set mouse updated position
    super.setPosition( viewColumn, viewRow );

    // update the mouse cursor if requested
    if ( updateCursor )
      setCursor();
  }

  /****************************************** setCursor ******************************************/
  private void setCursor()
  {
    // if over table headers corner, set cursor to default
    int column = m_view.getMouseCell().getColumn();
    int row = m_view.getMouseCell().getRow();

    int headerHeight = m_view.getHeaderHeight();
    int headerWidth = m_view.getHeaderWidth();

    // if header corner cell
    if ( m_x < headerWidth && m_y < headerHeight )
    {
      m_view.setCursor( Cursors.CORNER_CELL );
      return;
    }

    // if beyond table cells, set cursor to default
    if ( m_x >= m_view.getTableWidth() || m_y >= m_view.getTableHeight() )
    {
      m_view.setCursor( Cursors.DEFAULT );
      return;
    }

    // if over column header, check if resize, move, or select
    if ( m_y < headerHeight )
    {
      if ( ( m_x - m_cellXstart <= PROXIMITY && m_view.isColumnResizable( column - 1 ) )
          || ( m_cellXend - m_x <= PROXIMITY && m_view.isColumnResizable( column ) ) )
      {
        m_view.setCursor( Cursors.COLUMNS_RESIZE );
        return;
      }

      // if column is selected, set cursor to move
      if ( m_view.getSelection().isColumnSelected( column ) )
      {
        m_view.setCursor( Cursors.COLUMNS_REORDER );
        return;
      }

      // otherwise, set cursor to down-arrow for selecting
      m_view.setCursor( Cursors.COLUMNS_HOVER );
      return;
    }

    // if over vertical header, check if resize, move, or select
    if ( m_x < headerWidth )
    {
      // if near row edge, set cursor to resize
      if ( ( m_y - m_cellYstart <= PROXIMITY && m_view.isRowResizable( row - 1 ) )
          || ( m_cellYend - m_y <= PROXIMITY && m_view.isRowResizable( row ) ) )
      {
        m_view.setCursor( Cursors.ROWS_RESIZE );
        return;
      }

      // if row is selected, set cursor to move
      if ( m_view.getSelection().isRowSelected( row ) )
      {
        m_view.setCursor( Cursors.ROWS_REORDER );
        return;
      }

      // otherwise, set cursor to right-arrow for selecting
      m_view.setCursor( Cursors.ROWS_HOVER );
      return;
    }

    // mouse over table body cells
    m_view.setCursor( Cursors.CELLS_HOVER );
  }

}