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

import javafx.geometry.Orientation;
import rjc.table.HashSetInt;
import rjc.table.data.IDataReorderColumns;
import rjc.table.data.IDataReorderRows;
import rjc.table.data.TableData;
import rjc.table.undo.IUndoCommand;
import rjc.table.view.cursor.AbstractReorderCursor;

/*************************************************************************************************/
/******************* UndoCommand for reordering columns or rows in table-data ********************/
/*************************************************************************************************/

public class CommandReorderData implements IUndoCommand
{
  private TableData   m_data;        // table data
  private Orientation m_orientation; // orientation being reordered
  private HashSetInt  m_indexes;     // data-indexes being moved
  private int         m_insert;      // insert position
  private String      m_text;        // text describing command

  /**************************************** constructor ******************************************/
  public CommandReorderData( TableData data, Orientation orientation, HashSetInt selected, int insertIndex )
  {
    // prepare reorder command
    m_data = data;
    m_orientation = orientation;
    m_indexes = selected;
    m_insert = insertIndex;

    // action the reorder - if not successful invalid the command
    boolean success = m_orientation == Orientation.HORIZONTAL
        ? ( (IDataReorderColumns) m_data ).reorderColumns( m_indexes, m_insert )
        : ( (IDataReorderRows) m_data ).reorderRows( m_indexes, m_insert );
    if ( !success )
      m_data = null;
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    // action command, data-model should signal which views to redraw
    if ( m_orientation == Orientation.HORIZONTAL )
      ( (IDataReorderColumns) m_data ).reorderColumns( m_indexes, m_insert );
    else
      ( (IDataReorderRows) m_data ).reorderRows( m_indexes, m_insert );
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
        if ( m_orientation == Orientation.HORIZONTAL )
          ( (IDataReorderColumns) m_data ).reorderColumns( index, oldPos );
        else
          ( (IDataReorderRows) m_data ).reorderRows( index, oldPos );
        newOffset--;
      }
      else
      {
        index.clear();
        index.add( m_insert );
        if ( m_orientation == Orientation.HORIZONTAL )
          ( (IDataReorderColumns) m_data ).reorderColumns( index, oldPos + oldOffset );
        else
          ( (IDataReorderRows) m_data ).reorderRows( index, oldPos + oldOffset );
        oldOffset--;
      }
  }

  /******************************************* text **********************************************/
  @Override
  public String text()
  {
    // command description
    if ( m_text == null )
      m_text = "Moved " + m_indexes.size() + ( m_orientation == Orientation.HORIZONTAL ? " column" : " row" )
          + ( m_indexes.size() > 1 ? "s" : "" );

    return m_text;
  }

  /******************************************* isValid *******************************************/
  @Override
  public boolean isValid()
  {
    // command is valid only if reorder results in difference
    return m_data != null;
  }
}
