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

import rjc.table.undo.IUndoCommand;
import rjc.table.view.TableView;

/*************************************************************************************************/
/*************************** UndoCommand for table view zoom in or out ***************************/
/*************************************************************************************************/

public class CommandZoom implements IUndoCommand
{
  private TableView m_view;    // table view
  private double    m_oldZoom; // old zoom setting
  private double    m_newZoom; // new zoom setting
  private String    m_text;    // text describing command

  /**************************************** constructor ******************************************/
  public CommandZoom( TableView view, double oldZoom, double newZoom )
  {
    // prepare zoom command
    m_view = view;
    m_oldZoom = oldZoom;
    m_newZoom = newZoom;
  }

  /******************************************* setZoom *******************************************/
  public void setZoom( double zoom )
  {
    // update command new zoom
    if ( m_newZoom != zoom )
    {
      m_newZoom = zoom;
      m_text = null;
    }
  }

  /******************************************* getView *******************************************/
  public TableView getView()
  {
    // return table view
    return m_view;
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    // action command
    m_view.getZoom().set( m_newZoom );
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // revert command
    m_view.getZoom().set( m_oldZoom );
  }

  /******************************************* text **********************************************/
  @Override
  public String text()
  {
    // command description
    if ( m_text == null )
    {
      if ( m_view.getId() == null )
        m_text = "Zoom " + Math.round( m_newZoom * 100 ) + "%";
      else
        m_text = m_view.getId() + " zoom " + Math.round( m_newZoom * 100 ) + "%";
    }

    return m_text;
  }
}