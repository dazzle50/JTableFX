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

package rjc.table.view.action;

import java.util.HashSet;

import rjc.table.undo.commands.CommandResize;
import rjc.table.undo.commands.CommandResizeAll;
import rjc.table.undo.commands.ICommandResize;
import rjc.table.view.TableScrollBar;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/******************************* Supports column and row re-sizing *******************************/
/*************************************************************************************************/

public class Resize
{
  private static TableView      m_view;       // table view for resizing
  private static TableAxis      m_axis;       // horizontal or vertical axis
  private static TableScrollBar m_scrollbar;  // horizontal or vertical scroll-bar

  private static int            m_coordinate; // latest coordinate used when table scrolled
  private static int            m_offset;     // resize coordinate offset
  private static int            m_before;     // number of positions being resized before current position
  private static ICommandResize m_command;    // command for undo-stack

  /**************************************** startColumns *****************************************/
  public static void startColumns( TableView view, int coordinate )
  {
    // static method to support column resizing
    m_view = view;
    m_axis = view.getCanvas().getColumnsAxis();
    m_scrollbar = view.getHorizontalScrollBar();

    // if selected is "null" means all indexes selected
    var selected = view.getSelection().getResizableColumns();
    if ( selected == null )
      startAll( coordinate );
    else
      start( coordinate, selected );
  }

  /****************************************** startRows ******************************************/
  public static void startRows( TableView view, int coordinate )
  {
    // static method to support row resizing
    m_view = view;
    m_axis = view.getCanvas().getRowsAxis();
    m_scrollbar = view.getVerticalScrollBar();

    // if selected is "null" means all indexes selected
    var selected = view.getSelection().getResizableRows();
    if ( selected == null )
      startAll( coordinate );
    else
      start( coordinate, selected );
  }

  /******************************************** start ********************************************/
  private static void start( int coordinate, HashSet<Integer> selected )
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
    m_command = new CommandResize( m_view, m_axis, selected );

    // start reordering
    drag( coordinate );
  }

  /****************************************** startAll *******************************************/
  private static void startAll( int coordinate )
  {
    // resizing all indexes, count visible sections before and including resize index
    int index = getSelectedIndex( coordinate );
    m_offset = m_axis.getIndexPixels( TableAxis.HEADER );
    m_before = 0;
    for ( int section = 0; section <= index; section++ )
      if ( m_axis.isIndexVisible( index ) )
        m_before++;

    // prepare resize command (if selected is null = all)
    m_command = new CommandResizeAll( m_view, m_axis );

    // start reordering
    drag( coordinate );
  }

  /************************************** getSelectedIndex ***************************************/
  private static int getSelectedIndex( int coordinate )
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
  public static void drag( int coordinate )
  {
    // resize columns or rows
    m_coordinate = coordinate;
    double pixels = ( coordinate - m_offset + m_scrollbar.getValue() ) / m_before;
    int size = (int) ( pixels / m_view.getZoom().get() );

    // resize
    m_command.setNewSize( size );
    m_command.redo();
  }

  /***************************************** inProgress ******************************************/
  public static boolean inProgress()
  {
    // if no resize in progress return false
    if ( m_view == null )
      return false;

    // only resize if scrolling to start or end to avoid feedback loop
    var vsb = m_view.getVerticalScrollBar();
    var hsb = m_view.getHorizontalScrollBar();
    if ( vsb.isAnimationStartEnd() || hsb.isAnimationStartEnd() )
      drag( m_coordinate );

    // return true as resize in progress
    return true;
  }

  /********************************************* end *********************************************/
  public static void end()
  {
    // end resizing, push resize command onto undo-stack
    m_view.getUndoStack().push( m_command );
    m_view = null;
    m_command = null;
  }
}