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

import rjc.table.data.TableData;
import rjc.table.data.TableData.Signal;
import rjc.table.signal.ObservablePosition;
import rjc.table.view.TableScrollBar.Animation;
import rjc.table.view.axis.TableAxis;
import rjc.table.view.cell.CellDrawer;
import rjc.table.view.cell.CellLocation;
import rjc.table.view.cursor.Cursors;
import rjc.table.view.editor.CellEditorBase;
import rjc.table.view.events.KeyPressed;
import rjc.table.view.events.KeyTyped;
import rjc.table.view.events.MouseDragged;
import rjc.table.view.events.MouseMoved;
import rjc.table.view.events.MousePressed;
import rjc.table.view.events.MouseReleased;

/*************************************************************************************************/
/************** Base class for scrollable table-view to visualise a table-data model *************/
/*************************************************************************************************/

public class TableView extends TableViewAssemble
{
  private TableData  m_data;
  private CellDrawer m_drawer;

  /**************************************** constructor ******************************************/
  public TableView( TableData data, String name )
  {
    // construct the table view
    if ( data == null )
      throw new NullPointerException( "TableData must not be null" );
    m_data = data;
    setId( name );

    // add event handlers & reset table view to default settings
    assembleView();
    addEventHandlers();
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
    getHorizontalScrollBar().valueProperty().addListener( ( observable, oldValue, newValue ) -> tableScrolled() );
    getVerticalScrollBar().valueProperty().addListener( ( observable, oldValue, newValue ) -> tableScrolled() );

    // react to select-cell position and changes to cell selections
    getSelectCell().addLaterListener( ( sender, msg ) ->
    {
      getSelection().update();

      // scroll to show select cell unless selecting using mouse which has its own scrolling behaviour
      if ( !Cursors.isSelecting( getCursor() ) )
        scrollTo( getSelectCell() );
    } );
    getSelection().addLaterListener( ( sender, msg ) ->
    {
      getCanvas().redrawColumn( TableAxis.HEADER );
      getCanvas().redrawRow( TableAxis.HEADER );
      getCanvas().redrawOverlay();
    } );

    // react to mouse cell-position as might be used for selecting
    getMouseCell().addListener( ( sender, msg ) -> checkSelectPosition() );

    // react to zoom values changes
    getZoom().addListener( ( sender, msg ) ->
    {
      layoutDisplay();
      tableScrolled();
    } );

    // react to data model signals
    m_data.addListener( ( sender, msg ) ->
    {
      Signal change = (Signal) msg[0];
      if ( change == Signal.TABLE_VALUES_CHANGED )
        redraw();
      else if ( change == Signal.COLUMN_VALUES_CHANGED )
        getCanvas().redrawColumn( (int) msg[1] );
      else if ( change == Signal.ROW_VALUES_CHANGED )
        getCanvas().redrawRow( (int) msg[1] );
      else if ( change == Signal.CELL_VALUE_CHANGED )
        getCanvas().redrawCell( (int) msg[1], (int) msg[2] );
    } );

    // react to keyboard events
    setOnKeyPressed( new KeyPressed() );
    setOnKeyTyped( new KeyTyped() );

    // react to mouse events on table body top node (is the overlay)
    var overlay = getCanvas().getOverlay();
    overlay.setOnMouseMoved( new MouseMoved() );
    overlay.setOnMousePressed( new MousePressed() );
    overlay.setOnMouseDragged( new MouseDragged() );
    overlay.setOnMouseReleased( new MouseReleased() );
    // TODO overlay.setOnMouseClicked( new MouseClicked() );
    // TODO overlay.setOnMouseExited( new MouseExited() );
    // TODO overlay.setOnMouseEntered( new MouseEntered() );
    // TODO overlay.setOnScroll( new MouseScroll() );
    // TODO overlay.setOnContextMenuRequested( new ContextMenu() );
  }

  /************************************* checkSelectPosition *************************************/
  private void checkSelectPosition()
  {
    // if not selecting then nothing to do and just return
    if ( !Cursors.isSelecting( getCursor() ) )
      return;

    // update select cell position
    int column = checkColumnPosition();
    int row = checkRowPosition();
    getSelectCell().setPosition( column, row );
  }

