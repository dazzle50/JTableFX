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

public class ObservablePosition implements ISignal
{
  private int m_column;
  private int m_row;

  /**************************************** constructor ******************************************/
  public ObservablePosition()
  {
    // construct
    m_column = TableAxis.INVALID;
    m_row = TableAxis.INVALID;
  }

  /**************************************** constructor ******************************************/
  public ObservablePosition( int column, int row )
  {
    // construct and set position
    m_column = column;
    m_row = row;
  }

  /***************************************** setPosition *****************************************/
  public void setPosition( int column, int row )
  {
    // if position is change, fire listeners
    if ( column != m_column || row != m_row )
    {
      m_column = column;
      m_row = row;
      signal();
    }
  }

  /***************************************** setPosition *****************************************/
  public void setPosition( ObservablePosition position )
  {
    // set position to same as other observable-position
    setPosition( position.getColumn(), position.getRow() );
  }

  /****************************************** getColumn ******************************************/
  public int getColumn()
  {
    // return column position
    return m_column;
  }

  /******************************************* getRow ********************************************/
  public int getRow()
  {
    // return row position
    return m_row;
  }

  /****************************************** setColumn ******************************************/
  public void setColumn( int column )
  {
    // set column position
    setPosition( column, m_row );
  }

  /******************************************* setRow ********************************************/
  public void setRow( int row )
  {
    // set row position
    setPosition( m_column, row );
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return as string
    return Utils.name( this ) + "[" + m_column + "," + m_row + "]";
  }

}