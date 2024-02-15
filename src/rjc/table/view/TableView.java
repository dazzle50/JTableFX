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

import javafx.scene.Parent;
import rjc.table.data.TableData;

/*************************************************************************************************/
/************** Base class for scrollable table-view to visualise a table-data model *************/
/*************************************************************************************************/

public class TableView extends Parent
{
  private TableData      m_data;

  private TableCanvas    m_canvas;
  private TableScrollBar m_verticalScrollBar;
  private TableScrollBar m_horizontalScrollBar;

  /**************************************** constructor ******************************************/
  public TableView( TableData data, String name )
  {
    // set view name and construct the view
    setId( name );
  }

  /**************************************** constructor ******************************************/
  public TableView( TableData data )
  {
    // construct the view
  }

  /*************************************** getColumnStartX ***************************************/
  public int getColumnStartX( int columnIndex )
  {
    // return x coordinate of cell start for specified column position
    return 0; // TODO
  }

  /**************************************** getRowStartY *****************************************/
  public int getRowStartY( int rowIndex )
  {
    // return y coordinate of cell start for specified row position
    return 0; // TODO
  }

  /*************************************** getColumnIndex ****************************************/
  public int getColumnIndex( int xCoordinate )
  {
    // return column index at specified x coordinate
    return 0; // TODO
  }

  /***************************************** getRowIndex *****************************************/
  public int getRowIndex( int yCoordinate )
  {
    // return row index at specified y coordinate
    return 0; // TODO
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return as string
    return getClass().getSimpleName() + "@" + Integer.toHexString( System.identityHashCode( this ) ) + "[ID=" + getId()
        + " m_canvas=" + m_canvas + "]";
  }

}
