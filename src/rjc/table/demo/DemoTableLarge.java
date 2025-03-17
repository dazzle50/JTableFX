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

import javafx.scene.control.Tab;
import rjc.table.data.TableData;
import rjc.table.demo.large.LargeView;
import rjc.table.signal.ObservableStatus;
import rjc.table.undo.UndoStack;
import rjc.table.view.TableView;

/*************************************************************************************************/
/*************************** Demonstrates a very large table and view ****************************/
/*************************************************************************************************/

public class DemoTableLarge extends Tab
{
  private static TableData m_data; // data for the table view

  /**************************************** constructor ******************************************/
  public DemoTableLarge( UndoStack undostack, ObservableStatus status )
  {
    // create default table (but with many rows & columns)
    if ( m_data == null )
    {
      m_data = new TableData();
      m_data.setColumnCount( 1_000_000 );
      m_data.setRowCount( 1_000_000 );
    }

    // create specialised view
    TableView view = new LargeView( m_data, "Large" );
    view.setUndostack( undostack );
    view.setStatus( status );

    // make view only visible when tab is selected
    view.visibleProperty().bind( selectedProperty() );

    // configure the tab
    setText( view.getId() );
    setClosable( false );
    setContent( view );
  }

}
