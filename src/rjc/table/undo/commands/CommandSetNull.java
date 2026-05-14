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

import java.util.HashMap;

import rjc.table.data.TableData;
import rjc.table.undo.IUndoCommand;
import rjc.table.view.TableView;

/*************************************************************************************************/
/************ UndoCommand setting to null (deleting) multiple TableData cell values **************/
/*************************************************************************************************/

public class CommandSetNull implements IUndoCommand
{
  private TableData                m_data;        // table data model
  private int                      m_columnCount; // used to hash column & row indexes
  private HashMap<Integer, Object> m_oldValues;   // cells location and old value before setting to null
  private String                   m_text;        // text describing command

  /**************************************** constructor ******************************************/
  public CommandSetNull( TableView view )
  {
    // initialise private variables - after creating command use add() method populate old-values hash-map
    m_data = view.getData();
    m_columnCount = m_data.getColumnCount();
    m_oldValues = new HashMap<>();
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    // set value to null of selected cells
    m_oldValues.keySet().forEach( hash ->
    {
      int col = hash % m_columnCount;
      int row = hash / m_columnCount;
      m_data.setValue( col, row, null );
      m_data.signalCellChanged( col, row );
    } );
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // set deleted cells back to their original values
    m_oldValues.forEach( ( hash, oldValue ) ->
    {
      int col = hash % m_columnCount;
      int row = hash / m_columnCount;
      m_data.setValue( col, row, oldValue );
      m_data.signalCellChanged( col, row );
    } );
  }

  /******************************************* text **********************************************/
  @Override
  public String text()
  {
    // command description
    if ( m_text == null )
      m_text = "Deleted " + m_oldValues.size() + " cell" + ( m_oldValues.size() == 1 ? "" : "s" );
    return m_text;
  }

  /******************************************* isValid *******************************************/
  @Override
  public boolean isValid()
  {
    // command is only ready and valid when old-values hash-map is not empty
    return !m_oldValues.isEmpty();
  }

  /********************************************* add *********************************************/
  public void add( int columnIndex, int rowIndex )
  {
    // if can successfully set cell value to null, store old value in hash-map
    Object oldValue = m_data.getValue( columnIndex, rowIndex );
    if ( oldValue != null && m_data.setValue( columnIndex, rowIndex, null ) == null )
    {
      int hash = rowIndex * m_columnCount + columnIndex;
      m_oldValues.put( hash, oldValue );
      m_data.signalCellChanged( columnIndex, rowIndex );
      m_text = null;
    }
  }

}