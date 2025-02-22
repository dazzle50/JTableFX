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

import java.util.HashSet;
import java.util.Set;

import javafx.geometry.Orientation;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import rjc.table.Utils;
import rjc.table.data.IDataReorderColumns;
import rjc.table.data.IDataReorderRows;
import rjc.table.undo.IUndoCommand;
import rjc.table.undo.commands.CommandReorderData;
import rjc.table.undo.commands.CommandReorderView;
import rjc.table.view.Colours;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/************** Base Mouse cursor for reordering table-view or data columns & rows ***************/
/*************************************************************************************************/

public class ReorderCursor extends ViewBaseCursor
{
  private static final int        LINE_WIDTH = 5;
  private static Line             m_line;
  private static HashSet<Integer> m_selected;    // columns or rows to be moved
  private static int              m_pos;         // column or row axis position for reordering

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
    m_line.setEndY( Math.min( m_view.getCanvas().getHeight(), m_view.getTableHeight() ) );

    // reselect just the specified columns
    m_selected = m_view.getSelection().getSelectedColumns();
    m_view.getSelection().clear();
    m_view.getSelection().selectColumns( m_selected );

    // start reordering
    dragHorizontal( m_x );
    m_view.add( m_line );
  }

  /**************************************** startVertical ****************************************/
  protected void startVertical()
  {
    // prepare line length
    m_line.setStartX( 0.0 );
    m_line.setEndX( Math.min( m_view.getCanvas().getWidth(), m_view.getTableWidth() ) );

    // reselect just the specified columns
    m_selected = m_view.getSelection().getSelectedRows();
    m_view.getSelection().clear();
    m_view.getSelection().selectRows( m_selected );

    // start reordering
    dragVertical( m_y );
    m_view.add( m_line );
  }

  /*************************************** dragHorizontal ****************************************/
  protected void dragHorizontal( int coordinate )
  {
    // horizontal reordering so position line at nearest column edge
    int column = Utils.clamp( m_view.getColumnIndex( coordinate ), TableAxis.FIRSTCELL,
        m_view.getData().getColumnCount() - 1 );
    double xs = m_view.getColumnStartX( column );
    double xe = m_view.getColumnStartX( column + 1 );
    m_pos = coordinate - xs < xe - coordinate ? column : column + 1;
    double x = coordinate - xs < xe - coordinate ? xs : xe;

    // check x is on visible edge
    if ( x > m_view.getCanvas().getWidth() )
    {
      m_pos = m_view.getColumnIndex( (int) m_view.getCanvas().getWidth() );
      x = m_view.getColumnStartX( m_pos );
    }
    if ( x < m_view.getHeaderWidth() )
    {
      m_pos = m_view.getColumnIndex( m_view.getHeaderWidth() );
      x = m_view.getColumnStartX( m_pos + 1 );
    }

    // place line on column edge
    m_line.setStartX( x );
    m_line.setEndX( x );
  }

  /**************************************** dragVertical *****************************************/
  protected void dragVertical( int coordinate )
  {
    // vertical reordering so position line at nearest row edge
    int row = Utils.clamp( m_view.getRowIndex( coordinate ), TableAxis.FIRSTCELL, m_view.getData().getRowCount() - 1 );
    double ys = m_view.getRowStartY( row );
    double ye = m_view.getRowStartY( row + 1 );
    m_pos = coordinate - ys < ye - coordinate ? row : row + 1;
    double y = coordinate - ys < ye - coordinate ? ys : ye;

    // check y is on visible edge
    if ( y > m_view.getCanvas().getHeight() )
    {
      m_pos = m_view.getRowIndex( (int) m_view.getCanvas().getHeight() );
      y = m_view.getRowStartY( m_pos );
    }
    if ( y < m_view.getHeaderHeight() )
    {
      m_pos = m_view.getRowIndex( m_view.getHeaderHeight() );
      y = m_view.getRowStartY( m_pos + 1 );
    }

    // place line on row edge
    m_line.setStartY( y );
    m_line.setEndY( y );
  }

  /********************************************* end *********************************************/
  protected void end()
  {
    // move selected columns or rows to new position via command
    m_view.remove( m_line );
    Orientation orientation = m_line.getStartX() == m_line.getEndX() ? Orientation.HORIZONTAL : Orientation.VERTICAL;

    // reorder in data-model if supported, otherwise reorder view only
    IUndoCommand command;
    if ( orientation == Orientation.HORIZONTAL )
    {
      // columns
      command = m_view.getData() instanceof IDataReorderColumns
          ? new CommandReorderData( m_view.getData(), orientation, m_selected, m_pos )
          : new CommandReorderView( m_view, m_view.getColumnsAxis(), m_selected, m_pos );
    }
    else
    {
      // rows
      command = m_view.getData() instanceof IDataReorderRows
          ? new CommandReorderData( m_view.getData(), orientation, m_selected, m_pos )
          : new CommandReorderView( m_view, m_view.getRowsAxis(), m_selected, m_pos );
    }

    // push onto stack will only be successful if command is valid (move resulted in changed order)
    m_view.getUndoStack().push( command );

    // update selection
    m_view.getSelection().clear();
    int start = m_pos - countBefore( m_selected, m_pos );
    int end = start + m_selected.size() - 1;

    if ( orientation == Orientation.HORIZONTAL )
    {
      m_view.getSelection().select( start, TableAxis.FIRSTCELL, end, TableAxis.AFTER );
      m_view.getFocusCell().setColumn( start );
      m_view.getSelectCell().setColumn( end );
    }
    else
    {
      m_view.getSelection().select( TableAxis.FIRSTCELL, start, TableAxis.AFTER, end );
      m_view.getFocusCell().setRow( start );
      m_view.getSelectCell().setRow( end );
    }

    m_view.redraw();
  }

  /***************************************** countBefore *****************************************/
  public static int countBefore( Set<Integer> set, int position )
  {
    // count set entries with value lower than position
    int count = 0;
    for ( int value : set )
      if ( value < position )
        count++;

    return count;
  }
}
