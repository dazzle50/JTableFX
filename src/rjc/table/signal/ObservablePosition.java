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

package rjc.table.signal;

import rjc.table.Utils;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/************************* Observable position (column index, row index) *************************/
/*************************************************************************************************/

/**
 * Observable position value that can signal listeners when changed.
 * Represents a two-dimensional position with column and row coordinates,
 * typically used for table cell positioning with change notification capabilities.
 */
public class ObservablePosition implements ISignal
{
  private int m_column; // column position index
  private int m_row;    // row position index

  /**************************************** constructor ******************************************/
  /**
   * Creates an observable position with invalid default coordinates.
   * Both column and row are initialised to TableAxis.INVALID.
   */
  public ObservablePosition()
  {
    // construct with invalid default coordinates
    m_column = TableAxis.INVALID;
    m_row = TableAxis.INVALID;
  }

  /**************************************** constructor ******************************************/
  /**
   * Creates an observable position with the specified initial coordinates.
   * 
   * @param column the initial column index
   * @param row    the initial row index
   */
  public ObservablePosition( int column, int row )
  {
    // construct with specified initial coordinates
    m_column = column;
    m_row = row;
  }

  /***************************************** setPosition *****************************************/
  /**
   * Sets new coordinates for this observable position.
   * If either coordinate differs from the current values,
   * signals all listeners with the change notification.
   * 
   * @param column the new column index
   * @param row    the new row index
   */
  public void setPosition( int column, int row )
  {
    // set coordinates and signal if changed
    if ( column != m_column || row != m_row )
    {
      m_column = column;
      m_row = row;
      signal();
    }
  }

  /***************************************** setPosition *****************************************/
  /**
   * Sets coordinates to match another observable position.
   * If the coordinates differ from the current values,
   * signals all listeners with the change notification.
   * 
   * @param position the observable position to copy coordinates from
   */
  public void setPosition( ObservablePosition position )
  {
    // set coordinates to match other observable position
    setPosition( position.getColumn(), position.getRow() );
  }

  /****************************************** getColumn ******************************************/
  /**
   * Gets the current column index of this observable position.
   * 
   * @return the current column index
   */
  public int getColumn()
  {
    // return current column position
    return m_column;
  }

  /******************************************* getRow ********************************************/
  /**
   * Gets the current row index of this observable position.
   * 
   * @return the current row index
   */
  public int getRow()
  {
    // return current row position
    return m_row;
  }

  /****************************************** setColumn ******************************************/
  /**
   * Sets a new column index while keeping the current row unchanged.
   * If the column differs from the current value,
   * signals all listeners with the change notification.
   * 
   * @param column the new column index
   */
  public void setColumn( int column )
  {
    // set column position keeping current row
    setPosition( column, m_row );
  }

  /******************************************* setRow ********************************************/
  /**
   * Sets a new row index while keeping the current column unchanged.
   * If the row differs from the current value,
   * signals all listeners with the change notification.
   * 
   * @param row the new row index
   */
  public void setRow( int row )
  {
    // set row position keeping current column
    setPosition( m_column, row );
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return class name and current coordinates
    return Utils.name( this ) + "[" + m_column + "," + m_row + "]";
  }

}