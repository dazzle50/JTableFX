
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

import java.util.Map;

import rjc.table.view.TableView;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/******************** UndoCommand for resizing all columns or rows + default *********************/
/*************************************************************************************************/

public class CommandResizeAll implements ICommandResize
{
  private TableView           m_view;          // table view
  private TableAxis           m_axis;          // columns or rows being resized
  private String              m_text;          // text describing command

  private Map<Integer, Short> m_oldExceptions; // old size exceptions before resize
  private int                 m_oldDefault;    // old default before resize
  private int                 m_newDefault;    // new default

  /**************************************** constructor ******************************************/
  public CommandResizeAll( TableView view, TableAxis axis )
  {
    // prepare resize all command
    m_view = view;
    m_axis = axis;

    // get old default size and exceptions before resizing starts
    m_oldDefault = axis.getDefaultNominalSize();
    m_oldExceptions = axis.getSizeExceptions();
  }

  /***************************************** setNewSize ******************************************/
  @Override
  public void setNewSize( int size )
  {
    // set command new size
    m_newDefault = size;
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    // action command
    m_axis.setDefaultNominalSize( m_newDefault );
    m_axis.clearSizeExceptions();
    m_view.redraw();
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // revert command
    m_axis.setDefaultNominalSize( m_oldDefault );
    m_oldExceptions.forEach( ( index, size ) -> m_axis.setNominalSize( index, size ) );
    m_view.redraw();
  }

  /******************************************* text **********************************************/
  @Override
  public String text()
  {
    // command description
    if ( m_text == null )
    {
      m_text = "Resized all" + ( m_axis == m_view.getColumnsAxis() ? " columns" : " rows" );

      if ( m_view.getId() != null )
        m_text = m_view.getId() + " - " + m_text;
    }

    return m_text;
  }

}