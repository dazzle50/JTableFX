
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

package rjc.table.undo.commands;

import java.util.HashMap;
import java.util.HashSet;

import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/******************* UndoCommand for resizing columns widths or rows heights *********************/
/*************************************************************************************************/

public class CommandResize implements ICommandResize
{
  private TableView                 m_view;                 // table view
  private TableAxis                 m_axis;                 // columns or rows being resized
  private HashSet<Integer>          m_indexes;              // indexes being resized
  private String                    m_text;                 // text describing command

  private HashMap<Integer, Integer> m_oldExceptions;        // old size exceptions before resize
  private int                       m_newSize;              // new size

  final static private int          NO_EXCEPTION = -999999; // no size exception

  /**************************************** constructor ******************************************/
  public CommandResize( TableView view, TableAxis axis, HashSet<Integer> selected )
  {
    // prepare resize command
    m_view = view;
    m_axis = axis;
    m_indexes = selected;

    // get any exceptions for selected before resizing starts
    m_oldExceptions = new HashMap<>();
    selected.forEach( ( index ) ->
    {
      int size = axis.getSizeExceptions().getOrDefault( index, NO_EXCEPTION );
      m_oldExceptions.put( index, size );
    } );
  }

  /***************************************** setNewSize ******************************************/
  @Override
  public void setNewSize( int size )
  {
    // set command new size
    m_newSize = size;
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    // action command
    for ( int index : m_indexes )
      m_axis.setIndexSize( index, m_newSize );

    // update layout in case scroll-bar changed and redraw table view
    m_view.layoutDisplay();
    m_view.redraw();
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // revert command - restore old exceptions
    m_oldExceptions.forEach( ( index, size ) ->
    {
      if ( size == NO_EXCEPTION )
        m_axis.clearIndexSize( index );
      else
        m_axis.setIndexSize( index, size );
    } );

    // update layout in case scroll-bar need changed and redraw table view
    m_view.layoutDisplay();
    m_view.redraw();
  }

  /******************************************* text **********************************************/
  @Override
  public String text()
  {
    // command description
    if ( m_text == null )
    {
      m_text = "Resized " + m_indexes.size();
      m_text += m_axis == m_view.getCanvas().getColumnsAxis() ? " column" : " row";
      m_text += m_indexes.size() > 1 ? "s" : "";

      if ( m_view.getId() != null )
        m_text = m_view.getId() + " - " + m_text;
    }

    return m_text;
  }

}
