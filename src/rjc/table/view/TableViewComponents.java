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
import rjc.table.view.axis.TableAxis;
import rjc.table.view.cell.CellSelection;
import rjc.table.view.cell.MousePosition;
import rjc.table.view.cell.ViewPosition;

/*************************************************************************************************/
/************** Table-view components to visualise & interact with table-data model **************/
/*************************************************************************************************/

public class TableViewComponents extends TableViewParent
{
  private TableCanvas      m_canvas;
  private TableScrollBar   m_verticalScrollBar;
  private TableScrollBar   m_horizontalScrollBar;

  private TableAxis        m_columnsAxis;        // columns (horizontal) axis
  private TableAxis        m_rowsAxis;           // rows (vertical) axis

  private UndoStack        m_undostack;
  private ObservableStatus m_status;
  private ObservableDouble m_zoom;

  private CellSelection    m_selection;
  private ViewPosition     m_focusCell;
  private ViewPosition     m_selectCell;
  private MousePosition    m_mouseCell;

  /************************************** assembleElements ***************************************/
  void assembleView()
  {
    // assemble the table-view components
    TableView view = (TableView) this;
    m_columnsAxis = new TableAxis( view.getData().columnCountProperty() );
    m_rowsAxis = new TableAxis( view.getData().rowCountProperty() );
    m_canvas = new TableCanvas( view );
    m_horizontalScrollBar = new TableScrollBar( m_columnsAxis, Orientation.HORIZONTAL );
    m_verticalScrollBar = new TableScrollBar( m_rowsAxis, Orientation.VERTICAL );
    getChildren().addAll( m_canvas, m_canvas.getOverlay(), m_horizontalScrollBar, m_verticalScrollBar );

    // create observable zoom parameter, and tell the canvas axis
    m_zoom = new ObservableDouble( 1.0 );
    m_columnsAxis.setZoomProperty( m_zoom.getReadOnly() );
    m_rowsAxis.setZoomProperty( m_zoom.getReadOnly() );

    // create observable positions for mouse, focus & select, and cell-selection store
    m_mouseCell = new MousePosition( view );
    m_focusCell = new ViewPosition( view );
    m_selectCell = new ViewPosition( view );
    m_selection = new CellSelection( view );

    // call reset to set view to default settings
    reset();
  }

  /******************************************** reset ********************************************/
  public void reset()
  {
    // reset table view to default settings
    getColumnsAxis().reset();
    getRowsAxis().reset();
    getRowsAxis().setDefaultSize( 20 );
    getRowsAxis().setHeaderSize( 20 );
  }

  /**************************************** setUndostack *****************************************/
  public void setUndostack( UndoStack undostack )
  {
    // set undo-stack for table-view
    m_undostack = undostack == null ? new UndoStack() : undostack;
  }

  /***************************************** getUndoStack ****************************************/
  public UndoStack getUndoStack()
  {
    // return undo-stack for table-view (create if necessary)
    if ( m_undostack == null )
      m_undostack = new UndoStack();
    return m_undostack;
  }

  /***************************************** setStatus *******************************************/
  public void setStatus( ObservableStatus status )
  {
    // set status for table-view
    m_status = status == null ? new ObservableStatus() : status;
  }

  /***************************************** getStatus *******************************************/
  public ObservableStatus getStatus()
  {
    // return status object for table-view
    return m_status;
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

  /*************************************** getColumnsAxis ****************************************/
  public TableAxis getColumnsAxis()
  {
    // return columns (horizontal) axis for cell widths & x-coordinates
    return m_columnsAxis;
  }

  /***************************************** getRowsAxis *****************************************/
  public TableAxis getRowsAxis()
  {
    // return rows (vertical) axis for cell heights & y-coordinates
    return m_rowsAxis;
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
    return m_columnsAxis.getStartPixel( viewColumn, (int) getHorizontalScrollBar().getValue() );
  }

  /**************************************** getRowStartY *****************************************/
  public int getRowStartY( int viewRow )
  {
    // return y coordinate of cell start for specified row position
    return m_rowsAxis.getStartPixel( viewRow, (int) getVerticalScrollBar().getValue() );
  }

  /*************************************** getColumnIndex ****************************************/
  public int getColumnIndex( int xCoordinate )
  {
    // return column index at specified x coordinate
    return m_columnsAxis.getIndexFromCoordinate( xCoordinate, (int) getHorizontalScrollBar().getValue() );
  }

  /***************************************** getRowIndex *****************************************/
  public int getRowIndex( int yCoordinate )
  {
    // return row index at specified y coordinate
    return m_rowsAxis.getIndexFromCoordinate( yCoordinate, (int) getVerticalScrollBar().getValue() );
  }

  /*************************************** getHeaderHeight ***************************************/
  public int getHeaderHeight()
  {
    // return table header height in pixels
    return m_rowsAxis.getHeaderPixels();
  }

  /*************************************** getHeaderWidth ****************************************/
  public int getHeaderWidth()
  {
    // return table header width in pixels
    return m_columnsAxis.getHeaderPixels();
  }

  /**************************************** getTableHeight ***************************************/
  public int getTableHeight()
  {
    // return whole table (including header) height in pixels
    return m_rowsAxis.getTotalPixels();
  }

  /**************************************** getTableWidth ****************************************/
  public int getTableWidth()
  {
    // return whole table (including header) width in pixels
    return m_columnsAxis.getTotalPixels();
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
