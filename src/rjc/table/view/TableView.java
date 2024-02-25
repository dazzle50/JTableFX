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
import javafx.scene.Parent;
import rjc.table.data.TableData;
import rjc.table.view.cell.CellDrawer;

/*************************************************************************************************/
/************** Base class for scrollable table-view to visualise a table-data model *************/
/*************************************************************************************************/

public class TableView extends Parent
{
  private TableData      m_data;

  private TableCanvas    m_canvas;
  private TableScrollBar m_verticalScrollBar;
  private TableScrollBar m_horizontalScrollBar;

  private CellDrawer     m_drawer;

  /**************************************** constructor ******************************************/
  public TableView( TableData data, String name )
  {
    // set view name and construct the view
    if ( data == null )
      throw new NullPointerException( "TableData must not be null" );
    m_data = data;
    setId( name );

    m_canvas = new TableCanvas( this );
    m_horizontalScrollBar = new TableScrollBar( m_canvas.getColumnAxis(), Orientation.HORIZONTAL );
    m_verticalScrollBar = new TableScrollBar( m_canvas.getRowAxis(), Orientation.VERTICAL );
    getChildren().addAll( m_canvas, m_canvas.getOverlay(), m_horizontalScrollBar, m_verticalScrollBar );

    // react to losing & gaining focus and visibility
    focusedProperty().addListener( ( observable, oldFocus, newFocus ) -> redraw() );
    visibleProperty().addListener( ( observable, oldVisibility, newVisibility ) -> redraw() );
  }

  /******************************************* redraw ********************************************/
  public void redraw()
  {
    // request redraw of full visible table (headers and body)
    getCanvas().redraw();
  }

  /****************************************** getData ********************************************/
  public TableData getData()
  {
    // return data model for table-view
    return m_data;
  }

  /***************************************** getCanvas *******************************************/
  public TableCanvas getCanvas()
  {
    // return canvas (shows table headers & body cells + BLANK excess space) for table-view
    return m_canvas;
  }

  /*********************************** getHorizontalScrollBar ************************************/
  public TableScrollBar getHorizontalScrollBar()
  {
    // return horizontal scroll bar (will not be visible if not needed) for table-view
    return m_horizontalScrollBar;
  }

  /************************************ getVerticalScrollBar *************************************/
  public TableScrollBar getVerticalScrollBar()
  {
    // return vertical scroll bar (will not be visible if not needed) for table-view
    return m_verticalScrollBar;
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

  /*************************************** getCellDrawer *****************************************/
  public CellDrawer getCellDrawer()
  {
    // return class responsible for drawing the cells on canvas
    if ( m_drawer == null )
      m_drawer = new CellDrawer();
    return m_drawer;
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
