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

import javafx.geometry.Orientation;
import rjc.table.control.IObservableStatus;
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

/**
 * Internal coordinator that assembles and manages the core components of a table view.
 * <p>
 * This class owns and connects:
 * <ul>
 *   <li>{@link TableAxis} for rows and columns</li>
 *   <li>{@link TableCanvas} for rendering</li>
 *   <li>Horizontal and vertical {@link TableScrollBar}</li>
 *   <li>{@link CellSelection} model</li>
 *   <li>Observable positions (mouse, focus, anchor)</li>
 *   <li>Zoom, undo stack, and status</li>
 * </ul>
 * <p>
 * It handles layout, coordinate translation, scrollbar visibility, and basic dimension queries.
 * Application code should normally interact only with the enclosing {@link TableView} class.
 *
 * @see TableView
 * @see TableAxis
 * @see TableCanvas
 * @see CellSelection
 */
public class TableViewComponents extends TableViewParent implements IObservableStatus
{
  private TableCanvas      m_canvas;              // renders table headers and body cells
  private TableScrollBar   m_verticalScrollBar;   // vertical scrolling control
  private TableScrollBar   m_horizontalScrollBar; // horizontal scrolling control

  private TableAxis        m_columnsAxis;         // manages column widths and x-coordinates
  private TableAxis        m_rowsAxis;            // manages row heights and y-coordinates

  private UndoStack        m_undostack;           // undo/redo command stack
  private ObservableStatus m_status;              // status message observable
  private ObservableDouble m_zoom;                // zoom factor (1.0 = 100%)

  private CellSelection    m_selection;           // selected cells model
  private ViewPosition     m_focusCell;           // keyboard focus position
  private ViewPosition     m_selectCell;          // selection anchor position
  private MousePosition    m_mouseCell;           // current mouse position

  /************************************** assembleView ***************************************/
  /**
   * Assembles all table-view components and initialises their relationships.
   * Creates axes, canvas, scrollbars, and observable positions, then resets to defaults.
   */
  void assembleView()
  {
    // get reference to the table view
    TableView view = (TableView) this;

    // create axes bound to data model row and column counts
    m_columnsAxis = new TableAxis( view.getData().columnCountProperty() );
    m_rowsAxis = new TableAxis( view.getData().rowCountProperty() );

    // create main canvas and scrollbars
    m_canvas = new TableCanvas( view );
    m_horizontalScrollBar = new TableScrollBar( m_columnsAxis, Orientation.HORIZONTAL );
    m_verticalScrollBar = new TableScrollBar( m_rowsAxis, Orientation.VERTICAL );
    getChildren().addAll( m_canvas, m_canvas.getOverlay(), m_horizontalScrollBar, m_verticalScrollBar );

    // create observable zoom parameter and bind to axes
    m_zoom = new ObservableDouble( 1.0 );
    m_columnsAxis.setZoomProperty( m_zoom.getReadOnly() );
    m_rowsAxis.setZoomProperty( m_zoom.getReadOnly() );

    // create observable cell positions and selection model
    m_mouseCell = new MousePosition( view );
    m_focusCell = new ViewPosition( view );
    m_selectCell = new ViewPosition( view );
    m_selection = new CellSelection( view );

    // initialise view with default settings
    reset();
  }

  /******************************************** reset ********************************************/
  /**
   * Resets the table view to default settings.
   * Clears axes configuration and sets default cell sizes.
   */
  public void reset()
  {
    // reset axes to initial state
    getColumnsAxis().reset();
    getRowsAxis().reset();

    // set default row and header sizes
    getRowsAxis().setDefaultNominalSize( 20 );
    getRowsAxis().setHeaderNominalSize( 20 );
  }

