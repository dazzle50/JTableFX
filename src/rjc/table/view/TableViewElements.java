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

package rjc.table.view;

import javafx.geometry.Orientation;
import rjc.table.signal.ObservableDouble;
import rjc.table.signal.ObservableStatus;
import rjc.table.undo.UndoStack;
import rjc.table.view.cell.CellSelection;
import rjc.table.view.cell.MousePosition;
import rjc.table.view.cell.ViewPosition;

/*************************************************************************************************/
/************** Base class for scrollable table-view to visualise a table-data model *************/
/*************************************************************************************************/

public class TableViewElements extends TableViewParent
{
  private TableCanvas      m_canvas;
  private TableScrollBar   m_verticalScrollBar;
  private TableScrollBar   m_horizontalScrollBar;

  private UndoStack        m_undostack;
  private ObservableStatus m_status;
  private ObservableDouble m_zoom;

  private CellSelection    m_selection;
  private ViewPosition     m_focusCell;
  private ViewPosition     m_selectCell;
  private MousePosition    m_mouseCell;

  /************************************** assembleElements ***************************************/
  void assembleElements()
  {
    // assemble the table-view components
    TableView view = (TableView) this;
    m_canvas = new TableCanvas( view );
    m_horizontalScrollBar = new TableScrollBar( m_canvas.getColumnsAxis(), Orientation.HORIZONTAL );
    m_verticalScrollBar = new TableScrollBar( m_canvas.getRowsAxis(), Orientation.VERTICAL );
    getChildren().addAll( m_canvas, m_canvas.getOverlay(), m_horizontalScrollBar, m_verticalScrollBar );

    // create observable zoom parameter, and tell the canvas axis
    m_zoom = new ObservableDouble( 1.0 );
    m_canvas.getColumnsAxis().setZoomProperty( m_zoom.getReadOnly() );
    m_canvas.getRowsAxis().setZoomProperty( m_zoom.getReadOnly() );

    // create observable positions for mouse, focus & select, and cell-selection store
    m_mouseCell = new MousePosition( view );
    m_focusCell = new ViewPosition( view );
    m_selectCell = new ViewPosition( view );
    m_selection = new CellSelection( view );
  }

  /**************************************** setUndostack *****************************************/
  public void setUndostack( UndoStack undostack )
  {
    // set undo-stack for table-view
    m_undostack = undostack == null ? new UndoStack() : undostack;
  }

  /***************************************** setStatus *******************************************/
  public void setStatus( ObservableStatus status )
  {
    // set status for table-view
    m_status = status == null ? new ObservableStatus() : status;
  }

  /****************************************** getZoom ********************************************/
  public ObservableDouble getZoom()
  {
    // return observable zoom factor (1.0 is normal 100% size) for table-view
    return m_zoom;
  }

  /**************************************** getMouseCell *****************************************/
  public MousePosition getMouseCell()
  {
    // return observable mouse cell position on table-view
    return m_mouseCell;
  }

  /**************************************** getFocusCell *****************************************/
  public ViewPosition getFocusCell()
  {
    // return observable focus cell position on table-view
    return m_focusCell;
  }

  /**************************************** getSelectCell ****************************************/
  public ViewPosition getSelectCell()
  {
    // return observable select cell position on table-view
    return m_selectCell;
  }

  /**************************************** getSelection *****************************************/
  public CellSelection getSelection()
  {
    // return selection model for table-view
    return m_selection;
  }

  /***************************************** getCanvas *******************************************/
  public TableCanvas getCanvas()
  {
    // return canvas (shows table headers & body cells + BLANK excess space) for table-view
    return m_canvas;
  }

  /*********************************** getHorizontalScrollBar ************************************/
  public TableScrollBar getHorizontalScrollBar()
  {
    // return horizontal scroll bar (will not be visible if not needed) for table-view
    return m_horizontalScrollBar;
  }

  /************************************ getVerticalScrollBar *************************************/
  public TableScrollBar getVerticalScrollBar()
  {
    // return vertical scroll bar (will not be visible if not needed) for table-view
    return m_verticalScrollBar;
  }

  /*************************************** getColumnStartX ***************************************/
  public int getColumnStartX( int viewColumn )
  {
    // return x coordinate of cell start for specified column position
    return m_canvas.getColumnsAxis().getStartPixel( viewColumn, (int) getHorizontalScrollBar().getValue() );
  }

  /**************************************** getRowStartY *****************************************/
  public int getRowStartY( int viewRow )
  {
    // return y coordinate of cell start for specified row position
    return m_canvas.getRowsAxis().getStartPixel( viewRow, (int) getVerticalScrollBar().getValue() );
  }

  /*************************************** getColumnIndex ****************************************/
  public int getColumnIndex( int xCoordinate )
  {
    // return column index at specified x coordinate
    return m_canvas.getColumnsAxis().getIndexFromCoordinate( xCoordinate, (int) getHorizontalScrollBar().getValue() );
  }

  /***************************************** getRowIndex *****************************************/
  public int getRowIndex( int yCoordinate )
  {
    // return row index at specified y coordinate
    return m_canvas.getRowsAxis().getIndexFromCoordinate( yCoordinate, (int) getVerticalScrollBar().getValue() );
  }

  /*************************************** getHeaderHeight ***************************************/
  public int getHeaderHeight()
  {
    // return table header height in pixels
    return m_canvas.getRowsAxis().getHeaderPixels();
  }

  /*************************************** getHeaderWidth ****************************************/
  public int getHeaderWidth()
  {
    // return table header width in pixels
    return m_canvas.getColumnsAxis().getHeaderPixels();
  }

  /**************************************** getTableHeight ***************************************/
  public int getTableHeight()
  {
    // return whole table (including header) height in pixels
    return m_canvas.getRowsAxis().getTotalPixels();
  }

  /**************************************** getTableWidth ****************************************/
  public int getTableWidth()
  {
    // return whole table (including header) width in pixels
    return m_canvas.getColumnsAxis().getTotalPixels();
  }

  /************************************** isColumnResizable **************************************/
  public boolean isColumnResizable( int viewColumn )
  {
    // return if column is resizable
    return true;
  }

  /*************************************** isRowResizable ****************************************/
  public boolean isRowResizable( int viewRow )
  {
    // return if row is resizable
    return true;
  }

}
