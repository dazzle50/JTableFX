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

package rjc.table.view.cell;

import rjc.table.signal.ObservablePosition;
import rjc.table.view.TableView;

/*************************************************************************************************/
/****************************** Observable table-view cell position ******************************/
/*************************************************************************************************/

public class ViewPosition extends ObservablePosition
{
  private TableView m_view; // associated table view

  /**************************************** constructor ******************************************/
  public ViewPosition( TableView view )
  {
    // construct
    super();
    m_view = view;
  }

  /****************************************** getData ********************************************/
  public Object getData()
  {
    // return data-model object for specified view index
    int dataColumn = m_view.getColumnsAxis().getDataIndex( getColumn() );
    int dataRow = m_view.getRowsAxis().getDataIndex( getRow() );
    return m_view.getData().getValue( dataColumn, dataRow );
  }

  /****************************************** isVisible ******************************************/
  public boolean isVisible()
  {
    // return true if position is visible
    return m_view.getColumnsAxis().isIndexVisible( getColumn() ) && m_view.getRowsAxis().isIndexVisible( getRow() );
  }

  /**************************************** moveToVisible ****************************************/
  public void moveToVisible()
  {
    // if position is not visible, move to a visible
    int column = m_view.getColumnsAxis().getVisible( getColumn() );
    int row = m_view.getRowsAxis().getVisible( getRow() );
    setPosition( column, row );
  }

  /***************************************** moveRight *******************************************/
  public void moveRight()
  {
    // move position right one visible column
    int column = m_view.getColumnsAxis().getNextVisible( getColumn() );
    int row = m_view.getRowsAxis().getVisible( getRow() );
    setPosition( column, row );
  }

  /*************************************** moveRightEdge *****************************************/
  public void moveRightEdge()
  {
    // move position to right-most visible column
    int column = m_view.getColumnsAxis().getLastVisible();
    int row = m_view.getRowsAxis().getVisible( getRow() );
    setPosition( column, row );
  }

  /***************************************** moveLeft ********************************************/
  public void moveLeft()
  {
    // move position left one visible column
    int column = m_view.getColumnsAxis().getPreviousVisible( getColumn() );
    int row = m_view.getRowsAxis().getVisible( getRow() );
    setPosition( column, row );
  }

  /**************************************** moveLeftEdge *****************************************/
  public void moveLeftEdge()
  {
    // move position to left-most visible column
    int column = m_view.getColumnsAxis().getFirstVisible();
    int row = m_view.getRowsAxis().getVisible( getRow() );
    setPosition( column, row );
  }

  /******************************************* moveUp ********************************************/
  public void moveUp()
  {
    // move position up one visible row
    int column = m_view.getColumnsAxis().getVisible( getColumn() );
    int row = m_view.getRowsAxis().getPreviousVisible( getRow() );
    setPosition( column, row );
  }

  /****************************************** moveTop ********************************************/
  public void moveTop()
  {
    // move position to top-most visible row
    int column = m_view.getColumnsAxis().getVisible( getColumn() );
    int row = m_view.getRowsAxis().getFirstVisible();
    setPosition( column, row );
  }

  /****************************************** moveDown *******************************************/
  public void moveDown()
  {
    // move position down one visible row
    int column = m_view.getColumnsAxis().getVisible( getColumn() );
    int row = m_view.getRowsAxis().getNextVisible( getRow() );
    setPosition( column, row );
  }

  /**************************************** moveBottom *******************************************/
  public void moveBottom()
  {
    // move position to bottom visible row
    int column = m_view.getColumnsAxis().getVisible( getColumn() );
    int row = m_view.getRowsAxis().getLastVisible();
    setPosition( column, row );
  }

  /*************************************** isColumnAfter *****************************************/
  public boolean isColumnAfter()
  {
    // return true if column position is after
    return getColumn() >= m_view.getColumnsAxis().getCount();
  }

  /***************************************** isRowAfter ******************************************/
  public boolean isRowAfter()
  {
    // return true if row position is after
    return getRow() >= m_view.getRowsAxis().getCount();
  }

}