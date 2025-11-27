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

import java.util.Objects;

import rjc.table.data.TableData;
import rjc.table.undo.IUndoCommand;
import rjc.table.view.axis.TableAxis;

/*************************************************************************************************/
/********************* Default UndoCommand for setting TableData cell value **********************/
/*************************************************************************************************/

public class CommandSetValue implements IUndoCommand
{
  private TableData m_data;
  private int       m_dataColumn;
  private int       m_dataRow;
  private Object    m_newValue;  // new value after command
  private Object    m_oldValue;  // old value before command
  private String    m_text;      // text describing command

  /**************************************** constructor ******************************************/
  public CommandSetValue( TableData tableData, int dataColumn, int dataRow, Object newValue )
  {
    // get old value before attempt to set
    m_oldValue = tableData.getValue( dataColumn, dataRow );

    // attempt to set, abort creating command if attempt failed
    if ( tableData.setValue( dataColumn, dataRow, newValue ) != null )
      return;

    // don't create command if new value (retrieved from model) equals old value
    m_newValue = tableData.getValue( dataColumn, dataRow );
    if ( Objects.equals( m_oldValue, m_newValue ) )
      return;

    // initialise private variables
    m_data = tableData;
    m_dataColumn = dataColumn;
    m_dataRow = dataRow;
  }

  /******************************************* redo **********************************************/
  @Override
  public void redo()
  {
    // action command
    m_data.setValue( m_dataColumn, m_dataRow, m_newValue );
  }

  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // revert command
    m_data.setValue( m_dataColumn, m_dataRow, m_oldValue );
  }

  /******************************************* text **********************************************/
  @Override
  public String text()
  {
    // command description
    if ( m_text == null )
      m_text = m_data.getValue( m_dataColumn, TableAxis.HEADER ) + " " + m_data.getValue( TableAxis.HEADER, m_dataRow )
          + " = " + m_newValue;

    return m_text;
  }

  /******************************************* isValid *******************************************/
  @Override
  public boolean isValid()
  {
    // command is only ready and valid when pointer to data is set
    return m_data != null;
  }
}