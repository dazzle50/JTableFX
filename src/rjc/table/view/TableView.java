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

import rjc.table.Utils;
import rjc.table.data.TableData;
import rjc.table.data.TableData.Signal;
import rjc.table.view.cell.CellDrawer;
import rjc.table.view.events.KeyPressed;
import rjc.table.view.events.KeyTyped;
import rjc.table.view.events.MouseMoved;

/*************************************************************************************************/
/************** Base class for scrollable table-view to visualise a table-data model *************/
/*************************************************************************************************/

public class TableView extends TableViewElements
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
    assembleElements();
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
    getHorizontalScrollBar().valueProperty().addListener( ( observable, oldValue, newValue ) -> tableScrolled() );
    getVerticalScrollBar().valueProperty().addListener( ( observable, oldValue, newValue ) -> tableScrolled() );

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
    // TODO overlay.setOnMouseClicked( new MouseClicked() );
    // TODO overlay.setOnMousePressed( new MousePressed() );
    // TODO overlay.setOnMouseReleased( new MouseReleased() );
    // TODO overlay.setOnMouseExited( new MouseExited() );
    // TODO overlay.setOnMouseEntered( new MouseEntered() );
    // TODO overlay.setOnMouseDragged( new MouseDragged() );
    // TODO overlay.setOnScroll( new MouseScroll() );
    // TODO overlay.setOnContextMenuRequested( new ContextMenu() );
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
    getCanvas().getColumnsAxis().reset();
    getCanvas().getRowsAxis().reset();
    getCanvas().getRowsAxis().setDefaultSize( 20 );
    getCanvas().getRowsAxis().setHeaderSize( 20 );
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

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return as string
    return getClass().getSimpleName() + "@" + Integer.toHexString( System.identityHashCode( this ) ) + "[ID=" + getId()
        + " m_canvas=" + getCanvas() + "]";
  }

}
