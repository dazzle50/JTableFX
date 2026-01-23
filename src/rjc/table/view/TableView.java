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

import javafx.scene.input.ContextMenuEvent;
import rjc.table.Utils;
import rjc.table.data.TableData;
import rjc.table.data.TableData.Signal;
import rjc.table.signal.ObservablePosition;
import rjc.table.view.axis.TableAxis;
import rjc.table.view.cell.CellDrawer;
import rjc.table.view.cursor.ITableViewCursor;
import rjc.table.view.cursor.SelectCursor;
import rjc.table.view.editor.AbstractCellEditor;
import rjc.table.view.events.KeyPressed;
import rjc.table.view.events.KeyTyped;

/*************************************************************************************************/
/************** Base class for scrollable table-view to visualise a table-data model *************/
/*************************************************************************************************/

/**
 * A scrollable table view component for visualising and interacting with tabular data models.
 * 
 * <p>TableView extends {@link TableViewComponents} to provide a complete table visualisation
 * system with support for scrolling, selection, editing, zooming, and user interaction through
 * mouse and keyboard events. The view is backed by a {@link TableData} model which provides
 * the table's size and content.</p>
 * 
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Automatic synchronisation with underlying data model changes</li>
 *   <li>Efficient partial redrawing for modified cells, rows, or columns</li>
 *   <li>Customisable cell rendering via {@link CellDrawer}</li>
 *   <li>Optional cell editing support via {@link AbstractCellEditor}</li>
 *   <li>Mouse-based selection and interaction</li>
 *   <li>Keyboard navigation and shortcuts</li>
 *   <li>Horizontal and vertical scrolling with mouse wheel support</li>
 *   <li>Zoom functionality</li>
 *   <li>Context menu support</li>
 * </ul>
 * 
 * <p><b>Architecture:</b></p>
 * <p>The table view consists of several coordinated components:</p>
 * <ul>
 *   <li><b>Canvas</b> - renders the visible cells, headers and overlay</li>
 *   <li><b>Axes</b> - manage column and row sizing, positioning and visibility</li>
 *   <li><b>Scrollbars</b> - control which portion of the table is visible</li>
 *   <li><b>Selection</b> - tracks selected cells and ranges</li>
 *   <li><b>Cursor</b> - handles mouse interaction modes</li>
 *   <li><b>Positions</b> - track focus, select, and mouse cell positions</li>
 * </ul>
 * 
 * <p><b>Data Synchronisation:</b></p>
 * <p>The view automatically listens to the data model and updates efficiently:</p>
 * <ul>
 *   <li>Individual cell changes trigger single cell redraws</li>
 *   <li>Row/column changes trigger row/column redraws</li>
 *   <li>Table-wide changes trigger full redraws</li>
 *   <li>Row/column count changes trigger layout recalculation</li>
 * </ul>
 * 
 * <p><b>Customisation:</b></p>
 * <p>Subclasses can customise behaviour by overriding:</p>
 * <ul>
 *   <li>{@link #getCellDrawer()} - to provide custom cell rendering</li>
 *   <li>{@link #getCellEditor(CellDrawer)} - to enable cell editing</li>
 *   <li>{@link #openContextMenu(ContextMenuEvent)} - to customise the context menu</li>
 *   <li>{@link #addDataListeners()}, {@link #addMouseHandlers()}, {@link #addEventHandlers()}
 *       - to add additional event handling</li>
 * </ul>
 * 
 * <p><b>Thread Safety:</b></p>
 * <p>This class is not thread-safe and must be used only on the JavaFX Application Thread.
 * Data model signals should be fired on the JavaFX thread to avoid threading violations.</p>
 * 
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * TableData data = new MyTableData();
 * TableView view = new TableView(data, "myTable");
 * 
 * // Add to scene
 * Scene scene = new Scene(view, 800, 600);
 * stage.setScene(scene);
 * }</pre>
 * 
 * @see TableViewComponents
 * @see TableData
 * @see CellDrawer
 * @see AbstractCellEditor
 */
public class TableView extends TableViewComponents
{
  private TableData m_data; // data-model for table size and contents

