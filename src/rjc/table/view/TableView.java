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
import rjc.table.Utils;
import rjc.table.data.TableData;
import rjc.table.data.TableData.Signal;
import rjc.table.signal.ObservableDouble;
import rjc.table.signal.ObservableStatus;
import rjc.table.undo.UndoStack;
import rjc.table.view.cell.CellDrawer;
import rjc.table.view.cell.CellSelection;
import rjc.table.view.cell.ViewPosition;

/*************************************************************************************************/
/************** Base class for scrollable table-view to visualise a table-data model *************/
/*************************************************************************************************/

public class TableView extends TableViewParent
{
  private TableData        m_data;

  private TableCanvas      m_canvas;
  private TableScrollBar   m_verticalScrollBar;
  private TableScrollBar   m_horizontalScrollBar;

  private CellDrawer       m_drawer;
  private CellSelection    m_selection;
  private UndoStack        m_undostack;
  private ObservableStatus m_status;
  private ObservableDouble m_zoom;

  private ViewPosition     m_focusCell;
  private ViewPosition     m_selectCell;

  /**************************************** constructor ******************************************/
  public TableView( TableData data, String name )
  {
    // construct the table view
    if ( data == null )
      throw new NullPointerException( "TableData must not be null" );
    m_data = data;
    setId( name );

    // assemble the table-view components
    m_canvas = new TableCanvas( this );
    m_horizontalScrollBar = new TableScrollBar( m_canvas.getColumnsAxis(), Orientation.HORIZONTAL );
    m_verticalScrollBar = new TableScrollBar( m_canvas.getRowsAxis(), Orientation.VERTICAL );
    getChildren().addAll( m_canvas, m_canvas.getOverlay(), m_horizontalScrollBar, m_verticalScrollBar );

    // create observable zoom parameter, and tell the canvas axis
    m_zoom = new ObservableDouble( 1.0 );
    m_canvas.getColumnsAxis().setZoomProperty( m_zoom.getReadOnly() );
    m_canvas.getRowsAxis().setZoomProperty( m_zoom.getReadOnly() );

    // create observable positions for focus & select, and cell-selection store
    m_focusCell = new ViewPosition( this );
    m_selectCell = new ViewPosition( this );
    m_selection = new CellSelection( this );

    // add event handlers & reset table view to default settings
    addEventHandlers();
    reset();
  }

  /************************************** addEventHandlers ***************************************/
  protected void addEventHandlers()
  {
    // react to losing & gaining focus and visibility
    focusedProperty().addListener( ( observable, oldFocus, newFocus ) -> redraw() );
    visibleProperty().addListener( ( observable, oldVisibility, newVisibility ) ->
    {
      layoutDisplay();
      redraw();
    } );

    // react to size changes (don't use layoutChildren as that gets called even when scrolling)
    widthProperty().addListener( ( sender, msg ) -> layoutDisplay() );
    heightProperty().addListener( ( sender, msg ) -> layoutDisplay() );

    // react to scroll bar position value changes
    m_horizontalScrollBar.valueProperty().addListener( ( observable, oldValue, newValue ) -> tableScrolled() );
    m_verticalScrollBar.valueProperty().addListener( ( observable, oldValue, newValue ) -> tableScrolled() );

    // react to data model signals
    m_data.addListener( ( sender, msg ) ->
    {
      Signal change = (Signal) msg[0];
      if ( change == Signal.TABLE_VALUES_CHANGED )
        redraw();
      else if ( change == Signal.COLUMN_VALUES_CHANGED )
        m_canvas.redrawColumn( (int) msg[1] );
      else if ( change == Signal.ROW_VALUES_CHANGED )
        m_canvas.redrawRow( (int) msg[1] );
      else if ( change == Signal.CELL_VALUE_CHANGED )
        m_canvas.redrawCell( (int) msg[1], (int) msg[2] );
    } );
  }

