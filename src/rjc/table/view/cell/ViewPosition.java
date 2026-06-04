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

package rjc.table.view.cell;

import rjc.table.signal.ObservablePosition;
import rjc.table.view.TableView;

/*************************************************************************************************/
/****************************** Observable table-view cell position ******************************/
/*************************************************************************************************/

/**
 * Observable table-view cell position with helper methods for navigating through visible rows and
 * columns.
 *
 * <p>The stored column and row are view indices for the associated {@link TableView}. Navigation
 * methods use the table view's row and column axes, so hidden rows and columns are skipped where
 * appropriate.</p>
 */
public class ViewPosition extends ObservablePosition
{
  private TableView m_view; // associated table view

  /**************************************** constructor ******************************************/
  /**
   * Creates a view position associated with the specified table view.
   *
   * @param view the table view whose axes and data model are used by this position
   */
  public ViewPosition( TableView view )
  {
    // construct
    m_view = view;
  }

  /****************************************** getData ********************************************/
  /**
   * Returns the data value for the current view position.
   *
   * <p>The current view column and row are mapped through the table view's axes before the value is
   * requested from the data model.</p>
   *
   * @return the data-model value at the current position
   */
  public Object getData()
  {
    // return data-model object for specified view index
    int dataColumn = m_view.getColumnsAxis().getDataIndex( getColumn() );
    int dataRow = m_view.getRowsAxis().getDataIndex( getRow() );
    return m_view.getData().getValue( dataColumn, dataRow );
  }

  /****************************************** isVisible ******************************************/
  /**
   * Returns whether the current view position is visible in both axes.
   *
   * @return {@code true} if both the current column and row are visible
   */
  public boolean isVisible()
  {
    // return true if position is visible
    return m_view.getColumnsAxis().isVisible( getColumn() ) && m_view.getRowsAxis().isVisible( getRow() );
  }

  /**************************************** moveToVisible ****************************************/
  /**
   * Moves this position to visible column and row indices.
   *
   * <p>If either current index is hidden, the table view's axes choose the nearest visible index
   * according to their visibility rules.</p>
   */
  public void moveToVisible()
  {
    // if position is not visible, move to a visible
    int column = m_view.getColumnsAxis().getVisible( getColumn() );
    int row = m_view.getRowsAxis().getVisible( getRow() );
    setPosition( column, row );
  }

  /***************************************** moveRight *******************************************/
  /**
   * Moves this position to the next visible column to the right.
   *
   * <p>The row is normalised to a visible row before the new position is stored.</p>
   */
  public void moveRight()
  {
    // move position right one visible column
    int column = m_view.getColumnsAxis().getNextVisible( getColumn() );
    int row = m_view.getRowsAxis().getVisible( getRow() );
    setPosition( column, row );
  }

  /*************************************** moveRightEdge *****************************************/
  /**
   * Moves this position to the right-most visible column.
   *
   * <p>The row is normalised to a visible row before the new position is stored.</p>
   */
  public void moveRightEdge()
  {
    // move position to right-most visible column
    int column = m_view.getColumnsAxis().getLastVisible();
    int row = m_view.getRowsAxis().getVisible( getRow() );
    setPosition( column, row );
  }

  /***************************************** moveLeft ********************************************/
  /**
   * Moves this position to the previous visible column to the left.
   *
   * <p>The row is normalised to a visible row before the new position is stored.</p>
   */
  public void moveLeft()
  {
    // move position left one visible column
    int column = m_view.getColumnsAxis().getPreviousVisible( getColumn() );
    int row = m_view.getRowsAxis().getVisible( getRow() );
    setPosition( column, row );
  }

  /**************************************** moveLeftEdge *****************************************/
  /**
   * Moves this position to the left-most visible column.
   *
   * <p>The row is normalised to a visible row before the new position is stored.</p>
   */
  public void moveLeftEdge()
  {
    // move position to left-most visible column
    int column = m_view.getColumnsAxis().getFirstVisible();
    int row = m_view.getRowsAxis().getVisible( getRow() );
    setPosition( column, row );
  }

  /******************************************* moveUp ********************************************/
  /**
   * Moves this position to the previous visible row above the current row.
   *
   * <p>The column is normalised to a visible column before the new position is stored.</p>
   */
  public void moveUp()
  {
    // move position up one visible row
    int column = m_view.getColumnsAxis().getVisible( getColumn() );
    int row = m_view.getRowsAxis().getPreviousVisible( getRow() );
    setPosition( column, row );
  }

  /****************************************** moveTop ********************************************/
  /**
   * Moves this position to the top-most visible row.
   *
   * <p>The column is normalised to a visible column before the new position is stored.</p>
   */
  public void moveTop()
  {
    // move position to top-most visible row
    int column = m_view.getColumnsAxis().getVisible( getColumn() );
    int row = m_view.getRowsAxis().getFirstVisible();
    setPosition( column, row );
  }

  /****************************************** moveDown *******************************************/
  /**
   * Moves this position to the next visible row below the current row.
   *
   * <p>The column is normalised to a visible column before the new position is stored.</p>
   */
  public void moveDown()
  {
    // move position down one visible row
    int column = m_view.getColumnsAxis().getVisible( getColumn() );
    int row = m_view.getRowsAxis().getNextVisible( getRow() );
    setPosition( column, row );
  }

  /**************************************** moveBottom *******************************************/
  /**
   * Moves this position to the bottom-most visible row.
   *
   * <p>The column is normalised to a visible column before the new position is stored.</p>
   */
  public void moveBottom()
  {
    // move position to bottom visible row
    int column = m_view.getColumnsAxis().getVisible( getColumn() );
    int row = m_view.getRowsAxis().getLastVisible();
    setPosition( column, row );
  }

  /*************************************** isColumnAfter *****************************************/
  /**
   * Returns whether the current column is positioned after the last body column.
   *
   * @return {@code true} if the current column is greater than or equal to the column count
   */
  public boolean isColumnAfter()
  {
    // return true if column position is after
    return getColumn() >= m_view.getColumnsAxis().getCount();
  }

  /***************************************** isRowAfter ******************************************/
  /**
   * Returns whether the current row is positioned after the last body row.
   *
   * @return {@code true} if the current row is greater than or equal to the row count
   */
  public boolean isRowAfter()
  {
    // return true if row position is after
    return getRow() >= m_view.getRowsAxis().getCount();
  }

}