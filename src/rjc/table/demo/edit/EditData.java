/**************************************************************************
 *  Copyright (C) 2023 by Richard Crook                                   *
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

package rjc.table.demo.edit;

import rjc.table.data.IDataReorderRows;
import rjc.table.data.TableData;

/*************************************************************************************************/
/******************** Example customised table data source for editable table ********************/
/*************************************************************************************************/

public class EditData extends TableData implements IDataReorderRows
{
  public static final int SECTION_READONLY = 0;
  public static final int SECTION_TEXT     = 1;
  public static final int SECTION_INTEGER  = 2;
  public static final int SECTION_DOUBLE   = 3;
  public static final int SECTION_DATE     = 4;
  public static final int SECTION_TIME     = 5;
  public static final int SECTION_DATETIME = 6;
  public static final int SECTION_CHOOSE   = 7;
  public static final int SECTION_MAX      = SECTION_CHOOSE;

  private final int       ROWS             = 30;

  public enum Fruit
  {
    Apple, Banana, Pear, Plum, Orange, Cherry
  }

  /**************************************** constructor ******************************************/
  public EditData()
  {
    // populate the private variables with table contents
    setColumnCount( SECTION_MAX + 1 );
    setRowCount( ROWS );
  }

}