  /**************************************** updateLayout *****************************************/
  /**
   * Updates the layout of canvas and scrollbars based on current view size.
   * Calculates scrollbar visibility and positions all components appropriately.
   */
  public void updateLayout()
  {
    // skip layout if view not visible or dimensions not set
    if ( !isVisible() || getWidth() == prefWidth( 0 ) || getHeight() == prefHeight( 0 ) )
      return;

    // get table dimensions and scrollbar size
    int tableHeight = getTableHeight();
    int tableWidth = getTableWidth();
    int scrollbarSize = (int) getVerticalScrollBar().getWidth();

    // determine which scrollbars are needed (may require two passes)
    boolean isVSBvisible = getHeight() < tableHeight;
    int canvasWidth = isVSBvisible ? getWidth() - scrollbarSize : getWidth();
    boolean isHSBvisible = canvasWidth < tableWidth;
    int canvasHeight = isHSBvisible ? getHeight() - scrollbarSize : getHeight();
    isVSBvisible = canvasHeight < tableHeight;
    canvasWidth = isVSBvisible ? getWidth() - scrollbarSize : getWidth();

    // configure vertical scrollbar
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

    // configure horizontal scrollbar
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

    // update canvas to fill available space
    getCanvas().resize( canvasWidth, canvasHeight );
  }

  /**************************************** setUndostack *****************************************/
  /**
   * Sets the undo stack for this table view.
   * Creates a new stack if null is provided.
   *
   * @param undostack the undo stack to use, or null to create a new one
   */
  public void setUndostack( UndoStack undostack )
  {
    // use provided stack or create new empty one
    m_undostack = undostack == null ? new UndoStack() : undostack;
  }

  /***************************************** getUndoStack ****************************************/
  /**
   * Returns the undo stack for this table view.
   * Creates a new stack if one doesn't exist.
   *
   * @return the undo stack
   */
  public UndoStack getUndoStack()
  {
    // create undo stack if not yet initialised
    if ( m_undostack == null )
      m_undostack = new UndoStack();
    return m_undostack;
  }

  /***************************************** setStatus *******************************************/
  /**
   * Sets the observable status for this table view.
   * Creates a new status object if null is provided.
   *
   * @param status the observable status to use, or null to create a new one
   */
  @Override
  public void setStatus( ObservableStatus status )
  {
    // use provided status or create new empty one
    m_status = status == null ? new ObservableStatus() : status;
  }

  /***************************************** getStatus *******************************************/
  /**
   * Returns the observable status for this table view.
   *
   * @return the observable status object
   */
  @Override
  public ObservableStatus getStatus()
  {
    // return status message observable
    return m_status;
  }

  /****************************************** getZoom ********************************************/
  /**
   * Returns the observable zoom factor for this table view.
   * A value of 1.0 represents 100% normal size.
   *
   * @return the observable zoom factor
   */
  public ObservableDouble getZoom()
  {
    // return zoom scale observable (1.0 = 100%)
    return m_zoom;
  }

  /**************************************** getMouseCell *****************************************/
  /**
   * Returns the observable mouse cell position on this table view.
   *
   * @return the observable mouse position
   */
  public MousePosition getMouseCell()
  {
    // return current mouse cell position
    return m_mouseCell;
  }

  /**************************************** getFocusCell *****************************************/
  /**
   * Returns the observable focus cell position on this table view.
   * This is the cell that has keyboard focus.
   *
   * @return the observable focus position
   */
  public ViewPosition getFocusCell()
  {
    // return keyboard focus cell position
    return m_focusCell;
  }

  /**************************************** getSelectCell ****************************************/
  /**
   * Returns the observable select cell position on this table view.
   * This is the anchor point for range selections.
   *
   * @return the observable select position
   */
  public ViewPosition getSelectCell()
  {
    // return selection anchor cell position
    return m_selectCell;
  }

  /**************************************** getSelection *****************************************/
  /**
   * Returns the cell selection model for this table view.
   *
   * @return the cell selection model
   */
  public CellSelection getSelection()
  {
    // return selection state model
    return m_selection;
  }

  /***************************************** getCanvas *******************************************/
  /**
   * Returns the canvas that renders table headers and body cells.
   * Also includes any blank excess space beyond the table bounds.
   *
   * @return the table canvas
   */
  public TableCanvas getCanvas()
  {
    // return main rendering canvas
    return m_canvas;
  }

  /*************************************** getColumnsAxis ****************************************/
  /**
   * Returns the columns (horizontal) axis.
   * Manages column widths and x-coordinates.
   *
   * @return the columns axis
   */
  public TableAxis getColumnsAxis()
  {
    // return horizontal axis for column layout
    return m_columnsAxis;
  }

  /***************************************** getRowsAxis *****************************************/
  /**
   * Returns the rows (vertical) axis.
   * Manages row heights and y-coordinates.
   *
   * @return the rows axis
   */
  public TableAxis getRowsAxis()
  {
    // return vertical axis for row layout
    return m_rowsAxis;
  }

