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

import rjc.table.data.TableData;
import rjc.table.view.TableView;
import rjc.table.view.cell.CellDrawer;
import rjc.table.view.editor.CellEditorBase;
import rjc.table.view.editor.EditorText;

/*************************************************************************************************/
/*********************** Example customised table view for editable table ************************/
/*************************************************************************************************/

public class TableViewEditable extends TableView
{

  /**************************************** constructor ******************************************/
  public TableViewEditable( TableData data, String name )
  {
    // construct the table view
    super( data, name );
  }

  /******************************************** reset ********************************************/
  @Override
  public void reset()
  {
    // reset table view to custom settings
    super.reset();

    var axis = getColumnsAxis();
    axis.setIndexSize( RowData.Column.ReadOnly.ordinal(), 120 );
    axis.setIndexSize( RowData.Column.Text.ordinal(), 120 );
    axis.setIndexSize( RowData.Column.Integer.ordinal(), 80 );
    axis.setIndexSize( RowData.Column.Double.ordinal(), 80 );
    axis.setIndexSize( RowData.Column.DateTime.ordinal(), 180 );
  }

  /**************************************** getCellEditor ****************************************/
  @Override
  public CellEditorBase getCellEditor( CellDrawer cell )
  {
    // determine editor appropriate for cell
    int column = cell.getDataColumn();
    if ( column == RowData.Column.Text.ordinal() )
      return new EditorText();

    return null;
  }

  /**************************************** getCellDrawer ****************************************/
  @Override
  public CellDrawer getCellDrawer()
  {
    // return new instance of class responsible for drawing the cells on canvas
    return new CellDrawerEditable();
  }

}
