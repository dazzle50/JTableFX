/**************************************************************************
 *  Copyright (C) 2024 by Richard Crook                                   *
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

import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/********************* Represents one selected rectangle of table-view cells *********************/
/*************************************************************************************************/

public class Selection
{
  final static private int FIRSTCELL = TableAxis.FIRSTCELL;
  final static private int AFTER     = TableAxis.AFTER;

  public int               c1;                             // smallest column index
  public int               r1;                             // smallest row index
  public int               c2;                             // largest column index
  public int               r2;                             // largest row index

  /***************************************** constructor *****************************************/
  public Selection( int columnIndex1, int rowIndex1, int columnIndex2, int rowIndex2 )
  {
    // set private variables in correct order
    set( columnIndex1, rowIndex1, columnIndex2, rowIndex2 );
  }

  /********************************************* set *********************************************/
  public void set( int columnIndex1, int rowIndex1, int columnIndex2, int rowIndex2 )
  {
    // set private variables in correct order
    c1 = Math.min( columnIndex1, columnIndex2 );
    c1 = c1 < FIRSTCELL ? FIRSTCELL : c1;
    c2 = Math.max( columnIndex1, columnIndex2 );
    r1 = Math.min( rowIndex1, rowIndex2 );
    r1 = r1 < FIRSTCELL ? FIRSTCELL : r1;
    r2 = Math.max( rowIndex1, rowIndex2 );
  }

  /*************************************** isCellSelected ****************************************/
  public boolean isCellSelected( int column, int row )
  {
    // return true is specified position is selected
    return column >= c1 && column <= c2 && row >= r1 && row <= r2;
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "@" + Integer.toHexString( System.identityHashCode( this ) ) + "[c1=" + c1
        + " r1=" + r1 + " c2=" + c2 + " r2=" + r2 + "]";
  }
}