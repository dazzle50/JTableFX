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

package rjc.table.view.cursor;

import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import rjc.table.Utils;
import rjc.table.view.Colours;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/************** Base Mouse cursor for reordering table-view or data columns & rows ***************/
/*************************************************************************************************/

public class ReorderCursor extends ViewBaseCursor
{
  private static final int LINE_WIDTH = 5;
  private static Line      m_line;

  /**************************************** constructor ******************************************/
  public ReorderCursor( String imageFile, int xHotspot, int yHotstop )
  {
    // construct reorder cursor
    super( imageFile, xHotspot, yHotstop );

    // initialise line
    m_line = new Line();
    m_line.setStrokeLineCap( StrokeLineCap.BUTT );
    m_line.setStrokeWidth( LINE_WIDTH );
    m_line.setStroke( Colours.REORDER_LINE );
  }

  /*************************************** startHorizontal ***************************************/
  protected void startHorizontal()
  {
    // prepare line length
    m_line.setStartY( 0.0 );
    m_line.setEndY( Math.min( view.getCanvas().getHeight(), view.getTableHeight() ) );

    // reselect just the specified columns TODO
    view.getSelection().clear();

    // start reordering
    dragHorizontal( x );
    view.add( m_line );
  }

  /**************************************** startVertical ****************************************/
  protected void startVertical()
  {
    // prepare line length
    m_line.setStartX( 0.0 );
    m_line.setEndX( Math.min( view.getCanvas().getWidth(), view.getTableWidth() ) );

    // reselect just the specified columns TODO
    view.getSelection().clear();

    // start reordering
    dragVertical( y );
    view.add( m_line );
  }

  /*************************************** dragHorizontal ****************************************/
  protected void dragHorizontal( int coordinate )
  {
    // horizontal reordering so position line at nearest column edge
    int column = Utils.clamp( view.getColumnIndex( coordinate ), TableAxis.FIRSTCELL,
        view.getData().getColumnCount() - 1 );
    double xs = view.getColumnStartX( column );
    double xe = view.getColumnStartX( column + 1 );
    int pos = coordinate - xs < xe - coordinate ? column : column + 1;
    double x = coordinate - xs < xe - coordinate ? xs : xe;

    // check x is on visible edge
    if ( x > view.getCanvas().getWidth() )
    {
      pos = view.getColumnStartX( (int) view.getCanvas().getWidth() );
      x = view.getColumnStartX( pos );
    }
    if ( x < view.getHeaderWidth() )
    {
      pos = view.getColumnStartX( view.getHeaderWidth() );
      x = view.getColumnStartX( pos + 1 );
    }

    // place line on column edge
    m_line.setStartX( x );
    m_line.setEndX( x );
  }

  /**************************************** dragVertical *****************************************/
  protected void dragVertical( int coordinate )
  {
    // vertical reordering so position line at nearest row edge
    int row = Utils.clamp( view.getRowIndex( coordinate ), TableAxis.FIRSTCELL, view.getData().getRowCount() - 1 );
    double ys = view.getRowStartY( row );
    double ye = view.getRowStartY( row + 1 );
    int pos = coordinate - ys < ye - coordinate ? row : row + 1;
    double y = coordinate - ys < ye - coordinate ? ys : ye;

    // check y is on visible edge
    if ( y > view.getCanvas().getHeight() )
    {
      pos = view.getRowIndex( (int) view.getCanvas().getHeight() );
      y = view.getRowStartY( pos );
    }
    if ( y < view.getHeaderHeight() )
    {
      pos = view.getRowIndex( view.getHeaderHeight() );
      y = view.getRowStartY( pos + 1 );
    }

    // place line on row edge
    m_line.setStartY( y );
    m_line.setEndY( y );
  }

  /********************************************* end *********************************************/
  protected void end()
  {
    view.remove( m_line );
  }

}
