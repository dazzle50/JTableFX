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

import javafx.geometry.Point2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.ContextMenuEvent;
import javafx.stage.WindowEvent;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/**************** Coordinates table-menu targeting, selection and popup lifetime *****************/
/*************************************************************************************************/

/**
 * Coordinates context-menu targeting, mouse selection preparation, placement and popup lifetime
 * for one {@link TableView}.
 *
 * <p>The owning view handles editor-owned requests, primary-gesture guards, application validation,
 * editor completion and deferred-selection revision tracking. This helper resolves the resulting
 * request against the current layout and selection, then invokes the view's selection-notification
 * and menu-construction hooks. Keyboard requests preserve selection, cell focus, anchor and mouse
 * position.</p>
 *
 * <p>Each view owns one handler. The handler requests menu contents for each resolved request and
 * releases its active-popup reference when the popup is hidden. Use on the JavaFX Application
 * Thread.</p>
 */
final class TableContextMenuHandler
{
  private final TableView m_view;        // owning view and its subclass customisation hooks
  private ContextMenu     m_contextMenu; // currently open table popup

  /***************************************** constructor *****************************************/
  /**
   * Associates this handler with its owning table view.
   *
   * @param view the owning view and provider of menu customisation hooks
   */
  TableContextMenuHandler( TableView view )
  {
    m_view = view;
  }

  /******************************************** open *********************************************/
  /**
   * Resolves a prepared request, constructs its menu through
   * {@link TableView#buildContextMenu(int, int, boolean)}, and shows a nonempty popup.
   *
   * <p>Mouse requests refresh hit testing before applying the common selection policy. Keyboard
   * requests classify the complete visible selection and use focus, or a selected header member,
   * to identify the target. Unusable targets or placement, null/empty menus, and a missing owner
   * window suppress display.</p>
   *
   * <p>The caller must first validate the request, dismiss the previous table popup, complete
   * editing and invalidate obsolete deferred selection updates. This method deliberately resolves
   * coordinates afterwards because committing an edit may change layout or index mappings.</p>
   *
   * @param event the accepted mouse or keyboard context-menu request
   */
  void open( ContextMenuEvent event )
  {
    boolean keyboard = event.isKeyboardTrigger();
    int row;
    int column;
    Point2D position;
    if ( keyboard )
    {
      row = m_view.getFocusCell().getRow();
      column = m_view.getFocusCell().getColumn();
      boolean rows = m_view.getSelection().areOnlyRowsSelected();
      boolean columns = m_view.getSelection().areOnlyColumnsSelected();
      if ( rows && !columns )
      {
        if ( !m_view.getRowsAxis().isVisible( row ) || !m_view.getSelection().isRowSelected( row ) )
          row = getFirstSelectedIndex( false );
        column = TableAxis.HEADER;
      }
      else if ( columns && !rows )
      {
        if ( !m_view.getColumnsAxis().isVisible( column ) || !m_view.getSelection().isColumnSelected( column ) )
          column = getFirstSelectedIndex( true );
        row = TableAxis.HEADER;
      }
      else if ( !m_view.getRowsAxis().isVisible( row ) || !m_view.getColumnsAxis().isVisible( column ) )
        return;
      position = getKeyboardMenuPosition( row, column );
      if ( position == null )
        return;
    }
    else
    {
      // committing an edit may have changed the layout and view-to-data mapping
      var local = m_view.screenToLocal( event.getScreenX(), event.getScreenY() );
      if ( !isOnCanvas( local ) )
        return;
      m_view.getMouseCell().refreshXY( (int) Math.floor( local.getX() ), (int) Math.floor( local.getY() ), false );
      row = m_view.getMouseCell().getRow();
      column = m_view.getMouseCell().getColumn();
      selectContextMenuTarget( row, column );
      position = new Point2D( event.getScreenX(), event.getScreenY() );
    }

    var menu = m_view.buildContextMenu( row, column, keyboard );
    if ( menu == null || menu.getItems().isEmpty() || m_view.getScene() == null
        || m_view.getScene().getWindow() == null )
      return;
    m_contextMenu = menu;
    menu.addEventHandler( WindowEvent.WINDOW_HIDDEN, hidden ->
    {
      if ( m_contextMenu == menu )
        m_contextMenu = null;
    } );
    menu.show( m_view.getScene().getWindow(), position.getX(), position.getY() );
  }

  /*************************************** hideContextMenu ***************************************/
  /**
   * Clears the active-popup reference and hides that popup if one exists. Clearing the reference
   * first prevents its hidden-event callback from interfering with a subsequently opened menu.
   */
  void hideContextMenu()
  {
    var menu = m_contextMenu;
    m_contextMenu = null;
    if ( menu != null )
      menu.hide();
  }

  /***************************************** isOnCanvas ******************************************/
  /**
   * Tests whether a view-local position lies inside the table canvas, excluding its right and
   * bottom edges so adjoining scrollbars do not count as table-menu targets.
   *
   * @param position the position in the owning view's local coordinates, or null if unavailable
   * @return true if the position is inside the canvas; false for null or an outside position
   */
  boolean isOnCanvas( Point2D position )
  {
    var bounds = m_view.getCanvas().getBoundsInParent();
    return position != null && position.getX() >= bounds.getMinX() && position.getX() < bounds.getMaxX()
        && position.getY() >= bounds.getMinY() && position.getY() < bounds.getMaxY();
  }

