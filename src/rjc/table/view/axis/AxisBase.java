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

package rjc.table.view.axis;

import rjc.table.signal.ObservableInteger.ReadOnlyInteger;

/*************************************************************************************************/
/************** Base class for table X or Y axis with count of body cells property ***************/
/*************************************************************************************************/

public class AxisBase
{
  // axis index starts at 0 for table body, index of -1 is for axis header
  final static public int INVALID   = -2;
  final static public int HEADER    = -1;
  final static public int FIRSTCELL = 0;
  final static public int BEFORE    = Integer.MIN_VALUE + 1;
  final static public int AFTER     = Integer.MAX_VALUE - 1;

  // count of body cells on axis
  private ReadOnlyInteger m_countProperty;

  /**************************************** constructor ******************************************/
  public AxisBase( ReadOnlyInteger countProperty )
  {
    // store count of body cells property - normally provided by table-data
    if ( countProperty == null || countProperty.get() < 0 )
      throw new IllegalArgumentException( "Bad body cell count = " + countProperty );
    m_countProperty = countProperty;
  }

  /************************************** getCountProperty ***************************************/
  final public ReadOnlyInteger getCountProperty()
  {
    // return count property for this axis
    return m_countProperty;
  }

  /****************************************** getCount *******************************************/
  final public int getCount()
  {
    // return count of body cells on this axis
    return m_countProperty.get();
  }

}