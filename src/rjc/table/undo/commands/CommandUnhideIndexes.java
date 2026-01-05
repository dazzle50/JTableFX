/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
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

/*************************************************************************************************/
/**************** UndoCommand for unhiding hidden columns or rows on a table-view ****************/
/*************************************************************************************************/

public class CommandUnhideIndexes implements IUndoCommand
{
  private TableView  m_view;    // table view
  private TableAxis  m_axis;    // axis for unhiding
  private HashSetInt m_indexes; // data-indexes being shown
  private String     m_text;    // text describing command

  /**************************************** constructor ******************************************/
  public CommandUnhideIndexes( TableView view, TableAxis axis, HashSetInt viewIndexes )
  {
    // prepare unhide command to show specified hidden indexes
    m_view = view;
    m_axis = axis;

    // convert view indexes to data indexes and hide them
    HashSetInt dataIndexes = new HashSetInt( viewIndexes.size() );
    var it = viewIndexes.iterator();
    while ( it.hasNext() )
      dataIndexes.add( m_axis.getDataIndex( it.next() ) );

    m_indexes = axis.unhide( dataIndexes );
    m_view.redraw();
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    // unhide the indexes
    m_axis.unhide( m_indexes );
    m_view.redraw();
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // re-hide the indexes
    m_axis.hide( m_indexes );
    m_view.redraw();
  }

  /******************************************* text **********************************************/
  @Override
  public String text()
  {
    // construct command description
    if ( m_text == null )
    {
      m_text = "Unhide " + m_indexes.size() + ( m_axis == m_view.getColumnsAxis() ? " column" : " row" )
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
    // command is valid only if some indexes unhidden (indicated by m_indexes not null)
    return m_indexes != null;
  }

}