  /**************************************** tableScrolled ****************************************/
  private void tableScrolled()
  {
    // handle any actions needed due to view being modified usually scrolled
    redraw();
    // getMouseCell().checkXY();
    // CellEditorBase.endEditing();
  }

  /******************************************** reset ********************************************/
  public void reset()
  {
    // reset table view to default settings
    m_canvas.getColumnsAxis().reset();
    m_canvas.getRowsAxis().reset();
    m_canvas.getRowsAxis().setDefaultSize( 20 );
    m_canvas.getRowsAxis().setHeaderSize( 20 );
  }

  /******************************************* redraw ********************************************/
  public void redraw()
  {
    // request redraw of full visible table (headers and body)
    getCanvas().redraw();
  }

  /****************************************** getData ********************************************/
  public TableData getData()
  {
    // return data model for table-view
    return m_data;
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

  /**************************************** getCellDrawer ****************************************/
  public CellDrawer getCellDrawer()
  {
    // return class responsible for drawing the cells on canvas
    if ( m_drawer == null )
      m_drawer = new CellDrawer();
    return m_drawer;
  }

  /**************************************** layoutDisplay ****************************************/
  protected void layoutDisplay()
  {
    // do nothing if not visible or width/height not set
    if ( !isVisible() || getWidth() == prefWidth( 0 ) || getHeight() == prefHeight( 0 ) )
      return;

    Utils.trace( "===============", getId(), getWidth(), getHeight() );

    // determine which scroll-bars should be visible
    int tableHeight = m_canvas.getRowsAxis().getTotalPixels();
    int tableWidth = m_canvas.getColumnsAxis().getTotalPixels();
    int scrollbarSize = (int) getVerticalScrollBar().getWidth();

    boolean isVSBvisible = getHeight() < tableHeight;
    int canvasWidth = isVSBvisible ? getWidth() - scrollbarSize : getWidth();
    boolean isHSBvisible = canvasWidth < tableWidth;
    int canvasHeight = isHSBvisible ? getHeight() - scrollbarSize : getHeight();
    isVSBvisible = canvasHeight < tableHeight;
    canvasWidth = isVSBvisible ? getWidth() - scrollbarSize : getWidth();

    // update vertical scroll bar
    var sb = getVerticalScrollBar();
    sb.setVisible( isVSBvisible );
    if ( isVSBvisible )
    {
      sb.setPrefHeight( canvasHeight );
      sb.relocate( getWidth() - scrollbarSize, 0.0 );

      double max = tableHeight - canvasHeight;
      sb.setMax( max );
      sb.setVisibleAmount( max * canvasHeight / tableHeight );
      sb.setBlockIncrement( canvasHeight - m_canvas.getRowsAxis().getHeaderPixels() );

      if ( sb.getValue() > max )
        sb.setValue( max );
    }
    else
    {
      sb.setValue( 0.0 );
      sb.setMax( 0.0 );
    }

    // update horizontal scroll bar
    sb = getHorizontalScrollBar();
    sb.setVisible( isHSBvisible );
    if ( isHSBvisible )
    {
      sb.setPrefWidth( canvasWidth );
      sb.relocate( 0.0, getHeight() - scrollbarSize );

      double max = tableWidth - canvasWidth;
      sb.setMax( max );
      sb.setVisibleAmount( max * canvasWidth / tableWidth );
      sb.setBlockIncrement( canvasWidth - m_canvas.getColumnsAxis().getHeaderPixels() );

      if ( sb.getValue() > max )
        sb.setValue( max );
    }
    else
    {
      sb.setValue( 0.0 );
      sb.setMax( 0.0 );
    }

    // update canvas size (table + blank excess space)
    m_canvas.resize( canvasWidth, canvasHeight );
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return as string
    return getClass().getSimpleName() + "@" + Integer.toHexString( System.identityHashCode( this ) ) + "[ID=" + getId()
        + " m_canvas=" + m_canvas + "]";
  }

}
