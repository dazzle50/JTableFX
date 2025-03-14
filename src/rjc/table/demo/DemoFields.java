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

package rjc.table.demo;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.GridPane;
import rjc.table.control.ChooseField;
import rjc.table.control.DateField;
import rjc.table.control.DateTimeField;
import rjc.table.control.ExpandingField;
import rjc.table.control.IObservableStatus;
import rjc.table.control.MonthSpinField;
import rjc.table.control.NumberSpinField;
import rjc.table.control.TimeField;
import rjc.table.control.TimeWidget;
import rjc.table.demo.edit.EditableData;
import rjc.table.signal.ObservableStatus;
import rjc.table.undo.UndoStack;

/*************************************************************************************************/
/********************** Demonstrates the enhanced controls for cell editors **********************/
/*************************************************************************************************/

public class DemoFields extends Tab
{
  private ObservableStatus m_status;

  /**************************************** constructor ******************************************/
  public DemoFields( UndoStack undostack, ObservableStatus status )
  {
    // create grid layout for field controls demo
    GridPane grid = new GridPane();
    grid.setPadding( new Insets( 16 ) );
    grid.setHgap( 8 );
    grid.setVgap( 8 );
    m_status = status;

    // prepare fields
    var yearField = new NumberSpinField();
    yearField.setRange( 1000, 5000 );
    yearField.setPrefixSuffix( "Year ", " CE" );
    yearField.setValue( 2000 );

    var monthField = new MonthSpinField();
    monthField.setOverflowField( yearField );

    // layout fields with labels
    int row = 0;
    addToGrid( grid, "ExpandingField", new ExpandingField(), 0, row++ );
    addToGrid( grid, "DateField", new DateField(), 0, row++ );
    addToGrid( grid, "TimeField", new TimeField(), 0, row++ );
    addToGrid( grid, "DateTimeField", new DateTimeField(), 0, row++ );
    addToGrid( grid, "ChooseField", new ChooseField( EditableData.Fruit.values() ), 0, row++ );
    addToGrid( grid, "TimeWidget", new TimeWidget(), 0, row++ );

    row = 0;
    addToGrid( grid, "NumberSpinField", new NumberSpinField(), 1, row++ );
    row++;
    addToGrid( grid, "Below month field overflows into year field", null, 1, row++ );
    addToGrid( grid, "MonthSpinField", monthField, 1, row++ );
    addToGrid( grid, "Year Field", yearField, 1, row++ );

    // configure the tab
    setText( "Fields" );
    setClosable( false );
    setContent( grid );
  }

  /****************************************** addToGrid ******************************************/
  private void addToGrid( GridPane grid, String txt, Node node, int col, int row )
  {
    // create grid layout for field controls demo
    Label label = new Label( txt );
    if ( node == null )
      grid.add( label, 3 * col, row, 2, 1 );
    else
    {
      GridPane.setHalignment( label, HPos.RIGHT );
      grid.add( label, 3 * col, row );
      grid.add( node, 3 * col + 1, row );
    }

    // attach the node to demo status if has observable status
    if ( node instanceof IObservableStatus field )
      field.setStatus( m_status );
  }

}
