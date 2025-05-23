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

package rjc.table.demo.edit;

import javafx.geometry.Pos;
import rjc.table.view.axis.TableAxis;
import rjc.table.view.cell.CellDrawer;

/*************************************************************************************************/
/******************************** Example customised cell drawer *********************************/
/*************************************************************************************************/

public class CellDrawerEditable extends CellDrawer
{
  /************************************ getTextAlignment *************************************/
  @Override
  protected Pos getTextAlignment()
  {
    // return left alignment for the two text columns
    if ( viewRow > TableAxis.HEADER )
      if ( dataColumn == EditableData.Column.ReadOnly.ordinal() || dataColumn == EditableData.Column.Text.ordinal() )
        return Pos.CENTER_LEFT;

    // otherwise centre alignment
    return Pos.CENTER;
  }

}