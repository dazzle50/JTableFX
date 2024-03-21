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

package rjc.table.demo.edit;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import rjc.table.view.axis.TableAxis;
import rjc.table.view.cell.CellDrawer;

/*************************************************************************************************/
/******************************** Example customised cell drawer *********************************/
/*************************************************************************************************/

public class CellDrawerEditable extends CellDrawer
{
  protected final static Insets CELL_TEXT_INSERTS = new Insets( 0.0, 5.0, 1.0, 4.0 );

  /************************************ getTextAlignment *************************************/
  @Override
  protected Pos getTextAlignment()
  {
    // return left alignment for the two text columns
    int dataColumn = getDataColumn();
    if ( viewRow > TableAxis.HEADER )
      if ( dataColumn == RowData.Column.ReadOnly.ordinal() || dataColumn == RowData.Column.Text.ordinal() )
        return Pos.CENTER_LEFT;

    // otherwise centre alignment
    return Pos.CENTER;
  }

  /************************************** getTextInsets **************************************/
  @Override
  protected Insets getTextInsets()
  {
    // return cell text insets with wider right & left margins to give nicer look
    return CELL_TEXT_INSERTS;
  }

}