  /************************************* checkColumnPosition *************************************/
  private int checkColumnPosition()
  {
    // if mouse is beyond the table, limit to last visible column/row
    var axis = getCanvas().getColumnsAxis();
    int column = Math.min( axis.getLastVisible(), getMouseCell().getColumn() );

    // if selecting rows ignore mouse column
    column = getCursor() == Cursors.SELECTING_ROWS ? getSelectCell().getColumn() : column;

    // if animating to start or end, ensure selection edge is visible on view
    var animation = getHorizontalScrollBar().getAnimation();
    if ( animation == Animation.TO_START )
    {
      column = getColumnIndex( getHeaderWidth() );
      column = axis.getNextVisible( column );
    }
    if ( animation == Animation.TO_END )
    {
      column = getColumnIndex( (int) getCanvas().getWidth() );
      column = axis.getPreviousVisible( column );
    }

    return column;
  }

  /************************************** checkRowPosition ***************************************/
  private int checkRowPosition()
  {
    // if mouse is beyond the table, limit to last visible column/row
    var rowAxis = getCanvas().getRowsAxis();
    int row = Math.min( rowAxis.getLastVisible(), getMouseCell().getRow() );

    // if selecting columns ignore mouse row
    row = getCursor() == Cursors.SELECTING_COLS ? getSelectCell().getRow() : row;

    var animation = getVerticalScrollBar().getAnimation();
    if ( animation == Animation.TO_START )
    {
      row = getRowIndex( getHeaderHeight() );
      row = rowAxis.getNextVisible( row );
    }
    if ( animation == Animation.TO_END )
    {
      row = getRowIndex( (int) getCanvas().getHeight() );
      row = rowAxis.getPreviousVisible( row );
    }

    return row;
  }

  /**************************************** tableScrolled ****************************************/
  private void tableScrolled()
  {
    // handle any actions needed due to view being modified usually scrolled
    redraw();
    getMouseCell().checkXY();
    // CellEditorBase.endEditing();

    // TODO if column/row resize in progress, no need to do anything more

    // TODO if column/row resize in progress, no need to do anything more

    // check selected cell position
    checkSelectPosition();
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

  /**************************************** setCellDrawer ****************************************/
  public void setCellDrawer( CellDrawer drawer )
  {
    // set class responsible for drawing the cells on canvas
    m_drawer = drawer;
  }

  /**************************************** getCellDrawer ****************************************/
  public CellDrawer getCellDrawer()
  {
    // return class responsible for drawing the cells on canvas
    if ( m_drawer == null )
      m_drawer = new CellDrawer();
    return m_drawer;
  }

  /**************************************** getCellEditor ****************************************/
  public CellEditorBase getCellEditor( CellLocation cell )
  {
    // return cell editor control (or null if cell is read-only)
    return null;
  }

  /**************************************** layoutDisplay ****************************************/
  protected void layoutDisplay()
  {
    // do nothing if not visible or width/height not set
    if ( !isVisible() || getWidth() == prefWidth( 0 ) || getHeight() == prefHeight( 0 ) )
      return;

    // determine which scroll-bars should be visible
    int tableHeight = getTableHeight();
    int tableWidth = getTableWidth();
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
      sb.setBlockIncrement( canvasHeight - getHeaderHeight() );

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
      sb.setBlockIncrement( canvasWidth - getHeaderWidth() );

      if ( sb.getValue() > max )
        sb.setValue( max );
    }
    else
    {
      sb.setValue( 0.0 );
      sb.setMax( 0.0 );
    }

    // update canvas size (table + blank excess space)
    getCanvas().resize( canvasWidth, canvasHeight );
  }

  /**************************************** getEventView *****************************************/
  public static TableView getEventView( Object source )
  {
    // return the table-view from mouse event canvas-overlay source
    return ( (TableOverlay) source ).getView();
  }

  /****************************************** scrollTo *******************************************/
  public void scrollTo( ObservablePosition position )
  {
    // scroll view if necessary to show specified position
    int column = position.getColumn();
    if ( column >= TableAxis.FIRSTCELL && column < getData().getColumnCount() )
      getHorizontalScrollBar().scrollToShowIndex( column );

    int row = position.getRow();
    if ( row >= TableAxis.FIRSTCELL && row < getData().getRowCount() )
      getVerticalScrollBar().scrollToShowIndex( row );
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return as string
    return getClass().getSimpleName() + "@" + Integer.toHexString( System.identityHashCode( this ) ) + "[ID=" + getId()
        + " m_canvas=" + getCanvas() + "]";
  }

}
