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

package rjc.table.view;

import javafx.geometry.Orientation;
import javafx.scene.control.ScrollBar;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/*************** Extended version of ScrollBar with special increment & decrement ****************/
/*************************************************************************************************/

public class TableScrollBar extends ScrollBar
{
  private TableAxis       m_axis;    // associated table axis

  final public static int SIZE = 18; // pixels

  /**************************************** constructor ******************************************/
  public TableScrollBar( TableAxis axis, Orientation orientation )
  {
    // create scroll bar
    m_axis = axis;
    setOrientation( orientation );

    // set width/height
    if ( orientation == Orientation.VERTICAL )
    {
      setMinWidth( SIZE );
      setMinHeight( USE_PREF_SIZE );
      setMaxHeight( USE_PREF_SIZE );
    }
    else
    {
      setMinWidth( USE_PREF_SIZE );
      setMaxWidth( USE_PREF_SIZE );
      setMinHeight( SIZE );
    }
  }

}
