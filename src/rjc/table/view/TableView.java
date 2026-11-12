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

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import rjc.table.Utils;
import rjc.table.data.TableData;
import rjc.table.data.TableData.DataChange;
import rjc.table.signal.ObservablePosition;
import rjc.table.signal.ObservableStatus.Level;
import rjc.table.view.axis.TableAxis;
import rjc.table.view.cell.CellDrawer;
import rjc.table.view.cursor.Cursors;
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
 *   <li>Horizontal and vertical scrollbars, with vertical mouse-wheel scrolling</li>
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
 * <p>Each view owns a context-menu handler for target resolution, popup selection, placement and
 * popup lifetime. The view retains validation, editor completion and subclass hooks. Mouse requests
 * preserve selected groups or synchronously select a new target; keyboard requests classify the
 * complete visible selection without changing cell focus, anchor or pointer position.</p>
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
 *   <li>{@link #prepareContextMenu(ContextMenuEvent)}, {@link #contextMenuSelectionChanged(int, int)},
 *       {@link #buildContextMenu(int, int, boolean)} - to customise context menus while retaining
 *       the shared targeting, selection and popup lifecycle. A full override of
 *       {@link #openContextMenu(ContextMenuEvent)} takes responsibility for that lifecycle.</li>
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
  private TableData                     m_data;               // data-model for table size and contents
  TableContextMenuBuilder               m_contextMenuBuilder; // per-view builder retained through reset
  private final TableContextMenuHandler m_contextMenuHandler; // menu targeting and popup lifetime
  private ITableViewCursor              m_primaryCursor;      // active primary gesture cursor
  private boolean                       m_ignoreContextMenu;  // suppress popup during primary gesture
  private long                          m_selectRevision;     // latest select-cell or menu revision
  private long                          m_selectionRevision;  // latest explicit selection revision

  /**************************************** constructor ******************************************/
  /**
   * Constructs a view bound to the supplied data model and assigns its node identifier. Assembles
   * components, then installs data, mouse and other event handling through the corresponding
   * overridable setup methods. Those overrides run before subclass construction has completed.
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
    m_contextMenuHandler = new TableContextMenuHandler( this );
    setId( name );

    // assemble view components and add listeners & event handlers
    assembleView();
    addDataListeners();
    addMouseHandlers();
    addEventHandlers();
  }

  /******************************************* resize ********************************************/
  /**
   * Called by the layout system when this node should be resized to the specified dimensions.
   * <p>
   * Updates the standard Region size properties before refreshing the integer drawing extents.
   * Recalculates component layout only if the integer allocated width or height changes; fractional
   * dimensions are still retained by the underlying Region.
   *
   * @param width   the target layout width
   * @param height  the target layout height
   */
  @Override
  public void resize( double width, double height )
  {
    // update both allocated dimensions before laying out integer drawing extents
    int oldWidth = getPixelWidth();
    int oldHeight = getPixelHeight();
    super.resize( width, height );
    if ( oldWidth != getPixelWidth() || oldHeight != getPixelHeight() )
      updateLayout();
  }

  /************************************** addDataListeners ***************************************/
  /**
   * Adds content-change listeners that map data indexes to view indexes for redraw, plus count-change
   * listeners that invoke {@link #tableModified()}. Called during construction; overrides should
   * retain the superclass setup when extending the standard model synchronisation.
   */
  protected void addDataListeners()
  {
    // react to data-model signals; convert data indices to view indices
    getData().addListener( ( _, msg ) ->
    {
      if ( msg[0] instanceof DataChange change )
        switch ( change )
        {
          case WHOLE_TABLE -> redraw();
          case COLUMN_VALUES -> {
            int viewColumn = getColumnsAxis().getViewIndex( (int) msg[1] );
            getCanvas().redrawColumn( viewColumn );
          }
          case ROW_VALUES -> {
            int viewRow = getRowsAxis().getViewIndex( (int) msg[1] );
            getCanvas().redrawRow( viewRow );
          }
          case CELL_VALUE -> {
            int viewColumn = getColumnsAxis().getViewIndex( (int) msg[1] );
            int viewRow = getRowsAxis().getViewIndex( (int) msg[2] );
            getCanvas().redrawCell( viewColumn, viewRow );
          }
        }
    } );

    // react to structural changes (column/row count)
    getData().columnCountProperty().addListener( ( _, _ ) -> tableModified() );
    getData().rowCountProperty().addListener( ( _, _ ) -> tableModified() );
  }

  /************************************** addMouseHandlers ***************************************/
  /**
   * Installs movement/exit handlers on the canvas overlay and button, context-menu and vertical
   * wheel handlers on the view. Primary press selects the gesture cursor used for subsequent drag
   * and release dispatch. Secondary events do not enter primary selection/resize/reorder handlers;
   * menu opening occurs through the separate context-menu request event.
   *
   * <p>Called during construction. Overrides extending these interactions should preserve the
   * superclass handlers and delegate retained events once.</p>
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
    setOnMousePressed( event ->
    {
      // secondary events belong to the native popup lifecycle and must not change primary gesture state
      if ( event.getButton() == MouseButton.SECONDARY )
      {
        m_ignoreContextMenu = m_primaryCursor != null || event.isPrimaryButtonDown();
        return;
      }
      if ( event.getButton() != MouseButton.PRIMARY )
        return;

      // delegate to cursor if it implements table view cursor interface
      if ( getCursor() instanceof ITableViewCursor cursor )
      {
        cursor.handlePressed( event );
        // a hover press may have switched to a selection cursor
        m_primaryCursor = getCursor() instanceof ITableViewCursor active ? active : cursor;
      }
      else
      {
        // ensure view has focus
        requestFocus();
        event.consume();
      }
    } );

    // react to mouse dragged events (mouse moved whilst mouse button down)
    setOnMouseDragged( event ->
    {
      // delegate to cursor for drag handling (typically for selections)
      if ( event.isPrimaryButtonDown() && m_primaryCursor != null )
        m_primaryCursor.handleDragged( event );
    } );

    // react to mouse button released events
    setOnMouseReleased( event ->
    {
      // delegate to cursor to complete any drag or press operations
      if ( event.getButton() == MouseButton.PRIMARY && m_primaryCursor != null )
      {
        var cursor = m_primaryCursor;
        m_primaryCursor = null;
        cursor.handleReleased( event );
      }
    } );

    // react to mouse mouse button clicked events
    setOnMouseClicked( event ->
    {
      // delegate to cursor for click handling (e.g. cell selection)
      if ( event.getButton() == MouseButton.PRIMARY && getCursor() instanceof ITableViewCursor cursor )
        cursor.handleClicked( event );
    } );

    // react to context-menu request events
    setOnContextMenuRequested( event -> openContextMenu( event ) );

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
   *
   * <p>Select-cell changes queue area updates with position and selection revisions, so synchronous
   * menu preparation or later commands can invalidate obsolete work. Selection redraw remains
   * deferred. Mouse-position changes update the endpoint only for the active primary cursor.</p>
   *
   * <p>Called during construction; overrides extending the standard behaviour should retain the
   * superclass listener and keyboard-handler setup.</p>
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

    // CSS and child layout determine scrollbar thickness, including after the first table resize
    getVerticalScrollBar().widthProperty().addListener( ( property, oldWidth, newWidth ) -> updateLayout() );
    getHorizontalScrollBar().heightProperty().addListener( ( property, oldHeight, newHeight ) -> updateLayout() );

    // defer axis-driven layout until both axes have invalidated their zoom caches
    getColumnsAxis().getTotalPixelsProperty().addLaterListener( ( sender, msg ) -> updateLayout() );
    getRowsAxis().getTotalPixelsProperty().addLaterListener( ( sender, msg ) -> updateLayout() );

    // react to scroll bar position value changes
    getHorizontalScrollBar().valueProperty().addListener( ( property, oldValue, newValue ) -> tableModified() );
    getVerticalScrollBar().valueProperty().addListener( ( property, oldValue, newValue ) -> tableModified() );

    // react to select-cell position and changes to cell selections
    getSelection().addListener( ( sender, msg ) -> m_selectionRevision++ );
    getSelectCell().addListener( ( sender, msg ) ->
    {
      long selectRevision = ++m_selectRevision;
      long selectionRevision = m_selectionRevision;
      Platform.runLater( () ->
      {
        // a synchronous menu selection or later command must not be overwritten by an old notification
        if ( selectRevision != m_selectRevision || selectionRevision != m_selectionRevision )
          return;
        getSelection().update();

        // mouse selection has its own scrolling behaviour
        if ( getCursor() instanceof SelectCursor == false )
          scrollTo( getSelectCell() );
      } );
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
      if ( m_primaryCursor != null )
        m_primaryCursor.checkSelectPosition();
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
   * or zooming. Requests redraw, schedules mouse-position revalidation, ends the globally active
   * editor through {@link AbstractCellEditor#endEditing()}, and notifies the current table cursor.
   * Editor completion therefore uses the normal commit-or-discard policy, including for errors.
   */
  private void tableModified()
  {
    // redraw entire view to reflect modifications
    redraw();

    // revalidate mouse cell position in case it's now invalid
    getMouseCell().revalidatePosition();

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
   * The drawer also provides cell geometry, value and styling when opening an editor. The default
   * implementation creates a fresh drawer; subclasses may supply customised drawing behaviour.
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
   * Prepares a drawer for the current focus cell, obtains its editor and opens it if
   * {@link AbstractCellEditor#isValueValid(Object)} accepts the supplied starting value. Focus must
   * already identify a valid body cell. A null editor or rejected opening value leaves editing
   * unchanged; this preflight is not a model-acceptance or commit check.
   *
   * <p>Does not finish another active edit first: {@link AbstractCellEditor#open(Object, CellDrawer)}
   * declines to open while an editor is active. Editor-specific conversion failures propagate.</p>
   * 
   * @param value the existing cell value or a typed starter value for replacement editing
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
   * Scrolls each axis independently to reveal its coordinate when that coordinate is a valid body
   * index. Header, invalid and beyond-count coordinates are ignored for that axis. Resulting scroll
   * changes use the normal {@link #tableModified()} path and may finish an active editor.
   * 
   * @param position the non-null view-coordinate position to reveal
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
   * Validates a context-menu request, prepares its selection, and replaces the table's popup.
   * Mouse requests use the specified screen coordinates; keyboard requests use the selection
   * region and focus without moving the pointer or scrolling. Override the narrower preparation,
   * selection-notification and construction hooks to retain this shared lifecycle.
   *
   * <p>Editor-owned requests and mouse requests outside the canvas are left unhandled. Requests
   * during an active primary gesture are consumed without opening a replacement popup. Other table
   * requests are consumed, validated and dismiss the old popup, including when rejected or when
   * construction returns no items. Validation precedes default editor completion and focus changes.</p>
   *
   * <p>Targets are resolved after editing completes. New mouse selections are established before
   * construction; keyboard requests preserve selection and use a selected header member or usable
   * body focus for targeting. Keyboard placement is clamped to the canvas without scrolling.</p>
   * 
   * @param event the context menu event containing screen coordinates
   */
  public void openContextMenu( ContextMenuEvent event )
  {
    boolean keyboard = event.isKeyboardTrigger();
    if ( event.getTarget() instanceof Node node && AbstractCellEditor.isEditorNode( node ) )
      return;
    if ( keyboard && getScene() != null && AbstractCellEditor.isEditorNode( getScene().getFocusOwner() ) )
      return;

    // preserve an actual primary gesture, including when a second button is pressed
    if ( m_primaryCursor != null || ( !keyboard && m_ignoreContextMenu ) )
    {
      event.consume();
      return;
    }

    // ignore scrollbars and outside requests before validation can commit an editor
    if ( !keyboard && !m_contextMenuHandler.isOnCanvas( screenToLocal( event.getScreenX(), event.getScreenY() ) ) )
      return;
    event.consume();
    boolean permitted = prepareContextMenu( event );
    m_contextMenuHandler.hideContextMenu();
    if ( !permitted )
      return;

    AbstractCellEditor.endEditing();
    requestFocus();
    m_selectRevision++;
    m_contextMenuHandler.open( event );
  }

  /************************************* prepareContextMenu **************************************/
  /**
   * Validates an accepted table-menu request before default editor completion, focus acquisition
   * or selection preparation. The default implementation permits it. Overrides may finish a valid
   * edit or reject invalid input that must remain open; targeting is resolved afterwards so changes
   * to layout or index ordering are respected.
   *
   * @param event the table request, already consumed by {@link #openContextMenu(ContextMenuEvent)}
   * @return true to continue; false to dismiss the previous table popup and stop processing
   */
  protected boolean prepareContextMenu( ContextMenuEvent event )
  {
    // specialised views may reject a request while keeping invalid editor input intact
    return true;
  }

  /******************************** contextMenuSelectionChanged **********************************/
  /**
   * Notifies subclasses after a mouse menu request synchronously replaces selection and before
   * menu construction. Not called for preserved selections or keyboard requests. The default does
   * nothing; overrides can record selection origin without replacing the common selection policy.
   *
   * @param viewRow the trigger row view index, or {@link TableAxis#HEADER} for a column header
   * @param viewColumn the trigger column view index, or {@link TableAxis#HEADER} for a row header
   */
  protected void contextMenuSelectionChanged( int viewRow, int viewColumn )
  {
    // subclasses may record the origin of a newly created selection
  }

  /************************************** buildContextMenu ***************************************/
  /**
   * Constructs contents for a resolved request after validation, editor completion and selection
   * preparation. The default creates a {@link TableContextMenu} using explicit coordinates and
   * the configured builder. Coordinates identify the menu region and do not themselves grant a
   * selection-only command a fallback target.
   *
   * <p>Overrides supply contents; the shared lifecycle owns display, dismissal and event consumption.
   * Return a fresh menu for the request, or null/an empty menu to suppress display.</p>
   *
   * @param viewRow the body row index or header/trailing-space sentinel
   * @param viewColumn the body column index or header/trailing-space sentinel
   * @param keyboardTrigger true for a keyboard request; false for a mouse request
   * @return the constructed menu, or null to suppress display
   */
  protected ContextMenu buildContextMenu( int viewRow, int viewColumn, boolean keyboardTrigger )
  {
    return new TableContextMenu( this, viewRow, viewColumn );
  }

  /**************************************** setBusyState *****************************************/
  /**
   * Sets the busy state of the scene containing this table-view, updating the mouse cursor and 
   * observable status accordingly.
   *
   * <p>When entering busy state, this node's own cursor is cleared ({@code null}) so the
   * scene-level wait cursor takes visual precedence. On exit, the scene cursor is set to its
   * default and {@link #getMouseCell()} schedules a hover-cursor refresh using stored coordinates.
   * Requires an attached scene and a status object assigned to the view.
   *
   * @param busy    {@code true} to enter busy state; {@code false} to leave it
   * @param message the status message to display, or null for no message
   */
  public void setBusyState( boolean busy, String message )
  {
    // set busy state of the scene containing this table-view
    if ( busy )
    {
      setCursor( null );
      getScene().setCursor( Cursors.WAIT );
      getStatus().update( Level.BUSY, message );
    }
    else
    {
      getScene().setCursor( Cursors.DEFAULT );
      getMouseCell().revalidateCursor();
      getStatus().update( Level.INFO, message );
    }
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
