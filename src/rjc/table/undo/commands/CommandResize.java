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

import java.util.HashMap;
import java.util.Map;

import rjc.table.HashSetInt;
import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/******************* UndoCommand for resizing columns widths or rows heights *********************/
/*************************************************************************************************/

public class CommandResize implements ICommandResize
{
  private TableView               m_view;     // table view
  private TableAxis               m_axis;     // columns or rows being resized
  private HashMap<Integer, Short> m_oldSizes; // data-index to old size map
  private int                     m_newSize;  // new size
  private String                  m_text;     // text describing command

  /**************************************** constructor ******************************************/
  public CommandResize( TableView view, TableAxis axis, HashSetInt viewIndexes )
  {
    // prepare resize command
    m_view = view;
    m_axis = axis;

    // populate map of data indexes being resized to their old sizes
    m_oldSizes = new HashMap<>();
    HashSetInt.IntIterator iter = viewIndexes.iterator();
    while ( iter.hasNext() )
    {
      int dataIndex = m_axis.getDataIndex( iter.next() );
      short size = m_axis.getNominalSize( dataIndex );
      m_oldSizes.put( dataIndex, size );
    }
  }

  /***************************************** setNewSize ******************************************/
  @Override
  public void setNewSize( int nominalSize )
  {
    // set command new size
    m_newSize = nominalSize;
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    // action command
    for ( Integer index : m_oldSizes.keySet() )
      m_axis.setNominalSize( index, m_newSize );

    m_view.redraw();
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // revert command - restore old size
    for ( Map.Entry<Integer, Short> entry : m_oldSizes.entrySet() )
      m_axis.setNominalSize( entry.getKey(), entry.getValue() );

    m_view.redraw();
  }

  /******************************************* text **********************************************/
  @Override
  public String text()
  {
    // command description
    if ( m_text == null )
    {
      m_text = "Resized " + m_oldSizes.size();
      m_text += m_axis == m_view.getColumnsAxis() ? " column" : " row";
      m_text += m_oldSizes.size() > 1 ? "s" : "";

      if ( m_view.getId() != null )
        m_text = m_view.getId() + " - " + m_text;
    }

    return m_text;
  }

}