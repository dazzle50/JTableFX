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

package rjc.table.undo.commands;

import rjc.table.HashSetInt;
import rjc.table.undo.IUndoCommand;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;
import rjc.table.view.cursor.AbstractReorderCursor;

/*************************************************************************************************/
/******************* UndoCommand for reordering columns or rows on table-view ********************/
/*************************************************************************************************/

public class CommandReorderView implements IUndoCommand
{
  private TableView  m_view;    // table view
  private TableAxis  m_axis;    // axis being reordered
  private HashSetInt m_indexes; // view-indexes being moved
  private int        m_insert;  // insert position
  private String     m_text;    // text describing command

  /**************************************** constructor ******************************************/
  public CommandReorderView( TableView view, TableAxis axis, HashSetInt selected, int insertIndex )
  {
    // prepare reorder command
    m_view = view;
    m_axis = axis;
    m_indexes = selected;
    m_insert = insertIndex;

    // test if reorder changes mapping, if not make command invalid
    var hash = m_axis.getIndexMappingHash();
    redo();
    if ( hash == m_axis.getIndexMappingHash() )
      m_view = null;
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    // action command and redraw view
    m_axis.reorder( m_indexes, m_insert );
    m_view.redraw();
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // revert command
    int newOffset = AbstractReorderCursor.countBefore( m_indexes, m_insert );
    int oldOffset = m_indexes.size() - newOffset;

    // create ordered list to process moves in predictable order
    var list = m_indexes.toSortedArray();

    // move columns or rows back to their prior positions
    HashSetInt index = new HashSetInt();
    for ( int oldPos : list )
      if ( m_insert > oldPos )
      {
        index.clear();
        index.add( m_insert - newOffset );
        m_axis.reorder( index, oldPos );
        newOffset--;
      }
      else
      {
        index.clear();
        index.add( m_insert );
        m_axis.reorder( index, oldPos + oldOffset );
        oldOffset--;
      }

    // redraw table in this view only
    m_view.redraw();
  }

  /******************************************* text **********************************************/
  @Override
  public String text()
  {
    // command description
    if ( m_text == null )
    {
      m_text = "Moved " + m_indexes.size() + ( m_axis == m_view.getColumnsAxis() ? " column" : " row" )
          + ( m_indexes.size() > 1 ? "s" : "" );

      if ( m_view.getId() != null )
        m_text = m_view.getId() + " - " + m_text;
    }
    return m_text;
  }

  /******************************************* isValid *******************************************/
  @Override
  public boolean isValid()
  {
    // command is valid only if reorder results in difference (via hashcode)
    return m_view != null;
  }
}
