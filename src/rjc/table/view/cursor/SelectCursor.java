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

package rjc.table.view.cursor;

import javafx.scene.input.MouseEvent;
import rjc.table.view.TableScrollBar.Animation;

/*************************************************************************************************/
/******************* Mouse cursor when selecting table view cells/columns/rows *******************/
/*************************************************************************************************/

/**
 * Handles dragging for body-cell, whole-row and whole-column selection, including edge scrolling.
 * Uses the shared event/view state maintained by {@link AbstractCursor}; the owning table must
 * route matching primary-gesture events to the active cursor on the JavaFX Application Thread.
 *
 * <p>Dragging updates mouse position and scrolling. {@link #checkSelectPosition()} updates the
 * select-cell endpoint; the table's position listeners perform the selection-area update.</p>
 */
public class SelectCursor extends AbstractCursor
{

  /**************************************** constructor ******************************************/
  /**
   * Creates a selection cursor using an image resource resolved relative to {@link Cursors}.
   *
   * @param imageFile the cursor image resource name
   * @param xHotspot the horizontal hotspot in image pixels
   * @param yHotspot the vertical hotspot in image pixels
   */
  public SelectCursor( String imageFile, int xHotspot, int yHotspot )
  {
    super( imageFile, xHotspot, yHotspot );
  }

  /*************************************** handleReleased ****************************************/
  /**
   * Consumes and records the release event, refreshes the hover cursor at its current coordinates,
   * and stops edge-scrolling animations on both axes. The caller must ensure the release belongs
   * to this cursor's primary selection gesture; this method does not test the released button.
   *
   * @param event the release event whose source is the owning table view
   */
  @Override
  public void handleReleased( MouseEvent event )
  {
    // finishing selecting, so update cursor and stop any animations
    extractDetails( event );
    m_view.getMouseCell().setXY( m_x, m_y, true );
    m_view.getHorizontalScrollBar().stopAnimationStartEnd();
    m_view.getVerticalScrollBar().stopAnimationStartEnd();
  }

  /**************************************** handleDragged ****************************************/
  /**
   * Consumes and records a drag event. While the primary button remains down, updates edge scrolling
   * and the mouse-cell position without replacing the selection cursor. Additional pressed buttons
   * do not prevent primary dragging. Otherwise skips scrolling and position updates.
   *
   * @param event the drag event whose source is the owning table view
   */
  @Override
  public void handleDragged( MouseEvent event )
  {
    // selecting cells by dragging mouse whilst primary button held down
    extractDetails( event );
    if ( !event.isPrimaryButtonDown() )
      return;

    // check if table scrolling is wanted
    checkScrollingX();
    checkScrollingY();

    // update mouse cell position
    m_mouseCell.setXY( m_x, m_y, false );
  }

  /************************************* checkSelectPosition *************************************/
  /**
   * Updates the owning view's select-cell endpoint from the current gesture state and scrolling
   * animation. Requires this cursor's view and position references to have been established by a
   * mouse event. Does not directly modify selection rectangles.
   */
  @Override
  public void checkSelectPosition()
  {
    // update select cell position only if cursor is selecting
    int column = checkColumnPosition();
    int row = checkRowPosition();
    m_view.getSelectCell().setPosition( column, row );
  }

  /************************************* checkColumnPosition *************************************/
  /**
   * Calculates the column endpoint, limiting its upper bound to the last unhidden column. Whole-row
   * gestures initially retain their existing column endpoint, then active horizontal edge-scrolling
   * adjusts the result to the viewport edge.
   *
   * @return the endpoint's view column or an axis sentinel when no body column is resolved
   */
  private int checkColumnPosition()
  {
    // if mouse is beyond the table, limit to last visible column
    var axis = m_view.getColumnsAxis();
    int column = Math.min( axis.getLastVisible(), m_mouseCell.getColumn() );

    // if selecting rows ignore mouse column
    column = this == Cursors.ROWS_SELECT ? m_selectCell.getColumn() : column;

    // if animating to start or end, ensure selection edge is visible on view
    var animation = m_view.getHorizontalScrollBar().getAnimation();
    if ( animation == Animation.TO_START )
    {
      column = m_view.getColumnIndex( m_view.getHeaderWidth() );
      column = axis.getNextVisible( column );
    }
    if ( animation == Animation.TO_END )
    {
      column = m_view.getColumnIndex( (int) m_view.getCanvas().getWidth() );
      column = axis.getPreviousVisible( column );
    }

    return column;
  }

  /************************************** checkRowPosition ***************************************/
  /**
   * Calculates the row endpoint, limiting its upper bound to the last unhidden row. Whole-column
   * gestures initially retain their existing row endpoint, then active vertical edge-scrolling
   * adjusts the result to the viewport edge.
   *
   * @return the endpoint's view row or an axis sentinel when no body row is resolved
   */
  private int checkRowPosition()
  {
    // if mouse is beyond the table, limit to last visible row
    var rowAxis = m_view.getRowsAxis();
    int row = Math.min( rowAxis.getLastVisible(), m_mouseCell.getRow() );

    // if selecting columns ignore mouse row
    row = this == Cursors.COLUMNS_SELECT ? m_selectCell.getRow() : row;

    var animation = m_view.getVerticalScrollBar().getAnimation();
    if ( animation == Animation.TO_START )
    {
      row = m_view.getRowIndex( m_view.getHeaderHeight() );
      row = rowAxis.getNextVisible( row );
    }
    if ( animation == Animation.TO_END )
    {
      row = m_view.getRowIndex( (int) m_view.getCanvas().getHeight() );
      row = rowAxis.getPreviousVisible( row );
    }

    return row;
  }
}