  /*********************************** getHorizontalScrollBar ************************************/
  /**
   * Returns the horizontal scrollbar.
   * May not be visible if table fits within view width.
   *
   * @return the horizontal scrollbar
   */
  public TableScrollBar getHorizontalScrollBar()
  {
    // return horizontal scroll control
    return m_horizontalScrollBar;
  }

  /************************************ getVerticalScrollBar *************************************/
  /**
   * Returns the vertical scrollbar.
   * May not be visible if table fits within view height.
   *
   * @return the vertical scrollbar
   */
  public TableScrollBar getVerticalScrollBar()
  {
    // return vertical scroll control
    return m_verticalScrollBar;
  }

  /*************************************** getColumnStartX ***************************************/
  /**
   * Returns the x coordinate of the start of the specified column.
   * Accounts for current horizontal scroll position.
   *
   * @param viewColumn the column index
   * @return the x coordinate in pixels
   */
  public int getColumnStartX( int viewColumn )
  {
    // calculate x coordinate adjusted for scroll offset
    return m_columnsAxis.getPixelStart( viewColumn, (int) getHorizontalScrollBar().getValue() );
  }

  /**************************************** getRowStartY *****************************************/
  /**
   * Returns the y coordinate of the start of the specified row.
   * Accounts for current vertical scroll position.
   *
   * @param viewRow the row index
   * @return the y coordinate in pixels
   */
  public int getRowStartY( int viewRow )
  {
    // calculate y coordinate adjusted for scroll offset
    return m_rowsAxis.getPixelStart( viewRow, (int) getVerticalScrollBar().getValue() );
  }

  /*************************************** getColumnIndex ****************************************/
  /**
   * Returns the column index at the specified x coordinate.
   * Accounts for current horizontal scroll position.
   *
   * @param xCoordinate the x coordinate in pixels
   * @return the column index
   */
  public int getColumnIndex( int xCoordinate )
  {
    // find column at x position adjusted for scroll offset
    return m_columnsAxis.getViewIndexAtPixel( xCoordinate, (int) getHorizontalScrollBar().getValue() );
  }

  /***************************************** getRowIndex *****************************************/
  /**
   * Returns the row index at the specified y coordinate.
   * Accounts for current vertical scroll position.
   *
   * @param yCoordinate the y coordinate in pixels
   * @return the row index
   */
  public int getRowIndex( int yCoordinate )
  {
    // find row at y position adjusted for scroll offset
    return m_rowsAxis.getViewIndexAtPixel( yCoordinate, (int) getVerticalScrollBar().getValue() );
  }

  /*************************************** getHeaderHeight ***************************************/
  /**
   * Returns the height of the table header in pixels.
   *
   * @return the header height
   */
  public int getHeaderHeight()
  {
    // get header row height from vertical axis
    return m_rowsAxis.getHeaderPixels();
  }

  /*************************************** getHeaderWidth ****************************************/
  /**
   * Returns the width of the table header in pixels.
   *
   * @return the header width
   */
  public int getHeaderWidth()
  {
    // get header column width from horizontal axis
    return m_columnsAxis.getHeaderPixels();
  }

  /**************************************** getTableHeight ***************************************/
  /**
   * Returns the total height of the entire table including header.
   *
   * @return the total table height in pixels
   */
  public int getTableHeight()
  {
    // get total vertical extent from rows axis
    return m_rowsAxis.getTotalPixels();
  }

  /**************************************** getTableWidth ****************************************/
  /**
   * Returns the total width of the entire table including header.
   *
   * @return the total table width in pixels
   */
  public int getTableWidth()
  {
    // get total horizontal extent from columns axis
    return m_columnsAxis.getTotalPixels();
  }

  /************************************** isColumnResizable **************************************/
  /**
   * Returns whether the specified column is resizable by the user.
   *
   * @param viewColumn the column index
   * @return true if the column can be resized
   */
  public boolean isColumnResizable( int viewColumn )
  {
    // all columns currently resizable
    return true;
  }

  /*************************************** isRowResizable ****************************************/
  /**
   * Returns whether the specified row is resizable by the user.
   *
   * @param viewRow the row index
   * @return true if the row can be resized
   */
  public boolean isRowResizable( int viewRow )
  {
    // all rows currently resizable
    return true;
  }

}