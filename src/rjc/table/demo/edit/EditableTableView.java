/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
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
import rjc.table.demo.edit.EditableTableRow.Column;
import rjc.table.demo.edit.EditableTableRow.Fruit;
import rjc.table.view.TableView;
import rjc.table.view.cell.CellDrawer;
import rjc.table.view.editor.AbstractCellEditor;
import rjc.table.view.editor.EditorChoose;
import rjc.table.view.editor.EditorDate;
import rjc.table.view.editor.EditorDateTime;
import rjc.table.view.editor.EditorDouble;
import rjc.table.view.editor.EditorInteger;
import rjc.table.view.editor.EditorText;
import rjc.table.view.editor.EditorTime;

/*************************************************************************************************/
/*********************** Example customised table view for editable table ************************/
/*************************************************************************************************/

/**
 * Customised table view for the editable table demo, configuring column widths and providing
 * appropriate cell editors and drawers.
 */
public class EditableTableView extends TableView
{

  /************************************* EditableTableView ***************************************/
  /**
   * Constructs an editable table view bound to the given data source.
   *
   * @param data  the table data source
   * @param name  the display name of this view
   */
  public EditableTableView( TableData data, String name )
  {
    // construct the table view
    super( data, name );
  }

  /******************************************* reset *********************************************/
  /**
   * Resets the view to its custom default settings, including nominal column widths.
   */
  @Override
  public void reset()
  {
    // reset table view to custom settings
    super.reset();

    var axis = getColumnsAxis();
    axis.setNominalSize( Column.ReadOnly.ordinal(), 120 );
    axis.setNominalSize( Column.Text.ordinal(), 120 );
    axis.setNominalSize( Column.Integer.ordinal(), 80 );
    axis.setNominalSize( Column.Double.ordinal(), 80 );
    axis.setNominalSize( Column.DateTime.ordinal(), 180 );
  }

  /*************************************** getCellEditor *****************************************/
  /**
   * Returns the appropriate cell editor for the given cell, or {@code null} for read-only cells.
   *
   * @param cell  the cell drawer describing the cell to be edited
   * @return an {@link AbstractCellEditor} appropriate for the column type, or {@code null}
   */
  @Override
  public AbstractCellEditor getCellEditor( CellDrawer cell )
  {
    // determine editor appropriate for cell
    switch ( Column.values()[cell.dataColumn] )
    {
      case Text:
        return new EditorText();
      case Integer:
        return new EditorInteger();
      case Double:
        return new EditorDouble();
      case Date:
        return new EditorDate();
      case DateTime:
        return new EditorDateTime();
      case Time:
        return new EditorTime();
      case Select:
        return new EditorChoose( Fruit.values() );
      default:
        return null;
    }
  }

  /*************************************** getCellDrawer *****************************************/
  /**
   * Returns a new instance of the customised cell drawer used to render table cells.
   *
   * @return a new {@link EditableTableCellDrawer}
   */
  @Override
  public CellDrawer getCellDrawer()
  {
    // return new instance of class responsible for drawing the cells on canvas
    return new EditableTableCellDrawer();
  }

}