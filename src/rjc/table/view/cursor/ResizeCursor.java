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

import javafx.geometry.Orientation;
import rjc.table.undo.commands.CommandResize;
import rjc.table.undo.commands.CommandResizeAll;
import rjc.table.undo.commands.ICommandResize;
import rjc.table.view.TableScrollBar;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/******************* Base Mouse cursor for resizing table-view columns & rows ********************/
/*************************************************************************************************/

public class ResizeCursor extends ViewBaseCursor
{
  private static TableAxis      m_axis;      // horizontal or vertical axis
  private static TableScrollBar m_scrollbar; // horizontal or vertical scroll-bar
  private static int            m_offset;    // resize coordinate offset
  private static int            m_before;    // number of positions being resized before current position
  private static ICommandResize m_command;   // command for undo-stack

  /**************************************** constructor ******************************************/
  public ResizeCursor( String imageFile, int xHotspot, int yHotstop )
  {
    super( imageFile, xHotspot, yHotstop );
  }

  /******************************************** start ********************************************/
  protected void setOrientation( Orientation orient )
  {
    // set resize orientation to vertical or horizontal
    if ( orient == Orientation.HORIZONTAL )
    {
      m_scrollbar = view.getHorizontalScrollBar();
      m_axis = view.getColumnsAxis();
    }
    else
    {
      m_scrollbar = view.getVerticalScrollBar();
      m_axis = view.getRowsAxis();
    }
  }

  /******************************************** start ********************************************/
  protected void start( int coordinate, HashSet<Integer> selected )
  {
    // if resize index is not selected, ignore selected
    int index = getSelectedIndex( coordinate );
    if ( !selected.contains( index ) )
      selected.clear();

    // if selected is empty, add resize index
    if ( selected.isEmpty() )
      selected.add( index );

    // count visible selected sections before and adjust offset
    m_offset = m_axis.getStartPixel( index + 1, 0 );
    m_before = 0;
    for ( int section : selected )
      if ( section <= index && m_axis.isIndexVisible( section ) )
      {
        m_before++;
        m_offset -= m_axis.getIndexPixels( section );
      }

    // create resize command
    m_command = new CommandResize( view, m_axis, selected );

    // start resizing
    drag( coordinate );
  }

  /****************************************** startAll *******************************************/
  protected void startAll( int coordinate )
  {
    // resizing all indexes, count visible sections before and including resize index
    int index = getSelectedIndex( coordinate );
    m_offset = m_axis.getIndexPixels( TableAxis.HEADER );
    m_before = 0;
    for ( int section = 0; section <= index; section++ )
      if ( m_axis.isIndexVisible( index ) )
        m_before++;

    // prepare resize command (if selected is null = all)
    m_command = new CommandResizeAll( view, m_axis );

    // start resizing
    drag( coordinate );
  }

  /************************************** getSelectedIndex ***************************************/
  private int getSelectedIndex( int coordinate )
  {
    // determine resize index from mouse coordinate
    int scroll = (int) m_scrollbar.getValue();
    int index = m_axis.getIndexFromCoordinate( coordinate, scroll );
    int indexStart = m_axis.getStartPixel( index, scroll );
    int indexEnd = m_axis.getStartPixel( index + 1, scroll );
    if ( coordinate - indexStart < indexEnd - coordinate )
    {
      index = m_axis.getPreviousVisible( index );
      m_offset = coordinate - indexStart;
    }
    else
      m_offset = coordinate - indexEnd;

    return index;
  }

  /******************************************** drag *********************************************/
  protected void drag( int coordinate )
  {
    // resize columns or rows
    double pixels = ( coordinate - m_offset + m_scrollbar.getValue() ) / m_before;
    int size = (int) ( pixels / view.getZoom().get() );

    // resize
    m_command.setNewSize( size );
    m_command.redo();
  }

  /********************************************* end *********************************************/
  protected void end()
  {
    // end resizing, push resize command onto undo-stack
    view.getUndoStack().push( m_command );
    m_command = null;
  }
}