  /**************************************** constructor ******************************************/
  /**
   * Constructs a new table view with the specified data model and identifier.
   * 
   * @param data the table data model containing size and content information
   * @param name the identifier name for this table view
   * @throws NullPointerException if data is null
   */
  public TableView( TableData data, String name )
  {
    // validate data parameter is not null
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
  /**
   * Adds listeners to the data model to respond to changes in table content.
   * Converts data model indices to view indices and triggers appropriate redraws.
   */
  protected void addDataListeners()
  {
    // react to data model signals, convert data indexes into view indexes
    getData().addListener( ( sender, msg ) ->
    {
      Signal change = (Signal) msg[0];
      // handle different types of data change signals
      if ( change == Signal.TABLE_VALUES_CHANGED )
        redraw();
      else if ( change == Signal.COLUMN_VALUES_CHANGED )
      {
        // redraw only the affected column
        int viewColumn = getColumnsAxis().getViewIndex( (int) msg[1] );
        getCanvas().redrawColumn( viewColumn );
      }
      else if ( change == Signal.ROW_VALUES_CHANGED )
      {
        // redraw only the affected row
        int viewRow = getRowsAxis().getViewIndex( (int) msg[1] );
        getCanvas().redrawRow( viewRow );
      }
      else if ( change == Signal.CELL_VALUE_CHANGED )
      {
        // redraw only the affected cell
        int viewColumn = getColumnsAxis().getViewIndex( (int) msg[1] );
        int viewRow = getRowsAxis().getViewIndex( (int) msg[2] );
        getCanvas().redrawCell( viewColumn, viewRow );
      }
    } );

    // react to column & row count changes
    getData().columnCountProperty().addListener( ( sender, msg ) -> tableModified() );
    getData().rowCountProperty().addListener( ( sender, msg ) -> tableModified() );
  }

  /************************************** addMouseHandlers ***************************************/
  /**
   * Adds mouse event handlers to the table view overlay for user interaction.
   * Handles mouse movement, button presses, drags, releases, clicks, context menus and scrolling.
   */
  protected void addMouseHandlers()
  {
    // react to mouse move events on table body top node (is the overlay)
    var overlay = getCanvas().getOverlay();
    overlay.setOnMouseMoved( event ->
    {
      // update the mouse cell position based on pixel coordinates
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
      // delegate to cursor if it implements table view cursor interface
      if ( getCursor() instanceof ITableViewCursor cursor )
        cursor.handlePressed( event );
    } );

    // react to mouse dragged events (mouse moved whilst mouse button down)
    overlay.setOnMouseDragged( event ->
    {
      // delegate to cursor for drag handling (typically for selections)
      if ( getCursor() instanceof ITableViewCursor cursor )
        cursor.handleDragged( event );
    } );

    // react to mouse button released events
    overlay.setOnMouseReleased( event ->
    {
      // delegate to cursor to complete any drag or press operations
      if ( getCursor() instanceof ITableViewCursor cursor )
        cursor.handleReleased( event );
    } );

    // react to mouse mouse button clicked events
    overlay.setOnMouseClicked( event ->
    {
      // delegate to cursor for click handling (e.g. cell selection)
      if ( getCursor() instanceof ITableViewCursor cursor )
        cursor.handleClicked( event );
    } );

    // react to context-menu request events
    overlay.setOnContextMenuRequested( event -> openContextMenu( event ) );

    // react to mouse wheel scroll events
    setOnScroll( event ->
    {
      // scroll up or down depending on mouse wheel scroll direction
      var scrollbar = getVerticalScrollBar();
      event.consume();
      if ( scrollbar.isVisible() )
      {
        // scroll up when wheel scrolls up (positive delta)
        if ( event.getDeltaY() > 0 )
        {
          scrollbar.finishAnimation();
          scrollbar.decrement();
        }
        else
        {
          // scroll down when wheel scrolls down (negative delta)
          scrollbar.finishAnimation();
          scrollbar.increment();
        }
      }
    } );
  }

  /************************************** addEventHandlers ***************************************/
  /**
   * Adds various event handlers for focus, visibility, size changes, scrolling, selection,
   * zoom and keyboard input. These handlers coordinate the view's response to user actions
   * and system events.
   */
  protected void addEventHandlers()
  {
    // react to losing & gaining focus and visibility
    focusedProperty().addListener( ( property, oldFocus, newFocus ) -> redraw() );
    visibleProperty().addListener( ( property, oldVisibility, newVisibility ) ->
    {
      // clear status and update layout when visibility changes
      getStatus().clear();
      updateLayout();
      redraw();
    } );

    // react to view size changes (don't use layoutChildren as that gets called even when scrolling)
    widthProperty().addListener( ( sender, msg ) -> updateLayout() );
    heightProperty().addListener( ( sender, msg ) -> updateLayout() );

    // react to axis size changes including hiding & unhiding indexes
    getColumnsAxis().getTotalPixelsProperty().addListener( ( sender, msg ) -> updateLayout() );
    getRowsAxis().getTotalPixelsProperty().addListener( ( sender, msg ) -> updateLayout() );

    // react to scroll bar position value changes
    getHorizontalScrollBar().valueProperty().addListener( ( property, oldValue, newValue ) -> tableModified() );
    getVerticalScrollBar().valueProperty().addListener( ( property, oldValue, newValue ) -> tableModified() );

    // react to select-cell position and changes to cell selections
    getSelectCell().addLaterListener( ( sender, msg ) ->
    {
      // update selection based on new select cell position
      getSelection().update();

      // scroll to show select cell unless selecting using mouse which has its own scrolling behaviour
      if ( getCursor() instanceof SelectCursor == false )
        scrollTo( getSelectCell() );
    } );
    getSelection().addLaterListener( ( sender, msg ) ->
    {
      // redraw headers and overlay to reflect new selection state
      getCanvas().redrawColumn( TableAxis.HEADER );
      getCanvas().redrawRow( TableAxis.HEADER );
      getCanvas().redrawOverlay();
    } );

    // react to mouse cell-position as might be used for selecting
    getMouseCell().addListener( ( sender, msg ) ->
    {
      // notify cursor to check if selection position should be updated
      if ( getCursor() instanceof ITableViewCursor cursor )
        cursor.checkSelectPosition();
    } );

    // react to zoom values changes
    getZoom().addListener( ( sender, msg ) -> tableModified() );

    // react to keyboard events
    setOnKeyPressed( new KeyPressed() );
    setOnKeyTyped( new KeyTyped() );
  }

  /**************************************** tableModified ****************************************/
  /**
   * Handles actions needed when the table view has been modified, typically due to scrolling
   * or zooming. Triggers redraw, validates mouse position, ends any active editing, and
   * notifies the cursor.
   */
  private void tableModified()
  {
    // redraw entire view to reflect modifications
    redraw();

    // revalidate mouse cell position in case it's now invalid
    getMouseCell().checkXY();

    // end any active cell editing as view state has changed
    AbstractCellEditor.endEditing();

    // notify cursor that table has been modified
    if ( getCursor() instanceof ITableViewCursor cursor )
      cursor.tableModified();
  }

  /******************************************* redraw ********************************************/
  /**
   * Requests a complete redraw of the visible table including headers, body cells and overlay.
   */
  public void redraw()
  {
    // request redraw of full visible table (headers and body) including overlay
    getCanvas().redraw();
  }

  /****************************************** getData ********************************************/
  /**
   * Returns the table data model that provides size and content information for this view.
   * 
   * @return the table data model
   */
  public TableData getData()
  {
    // return data model for table-view
    return m_data;
  }

  /**************************************** getCellDrawer ****************************************/
  /**
   * Creates and returns a new cell drawer instance responsible for rendering cells on the canvas.
   * Subclasses can override this method to provide custom cell drawing behaviour.
   * 
   * @return a new cell drawer instance
   */
  public CellDrawer getCellDrawer()
  {
    // return a new cell drawer instance responsible for drawing the cells on canvas
    return new CellDrawer();
  }

  /**************************************** getCellEditor ****************************************/
  /**
   * Returns an appropriate cell editor for the specified cell, or null if the cell is read-only.
   * Subclasses should override this method to provide cell editing capabilities.
   * 
   * @param cell the cell drawer containing cell information and rendering state
   * @return a cell editor for the specified cell, or null if editing is not supported
   */
  public AbstractCellEditor getCellEditor( CellDrawer cell )
  {
    // return cell editor for specified cell (or null if cell is read-only)
    return null;
  }

  /***************************************** openEditor ******************************************/
  /**
   * Opens a cell editor for the current focus cell with the specified initial value.
   * If no editor is available or the value is invalid, the editor will not open.
   * 
   * @param value the initial value to display in the editor
   */
  public void openEditor( Object value )
  {
    // create cell drawer for focus cell position
    var cell = getCellDrawer();
    cell.setIndex( this, getFocusCell().getColumn(), getFocusCell().getRow() );
    cell.getValueVisual();

    // obtain appropriate editor and open it if value is valid
    var editor = getCellEditor( cell );
    if ( editor != null && editor.isValueValid( value ) )
      editor.open( value, cell );
  }

  /****************************************** scrollTo *******************************************/
  /**
   * Scrolls the view if necessary to ensure the specified cell position is visible.
   * Only scrolls if the position is within valid table bounds.
   * 
   * @param position the cell position to scroll to and make visible
   */
  public void scrollTo( ObservablePosition position )
  {
    // scroll horizontally if column is valid and not already visible
    int column = position.getColumn();
    if ( column >= TableAxis.FIRSTCELL && column < getData().getColumnCount() )
      getHorizontalScrollBar().scrollToShowIndex( column );

    // scroll vertically if row is valid and not already visible
    int row = position.getRow();
    if ( row >= TableAxis.FIRSTCELL && row < getData().getRowCount() )
      getVerticalScrollBar().scrollToShowIndex( row );
  }

  /*************************************** openContextMenu ***************************************/
  /**
   * Opens the context menu for this table view at the specified screen coordinates.
   * Typically called in response to a right-click or context menu key press.
   * 
   * @param event the context menu event containing screen coordinates
   */
  public void openContextMenu( ContextMenuEvent event )
  {
    // show context menu for this table-view at mouse click position
    TableContextMenu.show( this, event.getScreenX(), event.getScreenY() );
  }

  /****************************************** toString *******************************************/
  /**
   * Returns a string representation of this table view including its ID and dimensions.
   * 
   * @return a string representation of this table view
   */
  @Override
  public String toString()
  {
    // return as string with id and dimensions
    return Utils.name( this ) + "[ID=" + getId() + " w=" + getWidth() + " h=" + getHeight() + "]";
  }

}