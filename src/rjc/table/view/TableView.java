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

import rjc.table.Utils;
import rjc.table.data.TableData;
import rjc.table.data.TableData.Signal;
import rjc.table.signal.ObservablePosition;
import rjc.table.view.axis.TableAxis;
import rjc.table.view.cell.CellDrawer;
import rjc.table.view.cursor.ITableViewCursor;
import rjc.table.view.cursor.SelectCursor;
import rjc.table.view.editor.CellEditorBase;
import rjc.table.view.events.KeyPressed;
import rjc.table.view.events.KeyTyped;

/*************************************************************************************************/
/************** Base class for scrollable table-view to visualise a table-data model *************/
/*************************************************************************************************/

public class TableView extends TableViewComponents
{
  private TableData m_data;

  /**************************************** constructor ******************************************/
  public TableView( TableData data, String name )
  {
    // construct the table view
    if ( data == null )
      throw new NullPointerException( "TableData must not be null" );
    m_data = data;
    setId( name );

    // assemble view components and add listeners & event handlers
    assembleView();
    addDataListeners();
    addMouseHandlers();
    addEventHandlers();
  }

  /************************************** addDataListeners ***************************************/
  protected void addDataListeners()
  {
    // react to data model signals
    getData().addListener( ( sender, msg ) ->
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
  }

  /************************************** addMouseHandlers ***************************************/
  protected void addMouseHandlers()
  {
    // react to mouse move events on table body top node (is the overlay)
    var overlay = getCanvas().getOverlay();
    overlay.setOnMouseMoved( event ->
    {
      // consume the event and update the mouse cell position & cursor
      event.consume();
      int x = (int) event.getX();
      int y = (int) event.getY();
      getMouseCell().setXY( x, y, true );
    } );

    // invalidate the mouse cell position if mouse exits the view
    overlay.setOnMouseExited( event -> getMouseCell().setInvalid() );

    // react to mouse button pressed events
    overlay.setOnMousePressed( event ->
    {
      if ( getCursor() instanceof ITableViewCursor cursor )
        cursor.handlePressed( event );
    } );

    // react to mouse dragged events (mouse moved whilst mouse button down)
    overlay.setOnMouseDragged( event ->
    {
      if ( getCursor() instanceof ITableViewCursor cursor )
        cursor.handleDragged( event );
    } );

    // react to mouse button released events
    overlay.setOnMouseReleased( event ->
    {
      if ( getCursor() instanceof ITableViewCursor cursor )
        cursor.handleReleased( event );
    } );

    // react to mouse mouse button clicked events
    overlay.setOnMouseClicked( event ->
    {
      if ( getCursor() instanceof ITableViewCursor cursor )
        cursor.handleClicked( event );
    } );

    // react to mouse wheel scroll events
    overlay.setOnScroll( event ->
    {
      // scroll up or down depending on mouse wheel scroll event
      var scrollbar = getVerticalScrollBar();

      if ( scrollbar.isVisible() )
      {
        if ( event.getDeltaY() > 0 )
        {
          scrollbar.finishAnimation();
          scrollbar.decrement();
        }
        else
        {
          scrollbar.finishAnimation();
          scrollbar.increment();
        }
      }
    } );
  }

  /************************************** addEventHandlers ***************************************/
  protected void addEventHandlers()
  {
    // react to losing & gaining focus and visibility
    focusedProperty().addListener( ( observable, oldFocus, newFocus ) -> redraw() );
    visibleProperty().addListener( ( observable, oldVisibility, newVisibility ) ->
    {
      updateLayout();
      redraw();
    } );

    // react to size changes (don't use layoutChildren as that gets called even when scrolling)
    widthProperty().addListener( ( sender, msg ) -> updateLayout() );
    heightProperty().addListener( ( sender, msg ) -> updateLayout() );

    // react to scroll bar position value changes
    getHorizontalScrollBar().valueProperty().addListener( ( observable, oldValue, newValue ) -> tableScrolled() );
    getVerticalScrollBar().valueProperty().addListener( ( observable, oldValue, newValue ) -> tableScrolled() );

    // react to select-cell position and changes to cell selections
    getSelectCell().addLaterListener( ( sender, msg ) ->
    {
      getSelection().update();

      // scroll to show select cell unless selecting using mouse which has its own scrolling behaviour
      if ( getCursor() instanceof SelectCursor == false )
        scrollTo( getSelectCell() );
    } );
    getSelection().addLaterListener( ( sender, msg ) ->
    {
      getCanvas().redrawColumn( TableAxis.HEADER );
      getCanvas().redrawRow( TableAxis.HEADER );
      getCanvas().redrawOverlay();
    } );

    // react to mouse cell-position as might be used for selecting
    getMouseCell().addListener( ( sender, msg ) ->
    {
      if ( getCursor() instanceof ITableViewCursor cursor )
        cursor.checkSelectPosition();
    } );

    // react to zoom values changes
    getZoom().addListener( ( sender, msg ) ->
    {
      updateLayout();
      tableScrolled();
    } );

    // react to keyboard events
    setOnKeyPressed( new KeyPressed() );
    setOnKeyTyped( new KeyTyped() );
  }

  /**************************************** tableScrolled ****************************************/
  private void tableScrolled()
  {
    // handle any actions needed due to view being modified usually scrolled
    redraw();
    getMouseCell().checkXY();
    CellEditorBase.endEditing();
    if ( getCursor() instanceof ITableViewCursor cursor )
      cursor.tableScrolled();
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
    // return new instance of class responsible for drawing the cells on canvas
    return new CellDrawer( this );
  }

  /**************************************** getCellEditor ****************************************/
  public CellEditorBase getCellEditor( CellDrawer cell )
  {
    // return cell editor for specified cell (or null if cell is read-only)
    return null;
  }

  /***************************************** openEditor ******************************************/
  public void openEditor( Object value )
  {
    // open editor for focus cell with specified value
    var cell = getCellDrawer();
    cell.setIndex( this, getFocusCell().getColumn(), getFocusCell().getRow() );
    var editor = getCellEditor( cell );
    if ( editor != null )
      editor.open( value, cell );
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
    return Utils.name( this ) + "[ID=" + getId() + " w=" + getWidth() + " h=" + getHeight() + "]";
  }

}