  /************************************ getFirstSelectedIndex ************************************/
  /**
   * Finds the lowest unhidden, fully selected index for a keyboard header request. Called after
   * the complete selection has been classified as whole rows or whole columns.
   *
   * @param columns true to search columns; false to search rows
   * @return the first selected visible view index, or {@link TableAxis#INVALID} if none exists
   */
  private int getFirstSelectedIndex( boolean columns )
  {
    var axis = columns ? m_view.getColumnsAxis() : m_view.getRowsAxis();
    int first = TableAxis.INVALID;
    for ( var area : m_view.getSelection().getAreas() )
    {
      int start = area[columns ? 0 : 1];
      int end = area[columns ? 2 : 3];
      if ( start > end )
        continue;
      int index = axis.getNextVisible( start - 1 );
      if ( index >= start && index <= end && ( first == TableAxis.INVALID || index < first )
          && ( columns ? m_view.getSelection().isColumnSelected( index )
              : m_view.getSelection().isRowSelected( index ) ) )
        first = index;
    }
    return first;
  }

  /*********************************** getKeyboardMenuPosition ***********************************/
  /**
   * Converts a resolved keyboard target to a screen position, clamping the local anchor to the
   * canvas viewport without scrolling or changing selection. The coordinates must already
   * identify a valid body cell or header target.
   *
   * @param row the resolved row view index, or {@link TableAxis#HEADER} for a column-header menu
   * @param column the resolved column view index, or {@link TableAxis#HEADER} for a row-header menu
   * @return the screen position, or null if the canvas has no extent, a coordinate is below
   *         {@code HEADER}, or the view cannot be mapped to screen coordinates
   */
  private Point2D getKeyboardMenuPosition( int row, int column )
  {
    double width = m_view.getCanvas().getWidth();
    double height = m_view.getCanvas().getHeight();
    if ( width <= 0 || height <= 0 || row < TableAxis.HEADER || column < TableAxis.HEADER )
      return null;

    // clamp the resolved target to the viewport without changing focus, selection or scrolling
    double x = column == TableAxis.HEADER ? m_view.getHeaderWidth() / 2.0
        : m_view.getColumnStartX( column ) + m_view.getColumnsAxis().getPixelSize( column ) / 2.0;
    double y = row == TableAxis.HEADER ? m_view.getHeaderHeight() / 2.0
        : m_view.getRowStartY( row ) + m_view.getRowsAxis().getPixelSize( row );
    x = Math.max( 0, Math.min( width - 1, x ) );
    y = Math.max( 0, Math.min( height - 1, y ) );
    return m_view.localToScreen( x, y );
  }

  /*********************************** selectContextMenuTarget ***********************************/
  /**
   * Applies the mouse context-menu selection policy synchronously. A fully selected header or
   * selected body cell preserves existing areas, focus and anchor. Other headers replace the
   * selection with their whole row/column; an unselected body cell becomes the sole selected and
   * focused cell. Corner and trailing-space requests leave selection unchanged.
   *
   * <p>An empty opposite axis does not cause a header request to invent a body focus cell.
   * Explicit selection signals precede
   * {@link TableView#contextMenuSelectionChanged(int, int)}, allowing the view to discard obsolete
   * deferred position updates. The hook is called only when this request creates a selection.</p>
   *
   * @param viewRow the mouse row view index, including header or trailing-space sentinels
   * @param viewColumn the mouse column view index, including header or trailing-space sentinels
   */
  private void selectContextMenuTarget( int viewRow, int viewColumn )
  {
    // preserve the selected group and its anchor when opening a menu on a selected header
    if ( viewRow == TableAxis.HEADER && viewColumn >= TableAxis.FIRSTCELL
        && viewColumn < m_view.getColumnsAxis().getCount() )
    {
      if ( m_view.getSelection().isColumnSelected( viewColumn ) )
        return;

      int topRow = m_view.getRowIndex( m_view.getHeaderHeight() );
      m_view.getSelection().clear();
      if ( m_view.getRowsAxis().isVisible( topRow ) )
      {
        m_view.getFocusCell().setPosition( viewColumn, topRow );
        m_view.getSelectCell().setPosition( viewColumn, TableAxis.AFTER );
      }
      m_view.getSelection().select( viewColumn, 0, viewColumn, TableAxis.AFTER );
    }
    else if ( viewColumn == TableAxis.HEADER && viewRow >= TableAxis.FIRSTCELL
        && viewRow < m_view.getRowsAxis().getCount() )
    {
      if ( m_view.getSelection().isRowSelected( viewRow ) )
        return;

      int leftColumn = m_view.getColumnIndex( m_view.getHeaderWidth() );
      m_view.getSelection().clear();
      if ( m_view.getColumnsAxis().isVisible( leftColumn ) )
      {
        m_view.getFocusCell().setPosition( leftColumn, viewRow );
        m_view.getSelectCell().setPosition( TableAxis.AFTER, viewRow );
      }
      m_view.getSelection().select( 0, viewRow, TableAxis.AFTER, viewRow );
    }
    else if ( m_view.getRowsAxis().isVisible( viewRow ) && m_view.getColumnsAxis().isVisible( viewColumn ) )
    {
      if ( m_view.getSelection().isCellSelected( viewColumn, viewRow ) )
        return;
      m_view.getSelection().clear();
      m_view.getFocusCell().setPosition( viewColumn, viewRow );
      m_view.getSelectCell().setPosition( viewColumn, viewRow );
      m_view.getSelection().select( viewColumn, viewRow, viewColumn, viewRow );
    }
    else
      return;

    // explicit selection signals invalidate queued position updates before subclass notification
    m_view.contextMenuSelectionChanged( viewRow, viewColumn );
    m_view.getMouseCell().revalidateCursor();
  }

}
