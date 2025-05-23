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

package rjc.table.view.cell;

import javafx.scene.canvas.GraphicsContext;
import rjc.table.Utils;
import rjc.table.view.TableView;

/*************************************************************************************************/
/*************************** Table cell location on table-view canvas ****************************/
/*************************************************************************************************/

public class CellLocation
{
  public int             viewColumn; // cell column index
  public int             viewRow;    // cell row index

  public TableView       view;       // table view
  public GraphicsContext gc;         // graphics context drawing
  public double          x;          // start x coordinate of cell on canvas
  public double          y;          // start y coordinate of cell on canvas
  public double          w;          // width of cell on canvas
  public double          h;          // height of cell on canvas

  /****************************************** setIndex *******************************************/
  public void setIndex( TableView tableView, int viewColumnIndex, int viewRowIndex )
  {
    // set cell location for table-view cell index
    view = tableView;
    viewColumn = viewColumnIndex;
    viewRow = viewRowIndex;

    gc = view.getCanvas().getGraphicsContext2D();
    x = view.getColumnStartX( viewColumnIndex );
    y = view.getRowStartY( viewRowIndex );
    w = view.getColumnStartX( viewColumnIndex + 1 ) - x;
    h = view.getRowStartY( viewRowIndex + 1 ) - y;
  }

  /**************************************** getDataColumn ****************************************/
  public int getDataColumn()
  {
    // return data-model column for this cell
    return view.getColumnsAxis().getDataIndex( viewColumn );
  }

  /***************************************** getDataRow ******************************************/
  public int getDataRow()
  {
    // return data-model row for this cell
    return view.getRowsAxis().getDataIndex( viewRow );
  }

  /***************************************** getValue ********************************************/
  public Object getValue()
  {
    // return data-model object value for this cell
    return view.getData().getValue( getDataColumn(), getDataRow() );
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    return Utils.name( this ) + "[viewColumn=" + viewColumn + ", viewRow=" + viewRow + ", x=" + x + ", y=" + y + ", w="
        + w + ", h=" + h + "]";
  }
}
