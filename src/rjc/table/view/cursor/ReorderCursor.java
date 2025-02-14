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
    m_line.setEndY( Math.min( view.getCanvas().getHeight(), view.getTableHeight() ) );

    // reselect just the specified columns
    m_selected = view.getSelection().getSelectedColumns();
    view.getSelection().clear();
    view.getSelection().selectColumns( m_selected );

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

    // reselect just the specified columns
    m_selected = view.getSelection().getSelectedRows();
    view.getSelection().clear();
    view.getSelection().selectRows( m_selected );

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
    m_pos = coordinate - xs < xe - coordinate ? column : column + 1;
    double x = coordinate - xs < xe - coordinate ? xs : xe;

    // check x is on visible edge
    if ( x > view.getCanvas().getWidth() )
    {
      m_pos = view.getColumnStartX( (int) view.getCanvas().getWidth() );
      x = view.getColumnStartX( m_pos );
    }
    if ( x < view.getHeaderWidth() )
    {
      m_pos = view.getColumnStartX( view.getHeaderWidth() );
      x = view.getColumnStartX( m_pos + 1 );
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
    m_pos = coordinate - ys < ye - coordinate ? row : row + 1;
    double y = coordinate - ys < ye - coordinate ? ys : ye;

    // check y is on visible edge
    if ( y > view.getCanvas().getHeight() )
    {
      m_pos = view.getRowIndex( (int) view.getCanvas().getHeight() );
      y = view.getRowStartY( m_pos );
    }
    if ( y < view.getHeaderHeight() )
    {
      m_pos = view.getRowIndex( view.getHeaderHeight() );
      y = view.getRowStartY( m_pos + 1 );
    }

    // place line on row edge
    m_line.setStartY( y );
    m_line.setEndY( y );
  }

  /********************************************* end *********************************************/
  protected void end()
  {
    // move selected columns or rows to new position via command
    Orientation orientation = m_line.getStartX() == m_line.getEndX() ? Orientation.HORIZONTAL : Orientation.VERTICAL;
    view.remove( m_line );
    view.getSelection().clear();

    int start = m_pos - countBefore( m_selected, m_pos );
    int end = start + m_selected.size() - 1;
    // int beforeHashcode = orientation == Orientation.HORIZONTAL ? view.getColumnsAxis().orderHashcode()
    // : view.getRowsAxis().orderHashcode();

    IUndoCommand command;
    if ( orientation == Orientation.HORIZONTAL )
    {
      // columns
      command = view.getData() instanceof IDataReorderColumns
          ? new CommandReorderData( view.getData(), orientation, m_selected, m_pos )
          : new CommandReorderView( view, view.getColumnsAxis(), m_selected, m_pos );
    }
    else
    {
      // rows
      command = view.getData() instanceof IDataReorderRows
          ? new CommandReorderData( view.getData(), orientation, m_selected, m_pos )
          : new CommandReorderView( view, view.getRowsAxis(), m_selected, m_pos );
    }
    command.redo();

    // check that move has resulted in changed order
    // int afterHashcode = orientation == Orientation.HORIZONTAL ? view.getColumnsAxis().orderHashcode()
    // : view.getRowsAxis().orderHashcode();
    // if ( beforeHashcode != afterHashcode )
    view.getUndoStack().push( command );

    if ( orientation == Orientation.HORIZONTAL )
    {
      view.getSelection().select( start, TableAxis.FIRSTCELL, end, TableAxis.AFTER );
      view.getFocusCell().setColumn( start );
      view.getSelectCell().setColumn( end );
    }
    else
    {
      view.getSelection().select( TableAxis.FIRSTCELL, start, TableAxis.AFTER, end );
      view.getFocusCell().setRow( start );
      view.getSelectCell().setRow( end );
    }

    view.redraw();
  }

  /***************************************** countBefore *****************************************/
  private static int countBefore( Set<Integer> set, int position )
  {
    // count set entries with value lower than position
    int count = 0;
    for ( int value : set )
      if ( value < position )
        count++;

    return count;
  }
}
