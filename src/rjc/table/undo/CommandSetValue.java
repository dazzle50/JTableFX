/**************************************************************************
 *  Copyright (C) 2021 by Richard Crook                                   *
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

package rjc.table.undo;

import rjc.table.Utils;
import rjc.table.data.TableData;
import rjc.table.view.axis.AxisBase;

/*************************************************************************************************/
/********************* Default UndoCommand for setting TableData cell value **********************/
/*************************************************************************************************/

public class CommandSetValue implements IUndoCommand
{
  private TableData m_data;
  private int       m_columnIndex;
  private int       m_rowIndex;
  private Object    m_newValue;   // new value after command
  private Object    m_oldValue;   // old value before command
  private String    m_text;       // text describing command

  /**************************************** constructor ******************************************/
  public CommandSetValue( TableData tableData, int columnIndex, int rowIndex, Object oldValue, Object newValue )
  {
    // initialise private variables
    m_data = tableData;
    m_columnIndex = columnIndex;
    m_rowIndex = rowIndex;
    m_newValue = newValue;
    m_oldValue = oldValue;
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    // action command
    if ( !m_data.setValue( m_columnIndex, m_rowIndex, m_newValue ) )
      Utils.trace( "WARNING : setValue was not successful! ", m_columnIndex, m_rowIndex, m_newValue, m_data );

    // redraw cell across all registered views
    m_data.redrawCell( m_columnIndex, m_rowIndex );
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // revert command
    if ( !m_data.setValue( m_columnIndex, m_rowIndex, m_oldValue ) )
      Utils.trace( "WARNING : setValue was not successful! ", m_columnIndex, m_rowIndex, m_oldValue, m_data );

    // redraw cell across all registered views
    m_data.redrawCell( m_columnIndex, m_rowIndex );
  }

  /******************************************* text **********************************************/
  @Override
  public String text()
  {
    // command description
    if ( m_text == null )
      m_text = m_data.getValue( m_columnIndex, AxisBase.HEADER ) + " " + m_data.getValue( AxisBase.HEADER, m_rowIndex )
          + " = " + m_newValue;

    return m_text;
  }

}
